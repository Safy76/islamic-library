package com.example.ui.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object PdfDownloader {

    private val client = OkHttpClient.Builder().build()

    /**
     * Downloads a book volume PDF and saves it to the system's public Download/Islamic Library folder.
     * Reports download progress via the [onProgress] callback.
     * Returns a Pair: (successUri, successFile)
     */
    suspend fun downloadPdf(
        context: Context,
        bookName: String,
        volumeName: String,
        volumeNumber: Int,
        pdfUrl: String,
        onProgress: (Int) -> Unit = {}
    ): Pair<Uri?, File?> = withContext(Dispatchers.IO) {
        try {
            val fileId = Config.extractDriveId(pdfUrl)
            val downloadUrl = if (fileId != null) {
                "https://drive.google.com/uc?export=download&id=$fileId"
            } else {
                pdfUrl
            }

            // Create a sanitised, clean file name: "BookName_Vol_X.pdf"
            val sanitizedBook = bookName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            val sanitizedVolName = volumeName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            val fileName = "${sanitizedBook}_${sanitizedVolName}_Vol_$volumeNumber.pdf"

            val request = Request.Builder().url(downloadUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Pair(null, null)
            }

            val responseBody = response.body ?: return@withContext Pair(null, null)
            val totalBytes = responseBody.contentLength()
            val inputStream: InputStream = responseBody.byteStream()

            var bytesCopied: Long = 0
            val buffer = ByteArray(8 * 1024)
            var bytes = inputStream.read(buffer)

            val outputStream: OutputStream?
            val uriQuery: Uri?
            val targetFile: File?
            val resolver = context.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Modern Android Scoped Storage: use MediaStore.Downloads
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Islamic Library")
                }
                uriQuery = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = uriQuery?.let { resolver.openOutputStream(it) }
                targetFile = null
            } else {
                // Legacy storage (API < 29)
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val libraryFolder = File(downloadDir, "Islamic Library")
                if (!libraryFolder.exists()) {
                    libraryFolder.mkdirs()
                }
                val tFile = File(libraryFolder, fileName)
                outputStream = FileOutputStream(tFile)
                uriQuery = null
                targetFile = tFile
            }

            if (outputStream != null) {
                outputStream.use { out ->
                    while (bytes >= 0) {
                        out.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        if (totalBytes > 0) {
                            val progress = ((bytesCopied * 100) / totalBytes).toInt()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        } else {
                            // Indeterminate simulation or safe fallback
                            val simulatedProgress = (bytesCopied / 1024 / 100).toInt().coerceAtMost(99)
                            withContext(Dispatchers.Main) {
                                onProgress(simulatedProgress)
                            }
                        }
                        bytes = inputStream.read(buffer)
                    }
                }

                // Finalize 100%
                withContext(Dispatchers.Main) {
                    onProgress(100)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    return@withContext Pair(uriQuery, null)
                } else {
                    targetFile?.let {
                        // Scan file so it's visible on the device
                        MediaScannerConnection.scanFile(context, arrayOf(it.absolutePath), arrayOf("application/pdf"), null)
                        val fileUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            it
                        )
                        return@withContext Pair(fileUri, it)
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext Pair(null, null)
    }

    /**
     * Share a PDF file by first downloading it to "Download/Islamic Library" and then showing the system share sheet.
     */
    suspend fun downloadAndSharePdf(
        context: Context,
        bookName: String,
        volumeName: String,
        volumeNumber: Int,
        pdfUrl: String,
        onStart: () -> Unit,
        onFinished: (Boolean, String) -> Unit,
        onProgress: (Int) -> Unit = {}
    ) {
        withContext(Dispatchers.Main) { onStart() }
        val (uri, file) = downloadPdf(context, bookName, volumeName, volumeNumber, pdfUrl, onProgress)
        withContext(Dispatchers.Main) {
            if (uri != null) {
                onFinished(true, "PDF saved to Download/Islamic Library successfully!")
                try {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_SUBJECT, "$bookName - $volumeName")
                        putExtra(Intent.EXTRA_TEXT, "Read and download '$bookName - $volumeName' from our Islamic Library.")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Book PDF"))
                } catch (e: Exception) {
                    onFinished(false, "Failed to share PDF: ${e.message}")
                }
            } else {
                onFinished(false, "Could not download the file. Please check your connection or link.")
            }
        }
    }

    /**
     * Gets a cached local PDF file or downloads it if not present.
     * Essential for viewing huge files (100MB+) instantly and offline.
     */
    suspend fun getCachedPdfFile(
        context: Context,
        bookName: String,
        volumeName: String,
        volumeNumber: Int,
        pdfUrl: String,
        onProgress: (Int) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        try {
            val fileId = Config.extractDriveId(pdfUrl)
            val downloadUrl = if (fileId != null) {
                "https://drive.google.com/uc?export=download&id=$fileId"
            } else {
                pdfUrl
            }

            // Clean cache file name
            val sanitizedBook = bookName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            val sanitizedVolName = volumeName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            val fileName = "${sanitizedBook}_${sanitizedVolName}_Vol_${volumeNumber}_cache.pdf"
            val cacheFile = File(context.cacheDir, fileName)

            // If already cached, return immediately
            if (cacheFile.exists() && cacheFile.length() > 0) {
                withContext(Dispatchers.Main) {
                    onProgress(100)
                }
                return@withContext cacheFile
            }

            // Otherwise download to cache
            val request = Request.Builder().url(downloadUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext null
            }

            val responseBody = response.body ?: return@withContext null
            val totalBytes = responseBody.contentLength()
            val inputStream: InputStream = responseBody.byteStream()

            var bytesCopied: Long = 0
            val buffer = ByteArray(8 * 1024)
            var bytes = inputStream.read(buffer)

            val outputStream = FileOutputStream(cacheFile)
            outputStream.use { out ->
                while (bytes >= 0) {
                    out.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    if (totalBytes > 0) {
                        val progress = ((bytesCopied * 100) / totalBytes).toInt()
                        withContext(Dispatchers.Main) {
                            onProgress(progress)
                        }
                    } else {
                        val simulatedProgress = (bytesCopied / 1024 / 100).toInt().coerceAtMost(99)
                        withContext(Dispatchers.Main) {
                            onProgress(simulatedProgress)
                        }
                    }
                    bytes = inputStream.read(buffer)
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(100)
            }
            return@withContext cacheFile
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
