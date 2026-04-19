package com.example.texttosound.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.texttosound.api.RetrofitClient
import com.example.texttosound.api.SynthesisRequest
import com.example.texttosound.model.Book
import com.example.texttosound.model.Chapter
import com.example.texttosound.player.AudioPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.texttosound.paging.PostPagingSource
import java.io.File
import java.util.zip.ZipFile

import com.example.texttosound.database.*

class BookViewModel(
    private val audioPlayer: AudioPlayer,
    private val bookDao: BookDao,
    private val progressDao: ReadingProgressDao,
    private val userDao: UserDao,
    private val postDao: SocialPostDao
) : ViewModel() {
    
    private var loadedZipFile: ZipFile? = null
    private var spineHrefs = mutableListOf<String>()

    private val _bookState = MutableStateFlow<Book?>(null)
    val bookState = _bookState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex = _currentChapterIndex.asStateFlow()

    private val _currentBlockIndex = MutableStateFlow(0)
    val currentBlockIndex = _currentBlockIndex.asStateFlow()

    private val _isSynthesizing = MutableStateFlow(false)
    val isSynthesizing = _isSynthesizing.asStateFlow()
    val isPlaying: StateFlow<Boolean> = audioPlayer.isPlaying

    // Audio Cache for continuous playback
    private val audioCache = mutableMapOf<String, File>() // Key: "chapterIdx_blockIdx_speed"

    // Holds the last error message to show in UI
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError = _loadError.asStateFlow()
    fun consumeError() { _loadError.value = null }

    // API Data flows
    private val _libraryBooks = MutableStateFlow<List<com.example.texttosound.api.LibraryBook>>(emptyList())
    val libraryBooks = _libraryBooks.asStateFlow()

    private val _storeBooks = MutableStateFlow<List<com.example.texttosound.api.Book>>(emptyList())
    val storeBooks = _storeBooks.asStateFlow()

    private val _posts = MutableStateFlow<List<com.example.texttosound.api.Post>>(emptyList())
    val posts = _posts.asStateFlow()

    private val _groups = MutableStateFlow<List<com.example.texttosound.api.Group>>(emptyList())
    val groups = _groups.asStateFlow()

    private val _userProfile = MutableStateFlow<com.example.texttosound.api.User?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _isBackendAvailable = MutableStateFlow(true)
    val isBackendAvailable = _isBackendAvailable.asStateFlow()

    val postsPagingFlow = Pager(
        config = PagingConfig(pageSize = 10),
        pagingSourceFactory = { PostPagingSource(RetrofitClient.appApiService) }
    ).flow.cachedIn(viewModelScope)

    init {
        fetchLibrary()
        fetchStore()
        fetchPosts()
        fetchGroups()
        fetchUserProfile()
        
        startAutoReconnectLoop()

        // Aggressive cleanup for unwanted laptop-covered books and duplicates
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.deleteBook("1")
            bookDao.deleteBook("2")
            
            val allBooks = bookDao.getAllBooks()
            // Track seen titles to remove duplicates within the local library
            val seenTitles = mutableSetOf<String>()
            
            for (book in allBooks) {
                val isLaptop = book.coverUrl?.contains("laptop") == true || 
                              book.coverUrl?.contains("placeholder") == true ||
                              book.title.contains("laptop", ignoreCase = true)
                
                if (isLaptop) {
                    Log.d("BookViewModel", "Cleaning up laptop placeholder: ${book.title}")
                    bookDao.deleteBook(book.id)
                } else {
                    val normalizedTitle = book.title.trim().lowercase()
                    if (seenTitles.contains(normalizedTitle)) {
                        // Keep the one with an epubPath if possible
                        if (book.epubPath == null) {
                            Log.d("BookViewModel", "Cleaning up duplicate: ${book.title}")
                            bookDao.deleteBook(book.id)
                        } else {
                            // For simplicity, we just delete the current one if it's the duplicate
                            Log.d("BookViewModel", "Found duplicate with path, but already saw one: ${book.title}")
                        }
                    } else {
                        seenTitles.add(normalizedTitle)
                    }
                }
            }
            fetchLibrary() // Refresh after cleanup
        }
    }

    fun preloadBooks(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadDir = File("/sdcard/Download")
            if (downloadDir.exists() && downloadDir.isDirectory) {
                val files = downloadDir.listFiles { _, name -> name.endsWith(".epub") }
                files?.forEach { file ->
                    var extractedTitle = file.nameWithoutExtension
                    var extractedAuthor = "Unknown"
                    
                    try {
                        ZipFile(file).use { zip ->
                            val containerEntry = zip.getEntry("META-INF/container.xml")
                            val containerDoc = Jsoup.parse(zip.getInputStream(containerEntry).bufferedReader().readText(), "", Parser.xmlParser())
                            val opfPath = containerDoc.select("rootfile").attr("full-path")
                            val opfDoc = Jsoup.parse(zip.getInputStream(zip.getEntry(opfPath)).bufferedReader().readText(), "", Parser.xmlParser())
                            
                            extractedTitle = opfDoc.select("dc|title, title").first()?.text() ?: file.nameWithoutExtension
                            extractedAuthor = opfDoc.select("dc|creator, creator").first()?.text() ?: "Unknown Author"
                        }
                    } catch (e: Exception) {
                        Log.e("BookViewModel", "Preload meta failed: ${e.message}")
                    }

                    // Check DB directly
                    val existingEntity = bookDao.findLocalBook(extractedTitle, extractedAuthor)
                    val uniqueId = existingEntity?.id ?: "preloaded_${file.nameWithoutExtension}"
                    
                    if (existingEntity == null || (existingEntity.coverUrl?.endsWith(".epub") == true)) {
                        var coverPath: String? = null
                        try {
                            ZipFile(file).use { zip ->
                                val containerEntry = zip.getEntry("META-INF/container.xml")
                                val containerDoc = Jsoup.parse(zip.getInputStream(containerEntry).bufferedReader().readText(), "", Parser.xmlParser())
                                val opfPath = containerDoc.select("rootfile").attr("full-path")
                                val opfDoc = Jsoup.parse(zip.getInputStream(zip.getEntry(opfPath)).bufferedReader().readText(), "", Parser.xmlParser())
                                
                                val manifestItems = opfDoc.select("manifest > item")
                                val itemMap = manifestItems.associate { it.attr("id") to it.attr("href") }
                                val parentDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
                                
                                val coverId = opfDoc.select("meta[name=cover]").attr("content").ifEmpty {
                                    opfDoc.select("item[properties=cover-image]").attr("id")
                                }
                                if (coverId.isNotEmpty()) {
                                    val coverHref = itemMap[coverId]
                                    if (coverHref != null) {
                                        zip.getEntry(parentDir + coverHref)?.let { entry ->
                                            coverPath = saveCoverImage(context, uniqueId, zip.getInputStream(entry).readBytes())
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("BookViewModel", "Migration failed for $extractedTitle: ${e.message}")
                        }

                        val entity = BookEntity(
                            id = uniqueId,
                            title = extractedTitle,
                            author = extractedAuthor,
                            coverUrl = coverPath ?: file.absolutePath,
                            epubPath = file.absolutePath,
                            isLibrary = true,
                            genre = "Local"
                        )
                        bookDao.insertBooks(listOf(entity))
                        Log.d("BookViewModel", "Book synced: $extractedTitle")
                    }
                }
            }
        }
    }

    private fun saveCoverImage(context: Context, bookId: String, coverBytes: ByteArray?): String? {
        if (coverBytes == null) return null
        return try {
            val coversDir = File(context.filesDir, "covers")
            if (!coversDir.exists()) coversDir.mkdirs()
            val coverFile = File(coversDir, "$bookId.jpg")
            coverFile.writeBytes(coverBytes)
            coverFile.absolutePath
        } catch (e: Exception) {
            Log.e("BookViewModel", "Error saving cover: ${e.message}")
            null
        }
    }

    private fun resolvePath(base: String, relative: String): String {
        var path = base + relative
        // Handle ../
        while (path.contains("/../")) {
            path = path.replaceFirst(Regex("[^/]+(?<!\\.\\.)/\\.\\./"), "")
        }
        return path
    }

    private fun extractChapterTitles(zip: ZipFile, opfDoc: Document, itemMap: Map<String, String>, parentDir: String): Map<String, String> {
        val titleMap = mutableMapOf<String, String>()
        try {
            // Find NCX (EPUB 2)
            val ncxItem = opfDoc.select("item[media-type=application/x-dtbncx+xml]").firstOrNull() ?: opfDoc.select("item[id=ncx]").firstOrNull()
            if (ncxItem != null) {
                val ncxHref = ncxItem.attr("href")
                val ncxPath = parentDir + ncxHref
                val ncxDir = if (ncxPath.contains("/")) ncxPath.substringBeforeLast("/") + "/" else ""
                
                val ncxEntry = zip.getEntry(ncxPath)
                if (ncxEntry != null) {
                    val ncxDoc = Jsoup.parse(zip.getInputStream(ncxEntry).bufferedReader().readText(), "", Parser.xmlParser())
                    ncxDoc.select("navPoint").forEach { navPoint ->
                        val title = navPoint.select("navLabel > text").first()?.text()
                        val rawSrc = navPoint.select("content").attr("src").substringBefore("#")
                        val src = resolvePath(ncxDir, rawSrc)
                        
                        if (title != null && src.isNotEmpty()) {
                            titleMap[src] = title
                        }
                    }
                }
            }
            
            // Find Nav (EPUB 3)
            val navItem = opfDoc.select("item[properties=nav]").firstOrNull()
            if (navItem != null) {
                val navHref = navItem.attr("href")
                val navPath = parentDir + navHref
                val navDir = if (navPath.contains("/")) navPath.substringBeforeLast("/") + "/" else ""
                
                val navEntry = zip.getEntry(navPath)
                if (navEntry != null) {
                    val navDoc = Jsoup.parse(zip.getInputStream(navEntry).bufferedReader().readText())
                    navDoc.select("nav[epub|type=toc] ol li a, nav ol li a").forEach { a ->
                        val title = a.text()
                        var rawHref = a.attr("href")
                        if (rawHref.startsWith("./")) rawHref = rawHref.substring(2)
                        val href = resolvePath(navDir, rawHref.substringBefore("#"))
                        
                        if (title.isNotEmpty() && href.isNotEmpty()) {
                            titleMap[href] = title
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BookViewModel", "Error extracting chapter titles: ${e.message}")
        }
        return titleMap
    }

    fun fetchLibrary(userId: Int = 1) {
        // First load from local DB
        viewModelScope.launch {
            bookDao.getLibraryBooks().collect { localBooks ->
                if (_libraryBooks.value.isEmpty() || localBooks.isNotEmpty()) {
                    _libraryBooks.value = localBooks.map { 
                        com.example.texttosound.api.LibraryBook(
                            id = it.id.toIntOrNull() ?: 0,
                            title = it.title,
                            author = it.author,
                            coverUrl = it.coverUrl,
                            genre = it.genre ?: "",
                            rating = it.rating.toFloat(),
                            chapterCount = 0,
                            description = it.description,
                            readerCountInfo = "0 người đã đọc",
                            progressPercent = 0.0f, // Should fetch from progressDao
                            lastReadAt = "",
                            epubPath = it.epubPath,
                            localId = it.id
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.appApiService.getUserLibrary(userId)
                if (response.isSuccessful) {
                    _isBackendAvailable.value = true
                    val books = response.body() ?: emptyList()
                    val entities = mutableListOf<BookEntity>()
                    for (book in books) {
                        val existing = bookDao.findLocalBook(book.title, book.author)
                        entities.add(BookEntity(
                            id = existing?.id ?: book.id.toString(),
                            title = book.title, 
                            author = book.author, 
                            coverUrl = existing?.coverUrl ?: book.coverUrl, 
                            epubPath = existing?.epubPath,
                            isLibrary = true,
                            description = existing?.description ?: book.description,
                            genre = existing?.genre ?: book.genre,
                            rating = existing?.rating ?: book.rating.toFloat()
                        ))
                    }
                    bookDao.insertBooks(entities)
                    // NOTE: The collector above will automatically emit the merged list.
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Error fetching library: ${e.message}")
                _isBackendAvailable.value = false
            }
        }
    }

    fun fetchStore() {
        viewModelScope.launch {
            bookDao.getStoreBooks().collect { localBooks ->
                if (_storeBooks.value.isEmpty() || localBooks.isNotEmpty()) {
                    _storeBooks.value = localBooks.map { 
                        com.example.texttosound.api.Book(
                            id = it.id.toIntOrNull() ?: 0,
                            title = it.title,
                            author = it.author,
                            coverUrl = it.coverUrl,
                            genre = it.genre ?: "",
                            rating = it.rating.toFloat(), // Fixed Float mismatch
                            chapterCount = 0,
                            description = it.description ?: "",
                            epubPath = it.epubPath,
                            readerCountInfo = "0 người đã đọc"
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.appApiService.getBooks()
                if (response.isSuccessful) {
                    _isBackendAvailable.value = true
                    val books = response.body() ?: emptyList()
                    _storeBooks.value = books
                    
                    val entities = books.map { 
                        BookEntity(
                            id = it.id.toString(),
                            title = it.title,
                            author = it.author,
                            coverUrl = it.coverUrl,
                            isLibrary = false,
                            description = it.description,
                            genre = it.genre,
                            rating = it.rating.toFloat()
                        )
                    }
                    bookDao.insertBooks(entities)
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Error fetching store: ${e.message}")
                _isBackendAvailable.value = false
            }
        }
    }

    fun fetchPosts() {
        viewModelScope.launch {
            postDao.getPosts().collect { localPosts ->
                if (_posts.value.isEmpty() || localPosts.isNotEmpty()) {
                    _posts.value = localPosts.map { 
                        com.example.texttosound.api.Post(
                            id = it.id,
                            authorId = 0,
                            content = it.content,
                            likesCount = it.likesCount,
                            commentsCount = it.commentsCount,
                            createdAt = it.createdAt,
                            author = com.example.texttosound.api.User(
                                id = 0,
                                name = it.authorName,
                                avatarUrl = it.authorAvatarUrl,
                                bio = null,
                                daysOnPlatform = 0,
                                badgeCount = 0
                            )
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.appApiService.getPosts()
                if (response.isSuccessful) {
                    _isBackendAvailable.value = true
                    val posts = response.body() ?: emptyList()
                    _posts.value = posts
                    
                    val entities = posts.map { 
                        SocialPostEntity(
                            id = it.id,
                            authorName = it.author.name,
                            authorAvatarUrl = it.author.avatarUrl,
                            content = it.content,
                            likesCount = it.likesCount,
                            commentsCount = it.commentsCount,
                            createdAt = it.createdAt
                        )
                    }
                    postDao.clearPosts()
                    postDao.insertPosts(entities)
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Error fetching posts: ${e.message}")
                _isBackendAvailable.value = false
            }
        }
    }

    fun fetchGroups() {
        // Groups don't have a dedicated DAO/Entity yet in my implementation, 
        // but I could add them if needed. For now, let's keep the basic ones.
        viewModelScope.launch {
            try {
                val response = RetrofitClient.appApiService.getGroups()
                if (response.isSuccessful) {
                    _groups.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Error fetching groups: ${e.message}")
            }
        }
    }

    fun fetchUserProfile(userId: Int = 1) {
        viewModelScope.launch {
            userDao.getUser(userId).collect { localUser ->
                if (localUser != null) {
                    _userProfile.value = com.example.texttosound.api.User(
                        id = localUser.id,
                        name = localUser.name,
                        avatarUrl = localUser.avatarUrl,
                        bio = localUser.bio,
                        daysOnPlatform = localUser.daysOnPlatform,
                        badgeCount = localUser.badgeCount
                    )
                }
            }
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.appApiService.getUser(userId)
                if (response.isSuccessful) {
                    _isBackendAvailable.value = true
                    val user = response.body()
                    if (user != null) {
                        _userProfile.value = user
                        userDao.insertUser(
                            UserEntity(
                                id = user.id,
                                name = user.name,
                                avatarUrl = user.avatarUrl,
                                bio = user.bio,
                                daysOnPlatform = user.daysOnPlatform,
                                badgeCount = user.badgeCount
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Error fetching user profile: ${e.message}")
                _isBackendAvailable.value = false
            }
        }
    }

    fun loadApiBook(book: com.example.texttosound.api.LibraryBook) {
        audioPlayer.stop()
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Create a dummy book state from API metadata
                val dummyChapter = Chapter(
                    id = "0",
                    title = "Giới thiệu",
                    textBlocks = listOf(book.description ?: "Không có mô tả cho cuốn sách này.")
                )
                
                _bookState.value = Book(
                    id = book.id.toString(),
                    title = book.title,
                    author = book.author,
                    chapters = listOf(dummyChapter),
                    coverImage = null // We'll rely on remoteCoverUrl if we had one in BookState, or just metadata
                )
                _currentChapterIndex.value = 0
                _currentBlockIndex.value = 0
                
                // Note: We don't have a local file for this, so synthesis will read the description
            } catch (e: Exception) {
                Log.e("BookViewModel", "Error loading API book: ${e.message}")
                _loadError.value = "Không thể mở sách: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // EPUB Loading Logic...
    fun loadLocalEpub(context: Context, filePath: String, bookId: String) {
        audioPlayer.stop()
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    _loadError.value = "File not found: $filePath"
                    return@launch
                }
                loadedZipFile?.close()  // Close previous book's ZipFile
                val zip = ZipFile(file)
                loadedZipFile = zip
                
                val containerEntry = zip.getEntry("META-INF/container.xml")
                val containerContent = zip.getInputStream(containerEntry).bufferedReader().use { it.readText() }
                val containerDoc = Jsoup.parse(containerContent, "", Parser.xmlParser())
                val opfPath = containerDoc.select("rootfile").attr("full-path")
                
                val opfEntry = zip.getEntry(opfPath)
                val opfContent = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }
                val opfDoc = Jsoup.parse(opfContent, "", Parser.xmlParser())
                
                val title = opfDoc.select("dc|title, title").first()?.text() ?: "Unknown Title"
                val author = opfDoc.select("dc|creator, creator").first()?.text() ?: "Unknown Author"
                
                val manifestItems = opfDoc.select("manifest > item")
                val itemMap = manifestItems.associate { it.attr("id") to it.attr("href") }
                
                val spineItems = opfDoc.select("spine > itemref")
                spineHrefs.clear()
                val parentDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
                
                spineItems.forEach { 
                    val idref = it.attr("idref")
                    val relativeHref = itemMap[idref]
                    if (relativeHref != null) {
                        spineHrefs.add(parentDir + relativeHref)
                    }
                }

                val titleMap = extractChapterTitles(zip, opfDoc, itemMap, parentDir)
                val chapters = spineHrefs.mapIndexed { index, href ->
                    val extractedTitle = titleMap[href] ?: "Chương ${index + 1}"
                    Chapter(id = index.toString(), title = extractedTitle, textBlocks = emptyList())
                }

                val coverId = opfDoc.select("meta[name=cover]").attr("content").ifEmpty {
                    opfDoc.select("item[properties=cover-image]").attr("id")
                }
                var coverBytes: ByteArray? = null
                if (coverId.isNotEmpty()) {
                    val coverHref = itemMap[coverId]
                    if (coverHref != null) {
                        val fullCoverPath = parentDir + coverHref
                        zip.getEntry(fullCoverPath)?.let { entry ->
                            coverBytes = zip.getInputStream(entry).use { it.readBytes() }
                        }
                    }
                }

                _bookState.value = Book(
                    id = bookId, 
                    title = title, 
                    author = author, 
                    chapters = chapters, 
                    coverImage = coverBytes,
                    description = opfDoc.select("dc|description, description").first()?.text()
                )
               
                // Load saved progress
                viewModelScope.launch {
                    val progress = progressDao.getProgress(bookId).firstOrNull()
                    if (progress != null) {
                        _currentChapterIndex.value = progress.chapterIndex
                        _currentBlockIndex.value = progress.blockIndex
                        loadTextForChapter(progress.chapterIndex)
                    } else {
                        _currentChapterIndex.value = 0
                        _currentBlockIndex.value = 0
                        loadTextForChapter(0)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("BookViewModel", "Error loading local EPUB: ${e.message}")
                _loadError.value = "Không thể mở EPUB: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun loadEpub(context: Context, epubFileUri: Uri) {
        audioPlayer.stop()
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val uniqueId = "local_${System.currentTimeMillis()}"
                val tempFile = File(context.filesDir, "$uniqueId.epub")
                context.contentResolver.openInputStream(epubFileUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                loadedZipFile?.close()  // Close previous book's ZipFile
                val zip = ZipFile(tempFile)
                loadedZipFile = zip
                
                val containerEntry = zip.getEntry("META-INF/container.xml")
                val containerContent = zip.getInputStream(containerEntry).bufferedReader().use { it.readText() }
                val containerDoc = Jsoup.parse(containerContent, "", Parser.xmlParser())
                val opfPath = containerDoc.select("rootfile").attr("full-path")
                
                val opfEntry = zip.getEntry(opfPath)
                val opfContent = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }
                val opfDoc = Jsoup.parse(opfContent, "", Parser.xmlParser())
                
                val title = opfDoc.select("dc|title, title").first()?.text() ?: "Unknown Title"
                val author = opfDoc.select("dc|creator, creator").first()?.text() ?: "Unknown Author"

                // Deduplicate: check if this book already exists in library
                val existing = bookDao.findLocalBook(title, author)
                if (existing != null) {
                    Log.d("BookViewModel", "Book already exists: $title. Opening existing.")
                    loadLocalEpub(context, existing.coverUrl ?: tempFile.absolutePath, existing.id)
                    return@launch
                }
                
                val manifestItems = opfDoc.select("manifest > item")
                val itemMap = manifestItems.associate { it.attr("id") to it.attr("href") }
                
                val spineItems = opfDoc.select("spine > itemref")
                spineHrefs.clear()
                val parentDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
                
                spineItems.forEach { 
                    val idref = it.attr("idref")
                    val relativeHref = itemMap[idref]
                    if (relativeHref != null) {
                        spineHrefs.add(parentDir + relativeHref)
                    }
                }

                val chapters = spineHrefs.mapIndexed { index, href ->
                    Chapter(id = index.toString(), title = "Chương ${index + 1}", textBlocks = emptyList())
                }

                val coverId = opfDoc.select("meta[name=cover]").attr("content").ifEmpty {
                    opfDoc.select("item[properties=cover-image]").attr("id")
                }
                var coverBytes: ByteArray? = null
                if (coverId.isNotEmpty()) {
                    val coverHref = itemMap[coverId]
                    if (coverHref != null) {
                        val fullCoverPath = parentDir + coverHref
                        zip.getEntry(fullCoverPath)?.let { entry ->
                            coverBytes = zip.getInputStream(entry).use { it.readBytes() }
                        }
                    }
                }

                _bookState.value = Book(
                    id = uniqueId, 
                    title = title, 
                    author = author, 
                    chapters = chapters, 
                    coverImage = coverBytes,
                    description = existing?.description,
                    rating = existing?.rating ?: 0f
                )
               
                val coverPath = saveCoverImage(context, uniqueId, coverBytes)
                
                // Insert to DB so it shows in Library
                val entity = BookEntity(
                    id = uniqueId,
                    title = title,
                    author = author,
                            coverUrl = coverPath ?: tempFile.absolutePath,
                            epubPath = tempFile.absolutePath,
                    isLibrary = true,
                    genre = "Local"
                )
                bookDao.insertBooks(listOf(entity))
                
                // Load saved progress, then signal UI to navigate to player
                viewModelScope.launch {
                    val progress = progressDao.getProgress(uniqueId).firstOrNull()
                    if (progress != null) {
                        _currentChapterIndex.value = progress.chapterIndex
                        _currentBlockIndex.value = progress.blockIndex
                        loadTextForChapter(progress.chapterIndex)
                    } else {
                        _currentChapterIndex.value = 0
                        _currentBlockIndex.value = 0
                        loadTextForChapter(0)
                    }
                    // Book is ready in library – no forced navigation, user can tap it
                }
                
            } catch (e: Exception) {
                Log.e("BookViewModel", "Error loading EPUB: ${e.message}")
                _loadError.value = "Không thể mở EPUB: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveReadingProgress() {
        val book = _bookState.value ?: return
        val chapterIdx = _currentChapterIndex.value
        val blockIdx = _currentBlockIndex.value
        
        viewModelScope.launch(Dispatchers.IO) {
            progressDao.saveProgress(
                ReadingProgressEntity(
                    bookId = book.id,
                    chapterIndex = chapterIdx,
                    blockIndex = blockIdx
                )
            )
        }
    }

    private suspend fun loadTextForChapter(chapterIndex: Int) {
        val zip = loadedZipFile ?: return
        if (chapterIndex !in spineHrefs.indices) return
        
        val href = spineHrefs[chapterIndex]
        val entry = zip.getEntry(href) ?: return
        
        val htmlContent = zip.getInputStream(entry).bufferedReader().use { it.readText() }
        val doc = Jsoup.parse(htmlContent)
        
        val paragraphs = doc.select("p, h1, h2, h3, h4, h5, h6, li")
        val blocks = paragraphs.map { it.text() }.filter { it.isNotBlank() }
        
        val currentBook = _bookState.value ?: return
        val updatedChapters = currentBook.chapters.toMutableList()
        updatedChapters[chapterIndex] = updatedChapters[chapterIndex].copy(textBlocks = blocks)
        
        _bookState.value = currentBook.copy(chapters = updatedChapters)
    }

    fun playCurrentBlock(context: Context, language: String, speed: Float, retryCount: Int = 0) {
        val book = _bookState.value ?: return
        val chapterIdx = _currentChapterIndex.value
        val blockIdx = _currentBlockIndex.value
        val chapter = book.chapters.getOrNull(chapterIdx) ?: return
        val text = chapter.textBlocks.getOrNull(blockIdx) ?: return

        audioPlayer.stop()
        
        // Speed-aware cache key
        val cacheKey = "${chapterIdx}_${blockIdx}_${speed}"
        val cachedFile = audioCache[cacheKey]

        if (cachedFile != null && cachedFile.exists()) {
            Log.d("BookViewModel", "Playing from cache: $cacheKey")
            _isSynthesizing.value = false
            audioPlayer.playAudioFile(cachedFile) {
                onAudioFinished(context, language, speed)
            }
            // Prefetch next blocks with same speed
            prefetchNextBlocks(context, chapterIdx, blockIdx + 1, language, speed)
            return
        }

        viewModelScope.launch {
            _isSynthesizing.value = true
            try {
                Log.d("BookViewModel", "Synthesizing block: $cacheKey at speed $speed (retry: $retryCount)")
                val response = RetrofitClient.apiService.synthesizeText(SynthesisRequest(text, language, speed))
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val tempFile = File(context.cacheDir, "audio_$cacheKey.wav")
                        withContext(Dispatchers.IO) {
                            tempFile.outputStream().use { output ->
                                body.byteStream().use { input ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        audioCache[cacheKey] = tempFile
                        audioPlayer.playAudioFile(tempFile) {
                            onAudioFinished(context, language, speed)
                        }
                        // Prefetch next blocks
                        prefetchNextBlocks(context, chapterIdx, blockIdx + 1, language, speed)
                    }
                } else {
                    Log.e("BookViewModel", "Server error ${response.code()}, retrying...")
                    handleSynthesisError(context, language, speed, retryCount)
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Synthesis error: ${e.message}, retrying...")
                handleSynthesisError(context, language, speed, retryCount)
            } finally {
                _isSynthesizing.value = false
            }
        }
    }

    private fun handleSynthesisError(context: Context, language: String, speed: Float, retryCount: Int) {
        if (retryCount < 2) {
            playCurrentBlock(context, language, speed, retryCount + 1)
        } else {
            Log.e("BookViewModel", "Max retries reached, skipping block.")
            _loadError.value = "Lỗi kết nối câu này, đang tự động bỏ qua..."
            onAudioFinished(context, language, speed) // Fail-forward
        }
    }

    private fun prefetchNextBlocks(context: Context, startChapIdx: Int, startBlockIdx: Int, language: String, speed: Float) {
        val book = _bookState.value ?: return
        
        // Caching strategy: current chapter + 50% of next chapter, max 10 mins (1500 words)
        val WORD_LIMIT = 1500
        
        viewModelScope.launch {
            var totalWords = 0
            var cIdx = startChapIdx
            var bIdx = startBlockIdx
            
            while (totalWords < WORD_LIMIT) {
                var chapter = book.chapters.getOrNull(cIdx) ?: break
                
                // If we've reached beyond the next chapter, stop
                if (cIdx > startChapIdx + 1) break
                
                // If we are in the next chapter, check the 50% limit
                if (cIdx == startChapIdx + 1 && bIdx >= chapter.textBlocks.size / 2) break

                if (bIdx >= chapter.textBlocks.size) {
                    cIdx++
                    bIdx = 0
                    chapter = book.chapters.getOrNull(cIdx) ?: break
                    // Load text for next chapter if empty
                    if (chapter.textBlocks.isEmpty()) {
                        withContext(Dispatchers.IO) {
                            loadTextForChapter(cIdx)
                        }
                        chapter = book.chapters.getOrNull(cIdx) ?: break
                    }
                    continue
                }
                
                val text = chapter.textBlocks.getOrNull(bIdx) ?: break
                val wordCount = text.split("\\s+".toRegex()).size
                totalWords += wordCount
                
                val key = "${cIdx}_${bIdx}_${speed}"
                if (!audioCache.containsKey(key)) {
                    try {
                        Log.d("BookViewModel", "Prefetching ($totalWords/1500 words): $key at speed $speed")
                        val response = RetrofitClient.apiService.synthesizeText(SynthesisRequest(text, language, speed))
                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body != null) {
                                val tempFile = File(context.cacheDir, "audio_$key.wav")
                                withContext(Dispatchers.IO) {
                                    tempFile.outputStream().use { output ->
                                        body.byteStream().use { input ->
                                            input.copyTo(output)
                                        }
                                    }
                                }
                                audioCache[key] = tempFile
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BookViewModel", "Prefetch error for $key: ${e.message}")
                        break 
                    }
                }
                bIdx++
            }
            Log.d("BookViewModel", "Prefetch look-ahead complete: $totalWords words.")
        }
    }

    private fun onAudioFinished(context: Context, language: String, speed: Float) {
        val book = _bookState.value ?: return
        val chapter = book.chapters[_currentChapterIndex.value]
        
        if (_currentBlockIndex.value < chapter.textBlocks.size - 1) {
            _currentBlockIndex.value++
            playCurrentBlock(context, language, speed)
        } else if (_currentChapterIndex.value < book.chapters.size - 1) {
            val newChapIndex = _currentChapterIndex.value + 1
            viewModelScope.launch {
                _isLoading.value = true
                loadTextForChapter(newChapIndex)
                _currentChapterIndex.value = newChapIndex
                _currentBlockIndex.value = 0
                _isLoading.value = false
                playCurrentBlock(context, language, speed)
            }
        }
    }

    fun nextBlock(context: Context, language: String, speed: Float) {
        audioPlayer.stop()
        onAudioFinished(context, language, speed)
    }

    fun previousBlock(context: Context, language: String, speed: Float) {
        audioPlayer.stop()
        if (_currentBlockIndex.value > 0) {
            _currentBlockIndex.value--
            playCurrentBlock(context, language, speed)
        } else if (_currentChapterIndex.value > 0) {
            val newChapIndex = _currentChapterIndex.value - 1
            viewModelScope.launch {
                _isLoading.value = true
                loadTextForChapter(newChapIndex)
                _currentChapterIndex.value = newChapIndex
                val book = _bookState.value!!
                _currentBlockIndex.value = book.chapters[newChapIndex].textBlocks.size - 1
                _isLoading.value = false
                playCurrentBlock(context, language, speed)
            }
        }
    }
    
    fun jumpToChapter(index: Int) {
        audioPlayer.stop()
        val book = _bookState.value ?: return
        if (index !in book.chapters.indices) return
        
        // Clear cache on major jump to avoid memory pressure and stale audio
        audioCache.values.forEach { it.delete() }
        audioCache.clear()
        
        viewModelScope.launch {
            _isLoading.value = true
            loadTextForChapter(index)
            _currentChapterIndex.value = index
            _currentBlockIndex.value = 0
            _isLoading.value = false
            // Optional: Auto-play might be expected after jump, but previous logic didn't auto-play.
            // Let's stick to the previous behavior unless requested.
        }
    }
    
    fun setBlockIndex(index: Int) {
        _currentBlockIndex.value = index
    }
    
    fun closeBook() {
        audioPlayer.stop()
        _bookState.value = null
        try {
            loadedZipFile?.close()
        } catch (e: Exception) {}
        loadedZipFile = null
        spineHrefs.clear()
        audioCache.values.forEach { it.delete() }
        audioCache.clear()
        _currentChapterIndex.value = 0
        _currentBlockIndex.value = 0
    }

    fun stopAudio() {
        audioPlayer.stop()
        saveReadingProgress()
    }

    private fun startAutoReconnectLoop() {
        viewModelScope.launch {
            while (true) {
                if (!_isBackendAvailable.value) {
                    Log.d("BookViewModel", "Backend offline, attempting to reconnect...")
                    try {
                        val response = RetrofitClient.appApiService.checkHealth()
                        if (response.isSuccessful) {
                            Log.d("BookViewModel", "Backend back online!")
                            _isBackendAvailable.value = true
                            // Refresh all data
                            fetchLibrary()
                            fetchStore()
                            fetchPosts()
                            fetchGroups()
                            fetchUserProfile()
                        }
                    } catch (e: Exception) {
                        Log.d("BookViewModel", "Reconnect attempt failed: ${e.message}")
                    }
                }
                delay(10000) // Check every 10 seconds
            }
        }
    }

    override fun onCleared() {
       super.onCleared()
        saveReadingProgress()
        audioPlayer.stop()
    }
}
