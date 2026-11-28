@echo off
setlocal EnableExtensions
title Echo Client Recovery Updater
set "ECHO_UPDATER_SELF=%~f0"
set "ECHO_UPDATER_TARGET=%~1"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$c = Get-Content -LiteralPath $env:ECHO_UPDATER_SELF -Raw; $i = $c.LastIndexOf('#__PS_START__'); if ($i -lt 0) { throw 'Script header corrupted.' }; Invoke-Expression $c.Substring($i)"
echo.
pause
exit /b %ERRORLEVEL%

#__PS_START__
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Add-Type -AssemblyName System.IO.Compression.FileSystem

$ApiUrl  = 'https://api.github.com/repos/kulitharanmith0-a11y/clumps-releases/contents/latest.json'
$RawUrl  = 'https://raw.githubusercontent.com/kulitharanmith0-a11y/clumps-releases/main/latest.json'
$DefaultJarName = 'Clumps-fabric-1.21.11-29.0.0.1.jar'

function Write-Header {
    Clear-Host
    Write-Host ''
    Write-Host '  =======================================================' -ForegroundColor Cyan
    Write-Host '            ECHO CLIENT  -  AUTO RECOVERY UPDATER' -ForegroundColor White
    Write-Host '  =======================================================' -ForegroundColor Cyan
    Write-Host ''
}

function Get-FileSha256([string]$Path) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $fs = [System.IO.File]::OpenRead($Path)
        try {
            $hash = $sha.ComputeHash($fs)
            return ([BitConverter]::ToString($hash) -replace '-', '').ToLowerInvariant()
        } finally { $fs.Dispose() }
    } finally { $sha.Dispose() }
}

function Test-IsEchoClientJar([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { return $false }
    try {
        $info = Get-Item -LiteralPath $Path
        if ($info.Length -lt 100000) { return $false }
        $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
        try {
            $hasInit = ($null -ne $zip.GetEntry('com/blamejared/clumps/client/init/Initializer.class'))
            $hasPre  = ($null -ne $zip.GetEntry('com/blamejared/clumps/client/PreLaunch.class'))
            $hasMain = ($null -ne $zip.GetEntry('com/blamejared/clumps/client/ClumpsClient.class'))
            $hasMod  = ($null -ne $zip.GetEntry('fabric.mod.json'))
            if (-not $hasMod) { return $false }
            if ($hasInit -or $hasPre) { return $true }
            if ($hasMain) {
                $sr = New-Object System.IO.StreamReader($zip.GetEntry('fabric.mod.json').Open())
                try { $json = $sr.ReadToEnd() } finally { $sr.Dispose() }
                return ($json -match '"id"\s*:\s*"clumps"')
            }
            return $false
        } finally { $zip.Dispose() }
    } catch {
        return $false
    }
}

function Get-JarVersion([string]$Path) {
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
        try {
            $e = $zip.GetEntry('META-INF/MANIFEST.MF')
            if ($null -eq $e) { return 'unknown' }
            $sr = New-Object System.IO.StreamReader($e.Open())
            try { $txt = $sr.ReadToEnd() } finally { $sr.Dispose() }
            if ($txt -match '(?:Build-Release|Implementation-Version):\s*([^\r\n]+)') {
                $v = $Matches[1].Trim()
                if ($v -notmatch '^v') { $v = "v$v" }
                return $v
            }
            return 'unknown'
        } finally { $zip.Dispose() }
    } catch {
        return 'unknown'
    }
}

function Get-LatestManifest {
    $headers = @{ 'User-Agent' = 'EchoClient-Updater'; 'Accept' = 'application/vnd.github+json' }
    try {
        $api = Invoke-RestMethod -Uri $ApiUrl -Headers $headers -TimeoutSec 20
        if ($api -and $api.content) {
            $raw = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String(($api.content -replace '\s', '')))
            return ($raw | ConvertFrom-Json)
        }
    } catch {
        # Fallback to raw GitHub CDN with cache buster
    }
    $bust = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    return (Invoke-RestMethod -Uri "$RawUrl`?t=$bust" -Headers @{ 'User-Agent' = 'EchoClient-Updater' } -TimeoutSec 20)
}

