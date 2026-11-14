# PowerShell script to guide or set up Android Command-Line Tools without Android Studio

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Android Command-Line Build Setup (No Android Studio)   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "You do NOT need the heavy Android Studio IDE to build and run this app!" -ForegroundColor Green
Write-Host "You already have Java 17 installed." -ForegroundColor Green
Write-Host ""
Write-Host "Option 1: Build in the Cloud via GitHub (Fastest, 0 MB download)" -ForegroundColor Yellow
Write-Host "  1. Push this folder to a GitHub repository."
Write-Host "  2. Go to Actions tab -> 'Build Android APK'."
Write-Host "  3. Download the compiled 'app-debug.apk' from Artifacts!"
Write-Host ""
Write-Host "Option 2: Install Lightweight Android Command-Line Tools (~140 MB)" -ForegroundColor Yellow
Write-Host "  1. Download Android command-line tools for Windows from:"
Write-Host "     https://developer.android.com/studio#command-line-tools-only"
Write-Host "  2. Extract to: %LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\"
Write-Host "  3. Set environment variable ANDROID_HOME = %LOCALAPPDATA%\Android\Sdk"
Write-Host "  4. In PowerShell, run:"
Write-Host "     sdkmanager --install 'platforms;android-34' 'build-tools;34.0.0' 'platform-tools'"
Write-Host "  5. Then build the APK anytime with:"
Write-Host "     .\gradlew assembleDebug"
Write-Host ""
Write-Host "Option 3: Install Android Studio (Full Graphical IDE)" -ForegroundColor Yellow
Write-Host "  Download from: https://developer.android.com/studio"
Write-Host "==========================================================" -ForegroundColor Cyan
