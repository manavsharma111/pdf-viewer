package com.antigravity.pdfviewer.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.antigravity.pdfviewer.PdfViewerApplication
import com.antigravity.pdfviewer.presentation.home.HomeScreen
import com.antigravity.pdfviewer.presentation.home.HomeViewModel
import com.antigravity.pdfviewer.presentation.page_management.PageManagementScreen
import com.antigravity.pdfviewer.presentation.page_management.PageManagementViewModel
import com.antigravity.pdfviewer.presentation.reader.PdfReaderScreen
import com.antigravity.pdfviewer.presentation.reader.PdfReaderViewModel
import com.antigravity.pdfviewer.presentation.settings.SettingsScreen
import com.antigravity.pdfviewer.presentation.settings.SettingsViewModel
import com.antigravity.pdfviewer.presentation.tools.PdfToolsViewModel
import com.antigravity.pdfviewer.presentation.tools.compress.CompressPdfScreen
import com.antigravity.pdfviewer.presentation.tools.image_to_pdf.ImageToPdfScreen
import com.antigravity.pdfviewer.presentation.tools.merge.MergePdfScreen
import com.antigravity.pdfviewer.presentation.tools.pdf_to_image.PdfToImageScreen
import com.antigravity.pdfviewer.presentation.tools.pdf_to_text.PdfToTextScreen
import com.antigravity.pdfviewer.presentation.tools.protect.ProtectPdfScreen
import com.antigravity.pdfviewer.presentation.tools.split.SplitPdfScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    app: PdfViewerApplication,
    initialUri: Uri? = null
) {
    val startDestination = if (initialUri != null) {
        Screen.Reader.createRoute(initialUri, 0)
    } else {
        Screen.Home.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Home
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(app.recentDocumentsRepository)
            )
            HomeScreen(
                viewModel = homeViewModel,
                onOpenDocument = { uri, page ->
                    navController.navigate(Screen.Reader.createRoute(uri, page))
                },
                onNavigateToMerge = { navController.navigate(Screen.Merge.route) },
                onNavigateToSplit = { navController.navigate(Screen.Split.route) },
                onNavigateToCompress = { navController.navigate(Screen.Compress.route) },
                onNavigateToPdfToImage = { navController.navigate(Screen.PdfToImage.route) },
                onNavigateToImageToPdf = { navController.navigate(Screen.ImageToPdf.route) },
                onNavigateToPdfToText = { navController.navigate(Screen.PdfToText.route) },
                onNavigateToProtect = { navController.navigate(Screen.Protect.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        // Reader
        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("page") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val uriStr = backStackEntry.arguments?.getString("uri") ?: ""
            val initialPage = backStackEntry.arguments?.getInt("page") ?: 0
            val uri = Uri.parse(Uri.decode(uriStr))

            val readerViewModel: PdfReaderViewModel = viewModel(
                factory = PdfReaderViewModel.Factory(
                    app.pdfDocumentRepository,
                    app.recentDocumentsRepository,
                    app.bookmarkRepository,
                    app.annotationRepository,
                    app.preferencesRepository
                )
            )

            PdfReaderScreen(
                uri = uri,
                initialPage = initialPage,
                viewModel = readerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPageManagement = { docUri ->
                    navController.navigate(Screen.PageManagement.createRoute(docUri))
                }
            )
        }

        // Page Management
        composable(
            route = Screen.PageManagement.route,
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val uriStr = backStackEntry.arguments?.getString("uri") ?: ""
            val uri = Uri.parse(Uri.decode(uriStr))

            val pageManagementViewModel: PageManagementViewModel = viewModel(
                factory = PageManagementViewModel.Factory(
                    app.pdfDocumentRepository,
                    app.pdfToolsRepository
                )
            )

            PageManagementScreen(
                uri = uri,
                viewModel = pageManagementViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Tools
        composable(Screen.Merge.route) {
            val toolsViewModel: PdfToolsViewModel = viewModel(
                factory = PdfToolsViewModel.Factory(app.pdfToolsRepository)
            )
            MergePdfScreen(
                viewModel = toolsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Split.route) {
            val toolsViewModel: PdfToolsViewModel = viewModel(
                factory = PdfToolsViewModel.Factory(app.pdfToolsRepository)
            )
            SplitPdfScreen(
                viewModel = toolsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Compress.route) {
            val toolsViewModel: PdfToolsViewModel = viewModel(
                factory = PdfToolsViewModel.Factory(app.pdfToolsRepository)
            )
            CompressPdfScreen(
                viewModel = toolsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PdfToImage.route) {
            val toolsViewModel: PdfToolsViewModel = viewModel(
                factory = PdfToolsViewModel.Factory(app.pdfToolsRepository)
            )
            PdfToImageScreen(
                viewModel = toolsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ImageToPdf.route) {
            val toolsViewModel: PdfToolsViewModel = viewModel(
                factory = PdfToolsViewModel.Factory(app.pdfToolsRepository)
            )
            ImageToPdfScreen(
                viewModel = toolsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PdfToText.route) {
            val toolsViewModel: PdfToolsViewModel = viewModel(
                factory = PdfToolsViewModel.Factory(app.pdfToolsRepository)
            )
            PdfToTextScreen(
                viewModel = toolsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Protect.route) {
            val toolsViewModel: PdfToolsViewModel = viewModel(
                factory = PdfToolsViewModel.Factory(app.pdfToolsRepository)
            )
            ProtectPdfScreen(
                viewModel = toolsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings & Themes
        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.preferencesRepository)
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateToChangeTheme = { navController.navigate(Screen.ChangeTheme.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ChangeTheme.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.preferencesRepository)
            )
            com.antigravity.pdfviewer.presentation.settings.ChangeThemeScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
