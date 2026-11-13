# atend-ia-piloto

Backend de integração entre o atendente de IA (Vapi) e a agenda do Google Calendar
do consultor da PS Varejo. Projeto piloto: a IA atende a ligação, conversa com o
cliente e, por meio deste backend, consulta horários livres e cria o evento
confirmado na agenda do Patrick Salomão.

## O que já está pronto neste repositório

- Projeto Spring Boot (Java 21, Maven) com dois endpoints que a Vapi chama como
  "tools" durante a ligação:
  - `POST /vapi/tools/check-availability` — consulta horários livres via
    Google Calendar FreeBusy API
  - `POST /vapi/tools/book-meeting` — cria o evento confirmado via Google
    Calendar Events API (com revalidação de conflito no momento da criação)
- Filtro de segurança simples que exige um segredo compartilhado no header
  `X-Vapi-Secret` em toda chamada a `/vapi/**`
- Configuração de horário comercial, dias úteis, duração da reunião e fuso
  horário via `application.yml` / variáveis de ambiente (nada disso está
  hardcoded)

⚠️ **Não consegui rodar `mvn compile` aqui no ambiente** — o Maven Central não é
acessível a partir deste sandbox (só tenho saída liberada pra npm, pip e
GitHub). O código segue os padrões normais da lib oficial do Google
(`google-api-services-calendar` + `google-auth-library-oauth2-http`), mas a
primeira validação de build de verdade vai acontecer na sua máquina — se
aparecer algum erro de compilação, me manda o log que eu ajusto.

## Passo a passo do que falta você fazer

### 1. Criar o projeto no Google Cloud e a Service Account

1. Acesse [console.cloud.google.com](https://console.cloud.google.com) e crie
   um projeto novo (ex: `ps-varejo-atendimento`)
2. Em **APIs e serviços > Biblioteca**, ative a **Google Calendar API**
3. Em **APIs e serviços > Credenciais > Criar credenciais > Conta de
   serviço**, crie uma service account (ex: `atend-ia-calendar`)
4. Na aba **Chaves** dessa service account, crie uma chave nova tipo **JSON**
   — isso baixa um arquivo `.json`. Guarde ele, é a credencial da aplicação.

### 2. Compartilhar a agenda do Patrick com a service account

1. O Patrick abre o Google Calendar dele, vai em **Configurações da agenda >
   Compartilhar com pessoas específicas**
2. Adiciona o e-mail da service account (algo como
   `atend-ia-calendar@ps-varejo-atendimento.iam.gserviceaccount.com`, você
   encontra esse e-mail na própria tela da service account)
3. Permissão: **"Fazer alterações em eventos"**

### 3. Rodar o projeto localmente

1. Clone o repositório e coloque o `service-account.json` baixado dentro de
   uma pasta `credentials/` na raiz do projeto (essa pasta já está no
   `.gitignore`, não vai parar no Git)
2. Defina as variáveis de ambiente (ou edite o `application.yml` direto para
   testar):

```bash
export AGENDA_CALENDAR_ID="email-da-agenda-do-patrick@gmail.com"
export AGENDA_CREDENTIALS_PATH="$(pwd)/credentials/service-account.json"
export AGENDA_VAPI_WEBHOOK_SECRET="escolha-um-segredo-forte-aqui"
```

3. Rodar: `./mvnw spring-boot:run` (ou `mvn spring-boot:run`)
4. A API sobe em `http://localhost:8080`

### 4. Expor pra internet (pra testar com a Vapi de verdade)

Enquanto não tem deploy definitivo, use o [ngrok](https://ngrok.com):

```bash
ngrok http 8080
```

Isso te dá uma URL pública tipo `https://abc123.ngrok-free.app` — é essa URL
que você vai usar no passo seguinte.

### 5. Configurar as duas tools no dashboard da Vapi

No assistant que você já criou na Vapi, adicione duas functions/tools:

**Tool 1 — `check_availability`**
- URL: `https://SEU-DOMINIO/vapi/tools/check-availability`
- Header customizado: `X-Vapi-Secret: <o mesmo valor que você colocou em AGENDA_VAPI_WEBHOOK_SECRET>`
- Parâmetros que a IA deve preencher: `dataInicio` (date), `dataFim` (date),
  `preferenciaPeriodo` (string, opcional: "manha" ou "tarde")

**Tool 2 — `book_meeting`**
- URL: `https://SEU-DOMINIO/vapi/tools/book-meeting`
- Mesmo header `X-Vapi-Secret`
- Parâmetros: `horarioEscolhido` (um dos valores ISO que a tool anterior
  devolveu), `nome`, `empresa`, `contato`

Atualize o system prompt do assistant pra mencionar essas duas tools pelo
nome, do jeito que já deixei no rascunho anterior.

### 6. Próximo passo depois que estiver rodando

- Testar uma ligação de ponta a ponta pelo navegador da Vapi
- Depois, pensar em onde fazer o deploy definitivo (Render/Railway/EC2) pra
  não depender do ngrok — me chama quando chegar nessa parte que eu ajudo

## Configuração disponível (`application.yml` / variáveis de ambiente)

| Variável | Default | O que é |
|---|---|---|
| `AGENDA_CALENDAR_ID` | — (obrigatório) | e-mail da agenda do Patrick |
| `AGENDA_CREDENTIALS_PATH` | `credentials/service-account.json` | caminho do JSON da service account |
| `AGENDA_TIMEZONE` | `America/Sao_Paulo` | fuso usado nos cálculos |
| `AGENDA_BUSINESS_HOUR_START` | `09:00` | início do expediente considerado |
| `AGENDA_BUSINESS_HOUR_END` | `18:00` | fim do expediente considerado |
| `AGENDA_SLOT_DURATION_MINUTES` | `30` | duração de cada reunião/slot |
| `AGENDA_MAX_SLOTS_RETURNED` | `5` | quantos horários oferecer por chamada |
| `AGENDA_MAX_DAYS_AHEAD` | `30` | limite de dias pra frente na busca |
| `AGENDA_VAPI_WEBHOOK_SECRET` | — (obrigatório) | segredo exigido no header `X-Vapi-Secret` |
