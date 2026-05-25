@echo off
setlocal
set BASE_DIR=%~dp0
set MAVEN_VERSION=3.9.8
set MAVEN_HOME=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%
if exist "C:\Program Files\Java\jdk-21.0.10\bin\java.exe" (
  set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.10"
  set "PATH=C:\Program Files\Java\jdk-21.0.10\bin;%PATH%"
)

where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
  mvn %*
  exit /b %ERRORLEVEL%
)

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$zip='%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%-bin.zip'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -LiteralPath $zip -DestinationPath '%BASE_DIR%.mvn' -Force"
  if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
