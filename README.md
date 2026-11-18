# atend-ia-piloto

Backend de integração entre o atendente de IA (Vapi) e a agenda do consultor da
PS Varejo. Projeto piloto: a IA atende a ligação, conversa com o cliente e, por
meio deste backend, consulta horários livres e cria a reunião confirmada com o
Patrick Salomão.

A agenda é gerenciada pelo **[Cal.com](https://cal.com)**, que fica conectado ao
Google Calendar do consultor. O backend não fala com o Google diretamente — só
com a API v2 do Cal.com (autenticação por chave de API, sem Google Cloud).

```
Ligação ──> Vapi (STT + LLM + TTS) ──tools──> este backend ──> Cal.com ──> Google Calendar
```

## Endpoints (tools da Vapi)

| Tool | Endpoint | O que faz |
|---|---|---|
| `check_availability` | `POST /vapi/tools/check-availability` | Busca horários livres no Cal.com e devolve poucas opções, espaçadas e espalhadas entre dias |
| `book_meeting` | `POST /vapi/tools/book-meeting` | Cria a reserva no Cal.com (que revalida a disponibilidade) |

- Toda chamada a `/vapi/**` exige o header `X-Vapi-Secret` com o segredo configurado.
- Os endpoints recebem o envelope da Vapi (`message.toolCallList[]`) e respondem
  no formato `{"results":[{"toolCallId":"...","result":"..."}]}`.
- Se o LLM não informar o telefone no `book_meeting`, é usado o número de quem ligou
  (`message.call.customer.number`).

## Configuração do Cal.com

1. **Apps instalados > Calendário**: conecte o Google Calendar e ligue
   "Verificar se há conflitos" na agenda do consultor.
2. **Disponibilidade**: seg–sex, 09:00–18:00, fuso `America/Sao_Paulo`.
3. **Links > Novo**: "Conversa com Patrick Salomão", 30 min.
   - *Limites e intervalos*: aviso mínimo (ex: 2h) e intervalo entre reuniões.
   - *Formulário de reserva*: telefone obrigatório, e-mail opcional.
4. **Configurações > Desenvolvedor > Chaves de API**
   (`https://app.cal.com/settings/developer/api-keys`): gere a chave.
5. Descubra o ID numérico do tipo de evento:

```powershell
(Invoke-RestMethod -Uri "https://api.cal.com/v2/event-types" -Headers @{ Authorization = "Bearer $env:CAL_API_KEY"; "cal-api-version" = "2024-06-14" }).data | Select-Object id, slug, title
```

## Rodando localmente

1. Copie `.env.example` para `.env` e preencha (o `.env` está no `.gitignore`;
   o Spring lê esse arquivo automaticamente via `spring.config.import`).
2. `mvn spring-boot:run` → sobe em `http://localhost:8080`.

Se faltar alguma variável obrigatória a aplicação nem sobe e diz qual é
(`@Validated` no `AgendaProperties`).

### Testando sem a Vapi (PowerShell)

```powershell
$body = '{"message":{"type":"tool-calls","toolCallList":[{"id":"teste1","function":{"name":"check_availability","arguments":{"dataInicio":"2026-11-18","dataFim":"2026-11-21","preferenciaPeriodo":"tarde"}}}]}}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/vapi/tools/check-availability -ContentType 'application/json' -Headers @{ 'X-Vapi-Secret' = 'SEU_SEGREDO' } -Body $body | ConvertTo-Json -Depth 5
```

## Expondo para a internet (teste com a Vapi)

Enquanto não há deploy definitivo, use o [ngrok](https://ngrok.com): `ngrok http 8080`.
A URL pública gerada (ex: `https://abc123.ngrok-free.app`) é a que vai nas tools da Vapi.

## Tools na Vapi

Nas duas tools, em *Server*: URL do endpoint e, em *Secret*/headers,
`X-Vapi-Secret` com o mesmo valor de `AGENDA_VAPI_WEBHOOK_SECRET`.

**`check_availability`**: `dataInicio` (string, AAAA-MM-DD), `dataFim` (string,
AAAA-MM-DD), `preferenciaPeriodo` (string opcional: `manha` ou `tarde`).

**`book_meeting`**: `horarioEscolhido` (string, obrigatório — o campo `horario`
devolvido por `check_availability`), `nome` (obrigatório), `empresa`, `telefone`,
`email` (opcionais).

## Variáveis de ambiente

| Variável | Default | O que é |
|---|---|---|
| `CAL_API_KEY` | — (obrigatório) | chave de API do Cal.com (`cal_live_...`) |
| `CAL_EVENT_TYPE_ID` | — (obrigatório) | ID numérico do tipo de evento |
| `AGENDA_VAPI_WEBHOOK_SECRET` | — (obrigatório) | segredo exigido no header `X-Vapi-Secret` |
| `CAL_BASE_URL` | `https://api.cal.com/v2` | URL base da API do Cal.com |
| `AGENDA_TIMEZONE` | `America/Sao_Paulo` | fuso usado para buscar e falar os horários |
| `AGENDA_MAX_SLOTS_RETURNED` | `5` | quantas opções devolver por chamada |
| `AGENDA_MAX_SLOTS_PER_DAY` | `2` | máximo de opções do mesmo dia |
| `AGENDA_MAX_DAYS_AHEAD` | `30` | limite de dias à frente na busca |

Expediente, dias úteis, duração e intervalos são configurados no próprio Cal.com.
