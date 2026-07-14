package data.auth

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Swift Google Sign-In registers a handler here on iOS app launch.
 * See iosApp/iosApp/GoogleSignInManager.swift
 */
object IosGoogleSignInIntegration {
    private var signInHandler: (((String?) -> Unit) -> Unit)? = null

    fun registerGoogleSignInHandler(signIn: ((String?) -> Unit) -> Unit) -> Unit) {
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
