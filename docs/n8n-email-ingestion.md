# Ingestao de E-mails Financeiros via n8n

Esta integracao prepara o produto para ler e-mails financeiros com n8n sem mover regra de negocio para fora do backend.

Princípios da V1:

1. o n8n so orquestra captura, triagem barata e encaminhamento operacional
2. o backend continua como fonte de verdade para household, deduplicacao e decisao final
3. IA entra apenas em casos ambíguos ou e-mails livres
4. nada de RAG, memory longa ou agent livre nesta fase

## Backend Ja Resolvido

### 1. Mapeamento operacional por household

- `GET /api/v1/email-ingestion/sources`
- `POST /api/v1/email-ingestion/sources`

Uso:
- autenticacao normal da API com `Authorization: Bearer <access-token>`
- acesso restrito a `OWNER`

Objetivo:
- mapear `sourceAccount` para o household correto
- definir thresholds de `AUTO_IMPORT` e `REVIEW` por origem operacional

### 2. Ingestao operacional desacoplada do n8n

- `POST /api/v1/operations/email-ingestions`

Uso:
- `Authorization: Bearer <APP_OPERATIONAL_EMAIL_INGESTION_TOKEN>`
- endpoint operacional dedicado, sem acoplamento direto ao n8n

Objetivo:
- receber candidatos estruturados
- validar payload
- deduplicar
- resolver household
- decidir entre `AUTO_IMPORTED`, `REVIEW_REQUIRED` e `IGNORED`

## Payload Minimo do n8n

Campos suportados:

- `sourceAccount`
- `externalMessageId`
- `sender`
- `subject`
- `receivedAt`
- `merchantOrPayee`
- `suggestedCategoryName`
- `suggestedSubcategoryName`
- `totalAmount`
- `dueDate`
- `occurredOn`
- `currency`
- `items`
- `summary`
- `classification`
- `confidence`
- `rawReference`
- `desiredDecision`

Exemplos versionados:
- [docs/examples/email-ingestion-candidates.json](/home/gil/workspace/claude/despesas/docs/examples/email-ingestion-candidates.json)

## Decisao Final do Backend

O n8n pode sugerir:

- `AUTO_IMPORT`
- `REVIEW`
- `IGNORE`

O backend decide de forma deterministica:

- `AUTO_IMPORTED`
- `REVIEW_REQUIRED`
- `IGNORED`

Regras principais:

- irrelevante -> `IGNORED`
- duplicado por `externalMessageId` -> `IGNORED`
- duplicado por fingerprint -> `IGNORED`
- confianca abaixo do threshold de review -> `IGNORED`
- confianca media -> `REVIEW_REQUIRED`
- moeda nao suportada ou catalogo nao resolvido -> `REVIEW_REQUIRED`
- alta confianca + categoria/subcategoria validas -> `AUTO_IMPORTED`

## Workflows Versionados

Arquivos exportaveis da V1:

- [email-ingestion-candidate-processor-v1.json](/home/gil/workspace/claude/despesas/n8n/workflows/email-ingestion-v1/email-ingestion-candidate-processor-v1.json)
- [email-ingestion-replay-harness-v1.json](/home/gil/workspace/claude/despesas/n8n/workflows/email-ingestion-v1/email-ingestion-replay-harness-v1.json)
- [email-ingestion-gmail-trigger-v1.json](/home/gil/workspace/claude/despesas/n8n/workflows/email-ingestion-v1/email-ingestion-gmail-trigger-v1.json)
- [email-ingestion-outlook-trigger-v1.json](/home/gil/workspace/claude/despesas/n8n/workflows/email-ingestion-v1/email-ingestion-outlook-trigger-v1.json)
- [email-ingestion-mailbox-bootstrap-v1.json](/home/gil/workspace/claude/despesas/n8n/workflows/email-ingestion-v1/email-ingestion-mailbox-bootstrap-v1.json)
- [email-ingestion-outlook-live-sender-v1.json](/home/gil/workspace/claude/despesas/n8n/workflows/email-ingestion-v1/email-ingestion-outlook-live-sender-v1.json)

Geracao local:

- `python3 scripts/n8n/generate_email_ingestion_workflows.py`

Papel de cada workflow:

