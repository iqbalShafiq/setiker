package domain.error

/**
 * Typed Result for the Setiker data/presentation layers, per the `android-error-handling` skill.
 * Marker name [AppError] (instead of the skill's `Error`) avoids import ambiguity with
 * `kotlin.Error` at usage sites.
 */
sealed interface Result<out D, out E : AppError> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : AppError>(val error: E) : Result<Nothing, E>
}

/** Convenience type for operations with no meaningful success payload. (Bounds are prohibited on typealias parameters.) */
typealias EmptyResult<E> = Result<Unit, E>

/** Runs [transform] on the success branch, passes errors through unchanged. */
inline fun <T, E : AppError, R> Result<T, E>.map(
    transform: (T) -> R,
): Result<R, E> = when (this) {
    is Result.Error -> Result.Error(error)
    is Result.Success -> Result.Success(transform(data))
}

/** Invokes [action] on success; the Result itself is returned for chaining. */
inline fun <T, E : AppError> Result<T, E>.onSuccess(
    action: (T) -> Unit,
): Result<T, E> = when (this) {
    is Result.Error -> this
    is Result.Success -> {
        action(data)
        this
    }
}

/** Invokes [action] on failure; the Result itself is returned for chaining. */
inline fun <T, E : AppError> Result<T, E>.onFailure(
    action: (E) -> Unit,
): Result<T, E> = when (this) {
    is Result.Error -> {
        action(error)
        this
    }
    is Result.Success -> this
}

/** Drops the payload, keeping the (potentially typed) error. */
fun <T, E : AppError> Result<T, E>.asEmpty(): EmptyResult<E> = when (this) {
    is Result.Error -> Result.Error(error)
    is Result.Success -> Result.Success(Unit)
}

fun <T, E : AppError> Result<T, E>.getOrNull(): T? = when (this) {
    is Result.Error -> null
    is Result.Success -> data
}

fun <T, E : AppError> Result<T, E>.getOrDefault(default: T): T = when (this) {
    is Result.Error -> default
    is Result.Success -> data
}

inline fun <T, E : AppError> Result<T, E>.getOrElse(
    onError: (E) -> T,
): T = when (this) {
    is Result.Error -> onError(error)
    is Result.Success -> data
}
