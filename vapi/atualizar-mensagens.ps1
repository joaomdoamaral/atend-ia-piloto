# Atualiza as mensagens de silencio (idle) e de encerramento do assistente via API da Vapi,
# porque o painel recoloca as frases padrao em ingles quando a lista fica vazia.
# Uso (na raiz do projeto):  powershell -ExecutionPolicy Bypass -File vapi\atualizar-mensagens.ps1
# Requer no .env: VAPI_PRIVATE_KEY e VAPI_ASSISTANT_ID

$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot '..\.env'
$vars = @{}
Get-Content $envFile | Where-Object { $_ -match '^\s*([A-Z_]+)\s*=\s*(.*)$' } | ForEach-Object {
    $vars[$Matches[1]] = $Matches[2].Trim()
}
if (-not $vars['VAPI_PRIVATE_KEY']) { throw 'Defina VAPI_PRIVATE_KEY no .env' }
if (-not $vars['VAPI_ASSISTANT_ID']) { throw 'Defina VAPI_ASSISTANT_ID no .env' }

$url = "https://api.vapi.ai/assistant/$($vars['VAPI_ASSISTANT_ID'])"
$headers = @{ Authorization = "Bearer $($vars['VAPI_PRIVATE_KEY'])" }

# Le o assistente atual para preservar o resto do messagePlan
$atual = Invoke-RestMethod -Uri $url -Headers $headers
$messagePlan = @{}
if ($atual.messagePlan) {
    $atual.messagePlan.PSObject.Properties | ForEach-Object { $messagePlan[$_.Name] = $_.Value }
}
$messagePlan['idleMessages'] = @(
    'Oi, você ainda está aí?',
    'Pode falar, tô te ouvindo.',
    'Se preferir, posso repetir a pergunta.'
)
$messagePlan['idleTimeoutSeconds'] = 8
$messagePlan['idleMessageMaxSpokenCount'] = 3

# Mexe so nas mensagens de silencio: encerramento e voicemail ficam como estao no painel.
# ATENCAO: publicar o assistente pelo painel recoloca as frases em ingles -> rode este script de novo depois.
$body = @{
    messagePlan = $messagePlan
} | ConvertTo-Json -Depth 10

$bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
$novo = Invoke-RestMethod -Method Patch -Uri $url -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $bytes

Write-Output "Assistente atualizado: $($novo.name)"
Write-Output "Idle messages: $($novo.messagePlan.idleMessages -join ' | ')"
Write-Output "Idle timeout: $($novo.messagePlan.idleTimeoutSeconds)s, max: $($novo.messagePlan.idleMessageMaxSpokenCount)"
Write-Output "End call message: $($novo.endCallMessage)"
Write-Output "End call phrases: $($novo.endCallPhrases -join ', ')"
