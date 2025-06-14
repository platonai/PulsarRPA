# Maven Repository Reuse in Docker Builds

This document explains different methods to reuse your host's Maven repository when building PulsarRPA Docker images, which significantly speeds up builds by avoiding re-downloading dependencies.

## 🚀 Method 1: BuildKit Cache Mount (Recommended)

**File:** `Dockerfile` (main)

**Advantages:**
- ✅ Fastest builds after first run
- ✅ Persistent cache across builds
- ✅ No manual volume management
- ✅ Works with Docker Compose

**Usage:**
```bash
# Run the build script
./build-with-cache.sh

# Or manually:
export DOCKER_BUILDKIT=1
docker build -t pulsar-rpa:latest -f docker/pulsar-rpa-prod/Dockerfile .
```

**How it works:**
- Uses `RUN --mount=type=cache,target=/root/.m2` in Dockerfile
- Docker automatically manages a persistent cache volume
- Dependencies persist between builds

## 🔧 Method 2: Host Volume Mount

**File:** `Dockerfile.with-volume`

**Advantages:**
- ✅ Uses existing host Maven repository
- ✅ No additional cache storage needed
- ✅ Immediate access to all host dependencies

**Disadvantages:**
- ❌ Requires manual volume mounting
- ❌ Platform-specific paths
- ❌ Doesn't work with all CI/CD systems

**Usage:**

### Linux/macOS:
```bash
# Run the build script
./build-with-volume.sh

# Or manually:
docker build -v ~/.m2:/root/.m2 -t pulsar-rpa:volume -f Dockerfile.with-volume .
```

### Windows (with repository at `D:\Users\pereg\.m2`):
```bash
# Run the Windows build script
./build-with-volume-windows.sh

# Or run the batch file
build-with-volume-windows.cmd

# Or manually:
docker build -v "D:/Users/pereg/.m2:/root/.m2" -t pulsar-rpa:windows-volume -f Dockerfile.with-volume .
```

**Note for Windows users:**
- Use forward slashes in Docker volume paths: `D:/Users/pereg/.m2`
- Enclose the path in quotes to handle spaces
- Your actual Maven repository path: `D:\Users\pereg\.m2`

## 📊 Performance Comparison

| Method | First Build | Subsequent Builds | CI/CD Friendly | Complexity |
|--------|-------------|-------------------|----------------|------------|
| Cache Mount | Normal | ⚡ Very Fast | ✅ Yes | Low |
| Volume Mount | ⚡ Fast | ⚡ Very Fast | ❌ Limited | Medium |
| No Cache | Normal | Normal | ✅ Yes | Low |

## 🎯 Recommended Approach

**For Development:**
- Use **Method 1 (Cache Mount)** for the best balance of performance and simplicity

**For CI/CD:**
- Use **Method 1 (Cache Mount)** with BuildKit enabled
- Configure cache volumes in your CI/CD pipeline

**For Quick Testing:**
- Use **Method 2 (Volume Mount)** if you already have a populated `.m2` directory

## 📝 Additional Tips

1. **First-time setup:** The first build will still download all dependencies
2. **Cache location:** BuildKit caches are stored in Docker's cache directory
3. **Cache cleanup:** Use `docker builder prune` to clean BuildKit caches
4. **Windows users:** Use the Windows-specific scripts provided for your Maven path

## 🪟 Windows-Specific Notes

**Your Maven Repository Location:**
- **Windows path**: `D:\Users\pereg\.m2`
- **Docker volume path**: `"D:/Users/pereg/.m2:/root/.m2"`

**Available Windows Scripts:**
- `build-with-volume-windows.sh` (for Git Bash/WSL)
- `build-with-volume-windows.cmd` (for Command Prompt/PowerShell)

## 🐳 Docker Compose Example

```yaml
version: '3.8'
services:
  pulsar-rpa:
    build:
      context: .
      dockerfile: docker/pulsar-rpa-prod/Dockerfile
      cache_from:
        - maven:3.9.9-eclipse-temurin-21-alpine
    environment:
      - DOCKER_BUILDKIT=1
    # For Windows volume mount:
    volumes:
      - "D:/Users/pereg/.m2:/root/.m2"
```

## 🔍 Troubleshooting

**Build fails with cache mount:**
- Ensure Docker version supports BuildKit (19.03+)
- Enable BuildKit: `export DOCKER_BUILDKIT=1`

**Volume mount not working (Windows):**
- Check Maven directory exists: `dir "D:\Users\pereg\.m2\repository"`
- Verify Docker Desktop is running
- Ensure drive sharing is enabled in Docker Desktop settings

**Slow builds despite caching:**
- Clear Docker cache: `docker system prune -a`
- Rebuild with `--no-cache` flag once 