1. `Candidate Processor V1`
   - subworkflow comum
   - normaliza o envelope
   - faz triagem barata
   - usa IA apenas quando a heuristica nao basta
   - posta no backend operacional

2. `Replay Harness V1`
   - ponto de teste manual e replay
   - suporta cenarios sinteticos sem depender de caixa real

3. `Gmail Trigger V1`
   - usa `Gmail Trigger`
   - aplica filtro inicial da caixa
   - envia envelope ao processor comum

4. `Outlook Trigger V1`
   - usa `Microsoft Outlook Trigger`
   - reutilizavel para Outlook, Hotmail e Live
   - envia envelope ao processor comum

5. `Mailbox Bootstrap V1`
   - cria labels no Gmail e pastas no Outlook/Live
   - prepara a marcacao operacional real da caixa

6. `Outlook Live Sender V1`
   - utilitario de smoke local
   - envia lote controlado para Gmail e Outlook/Live sem depender de caixa de terceiro

## Onde a IA Entra

Triagem deterministica primeiro:

- remetentes/dominios conhecidos
- palavras-chave de cobranca, fatura, mensalidade, conta, recibo
- regex simples de valor monetario
- descarte barato de newsletter, promocao e ruido social

IA entra somente quando agrega valor:

- e-mail livre/manual descrevendo compra
- candidato financeiro ambiguo
- caso em que merchant/categoria/resumo/confianca nao ficam bons so com heuristica

Na V1, a IA e usada pelo `Information Extractor` do n8n com prompt curto e schema rigidamente estruturado.

## Estrategia de Custo

Protecoes aplicadas:

- triagem barata antes da IA
- sem RAG
- sem memory
- sem historico longo
- sem agent livre
- sem anexos longos na V1
- payload curto e estruturado
- respostas objetivas para o backend
- categorias conhecidas tentam ir direto para `AUTO_IMPORT` ou `REVIEW`

O objetivo e gastar token apenas onde a heuristica barata deixa de ser suficiente.

## Credenciais e Variaveis no n8n

Nao versione segredos nem bindings locais de credencial.

O que precisa ser configurado no n8n antes de ativar os triggers reais:

### Credenciais

- Gmail: credencial OAuth2 do `Gmail Trigger`
- Outlook/Hotmail/Live: credencial do `Microsoft Outlook Trigger`
- IA do `Information Extractor`: vincular manualmente um modelo suportado no ambiente local

Observacao:
- os JSONs versionados nao carregam segredos nem IDs locais de credenciais
- depois de importar, abra os nodes que precisam de credencial e faça o binding local

### Variaveis sugeridas no n8n

- `DESPESAS_BACKEND_BASE_URL`
- `DESPESAS_OPERATIONAL_EMAIL_INGESTION_TOKEN`
- `DESPESAS_GMAIL_SOURCE_ACCOUNT`
- `DESPESAS_OUTLOOK_SOURCE_ACCOUNT`

## Marcacoes Reais de Caixa

Depois que o backend devolve `mailboxAction`, o workflow reflete o desfecho no proprio e-mail:

### Gmail

- `imported` -> label `Despesas/Imported`
- `review` -> label `Despesas/Review`
- `ignored` -> label `Despesas/Ignored`
- `duplicate` -> label `Despesas/Duplicate`
- quando a marcacao roda, o workflow tambem remove `UNREAD`

### Outlook/Hotmail/Live

- `imported` -> pasta `Despesas Imported`
- `review` -> pasta `Despesas Review`
- `ignored` -> pasta `Despesas Ignored`
- `duplicate` -> pasta `Despesas Duplicate`

As labels e pastas sao criadas pelo workflow `Mailbox Bootstrap V1`.

## Como Testar Localmente

### 1. Backend

Configurar no backend:

- `APP_SECURITY_TOKEN_SECRET`
- `APP_OPERATIONAL_EMAIL_INGESTION_TOKEN`

Criar o household e mapear `sourceAccount`:

- `financeiro@gmail.com`
- `financeiro@outlook.com`

### 2. Replay Harness

Webhook:

- `POST /webhook/email-ingestion-replay-v1`

Payload minimo de teste:

