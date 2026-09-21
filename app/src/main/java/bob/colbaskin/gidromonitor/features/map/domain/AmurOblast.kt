package bob.colbaskin.gidromonitor.features.map.domain

import bob.colbaskin.gidromonitor.features.analysis.domain.model.GeoPoint

object AmurOblast {
    private const val MIN_LATITUDE = 48.85
    private const val MAX_LATITUDE = 57.10
    private const val MIN_LONGITUDE = 119.65
    private const val MAX_LONGITUDE = 134.95

    fun contains(point: GeoPoint): Boolean =
        point.latitude in MIN_LATITUDE..MAX_LATITUDE &&
            point.longitude in MIN_LONGITUDE..MAX_LONGITUDE

    fun containsAll(points: List<GeoPoint>): Boolean = points.all(::contains)
}
