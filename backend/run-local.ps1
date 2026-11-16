$ErrorActionPreference = "Stop"

$envFile = Join-Path $PSScriptRoot "..\.env"
if (-not (Test-Path -LiteralPath $envFile)) {
    throw "No se encontró el archivo .env en la raíz del proyecto: $envFile"
}

Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line -match '^(?<name>[A-Za-z_][A-Za-z0-9_]*)\s*=\s*(?<value>.*)$') {
        $name = $Matches.name
        $value = $Matches.value.Trim()
        if ($value.Length -ge 2 -and
            (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

if ([string]::IsNullOrWhiteSpace($env:CLOUDINARY_API_SECRET)) {
    throw "Completa CLOUDINARY_API_SECRET en el archivo .env con el secreto nuevo de Cloudinary."
}
if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    throw "Completa JWT_SECRET en el archivo .env con una clave Base64 aleatoria de al menos 32 bytes."
}
if ($env:MAIL_SMTP_AUTH -eq "true" -and [string]::IsNullOrWhiteSpace($env:MAIL_PASSWORD)) {
    throw "Completa MAIL_PASSWORD en el archivo .env con tu contraseña de aplicación SMTP."
}

Push-Location $PSScriptRoot
try {
    & .\mvnw.cmd spring-boot:run
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
