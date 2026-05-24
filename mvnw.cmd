@echo off
setlocal enabledelayedexpansion
set MVNW_DIR=%~dp0.mvn\wrapper
set MAVEN_VERSION=3.9.6
set ZIP_NAME=apache-maven-%MAVEN_VERSION%-bin.zip
if not exist "%MVNW_DIR%\apache-maven-%MAVEN_VERSION%" (
  echo Maven distribution not found in %MVNW_DIR% - attempting download...
  powershell -NoProfile -Command "$out='%MVNW_DIR%\\%ZIP_NAME%'; New-Item -ItemType Directory -Force -Path '%MVNW_DIR%'; Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/%ZIP_NAME%' -OutFile $out -UseBasicParsing; Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::ExtractToDirectory($out, '%MVNW_DIR%'); Remove-Item $out -Force"
  if errorlevel 1 (
    echo Failed to download or extract Maven. Please install Maven manually and ensure `mvn` is on PATH, or update this wrapper.
    exit /b 1
  )
)
set MAVEN_HOME=%MVNW_DIR%\apache-maven-%MAVEN_VERSION%
set PATH=%MAVEN_HOME%\bin;%PATH%
mvn %*
endlocal
