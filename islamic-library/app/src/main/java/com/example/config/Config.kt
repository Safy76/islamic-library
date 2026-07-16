package com.example.config

object Config {
    // Google Apps Script Web App Deployment URL
    // This allows the app to communicate with Google Sheets and Google Drive
    var API_URL = "https://script.google.com/macros/s/AKfycbzkRiXQ1TfWVbCtl-MGXnMg9MiVtoGGzqCRqBrpt7TpynlcM5wPzA3TzkVHDtHBbDcoXQ/exec"
    
    private fun extractId(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.contains("/d/") -> {
                val parts = trimmed.split("/d/")
                if (parts.size > 1) {
                    parts[1].split("/")[0].split("?")[0].split("#")[0]
                } else {
                    trimmed
                }
            }
            trimmed.contains("/folders/") -> {
                val parts = trimmed.split("/folders/")
                if (parts.size > 1) {
                    parts[1].split("?")[0].split("#")[0].split("/")[0]
                } else {
                    trimmed
                }
            }
            else -> trimmed
        }
    }

    var SPREADSHEET_ID: String = "1GRQ767NMW1siWHQXqxEHw5UdDQDLTkRGgSVhS07n2AU"
        get() = extractId(field)
        set(value) {
            field = extractId(value)
        }
    
    // Google Drive Folder IDs for uploads
    var FOLDER_BOOK_COVERS: String = "1Yw6wFYRkWqHuNMrtAMhZtWsaGs7BdkXi"
        get() = extractId(field)
        set(value) {
            field = extractId(value)
        }

    var FOLDER_VOLUME_THUMBNAILS: String = "1cWY33VF2AVM3LC_8jLd8QPEJKcgQG73O"
        get() = extractId(field)
        set(value) {
            field = extractId(value)
        }

    var FOLDER_BOOK_PDFS: String = "1b8AtdXrxRNuHQFLbPCtFRVKRVT6t_rj8"
        get() = extractId(field)
        set(value) {
            field = extractId(value)
        }
    
    const val ADMIN_EMAIL = "info@islamic.book.my.com"
    const val APP_NAME = "Al-Falah Library"
    const val APP_VERSION = "1.0.0"

    fun extractDriveId(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        
        // Bypass if it is a general HTTP/HTTPS URL not hosted on Google Drive/Docs
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            if (!trimmed.contains("drive.google.com", ignoreCase = true) && !trimmed.contains("docs.google.com", ignoreCase = true)) {
                return null
            }
        }
        
        // If it doesn't contain "/", "=", or "?", and its length is between 15 and 50, it is likely already a direct file ID.
        if (!trimmed.contains("/") && !trimmed.contains("=") && !trimmed.contains("?") && trimmed.length >= 15 && trimmed.length <= 50) {
            return trimmed
        }
        
        if (trimmed.contains("/d/")) {
            val parts = trimmed.split("/d/")
            if (parts.size > 1) {
                return parts[1].split("/")[0].split("?")[0].split("#")[0].trim()
            }
        }
        
        if (trimmed.contains("id=")) {
            val queryParts = trimmed.split("id=")
            if (queryParts.size > 1) {
                return queryParts[1].split("&")[0].split("#")[0].trim()
            }
        }
        
        return null
    }

    fun sanitizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        
        // Find if there is a http:// or https:// in the string
        val httpIndex = trimmed.indexOf("http://", ignoreCase = true)
        val httpsIndex = trimmed.indexOf("https://", ignoreCase = true)
        
        val startIndex = when {
            httpIndex >= 0 && httpsIndex >= 0 -> minOf(httpIndex, httpsIndex)
            httpIndex >= 0 -> httpIndex
            httpsIndex >= 0 -> httpsIndex
            else -> return trimmed // No URL found, return as is
        }
        
        // Extract from startIndex to the end or to the next whitespace
        val substring = trimmed.substring(startIndex)
        val spaceIndex = substring.indexOfAny(charArrayOf(' ', '\t', '\n', '\r'))
        return if (spaceIndex >= 0) {
            substring.substring(0, spaceIndex).trim()
        } else {
            substring.trim()
        }
    }

    fun getGoogleDriveImageUrl(url: String): String {
        val id = extractDriveId(url)
        return if (id != null) {
            "https://lh3.googleusercontent.com/d/$id"
        } else {
            url
        }
    }

    fun getBookCoverUrl(url: String?): String {
        val trimmed = url?.trim() ?: ""
        val resolved = if (trimmed.isEmpty()) {
            "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=600"
        } else {
            trimmed
        }
        return getGoogleDriveImageUrl(resolved)
    }

    fun getGoogleDrivePreviewUrl(url: String): String {
        val id = extractDriveId(url)
        return if (id != null) {
            "https://drive.google.com/file/d/$id/preview"
        } else {
            url
        }
    }
}
