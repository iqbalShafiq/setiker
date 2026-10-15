package data.auth

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Swift Sign in with Apple registers a handler here on iOS app launch.
 * See iosApp/iosApp/AppleSignInManager.swift
 */
object IosAppleSignInIntegration {
    private var signInHandler: (((String?) -> Unit) -> Unit)? = null

    fun registerAppleSignInHandler(signIn: ((String?) -> Unit) -> Unit) -> Unit) {
        signInHandler = signIn
    }

    internal fun isHandlerRegistered(): Boolean = signInHandler != null

    internal suspend fun signIn(): String? {
        val handler = signInHandler ?: return null
        return suspendCancellableCoroutine { continuation ->
            handler { idToken ->
                continuation.resume(idToken)
            }
        }
    }
}
