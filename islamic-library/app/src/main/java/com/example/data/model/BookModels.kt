package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Book(
    @Json(name = "BookName") val bookName: String?,
    @Json(name = "Author") val author: String?,
    @Json(name = "Category") val category: String?,
    @Json(name = "Language") val language: String?,
    @Json(name = "Description") val description: String?,
    @Json(name = "CoverImage") val coverImage: String?,
    @Json(name = "TotalVolumes") val totalVolumes: Int?,
    @Json(name = "Featured") val featured: Boolean?,
    @Json(name = "CreatedAt") val createdAt: String? = null,
    @Json(name = "UpdatedAt") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Volume(
    @Json(name = "BookName") val bookName: String?,
    @Json(name = "VolumeNumber") val volumeNumber: Int?,
    @Json(name = "VolumeName") val volumeName: String?,
    @Json(name = "Thumbnail") val thumbnail: String?,
    @Json(name = "PDF") val pdf: String?, // Google Drive Direct View/Download link
    @Json(name = "FileSize") val fileSize: String?,
    @Json(name = "CreatedAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Category(
    @Json(name = "CategoryName") val categoryName: String
)

@JsonClass(generateAdapter = true)
data class Language(
    @Json(name = "LanguageName") val languageName: String
)

@JsonClass(generateAdapter = true)
data class DarsBook(
    @Json(name = "BookName") val bookName: String?,
    @Json(name = "Author") val author: String?,
    @Json(name = "DarsClass") val darsClass: String?,
    @Json(name = "Language") val language: String?,
    @Json(name = "Description") val description: String?,
    @Json(name = "CoverImage") val coverImage: String?,
    @Json(name = "TotalVolumes") val totalVolumes: Int?,
    @Json(name = "Featured") val featured: Boolean?,
    @Json(name = "CreatedAt") val createdAt: String? = null,
    @Json(name = "UpdatedAt") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class DarsClass(
    @Json(name = "ClassName") val className: String
)

@JsonClass(generateAdapter = true)
data class AdminLoginResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "token") val token: String?,
    @Json(name = "email") val email: String?,
    @Json(name = "role") val role: String?
)

@JsonClass(generateAdapter = true)
data class BaseResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class UploadResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "url") val url: String? // Direct Google Drive URL
)
