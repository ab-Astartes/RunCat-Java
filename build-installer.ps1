# build-installer.ps1 - Create a Windows installer (EXE/MSI) for Java RunCat
# Requires: JDK 17+ with jpackage, WiX Toolset (for MSI) or Inno Setup (for EXE)
$ErrorActionPreference = "Stop"

$VERSION = "1.2.0"
$APP_NAME = "JavaRunCat"

# First ensure app-image is built
if (-not (Test-Path "dist\$APP_NAME\$APP_NAME.exe")) {
    Write-Host "App image not found. Running build-exe.ps1 first..." -ForegroundColor Yellow
    .\build-exe.ps1
}

Write-Host "`n=== Creating Windows Installer ===" -ForegroundColor Cyan

# Check for WiX Toolset (needed for MSI)
$wixFound = (Get-Command "candle.exe" -ErrorAction SilentlyContinue) -ne $null
$isccFound = (Get-Command "ISCC.exe" -ErrorAction SilentlyContinue) -ne $null

# Option A: Use jpackage to create installer directly (requires Wix for MSI)
if ($wixFound) {
    Write-Host "[Option A] Creating MSI installer via jpackage..." -ForegroundColor Yellow
    $jpackageArgs = @(
        "--type", "msi",
        "--name", $APP_NAME,
        "--app-image", "dist\$APP_NAME",
        "--app-version", $VERSION,
        "--vendor", "JavaRunCat Team",
        "--description", "A cute running cat animation on your Windows taskbar",
        "--win-dir-chooser",
        "--win-menu",
        "--win-menu-group", "Java RunCat",
        "--win-shortcut",
        "--win-per-user-install",
        "--dest", "installer",
        "--license-file", "LICENSE"
    )
    & jpackage $jpackageArgs
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   MSI installer created: installer\$APP_NAME-$VERSION.msi" -ForegroundColor Green
    } else {
        Write-Host "   jpackage MSI creation failed (exit code: $LASTEXITCODE)" -ForegroundColor Red
    }
} else {
    Write-Host "[Option A] WiX Toolset not found. Skipping MSI creation." -ForegroundColor DarkGray
    Write-Host "   Install WiX: https://wixtoolset.org/" -ForegroundColor DarkGray
}

# Option B: Use jpackage exe installer (available in newer JDK distributions)
Write-Host "[Option B] Creating EXE package via jpackage..." -ForegroundColor Yellow
$installerArgs = @(
    "--type", "exe",
    "--name", $APP_NAME,
    "--app-image", "dist\$APP_NAME",
    "--app-version", $VERSION,
    "--vendor", "JavaRunCat Team",
    "--description", "A cute running cat animation on your Windows taskbar",
    "--win-dir-chooser",
    "--win-menu",
    "--win-menu-group", "Java RunCat",
    "--win-shortcut",
    "--win-per-user-install",
    "--dest", "installer",
    "--license-file", "LICENSE"
)

try {
    & jpackage $installerArgs 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   EXE installer created: installer\$APP_NAME-$VERSION.exe" -ForegroundColor Green
    } else {
        Write-Host "   EXE installer creation failed (exit code: $LASTEXITCODE)" -ForegroundColor Red
        Write-Host "   This is expected if your JDK distribution doesn't support --type exe." -ForegroundColor DarkGray
    }
} catch {
    Write-Host "   EXE installer creation skipped (not supported in this JDK distribution)" -ForegroundColor DarkGray
}

Write-Host "`n=== Installer Build Summary ===" -ForegroundColor Cyan
if (Test-Path "installer") {
    Get-ChildItem -Path "installer" -Recurse -File | ForEach-Object {
        $size = [math]::Round($_.Length / 1MB, 1)
        Write-Host "   $($_.Name) - ${size}MB" -ForegroundColor White
    }
}
Write-Host ""
Write-Host "If you need an MSI installer:" -ForegroundColor Yellow
Write-Host "  1. Install WiX Toolset from: https://github.com/wixtoolset/wix3/releases" -ForegroundColor White
Write-Host "  2. Add to PATH: `"C:\Program Files (x86)\WiX Toolset v3.14\bin`"" -ForegroundColor White
Write-Host "  3. Re-run: .\build-installer.ps1" -ForegroundColor White
