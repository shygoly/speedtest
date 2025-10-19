#!/bin/bash

# Swiftest Speed Test - Build All Modules Script
# This script builds all components of the Swiftest speed test project

set -e  # Exit on any error

echo "🚀 Building Swiftest Speed Test Project..."
echo "=========================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "settings.gradle" ]; then
    print_error "Please run this script from the project root directory"
    exit 1
fi

# Build Android modules (Core, App, SDK)
print_status "Building Android modules..."

print_status "Building Core module..."
./gradlew :core:clean :core:build
if [ $? -eq 0 ]; then
    print_success "Core module built successfully"
else
    print_error "Failed to build Core module"
    exit 1
fi

print_status "Building Android App..."
./gradlew :android-app:clean :android-app:build
if [ $? -eq 0 ]; then
    print_success "Android App built successfully"
else
    print_error "Failed to build Android App"
    exit 1
fi

print_status "Building Android SDK..."
./gradlew :android-sdk:sdk:clean :android-sdk:sdk:build
if [ $? -eq 0 ]; then
    print_success "Android SDK built successfully"
else
    print_error "Failed to build Android SDK"
    exit 1
fi

print_status "Building SDK Sample App..."
./gradlew :android-sdk:sample:clean :android-sdk:sample:build
if [ $? -eq 0 ]; then
    print_success "SDK Sample App built successfully"
else
    print_error "Failed to build SDK Sample App"
    exit 1
fi

# Build Web Frontend
print_status "Building Web Frontend..."
cd web-frontend

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    print_status "Installing Node.js dependencies..."
    npm install
fi

print_status "Building Vue.js application..."
npm run build
if [ $? -eq 0 ]; then
    print_success "Web Frontend built successfully"
    cd ..
else
    print_error "Failed to build Web Frontend"
    cd ..
    exit 1
fi

# Summary
echo ""
echo "=========================================="
print_success "🎉 All modules built successfully!"
echo ""
print_status "Build artifacts:"
echo "  📱 Android App APK: android-app/app/build/outputs/apk/"
echo "  📚 Android SDK AAR: android-sdk/sdk/build/outputs/aar/"
echo "  🏗️  Core Library AAR: core/build/outputs/aar/"
echo "  🌐 Web Frontend: web-frontend/dist/"
echo "  📱 SDK Sample APK: android-sdk/sample/build/outputs/apk/"
echo ""
print_status "Ready for deployment! 🚀"