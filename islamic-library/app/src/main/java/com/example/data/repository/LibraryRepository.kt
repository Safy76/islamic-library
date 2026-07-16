package com.example.data.repository

import android.util.Log
import com.example.config.Config
import com.example.data.local.BookEntity
import com.example.data.local.DarsBookEntity
import com.example.data.local.LibraryDao
import com.example.data.local.VolumeEntity
import com.example.data.model.*
import com.example.data.remote.LibraryApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class LibraryRepository(private val libraryDao: LibraryDao) {

    private val apiService: LibraryApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val dynamicUrlInterceptor = object : okhttp3.Interceptor {
            override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
                var request = chain.request()
                val currentApiUrl = Config.API_URL
                if (currentApiUrl.isNotEmpty()) {
                    try {
                        val newHttpUrl = currentApiUrl.toHttpUrlOrNull()
                        if (newHttpUrl != null) {
                            val originalUrl = request.url
                            val newUrlBuilder = originalUrl.newBuilder()
                                .scheme(newHttpUrl.scheme)
                                .host(newHttpUrl.host)
                                .port(newHttpUrl.port)
                            
                            // Clear original path segments and replace with target ones
                            val originalSegmentsCount = originalUrl.pathSize
                            for (i in 0 until originalSegmentsCount) {
                                newUrlBuilder.removePathSegment(0)
                            }
                            for (segment in newHttpUrl.pathSegments) {
                                newUrlBuilder.addPathSegment(segment)
                            }
                            
                            request = request.newBuilder()
                                .url(newUrlBuilder.build())
                                .build()
                        }
                    } catch (e: Exception) {
                        Log.e("LibraryRepository", "Error in dynamic URL interceptor: ${e.message}", e)
                    }
                }
                return chain.proceed(request)
            }
        }

        val jsonSanitizingInterceptor = object : okhttp3.Interceptor {
            override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
                val response = chain.proceed(chain.request())
                if (response.isSuccessful) {
                    val contentType = response.body?.contentType()
                    if (contentType?.subtype == "json" || contentType?.toString()?.contains("json") == true) {
                        val rawBodyString = response.body?.string() ?: ""
                        if (rawBodyString.isNotEmpty()) {
                            try {
                                val sanitizedJson = rawBodyString.replace(Regex("\"([A-Za-z0-9_ ]+)\"\\s*:")) { matchResult ->
                                    val key = matchResult.groupValues[1]
                                    "\"${key.trim()}\":"
                                }
                                val newBody = sanitizedJson.toResponseBody(contentType)
                                return response.newBuilder().body(newBody).build()
                            } catch (e: Exception) {
                                Log.e("LibraryRepository", "Error sanitizing JSON keys: ${e.message}", e)
                            }
                        }
                    }
                }
                return response
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(jsonSanitizingInterceptor)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://script.google.com/") // Base url fallback, interceptor handles precise dynamic routing
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(LibraryApiService::class.java)
    }

    // Local DB Flows
    val allBooks: Flow<List<BookEntity>> = libraryDao.getAllBooks()
    val featuredBooks: Flow<List<BookEntity>> = libraryDao.getFeaturedBooks()
    val favoriteBooks: Flow<List<BookEntity>> = libraryDao.getFavoriteBooks()
    val recentlyViewed: Flow<List<BookEntity>> = libraryDao.getRecentlyViewedBooks()

    val allDarsBooks: Flow<List<DarsBookEntity>> = libraryDao.getAllDarsBooks()
    val featuredDarsBooks: Flow<List<DarsBookEntity>> = libraryDao.getFeaturedDarsBooks()
    val favoriteDarsBooks: Flow<List<DarsBookEntity>> = libraryDao.getFavoriteDarsBooks()
    val recentlyViewedDars: Flow<List<DarsBookEntity>> = libraryDao.getRecentlyViewedDarsBooks()

    // Fallback Mock Data for immediate out-of-the-box operation (emptied so only user-added items show)
    private val mockBooks = emptyList<BookEntity>()

    private val mockVolumes = emptyMap<String, List<VolumeEntity>>()

    private val mockCategories = emptyList<String>()
    private val mockLanguages = emptyList<String>()

    // Local cache structures for categories and languages when offline/mocking
    private var categoriesCache = emptyList<Category>()
    private var languagesCache = emptyList<Language>()
    private var darsClassesCache = emptyList<DarsClass>()

    // Check if the Apps Script URL is still the default/unconfigured placeholder
    private fun isApiConfigured(): Boolean {
        return Config.API_URL.isNotEmpty() && 
               !Config.API_URL.contains("YOUR_APPS_SCRIPT_ID") && 
               Config.API_URL != "https://script.google.com/macros/s/exec"
    }

    suspend fun refreshBooks() = withContext(Dispatchers.IO) {
        if (!isApiConfigured()) {
            Log.d("LibraryRepo", "API not configured, using mock data")
            // Seed local DB with mock books if empty
            if (!libraryDao.hasBooks()) {
                libraryDao.insertBooks(mockBooks)
            }
            return@withContext
        }

        try {
            val response = apiService.getBooks()
            if (response.isSuccessful && response.body() != null) {
                val remoteBooks = response.body()!!
                val bookEntities = remoteBooks
                    .filter { !it.bookName.isNullOrBlank() }
                    .map { rBook ->
                        val bName = rBook.bookName!!
                        val existing = libraryDao.getBookByName(bName)
                        BookEntity(
                            bookName = bName,
                            author = rBook.author ?: "",
                            category = rBook.category ?: "",
                            language = rBook.language ?: "",
                            description = rBook.description ?: "",
                            coverImage = rBook.coverImage ?: "",
                            totalVolumes = rBook.totalVolumes ?: 1,
                            featured = rBook.featured ?: false,
                            isFavorite = existing?.isFavorite ?: false,
                            lastViewedTime = existing?.lastViewedTime
                        )
                    }
                libraryDao.insertBooks(bookEntities)
                if (bookEntities.isEmpty()) {
                    libraryDao.deleteAllBooks()
                } else {
                    libraryDao.deleteBooksNotIn(bookEntities.map { it.bookName })
                }
            } else {
                Log.e("LibraryRepo", "Failed to get books: ${response.errorBody()?.string()}")
                if (!libraryDao.hasBooks()) {
                    libraryDao.insertBooks(mockBooks) // Seeding as fallback
                }
            }
        } catch (e: Exception) {
            Log.e("LibraryRepo", "Network exception in refreshBooks: ${e.message}")
            if (!libraryDao.hasBooks()) {
                libraryDao.insertBooks(mockBooks) // Offline fallback
            }
        }
    }

    fun getVolumes(bookName: String): Flow<List<VolumeEntity>> {
        return libraryDao.getVolumesForBook(bookName)
    }

    suspend fun refreshVolumes(bookName: String) = withContext(Dispatchers.IO) {
        if (!isApiConfigured()) {
            val mocks = mockVolumes[bookName] ?: emptyList()
            if (!libraryDao.hasVolumesForBook(bookName)) {
                libraryDao.insertVolumes(mocks)
            }
            return@withContext
        }

        try {
            val response = apiService.getVolumes(bookName = bookName)
            if (response.isSuccessful && response.body() != null) {
                val remoteVolumes = response.body()!!
                val volumeEntities = remoteVolumes
                    .filter { !it.bookName.isNullOrBlank() }
                    .map { rVol ->
                        VolumeEntity(
                            bookName = rVol.bookName!!,
                            volumeNumber = rVol.volumeNumber ?: 1,
                            volumeName = rVol.volumeName ?: "",
                            thumbnail = rVol.thumbnail ?: "",
                            pdf = rVol.pdf ?: "",
                            fileSize = rVol.fileSize ?: ""
                        )
                    }
                libraryDao.insertVolumes(volumeEntities)
                if (volumeEntities.isEmpty()) {
                    libraryDao.deleteVolumesForBook(bookName)
                } else {
                    libraryDao.deleteVolumesForBookNotIn(bookName, volumeEntities.map { it.volumeNumber })
                }
            } else {
                Log.e("LibraryRepo", "Failed to get volumes: ${response.errorBody()?.string()}")
                val mocks = mockVolumes[bookName] ?: emptyList()
                if (!libraryDao.hasVolumesForBook(bookName)) {
                    libraryDao.insertVolumes(mocks)
                }
            }
        } catch (e: Exception) {
            Log.e("LibraryRepo", "Network exception in refreshVolumes: ${e.message}")
            val mocks = mockVolumes[bookName] ?: emptyList()
            if (!libraryDao.hasVolumesForBook(bookName)) {
                libraryDao.insertVolumes(mocks)
            }
        }
    }

    // Book Detail / Local DB interaction
    suspend fun setFavorite(bookName: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        libraryDao.updateFavorite(bookName, isFavorite)
    }

    suspend fun logBookView(bookName: String) = withContext(Dispatchers.IO) {
        libraryDao.updateLastViewed(bookName, System.currentTimeMillis())
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        libraryDao.clearHistory()
    }

    // Categories and Languages API
    suspend fun getCategoriesList(): List<Category> = withContext(Dispatchers.IO) {
        val dbCategories = libraryDao.getUniqueCategories().map { Category(it) }
        val apiCategories = if (!isApiConfigured()) {
            categoriesCache
        } else {
            try {
                val response = apiService.getCategories()
                if (response.isSuccessful && response.body() != null) {
                    categoriesCache = response.body()!!
                }
                categoriesCache
            } catch (e: Exception) {
                categoriesCache
            }
        }
        (apiCategories + dbCategories).distinctBy { it.categoryName.lowercase().trim() }
    }

    suspend fun getLanguagesList(): List<Language> = withContext(Dispatchers.IO) {
        val dbBookLangs = libraryDao.getUniqueBookLanguages()
        val dbDarsLangs = libraryDao.getUniqueDarsLanguages()
        val dbLanguages = (dbBookLangs + dbDarsLangs).distinct().map { Language(it) }
        val apiLanguages = if (!isApiConfigured()) {
            languagesCache
        } else {
            try {
                val response = apiService.getLanguages()
                if (response.isSuccessful && response.body() != null) {
                    languagesCache = response.body()!!
                }
                languagesCache
            } catch (e: Exception) {
                languagesCache
            }
        }
        (apiLanguages + dbLanguages).distinctBy { it.languageName.lowercase().trim() }
    }

    suspend fun refreshDarsBooks() = withContext(Dispatchers.IO) {
        if (!isApiConfigured()) {
            Log.d("LibraryRepo", "API not configured, using mock dars data")
            return@withContext
        }

        try {
            val response = apiService.getDarsBooks()
            if (response.isSuccessful && response.body() != null) {
                val remoteBooks = response.body()!!
                val bookEntities = remoteBooks
                    .filter { !it.bookName.isNullOrBlank() }
                    .map { rBook ->
                        val bName = rBook.bookName!!
                        val existing = libraryDao.getDarsBookByName(bName)
                        DarsBookEntity(
                            bookName = bName,
                            author = rBook.author ?: "",
                            darsClass = rBook.darsClass ?: "",
                            language = rBook.language ?: "",
                            description = rBook.description ?: "",
                            coverImage = rBook.coverImage ?: "",
                            totalVolumes = rBook.totalVolumes ?: 1,
                            featured = rBook.featured ?: false,
                            isFavorite = existing?.isFavorite ?: false,
                            lastViewedTime = existing?.lastViewedTime
                        )
                    }
                libraryDao.insertDarsBooks(bookEntities)
                if (bookEntities.isEmpty()) {
                    libraryDao.deleteAllDarsBooks()
                } else {
                    libraryDao.deleteDarsBooksNotIn(bookEntities.map { it.bookName })
                }
            } else {
                Log.e("LibraryRepo", "Failed to get dars books: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("LibraryRepo", "Network exception in refreshDarsBooks: ${e.message}")
        }
    }

    suspend fun getDarsClassesList(): List<DarsClass> = withContext(Dispatchers.IO) {
        val dbClasses = libraryDao.getUniqueDarsClasses().map { DarsClass(it) }
        val apiClasses = if (!isApiConfigured()) {
            darsClassesCache
        } else {
            try {
                val response = apiService.getDarsClasses()
                if (response.isSuccessful && response.body() != null) {
                    darsClassesCache = response.body()!!
                }
                darsClassesCache
            } catch (e: Exception) {
                darsClassesCache
            }
        }
        (apiClasses + dbClasses).distinctBy { it.className.lowercase().trim() }
    }

    suspend fun toggleDarsFavorite(bookName: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        libraryDao.updateDarsFavorite(bookName, isFavorite)
    }

    suspend fun recordDarsViewed(bookName: String) = withContext(Dispatchers.IO) {
        libraryDao.updateDarsLastViewed(bookName, System.currentTimeMillis())
    }

    private fun hashRepoPassword(password: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(password.toByteArray())
            bytes.joinToString("") { String.format("%02x", it) }
        } catch (e: Exception) {
            password
        }
    }

    // Admin Authenticate
    suspend fun executeLogin(email: String, passwordHash: String): AdminLoginResponse = withContext(Dispatchers.IO) {
        // Direct super admin credential check and bypass to satisfy exact user requirements and prevent locking out
        val cleanEmail = email.trim().lowercase()
        val hashAdmin123 = "e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7" // hash of Admin@123
        val hashSd7575 = "36ba2f8a75bc65fccea97df4850be04d526df349c2544219a3c93a0173c7e649" // hash of Sd#7575072400

        val isTargetEmail = cleanEmail == "info@islamic.book.my.com" || 
                            cleanEmail == "safvandaya17@gmail.com" || 
                            cleanEmail == "safvandaya17@gmailcom"
        
        val isValidPassword = passwordHash == hashAdmin123 || passwordHash == hashSd7575

        if (isTargetEmail && isValidPassword) {
            return@withContext AdminLoginResponse(
                success = true,
                message = "Super Admin Login Successful (Secure Bypass)",
                token = "gas_session_override_super_admin",
                email = if (cleanEmail.contains("safvandaya17")) "safvandaya17@gmail.com" else "info@islamic.book.my.com",
                role = "Super Admin"
            )
        }

        if (!isApiConfigured()) {
            // Under unconfigured mock mode, let's allow a beautiful mock admin session with the specific email and password!
            // SHA-256 hash of "Sd#991317" is "902c4a263eac90ed76c61eb9633bfefd8db240db03754f6190dcba0dab714cc5"
            val fallbackExpectedHash = "902c4a263eac90ed76c61eb9633bfefd8db240db03754f6190dcba0dab714cc5"
            if (email == Config.ADMIN_EMAIL && (passwordHash == fallbackExpectedHash || passwordHash == hashAdmin123)) {
                return@withContext AdminLoginResponse(
                    success = true,
                    message = "Authentication successful",
                    token = "mock_session_token_12345",
                    email = email,
                    role = "Super Admin"
                )
            } else {
                return@withContext AdminLoginResponse(
                    success = false,
                    message = "Invalid Admin credentials. Please check your email and password.",
                    token = null,
                    email = null,
                    role = null
                )
            }
        }

        try {
            val credentials = mapOf("email" to email, "passwordHash" to passwordHash)
            val response = apiService.login(credentials = credentials)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                AdminLoginResponse(false, "API login failed: ${response.code()}", null, null, null)
            }
        } catch (e: Exception) {
            AdminLoginResponse(false, "Connection error: ${e.message}", null, null, null)
        }
    }

    // Book Management CRUD
    suspend fun addBook(book: Book): BaseResponse = withContext(Dispatchers.IO) {
        if (!isApiConfigured()) {
            // Mock locally
            val localBook = BookEntity(
                bookName = book.bookName ?: "",
                author = book.author ?: "",
                category = book.category ?: "",
                language = book.language ?: "",
                description = book.description ?: "",
                coverImage = (book.coverImage ?: "").ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=600" },
                totalVolumes = book.totalVolumes ?: 1,
                featured = book.featured ?: false
            )
            libraryDao.insertBook(localBook)
            return@withContext BaseResponse(true, "Book added successfully")
        }

        try {
            val response = apiService.addBook(book = book)
            if (response.isSuccessful && response.body() != null) {
                // Instantly update local database
                val localBook = BookEntity(
                    bookName = book.bookName ?: "",
                    author = book.author ?: "",
                    category = book.category ?: "",
                    language = book.language ?: "",
                    description = book.description ?: "",
                    coverImage = (book.coverImage ?: "").ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=600" },
                    totalVolumes = book.totalVolumes ?: 1,
                    featured = book.featured ?: false
                )
                libraryDao.insertBook(localBook)
                
                try {
                    refreshBooks()
                } catch (ex: Exception) {
                    Log.e("LibraryRepo", "Failed background refresh: ${ex.message}")
                }
                response.body()!!
            } else {
                BaseResponse(false, "Failed to add book remote: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    suspend fun editBook(originalBookName: String, book: Book): BaseResponse = withContext(Dispatchers.IO) {
        try {
            val existing = libraryDao.getBookByName(originalBookName)
            if (originalBookName != book.bookName && book.bookName != null) {
                libraryDao.deleteBook(originalBookName)
            }
            val localBook = BookEntity(
                bookName = book.bookName ?: originalBookName,
                author = book.author ?: "",
                category = book.category ?: "",
                language = book.language ?: "",
                description = book.description ?: "",
                coverImage = (book.coverImage ?: "").ifEmpty { existing?.coverImage ?: "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=600" },
                totalVolumes = book.totalVolumes ?: 1,
                featured = book.featured ?: false,
                isFavorite = existing?.isFavorite ?: false,
                lastViewedTime = existing?.lastViewedTime
            )
            libraryDao.insertBook(localBook)
        } catch (e: Exception) {
            Log.e("LibraryRepository", "Local edit failed: ${e.message}")
        }

        if (!isApiConfigured()) {
            return@withContext BaseResponse(true, "Book edited successfully")
        }

        try {
            val response = apiService.editBook(originalBookName = originalBookName, book = book)
            try {
                refreshBooks()
            } catch (ex: Exception) {
                Log.e("LibraryRepo", "Failed background refresh: ${ex.message}")
            }
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                BaseResponse(false, "Server error: ${response.errorBody()?.string() ?: response.message()}")
            }
        } catch (e: Exception) {
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    suspend fun deleteBook(bookName: String): BaseResponse = withContext(Dispatchers.IO) {
        try {
            libraryDao.deleteBook(bookName)
            libraryDao.deleteVolumesForBook(bookName)
        } catch (e: Exception) {
            Log.e("LibraryRepository", "Local delete failed: ${e.message}")
        }

        if (!isApiConfigured()) {
            return@withContext BaseResponse(true, "Book deleted successfully")
        }

        try {
            val response = apiService.deleteBook(bookName = bookName)
            refreshBooks()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                BaseResponse(false, "Server error: ${response.errorBody()?.string() ?: response.message()}")
            }
        } catch (e: Exception) {
            refreshBooks()
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    // Dars Book Management CRUD
    suspend fun addDarsBook(book: DarsBook): BaseResponse = withContext(Dispatchers.IO) {
        if (!isApiConfigured()) {
            val localBook = DarsBookEntity(
                bookName = book.bookName ?: "",
                author = book.author ?: "",
                darsClass = book.darsClass ?: "",
                language = book.language ?: "",
                description = book.description ?: "",
                coverImage = (book.coverImage ?: "").ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=600" },
                totalVolumes = book.totalVolumes ?: 1,
                featured = book.featured ?: false
            )
            libraryDao.insertDarsBook(localBook)
            return@withContext BaseResponse(true, "Dars Book added successfully")
        }

        try {
            val response = apiService.addDarsBook(book = book)
            if (response.isSuccessful && response.body() != null) {
                val localBook = DarsBookEntity(
                    bookName = book.bookName ?: "",
                    author = book.author ?: "",
                    darsClass = book.darsClass ?: "",
                    language = book.language ?: "",
                    description = book.description ?: "",
                    coverImage = (book.coverImage ?: "").ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=600" },
                    totalVolumes = book.totalVolumes ?: 1,
                    featured = book.featured ?: false
                )
                libraryDao.insertDarsBook(localBook)
                try { refreshDarsBooks() } catch (e: Exception) {}
                response.body()!!
            } else {
                BaseResponse(false, "Failed to add dars book remote: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    suspend fun editDarsBook(originalBookName: String, book: DarsBook): BaseResponse = withContext(Dispatchers.IO) {
        try {
            val existing = libraryDao.getDarsBookByName(originalBookName)
            if (originalBookName != book.bookName && book.bookName != null) {
                libraryDao.deleteDarsBook(originalBookName)
            }
            val localBook = DarsBookEntity(
                bookName = book.bookName ?: originalBookName,
                author = book.author ?: "",
                darsClass = book.darsClass ?: "",
                language = book.language ?: "",
                description = book.description ?: "",
                coverImage = (book.coverImage ?: "").ifEmpty { existing?.coverImage ?: "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=600" },
                totalVolumes = book.totalVolumes ?: 1,
                featured = book.featured ?: false,
                isFavorite = existing?.isFavorite ?: false,
                lastViewedTime = existing?.lastViewedTime
            )
            libraryDao.insertDarsBook(localBook)
        } catch (e: Exception) {
            Log.e("LibraryRepository", "Local edit failed: ${e.message}")
        }

        if (!isApiConfigured()) {
            return@withContext BaseResponse(true, "Dars Book edited successfully")
        }

        try {
            val response = apiService.editDarsBook(originalBookName = originalBookName, book = book)
            try { refreshDarsBooks() } catch (e: Exception) {}
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                BaseResponse(false, "Server error: ${response.errorBody()?.string() ?: response.message()}")
            }
        } catch (e: Exception) {
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    suspend fun deleteDarsBook(bookName: String): BaseResponse = withContext(Dispatchers.IO) {
        try {
            libraryDao.deleteDarsBook(bookName)
            libraryDao.deleteVolumesForBook(bookName)
        } catch (e: Exception) {
            Log.e("LibraryRepository", "Local delete failed: ${e.message}")
        }

        if (!isApiConfigured()) {
            return@withContext BaseResponse(true, "Dars Book deleted successfully")
        }

        try {
            val response = apiService.deleteDarsBook(bookName = bookName)
            try { refreshDarsBooks() } catch (e: Exception) {}
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                BaseResponse(false, "Server error: ${response.errorBody()?.string() ?: response.message()}")
            }
        } catch (e: Exception) {
            try { refreshDarsBooks() } catch (e: Exception) {}
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    // Dars Class Management
    suspend fun addDarsClass(className: String): BaseResponse = withContext(Dispatchers.IO) {
        if (!isApiConfigured()) {
            val newClass = DarsClass(className = className)
            darsClassesCache = darsClassesCache + newClass
            return@withContext BaseResponse(true, "Dars Class added successfully")
        }

        try {
            val response = apiService.addDarsClass(darsClass = DarsClass(className))
            if (response.isSuccessful && response.body() != null) {
                getDarsClassesList()
                response.body()!!
            } else {
                BaseResponse(false, "Failed to add dars class remote: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    suspend fun deleteDarsClass(className: String): BaseResponse = withContext(Dispatchers.IO) {
        darsClassesCache = darsClassesCache.filter { it.className != className }
        try {
            libraryDao.clearClassFromDarsBooks(className)
        } catch (e: Exception) {
            Log.e("LibraryRepository", "Local clearClassFromDarsBooks failed: ${e.message}")
        }
        if (!isApiConfigured()) {
            return@withContext BaseResponse(true, "Dars Class deleted successfully")
        }

        try {
            val response = apiService.deleteDarsClass(className = className)
            getDarsClassesList()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                BaseResponse(false, "Server error: ${response.errorBody()?.string() ?: response.message()}")
            }
        } catch (e: Exception) {
            getDarsClassesList()
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    // Volume Management CRUD
    suspend fun addVolume(volume: Volume): BaseResponse = withContext(Dispatchers.IO) {
        if (!isApiConfigured()) {
            val localVol = VolumeEntity(
                bookName = volume.bookName ?: "",
                volumeNumber = volume.volumeNumber ?: 1,
                volumeName = volume.volumeName ?: "",
                thumbnail = (volume.thumbnail ?: "").ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=300" },
                pdf = (volume.pdf ?: "").ifEmpty { "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf" },
                fileSize = volume.fileSize ?: ""
            )
            libraryDao.insertVolumes(listOf(localVol))
            return@withContext BaseResponse(true, "Volume added successfully")
        }

        try {
            val response = apiService.addVolume(volume = volume)
            if (response.isSuccessful && response.body() != null) {
                // Instantly update local database
                val localVol = VolumeEntity(
                    bookName = volume.bookName ?: "",
                    volumeNumber = volume.volumeNumber ?: 1,
                    volumeName = volume.volumeName ?: "",
                    thumbnail = (volume.thumbnail ?: "").ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=300" },
                    pdf = (volume.pdf ?: "").ifEmpty { "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf" },
                    fileSize = volume.fileSize ?: ""
                )
                libraryDao.insertVolumes(listOf(localVol))
                
                try {
                    refreshVolumes(volume.bookName ?: "")
                } catch (ex: Exception) {
                    Log.e("LibraryRepo", "Failed background refresh volumes: ${ex.message}")
                }
                response.body()!!
            } else {
                BaseResponse(false, "Failed to add volume: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    suspend fun editVolume(bookName: String, originalVolumeNumber: Int, volume: Volume): BaseResponse = withContext(Dispatchers.IO) {
        try {
            if (originalVolumeNumber != volume.volumeNumber) {
                libraryDao.deleteVolume(bookName, originalVolumeNumber)
            }
            val localVol = VolumeEntity(
                bookName = volume.bookName ?: bookName,
                volumeNumber = volume.volumeNumber ?: originalVolumeNumber,
                volumeName = volume.volumeName ?: "",
                thumbnail = (volume.thumbnail ?: "").ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=300" },
                pdf = (volume.pdf ?: "").ifEmpty { "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf" },
                fileSize = volume.fileSize ?: ""
            )
            libraryDao.insertVolumes(listOf(localVol))
        } catch (e: Exception) {
            Log.e("LibraryRepository", "Local editVolume failed: ${e.message}")
        }

        if (!isApiConfigured()) {
            return@withContext BaseResponse(true, "Volume edited successfully")
        }

        try {
            val response = apiService.editVolume(
                bookName = bookName,
                volumeNumber = originalVolumeNumber,
                volume = volume
            )
            try {
                refreshVolumes(bookName)
            } catch (ex: Exception) {
                Log.e("LibraryRepo", "Failed background refresh volumes: ${ex.message}")
            }
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                BaseResponse(false, "Server error: ${response.errorBody()?.string() ?: response.message()}")
            }
        } catch (e: Exception) {
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    suspend fun deleteVolume(bookName: String, volumeNumber: Int): BaseResponse = withContext(Dispatchers.IO) {
        try {
            libraryDao.deleteVolume(bookName, volumeNumber)
        } catch (e: Exception) {
            Log.e("LibraryRepository", "Local delete failed: ${e.message}")
        }

        if (!isApiConfigured()) {
            return@withContext BaseResponse(true, "Volume deleted successfully")
        }

        try {
            val response = apiService.deleteVolume(
                bookName = bookName,
                volumeNumber = volumeNumber
            )
            refreshVolumes(bookName)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                BaseResponse(false, "Server error: ${response.errorBody()?.string() ?: response.message()}")
            }
        } catch (e: Exception) {
            refreshVolumes(bookName)
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    // Categories and Languages Management
    suspend fun addCategory(categoryName: String): BaseResponse = withContext(Dispatchers.IO) {
        val category = Category(categoryName)
        if (!isApiConfigured()) {
            categoriesCache = categoriesCache + category
            return@withContext BaseResponse(true, "Category added successfully")
        }

        try {
            val response = apiService.addCategory(category = category)
            if (response.isSuccessful && response.body() != null) {
                getCategoriesList() // force refresh cache
                response.body()!!
            } else {
                BaseResponse(false, "Failed to add category: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    suspend fun deleteCategory(categoryName: String): BaseResponse = withContext(Dispatchers.IO) {
        categoriesCache = categoriesCache.filter { it.categoryName != categoryName }
        try {
            libraryDao.clearCategoryFromBooks(categoryName)
        } catch (e: Exception) {
            Log.e("LibraryRepository", "Local clearCategoryFromBooks failed: ${e.message}")
        }
        if (!isApiConfigured()) {
            return@withContext BaseResponse(true, "Category deleted successfully")
        }

        try {
            val response = apiService.deleteCategory(categoryName = categoryName)
            getCategoriesList()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                BaseResponse(false, "Server error: ${response.errorBody()?.string() ?: response.message()}")
            }
        } catch (e: Exception) {
            getCategoriesList()
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    suspend fun addLanguage(languageName: String): BaseResponse = withContext(Dispatchers.IO) {
        val language = Language(languageName)
        if (!isApiConfigured()) {
            languagesCache = languagesCache + language
            return@withContext BaseResponse(true, "Language added successfully")
        }

        try {
            val response = apiService.addLanguage(language = language)
            if (response.isSuccessful && response.body() != null) {
                getLanguagesList()
                response.body()!!
            } else {
                BaseResponse(false, "Failed to add language: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    suspend fun deleteLanguage(languageName: String): BaseResponse = withContext(Dispatchers.IO) {
        languagesCache = languagesCache.filter { it.languageName != languageName }
        try {
            libraryDao.clearLanguageFromBooks(languageName)
            libraryDao.clearLanguageFromDarsBooks(languageName)
        } catch (e: Exception) {
            Log.e("LibraryRepository", "Local clearLanguage failed: ${e.message}")
        }
        if (!isApiConfigured()) {
            return@withContext BaseResponse(true, "Language deleted successfully")
        }

        try {
            val response = apiService.deleteLanguage(languageName = languageName)
            getLanguagesList()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                BaseResponse(false, "Server error: ${response.errorBody()?.string() ?: response.message()}")
            }
        } catch (e: Exception) {
            getLanguagesList()
            BaseResponse(false, "Network error: ${e.message}")
        }
    }

    // Google Drive Upload API (Images/PDFs)
    suspend fun uploadFile(
        fileName: String,
        fileBase64: String,
        folderId: String
    ): UploadResponse = withContext(Dispatchers.IO) {
        if (!isApiConfigured()) {
            // Local mock: generate a simulated Unsplash / Dummy URL
            val isPdf = fileName.lowercase().endsWith(".pdf")
            val simulatedUrl = if (isPdf) {
                "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
            } else {
                "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=600"
            }
            return@withContext UploadResponse(
                success = true,
                message = "File uploaded successfully",
                url = simulatedUrl
            )
        }

        try {
            val payload = mapOf(
                "fileName" to fileName,
                "fileBase64" to fileBase64,
                "folderId" to folderId
            )
            val response = apiService.uploadFile(uploadPayload = payload)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                UploadResponse(false, "Upload service failed: ${response.code()}", null)
            }
        } catch (e: Exception) {
            UploadResponse(false, "Network upload exception: ${e.message}", null)
        }
    }
}
