# Scripts

## build.ps1, build.sh

Build the project.

## build-exe.ps1, build-exe.sh

Build Browser4 as a Windows executable (.exe). The resulting `Browser4.exe` can be run on Windows
systems with a Java Runtime Environment (JRE) 17+ installed.

Usage:
```bash
# On Windows PowerShell
.\bin\build-exe.ps1

# On Linux/macOS or Windows Git Bash
./bin/build-exe.sh
```

Options:
- `-Clean` / `-clean`: Perform a clean build before packaging
- `-Test` / `-test`: Run tests during the build (tests are skipped by default)

The executable will be created at `browser4/browser4-agents/target/Browser4.exe`.

## build-run.ps1, build-run.sh

Build and run the project.

## browser4.ps1, browser4.sh

Start the Browser4 server.

## command-sse.ps1, command-sse.sh

SSE client to connect to the server.

## run-integration-tests.ps1, run-integration-tests.sh

Run integration tests.

## scrape.ps1, scrape.sh, scrape.bat

Scrape a webpage using Browser4.

## scrape-async.ps1, scrape-async.sh

Scrape a webpage using Browser4 using async API.

## seeds.txt

The seed URLs to test Browser4.

## test-curl-commands.ps1, test-curl-commands.sh

Test Browser4 using curl commands.

## version.ps1, version.sh

Print the version of Browser4.
