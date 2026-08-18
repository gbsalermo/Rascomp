$ErrorActionPreference = "Stop"

$envFile = Join-Path $PSScriptRoot ".env.local"

function ConvertTo-PlainText([Security.SecureString]$secureString) {
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureString)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

if (-not (Test-Path $envFile)) {
    Write-Host "Primeira execução local do Rascomp." -ForegroundColor Cyan
    Write-Host "As credenciais ficarão somente em .env.local, que está ignorado pelo Git." -ForegroundColor Yellow

    $username = Read-Host "Usuário MySQL [root]"
    if ([string]::IsNullOrWhiteSpace($username)) {
        $username = "root"
    }

    $securePassword = Read-Host "Senha MySQL" -AsSecureString
    $password = ConvertTo-PlainText $securePassword

    @(
        "DB_USERNAME=$username"
        "DB_PASSWORD=$password"
    ) | Set-Content -Path $envFile -Encoding UTF8

    $password = $null
    Write-Host ".env.local criado." -ForegroundColor Green
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $parts = $line -split "=", 2
        if ($parts.Count -eq 2) {
            [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1], "Process")
        }
    }
}

if (-not $env:DB_USERNAME -or -not $env:DB_PASSWORD) {
    throw "DB_USERNAME e DB_PASSWORD precisam estar definidos em .env.local."
}

Write-Host "Iniciando Rascomp com MySQL local..." -ForegroundColor Cyan
Set-Location $PSScriptRoot

if (Test-Path (Join-Path $PSScriptRoot "mvnw.cmd")) {
    & .\mvnw.cmd spring-boot:run
}
elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    & mvn spring-boot:run
}
else {
    throw "Maven não encontrado. Instale o Maven ou restaure o Maven Wrapper do projeto."
}
