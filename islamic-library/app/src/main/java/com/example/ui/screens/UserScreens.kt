@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.local.BookEntity
import com.example.data.local.DarsBookEntity
import com.example.data.local.VolumeEntity
import com.example.data.model.DarsClass
import com.example.ui.components.DecorativeBgCircle
import com.example.ui.components.GradientButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShimmerItem
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.GoldDark
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import com.example.config.Config
import com.example.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.utils.PdfDownloader
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.rememberLazyListState

import com.example.R

// ================= SPLASH SCREEN =================
@Composable
fun SplashScreen(onNavigateHome: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Run animations in parallel
        delay(100)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000)
        )
        // Hold splash screen for 1.5 seconds and then navigate
        delay(1500)
        onNavigateHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative backdrop
        DecorativeBgCircle(Color.White, 350.dp, Modifier.align(Alignment.Center))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(2.dp, Color(0xFFD4AF37), RoundedCornerShape(32.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_launcher_foreground_logo_1784082516479),
                    contentDescription = "App Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Al-Falah Library",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Knowledge, Faith & Light",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

// ================= BOTTOM NAVIGATION COMPONENT =================
@Composable
fun LibraryBottomNavigation(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToDars: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), // subtle tint like bg-[#F1F5EB]
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = onNavigateToHome,
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        )
        NavigationBarItem(
            selected = currentRoute == "dars",
            onClick = onNavigateToDars,
            icon = { Icon(Icons.Default.School, contentDescription = "Dars-e-Nizami") },
            label = { Text("Dars", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        )
        NavigationBarItem(
            selected = currentRoute == "favorites",
            onClick = onNavigateToFavorites,
            icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
            label = { Text("Favorites", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = onNavigateToSettings,
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        )
    }
}

// ================= HOME SCREEN =================
@Composable
fun HomeScreen(
    viewModel: LibraryViewModel,
    onNavigateToBook: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToDars: () -> Unit
) {
    val books by viewModel.filteredBooks.collectAsState()
    val featured by viewModel.featuredBooks.collectAsState()
    val recentlyViewed by viewModel.recentlyViewed.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val languages by viewModel.languages.collectAsState()

    val loading by viewModel.booksLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val selectedLang by viewModel.selectedLanguage.collectAsState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Islamic Library",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Expand your digital library",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            LibraryBottomNavigation(
                currentRoute = "home",
                onNavigateToHome = {},
                onNavigateToDars = onNavigateToDars,
                onNavigateToFavorites = onNavigateToFavorites,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search & Filter Component
            item {
                SearchBarComponent(
                    query = searchQuery,
                    onQueryChange = { viewModel.setQuery(it) }
                )
                
                FilterChipsComponent(
                    categories = categories.map { it.categoryName },
                    languages = languages.map { it.languageName },
                    selectedCategory = selectedCat,
                    selectedLanguage = selectedLang,
                    onSelectCategory = { viewModel.selectCategory(it) },
                    onSelectLanguage = { viewModel.selectLanguage(it) }
                )
            }

            // Sync/Loading or Empty States
            if (loading && books.isEmpty()) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Synchronizing offline cache...", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerItem(height = 180.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        ShimmerItem(height = 100.dp)
                    }
                }
            } else if (books.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "No results",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No books match your criteria.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Try modifying filters or search query.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // Featured Books Slider (Only visible if no search filter is active)
                if (searchQuery.isEmpty() && selectedCat == null && selectedLang == null && featured.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Featured Books")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(featured) { book ->
                                FeaturedBookCard(book = book, onClick = { onNavigateToBook(book.bookName) })
                            }
                        }
                    }
                }

                // Recently Viewed Shelf (Only visible if no search is active)
                if (searchQuery.isEmpty() && selectedCat == null && selectedLang == null && recentlyViewed.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recently Viewed",
                            actionText = "Clear",
                            onActionClick = { viewModel.clearSearchHistory() }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentlyViewed) { book ->
                                RecentBookCard(book = book, onClick = { onNavigateToBook(book.bookName) })
                            }
                        }
                    }
                }

                // All Books Grid / List Section
                item {
                    val headerText = if (selectedCat != null || selectedLang != null || searchQuery.isNotEmpty()) {
                        "Search Results (${books.size})"
                    } else {
                        "All Available Books"
                    }
                    SectionHeader(title = headerText)
                }

                items(books) { book ->
                    BookListCard(book = book, onClick = { onNavigateToBook(book.bookName) })
                }
            }
        }
    }
}

