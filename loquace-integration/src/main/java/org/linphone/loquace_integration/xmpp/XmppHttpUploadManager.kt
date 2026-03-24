package org.linphone.loquace_integration.xmpp

import android.util.Log
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager
import java.io.File

object XmppHttpUploadManager {

    private const val TAG = "XmppHttpUpload"

    suspend fun uploadFile(
        connection: XMPPTCPConnection,
        file: File
    ): String? {
        return try {
            Log.d(TAG, "Uploading file ${file.name} (${file.length()} bytes)")
            val manager = HttpFileUploadManager.getInstanceFor(connection)

            if (!manager.isUploadServiceDiscovered) {
                Log.d(TAG, "Discovering upload service...")
                manager.discoverUploadService()
            }

            if (!manager.isUploadServiceDiscovered) {
                Log.e(TAG, "HTTP upload service not available on this server")
                return null
            }

            val url = manager.uploadFile(file)
            Log.d(TAG, "File uploaded successfully: $url")
            url.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file: ${e.message}")
            null
        }
    }
}