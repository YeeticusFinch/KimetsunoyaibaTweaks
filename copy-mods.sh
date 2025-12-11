#!/bin/bash

# Auto-update mods script for Kimetsunoyaiba Multiplayer
# Automatically detects the correct mod version and copies to Minecraft instances

# set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Read mod properties from gradle.properties
if [ ! -f "gradle.properties" ]; then
    print_error "gradle.properties not found! Make sure you're in the project root directory."
    exit 1
fi

MOD_ID=$(grep "^mod_id=" gradle.properties | cut -d'=' -f2)
MOD_VERSION=$(grep "^mod_version=" gradle.properties | cut -d'=' -f2)

if [ -z "$MOD_ID" ] || [ -z "$MOD_VERSION" ]; then
    print_error "Could not read mod_id or mod_version from gradle.properties"
    exit 1
fi

print_status "Detected mod: $MOD_ID version $MOD_VERSION"

# Build the expected jar filename (for checking if build is needed)
CHECK_JAR_FILE="build/libs/${MOD_ID}-${MOD_VERSION}.jar"
REOBF_JAR_FILE="build/reobfJar/output.jar"
FINAL_JAR_NAME="${MOD_ID}-${MOD_VERSION}.jar"

# Check if the build/libs jar exists (to determine if we need to build)
if [ ! -f "$CHECK_JAR_FILE" ]; then
    print_warning "Built jar not found: $CHECK_JAR_FILE"
    print_status "Running gradle build..."

    if command -v ./gradlew.bat &> /dev/null; then
        ./gradlew.bat build --no-daemon
    elif command -v ./gradlew &> /dev/null; then
        ./gradlew build
    elif command -v gradle &> /dev/null; then
        gradle build
    else
        print_error "Neither ./gradlew nor gradle found. Please build the mod manually."
        exit 1
    fi

    # Check again after build
    if [ ! -f "$CHECK_JAR_FILE" ]; then
        print_error "Build failed or jar not found after build: $CHECK_JAR_FILE"
        exit 1
    fi
fi

print_status "Found jar file in build/libs: $CHECK_JAR_FILE"

# Check if reobfuscated jar exists
if [ ! -f "$REOBF_JAR_FILE" ]; then
    print_error "Reobfuscated jar not found: $REOBF_JAR_FILE"
    print_error "This should have been created by the build process."
    exit 1
fi

print_status "Found reobfuscated jar: $REOBF_JAR_FILE"

# Create a properly named copy of the reobfuscated jar in build/libs
NAMED_REOBF_JAR="build/libs/reobf-${FINAL_JAR_NAME}"
cp "$REOBF_JAR_FILE" "$NAMED_REOBF_JAR"
print_status "Created named copy: $NAMED_REOBF_JAR"

# Use the named reobfuscated jar for distribution
JAR_FILE="$NAMED_REOBF_JAR"

# Define Minecraft instance paths
INSTANCES=(
    'C:/Users/carlf/AppData/Roaming/PrismLauncher/instances/Demon Slayer Lite(3)/minecraft/mods/'
    'C:/Users/carlf/AppData/Roaming/PrismLauncher/instances/Demon Slayer Lite(2)/minecraft/mods/'
    'C:/Users/carlf/AppData/Roaming/PrismLauncher/instances/Demon Slayer Lite(1)/minecraft/mods/'
    'C:/Users/carlf/AppData/Roaming/PrismLauncher/instances/Demon Slayer Lite/minecraft/mods/'
    'C:/Users/carlf/Downloads/Test KnY Server/mods/'
)

for i in "${INSTANCES[@]}"; do
    echo "Instance: $i"
done

# Function to update a single instance
update_instance() {
    local instance_path="$1"
    local instance_name=$(basename "$(dirname "$(dirname "$instance_path")")")

    if [ ! -d "$instance_path" ]; then
        print_warning "Instance path not found: $instance_path"
    else
	    
	
	    print_status "Updating instance: $instance_name"
	
	    # Remove old versions of the mod (including reobf- prefixed ones)
	    shopt -s nullglob
		old_files=("$instance_path"${MOD_ID}-*.jar "$instance_path"reobf-${MOD_ID}-*.jar)
		old_files_count=${#old_files[@]}
		if [ "$old_files_count" -gt 0 ]; then
		    print_status "Removing $old_files_count old mod file(s)"
		    rm -f "${old_files[@]}"
		fi
		shopt -u nullglob
	
	    # Copy new version
	    cp "$JAR_FILE" "$instance_path"
	    print_status "Copied $JAR_FILE to $instance_name"
	
	    return 0
	fi
}

# Update all instances
successful_updates=0
total_instances=${#INSTANCES[@]}

for instance_path in "${INSTANCES[@]}"; do
    print_status "Updating instance at $instance_path"
    if update_instance "$instance_path"; then
        ((successful_updates++))
    fi
done

# Summary
print_status "Update complete!"
print_status "Successfully updated $successful_updates out of $total_instances instances"

if [ $successful_updates -eq $total_instances ]; then
    print_status "All instances updated successfully! ✓"
    exit 0
else
    print_warning "Some instances failed to update. Check the output above for details."
    exit 1
fi