package io.nuvalence.cx.tools.shared

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.SheetsScopes.*
import java.io.File
import java.io.InputStreamReader
import java.net.URL

/**
 * Implementation of Google's authorizer. You can find the details in the following URL:
 *
 * https://developers.google.com/workspace/guides/configure-oauth-consent
 *
 * This is required to control access to Google Sheets.
 */
class Authorizer(private val credentialsURL: URL, private val transport: HttpTransport) {
    fun getCredentials(): Credential? {
        credentialsURL.openStream().use { inputStream ->
            val jsonFactory = GsonFactory.getDefaultInstance()
            val clientSecrets: GoogleClientSecrets = GoogleClientSecrets.load(jsonFactory, InputStreamReader(inputStream))
            val flow: GoogleAuthorizationCodeFlow =
                GoogleAuthorizationCodeFlow
                    .Builder(
                        transport,
                        jsonFactory,
                        clientSecrets,
                        listOf(SPREADSHEETS, DRIVE, DRIVE_FILE)
                    ).setDataStoreFactory(FileDataStoreFactory(File("tokens")))
                    .setAccessType("offline")
                    .build()
            val receiver: LocalServerReceiver = LocalServerReceiver.Builder().setPort(8888).build()
            return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
        }
    }
}
