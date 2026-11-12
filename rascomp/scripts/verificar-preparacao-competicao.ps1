# RASCOMP — verificacao NAO destrutiva do servidor Windows.
# Executar: powershell -ExecutionPolicy Bypass -File .\scripts\verificar-preparacao-competicao.ps1
# Nao configura DNS, nao inicia backend e nao acessa credenciais.
$ErrorActionPreference = 'Continue'
Write-Host '=== RASCOMP: verificacao do ambiente (somente leitura) ==='
foreach ($cmd in @('java','mvn','cloudflared','mysql','mysqldump')) {
  $found = Get-Command $cmd -ErrorAction SilentlyContinue
  if ($found) { Write-Host "[OK] $cmd encontrado: $($found.Source)" }
  else { Write-Host "[PENDENTE] $cmd nao encontrado no PATH" }
}
try { java -version 2>&1 | Select-Object -First 3 | Out-Host } catch {}
try { cloudflared --version | Out-Host } catch {}
$svc = Get-Service cloudflared -ErrorAction SilentlyContinue
if ($svc) { Write-Host "[INFO] Servico cloudflared: $($svc.Status) ($($svc.StartType))" }
else { Write-Host '[PENDENTE] Servico cloudflared nao instalado' }
try {
  $local = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/actuator/health' -UseBasicParsing -TimeoutSec 8
  Write-Host "[INFO] API local health HTTP $($local.StatusCode) — NAO valida banco"
} catch { Write-Host "[INFO] API local indisponivel: $($_.Exception.Message)" }
try {
  $remote = Invoke-WebRequest -Uri 'https://api-teste.rasufrb.site/actuator/health' -UseBasicParsing -TimeoutSec 15
  Write-Host "[INFO] API pelo tunnel HTTP $($remote.StatusCode) — NAO valida banco"
} catch { Write-Host "[INFO] API pelo tunnel indisponivel: $($_.Exception.Message)" }
Write-Host '=== Teste CORS (preflight; sem login nem escrita) ==='
try {
  $response = Invoke-WebRequest -Uri 'https://api-teste.rasufrb.site/api/v1/auth/login' -Method Options -UseBasicParsing -TimeoutSec 15 -Headers @{
    Origin='https://app.rasufrb.site'
    'Access-Control-Request-Method'='POST'
    'Access-Control-Request-Headers'='content-type'
  }
  Write-Host "[INFO] Preflight HTTP $($response.StatusCode)"
  Write-Host "[INFO] Allow-Origin: $($response.Headers['Access-Control-Allow-Origin'])"
} catch { Write-Host "[INFO] Preflight nao validado: $($_.Exception.Message)" }
Write-Host 'Nao conclua homologacao com base apenas neste script.'
