package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.Config
import com.example.data.local.AppDatabase
import com.example.data.local.BookEntity
import com.example.data.local.VolumeEntity
import com.example.data.local.DarsBookEntity
import com.example.data.model.*
import com.example.data.repository.LibraryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.security.MessageDigest

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("islamic_library_prefs", Context.MODE_PRIVATE)
    
    // Initialize Room & Repository
    private val database: AppDatabase by lazy {
        androidx.room.Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "islamic_library_db"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository: LibraryRepository by lazy {
        LibraryRepository(database.libraryDao())
    }

    // Theme state
    private val _themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
    }

    // Book List State
    private val _booksLoading = MutableStateFlow(false)
    val booksLoading: StateFlow<Boolean> = _booksLoading.asStateFlow()

    private val _darsBooksLoading = MutableStateFlow(false)
    val darsBooksLoading: StateFlow<Boolean> = _darsBooksLoading.asStateFlow()

    private val _booksError = MutableStateFlow<String?>(null)
    val booksError: StateFlow<String?> = _booksError.asStateFlow()

    // Base flows
    val allBooks: StateFlow<List<BookEntity>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDarsBooks: StateFlow<List<DarsBookEntity>> = repository.allDarsBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredDarsBooks: StateFlow<List<DarsBookEntity>> = repository.featuredDarsBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteDarsBooks: StateFlow<List<DarsBookEntity>> = repository.favoriteDarsBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyViewedDars: StateFlow<List<DarsBookEntity>> = repository.recentlyViewedDars
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredBooks: StateFlow<List<BookEntity>> = repository.featuredBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteBooks: StateFlow<List<BookEntity>> = combine(
        repository.favoriteBooks,
        repository.favoriteDarsBooks
    ) { favs, darsFavs ->
        favs + darsFavs.map {
            BookEntity(
                bookName = it.bookName,
                author = it.author,
                category = it.darsClass,
                language = it.language,
                description = it.description,
                coverImage = it.coverImage,
                totalVolumes = it.totalVolumes,
                featured = it.featured,
                isFavorite = it.isFavorite,
                lastViewedTime = it.lastViewedTime
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyViewed: StateFlow<List<BookEntity>> = repository.recentlyViewed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Categories and Languages Lists
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _languages = MutableStateFlow<List<Language>>(emptyList())
    val languages: StateFlow<List<Language>> = _languages.asStateFlow()

    // Active Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

    // Combined filtered list
    val filteredBooks: StateFlow<List<BookEntity>> = combine(
        allBooks, searchQuery, selectedCategory, selectedLanguage
    ) { list, query, category, language ->
        list.filter { book ->
            val matchQuery = query.isEmpty() || 
                    book.bookName.contains(query, ignoreCase = true) || 
                    book.author.contains(query, ignoreCase = true) || 
                    book.description.contains(query, ignoreCase = true)
            
            val matchCategory = category == null || book.category == category
            val matchLanguage = language == null || book.language == language

            matchQuery && matchCategory && matchLanguage
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _darsClasses = MutableStateFlow<List<DarsClass>>(emptyList())
    val darsClasses: StateFlow<List<DarsClass>> = _darsClasses.asStateFlow()

    private val _selectedDarsClass = MutableStateFlow<String?>(null)
    val selectedDarsClass: StateFlow<String?> = _selectedDarsClass.asStateFlow()

    private val _selectedDarsLanguage = MutableStateFlow<String?>(null)
    val selectedDarsLanguage: StateFlow<String?> = _selectedDarsLanguage.asStateFlow()

    val filteredDarsBooks: StateFlow<List<DarsBookEntity>> = combine(
        allDarsBooks, searchQuery, _selectedDarsClass, _selectedDarsLanguage
    ) { list, query, darsClass, language ->
        list.filter { book ->
            val matchQuery = query.isEmpty() || 
                    book.bookName.contains(query, ignoreCase = true) || 
                    book.author.contains(query, ignoreCase = true) || 
                    book.description.contains(query, ignoreCase = true)
            
            val matchClass = darsClass == null || book.darsClass == darsClass
            val matchLanguage = language == null || book.language == language

            matchQuery && matchClass && matchLanguage
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDarsClass(darsClass: String?) {
        _selectedDarsClass.value = darsClass
    }

    fun selectDarsLanguage(language: String?) {
        _selectedDarsLanguage.value = language
    }

    // Active Book / Volume state
    private val _selectedBookName = MutableStateFlow<String?>(null)
    val selectedBookName: StateFlow<String?> = _selectedBookName.asStateFlow()

    val currentBook: StateFlow<BookEntity?> = _selectedBookName.flatMapLatest { name ->
        if (name == null) flowOf(null)
        else {
            combine(allBooks, allDarsBooks) { books, darsBooks ->
                val b = books.find { it.bookName == name }
                if (b != null) b else {
                    val db = darsBooks.find { it.bookName == name }
                    db?.let {
                        BookEntity(
                            bookName = it.bookName,
                            author = it.author,
                            category = it.darsClass,
                            language = it.language,
                            description = it.description,
                            coverImage = it.coverImage,
                            totalVolumes = it.totalVolumes,
                            featured = it.featured,
                            isFavorite = it.isFavorite,
                            lastViewedTime = it.lastViewedTime
                        )
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentBookVolumes: StateFlow<List<VolumeEntity>> = _selectedBookName.flatMapLatest { name ->
        if (name == null) flowOf(emptyList())
        else repository.getVolumes(name)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _volumesLoading = MutableStateFlow(false)
    val volumesLoading: StateFlow<Boolean> = _volumesLoading.asStateFlow()

    // Bookmarks list
    private val _bookmarkedVolumes = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedVolumes: StateFlow<Set<String>> = _bookmarkedVolumes.asStateFlow()

    // Volume bookmark toggle
    fun toggleVolumeBookmark(volumeKey: String) {
        val current = _bookmarkedVolumes.value.toMutableSet()
        if (current.contains(volumeKey)) {
            current.remove(volumeKey)
        } else {
            current.add(volumeKey)
        }
        _bookmarkedVolumes.value = current
        sharedPrefs.edit().putStringSet("bookmarked_volumes", current).apply()
    }

    // Admin Session State
    private val _adminEmail = MutableStateFlow(sharedPrefs.getString("admin_email", null))
    val adminEmail: StateFlow<String?> = _adminEmail.asStateFlow()

    private val _adminToken = MutableStateFlow(sharedPrefs.getString("admin_token", null))
    val adminToken: StateFlow<String?> = _adminToken.asStateFlow()

    val isAdminLoggedIn: StateFlow<Boolean> = combine(_adminEmail, _adminToken) { email, token ->
        email != null && token != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _adminLoading = MutableStateFlow(false)
    val adminLoading: StateFlow<Boolean> = _adminLoading.asStateFlow()

    private val _adminError = MutableStateFlow<String?>(null)
    val adminError: StateFlow<String?> = _adminError.asStateFlow()

    // Image/PDF Upload state
    private val _uploadProgress = MutableStateFlow<Int?>(null) // null = idle, 0-100 = active
    val uploadProgress: StateFlow<Int?> = _uploadProgress.asStateFlow()

    private val _uploadStatus = MutableStateFlow<String?>(null)
    val uploadStatus: StateFlow<String?> = _uploadStatus.asStateFlow()

    // Dynamic Cloud Configuration States
    val apiConfigUrl = MutableStateFlow(
        sharedPrefs.getString("config_api_url", Config.API_URL)?.let {
            if (it.contains("YOUR_APPS_SCRIPT_ID")) Config.API_URL else it
        } ?: Config.API_URL
    )
    val spreadsheetId = MutableStateFlow(
        sharedPrefs.getString("config_spreadsheet_id", Config.SPREADSHEET_ID)?.let {
            if (it.contains("your-spreadsheet-id")) Config.SPREADSHEET_ID else it
        } ?: Config.SPREADSHEET_ID
    )
    val folderBookCovers = MutableStateFlow(
        sharedPrefs.getString("config_folder_book_covers", Config.FOLDER_BOOK_COVERS)?.let {
            if (it.contains("your-folder-id")) Config.FOLDER_BOOK_COVERS else it
        } ?: Config.FOLDER_BOOK_COVERS
    )
    val folderVolumeThumbnails = MutableStateFlow(
        sharedPrefs.getString("config_folder_volume_thumbnails", Config.FOLDER_VOLUME_THUMBNAILS)?.let {
            if (it.contains("your-folder-id")) Config.FOLDER_VOLUME_THUMBNAILS else it
        } ?: Config.FOLDER_VOLUME_THUMBNAILS
    )
    val folderBookPdfs = MutableStateFlow(
        sharedPrefs.getString("config_folder_book_pdfs", Config.FOLDER_BOOK_PDFS)?.let {
            if (it.contains("your-folder-id")) Config.FOLDER_BOOK_PDFS else it
        } ?: Config.FOLDER_BOOK_PDFS
    )

    fun updateCloudConfig(
        apiUrl: String,
        sheetId: String,
        coversFolder: String,
        thumbnailsFolder: String,
        pdfsFolder: String
    ) {
        val cleanApiUrl = apiUrl.trim()
        val cleanSheetId = sheetId.trim()
        val cleanCovers = coversFolder.trim()
        val cleanThumbnails = thumbnailsFolder.trim()
        val cleanPdfs = pdfsFolder.trim()

        Config.API_URL = cleanApiUrl
        Config.SPREADSHEET_ID = cleanSheetId
        Config.FOLDER_BOOK_COVERS = cleanCovers
        Config.FOLDER_VOLUME_THUMBNAILS = cleanThumbnails
        Config.FOLDER_BOOK_PDFS = cleanPdfs

        sharedPrefs.edit()
            .putString("config_api_url", Config.API_URL)
            .putString("config_spreadsheet_id", Config.SPREADSHEET_ID)
            .putString("config_folder_book_covers", Config.FOLDER_BOOK_COVERS)
            .putString("config_folder_volume_thumbnails", Config.FOLDER_VOLUME_THUMBNAILS)
            .putString("config_folder_book_pdfs", Config.FOLDER_BOOK_PDFS)
            .apply()

        apiConfigUrl.value = Config.API_URL
        spreadsheetId.value = Config.SPREADSHEET_ID
        folderBookCovers.value = Config.FOLDER_BOOK_COVERS
        folderVolumeThumbnails.value = Config.FOLDER_VOLUME_THUMBNAILS
        folderBookPdfs.value = Config.FOLDER_BOOK_PDFS

        // Refresh data using the newly configured API endpoint
        refreshData()
    }

    init {
        // Sync dynamic config variables to Config object on start
        Config.API_URL = apiConfigUrl.value
        Config.SPREADSHEET_ID = spreadsheetId.value
        Config.FOLDER_BOOK_COVERS = folderBookCovers.value
        Config.FOLDER_VOLUME_THUMBNAILS = folderVolumeThumbnails.value
        Config.FOLDER_BOOK_PDFS = folderBookPdfs.value

        // Sync cleaned values back to the StateFlows
        apiConfigUrl.value = Config.API_URL
        spreadsheetId.value = Config.SPREADSHEET_ID
        folderBookCovers.value = Config.FOLDER_BOOK_COVERS
        folderVolumeThumbnails.value = Config.FOLDER_VOLUME_THUMBNAILS
        folderBookPdfs.value = Config.FOLDER_BOOK_PDFS

        // Load stored volume bookmarks
        val saved = sharedPrefs.getStringSet("bookmarked_volumes", emptySet()) ?: emptySet()
        _bookmarkedVolumes.value = saved

        // Initial Data Fetch
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _booksLoading.value = true
            _darsBooksLoading.value = true
            try {
                repository.refreshBooks()
                repository.refreshDarsBooks()
                _categories.value = repository.getCategoriesList()
                _languages.value = repository.getLanguagesList()
                _darsClasses.value = repository.getDarsClassesList()
                _booksError.value = null
            } catch (e: Exception) {
                Log.e("LibraryVM", "Error refreshing books: ${e.message}")
                _booksError.value = "Failed to synchronize library books. Operating offline."
            } finally {
                _booksLoading.value = false
                _darsBooksLoading.value = false
            }
        }
    }

    fun selectBook(bookName: String?) {
        _selectedBookName.value = bookName
        if (bookName != null) {
            viewModelScope.launch {
                _volumesLoading.value = true
                val isDars = allDarsBooks.value.any { it.bookName == bookName }
                if (isDars) {
                    repository.recordDarsViewed(bookName)
                } else {
                    repository.logBookView(bookName)
                }
                try {
                    repository.refreshVolumes(bookName)
                } catch (e: Exception) {
                    Log.e("LibraryVM", "Error refreshing volumes: ${e.message}")
                } finally {
                    _volumesLoading.value = false
                }
            }
        }
    }

    fun setQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun selectLanguage(language: String?) {
        _selectedLanguage.value = language
    }

    fun toggleFavorite(bookName: String, isFavorite: Boolean) {
        viewModelScope.launch {
            val isDars = allDarsBooks.value.any { it.bookName == bookName }
            if (isDars) {
                repository.toggleDarsFavorite(bookName, isFavorite)
            } else {
                repository.setFavorite(bookName, isFavorite)
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // MD5 or SHA-256 for Password Hash
    private fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(password.toByteArray())
            bytes.joinToString("") { String.format("%02x", it) }
        } catch (e: Exception) {
            password // Fallback
        }
    }

    // Admin API Calls
    fun adminLogin(email: String, passwordRaw: String, rememberMe: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            _adminError.value = null
            try {
                val pHash = hashPassword(passwordRaw)
                val response = repository.executeLogin(email, pHash)
                if (response.success && response.token != null) {
                    _adminEmail.value = response.email
                    _adminToken.value = response.token
                    
                    if (rememberMe) {
                        sharedPrefs.edit()
                            .putString("admin_email", response.email)
                            .putString("admin_token", response.token)
                            .apply()
                    }
                    onSuccess()
                } else {
                    _adminError.value = response.message ?: "Authentication failed."
                }
            } catch (e: Exception) {
                _adminError.value = "Network timeout or connection error: ${e.message}"
            } finally {
                _adminLoading.value = false
            }
        }
    }

    fun adminLogout() {
        _adminEmail.value = null
        _adminToken.value = null
        sharedPrefs.edit()
            .remove("admin_email")
            .remove("admin_token")
            .apply()
    }

    // CRUD Book Actions
    fun addBook(book: Book, coverBytes: ByteArray?, coverName: String?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                var coverUrl = book.coverImage
                _uploadProgress.value = 15
                _uploadStatus.value = "Processing book details..."
                if (coverBytes != null && coverName != null) {
                    _uploadProgress.value = 35
                    _uploadStatus.value = "Uploading cover image..."
                    val b64 = Base64.encodeToString(coverBytes, Base64.NO_WRAP)
                    val uploadResp = repository.uploadFile(coverName, b64, Config.FOLDER_BOOK_COVERS)
                    if (uploadResp.success && uploadResp.url != null) {
                        coverUrl = uploadResp.url
                        _uploadProgress.value = 75
                    } else {
                        onResult(false, "Cover upload failed: ${uploadResp.message}")
                        return@launch
                    }
                } else {
                    _uploadProgress.value = 50
                }

                _uploadStatus.value = "Saving book record to database..."
                val finalBook = book.copy(coverImage = coverUrl)
                val resp = repository.addBook(finalBook)
                if (resp.success) {
                    refreshData()
                    _uploadProgress.value = 100
                    _uploadStatus.value = "Saved successfully!"
                    delay(800)
                }
                onResult(resp.success, resp.message ?: "Task completed")
            } catch (e: Exception) {
                onResult(false, "Operation error: ${e.message}")
            } finally {
                _uploadProgress.value = null
                _uploadStatus.value = null
                _adminLoading.value = false
            }
        }
    }

    fun editBook(originalBookName: String, book: Book, coverBytes: ByteArray?, coverName: String?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                var coverUrl = book.coverImage
                _uploadProgress.value = 15
                _uploadStatus.value = "Processing updated book details..."
                if (coverBytes != null && coverName != null) {
                    _uploadProgress.value = 35
                    _uploadStatus.value = "Uploading cover image..."
                    val b64 = Base64.encodeToString(coverBytes, Base64.NO_WRAP)
                    val uploadResp = repository.uploadFile(coverName, b64, Config.FOLDER_BOOK_COVERS)
                    if (uploadResp.success && uploadResp.url != null) {
                        coverUrl = uploadResp.url
                        _uploadProgress.value = 75
                    } else {
                        onResult(false, "Cover upload failed: ${uploadResp.message}")
                        return@launch
                    }
                } else {
                    _uploadProgress.value = 50
                }

                _uploadStatus.value = "Updating book record in database..."
                val finalBook = book.copy(coverImage = coverUrl)
                val resp = repository.editBook(originalBookName, finalBook)
                if (resp.success) {
                    refreshData()
                    _uploadProgress.value = 100
                    _uploadStatus.value = "Updated successfully!"
                    delay(800)
                }
                onResult(resp.success, resp.message ?: "Task completed")
            } catch (e: Exception) {
                onResult(false, "Operation error: ${e.message}")
            } finally {
                _uploadProgress.value = null
                _uploadStatus.value = null
                _adminLoading.value = false
            }
        }
    }

    fun deleteBook(bookName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                val resp = repository.deleteBook(bookName)
                if (resp.success) {
                    refreshData()
                }
                onResult(resp.success, resp.message ?: "Book deleted successfully.")
            } catch (e: Exception) {
                onResult(false, "Failed to delete book: ${e.message}")
            } finally {
                _adminLoading.value = false
            }
        }
    }

    // CRUD Dars Book Actions
    fun addDarsBook(book: DarsBook, coverBytes: ByteArray?, coverName: String?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                var coverUrl = book.coverImage
                _uploadProgress.value = 15
                _uploadStatus.value = "Processing book details..."
                if (coverBytes != null && coverName != null) {
                    _uploadProgress.value = 35
                    _uploadStatus.value = "Uploading cover image..."
                    val b64 = Base64.encodeToString(coverBytes, Base64.NO_WRAP)
                    val uploadResp = repository.uploadFile(coverName, b64, Config.FOLDER_BOOK_COVERS)
                    if (uploadResp.success && uploadResp.url != null) {
                        coverUrl = uploadResp.url
                        _uploadProgress.value = 75
                    } else {
                        onResult(false, "Cover upload failed: ${uploadResp.message}")
                        return@launch
                    }
                } else {
                    _uploadProgress.value = 50
                }

                _uploadStatus.value = "Saving book record to database..."
                val finalBook = book.copy(coverImage = coverUrl)
                val resp = repository.addDarsBook(finalBook)
                if (resp.success) {
                    refreshData()
                    _uploadProgress.value = 100
                    _uploadStatus.value = "Saved successfully!"
                    delay(800)
                }
                onResult(resp.success, resp.message ?: "Task completed")
            } catch (e: Exception) {
                onResult(false, "Operation error: ${e.message}")
            } finally {
                _uploadProgress.value = null
                _uploadStatus.value = null
                _adminLoading.value = false
            }
        }
    }

    fun editDarsBook(originalBookName: String, book: DarsBook, coverBytes: ByteArray?, coverName: String?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                var coverUrl = book.coverImage
                _uploadProgress.value = 15
                _uploadStatus.value = "Processing updated book details..."
                if (coverBytes != null && coverName != null) {
                    _uploadProgress.value = 35
                    _uploadStatus.value = "Uploading cover image..."
                    val b64 = Base64.encodeToString(coverBytes, Base64.NO_WRAP)
                    val uploadResp = repository.uploadFile(coverName, b64, Config.FOLDER_BOOK_COVERS)
                    if (uploadResp.success && uploadResp.url != null) {
                        coverUrl = uploadResp.url
                        _uploadProgress.value = 75
                    } else {
                        onResult(false, "Cover upload failed: ${uploadResp.message}")
                        return@launch
                    }
                } else {
                    _uploadProgress.value = 50
                }

                _uploadStatus.value = "Updating book record in database..."
                val finalBook = book.copy(coverImage = coverUrl)
                val resp = repository.editDarsBook(originalBookName, finalBook)
                if (resp.success) {
                    refreshData()
                    _uploadProgress.value = 100
                    _uploadStatus.value = "Updated successfully!"
                    delay(800)
                }
                onResult(resp.success, resp.message ?: "Task completed")
            } catch (e: Exception) {
                onResult(false, "Operation error: ${e.message}")
            } finally {
                _uploadProgress.value = null
                _uploadStatus.value = null
                _adminLoading.value = false
            }
        }
    }

    fun deleteDarsBook(bookName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                val resp = repository.deleteDarsBook(bookName)
                if (resp.success) {
                    refreshData()
                }
                onResult(resp.success, resp.message ?: "Dars Book deleted successfully.")
            } catch (e: Exception) {
                onResult(false, "Failed to delete dars book: ${e.message}")
            } finally {
                _adminLoading.value = false
            }
        }
    }

    // CRUD Dars Class Actions
    fun addDarsClass(className: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                val resp = repository.addDarsClass(className)
                if (resp.success) {
                    _darsClasses.value = repository.getDarsClassesList()
                }
                onResult(resp.success, resp.message ?: "Dars class added.")
            } catch (e: Exception) {
                onResult(false, "Failed to add dars class: ${e.message}")
            } finally {
                _adminLoading.value = false
            }
        }
    }

    fun deleteDarsClass(className: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                val resp = repository.deleteDarsClass(className)
                if (resp.success) {
                    _darsClasses.value = repository.getDarsClassesList()
                    if (_selectedDarsClass.value == className) {
                        _selectedDarsClass.value = null
                    }
                }
                onResult(resp.success, resp.message ?: "Dars class deleted.")
            } catch (e: Exception) {
                onResult(false, "Failed to delete dars class: ${e.message}")
            } finally {
                _adminLoading.value = false
            }
        }
    }

    // CRUD Volume Actions
    fun addVolume(
        volume: Volume,
        thumbnailBytes: ByteArray?,
        thumbnailName: String?,
        pdfBytes: ByteArray?,
        pdfName: String?,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                var thumbUrl = volume.thumbnail
                var pdfUrl = volume.pdf

                _uploadProgress.value = 15
                _uploadStatus.value = "Processing volume details..."

                if (thumbnailBytes != null && thumbnailName != null) {
                    _uploadProgress.value = 35
                    _uploadStatus.value = "Uploading volume thumbnail..."
                    val b64 = Base64.encodeToString(thumbnailBytes, Base64.NO_WRAP)
                    val uploadResp = repository.uploadFile(thumbnailName, b64, Config.FOLDER_VOLUME_THUMBNAILS)
                    if (uploadResp.success && uploadResp.url != null) {
                        thumbUrl = uploadResp.url
                        _uploadProgress.value = 55
                    } else {
                        onResult(false, "Thumbnail upload failed: ${uploadResp.message}")
                        return@launch
                    }
                } else {
                    _uploadProgress.value = 40
                }

                if (pdfBytes != null && pdfName != null) {
                    _uploadProgress.value = 60
                    _uploadStatus.value = "Uploading PDF file (this may take a moment)..."
                    val b64 = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
                    val uploadResp = repository.uploadFile(pdfName, b64, Config.FOLDER_BOOK_PDFS)
                    if (uploadResp.success && uploadResp.url != null) {
                        pdfUrl = uploadResp.url
                        _uploadProgress.value = 85
                    } else {
                        onResult(false, "PDF upload failed: ${uploadResp.message}")
                        return@launch
                    }
                } else {
                    _uploadProgress.value = 80
                }

                _uploadStatus.value = "Saving volume details..."
                val finalVol = volume.copy(thumbnail = thumbUrl, pdf = pdfUrl)
                val resp = repository.addVolume(finalVol)
                
                if (resp.success) {
                    refreshData()
                    if (volume.bookName == _selectedBookName.value) {
                        selectBook(volume.bookName)
                    }
                    _uploadProgress.value = 100
                    _uploadStatus.value = "Saved successfully!"
                    delay(800)
                }

                onResult(resp.success, resp.message ?: "Volume added successfully.")
            } catch (e: Exception) {
                onResult(false, "Operation error: ${e.message}")
            } finally {
                _uploadProgress.value = null
                _uploadStatus.value = null
                _adminLoading.value = false
            }
        }
    }

    fun editVolume(
        bookName: String,
        originalVolumeNumber: Int,
        volume: Volume,
        thumbnailBytes: ByteArray?,
        thumbnailName: String?,
        pdfBytes: ByteArray?,
        pdfName: String?,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                var thumbUrl = volume.thumbnail
                var pdfUrl = volume.pdf

                _uploadProgress.value = 15
                _uploadStatus.value = "Processing updated volume details..."

                if (thumbnailBytes != null && thumbnailName != null) {
                    _uploadProgress.value = 35
                    _uploadStatus.value = "Uploading volume thumbnail..."
                    val b64 = Base64.encodeToString(thumbnailBytes, Base64.NO_WRAP)
                    val uploadResp = repository.uploadFile(thumbnailName, b64, Config.FOLDER_VOLUME_THUMBNAILS)
                    if (uploadResp.success && uploadResp.url != null) {
                        thumbUrl = uploadResp.url
                        _uploadProgress.value = 55
                    } else {
                        onResult(false, "Thumbnail upload failed: ${uploadResp.message}")
                        return@launch
                    }
                } else {
                    _uploadProgress.value = 40
                }

                if (pdfBytes != null && pdfName != null) {
                    _uploadProgress.value = 60
                    _uploadStatus.value = "Uploading PDF file..."
                    val b64 = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
                    val uploadResp = repository.uploadFile(pdfName, b64, Config.FOLDER_BOOK_PDFS)
                    if (uploadResp.success && uploadResp.url != null) {
                        pdfUrl = uploadResp.url
                        _uploadProgress.value = 85
                    } else {
                        onResult(false, "PDF upload failed: ${uploadResp.message}")
                        return@launch
                    }
                } else {
                    _uploadProgress.value = 80
                }

                _uploadStatus.value = "Updating volume details..."
                val finalVol = volume.copy(thumbnail = thumbUrl, pdf = pdfUrl)
                val resp = repository.editVolume(bookName, originalVolumeNumber, finalVol)
                
                if (resp.success) {
                    refreshData()
                    if (bookName == _selectedBookName.value) {
                        selectBook(bookName)
                    }
                    _uploadProgress.value = 100
                    _uploadStatus.value = "Updated successfully!"
                    delay(800)
                }

                onResult(resp.success, resp.message ?: "Volume edited successfully.")
            } catch (e: Exception) {
                onResult(false, "Operation error: ${e.message}")
            } finally {
                _uploadProgress.value = null
                _uploadStatus.value = null
                _adminLoading.value = false
            }
        }
    }

    fun deleteVolume(bookName: String, volumeNumber: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                val resp = repository.deleteVolume(bookName, volumeNumber)
                if (resp.success) {
                    refreshData()
                    if (bookName == _selectedBookName.value) {
                        selectBook(bookName)
                    }
                }
                onResult(resp.success, resp.message ?: "Volume deleted successfully.")
            } catch (e: Exception) {
                onResult(false, "Failed to delete volume: ${e.message}")
            } finally {
                _adminLoading.value = false
            }
        }
    }

    // CRUD Category Actions
    fun addCategory(categoryName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                val resp = repository.addCategory(categoryName)
                if (resp.success) {
                    _categories.value = repository.getCategoriesList()
                }
                onResult(resp.success, resp.message ?: "Category added successfully")
            } catch (e: Exception) {
                onResult(false, "Operation failed: ${e.message}")
            } finally {
                _adminLoading.value = false
            }
        }
    }

    fun deleteCategory(categoryName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                val resp = repository.deleteCategory(categoryName)
                if (resp.success) {
                    _categories.value = repository.getCategoriesList()
                    if (_selectedCategory.value == categoryName) {
                        _selectedCategory.value = null
                    }
                }
                onResult(resp.success, resp.message ?: "Category deleted successfully")
            } catch (e: Exception) {
                onResult(false, "Operation failed: ${e.message}")
            } finally {
                _adminLoading.value = false
            }
        }
    }

    // CRUD Language Actions
    fun addLanguage(languageName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                val resp = repository.addLanguage(languageName)
                if (resp.success) {
                    _languages.value = repository.getLanguagesList()
                }
                onResult(resp.success, resp.message ?: "Language added successfully")
            } catch (e: Exception) {
                onResult(false, "Operation failed: ${e.message}")
            } finally {
                _adminLoading.value = false
            }
        }
    }

    fun deleteLanguage(languageName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                val resp = repository.deleteLanguage(languageName)
                if (resp.success) {
                    _languages.value = repository.getLanguagesList()
                    if (_selectedLanguage.value == languageName) {
                        _selectedLanguage.value = null
                    }
                    if (_selectedDarsLanguage.value == languageName) {
                        _selectedDarsLanguage.value = null
                    }
                }
                onResult(resp.success, resp.message ?: "Language deleted successfully")
            } catch (e: Exception) {
                onResult(false, "Operation failed: ${e.message}")
            } finally {
                _adminLoading.value = false
            }
        }
    }
}
