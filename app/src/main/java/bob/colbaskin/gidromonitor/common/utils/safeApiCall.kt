package bob.colbaskin.gidromonitor.common.utils

import bob.colbaskin.gidromonitor.common.ApiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import retrofit2.HttpException
import java.io.IOException

suspend inline fun <reified T, reified R> safeApiCall(
    apiCall: suspend () -> T,
    successHandler: (T) -> R
): ApiResult<R> = try {
    ApiResult.Success(successHandler(apiCall()))
} catch (exception: TimeoutCancellationException) {
    ApiResult.Error("Пока не получилось", "Ответ занимает слишком много времени. Попробуйте ещё раз немного позже.")
} catch (exception: CancellationException) {
    throw exception
} catch (exception: IOException) {
    ApiResult.Error("Нет подключения", "Проверьте интернет-соединение и повторите попытку.")
} catch (exception: HttpException) {
    when (exception.code()) {
        400, 422 -> ApiResult.Error("Не удалось обработать запрос", "Проверьте выбранную территорию и даты, затем попробуйте снова.")
        404 -> ApiResult.Error("Данные не найдены", "Данные для этого запроса не найдены.")
        in 500..599 -> ApiResult.Error("Сервис временно недоступен", "Попробуйте ещё раз немного позже.")
        else -> ApiResult.Error("Не удалось получить данные", "Попробуйте ещё раз немного позже.")
    }
} catch (exception: Exception) {
    ApiResult.Error("Что-то пошло не так", "Попробуйте ещё раз немного позже.")
}
