# build-exe.ps1 - Build Java RunCat as a native Windows application using jpackage
# Requires: JDK 17+ with jpackage, Maven
$ErrorActionPreference = "Stop"

$VERSION = "1.2.0"
$APP_NAME = "JavaRunCat"
$VENDOR = "JavaRunCat Team"
$MAIN_CLASS = "com.runcat.RunCatApp"
$PROJECT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "=== Building Java RunCat v$VERSION as Windows Application ===" -ForegroundColor Cyan

# Step 1: Build fat JAR with Maven
Write-Host "`n[1/5] Building JAR with Maven..." -ForegroundColor Yellow
Push-Location $PROJECT_DIR
mvn clean package -B -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven build failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Write-Host "   JAR built: target/java-runcat-$VERSION.jar" -ForegroundColor Green

# Step 2: Generate application icons
Write-Host "`n[2/5] Generating application icons..." -ForegroundColor Yellow
java -cp target/classes com.runcat.util.IconGenerator "src/main/resources/icons"
Write-Host "   Icons generated" -ForegroundColor Green

# Step 3: Generate animation frames
Write-Host "`n[3/5] Generating animation frames..." -ForegroundColor Yellow
java -cp target/classes com.runcat.util.AnimationGenerator
Write-Host "   Animation frames generated" -ForegroundColor Green

# Step 4: Rebuild JAR with icons + animations included
Write-Host "`n[4/5] Rebuilding JAR with resources..." -ForegroundColor Yellow
mvn clean package -B -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "Rebuild failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Write-Host "   Final JAR ready" -ForegroundColor Green

# Step 5: Create Windows app with jpackage
Write-Host "`n[5/5] Creating Windows application with jpackage..." -ForegroundColor Yellow

$jpackageArgs = @(
    "--type", "app-image",
    "--name", $APP_NAME,
    "--input", "target",
    "--main-jar", "java-runcat-$VERSION.jar",
    "--main-class", $MAIN_CLASS,
    "--app-version", $VERSION,
    "--vendor", $VENDOR,
    "--description", "A cute running cat animation on your Windows taskbar",
    "--icon", "src/main/resources/icons/app-icon.ico",
    "--java-options", "--enable-native-access=ALL-UNNAMED",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--java-options", "-Dsun.jnu.encoding=UTF-8",
    "--dest", "dist"
)

# Clean previous dist
if (Test-Path "dist") { Remove-Item -Recurse -Force "dist" }

& jpackage $jpackageArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "jpackage failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}

Pop-Location

Write-Host "`n=== Build Complete! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Output: $PROJECT_DIR\dist\$APP_NAME\" -ForegroundColor White
Write-Host "Executable: $PROJECT_DIR\dist\$APP_NAME\$APP_NAME.exe" -ForegroundColor White
Write-Host ""
Write-Host "To create an installer (MSI/EXE), run:" -ForegroundColor Cyan
Write-Host "  .\build-installer.ps1" -ForegroundColor White
Write-Host ""
Write-Host "To run the app now:" -ForegroundColor Cyan
Write-Host "  & `"$PROJECT_DIR\dist\$APP_NAME\$APP_NAME.exe`"" -ForegroundColor White