// ================= SEARCH & CHIP COMPONENTS =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarComponent(query: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search books, authors, topics...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("search_input")
        )
    }
}

@Composable
fun PrestigiousFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) {
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface
            )
        )
    }
    
    val borderStroke = if (selected) {
        BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    }
    
    val textColor = if (selected) {
        MaterialTheme.colorScheme.secondary // Golden accent text on selected
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .border(borderStroke, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
fun FilterChipsComponent(
    categories: List<String>,
    languages: List<String>,
    selectedCategory: String?,
    selectedLanguage: String?,
    onSelectCategory: (String?) -> Unit,
    onSelectLanguage: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Filters & Classification",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = if (expanded) "Hide ▲" else "Show ▼",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // All Categories & All Languages on ONE line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrestigiousVerticalFilterItem(
                        selected = selectedCategory == null,
                        label = "All Categories",
                        onClick = { onSelectCategory(null) },
                        modifier = Modifier.weight(1f)
                    )
                    PrestigiousVerticalFilterItem(
                        selected = selectedLanguage == null,
                        label = "All Languages",
                        onClick = { onSelectLanguage(null) },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))

                Text(
                    text = "Select Category",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                categories.forEach { cat ->
                    PrestigiousVerticalFilterItem(
                        selected = selectedCategory == cat,
                        label = cat,
                        onClick = { onSelectCategory(cat) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))

                Text(
                    text = "Select Language",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                languages.forEach { lang ->
                    PrestigiousVerticalFilterItem(
                        selected = selectedLanguage == lang,
                        label = lang,
                        onClick = { onSelectLanguage(lang) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // Short summary when collapsed
            val catText = selectedCategory ?: "All Categories"
            val langText = selectedLanguage ?: "All Languages"
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Active: $catText • $langText",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun DarsFilterChipsComponent(
    classes: List<String>,
    languages: List<String>,
    selectedClass: String?,
    selectedLanguage: String?,
    onSelectClass: (String?) -> Unit,
    onSelectLanguage: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Classes",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dars Classes & Filters",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = if (expanded) "Hide ▲" else "Show ▼",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // All Classes & All Languages on ONE line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrestigiousVerticalFilterItem(
                        selected = selectedClass == null,
                        label = "All Classes",
                        onClick = { onSelectClass(null) },
                        modifier = Modifier.weight(1f)
                    )
                    PrestigiousVerticalFilterItem(
                        selected = selectedLanguage == null,
                        label = "All Languages",
                        onClick = { onSelectLanguage(null) },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))

                Text(
                    text = "Select Class",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                classes.forEach { cls ->
                    PrestigiousVerticalFilterItem(
                        selected = selectedClass == cls,
                        label = cls,
                        onClick = { onSelectClass(cls) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))

                Text(
                    text = "Select Language",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                languages.forEach { lang ->
                    PrestigiousVerticalFilterItem(
                        selected = selectedLanguage == lang,
                        label = lang,
                        onClick = { onSelectLanguage(lang) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // Short summary when collapsed
            val classText = selectedClass ?: "All Classes"
            val langText = selectedLanguage ?: "All Languages"
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Active Class: $classText • $langText",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun PrestigiousVerticalFilterItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }
    
    val textColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    }

    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = fontWeight,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ================= DARS-E-NIZAMI SCREEN =================
@Composable
fun DarsScreen(
    viewModel: LibraryViewModel,
    onNavigateToBook: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFavorites: () -> Unit
) {
    val books by viewModel.filteredDarsBooks.collectAsState()
    val featured by viewModel.featuredDarsBooks.collectAsState()
    val recentlyViewed by viewModel.recentlyViewedDars.collectAsState()
    val classes by viewModel.darsClasses.collectAsState()
    val languages by viewModel.languages.collectAsState()

    val loading by viewModel.darsBooksLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedClass by viewModel.selectedDarsClass.collectAsState()
    val selectedLang by viewModel.selectedDarsLanguage.collectAsState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Dars-e-Nizami",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Comprehensive Islamic curriculum courses",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            LibraryBottomNavigation(
                currentRoute = "dars",
                onNavigateToHome = {
                    viewModel.selectDarsClass(null)
                    viewModel.selectDarsLanguage(null)
                    onNavigateToHome()
                },
                onNavigateToDars = {},
                onNavigateToFavorites = onNavigateToFavorites,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search & Filter Component
            item {
                SearchBarComponent(
                    query = searchQuery,
                    onQueryChange = { viewModel.setQuery(it) }
                )
                
                DarsFilterChipsComponent(
                    classes = classes.map { it.className },
                    languages = languages.map { it.languageName },
                    selectedClass = selectedClass,
                    selectedLanguage = selectedLang,
                    onSelectClass = { viewModel.selectDarsClass(it) },
                    onSelectLanguage = { viewModel.selectDarsLanguage(it) }
                )
            }

            // Sync/Loading or Empty States
            if (loading && books.isEmpty()) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Synchronizing Dars-e-Nizami books...", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerItem(height = 180.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        ShimmerItem(height = 100.dp)
                    }
                }
            } else if (books.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "No results",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No books match your criteria.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Try modifying filters or search query.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // Featured Books Slider (Only visible if no search filter is active)
                if (searchQuery.isEmpty() && selectedClass == null && selectedLang == null && featured.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Featured Books")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(featured) { book ->
                                FeaturedBookCard(
                                    book = BookEntity(
                                        bookName = book.bookName,
                                        author = book.author,
                                        category = book.darsClass,
                                        language = book.language,
                                        description = book.description,
                                        coverImage = book.coverImage,
                                        totalVolumes = book.totalVolumes,
                                        featured = book.featured,
                                        isFavorite = book.isFavorite,
                                        lastViewedTime = book.lastViewedTime
                                    ),
                                    onClick = { onNavigateToBook(book.bookName) }
                                )
                            }
                        }
                    }
                }

                // Recently Viewed Shelf (Only visible if no search is active)
                if (searchQuery.isEmpty() && selectedClass == null && selectedLang == null && recentlyViewed.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recently Viewed",
                            actionText = "Clear",
                            onActionClick = { viewModel.clearSearchHistory() }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentlyViewed) { book ->
                                RecentBookCard(
                                    book = BookEntity(
                                        bookName = book.bookName,
                                        author = book.author,
                                        category = book.darsClass,
                                        language = book.language,
                                        description = book.description,
                                        coverImage = book.coverImage,
                                        totalVolumes = book.totalVolumes,
                                        featured = book.featured,
                                        isFavorite = book.isFavorite,
                                        lastViewedTime = book.lastViewedTime
                                    ),
                                    onClick = { onNavigateToBook(book.bookName) }
                                )
                            }
                        }
                    }
                }

                // All Books Grid / List Section
                item {
                    val activeClass = selectedClass ?: "All Curriculum"
                    SectionHeader(title = "Books in $activeClass")
                }

                items(books) { book ->
                    BookListCard(
                        book = BookEntity(
                            bookName = book.bookName,
                            author = book.author,
                            category = book.darsClass,
                            language = book.language,
                            description = book.description,
                            coverImage = book.coverImage,
                            totalVolumes = book.totalVolumes,
                            featured = book.featured,
                            isFavorite = book.isFavorite,
                            lastViewedTime = book.lastViewedTime
                        ),
                        onClick = { onNavigateToBook(book.bookName) }
                    )
                }
            }
        }
    }
}

