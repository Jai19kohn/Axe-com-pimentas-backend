[CmdletBinding()]
param(
    [string]$DestinationPath = $env:AXE_BACKUP_PATH,
    [ValidateRange(1, 3650)]
    [int]$RetentionCount = 14
)

$ErrorActionPreference = 'Stop'

function Invoke-Docker {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $output = & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed: docker $($Arguments -join ' ')"
    }

    return $output
}

if ([string]::IsNullOrWhiteSpace($DestinationPath)) {
    throw 'Set AXE_BACKUP_PATH or pass -DestinationPath with an existing network folder.'
}

if (-not (Test-Path -LiteralPath $DestinationPath -PathType Container)) {
    throw "Backup destination is unavailable or is not a folder: $DestinationPath"
}

$composeDirectory = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $composeDirectory 'compose.yaml'
if (-not (Test-Path -LiteralPath $composeFile -PathType Leaf)) {
    throw "Compose file not found: $composeFile"
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmssfff'
$backupName = "axe-com-pimentas-$timestamp.dump"
$containerDumpPath = "/tmp/$backupName"
$temporaryBackupPath = Join-Path $DestinationPath "$backupName.partial"
$localBackupPath = Join-Path ([System.IO.Path]::GetTempPath()) $backupName
$containerId = $null

Push-Location $composeDirectory
try {
    $containerId = (Invoke-Docker -Arguments @('compose', 'ps', '-q', 'postgres') | Out-String).Trim()
    if ([string]::IsNullOrWhiteSpace($containerId)) {
        throw 'The PostgreSQL Compose service is not running. Start it with: docker compose up -d'
    }

    Invoke-Docker -Arguments @(
        'exec', $containerId, 'pg_dump',
        '-U', 'postgres',
        '-d', 'axe_com_pimentas',
        '-Fc',
        '-f', $containerDumpPath
    ) | Out-Null

    Invoke-Docker -Arguments @(
        'exec', $containerId, 'pg_restore', '--list', $containerDumpPath
    ) | Out-Null

    $expectedLength = [long]((Invoke-Docker -Arguments @(
        'exec', $containerId, 'stat', '-c', '%s', $containerDumpPath
    ) | Out-String).Trim())
    if ($expectedLength -le 0) {
        throw 'The generated database dump is empty.'
    }

    Invoke-Docker -Arguments @('cp', "${containerId}:$containerDumpPath", $localBackupPath) | Out-Null
    if ((Get-Item -LiteralPath $localBackupPath).Length -ne $expectedLength) {
        throw 'The copied database dump size does not match the generated dump.'
    }
    $expectedHash = (Get-FileHash -LiteralPath $localBackupPath -Algorithm SHA256).Hash

    Copy-Item -LiteralPath $localBackupPath -Destination $temporaryBackupPath
    if ((Get-Item -LiteralPath $temporaryBackupPath).Length -ne $expectedLength) {
        throw 'The network copy size does not match the generated dump.'
    }
    if ((Get-FileHash -LiteralPath $temporaryBackupPath -Algorithm SHA256).Hash -ne $expectedHash) {
        throw 'The network copy hash does not match the generated dump.'
    }

    Move-Item -LiteralPath $temporaryBackupPath -Destination (Join-Path $DestinationPath $backupName)
    Write-Output "Backup created and validated: $(Join-Path $DestinationPath $backupName)"

    $backups = @(
        Get-ChildItem -LiteralPath $DestinationPath -File -Filter 'axe-com-pimentas-*.dump' |
            Sort-Object -Property LastWriteTimeUtc -Descending
    )
    if ($backups.Count -gt $RetentionCount) {
        $backups |
            Select-Object -Skip $RetentionCount |
            ForEach-Object {
                Remove-Item -LiteralPath $_.FullName
                Write-Output "Removed old backup: $($_.FullName)"
            }
    }
}
finally {
    if ($containerId) {
        & docker exec $containerId rm -f $containerDumpPath > $null 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "Could not remove temporary dump from the PostgreSQL container: $containerDumpPath"
        }
    }

    if (Test-Path -LiteralPath $localBackupPath) {
        Remove-Item -LiteralPath $localBackupPath
    }
    if (Test-Path -LiteralPath $temporaryBackupPath) {
        Remove-Item -LiteralPath $temporaryBackupPath
    }

    Pop-Location
}
