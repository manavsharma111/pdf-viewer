package com.antigravity.pdfviewer.presentation.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Reader : Screen("reader?uri={uri}&page={page}") {
        fun createRoute(uri: Uri, initialPage: Int = 0): String {
            return "reader?uri=${Uri.encode(uri.toString())}&page=$initialPage"
        }
    }
    object Tools : Screen("tools")
    object Merge : Screen("tools/merge")
    object Split : Screen("tools/split")
    object Compress : Screen("tools/compress")
    object PdfToImage : Screen("tools/pdf_to_image")
    object ImageToPdf : Screen("tools/image_to_pdf")
    object PdfToText : Screen("tools/pdf_to_text")
    object Protect : Screen("tools/protect")
    object PageManagement : Screen("page_management?uri={uri}") {
        fun createRoute(uri: Uri): String {
            return "page_management?uri=${Uri.encode(uri.toString())}"
        }
    }
    object Settings : Screen("settings")
    object ChangeTheme : Screen("settings/change_theme")
}
