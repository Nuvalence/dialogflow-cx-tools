package io.nuvalence.cx.tools.shared

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import java.net.URL

/**
 * Reads the supplied range of a Google Sheet and converts it to a list of lists of strings.
 *
 * @param credentialsURL URL pointing to where the credentials.json file is (e.g.
 * file:///path/to/credentials.json)
 * @param spreadsheetId the ID of the Google Sheet (details below).
 * @param range String specifying the range. For example, to read from Sheet1, this parameter
 * should contain the string "Sheet1". You can specify cell ranges as well, please see the
 * Google Sheets documentation for details.
 *
 * For the spreadsheetId, given this URL:
 * https://docs.google.com/spreadsheets/d/16xjZ4tnVlRLlwd0jiKX6Q7d3TV08jtGWF_SxoCS-DHc/edit#gid=27424266
 * The id is 16xjZ4tnVlRLlwd0jiKX6Q7d3TV08jtGWF_SxoCS-DHc
 */
class SheetReader(private val credentialsURL: URL, private val spreadsheetId: String, private val range: String) {
    fun read(): List<List<String>> {
        val transport = GoogleNetHttpTransport.newTrustedTransport()
        val service = Sheets.Builder(
            transport,
            GsonFactory.getDefaultInstance(),
            Authorizer(credentialsURL, transport).getCredentials()
        )
            .setApplicationName("Dialogflow Agent Generator")
            .build()
        val response = service.spreadsheets().values()
            .get(spreadsheetId, range)
            .execute()
        @Suppress("UNCHECKED_CAST")
        return response.getValues() as List<List<String>>
    }
}
