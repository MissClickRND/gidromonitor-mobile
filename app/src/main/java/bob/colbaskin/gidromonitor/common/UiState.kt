package bob.colbaskin.gidromonitor.common

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val title: String, val text: String) : UiState<Nothing>
}

fun <T> UiState<T>.updateIfSuccess(block: T.() -> T): UiState<T> = when (this) {
    is UiState.Success -> UiState.Success(data.block())
    else -> this
}

fun <T> UiState<T>.takeIfSuccess(): T? = (this as? UiState.Success<T>)?.data
