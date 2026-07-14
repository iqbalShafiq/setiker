package data.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.setiker.app.BuildConfig
import com.setiker.app.MainActivityHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidGoogleSignInGateway(
    private val activityProvider: () -> Activity? = { MainActivityHolder.current }
) : GoogleSignInGateway {

    override fun isAvailable(): Boolean =
        BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    override suspend fun signIn(mode: GoogleSignInMode): Result<GoogleIdTokenResult> =
        withContext(Dispatchers.Main) {
            val activity = activityProvider()
                ?: return@withContext Result.failure(IllegalStateException("No activity for Google Sign-In"))
            val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            if (serverClientId.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("GOOGLE_WEB_CLIENT_ID is not configured in local.properties")
                )
            }

            val credentialManager = CredentialManager.create(activity)
            try {
                when (mode) {
                    GoogleSignInMode.OneTap -> {
                        val idToken = requestOneTap(credentialManager, activity, serverClientId)
                            ?: return@withContext Result.failure(NoCredentialException("No Google credential"))
                        Result.success(GoogleIdTokenResult(idToken))
                    }
                    GoogleSignInMode.Button -> {
                        val idToken = requestButton(credentialManager, activity, serverClientId)
                            ?: return@withContext Result.failure(IllegalStateException("Google ID token missing"))
                        Result.success(GoogleIdTokenResult(idToken))
                    }
                }
            } catch (e: GetCredentialCancellationException) {
                Result.failure(e)
            } catch (e: NoCredentialException) {
                Result.failure(e)
            } catch (e: GetCredentialException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun requestOneTap(
        credentialManager: CredentialManager,
        activity: Activity,
        serverClientId: String
    ): String? {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()
        return extractIdToken(credentialManager, activity, option)
    }

    private suspend fun requestButton(
        credentialManager: CredentialManager,
        activity: Activity,
        serverClientId: String
    ): String? {
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        return extractIdToken(credentialManager, activity, option)
    }

    private suspend fun extractIdToken(
        credentialManager: CredentialManager,
        activity: Activity,
        option: androidx.credentials.CredentialOption
    ): String? {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val response = credentialManager.getCredential(activity, request)
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return try {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } catch (_: GoogleIdTokenParsingException) {
                null
            }
        }
        return null
    }
}
