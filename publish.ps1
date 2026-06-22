# publish.ps1 - Build and publish to GitHub (PowerShell)
$ErrorActionPreference = "Stop"

$VERSION = "1.0.0"
$JAR_NAME = "java-runcat-$VERSION.jar"

Write-Host "Building Java RunCat..." -ForegroundColor Cyan
mvn clean package -B

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Build complete: target\$JAR_NAME" -ForegroundColor Green

# Init git if needed
if (-not (Test-Path .git)) {
    Write-Host "Initializing git repository..." -ForegroundColor Yellow
    git init
    git branch -M main
}

# Commit uncommitted changes
$status = git status --porcelain
if ($status) {
    Write-Host "Committing changes..." -ForegroundColor Yellow
    git add -A
    git commit -m "release: v$VERSION"
}

# Tag
$tagExists = git rev-list "v$VERSION" 2>$null
if (-not $tagExists) {
    Write-Host "Creating tag v$VERSION..." -ForegroundColor Yellow
    git tag "v$VERSION"
}

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Add remote:  git remote add origin https://github.com/YOUR_USERNAME/java-runcat.git"
Write-Host "  2. Push code:   git push -u origin main"
Write-Host "  3. Push tag:    git push origin v$VERSION"
Write-Host "  4. GitHub Actions will auto-build and create a Release"
Write-Host ""
Write-Host "Upload manually: target\$JAR_NAME" -ForegroundColor Gray