```json
{
  "scenario": "recurringBill",
  "sourceAccount": "financeiro@gmail.com",
  "backendBaseUrl": "http://host.docker.internal:8091",
  "operationalToken": "<token-operacional>"
}
```

Cenarios suportados na V1:

- `recurringBill`
- `manualPurchase`
- `irrelevantNewsletter`

### 3. Resultado operacional esperado

- `recurringBill` -> `AUTO_IMPORTED`
- `manualPurchase` -> `REVIEW_REQUIRED`
- `irrelevantNewsletter` -> `IGNORED`

O workflow tambem reflete `mailboxAction` operacional:

- `imported`
- `review`
- `ignored`
- `duplicate`

### 4. Bootstrap operacional da caixa

Antes dos triggers reais, execute uma vez:

- `POST /webhook/email-ingestion-mailbox-bootstrap-v1`

Isso garante:

- labels `Despesas/*` no Gmail
- pastas `Despesas *` no Outlook/Live

### 5. Smoke real local

Webhook utilitario:

- `POST /webhook/email-ingestion-live-send-v1`

Payload util:

```json
{
  "batchTag": "FINAL1",
  "includeDuplicate": true,
  "gmailTo": "financeiro@gmail.com",
  "outlookTo": "financeiro@outlook.com"
}
```

Alternativamente, configure no n8n:

- `DESPESAS_SMOKE_GMAIL_TO`
- `DESPESAS_SMOKE_OUTLOOK_TO`

Resultados esperados:

- Gmail manual tipo Cobasi -> `REVIEW_REQUIRED` + label `Despesas/Review`
- Gmail irrelevante -> `IGNORED` + label `Despesas/Ignored`
- Outlook conta recorrente -> `AUTO_IMPORTED` + move para `Despesas Imported`
- Outlook irrelevante -> `IGNORED` + move para `Despesas Ignored`
- Outlook duplicado -> `duplicate` + move para `Despesas Duplicate`

## Review humano na web

As pendencias `REVIEW_REQUIRED` nao ficam perdidas no n8n. O ciclo operacional validado nesta fase ficou:

- trigger Gmail/Outlook ou replay
- triagem barata no n8n
- `POST /api/v1/operations/email-ingestions`
- persistencia e decisao final no backend
- fila humana em `GET /revisoes`
- aprovacao ou rejeicao manual via web

Regras importantes:

- o backend continua como fonte de verdade
- aprovar reaproveita o caminho deterministico de importacao
- rejeitar nao cria despesa
- candidatos incompletos continuam honestamente em review, sem numero inventado

## Validacao Local Feita Nesta Rodada

Validado no n8n local real:

- workflows importados no projeto pessoal existente
- `Candidate Processor V1`, `Gmail Trigger V1`, `Outlook Trigger V1`, `Mailbox Bootstrap V1` e `Outlook Live Sender V1` ativos
- bootstrap real criou as pastas `Despesas *` no Outlook/Live e confirmou labels `Despesas/*` no Gmail
- Gmail real adicionando label correto e removendo `UNREAD`
- Outlook/Live real movendo mensagens para as pastas operacionais corretas
- parser manual nao trata mais a linha `Total` como item
- `AUTO_IMPORTED` para cobranca recorrente de internet
- `REVIEW_REQUIRED` para compra manual tipo Cobasi
- `IGNORED` para newsletter irrelevante
- `duplicate` funcionando em trigger real do Outlook/Live

## Estabilidade Operacional do n8n Local

No ambiente local desta V1, a sequencia minima estavel apos importar workflows e:

1. importar os JSONs no projeto correto do n8n
2. garantir `active = 1` e `activeVersionId = versionId` para os workflows ativos
3. reiniciar o container `n8n-local`
4. confirmar em log que os workflows ativos foram carregados
5. confirmar os webhooks registrados antes de rodar bootstrap ou smoke real

Sem esse alinhamento, o n8n local pode ficar com draft/importado valido na base, mas sem registrar os webhooks no runtime.

## Lacunas Planejadas

- Gmail e Outlook reais dependem apenas da configuracao das credenciais no n8n
- bindings de credencial de IA devem ser feitos localmente apos importar os JSONs
- candidatos com total ausente ou moeda nao suportada continuam exigindo rejeicao manual na V1
