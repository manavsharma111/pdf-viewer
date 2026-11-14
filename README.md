# PDF & Office Viewer Pro 📄📊⚡

A production-quality, advanced, offline-first Android Document Suite supporting **PDF**, **Excel (.xlsx, .xls)**, **Word (.docx, .doc)**, and **PowerPoint (.pptx, .ppt)**. Built with **Jetpack Compose**, **Material 3**, and **Clean Architecture**.

Designed and optimized for **phones**, **tablets**, **foldable devices**, **Chromebooks**, and **Android desktop environments**.

---

## 📑 Supported Document Formats

- **PDF (.pdf)**: Full viewing, page management, 100-600% zoom, vector annotations, bookmarks, search, and utilities.
- **Excel (.xlsx, .xls)**: Parses sheets, rows, columns, and data cells, rendering clean tabular spreadsheet views.
- **Word (.docx, .doc)**: Parses paragraphs, formatting, typography, and line wrapping into paginated document pages.
- **PowerPoint (.pptx, .ppt)**: Parses presentation slides, titles, bullet points, and headers into 16:9 widescreen presentation slides.

All Office formats can be opened directly, annotated with pen/highlighter, navigated, searched, and converted/exported to standard PDF!

---

## 🚀 Key Highlights & Architecture

- **100% Offline-First & Privacy-Focused**: No document uploads, no external tracking, no cloud dependencies.
- **Fast, Native Rendering Engine**: Powered by Android's hardware-accelerated Skia `PdfRenderer` using memory-aware LRU bitmap caching with prioritized background prefetch and immediate cancellation for scrolled-out pages.
- **Real PDF Manipulation & Burning**: Powered by Apache PDFBox for Android (`com.tom-roush:pdfbox-android`). Saved annotations, page rotations, deletions, reorderings, and merges are written directly into standard PDF content streams—meaning saved documents display accurately in Adobe Acrobat, Google Chrome, and Apple Preview!
- **Zero Heavy Native .so Bloat**: Relies strictly on native Android APIs and standard JVM/Android libraries.
- **Multi-Form Factor UI**: Seamlessly adapts across phone layouts, foldable dual-screens, tablet split views, and full desktop/Chromebook windowing environments.

---

## 📱 Features Breakdown

### 1. Home Screen
- **Quick Open**: Instant document picker via Android Storage Access Framework (`ACTION_OPEN_DOCUMENT`).
- **Recent Documents**: Tracks reading progress, last page, last zoom, file size, page count, and offers one-tap resume.
- **Search History**: Instant search across all recent documents.
- **Tools Suite**: Direct access to all PDF utility operations.

### 2. PDF Reader & Reading Modes
- **Single Page**: Horizontal swipe between pages with page snap.
- **Continuous Vertical**: High-performance continuous vertical list with page margins.
- **Continuous Horizontal**: Continuous horizontal list for presentations and slide decks.
- **Double Page (Book Mode)**: Side-by-side two-page view for tablets, Chromebooks, and wide screens. Correctly balances odd/even page counts.

### 3. Zoom, Pan & Gestures
- **Pinch-to-Zoom**: Smooth scaling from 100% up to 600%.
- **Double-Tap Zoom**: Instantly toggles between Fit (1.0x) and 2.5x.
- **2D Smooth Panning**: Boundary-clamped viewport movement without over-scrolling.
- **Mouse & Keyboard Support**:
  - Mouse wheel smooth scroll and Ctrl+Wheel zooming.
  - Left / Right arrows: Previous / Next page.
  - `+` / `-`: Zoom in / Zoom out.
  - `Ctrl + F`: Search document.
  - `Ctrl + S`: Save / Export PDF.
  - `Ctrl + Z` / `Ctrl + Y`: Undo / Redo annotations.
  - `Home` / `End`: Jump to first / last page.
  - `Esc`: Close search / tool overlays.

### 4. Search & Navigation
- **In-Document Search**: Fast multi-page text search with case-sensitive and whole-word filters.
- **Interactive Match Cycling**: "Match 3 / 17" counter with instant jump to search hits.
- **Page Slider & Direct Jump**: Jump to any page number via page counter dialog.
- **Thumbnail Grid Sheet**: Fast visual page grid for immediate navigation.
- **Bookmarks**: Add, view, jump to, and delete page bookmarks.

### 5. Vector Annotations & Editing
- **Drawing & Handwriting**: Pen, Pencil, Marker, and Eraser with stylus/S-Pen/finger support.
- **Text Box**: Tap to place multi-line text notes with customizable font size and color.
- **Text Markup**: Highlighting (multi-color with opacity), Underline, and Strike-through.
- **Undo / Redo**: Multi-step session history stack.
- **Burn-in Export**: Burns all vector paths, text boxes, and highlights into the actual PDF file upon saving.

