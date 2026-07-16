@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Book
import com.example.data.model.Volume
import com.example.data.local.BookEntity
import com.example.data.local.DarsBookEntity
import com.example.data.local.VolumeEntity
import com.example.data.model.Category
import com.example.data.model.Language
import com.example.data.model.DarsClass
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.GradientButton
import com.example.ui.components.SectionHeader
import com.example.ui.viewmodel.LibraryViewModel
import com.example.config.Config
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap

// ================= ADMIN LOGIN SCREEN =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginScreen(
    viewModel: LibraryViewModel,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    val loading by viewModel.adminLoading.collectAsState()
    val error by viewModel.adminError.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Authorization") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(50.dp)
                    )
                    Text(
                        text = "Sign In to Console",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Access is restricted to authorized administrators. Google Drive & Sheets permissions are handled on validation.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    if (error != null) {
                        Text(
                            text = error!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Admin Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_email_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text("Remember Login Session", fontSize = 13.sp)
                    }

                    if (loading) {
                        CircularProgressIndicator()
                    } else {
                        GradientButton(
                            text = "Authenticate",
                            onClick = {
                                if (email.trim().isEmpty() || password.trim().isEmpty()) {
                                    Toast.makeText(viewModel.getApplication(), "Fill in credentials", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.adminLogin(email.trim(), password.trim(), rememberMe, onLoginSuccess)
                                }
                            },
                            testTag = "admin_login_submit_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ================= RESPONSIVE ADMIN DASHBOARD PANEL =================
@Composable
fun AdminDashboardPanel(
    viewModel: LibraryViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("dashboard") } // dashboard, books, volumes, categories, languages, dars_classes, dars_books

    val email by viewModel.adminEmail.collectAsState()

    // Retrieve global screen size width dynamically (responsive check)
    // In Android Compose, BoxWithConstraints is the optimal container to query bounds!
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 600.dp // Tablet or Desktop layout

        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar Navigation Rail (Visible on Tablets/Wide Screens)
            if (isWide) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Console",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp).padding(vertical = 12.dp)
                        )
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    NavigationRailItem(
                        selected = selectedTab == "dashboard",
                        onClick = { selectedTab = "dashboard" },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == "books",
                        onClick = { selectedTab = "books" },
                        icon = { Icon(Icons.Default.Book, contentDescription = "Books") },
                        label = { Text("Books") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == "volumes",
                        onClick = { selectedTab = "volumes" },
                        icon = { Icon(Icons.Default.Layers, contentDescription = "Volumes") },
                        label = { Text("Volumes") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == "categories",
                        onClick = { selectedTab = "categories" },
                        icon = { Icon(Icons.Default.Category, contentDescription = "Categories") },
                        label = { Text("Categories") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == "languages",
                        onClick = { selectedTab = "languages" },
                        icon = { Icon(Icons.Default.Translate, contentDescription = "Languages") },
                        label = { Text("Languages") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == "dars_classes",
                        onClick = { selectedTab = "dars_classes" },
                        icon = { Icon(Icons.Default.School, contentDescription = "Dars Classes") },
                        label = { Text("Dars Classes") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == "dars_books",
                        onClick = { selectedTab = "dars_books" },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = "Dars Books") },
                        label = { Text("Dars Books") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        selected = false,
                        onClick = { viewModel.adminLogout(); onBack() },
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout") },
                        label = { Text("Logout") }
                    )
                }
            }

            // Main Console Panel
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Library Admin Console (${selectedTab.uppercase()})",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            if (!isWide) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            Text(
                                text = email ?: "Admin",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            if (!isWide) {
                                IconButton(onClick = { viewModel.adminLogout(); onBack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                bottomBar = {
                    // Mobile navigation bar if screen is narrow
                    if (!isWide) {
                        val tabs = listOf(
                            "dashboard" to "Home",
                            "books" to "Books",
                            "volumes" to "Vols",
                            "categories" to "Cats",
                            "languages" to "Langs",
                            "dars_classes" to "Classes",
                            "dars_books" to "Dars Books"
                        )
                        val selectedIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)
                        
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScrollableTabRow(
                                selectedTabIndex = selectedIndex,
                                containerColor = Color.Transparent,
                                edgePadding = 16.dp,
                                modifier = Modifier.navigationBarsPadding()
                            ) {
                                tabs.forEachIndexed { index, pair ->
                                    Tab(
                                        selected = selectedIndex == index,
                                        onClick = { selectedTab = pair.first },
                                        text = { Text(pair.second, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                        icon = {
                                            val icon = when(pair.first) {
                                                "dashboard" -> Icons.Default.Dashboard
                                                "books" -> Icons.Default.Book
                                                "volumes" -> Icons.Default.Layers
                                                "categories" -> Icons.Default.Category
                                                "languages" -> Icons.Default.Translate
                                                "dars_classes" -> Icons.Default.School
                                                "dars_books" -> Icons.Default.MenuBook
                                                else -> Icons.Default.Dashboard
                                            }
                                            Icon(icon, contentDescription = pair.second, modifier = Modifier.size(18.dp))
                                        },
                                        selectedContentColor = MaterialTheme.colorScheme.primary,
                                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (selectedTab) {
                        "dashboard" -> DashboardView(viewModel)
                        "books" -> BookManagementView(viewModel)
                        "volumes" -> VolumeManagementView(viewModel)
                        "categories" -> CategoryManagementView(viewModel)
                        "languages" -> LanguageManagementView(viewModel)
                        "dars_classes" -> DarsClassManagementView(viewModel)
                        "dars_books" -> DarsBookManagementView(viewModel)
                    }
                }
            }
        }
    }
}

// ================= ADMIN DASHBOARD HOMEPAGE =================
@Composable
fun DashboardView(viewModel: LibraryViewModel) {
    val books by viewModel.allBooks.collectAsState()
    val cats by viewModel.categories.collectAsState()
    val langs by viewModel.languages.collectAsState()

    val totalVols = books.sumOf { if (it.totalVolumes <= 0) 1 else it.totalVolumes }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Overview Statistics", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        // Stats Cards Grid Row (Responsive columns)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        title = "Total Books",
                        count = books.size.toString(),
                        icon = Icons.Default.Book,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total Volumes",
                        count = totalVols.toString(),
                        icon = Icons.Default.Layers,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        title = "Categories",
                        count = cats.size.toString(),
                        icon = Icons.Default.Category,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Languages",
                        count = langs.size.toString(),
                        icon = Icons.Default.Translate,
                        color = Color.Magenta,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                var showConfigDialog by remember { mutableStateOf(false) }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Cloud Setup",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Sheets & Drive Setup",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Paste your Google Sheets URL and Google Drive folders here. The app will securely use your API backend to save and retrieve books, covers, and PDFs.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val currentApiUrl by viewModel.apiConfigUrl.collectAsState()
                    val currentSheetId by viewModel.spreadsheetId.collectAsState()
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text("• Web App API: ${if (currentApiUrl.contains("YOUR_APPS_SCRIPT_ID")) "❌ Not Configured (Offline Mode)" else "✅ Configured"}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("• Sheet ID: ${currentSheetId.take(15)}...", fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Config")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configure Google Sheet & Drive URLs")
                    }
                }

                if (showConfigDialog) {
                    CloudConfigDialog(
                        viewModel = viewModel,
                        onDismiss = { showConfigDialog = false }
                    )
                }
            }
        }

        item {
            SectionHeader(title = "Recent Database Books")
        }

        if (books.isEmpty()) {
            item {
                Text("No books saved inside Google Sheets yet. Click the 'Books' tab to add your first record.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
            }
        } else {
            items(books.take(5)) { book ->
                ListItem(
                    headlineContent = { Text(book.bookName, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Author: ${book.author} • ${book.category}") },
                    leadingContent = {
                        AsyncImage(
                            model = Config.getBookCoverUrl(book.coverImage),
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    },
                    trailingContent = {
                        Text(
                            text = "${if (book.totalVolumes <= 0) 1 else book.totalVolumes} Vols",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}

@Composable
fun CloudConfigDialog(
    viewModel: LibraryViewModel,
    onDismiss: () -> Unit
) {
    val currentApiUrl by viewModel.apiConfigUrl.collectAsState()
    val currentSheetId by viewModel.spreadsheetId.collectAsState()
    val currentCovers by viewModel.folderBookCovers.collectAsState()
    val currentThumbnails by viewModel.folderVolumeThumbnails.collectAsState()
    val currentPdfs by viewModel.folderBookPdfs.collectAsState()

    var apiUrlInput by remember { mutableStateOf(currentApiUrl) }
    var sheetIdInput by remember { mutableStateOf(currentSheetId) }
    var coversInput by remember { mutableStateOf(currentCovers) }
    var thumbnailsInput by remember { mutableStateOf(currentThumbnails) }
    var pdfsInput by remember { mutableStateOf(currentPdfs) }

    fun cleanInput(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.contains("/d/") -> {
                val parts = trimmed.split("/d/")
                if (parts.size > 1) parts[1].split("/")[0] else trimmed
            }
            trimmed.contains("/folders/") -> {
                val parts = trimmed.split("/folders/")
                if (parts.size > 1) parts[1].split("?")[0].split("/")[0] else trimmed
            }
            else -> trimmed
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Google Integration Setup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Paste your Google Sheet link/ID and Apps Script Web App link here. The app automatically extracts the correct ID if you paste a full URL!",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                // 1. Apps Script API URL
                Column {
                    Text("Google Apps Script URL (API Link)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    OutlinedTextField(
                        value = apiUrlInput,
                        onValueChange = { apiUrlInput = it },
                        placeholder = { Text("https://script.google.com/...") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Google Sheets URL/ID
                Column {
                    Text("Google Sheet URL or Spreadsheet ID", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    OutlinedTextField(
                        value = sheetIdInput,
                        onValueChange = { sheetIdInput = it },
                        placeholder = { Text("Paste Google Sheet URL/ID here") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 3. Covers Folder ID/URL
                Column {
                    Text("Google Drive Folder: Book Covers", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    OutlinedTextField(
                        value = coversInput,
                        onValueChange = { coversInput = it },
                        placeholder = { Text("Covers Folder URL or ID") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. Thumbnails Folder ID/URL
                Column {
                    Text("Google Drive Folder: Volume Thumbnails", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    OutlinedTextField(
                        value = thumbnailsInput,
                        onValueChange = { thumbnailsInput = it },
                        placeholder = { Text("Thumbnails Folder URL or ID") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 5. PDFs Folder ID/URL
                Column {
                    Text("Google Drive Folder: Book PDFs", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    OutlinedTextField(
                        value = pdfsInput,
                        onValueChange = { pdfsInput = it },
                        placeholder = { Text("PDFs Folder URL or ID") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalApiUrl = apiUrlInput.trim()
                    val finalSheetId = cleanInput(sheetIdInput)
                    val finalCovers = cleanInput(coversInput)
                    val finalThumbnails = cleanInput(thumbnailsInput)
                    val finalPdfs = cleanInput(pdfsInput)

                    viewModel.updateCloudConfig(
                        apiUrl = finalApiUrl,
                        sheetId = finalSheetId,
                        coversFolder = finalCovers,
                        thumbnailsFolder = finalThumbnails,
                        pdfsFolder = finalPdfs
                    )
                    onDismiss()
                }
            ) {
                Text("Save Configuration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StatCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(count, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ================= BOOK MANAGEMENT VIEW =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookManagementView(viewModel: LibraryViewModel) {
    val books by viewModel.allBooks.collectAsState()
    val cats by viewModel.categories.collectAsState()
    val langs by viewModel.languages.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var editBook by remember { mutableStateOf<BookEntity?>(null) }
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    // Form inputs
    var bName by remember { mutableStateOf("") }
    var bAuthor by remember { mutableStateOf("") }
    var bCategory by remember { mutableStateOf("") }
    var bLanguage by remember { mutableStateOf("") }
    var bDesc by remember { mutableStateOf("") }
    var bFeatured by remember { mutableStateOf(false) }
    var bCoverUrl by remember { mutableStateOf("") }
    var bVolCount by remember { mutableStateOf("1") }

    // Local picked file bytes
    var selectedImgBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImgName by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val isUploading by viewModel.adminLoading.collectAsState()
    val progress by viewModel.uploadProgress.collectAsState()
    val statusText by viewModel.uploadStatus.collectAsState()

    // File pick launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                selectedImgName = "cover_${System.currentTimeMillis()}.png"
                selectedImgBytes = context.contentResolver.openInputStream(uri)?.readBytes()
                bCoverUrl = "" // clear pasted link when choosing file
                Toast.makeText(context, "Cover selected!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading image file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showForm) {
        // Render Form Add / Edit
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentEditBook = editBook
                        Text(
                            text = if (currentEditBook == null) "Add Book Entry" else "Edit Book: ${currentEditBook.bookName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showForm = false; editBook = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Form")
                        }
                    }
                }

                // Progress is beautifully managed via popup Dialog below

                item {
                    OutlinedTextField(
                        value = bName,
                        onValueChange = { bName = it },
                        label = { Text("Book Name (Relationship Key)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = bAuthor,
                        onValueChange = { bAuthor = it },
                        label = { Text("Author") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Category dropdown selection from added categories
                item {
                    var catExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = bCategory,
                            onValueChange = { bCategory = it },
                            label = { Text("Category (Select or Type)") },
                            placeholder = { Text("e.g. Hadith, Tafsir") },
                            trailingIcon = {
                                IconButton(onClick = { catExpanded = !catExpanded }) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                                        contentDescription = "Toggle Categories Dropdown"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text("You can also type a custom category name.") }
                        )
                        DropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            cats.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.categoryName) },
                                    onClick = {
                                        bCategory = cat.categoryName
                                        catExpanded = false
                                    }
                                )
                            }
                            if (cats.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No Categories found. Add them in Category panel.") },
                                    enabled = false,
                                    onClick = {}
                                )
                            }
                        }
                    }
                }

                // Language dropdown selection from added languages
                item {
                    var langExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = bLanguage,
                            onValueChange = { bLanguage = it },
                            label = { Text("Language (Select or Type)") },
                            placeholder = { Text("e.g. Arabic, Urdu, English") },
                            trailingIcon = {
                                IconButton(onClick = { langExpanded = !langExpanded }) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                                        contentDescription = "Toggle Languages Dropdown"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text("You can also type a custom language name.") }
                        )
                        DropdownMenu(
                            expanded = langExpanded,
                            onDismissRequest = { langExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            langs.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang.languageName) },
                                    onClick = {
                                        bLanguage = lang.languageName
                                        langExpanded = false
                                    }
                                )
                            }
                            if (langs.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No Languages found. Add them in Language panel.") },
                                    enabled = false,
                                    onClick = {}
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = bVolCount,
                        onValueChange = { bVolCount = it },
                        label = { Text("Total Volumes Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = bDesc,
                        onValueChange = { bDesc = it },
                        label = { Text("Short Description") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = bFeatured, onCheckedChange = { bFeatured = it })
                        Text("Featured Book Highlight")
                    }
                }

                // Image Cover select
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Book Cover Image Source", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = bCoverUrl,
                                onValueChange = { 
                                    bCoverUrl = it 
                                    if (it.trim().isNotEmpty()) {
                                        selectedImgBytes = null
                                        selectedImgName = null
                                    }
                                },
                                label = { Text("Paste Cover Image URL / Link") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("https://example.com/cover.jpg") }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("— OR —", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (selectedImgBytes != null) {
                                val bitmap = remember(selectedImgBytes) {
                                    try {
                                        BitmapFactory.decodeByteArray(selectedImgBytes, 0, selectedImgBytes!!.size)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Cover Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(60.dp, 80.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text("Picked: $selectedImgName", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else if (bCoverUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = Config.getBookCoverUrl(bCoverUrl),
                                    contentDescription = "Cover Source",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp, 80.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            } else {
                                Text("No Cover selected", color = Color.Gray, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { imagePicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Pick")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Image from Device")
                            }
                        }
                    }
                }

                item {
                    GradientButton(
                        text = if (editBook == null) "Save Book" else "Update Book",
                        onClick = {
                            if (bName.trim().isEmpty() || bAuthor.trim().isEmpty() || bCategory.trim().isEmpty() || bLanguage.trim().isEmpty()) {
                                Toast.makeText(context, "Please populate required fields", Toast.LENGTH_SHORT).show()
                                return@GradientButton
                            }

                            val volumeInt = bVolCount.toIntOrNull() ?: 1

                            val bookPayload = Book(
                                bookName = bName.trim(),
                                author = bAuthor.trim(),
                                category = bCategory.trim(),
                                language = bLanguage.trim(),
                                description = bDesc.trim(),
                                coverImage = Config.sanitizeUrl(bCoverUrl),
                                totalVolumes = volumeInt,
                                featured = bFeatured
                            )

                            val targetBook = editBook
                            if (targetBook == null) {
                                viewModel.addBook(bookPayload, selectedImgBytes, selectedImgName) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) showForm = false
                                }
                            } else {
                                viewModel.editBook(targetBook.bookName, bookPayload, selectedImgBytes, selectedImgName) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) {
                                        showForm = false
                                        editBook = null
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    } else {
        // Render List & Add trigger
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Books Repository (${books.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                FloatingActionButton(
                    onClick = {
                        // Reset form variables
                        bName = ""
                        bAuthor = ""
                        bCategory = ""
                        bLanguage = ""
                        bDesc = ""
                        bFeatured = false
                        bCoverUrl = ""
                        bVolCount = "1"
                        selectedImgBytes = null
                        selectedImgName = null
                        editBook = null
                        showForm = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(45.dp).testTag("add_book_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Book", tint = Color.White)
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(books) { book ->
                    ListItem(
                        headlineContent = { Text(book.bookName, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        supportingContent = { Text("Author: ${book.author} • Category: ${book.category}", fontSize = 11.sp) },
                        leadingContent = {
                            AsyncImage(
                                model = Config.getBookCoverUrl(book.coverImage),
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp, 48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {
                                    bName = book.bookName
                                    bAuthor = book.author
                                    bCategory = book.category
                                    bLanguage = book.language
                                    bDesc = book.description
                                    bFeatured = book.featured
                                    bCoverUrl = book.coverImage
                                    bVolCount = book.totalVolumes.toString()
                                    selectedImgBytes = null
                                    selectedImgName = null
                                    editBook = book
                                    showForm = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = {
                                    bookToDelete = book
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    }
    
    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete Book") },
            text = { Text("Are you sure you want to delete '${bookToDelete?.bookName}'? This will also delete all its volumes and cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val book = bookToDelete!!
                        bookToDelete = null
                        viewModel.deleteBook(book.bookName) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    UploadProgressDialog(isUploading = isUploading, progress = progress, statusText = statusText)
}

// ================= VOLUME MANAGEMENT VIEW =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeManagementView(viewModel: LibraryViewModel) {
    val books by viewModel.allBooks.collectAsState()
    val volumes by viewModel.currentBookVolumes.collectAsState()
    val activeBookName by viewModel.selectedBookName.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var editVolNumber by remember { mutableStateOf<Int?>(null) }
    var volumeToDelete by remember { mutableStateOf<VolumeEntity?>(null) }

    // Form states
    var vName by remember { mutableStateOf("") }
    var vNumber by remember { mutableStateOf("1") }
    var vSize by remember { mutableStateOf("4.5 MB") }
    var vThumbUrl by remember { mutableStateOf("") }
    var vPdfUrl by remember { mutableStateOf("") }

    // Dropdown selection state
    var expandedDropdown by remember { mutableStateOf(false) }

    // Selected byte files
    var thumbBytes by remember { mutableStateOf<ByteArray?>(null) }
    var thumbName by remember { mutableStateOf<String?>(null) }

    var pdfBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pdfName by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val isUploading by viewModel.adminLoading.collectAsState()
    val progress by viewModel.uploadProgress.collectAsState()
    val statusText by viewModel.uploadStatus.collectAsState()

    // Select launch files
    val thumbPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            thumbName = "thumb_${System.currentTimeMillis()}.png"
            thumbBytes = context.contentResolver.openInputStream(uri)?.readBytes()
            vThumbUrl = "" // clear pasted url since they chose a file
            Toast.makeText(context, "Thumbnail selected", Toast.LENGTH_SHORT).show()
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pdfName = "vol_${System.currentTimeMillis()}.pdf"
            pdfBytes = context.contentResolver.openInputStream(uri)?.readBytes()
            vPdfUrl = "" // clear pasted url since they chose a file
            Toast.makeText(context, "PDF Document selected", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Book select dropdown row
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = activeBookName ?: "Select Book to view volumes",
                onValueChange = {},
                readOnly = true,
                label = { Text("Active Book Selection") },
                trailingIcon = {
                    IconButton(onClick = { expandedDropdown = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedDropdown = true }
            )
            DropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                books.forEach { book ->
                    DropdownMenuItem(
                        text = { Text(book.bookName) },
                        onClick = {
                            viewModel.selectBook(book.bookName)
                            expandedDropdown = false
                        }
                    )
                }
            }
        }

        if (showForm) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (editVolNumber == null) "Add Book Volume" else "Edit Volume $editVolNumber",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { showForm = false; editVolNumber = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }

                    // Progress is beautifully managed via popup Dialog below

                    item {
                        OutlinedTextField(
                            value = vNumber,
                            onValueChange = { vNumber = it },
                            label = { Text("Volume Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = vName,
                            onValueChange = { vName = it },
                            label = { Text("Volume Name (e.g. Volume 1: Intro)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = vSize,
                            onValueChange = { vSize = it },
                            label = { Text("PDF File Size (e.g. 4.2 MB)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Thumbnail file picker
                    item {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Volume Thumbnail Source", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            OutlinedTextField(
                                value = vThumbUrl,
                                onValueChange = { 
                                    vThumbUrl = it 
                                    if (it.trim().isNotEmpty()) {
                                        thumbBytes = null
                                        thumbName = null
                                    }
                                },
                                label = { Text("Paste Thumbnail Image URL / Link") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("https://example.com/thumbnail.jpg") }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Thumbnail Preview
                                if (thumbBytes != null) {
                                    val bitmap = remember(thumbBytes) {
                                        try {
                                            BitmapFactory.decodeByteArray(thumbBytes, 0, thumbBytes!!.size)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (bitmap != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Thumbnail Preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                    }
                                } else if (vThumbUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = Config.getGoogleDriveImageUrl(vThumbUrl),
                                        contentDescription = "Current Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }

                                Button(onClick = { thumbPicker.launch("image/*") }) {
                                    Text("Choose Image")
                                }
                                Text(
                                    text = if (thumbBytes != null) "Ready: $thumbName" else if (vThumbUrl.isNotEmpty()) "Has URL Link" else "No icon selected",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.Gray,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // PDF document picker
                    item {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Volume PDF File Source", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            OutlinedTextField(
                                value = vPdfUrl,
                                onValueChange = { 
                                    vPdfUrl = it 
                                    if (it.trim().isNotEmpty()) {
                                        pdfBytes = null
                                        pdfName = null
                                    }
                                },
                                label = { Text("Paste PDF Document URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("https://example.com/document.pdf") }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { pdfPicker.launch("application/pdf") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("Choose PDF")
                                }
                                Text(
                                    text = if (pdfBytes != null) "Ready: $pdfName" else if (vPdfUrl.isNotEmpty()) "Has PDF URL Link" else "No document selected",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.Gray,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item {
                        GradientButton(
                            text = if (editVolNumber == null) "Create Volume" else "Update Volume",
                            onClick = {
                                if (vName.trim().isEmpty() || activeBookName == null) {
                                    Toast.makeText(context, "Complete inputs and verify Book selection", Toast.LENGTH_SHORT).show()
                                    return@GradientButton
                                }

                                val numInt = vNumber.toIntOrNull() ?: 1
                                val volPayload = Volume(
                                    bookName = activeBookName!!,
                                    volumeNumber = numInt,
                                    volumeName = vName.trim(),
                                    thumbnail = Config.sanitizeUrl(vThumbUrl),
                                    pdf = Config.sanitizeUrl(vPdfUrl),
                                    fileSize = vSize.trim()
                                )

                                if (editVolNumber == null) {
                                    viewModel.addVolume(volPayload, thumbBytes, thumbName, pdfBytes, pdfName) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (success) showForm = false
                                    }
                                } else {
                                    viewModel.editVolume(activeBookName!!, editVolNumber!!, volPayload, thumbBytes, thumbName, pdfBytes, pdfName) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (success) {
                                            showForm = false
                                            editVolNumber = null
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            // Render active lists
            if (activeBookName == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a Book from the dropdown to manage volumes.", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Volumes of '$activeBookName' (${volumes.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    FloatingActionButton(
                        onClick = {
                            vName = ""
                            vNumber = (volumes.size + 1).toString()
                            vSize = "4.5 MB"
                            vThumbUrl = ""
                            vPdfUrl = ""
                            thumbBytes = null
                            thumbName = null
                            pdfBytes = null
                            pdfName = null
                            editVolNumber = null
                            showForm = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Volume", tint = Color.White)
                    }
                }

                if (volumes.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No volumes uploaded yet. Click '+' to insert a volume.", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(volumes) { volume ->
                            ListItem(
                                headlineContent = { Text(volume.volumeName, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                supportingContent = { Text("Volume Number: ${volume.volumeNumber} • Size: ${volume.fileSize}", fontSize = 11.sp) },
                                leadingContent = {
                                    val resolvedThumb = if (volume.thumbnail.trim().isEmpty()) {
                                        val cImg = books.find { it.bookName == volume.bookName }?.coverImage ?: ""
                                        Config.getBookCoverUrl(cImg)
                                    } else {
                                        Config.getGoogleDriveImageUrl(volume.thumbnail)
                                    }
                                    AsyncImage(
                                        model = resolvedThumb,
                                        contentDescription = "Thumb",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                },
                                trailingContent = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            vName = volume.volumeName
                                            vNumber = volume.volumeNumber.toString()
                                            vSize = volume.fileSize
                                            vThumbUrl = volume.thumbnail
                                            vPdfUrl = volume.pdf
                                            thumbBytes = null
                                            thumbName = null
                                            pdfBytes = null
                                            pdfName = null
                                            editVolNumber = volume.volumeNumber
                                            showForm = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = {
                                            volumeToDelete = volume
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (volumeToDelete != null) {
        AlertDialog(
            onDismissRequest = { volumeToDelete = null },
            title = { Text("Delete Volume") },
            text = { Text("Are you sure you want to delete Volume ${volumeToDelete?.volumeNumber} ('${volumeToDelete?.volumeName}')? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val volume = volumeToDelete!!
                        volumeToDelete = null
                        viewModel.deleteVolume(activeBookName!!, volume.volumeNumber) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { volumeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    UploadProgressDialog(isUploading = isUploading, progress = progress, statusText = statusText)
}

// ================= CATEGORY MANAGEMENT VIEW =================
@Composable
fun CategoryManagementView(viewModel: LibraryViewModel) {
    val categories by viewModel.categories.collectAsState()
    var newCatName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val loading by viewModel.adminLoading.collectAsState()
    var catToDelete by remember { mutableStateOf<Category?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add New Topic Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Category Name (e.g. Fiqh, Aqeedah)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    GradientButton(
                        text = "Save Category",
                        onClick = {
                            if (newCatName.trim().isEmpty()) {
                                Toast.makeText(context, "Populate name", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addCategory(newCatName.trim()) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) newCatName = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        SectionHeader(title = "Existing Database Categories")

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(categories) { cat ->
                ListItem(
                    headlineContent = { Text(cat.categoryName, fontWeight = FontWeight.Bold) },
                    trailingContent = {
                        IconButton(onClick = {
                            catToDelete = cat
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }

    if (catToDelete != null) {
        AlertDialog(
            onDismissRequest = { catToDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete the category '${catToDelete?.categoryName}'? This will clear the category field from any books assigned to it.") },
            confirmButton = {
                Button(
                    onClick = {
                        val cat = catToDelete!!
                        catToDelete = null
                        viewModel.deleteCategory(cat.categoryName) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { catToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ================= LANGUAGE MANAGEMENT VIEW =================
@Composable
fun LanguageManagementView(viewModel: LibraryViewModel) {
    val languages by viewModel.languages.collectAsState()
    var newLangName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val loading by viewModel.adminLoading.collectAsState()
    var langToDelete by remember { mutableStateOf<Language?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add New Translation Language", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(
                    value = newLangName,
                    onValueChange = { newLangName = it },
                    label = { Text("Language Name (e.g. Arabic, Urdu)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    GradientButton(
                        text = "Save Language",
                        onClick = {
                            if (newLangName.trim().isEmpty()) {
                                Toast.makeText(context, "Populate language name", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addLanguage(newLangName.trim()) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) newLangName = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        SectionHeader(title = "Existing Translation Languages")

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(languages) { lang ->
                ListItem(
                    headlineContent = { Text(lang.languageName, fontWeight = FontWeight.Bold) },
                    trailingContent = {
                        IconButton(onClick = {
                            langToDelete = lang
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }

    if (langToDelete != null) {
        AlertDialog(
            onDismissRequest = { langToDelete = null },
            title = { Text("Delete Language") },
            text = { Text("Are you sure you want to delete the language '${langToDelete?.languageName}'? This will clear the language field from any books or dars books assigned to it.") },
            confirmButton = {
                Button(
                    onClick = {
                        val lang = langToDelete!!
                        langToDelete = null
                        viewModel.deleteLanguage(lang.languageName) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { langToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ================= UPLOAD PROGRESS DIALOG =================
@Composable
fun UploadProgressDialog(
    isUploading: Boolean,
    progress: Int?,
    statusText: String?
) {
    if (isUploading && progress != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {},
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = (progress) / 100f,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 6.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                        Text(
                            text = "$progress%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = statusText ?: "Uploading files...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Please do not close or minimize the application",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

// ================= DARS CLASS MANAGEMENT VIEW =================
@Composable
fun DarsClassManagementView(viewModel: LibraryViewModel) {
    val classes by viewModel.darsClasses.collectAsState()
    var newClassName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val loading by viewModel.adminLoading.collectAsState()
    var classToDelete by remember { mutableStateOf<DarsClass?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add New Dars Class / Grade", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(
                    value = newClassName,
                    onValueChange = { newClassName = it },
                    label = { Text("Class Name (e.g. Al-Saff al-Awwal, Class 1)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    GradientButton(
                        text = "Save Class",
                        onClick = {
                            if (newClassName.trim().isEmpty()) {
                                Toast.makeText(context, "Populate class name", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addDarsClass(newClassName.trim()) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) newClassName = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        SectionHeader(title = "Existing Dars Classes")

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(classes) { cls ->
                ListItem(
                    headlineContent = { Text(cls.className, fontWeight = FontWeight.Bold) },
                    trailingContent = {
                        IconButton(onClick = {
                            classToDelete = cls
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }

    if (classToDelete != null) {
        AlertDialog(
            onDismissRequest = { classToDelete = null },
            title = { Text("Delete Dars Class") },
            text = { Text("Are you sure you want to delete the class '${classToDelete?.className}'? This will clear the class field from any dars books assigned to it.") },
            confirmButton = {
                Button(
                    onClick = {
                        val cls = classToDelete!!
                        classToDelete = null
                        viewModel.deleteDarsClass(cls.className) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { classToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ================= DARS BOOK MANAGEMENT VIEW =================
@Composable
fun DarsBookManagementView(viewModel: LibraryViewModel) {
    val books by viewModel.allDarsBooks.collectAsState()
    val classes by viewModel.darsClasses.collectAsState()
    val langs by viewModel.languages.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var editBook by remember { mutableStateOf<com.example.data.local.DarsBookEntity?>(null) }
    var bookToDelete by remember { mutableStateOf<com.example.data.local.DarsBookEntity?>(null) }

    // Form inputs
    var bName by remember { mutableStateOf("") }
    var bAuthor by remember { mutableStateOf("") }
    var bClass by remember { mutableStateOf("") }
    var bLanguage by remember { mutableStateOf("") }
    var bDesc by remember { mutableStateOf("") }
    var bFeatured by remember { mutableStateOf(false) }
    var bCoverUrl by remember { mutableStateOf("") }
    var bVolCount by remember { mutableStateOf("1") }

    // Local picked file bytes
    var selectedImgBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImgName by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val isUploading by viewModel.adminLoading.collectAsState()
    val progress by viewModel.uploadProgress.collectAsState()
    val statusText by viewModel.uploadStatus.collectAsState()

    // File pick launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                selectedImgName = "dars_cover_${System.currentTimeMillis()}.png"
                selectedImgBytes = context.contentResolver.openInputStream(uri)?.readBytes()
                bCoverUrl = "" // clear pasted link when choosing file
                Toast.makeText(context, "Cover selected!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading image file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showForm) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentEditBook = editBook
                        Text(
                            text = if (currentEditBook == null) "Add Dars Book" else "Edit Dars Book: ${currentEditBook.bookName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showForm = false; editBook = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Form")
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = bName,
                        onValueChange = { bName = it },
                        label = { Text("Book Name (Relationship Key)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = bAuthor,
                        onValueChange = { bAuthor = it },
                        label = { Text("Author") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Dars Class dropdown selection from manually added classes
                item {
                    var classExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = bClass,
                            onValueChange = { bClass = it },
                            label = { Text("Dars Class (Select or Type)") },
                            placeholder = { Text("e.g. Al-Saff al-Awwal, Class 1") },
                            trailingIcon = {
                                IconButton(onClick = { classExpanded = !classExpanded }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Toggle Classes Dropdown"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text("Choose a manually managed Dars class.") }
                        )
                        DropdownMenu(
                            expanded = classExpanded,
                            onDismissRequest = { classExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            classes.forEach { cls ->
                                DropdownMenuItem(
                                    text = { Text(cls.className) },
                                    onClick = {
                                        bClass = cls.className
                                        classExpanded = false
                                    }
                                )
                            }
                            if (classes.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No Classes found. Add them in Dars Classes panel.") },
                                    enabled = false,
                                    onClick = {}
                                )
                            }
                        }
                    }
                }

                // Language dropdown selection
                item {
                    var langExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = bLanguage,
                            onValueChange = { bLanguage = it },
                            label = { Text("Language (Select or Type)") },
                            trailingIcon = {
                                IconButton(onClick = { langExpanded = !langExpanded }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Toggle Languages Dropdown"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = langExpanded,
                            onDismissRequest = { langExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            langs.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang.languageName) },
                                    onClick = {
                                        bLanguage = lang.languageName
                                        langExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = bVolCount,
                        onValueChange = { bVolCount = it },
                        label = { Text("Total Volumes Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = bDesc,
                        onValueChange = { bDesc = it },
                        label = { Text("Short Description") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = bFeatured, onCheckedChange = { bFeatured = it })
                        Text("Featured Highlight in Dars Shelf")
                    }
                }

                // Image Cover select
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Book Cover Image Source", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = bCoverUrl,
                                onValueChange = { 
                                    bCoverUrl = it 
                                    if (it.trim().isNotEmpty()) {
                                        selectedImgBytes = null
                                        selectedImgName = null
                                    }
                                },
                                label = { Text("Paste Cover Image URL / Link") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("https://example.com/cover.jpg") }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("— OR —", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (selectedImgBytes != null) {
                                val bitmap = remember(selectedImgBytes) {
                                    try {
                                        BitmapFactory.decodeByteArray(selectedImgBytes, 0, selectedImgBytes!!.size)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Cover Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(60.dp, 80.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text("Picked: $selectedImgName", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else if (bCoverUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = Config.getBookCoverUrl(bCoverUrl),
                                    contentDescription = "Cover Source",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp, 80.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            } else {
                                Text("No Cover selected", color = Color.Gray, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { imagePicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Pick")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Image from Device")
                            }
                        }
                    }
                }

                item {
                    GradientButton(
                        text = if (editBook == null) "Save Dars Book" else "Update Dars Book",
                        onClick = {
                            if (bName.trim().isEmpty() || bAuthor.trim().isEmpty() || bClass.trim().isEmpty() || bLanguage.trim().isEmpty()) {
                                Toast.makeText(context, "Please populate required fields", Toast.LENGTH_SHORT).show()
                                return@GradientButton
                            }

                            val volumeInt = bVolCount.toIntOrNull() ?: 1

                            val bookPayload = com.example.data.model.DarsBook(
                                bookName = bName.trim(),
                                author = bAuthor.trim(),
                                darsClass = bClass.trim(),
                                language = bLanguage.trim(),
                                description = bDesc.trim(),
                                coverImage = Config.sanitizeUrl(bCoverUrl),
                                totalVolumes = volumeInt,
                                featured = bFeatured
                            )

                            val targetBook = editBook
                            if (targetBook == null) {
                                viewModel.addDarsBook(bookPayload, selectedImgBytes, selectedImgName) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) showForm = false
                                }
                            } else {
                                viewModel.editDarsBook(targetBook.bookName, bookPayload, selectedImgBytes, selectedImgName) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) {
                                        showForm = false
                                        editBook = null
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dars-e-Nizami books (${books.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                FloatingActionButton(
                    onClick = {
                        bName = ""
                        bAuthor = ""
                        bClass = ""
                        bLanguage = ""
                        bDesc = ""
                        bFeatured = false
                        bCoverUrl = ""
                        bVolCount = "1"
                        selectedImgBytes = null
                        selectedImgName = null
                        editBook = null
                        showForm = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(45.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Dars Book", tint = Color.White)
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(books) { book ->
                    ListItem(
                        headlineContent = { Text(book.bookName, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Author: ${book.author} • ${book.darsClass} • ${book.language}") },
                        leadingContent = {
                            AsyncImage(
                                model = Config.getBookCoverUrl(book.coverImage),
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    bName = book.bookName
                                    bAuthor = book.author
                                    bClass = book.darsClass
                                    bLanguage = book.language
                                    bDesc = book.description
                                    bFeatured = book.featured
                                    bCoverUrl = book.coverImage
                                    bVolCount = book.totalVolumes.toString()
                                    selectedImgBytes = null
                                    selectedImgName = null
                                    editBook = book
                                    showForm = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    bookToDelete = book
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    }
    
    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete Dars Book") },
            text = { Text("Are you sure you want to delete '${bookToDelete?.bookName}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val book = bookToDelete!!
                        bookToDelete = null
                        viewModel.deleteDarsBook(book.bookName) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    UploadProgressDialog(isUploading = isUploading, progress = progress, statusText = statusText)
}
