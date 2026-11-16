package com.ehs.finances.feature.auth

import android.content.Context
import com.ehs.finances.BuildConfig
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Login Google via Credential Manager. O botão só aparece quando o client ID está configurado. */
object GoogleSignIn {
    val isConfigured: Boolean get() = BuildConfig.GOOGLE_CLIENT_ID.isNotBlank()

    suspend fun idToken(context: Context): String? {
        if (!isConfigured) return null
        val manager = CredentialManager.create(context)
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val response = manager.getCredential(context = context, request = request)
        val credential = response.credential
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        return null
    }
}
