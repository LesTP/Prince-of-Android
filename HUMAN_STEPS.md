# Human Steps Required

## Install JDK and Gradle (prerequisite for Steps 4-5)

JDK and Gradle are not installed on this machine. Needed for:
- Step 4 acceptance criteria: `gradle build` succeeds
- Step 5: Kotlin compilation and tests

### Option 1: Via Chocolatey (if allowed in Sandboxie)
```powershell
choco install temurin17 -y
choco install gradle -y
```

### Option 2: Manual install
1. Download Eclipse Temurin JDK 17 from https://adoptium.net/
2. Download Gradle from https://gradle.org/releases/
3. Add both to PATH

### After install, validate Step 4:
```powershell
cd "C:\Users\myeluashvili\claude-code-workspace\projects\PoP port\SDLPoP-kotlin"
gradle build
```

Expected result: `BUILD SUCCESSFUL`
