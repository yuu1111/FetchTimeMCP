@echo off
set JAVA_HOME=C:\Users\yuu21\.jdks\ms-21.0.7
echo Using Java: %JAVA_HOME%
call mvnw.bat clean package -DskipTests
echo Build completed!