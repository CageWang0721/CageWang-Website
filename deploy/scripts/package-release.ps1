[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^\d{4}\.\d{2}\.\d{2}\.\d+$')]
    [string]$Version
)

$ErrorActionPreference = 'Stop'
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$outputDirectory = Join-Path $root 'artifacts\releases'
$archiveName = "wineclouds-deploy-$Version.tar.gz"
$archivePath = Join-Path $outputDirectory $archiveName
$checksumPath = "$archivePath.sha256"

if (Test-Path -LiteralPath $archivePath) {
    throw "Release archive already exists: $archivePath"
}
if (Test-Path -LiteralPath $checksumPath) {
    throw "Release checksum already exists: $checksumPath"
}

$includePaths = @(
    '.dockerignore',
    '.env.example',
    '.env.production.example',
    '.gitattributes',
    '.gitignore',
    'README.md',
    'backend',
    'deploy',
    'docs',
    'frontend',
    'docker-compose.yml',
    'docker-compose.prod.yml',
    'package.json',
    'package-lock.json'
)

$excludedRelativePath = '(^|/)(node_modules|target|dist|\.nuxt|\.output|coverage)(/|$)|^deploy/certs(/|$)|^docs/superpowers(/|$)|\.tsbuildinfo$'
$textExtensions = @(
    '.conf', '.css', '.env', '.example', '.html', '.java', '.js', '.json',
    '.md', '.mjs', '.properties', '.ps1', '.sh', '.sql', '.ts', '.vue',
    '.xml', '.yaml', '.yml'
)
$sensitiveEnvironmentKeys = @(
    'ADMIN_INITIAL_PASSWORD', 'BACKUP_ENCRYPTION_PASSWORD', 'COS_SECRET_ID',
    'COS_SECRET_KEY', 'GITHUB_TOKEN', 'JWT_SECRET', 'MAIL_PASSWORD',
    'MYSQL_PASSWORD', 'MYSQL_ROOT_PASSWORD', 'REDIS_PASSWORD'
)

$filesToScan = [System.Collections.Generic.List[System.IO.FileInfo]]::new()
foreach ($relativePath in $includePaths) {
    $absolutePath = Join-Path $root $relativePath
    if (-not (Test-Path -LiteralPath $absolutePath)) {
        throw "Required release path is missing: $relativePath"
    }
    $item = Get-Item -LiteralPath $absolutePath
    if ($item.PSIsContainer) {
        Get-ChildItem -LiteralPath $absolutePath -Recurse -File -Force | ForEach-Object {
            $relative = $_.FullName.Substring($root.Length + 1).Replace('\', '/')
            if ($relative -notmatch $excludedRelativePath) {
                $filesToScan.Add($_)
            }
        }
    } else {
        $filesToScan.Add($item)
    }
}

$findings = [System.Collections.Generic.List[string]]::new()
foreach ($file in $filesToScan) {
    $relative = $file.FullName.Substring($root.Length + 1).Replace('\', '/')
    $isText = $textExtensions -contains $file.Extension.ToLowerInvariant() -or $file.Name -eq 'Dockerfile'
    if (-not $isText) {
        continue
    }

    $content = [System.IO.File]::ReadAllText($file.FullName)
    if ($content -match '-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----') {
        $findings.Add("private key marker in $relative")
    }
    if ($content -match '(?i)(?:gh[pousr]_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,})') {
        $findings.Add("GitHub token pattern in $relative")
    }
    if ($content -match '(?i)AKID[A-Za-z0-9]{20,}') {
        $findings.Add("cloud access key pattern in $relative")
    }

    if ($file.Name -in @('.env.example', '.env.production.example')) {
        foreach ($line in ($content -split "`r?`n")) {
            if ($line -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$') {
                continue
            }
            $key = $Matches[1]
            $value = $Matches[2].Trim()
            if ($key -notin $sensitiveEnvironmentKeys -or [string]::IsNullOrWhiteSpace($value)) {
                continue
            }
            if ($value -notmatch '^(replace-|change-|example|your-|<)') {
                $findings.Add("non-placeholder $key in $relative")
            }
        }
    }
}

if ($findings.Count -gt 0) {
    throw "Secret scan failed:`n - $($findings -join "`n - ")"
}

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$tarArguments = @(
    '-czf', $archivePath,
    '--exclude=backend/target',
    '--exclude=deploy/certs',
    '--exclude=docs/superpowers',
    '--exclude=frontend/admin/dist',
    '--exclude=frontend/admin/node_modules',
    '--exclude=frontend/web/.nuxt',
    '--exclude=frontend/web/.output',
    '--exclude=frontend/web/node_modules',
    '--exclude=*/node_modules',
    '--exclude=*/coverage',
    '--exclude=*.tsbuildinfo'
) + $includePaths

try {
    Push-Location $root
    try {
        & tar @tarArguments
        if ($LASTEXITCODE -ne 0) {
            throw "tar failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }

    $entries = @(& tar -tzf $archivePath)
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to inspect the generated archive'
    }
    $invalidEntries = @($entries | Where-Object {
        $_ -match '(^|/)\.\.?(/|$)' -or
        $_ -match '(^|/)(\.git|artifacts|node_modules|target|dist|\.nuxt|\.output)(/|$)' -or
        $_ -match '(^|/)deploy/certs(/|$)' -or
        $_ -match '(^|/)\.env(?:\.production)?$' -or
        $_ -match '\.(?:key|p12|pfx)$'
    })
    if ($invalidEntries.Count -gt 0) {
        throw "Generated archive contains forbidden paths: $($invalidEntries -join ', ')"
    }

    $hash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
    [System.IO.File]::WriteAllText(
        $checksumPath,
        "$hash *$archiveName$([Environment]::NewLine)",
        [System.Text.UTF8Encoding]::new($false)
    )

    [PSCustomObject]@{
        Version = $Version
        Archive = $archivePath
        SizeMB = [math]::Round((Get-Item -LiteralPath $archivePath).Length / 1MB, 2)
        SHA256 = $hash
        Files = $entries.Count
    }
} catch {
    if (Test-Path -LiteralPath $archivePath) {
        Remove-Item -LiteralPath $archivePath
    }
    if (Test-Path -LiteralPath $checksumPath) {
        Remove-Item -LiteralPath $checksumPath
    }
    throw
}
