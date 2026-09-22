package bob.colbaskin.gidromonitor.features.map.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import bob.colbaskin.gidromonitor.common.ApiResult
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisRasterFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mil.nga.tiff.TiffReader
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.roundToInt

data class DecodedCogRaster(val fileName: String, val channels: List<DecodedCogChannel>, val north: Double, val south: Double, val west: Double, val east: Double)
data class DecodedCogChannel(val id: String, val title: String, val bitmap: Bitmap, val color: Int)
data class CachedCogPreview(val filePath: String, val north: Double, val south: Double, val west: Double, val east: Double)

class CogRasterLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(file: AnalysisRasterFile, onPartialDecoded: suspend (DecodedCogRaster) -> Unit = {}): ApiResult<DecodedCogRaster> = withContext(Dispatchers.IO) {
        try {
            val directory = cacheDirectory(file).apply { mkdirs(); setLastModified(System.currentTimeMillis()) }
            readRenderedCache(directory, file.fileName, onPartialDecoded)?.let { return@withContext ApiResult.Success(it) }
            val source = File(directory, SOURCE_FILE)
            if (!source.exists() || source.length() == 0L) copySource(file, source)
            val decoded = decode(source, file.fileName, onPartialDecoded)
            saveRenderedCache(directory, decoded)
            trimCache()
            ApiResult.Success(decoded)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            ApiResult.Error("Не удалось загрузить слои", "Проверьте подключение к интернету и попробуйте открыть сравнение ещё раз.")
        }
    }

    suspend fun cachedOccurrencePreview(file: AnalysisRasterFile): CachedCogPreview? = withContext(Dispatchers.IO) {
        val directory = cacheDirectory(file)
        val metadata = readMetadata(directory) ?: return@withContext null
        val occurrence = metadata.channels.firstOrNull { it.title == OCCURRENCE } ?: return@withContext null
        val preview = File(directory, occurrence.previewFile)
        if (!preview.isFile || preview.length() == 0L) return@withContext null
        directory.setLastModified(System.currentTimeMillis())
        CachedCogPreview(preview.absolutePath, metadata.north, metadata.south, metadata.west, metadata.east)
    }

    private suspend fun readRenderedCache(directory: File, fileName: String, callback: suspend (DecodedCogRaster) -> Unit): DecodedCogRaster? {
        val metadata = readMetadata(directory) ?: return null
        if (metadata.channels.isEmpty()) return null
        val channels = mutableListOf<DecodedCogChannel>()
        for (item in metadata.channels) {
            val bitmap = BitmapFactory.decodeFile(File(directory, item.previewFile).absolutePath) ?: return null
            channels += DecodedCogChannel("$fileName#${item.sample}", item.title, bitmap, item.color)
            callback(DecodedCogRaster(fileName, channels.toList(), metadata.north, metadata.south, metadata.west, metadata.east))
            yield()
        }
        return DecodedCogRaster(fileName, channels, metadata.north, metadata.south, metadata.west, metadata.east)
    }

    private fun readMetadata(directory: File): CachedMetadata? = runCatching {
        val file = File(directory, METADATA_FILE)
        if (file.isFile) json.decodeFromString(CachedMetadata.serializer(), file.readText()) else null
    }.getOrNull()

    private fun saveRenderedCache(directory: File, raster: DecodedCogRaster) {
        val channels = raster.channels.mapIndexed { index, channel ->
            val name = "channel-$index.png"; val temporary = File(directory, "$name.part")
            temporary.outputStream().use { channel.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val target = File(directory, name); if (target.exists()) target.delete()
            if (!temporary.renameTo(target)) throw IOException("Не удалось сохранить канал COG")
            CachedChannel(index, channel.title, channel.color, name)
        }
        val temp = File(directory, "$METADATA_FILE.part")
        temp.writeText(json.encodeToString(CachedMetadata.serializer(), CachedMetadata(raster.north, raster.south, raster.west, raster.east, channels)))
        val metadata = File(directory, METADATA_FILE); if (metadata.exists()) metadata.delete()
        if (!temp.renameTo(metadata)) throw IOException("Не удалось сохранить метаданные COG")
    }

    private fun copySource(file: AnalysisRasterFile, target: File) {
        val partial = File(target.parentFile, "$SOURCE_FILE.part"); partial.delete()
        try {
            if (file.downloadUrl.startsWith(ASSET_URL_PREFIX)) {
                context.assets.open(file.downloadUrl.removePrefix(ASSET_URL_PREFIX)).use { input -> partial.outputStream().use { input.copyTo(it) } }
            } else okHttpClient.newCall(Request.Builder().url(file.downloadUrl).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Файл COG: HTTP ${response.code}")
                val body = response.body ?: throw IOException("Пустой ответ COG")
                body.byteStream().use { input -> partial.outputStream().use { input.copyTo(it) } }
            }
            if (partial.length() == 0L) throw IOException("Файл COG загружен пустым")
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) throw IOException("Не удалось сохранить COG на устройстве")
        } finally { if (partial.exists()) partial.delete() }
    }

    private suspend fun decode(file: File, fileName: String, callback: suspend (DecodedCogRaster) -> Unit): DecodedCogRaster {
        val directory = TiffReader.readTiff(file).fileDirectory
        val scale = directory.modelPixelScale ?: error("В COG отсутствует ModelPixelScale")
        val tie = directory.modelTiepoint ?: error("В COG отсутствует ModelTiepoint")
        require(scale.size >= 2 && tie.size >= 6) { "Некорректная геопривязка COG" }
        val inputWidth = directory.imageWidth.toInt(); val inputHeight = directory.imageHeight.toInt(); val samples = directory.samplesPerPixel
        require(samples >= 1) { "В COG отсутствуют каналы" }
        val step = maxOf(1, maxOf(inputWidth, inputHeight) / 512); val width = (inputWidth + step - 1) / step; val height = (inputHeight + step - 1) / step
        val north = mercatorLatitude(tie[4]); val south = mercatorLatitude(tie[4] - inputHeight * scale[1]); val west = mercatorLongitude(tie[3]); val east = mercatorLongitude(tie[3] + inputWidth * scale[0])
        val channels = mutableListOf<DecodedCogChannel>()
        for (sample in 0 until samples) {
            val raster = directory.readRasters(intArrayOf(sample)); val range = sampleRange(raster, inputWidth, inputHeight, step)
            val pixels = IntArray(width * height); val color = channelColors[sample % channelColors.size]
            for (y in 0 until height) for (x in 0 until width) {
                val value = raster.getPixelSample(0, minOf(x * step, inputWidth - 1), minOf(y * step, inputHeight - 1)).toDouble()
                if (value.isFinite() && value != 0.0) pixels[y * width + x] = ((normalize(value, range) * 0.72).roundToInt() shl 24) or color
            }
            channels += DecodedCogChannel("$fileName#$sample", channelNames.getOrElse(sample) { "Канал ${sample + 1}" }, Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { setPixels(pixels, 0, width, 0, 0, width, height) }, color)
            callback(DecodedCogRaster(fileName, channels.toList(), north, south, west, east)); yield()
        }
        return DecodedCogRaster(fileName, channels, north, south, west, east)
    }

    private fun sampleRange(raster: mil.nga.tiff.Rasters, width: Int, height: Int, step: Int): Pair<Double, Double> {
        var low = Double.POSITIVE_INFINITY; var high = Double.NEGATIVE_INFINITY
        for (y in 0 until height step step) for (x in 0 until width step step) { val value = raster.getPixelSample(0, x, y).toDouble(); if (value.isFinite() && value != 0.0) { low = minOf(low, value); high = maxOf(high, value) } }
        return low to high
    }
    private fun normalize(value: Double, range: Pair<Double, Double>): Int { val (low, high) = range; return if (!value.isFinite() || !low.isFinite() || high <= low) 0 else (((value - low) / (high - low)).coerceIn(0.0, 1.0) * 255).roundToInt() }
    private fun cacheDirectory(file: AnalysisRasterFile) = File(File(context.filesDir, CACHE_DIRECTORY), sha256("$CACHE_VERSION|${file.offlineCacheKey}"))
    private fun trimCache() {
        val root = File(context.filesDir, CACHE_DIRECTORY); val directories = root.listFiles()?.filter(File::isDirectory)?.sortedBy(File::lastModified).orEmpty().toMutableList()
        fun size(dir: File) = dir.walkTopDown().filter(File::isFile).sumOf(File::length)
        var total = directories.sumOf(::size)
        while ((directories.size > MAX_ENTRIES || total > MAX_BYTES) && directories.isNotEmpty()) { val victim = directories.removeAt(0); total -= size(victim); victim.deleteRecursively() }
    }
    private fun mercatorLongitude(value: Double) = Math.toDegrees(value / WEB_MERCATOR_RADIUS)
    private fun mercatorLatitude(value: Double) = Math.toDegrees(2 * atan(exp(value / WEB_MERCATOR_RADIUS)) - Math.PI / 2)
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    @Serializable private data class CachedMetadata(val north: Double, val south: Double, val west: Double, val east: Double, val channels: List<CachedChannel>)
    @Serializable private data class CachedChannel(val sample: Int, val title: String, val color: Int, val previewFile: String)
    private companion object {
        const val CACHE_DIRECTORY = "cog-offline"; const val SOURCE_FILE = "source.tif"; const val METADATA_FILE = "metadata.json"; const val CACHE_VERSION = 4
        const val ASSET_URL_PREFIX = "asset://"; const val OCCURRENCE = "OCCURRENCE"; const val MAX_ENTRIES = 5; const val MAX_BYTES = 250L * 1024 * 1024; const val WEB_MERCATOR_RADIUS = 6_378_137.0
        val channelNames = listOf("HAND", "NDVI", "NDWI", "MNDWI", "AWEISH", "SLOPE", "VV_VH", OCCURRENCE, "SEASONALITY")
        val channelColors = intArrayOf(0xFF8D6E63.toInt(), 0xFF33A852.toInt(), 0xFF0786E8.toInt(), 0xFF00A6B2.toInt(), 0xFFCF4D88.toInt(), 0xFFFFA000.toInt(), 0xFF7E57C2.toInt(), 0xFF1565C0.toInt(), 0xFFD84A9A.toInt())
    }
}
