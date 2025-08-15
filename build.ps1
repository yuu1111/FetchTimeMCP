$env:JAVA_HOME = "C:\Users\yuu21\.jdks\ms-21.0.7"
Write-Host "Using Java: $env:JAVA_HOME"
Write-Host "Building FetchTimeMCP..."
.\mvnw.bat clean package -DskipTests
Write-Host "Build completed!"