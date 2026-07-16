package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.admin.AdminDashboardPanel
import com.example.ui.admin.AdminLoginScreen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LibraryViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    
    private val viewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            // Retrieve persistent user theme mode
            val themeMode by viewModel.themeMode.collectAsState()
            val useDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = useDarkTheme, dynamicColor = false) {
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController,
                    startDestination = "splash",
                    modifier = Modifier.fillMaxSize()
                ) {
                        // 1. Splash Screen
                        composable("splash") {
                            SplashScreen(
                                onNavigateHome = {
                                    navController.navigate("home") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. Home Screen
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToBook = { bookName ->
                                    navController.navigate("details/${URLEncoder.encode(bookName, StandardCharsets.UTF_8.toString())}")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToFavorites = {
                                    navController.navigate("favorites")
                                },
                                onNavigateToDars = {
                                    navController.navigate("dars")
                                }
                            )
                        }

                        // 2b. Dars-e-Nizami Screen
                        composable("dars") {
                            DarsScreen(
                                viewModel = viewModel,
                                onNavigateToBook = { bookName ->
                                    navController.navigate("details/${URLEncoder.encode(bookName, StandardCharsets.UTF_8.toString())}")
                                },
                                onNavigateToHome = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToFavorites = {
                                    navController.navigate("favorites")
                                }
                            )
                        }

                        // 3. Book Details Screen
                        composable(
                            route = "details/{bookName}",
                            arguments = listOf(navArgument("bookName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val encodedBookName = backStackEntry.arguments?.getString("bookName") ?: ""
                            val bookName = URLDecoder.decode(encodedBookName, StandardCharsets.UTF_8.toString())
                            
                            BookDetailsScreen(
                                viewModel = viewModel,
                                bookName = bookName,
                                onBack = { navController.popBackStack() },
                                onOpenPdfReader = { volumeName, pdfUrl ->
                                    val encVol = URLEncoder.encode(volumeName, StandardCharsets.UTF_8.toString())
                                    val encUrl = URLEncoder.encode(pdfUrl, StandardCharsets.UTF_8.toString())
                                    navController.navigate("pdfViewer/$encVol/$encUrl")
                                }
                            )
                        }

                        // 4. PDF Reader Screen
                        composable(
                            route = "pdfViewer/{volumeName}/{pdfUrl}",
                            arguments = listOf(
                                navArgument("volumeName") { type = NavType.StringType },
                                navArgument("pdfUrl") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val encVol = backStackEntry.arguments?.getString("volumeName") ?: ""
                            val encUrl = backStackEntry.arguments?.getString("pdfUrl") ?: ""
                            
                            val volumeName = URLDecoder.decode(encVol, StandardCharsets.UTF_8.toString())
                            val pdfUrl = URLDecoder.decode(encUrl, StandardCharsets.UTF_8.toString())

                            PdfViewerScreen(
                                volumeName = volumeName,
                                pdfUrl = pdfUrl,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // 5. Favorites Screen
                        composable("favorites") {
                            FavoritesScreen(
                                viewModel = viewModel,
                                onNavigateToBook = { bookName ->
                                    navController.navigate("details/${URLEncoder.encode(bookName, StandardCharsets.UTF_8.toString())}")
                                },
                                onNavigateToHome = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                },
                                onNavigateToDars = {
                                    navController.navigate("dars") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // 6. Settings Screen
                        composable("settings") {
                            val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()
                            
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToAdmin = {
                                    if (isAdminLoggedIn) {
                                        navController.navigate("adminDashboard")
                                    } else {
                                        navController.navigate("adminLogin")
                                    }
                                },
                                onNavigateToHome = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                },
                                onNavigateToFavorites = {
                                    navController.navigate("favorites") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                },
                                onNavigateToDars = {
                                    navController.navigate("dars") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // 7. Admin Authorization (Login) Screen
                        composable("adminLogin") {
                            AdminLoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = {
                                    navController.navigate("adminDashboard") {
                                        popUpTo("adminLogin") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // 8. Responsive Admin Dashboard Panel Console
                        composable("adminDashboard") {
                            AdminDashboardPanel(
                                viewModel = viewModel,
                                onBack = {
                                    navController.navigate("home") {
                                        popUpTo("adminDashboard") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
            }
        }
    }
}