function Find-ModsDirectories {
    $candidates = New-Object System.Collections.Generic.List[string]
    $app = $env:APPDATA
    $usr = $env:USERPROFILE
    $selfDir = Split-Path -Parent $env:ECHO_UPDATER_SELF

    $directDirs = @(
        $selfDir,
        (Join-Path $app '.minecraft\mods'),
        (Join-Path $app '.feather\user-mods'),
        (Join-Path $app '.tlauncher\legacy\Minecraft\game\mods')
    )
    foreach ($d in $directDirs) {
        if ($d -and (Test-Path -LiteralPath $d -PathType Container)) { $candidates.Add($d) }
    }

    $globPatterns = @(
        (Join-Path $app '.fastclient\profiles\*\mods'),
        (Join-Path $app 'ModrinthApp\profiles\*\mods'),
        (Join-Path $app 'com.modrinth.theseus\profiles\*\mods'),
        (Join-Path $app 'PrismLauncher\instances\*\.minecraft\mods'),
        (Join-Path $app 'PrismLauncher\instances\*\minecraft\mods'),
        (Join-Path $app 'MultiMC\instances\*\.minecraft\mods'),
        (Join-Path $app 'MultiMC\instances\*\minecraft\mods'),
        (Join-Path $usr 'curseforge\minecraft\Instances\*\mods'),
        (Join-Path $app '.feather\user-mods\*'),
        (Join-Path $usr '.lunarclient\profiles\*\mods'),
        (Join-Path $usr '.lunarclient\offline\multiver\mods*'),
        (Join-Path $app 'ATLauncher\instances\*\mods'),
        (Join-Path $app 'gdlauncher_next\instances\*\mods'),
        (Join-Path $app '.minecraft\versions\*\mods')
    )
    foreach ($pat in $globPatterns) {
        try {
            Get-ChildItem -Path $pat -Directory -ErrorAction SilentlyContinue | ForEach-Object {
                $candidates.Add($_.FullName)
            }
        } catch {}
    }

    $seen = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
    $unique = New-Object System.Collections.Generic.List[string]
    foreach ($d in $candidates) {
        try {
            $full = [System.IO.Path]::GetFullPath($d)
            if ($seen.Add($full)) { $unique.Add($full) }
        } catch {}
    }
    return $unique
}

function Clean-StaleUpdaterFiles([string]$ModsDir) {
    try {
        Get-ChildItem -LiteralPath $ModsDir -File -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -like 'fabric-loader-cache-*.jar' -or
            $_.Name -eq 'fabric-loader-meta.dat' -or
            $_.Name -eq '.fabric-cache-upd'
        } | ForEach-Object {
            try { Remove-Item -LiteralPath $_.FullName -Force -ErrorAction SilentlyContinue } catch {}
        }
    } catch {}
}

function Copy-WithRetry([string]$Source, [string]$Dest) {
    while ($true) {
        try {
            [System.IO.File]::Copy($Source, $Dest, $true)
            return $true
        } catch [System.IO.IOException] {
            Write-Host ''
            Write-Host "  [!] File is currently in use: $Dest" -ForegroundColor Yellow
            Write-Host '      Please close Minecraft completely, then press ENTER to retry (or type S to skip).' -ForegroundColor Yellow
            $ans = Read-Host '      Action'
            if ($ans -match '^[sS]') { return $false }
        }
    }
}

Write-Header
$tempJar = Join-Path ([System.IO.Path]::GetTempPath()) ("echo-update-" + [Guid]::NewGuid().ToString('N') + ".jar")

