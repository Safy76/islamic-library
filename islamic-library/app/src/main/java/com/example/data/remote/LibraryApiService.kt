package com.example.data.remote

import com.example.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface LibraryApiService {

    @POST(".")
    suspend fun login(
        @Query("action") action: String = "login",
        @Body credentials: Map<String, String>
    ): Response<AdminLoginResponse>

    @GET(".")
    suspend fun getBooks(
        @Query("action") action: String = "getBooks"
    ): Response<List<Book>>

    @GET(".")
    suspend fun getVolumes(
        @Query("action") action: String = "getVolumes",
        @Query("bookName") bookName: String
    ): Response<List<Volume>>

    @GET(".")
    suspend fun getCategories(
        @Query("action") action: String = "getCategories"
    ): Response<List<Category>>

    @GET(".")
    suspend fun getLanguages(
        @Query("action") action: String = "getLanguages"
    ): Response<List<Language>>

    @GET(".")
    suspend fun getDarsBooks(
        @Query("action") action: String = "getDarsBooks"
    ): Response<List<DarsBook>>

    @GET(".")
    suspend fun getDarsClasses(
        @Query("action") action: String = "getDarsClasses"
    ): Response<List<DarsClass>>

    // Book Management
    @POST(".")
    suspend fun addBook(
        @Query("action") action: String = "addBook",
        @Body book: Book
    ): Response<BaseResponse>

    @POST(".")
    suspend fun editBook(
        @Query("action") action: String = "editBook",
        @Query("bookName") originalBookName: String,
        @Body book: Book
    ): Response<BaseResponse>

    @POST(".")
    suspend fun deleteBook(
        @Query("action") action: String = "deleteBook",
        @Query("bookName") bookName: String
    ): Response<BaseResponse>

    // Volume Management
    @POST(".")
    suspend fun addVolume(
        @Query("action") action: String = "addVolume",
        @Body volume: Volume
    ): Response<BaseResponse>

    @POST(".")
    suspend fun editVolume(
        @Query("action") action: String = "editVolume",
        @Query("bookName") bookName: String,
        @Query("volumeNumber") volumeNumber: Int,
        @Body volume: Volume
    ): Response<BaseResponse>

    @POST(".")
    suspend fun deleteVolume(
        @Query("action") action: String = "deleteVolume",
        @Query("bookName") bookName: String,
        @Query("volumeNumber") volumeNumber: Int
    ): Response<BaseResponse>

    // Category Management
    @POST(".")
    suspend fun addCategory(
        @Query("action") action: String = "addCategory",
        @Body category: Category
    ): Response<BaseResponse>

    @POST(".")
    suspend fun deleteCategory(
        @Query("action") action: String = "deleteCategory",
        @Query("categoryName") categoryName: String
    ): Response<BaseResponse>

    // Language Management
    @POST(".")
    suspend fun addLanguage(
        @Query("action") action: String = "addLanguage",
        @Body language: Language
    ): Response<BaseResponse>

    @POST(".")
    suspend fun deleteLanguage(
        @Query("action") action: String = "deleteLanguage",
        @Query("languageName") languageName: String
    ): Response<BaseResponse>

    // Dars Book Management
    @POST(".")
    suspend fun addDarsBook(
        @Query("action") action: String = "addDarsBook",
        @Body book: DarsBook
    ): Response<BaseResponse>

    @POST(".")
    suspend fun editDarsBook(
        @Query("action") action: String = "editDarsBook",
        @Query("bookName") originalBookName: String,
        @Body book: DarsBook
    ): Response<BaseResponse>

    @POST(".")
    suspend fun deleteDarsBook(
        @Query("action") action: String = "deleteDarsBook",
        @Query("bookName") bookName: String
    ): Response<BaseResponse>

    // Dars Class Management
    @POST(".")
    suspend fun addDarsClass(
        @Query("action") action: String = "addDarsClass",
        @Body darsClass: DarsClass
    ): Response<BaseResponse>

    @POST(".")
    suspend fun deleteDarsClass(
        @Query("action") action: String = "deleteDarsClass",
        @Query("className") className: String
    ): Response<BaseResponse>

    // Drive Upload (Images and PDFs)
    @POST(".")
    suspend fun uploadFile(
        @Query("action") action: String = "uploadFile",
        @Body uploadPayload: Map<String, String> // contains "fileBase64", "fileName", "folderId"
    ): Response<UploadResponse>
}
