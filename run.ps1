# run.ps1 - Download and run Java RunCat
$VERSION = "1.0.0"
$JAR_NAME = "java-runcat-$VERSION.jar"
$INSTALL_DIR = "$env:USERPROFILE\.java-runcat"
$JAR_PATH = "$INSTALL_DIR\$JAR_NAME"
$GITHUB_REPO = "YOUR_USERNAME/java-runcat"

# Check Java
$javaVersion = java -version 2>&1 | Select-String "version" | ForEach-Object { $_.Line }
if (-not $javaVersion) {
    Write-Host "Java is not installed. Please install Java 17+ from https://adoptium.net/" -ForegroundColor Red
    exit 1
}

# Download if not exists
if (-not (Test-Path $JAR_PATH)) {
    Write-Host "Downloading Java RunCat v$VERSION..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Path $INSTALL_DIR -Force | Out-Null
    $url = "https://github.com/$GITHUB_REPO/releases/download/v$VERSION/$JAR_NAME"
    Invoke-WebRequest -Uri $url -OutFile $JAR_PATH
    Write-Host "Download complete!" -ForegroundColor Green
}

# Run
Write-Host "Starting Java RunCat..." -ForegroundColor Cyan
Start-Process -FilePath "java" -ArgumentList "-jar", $JAR_PATH -WindowStyle Hidden
Write-Host "Java RunCat started in system tray." -ForegroundColor Green