try {
    Write-Host '  [1/4] Checking latest Echo Client release...' -ForegroundColor Cyan
    $manifest = Get-LatestManifest
    if (-not $manifest -or -not $manifest.version -or -not $manifest.url) {
        throw 'Could not parse latest release info from server.'
    }
    $latestVer = [string]$manifest.version
    $expectedSha = ([string]$manifest.sha256).Trim().ToLowerInvariant()
    $downloadUrl = [string]$manifest.url
    Write-Host "        Latest version : v$latestVer" -ForegroundColor Green
    if ($manifest.summary) {
        Write-Host "        Highlights     : $($manifest.summary)" -ForegroundColor Gray
    }
    Write-Host ''

    Write-Host "  [2/4] Downloading v$latestVer..." -ForegroundColor Cyan
    $wc = New-Object System.Net.WebClient
    $wc.Headers.Add('User-Agent', 'EchoClient-Updater')
    try {
        $wc.DownloadFile($downloadUrl, $tempJar)
    } finally { $wc.Dispose() }

    $actualSha = Get-FileSha256 $tempJar
    if ($expectedSha -and ($actualSha -ne $expectedSha)) {
        throw "Checksum mismatch! Expected $expectedSha but got $actualSha."
    }
    if (-not (Test-IsEchoClientJar $tempJar)) {
        throw 'Downloaded file failed integrity check.'
    }
    $mb = [Math]::Round((Get-Item -LiteralPath $tempJar).Length / 1MB, 2)
    Write-Host "        Downloaded & verified ($mb MB, SHA-256 OK)." -ForegroundColor Green
    Write-Host ''

    Write-Host '  [3/4] Locating your Echo Client installation(s)...' -ForegroundColor Cyan
    $targets = New-Object System.Collections.Generic.List[string]
    $installIntoDir = $null
    $dragArg = $env:ECHO_UPDATER_TARGET

    if ($dragArg -and $dragArg.Trim().Length -gt 0) {
        $dragPath = $dragArg.Trim().Trim('"')
        if (Test-Path -LiteralPath $dragPath -PathType Leaf) {
            if (Test-IsEchoClientJar $dragPath) {
                $targets.Add([System.IO.Path]::GetFullPath($dragPath))
            } else {
                Write-Host "        Dropped file doesn't look like an Echo Client JAR: $dragPath" -ForegroundColor Yellow
                $confirm = Read-Host '        Replace it anyway? (Y/N)'
                if ($confirm -match '^[yY]') { $targets.Add([System.IO.Path]::GetFullPath($dragPath)) }
            }
        } elseif (Test-Path -LiteralPath $dragPath -PathType Container) {
            Get-ChildItem -LiteralPath $dragPath -Filter '*.jar' -File -ErrorAction SilentlyContinue | ForEach-Object {
                if (Test-IsEchoClientJar $_.FullName) { $targets.Add($_.FullName) }
            }
            if ($targets.Count -eq 0) { $installIntoDir = [System.IO.Path]::GetFullPath($dragPath) }
        }
    } else {
        $dirs = Find-ModsDirectories
        foreach ($dir in $dirs) {
            Get-ChildItem -LiteralPath $dir -Filter '*.jar' -File -ErrorAction SilentlyContinue | ForEach-Object {
                if (Test-IsEchoClientJar $_.FullName) {
                    $targets.Add($_.FullName)
                }
            }
        }
    }

    if ($targets.Count -eq 0 -and -not $installIntoDir) {
        Write-Host '        No existing Echo Client JAR was found in standard launcher folders.' -ForegroundColor Yellow
        Write-Host '        Paste your mods folder path (or drag & drop the folder here) and press ENTER:' -ForegroundColor White
        $manual = (Read-Host '        Path').Trim().Trim('"')
        if ($manual -and (Test-Path -LiteralPath $manual -PathType Leaf)) {
            $targets.Add([System.IO.Path]::GetFullPath($manual))
        } elseif ($manual -and (Test-Path -LiteralPath $manual -PathType Container)) {
            Get-ChildItem -LiteralPath $manual -Filter '*.jar' -File -ErrorAction SilentlyContinue | ForEach-Object {
                if (Test-IsEchoClientJar $_.FullName) { $targets.Add($_.FullName) }
            }
            if ($targets.Count -eq 0) { $installIntoDir = [System.IO.Path]::GetFullPath($manual) }
        } else {
            throw 'No valid folder or JAR path provided.'
        }
    }

    Write-Host ''
    Write-Host "  [4/4] Updating to v$latestVer..." -ForegroundColor Cyan
    $updated = 0
    $alreadyLatest = 0

    if ($installIntoDir) {
        $dest = Join-Path $installIntoDir $DefaultJarName
        if (Copy-WithRetry $tempJar $dest) {
            Clean-StaleUpdaterFiles $installIntoDir
            Write-Host "        [INSTALLED] $dest" -ForegroundColor Green
            $updated++
        }
    } else {
        foreach ($t in $targets) {
            $oldSha = Get-FileSha256 $t
            $oldVer = Get-JarVersion $t
            $dir = Split-Path -Parent $t
            if ($oldSha -eq $actualSha) {
                Clean-StaleUpdaterFiles $dir
                Write-Host "        [UP TO DATE] $t (v$latestVer)" -ForegroundColor DarkGreen
                $alreadyLatest++
                continue
            }
            if (Copy-WithRetry $tempJar $t) {
                Clean-StaleUpdaterFiles $dir
                Write-Host "        [UPDATED] $t  ($oldVer -> v$latestVer)" -ForegroundColor Green
                $updated++
            }
        }
    }

    Write-Host ''
    Write-Host '  =======================================================' -ForegroundColor Green
    if ($updated -gt 0) {
        Write-Host "   Done! Updated $updated Echo Client installation(s) to v$latestVer." -ForegroundColor Green
        Write-Host '   You can now launch Minecraft normally.' -ForegroundColor White
    } elseif ($alreadyLatest -gt 0) {
        Write-Host "   All $alreadyLatest Echo Client installation(s) are already on v$latestVer!" -ForegroundColor Green
    } else {
        Write-Host '   No files were modified.' -ForegroundColor Yellow
    }
    Write-Host '  =======================================================' -ForegroundColor Green
    exit 0
} catch {
    Write-Host ''
    Write-Host "  [ERROR] $($_.Exception.Message)" -ForegroundColor Red
    Write-Host '  If this keeps happening, drag your mods folder directly onto EchoUpdater.bat.' -ForegroundColor Yellow
    exit 1
} finally {
    if (Test-Path -LiteralPath $tempJar) {
        try { Remove-Item -LiteralPath $tempJar -Force -ErrorAction SilentlyContinue } catch {}
    }
}