// ================= BOOK CARDS =================
@Composable
fun FeaturedBookCard(book: BookEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(320.dp)
            .height(180.dp)
            .clickable { onClick() }
            .testTag("featured_book_card_${book.bookName}"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary), // Beautiful gold border
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            Color(0xFF0D1E3A) // Deep Ocean Navy core
                        )
                    )
                )
        ) {
            // Background Decorative Layers (Asymmetry & Depth)
            Box(
                modifier = Modifier
                    .offset(x = 180.dp, y = 30.dp)
                    .size(150.dp, 200.dp)
                    .graphicsLayer(rotationZ = 12f)
                    .background(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                        RoundedCornerShape(12.dp)
                    )
            )
            
            // The actual cover image layered on top with a golden frame
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-16).dp, y = 0.dp)
                    .graphicsLayer(rotationZ = 4f)
                    .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = Config.getBookCoverUrl(book.coverImage),
                    contentDescription = book.bookName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(90.dp, 130.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            // Foreground Content
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(185.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "FEATURED LITERARY WORK",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary, // Gilded accent
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.bookName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Read Work", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecentBookCard(book: BookEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.Start
    ) {
        AsyncImage(
            model = Config.getBookCoverUrl(book.coverImage),
            contentDescription = book.bookName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(100.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = book.bookName,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = book.author,
            fontSize = 10.sp,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BookListCard(book: BookEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("book_card_${book.bookName}"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)), // Gorgeous thin gold border
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), RoundedCornerShape(8.dp)) // Gold framed cover
            ) {
                AsyncImage(
                    model = Config.getBookCoverUrl(book.coverImage),
                    contentDescription = book.bookName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 72.dp, height = 96.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = book.bookName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (book.featured) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Featured",
                            tint = GoldDark,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 2.dp)
                        )
                    }
                }
                Text(
                    text = "By ${book.author}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.description,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(0.08f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = book.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(0.12f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(0.5.dp, MaterialTheme.colorScheme.secondary.copy(0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = book.language,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(0.04f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${if (book.totalVolumes <= 0) 1 else book.totalVolumes} Vols",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// ================= BOOK DETAILS SCREEN =================
@Composable
fun BookDetailsScreen(
    viewModel: LibraryViewModel,
    bookName: String,
    onBack: () -> Unit,
    onOpenPdfReader: (String, String) -> Unit // volumeKey/Name, pdfUrl
) {
    val context = LocalContext.current
    val bookState by viewModel.currentBook.collectAsState()
    val volumesState by viewModel.currentBookVolumes.collectAsState()
    val bookmarkedVols by viewModel.bookmarkedVolumes.collectAsState()
    val volsLoading by viewModel.volumesLoading.collectAsState()

    LaunchedEffect(bookName) {
        viewModel.selectBook(bookName)
    }

    val book = bookState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("details_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (book != null) {
                        val isFav = book.isFavorite
                        IconButton(onClick = {
                            viewModel.toggleFavorite(book.bookName, !isFav)
                            Toast.makeText(context, if (!isFav) "Added to Favorites!" else "Removed from Favorites!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (isFav) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, book.bookName)
                                putExtra(Intent.EXTRA_TEXT, "Read '${book.bookName}' by ${book.author} in the Islamic Library application!")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Book"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Book")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (book == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Book Info Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = Config.getBookCoverUrl(book.coverImage),
                            contentDescription = book.bookName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(150.dp)
                                .height(210.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = book.bookName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Author: ${book.author}",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestionChip(onClick = {}, label = { Text(book.category) })
                            SuggestionChip(onClick = {}, label = { Text(book.language) })
                            SuggestionChip(onClick = {}, label = { Text("${if (book.totalVolumes <= 0) 1 else book.totalVolumes} Volumes") })
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Description",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = book.description,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Volume Header
                item {
                    SectionHeader(title = "All Volumes")
                }

                if (volsLoading) {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                } else if (volumesState.isEmpty()) {
                    item {
                        Text(
                            text = "No volumes uploaded yet. Admin must add volumes.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    items(volumesState) { volume ->
                        val isBookmarked = bookmarkedVols.contains("${volume.bookName}_${volume.volumeNumber}")
                        VolumeRowItem(
                            volume = volume,
                            isBookmarked = isBookmarked,
                            onRead = { onOpenPdfReader(volume.volumeName, volume.pdf) },
                            onBookmarkToggle = { viewModel.toggleVolumeBookmark("${volume.bookName}_${volume.volumeNumber}") },
                            fallbackThumbnailUrl = Config.getBookCoverUrl(book.coverImage)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VolumeRowItem(
    volume: VolumeEntity,
    isBookmarked: Boolean,
    onRead: () -> Unit,
    onBookmarkToggle: () -> Unit,
    fallbackThumbnailUrl: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't dismiss during active download */ },
            title = {
                Text(
                    text = "Downloading PDF",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = volume.volumeName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(90.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = downloadProgress / 100f,
                            modifier = Modifier.size(80.dp),
                            strokeWidth = 5.dp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "$downloadProgress%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Saving to Download/Islamic Library...",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {}
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("volume_card_${volume.volumeNumber}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val resolvedThumbnail = if (volume.thumbnail.trim().isEmpty()) fallbackThumbnailUrl else volume.thumbnail
            AsyncImage(
                model = Config.getGoogleDriveImageUrl(resolvedThumbnail),
                contentDescription = "Thumb",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = volume.volumeName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Size: ${volume.fileSize} • Vol: ${volume.volumeNumber}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onBookmarkToggle) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark Volume",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                IconButton(onClick = {
                    scope.launch {
                        PdfDownloader.downloadAndSharePdf(
                            context = context,
                            bookName = volume.bookName ?: "Book",
                            volumeName = volume.volumeName,
                            volumeNumber = volume.volumeNumber,
                            pdfUrl = volume.pdf,
                            onStart = {
                                Toast.makeText(context, "Preparing file to share...", Toast.LENGTH_SHORT).show()
                            },
                            onFinished = { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = {
                    showDownloadDialog = true
                    downloadProgress = 0
                    scope.launch {
                        val (uri, file) = PdfDownloader.downloadPdf(
                            context = context,
                            bookName = volume.bookName ?: "Book",
                            volumeName = volume.volumeName,
                            volumeNumber = volume.volumeNumber,
                            pdfUrl = volume.pdf,
                            onProgress = { progress ->
                                downloadProgress = progress
                            }
                        )
                        showDownloadDialog = false
                        if (uri != null) {
                            Toast.makeText(context, "Successfully downloaded to Download/Islamic Library folder!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Download failed. Please try again.", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = onRead,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .size(36.dp)
                        .testTag("volume_read_button_${volume.volumeNumber}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Read PDF",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ================= PDF VIEWER SCREEN =================
@Composable
fun PdfPageItem(
    renderer: PdfRenderer,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                val page = renderer.openPage(pageIndex)
                
                // 1.5x scale is the perfect balance for extreme sharpness without heavy memory usage
                val scale = 1.5f
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()
                
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                
                // Force background to solid white so dark themes do not render transparent PDFs with black-on-black text
                val canvas = android.graphics.Canvas(bmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap = bmp
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Explicitly recycle the bitmap when offscreen to avoid memory pressure on low-end devices
    DisposableEffect(pageIndex) {
        onDispose {
            try {
                bitmap?.recycle()
                bitmap = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (bitmap != null) bitmap!!.width.toFloat() / bitmap!!.height.toFloat() else 0.707f)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF10B981), // Emerald
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Loading Page ${pageIndex + 1}...",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PdfViewerScreen(
    volumeName: String,
    pdfUrl: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var pdfLoadError by remember { mutableStateOf<String?>(null) }
    
    // PdfRenderer local states
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var totalPages by remember { mutableStateOf(0) }
    var cacheFile by remember { mutableStateOf<File?>(null) }
    
    val listState = rememberLazyListState()
    val currentPage by remember {
        derivedStateOf {
            if (totalPages > 0) listState.firstVisibleItemIndex + 1 else 1
        }
    }

    // Load PDF locally using our high speed download & cache system
    LaunchedEffect(pdfUrl) {
        isDownloading = true
        pdfLoadError = null
        try {
            val file = PdfDownloader.getCachedPdfFile(
                context = context,
                bookName = "Al-Falah",
                volumeName = volumeName,
                volumeNumber = 1,
                pdfUrl = pdfUrl,
                onProgress = { progress ->
                    downloadProgress = progress
                }
            )
            if (file != null && file.exists()) {
                cacheFile = file
                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pfd = descriptor
                val renderer = PdfRenderer(descriptor)
                pdfRenderer = renderer
                totalPages = renderer.pageCount
            } else {
                pdfLoadError = "Could not load this book. Please verify your internet connection."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            pdfLoadError = "Error loading PDF locally: ${e.message}"
        } finally {
            isDownloading = false
        }
    }

    // Clean up file descriptors and native resources when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                pfd?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(volumeName, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = if (totalPages > 0) "Offline Reader • $totalPages Pages" else "Offline Reader",
                            fontSize = 10.sp,
                            color = Color(0xFF10B981) // Emerald Green Accent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("pdf_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Manual Save/Download to phone Downloads folder
                    IconButton(onClick = {
                        scope.launch {
                            Toast.makeText(context, "Saving book to your device...", Toast.LENGTH_SHORT).show()
                            val (uri, _) = PdfDownloader.downloadPdf(
                                context = context,
                                bookName = "Al-Falah Library",
                                volumeName = volumeName,
                                volumeNumber = 1,
                                pdfUrl = pdfUrl
                            )
                            if (uri != null) {
                                Toast.makeText(context, "Saved successfully to Downloads/Islamic Library!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Download failed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Download Book", tint = Color.White)
                    }
                    
                    // Share Book Link
                    IconButton(onClick = {
                        try {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Read and study '$volumeName' in Al-Falah Library app! Link: $pdfUrl")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Book Link"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Link", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A), // Premium Dark Slate Blue
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF070B11)), // Elegant extra dark background
            contentAlignment = Alignment.Center
        ) {
            when {
                isDownloading -> {
                    // Premium, elegant full-screen loader with progress percentage
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { downloadProgress / 100f },
                                color = Color(0xFF10B981), // Emerald
                                strokeWidth = 5.dp,
                                modifier = Modifier.size(90.dp)
                            )
                            Text(
                                text = "$downloadProgress%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Loading Book...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Opening your book directly on your screen. This only runs once; future openings will be instant!",
                            fontSize = 12.sp,
                            color = Color.LightGray.copy(alpha = 0.8f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
                pdfLoadError != null -> {
                    // Beautiful error panel with fallbacks
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Failed to load book",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pdfLoadError ?: "Unknown Error",
                            fontSize = 13.sp,
                            color = Color.LightGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Retries downloading
                                    isDownloading = true
                                    pdfLoadError = null
                                    scope.launch {
                                        try {
                                            val file = PdfDownloader.getCachedPdfFile(
                                                context = context,
                                                bookName = "Al-Falah",
                                                volumeName = volumeName,
                                                volumeNumber = 1,
                                                pdfUrl = pdfUrl,
                                                onProgress = { downloadProgress = it }
                                            )
                                            if (file != null && file.exists()) {
                                                cacheFile = file
                                                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                                                pfd = descriptor
                                                pdfRenderer = PdfRenderer(descriptor)
                                                totalPages = pdfRenderer!!.pageCount
                                            } else {
                                                pdfLoadError = "Could not load the PDF file. Please try again."
                                            }
                                        } catch (e: Exception) {
                                            pdfLoadError = e.message
                                        } finally {
                                            isDownloading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Retry", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(pdfUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Text("Open in Browser", color = Color.White)
                            }
                        }
                    }
                }
                pdfRenderer != null -> {
                    // Smooth scrolling native canvas
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(totalPages) { pageIndex ->
                                PdfPageItem(
                                    renderer = pdfRenderer!!,
                                    pageIndex = pageIndex,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Floating Premium Page Indicator Pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                                .background(Color(0xE60D1E16), RoundedCornerShape(24.dp)) // Deep emerald dark semi-transparent
                                .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f), RoundedCornerShape(24.dp)) // Gold stroke
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Page $currentPage of $totalPages",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.testTag("pdf_page_indicator")
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================= FAVORITES SCREEN =================
@Composable
fun FavoritesScreen(
    viewModel: LibraryViewModel,
    onNavigateToBook: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDars: () -> Unit,
    onBack: () -> Unit
) {
    val favorites by viewModel.favoriteBooks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorite Books", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            LibraryBottomNavigation(
                currentRoute = "favorites",
                onNavigateToHome = onNavigateToHome,
                onNavigateToDars = onNavigateToDars,
                onNavigateToFavorites = {},
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { padding ->
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "No favorites",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No favorites saved yet", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Click the heart icon on any book.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                items(favorites) { book ->
                    BookListCard(book = book, onClick = { onNavigateToBook(book.bookName) })
                }
            }
        }
    }
}

// ================= SETTINGS SCREEN =================
@Composable
fun SettingsScreen(
    viewModel: LibraryViewModel,
    onNavigateToAdmin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToDars: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeMode.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var rateTapCount by remember { mutableStateOf(0) }
    var isAdminVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & About", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            LibraryBottomNavigation(
                currentRoute = "settings",
                onNavigateToHome = onNavigateToHome,
                onNavigateToDars = onNavigateToDars,
                onNavigateToFavorites = onNavigateToFavorites,
                onNavigateToSettings = {}
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Application Theme", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("LIGHT", "DARK", "SYSTEM").forEach { mode ->
                            val isSelected = currentTheme == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode) }
                            )
                        }
                    }
                }
            }

            // General Settings List
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("About Application") },
                        supportingContent = { Text("Version and details of the Islamic Library") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = "Info") },
                        modifier = Modifier.clickable { showAboutDialog = true }
                    )
                    Divider(color = Color.LightGray.copy(0.3f))
                    ListItem(
                        headlineContent = { Text("Privacy Policy & Terms") },
                        supportingContent = { Text("Read our user terms and privacy rules") },
                        leadingContent = { Icon(Icons.Default.Security, contentDescription = "Security") },
                        modifier = Modifier.clickable { showPrivacyDialog = true }
                    )
                    Divider(color = Color.LightGray.copy(0.3f))
                    ListItem(
                        headlineContent = { Text("Contact Developers") },
                        supportingContent = { Text("WhatsApp Chat Support: +91 7383476444") },
                        leadingContent = { Icon(Icons.Default.Phone, contentDescription = "WhatsApp Contact") },
                        modifier = Modifier.clickable {
                            try {
                                val url = "https://api.whatsapp.com/send?phone=917383476444"
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse(url)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp Support: +91 7383476444", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                    Divider(color = Color.LightGray.copy(0.3f))
                    ListItem(
                        headlineContent = { Text("Rate and Share App") },
                        supportingContent = { Text("Recommend this library on Play Store") },
                        leadingContent = { Icon(Icons.Default.ThumbUp, contentDescription = "Rate") },
                        modifier = Modifier.clickable {
                            Toast.makeText(context, "Thank you for rating 5 stars! JazakAllah.", Toast.LENGTH_SHORT).show()
                            rateTapCount++
                            if (rateTapCount >= 7) {
                                if (!isAdminVisible) {
                                    isAdminVisible = true
                                    Toast.makeText(context, "Admin Options Enabled!", Toast.LENGTH_LONG).show()
                                }
                            } else if (rateTapCount >= 3) {
                                val remaining = 7 - rateTapCount
                                Toast.makeText(context, "You are $remaining steps away from admin portal.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // Admin Panel Portal Gateway (Secure Login)
            if (isAdminVisible) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Library Administration Portal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Manage categories, books, and download direct links via Google Drive & Sheets securely.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GradientButton(
                            text = "Access Admin Console",
                            onClick = onNavigateToAdmin,
                            testTag = "settings_access_admin_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Islamic Library") },
            text = {
                Text("This production-ready Islamic Book Library System has been developed utilizing native Android technology (Jetpack Compose, Kotlin, and Room DB) coupled with a scalable Google Sheets database and Google Drive storage API. Version: ${Config.APP_VERSION}")
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy & Terms") },
            text = {
                Text("Your privacy is extremely critical to us. Islamic Library does not require authentication, registration, or collect any personal details for user consumption. Book Favorites and History data remain completely on your secure local database via Room. No background telemetry exists.")
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("OK") }
            }
        )
    }
}
