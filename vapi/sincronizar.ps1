# Publica na Vapi o que esta nos arquivos do projeto (a "fonte da verdade"):
#   - vapi/system-prompt.md            -> System Prompt do assistente
#   - vapi/tool-check-availability.json -> parametros da tool check_availability
#   - vapi/tool-book-meeting.json       -> parametros da tool book_meeting
# e reaplica as mensagens de silencio em portugues (atualizar-mensagens.ps1).
# Uso (na raiz do projeto):  powershell -ExecutionPolicy Bypass -File vapi\sincronizar.ps1
# Requer no .env: VAPI_PRIVATE_KEY e VAPI_ASSISTANT_ID

$ErrorActionPreference = 'Stop'
$utf8 = [System.Text.Encoding]::UTF8
$vars = @{}
Get-Content (Join-Path $PSScriptRoot '..\.env') | Where-Object { $_ -match '^\s*([A-Z_]+)\s*=\s*(.*)$' } | ForEach-Object {
    $vars[$Matches[1]] = $Matches[2].Trim()
}
if (-not $vars['VAPI_PRIVATE_KEY']) { throw 'Defina VAPI_PRIVATE_KEY no .env' }
if (-not $vars['VAPI_ASSISTANT_ID']) { throw 'Defina VAPI_ASSISTANT_ID no .env' }
$headers = @{ Authorization = "Bearer $($vars['VAPI_PRIVATE_KEY'])" }

function Invoke-Vapi($method, $path, $body) {
    $params = @{ Method = $method; Uri = "https://api.vapi.ai$path"; Headers = $headers; UseBasicParsing = $true }
    if ($body) {
        $params.ContentType = 'application/json; charset=utf-8'
        $params.Body = $utf8.GetBytes(($body | ConvertTo-Json -Depth 20))
    }
    $resp = Invoke-WebRequest @params
    return $utf8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json
}

function Read-Utf8($relativo) {
    return [System.IO.File]::ReadAllText((Join-Path $PSScriptRoot $relativo), $utf8)
}

# 1) System prompt: mantem o resto do "model" (provider, modelo, tools) e troca so o texto
$assistente = Invoke-Vapi 'GET' "/assistant/$($vars['VAPI_ASSISTANT_ID'])"
$model = $assistente.model
$prompt = Read-Utf8 'system-prompt.md'
$mensagens = @(@{ role = 'system'; content = $prompt })
$model | Add-Member -NotePropertyName messages -NotePropertyValue $mensagens -Force
$atualizado = Invoke-Vapi 'PATCH' "/assistant/$($vars['VAPI_ASSISTANT_ID'])" @{ model = $model }
Write-Output "Prompt publicado ($($prompt.Length) caracteres) no assistente $($atualizado.name)"

# 2) Tools: troca os parametros pelos dos arquivos JSON, identificando cada tool pelo nome
$arquivos = @{ 'check_availability' = 'tool-check-availability.json'; 'book_meeting' = 'tool-book-meeting.json' }
foreach ($toolId in $model.toolIds) {
    $tool = Invoke-Vapi 'GET' "/tool/$toolId"
    $nome = $tool.function.name
    if (-not $arquivos.ContainsKey($nome)) { continue }
    $funcao = @{
        name        = $nome
        description = $tool.function.description
        parameters  = (Read-Utf8 $arquivos[$nome] | ConvertFrom-Json)
    }
    $nova = Invoke-Vapi 'PATCH' "/tool/$toolId" @{ function = $funcao }
    Write-Output "Tool $nome publicada com parametros: $(($nova.function.parameters.properties.PSObject.Properties.Name) -join ', ')"
}

# 3) Mensagens de silencio em portugues
& (Join-Path $PSScriptRoot 'atualizar-mensagens.ps1') | Out-Null
Write-Output 'Mensagens de silencio em portugues reaplicadas.'
