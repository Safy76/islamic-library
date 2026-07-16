package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val bookName: String,
    val author: String,
    val category: String,
    val language: String,
    val description: String,
    val coverImage: String,
    val totalVolumes: Int,
    val featured: Boolean,
    val isFavorite: Boolean = false,
    val lastViewedTime: Long? = null
)

@Entity(tableName = "dars_books")
data class DarsBookEntity(
    @PrimaryKey val bookName: String,
    val author: String,
    val darsClass: String, // Class/Year e.g., "Year 1", "Year 2"
    val language: String,
    val description: String,
    val coverImage: String,
    val totalVolumes: Int,
    val featured: Boolean,
    val isFavorite: Boolean = false,
    val lastViewedTime: Long? = null
)

@Entity(tableName = "volumes", primaryKeys = ["bookName", "volumeNumber"])
data class VolumeEntity(
    val bookName: String,
    val volumeNumber: Int,
    val volumeName: String,
    val thumbnail: String,
    val pdf: String,
    val fileSize: String
)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM books ORDER BY bookName ASC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE featured = 1")
    fun getFeaturedBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isFavorite = 1")
    fun getFavoriteBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE lastViewedTime IS NOT NULL ORDER BY lastViewedTime DESC LIMIT 15")
    fun getRecentlyViewedBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE bookName = :bookName")
    suspend fun updateFavorite(bookName: String, isFavorite: Boolean)

    @Query("UPDATE books SET lastViewedTime = :lastViewedTime WHERE bookName = :bookName")
    suspend fun updateLastViewed(bookName: String, lastViewedTime: Long)

    @Query("UPDATE books SET lastViewedTime = NULL")
    suspend fun clearHistory()

    @Query("SELECT * FROM books WHERE bookName = :bookName")
    suspend fun getBookByName(bookName: String): BookEntity?

    // Volumes Queries
    @Query("SELECT * FROM volumes WHERE bookName = :bookName ORDER BY volumeNumber ASC")
    fun getVolumesForBook(bookName: String): Flow<List<VolumeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVolumes(volumes: List<VolumeEntity>)

    @Query("DELETE FROM books WHERE bookName = :bookName")
    suspend fun deleteBook(bookName: String)

    @Query("DELETE FROM volumes WHERE bookName = :bookName")
    suspend fun deleteVolumesForBook(bookName: String)
    
    @Query("DELETE FROM volumes WHERE bookName = :bookName AND volumeNumber = :volumeNumber")
    suspend fun deleteVolume(bookName: String, volumeNumber: Int)

    @Query("SELECT DISTINCT category FROM books WHERE category != ''")
    suspend fun getUniqueCategories(): List<String>

    @Query("SELECT DISTINCT language FROM books WHERE language != ''")
    suspend fun getUniqueBookLanguages(): List<String>

    @Query("SELECT DISTINCT language FROM dars_books WHERE language != ''")
    suspend fun getUniqueDarsLanguages(): List<String>

    @Query("SELECT DISTINCT darsClass FROM dars_books WHERE darsClass != ''")
    suspend fun getUniqueDarsClasses(): List<String>

    @Query("UPDATE books SET category = '' WHERE category = :categoryName")
    suspend fun clearCategoryFromBooks(categoryName: String)

    @Query("UPDATE books SET language = '' WHERE language = :languageName")
    suspend fun clearLanguageFromBooks(languageName: String)

    @Query("UPDATE dars_books SET language = '' WHERE language = :languageName")
    suspend fun clearLanguageFromDarsBooks(languageName: String)

    @Query("UPDATE dars_books SET darsClass = '' WHERE darsClass = :className")
    suspend fun clearClassFromDarsBooks(className: String)

    @Query("DELETE FROM books WHERE bookName NOT IN (:bookNames)")
    suspend fun deleteBooksNotIn(bookNames: List<String>)

    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()

    @Query("DELETE FROM dars_books WHERE bookName NOT IN (:bookNames)")
    suspend fun deleteDarsBooksNotIn(bookNames: List<String>)

    @Query("DELETE FROM dars_books")
    suspend fun deleteAllDarsBooks()

    @Query("DELETE FROM volumes WHERE bookName = :bookName AND volumeNumber NOT IN (:volumeNumbers)")
    suspend fun deleteVolumesForBookNotIn(bookName: String, volumeNumbers: List<Int>)

    @Query("SELECT EXISTS(SELECT 1 FROM books LIMIT 1)")
    suspend fun hasBooks(): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM dars_books LIMIT 1)")
    suspend fun hasDarsBooks(): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM volumes WHERE bookName = :bookName LIMIT 1)")
    suspend fun hasVolumesForBook(bookName: String): Boolean

    // Dars Books Queries
    @Query("SELECT * FROM dars_books ORDER BY bookName ASC")
    fun getAllDarsBooks(): Flow<List<DarsBookEntity>>

    @Query("SELECT * FROM dars_books WHERE featured = 1")
    fun getFeaturedDarsBooks(): Flow<List<DarsBookEntity>>

    @Query("SELECT * FROM dars_books WHERE isFavorite = 1")
    fun getFavoriteDarsBooks(): Flow<List<DarsBookEntity>>

    @Query("SELECT * FROM dars_books WHERE lastViewedTime IS NOT NULL ORDER BY lastViewedTime DESC LIMIT 15")
    fun getRecentlyViewedDarsBooks(): Flow<List<DarsBookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDarsBooks(books: List<DarsBookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDarsBook(book: DarsBookEntity)

    @Query("UPDATE dars_books SET isFavorite = :isFavorite WHERE bookName = :bookName")
    suspend fun updateDarsFavorite(bookName: String, isFavorite: Boolean)

    @Query("UPDATE dars_books SET lastViewedTime = :lastViewedTime WHERE bookName = :bookName")
    suspend fun updateDarsLastViewed(bookName: String, lastViewedTime: Long)

    @Query("SELECT * FROM dars_books WHERE bookName = :bookName")
    suspend fun getDarsBookByName(bookName: String): DarsBookEntity?

    @Query("DELETE FROM dars_books WHERE bookName = :bookName")
    suspend fun deleteDarsBook(bookName: String)
}

@Database(entities = [BookEntity::class, VolumeEntity::class, DarsBookEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
