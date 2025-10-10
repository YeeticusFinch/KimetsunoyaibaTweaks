# Build and Development Commands

## Basic Build Commands

```bash
# Build the mod
./gradlew build

# Clean build artifacts
./gradlew clean

# Run development client
./gradlew runClient

# Run development server
./gradlew runServer

# Run data generators
./gradlew runData

# Refresh dependencies
./gradlew --refresh-dependencies
```

## IDE Setup

### Eclipse
```bash
./gradlew genEclipseRuns
# Then import as existing Gradle project
```

### IntelliJ IDEA
```bash
./gradlew genIntellijRuns
# Then import build.gradle file
```

## Testing

```bash
# Start dedicated server for testing
./gradlew runServer

# Copy mod to server for external testing
cp build/libs/kimetsunoyaibamultiplayer-*.jar /path/to/server/mods/
```