### 6. Page Management
- Multi-select pages with thumbnail previews.
- **Reorder**: Move selected pages up or down.
- **Rotate**: Rotate selected pages 90° clockwise or counterclockwise.
- **Duplicate**: Clone selected pages.
- **Delete**: Remove unwanted pages.
- **Extract**: Export selected pages into a brand new PDF file.

### 7. PDF Utilities Suite
- **Merge PDF**: Select multiple PDFs, drag/reorder files, merge in background.
- **Split PDF**: Split by page range (e.g. `1-5, 8, 10-12`), extract specific pages, or split every N pages.
- **Compress PDF**: Low, Medium, and High compression presets by re-encoding embedded images and stripping redundant resources.
- **PDF → Image**: Export pages to PNG (lossless) or JPEG at 72, 150, or 300 DPI.
- **Image → PDF**: Convert photos (JPEG, PNG, WebP) to PDF with A4, US Letter, or original image dimensions.
- **PDF → Text**: Extract clean text with paragraph separation and export to `.txt` or clipboard.
- **Password Protect**: Encrypt PDFs using standard 128-bit AES encryption with User and Owner passwords.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Language** | Kotlin 1.9.23 (JVM 17) |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Clean Architecture |
| **Concurrency** | Kotlin Coroutines + Flow / StateFlow |
| **Local Database** | Room 2.6.1 |
| **Preferences** | Jetpack DataStore Preferences |
| **PDF Manipulation** | Apache PDFBox for Android (`com.tom-roush:pdfbox-android`) |
| **PDF Rendering** | Android Native `android.graphics.pdf.PdfRenderer` |
| **File Access** | Storage Access Framework (SAF) |
| **Testing** | JUnit 4, Kotlinx Coroutines Test |

---

## 📂 Project Structure

```text
app/src/main/java/com/antigravity/pdfviewer/
│
├── MainActivity.kt                          # Edge-to-edge Activity with Intent handling
├── PdfViewerApplication.kt                  # Application class & dependency initialization
│
├── data/
│   ├── local/
│   │   ├── dao/                             # Room DAOs (RecentDocumentDao, BookmarkDao, AnnotationDao)
│   │   ├── database/                        # AppDatabase (Room)
│   │   ├── entity/                          # Room Entities
│   │   └── preferences/                     # UserPreferencesRepository (DataStore)
│   ├── pdf/
│   │   ├── NativePdfRendererManager.kt      # Hardware-accelerated Skia renderer + LRU bitmap cache
│   │   └── PdfBoxManager.kt                 # PDFBox wrapper for manipulation, merge, split, encrypt, burn
│   └── repository/                          # Data layer repository implementations
│
├── domain/
│   ├── model/                               # Clean domain models (PdfModels, AnnotationModels)
│   └── repository/                          # Repository interfaces
│
├── presentation/
│   ├── home/                                # Home screen & ViewModel
│   ├── reader/                              # PDF reader screen, ViewModel & components
│   │   └── components/                      # PdfViewer, PdfPageItem, AnnotationCanvas, TopBar, BottomBar, etc.
│   ├── page_management/                     # Page grid, reorder, rotate, delete screen & ViewModel
│   ├── tools/                               # PDF Utilities (Merge, Split, Compress, Image, Text, Protect)
│   ├── settings/                            # Settings screen & ViewModel
│   └── navigation/                          # NavHost, routes, and screen destinations
│
└── ui/
    └── theme/                               # Material 3 colors, typography, and theme
```

---

## 🔧 Building and Running (With or Without Android Studio)

### Option A: Cloud Build via GitHub Actions (Zero Local Setup)
You don't need Android Studio or any local Android SDK installed:
1. Push this project to any GitHub repository.
2. In your repo, go to the **Actions** tab -> **Build Android APK** workflow.
3. Once finished, click the build and download **PDFViewerPro-Debug-APK** directly under the **Artifacts** section!
4. Transfer the `app-debug.apk` to your Android phone or tablet and install it.

---

### Option B: Using VS Code / Antigravity IDE & Command-Line Tools (Lightweight ~140 MB)
If you prefer not to install the heavy Android Studio IDE:
1. **Java 17** is already installed on your system.
2. Download the official **Android Command-Line Tools** for Windows from [developer.android.com/studio#command-line-tools-only](https://developer.android.com/studio#command-line-tools-only).
3. Extract to `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\`.
4. Install the required platform and build tools:
   ```cmd
   sdkmanager --install "platforms;android-34" "build-tools;34.0.0" "platform-tools"
   ```
5. Build the APK directly in the terminal:
   ```cmd
   .\gradlew assembleDebug
   ```
   The compiled APK will be created at: `app/build/outputs/apk/debug/app-debug.apk`.
6. Install on your connected phone:
   ```cmd
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

### Option C: In Android Studio (Full GUI IDE)
1. Download and install [Android Studio](https://developer.android.com/studio).
2. Choose **Open** and select `c:\pdf_viewer_app`.
3. Android Studio will automatically download the Android 34 SDK and Gradle dependencies.
4. Click **Run** (`Shift + F10`) to launch on your device or emulator.
