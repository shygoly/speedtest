#!/bin/bash

# Swiftest Speed Test - Clean All Build Artifacts Script
# This script cleans all build outputs and temporary files

set -e  # Exit on any error

echo "🧹 Cleaning Swiftest Speed Test Project..."
echo "=========================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "settings.gradle" ]; then
    print_error "Please run this script from the project root directory"
    exit 1
fi

# Clean Android modules
print_status "Cleaning Android modules..."
./gradlew clean
if [ $? -eq 0 ]; then
    print_success "Android modules cleaned successfully"
else
    print_error "Failed to clean Android modules"
    exit 1
fi

# Clean Web Frontend
print_status "Cleaning Web Frontend..."
cd web-frontend

# Remove node_modules if exists
if [ -d "node_modules" ]; then
    print_status "Removing node_modules..."
    rm -rf node_modules
fi

# Remove dist folder if exists
if [ -d "dist" ]; then
    print_status "Removing dist folder..."
    rm -rf dist
fi

# Remove package-lock.json if exists
if [ -f "package-lock.json" ]; then
    print_status "Removing package-lock.json..."
    rm -f package-lock.json
fi

cd ..

# Clean additional build artifacts
print_status "Cleaning additional artifacts..."

# Remove .gradle folder from root
if [ -d ".gradle" ]; then
    print_status "Removing .gradle folder..."
    rm -rf .gradle
fi

# Remove build folders that might not be caught by gradle clean
find . -name "build" -type d -exec rm -rf {} + 2>/dev/null || true

# Remove IDE files
print_status "Cleaning IDE files..."
find . -name ".idea" -type d -exec rm -rf {} + 2>/dev/null || true
find . -name "*.iml" -type f -delete 2>/dev/null || true
find . -name ".vscode" -type d -exec rm -rf {} + 2>/dev/null || true

# Remove OS-specific files
print_status "Cleaning OS-specific files..."
find . -name ".DS_Store" -type f -delete 2>/dev/null || true
find . -name "Thumbs.db" -type f -delete 2>/dev/null || true

# Remove log files
print_status "Cleaning log files..."
find . -name "*.log" -type f -delete 2>/dev/null || true

# Summary
echo ""
echo "=========================================="
print_success "🎉 Project cleaned successfully!"
echo ""
print_status "Cleaned items:"
echo "  🏗️  Android build artifacts"
echo "  📦 Node.js dependencies and build outputs"
echo "  💾 Gradle cache and build folders"
echo "  🔧 IDE configuration files"
echo "  🗂️  OS-specific temporary files"
echo "  📝 Log files"
echo ""
print_status "Project is now clean and ready for a fresh build! ✨"