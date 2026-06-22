#!/usr/bin/env bash
# publish.sh - Build and publish to GitHub
set -e

VERSION="1.0.0"
JAR_NAME="java-runcat-${VERSION}.jar"

echo "🔨 Building Java RunCat..."
mvn clean package -B

echo "✅ Build complete: target/${JAR_NAME}"

# Check if git repo is initialized
if [ ! -d .git ]; then
    echo "📦 Initializing git repository..."
    git init
    git branch -M main
fi

# Check for uncommitted changes
if [ -n "$(git status --porcelain)" ]; then
    echo "📝 Committing changes..."
    git add -A
    git commit -m "release: v${VERSION}"
fi

# Tag the release
if ! git rev-list "v${VERSION}" >/dev/null 2>&1; then
    echo "🏷️  Creating tag v${VERSION}..."
    git tag "v${VERSION}"
fi

echo ""
echo "Next steps:"
echo "  1. Add remote:  git remote add origin https://github.com/YOUR_USERNAME/java-runcat.git"
echo "  2. Push code:   git push -u origin main"
echo "  3. Push tag:    git push origin v${VERSION}"
echo "  4. GitHub Actions will auto-build and create a Release"
echo ""
echo "Or create a release manually at: https://github.com/YOUR_USERNAME/java-runcat/releases/new"
echo "Upload: target/${JAR_NAME}"
