# ChatQ Deployment Automation Script
# Frontend Build -> Copy to Spring Boot static folder -> Backend Build

param(
    [switch]$SkipFrontend,  # Skip frontend build
    [switch]$SkipBackend,   # Skip backend build
    [switch]$RunServer      # Run server after build
)

# UTF-8 Encoding Settings
$PSDefaultParameterValues['*:Encoding'] = 'utf8'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 > $null 2>&1

$ErrorActionPreference = "Stop"
$BaseDir = $PSScriptRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ChatQ Deployment Started" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Frontend Build
if (-not $SkipFrontend) {
    Write-Host "[1/4] Building Frontend..." -ForegroundColor Yellow
    Set-Location "$BaseDir\chatq"
    
    if (-not (Test-Path "node_modules")) {
        Write-Host "   -> node_modules not found. Running npm install..." -ForegroundColor Gray
        npm install
    }
    
    Write-Host "   -> Running npm run build..." -ForegroundColor Gray
    npm run build
    
    if (-not (Test-Path "dist")) {
        Write-Host "   X Build failed: dist folder not created." -ForegroundColor Red
        exit 1
    }
    
    Write-Host "   OK Frontend build completed!" -ForegroundColor Green
    Write-Host ""
}
else {
    Write-Host "[1/4] Frontend build skipped" -ForegroundColor Gray
    Write-Host ""
}

# 2. Copy Build Files
if (-not $SkipFrontend) {
    Write-Host "[2/4] Copying static files..." -ForegroundColor Yellow
    
    $SourceDir = "$BaseDir\chatq\dist"
    $TargetDir = "$BaseDir\chatq-server\src\main\resources\static"
    
    # Remove existing static folder
    if (Test-Path $TargetDir) {
        Write-Host "   -> Removing existing static folder..." -ForegroundColor Gray
        Remove-Item -Path $TargetDir -Recurse -Force
    }
    
    # Create new directory
    New-Item -ItemType Directory -Path $TargetDir -Force | Out-Null
    
    # Copy files
    Write-Host "   -> Copying dist -> static..." -ForegroundColor Gray
    Copy-Item -Path "$SourceDir\*" -Destination $TargetDir -Recurse -Force
    
    $FileCount = (Get-ChildItem -Path $TargetDir -Recurse -File).Count
    Write-Host "   OK $FileCount files copied!" -ForegroundColor Green
    Write-Host ""
}
else {
    Write-Host "[2/4] Static file copy skipped" -ForegroundColor Gray
    Write-Host ""
}

# 3. Backend Build
if (-not $SkipBackend) {
    Write-Host "[3/4] Building Backend..." -ForegroundColor Yellow
    Set-Location "$BaseDir\chatq-server"
    
    # Check Maven Wrapper
    if (-not (Test-Path "mvnw.cmd")) {
        Write-Host "   X mvnw.cmd not found." -ForegroundColor Red
        exit 1
    }
    
    Write-Host "   -> Running ./mvnw clean package..." -ForegroundColor Gray
    & .\mvnw.cmd clean package -DskipTests
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "   X Backend build failed!" -ForegroundColor Red
        exit 1
    }
    
    # Find JAR/WAR file
    $ArtifactFile = Get-ChildItem -Path "target" -Include "*.jar", "*.war" -Exclude "*-sources.*", "*-javadoc.*", "*.original" -Recurse | Select-Object -First 1
    
    if ($ArtifactFile) {
        Write-Host "   OK Backend build completed!" -ForegroundColor Green
        Write-Host "   -> $($ArtifactFile.Extension.ToUpper().TrimStart('.')): $($ArtifactFile.Name)" -ForegroundColor Gray
    }
    else {
        Write-Host "   X JAR/WAR file not found." -ForegroundColor Red
        exit 1
    }
    Write-Host ""
}
else {
    Write-Host "[3/4] Backend build skipped" -ForegroundColor Gray
    Write-Host ""
}

# 4. Run Server (Optional)
if ($RunServer) {
    Write-Host "[4/4] Starting Server..." -ForegroundColor Yellow
    Set-Location "$BaseDir\chatq-server"
    
    $ArtifactFile = Get-ChildItem -Path "target" -Include "*.jar", "*.war" -Exclude "*-sources.*", "*-javadoc.*", "*.original" -Recurse | Select-Object -First 1
    
    if ($ArtifactFile) {
        $FileType = $ArtifactFile.Extension.ToUpper().TrimStart('.')
        Write-Host "   -> Starting server: java -jar $($ArtifactFile.Name)" -ForegroundColor Gray
        Write-Host "   -> File type: $FileType" -ForegroundColor Gray
        Write-Host "   -> Port: 8080" -ForegroundColor Gray
        Write-Host "   -> Press Ctrl+C to stop" -ForegroundColor Gray
        Write-Host ""
        
        & java -jar $ArtifactFile.FullName
    }
    else {
        Write-Host "   X JAR/WAR file not found to run." -ForegroundColor Red
        exit 1
    }
}
else {
    Write-Host "[4/4] Server execution skipped" -ForegroundColor Gray
    Write-Host ""
}

# Completion Message
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Deployment Completed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (-not $RunServer) {
    Set-Location "$BaseDir\chatq-server"
    $ArtifactFile = Get-ChildItem -Path "target" -Include "*.jar", "*.war" -Exclude "*-sources.*", "*-javadoc.*", "*.original" -Recurse | Select-Object -First 1
    
    if ($ArtifactFile) {
        Write-Host "How to run:" -ForegroundColor Yellow
        Write-Host "  cd chatq-server" -ForegroundColor Gray
        Write-Host "  java -jar target\$($ArtifactFile.Name)" -ForegroundColor Gray
        Write-Host ""
        Write-Host "Or:" -ForegroundColor Yellow
        Write-Host "  .\deploy.ps1 -RunServer" -ForegroundColor Gray
    }
}

Set-Location $BaseDir
