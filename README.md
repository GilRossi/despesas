# Controle de Despesas

Aplicação Spring Boot para gestão de despesas por household, com front-door oficial em Flutter Web e API REST em `/api/v1`.

O projeto evoluiu de um CRUD simples de despesas para uma base com:
- identidade por household
- catálogo de categorias e subcategorias por household
- despesas com snapshot histórico de categoria/subcategoria
- pagamentos vinculados às despesas
- autenticação da API por Bearer token consumida pelo Flutter Web e Flutter Mobile
- assistente financeiro com camada determinística + camada conversacional
- migrations Flyway, incluindo backfill do legado `tb_despesas`

## Objetivo do sistema

Oferecer uma base consistente para controle de despesas domésticas, com separação por household, catálogo configurável, rastreabilidade histórica e superfície pronta para evolução de web e mobile.

## Funcionalidades implementadas

- bootstrap controlado do primeiro `PLATFORM_ADMIN`
- provisionamento autenticado de household + owner por `PLATFORM_ADMIN`
- criação autenticada de member pelo `HOUSEHOLD_OWNER` do próprio household
- bootstrap automático de catálogo inicial para household novo
- login/refresh/me na API com Bearer token
- gestão de membros do household via API
- gestão de categorias e subcategorias por household via API
- criação, listagem, edição e exclusão lógica de despesas
- registro de pagamentos e cálculo de status da despesa
- dashboard resumido por status
- resumo financeiro, insights, recomendações e consulta conversacional por linguagem natural
- ingestão operacional de e-mails financeiros para n8n, com triagem conservadora e deduplicação no backend
- envelope de erro unificado em `/api/v1`
- backfill legado de `tb_despesas` para `expenses`
- front-door configurável para servir o build oficial do Flutter Web no mesmo processo do backend

## Stack

- Java 21
- Spring Boot 3.5.5
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Flyway
- LangChain4j
- PostgreSQL 15
- DeepSeek como provedor configurável de LLM
- H2 para parte da suíte de testes
- JUnit 5, Spring Test, Spring Security Test, Testcontainers
- Maven Wrapper

## Pré-requisitos

- JDK 21
- Docker e Docker Compose
- acesso a um PostgreSQL local ou container compatível com a configuração do projeto

## Como rodar localmente

### 1. Suba o PostgreSQL local

```bash
docker-compose up -d
```

Por padrão, o container sobe como `despesas-postgres` em `localhost:5432`.

### 2. Configure as variáveis importantes

O projeto usa estas variáveis no runtime:

- `APP_SECURITY_TOKEN_SECRET`
- `APP_BOOTSTRAP_PLATFORM_ADMIN_NAME`
- `APP_BOOTSTRAP_PLATFORM_ADMIN_EMAIL`
- `APP_BOOTSTRAP_PLATFORM_ADMIN_PASSWORD`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `APP_SHOW_SQL`
- `APP_OPERATIONAL_EMAIL_INGESTION_TOKEN`
- `APP_FRONTEND_WEB_DIST`
- `APP_ENV`
- `API_BASE_URL`
- `FINANCIAL_ASSISTANT_AI_ENABLED`
- `DEEPSEEK_API_KEY`
- `DEEPSEEK_BASE_URL`
- `DEEPSEEK_MODEL`
- `DEEPSEEK_TEMPERATURE`
- `DEEPSEEK_MAX_COMPLETION_TOKENS`
- `DEEPSEEK_TIMEOUT_SECONDS`
- `DEEPSEEK_MAX_RETRIES`

Estrutura oficial externa:

- `~/envs/despesas/local/backend.env`
- `~/envs/despesas/local/postgres.env`
- `~/envs/despesas/local/n8n.env`
- `~/envs/despesas/local/google.env`
- `~/envs/despesas/local/microsoft.env`
- `~/envs/despesas/prod/backend.env`
- `~/envs/despesas/prod/postgres.env`
- `~/envs/despesas/prod/n8n.env`
- `~/envs/despesas/prod/google.env`
- `~/envs/despesas/prod/microsoft.env`

Leitura de runtime no backend:

- [`application.properties`](/home/gil/workspace/claude/despesas/src/main/resources/application.properties)
- [`load-governed-env.sh`](/home/gil/workspace/claude/despesas/scripts/runtime/load-governed-env.sh)
- [`run-local-postgres.sh`](/home/gil/workspace/claude/despesas/scripts/runtime/run-local-postgres.sh)
- [`run-local-backend.sh`](/home/gil/workspace/claude/despesas/scripts/runtime/run-local-backend.sh)
- [`production-runtime.md`](/home/gil/workspace/claude/despesas/docs/production-runtime.md)
- [`hostinger-preflight.md`](/home/gil/workspace/claude/despesas/docs/hostinger-preflight.md)
- [`prepare-production-host.sh`](/home/gil/workspace/claude/despesas/scripts/runtime/prepare-production-host.sh)

Configuração relevante em [`application.properties`](/home/gil/workspace/claude/despesas/src/main/resources/application.properties):

- `DB_URL` default: `jdbc:postgresql://localhost:5432/despesasdb`
- `DB_USERNAME` default: `postgres`
- `DB_PASSWORD` default: `postgres`
- `APP_SHOW_SQL` default: `false`

Importante:

- `APP_SECURITY_TOKEN_SECRET` é obrigatória.
- A aplicação falha no startup se `app.security.token-secret` não estiver configurado.
- A API Bearer-only depende desse secret para emitir e validar access/refresh tokens.
- `APP_FRONTEND_WEB_DIST` define o resource location do build do Flutter Web oficial.
- O assistente conversacional só usa provedor externo se `FINANCIAL_ASSISTANT_AI_ENABLED=true` e `DEEPSEEK_API_KEY` estiver configurada.
- Sem IA configurada, o endpoint conversacional continua funcional via fallback determinístico.

PostgreSQL local governado:

```bash
scripts/runtime/run-local-postgres.sh
```

Backend local governado:

```bash
scripts/runtime/run-local-backend.sh
```

Validacao do runtime de producao, sem deploy:

```bash
scripts/runtime/validate-production-compose.sh
```

### 3. Acesse a aplicação

- Front-door Flutter Web: `http://localhost:8080/`
- API: `http://localhost:8080/api/v1`
- Healthcheck: `http://localhost:8080/actuator/health`

### Ordem recomendada do runtime local governado

1. `scripts/runtime/run-local-postgres.sh`
2. `scripts/runtime/run-local-backend.sh`
3. `cd /home/gil/StudioProjects/despesas_frontend && scripts/build_local_web.sh`
4. `cd /home/gil/n8n-local && scripts/run_local.sh`
5. opcional para Android físico: `cd /home/gil/StudioProjects/despesas_frontend && scripts/run_local_android.sh`

Observacoes:

- o backend local usa os envs oficiais em `~/envs/despesas/local`
- o n8n local usa envs externas e storage criptografado proprio
- o repo privado do n8n documenta o bootstrap/restauracao de credenciais em `/home/gil/n8n-local/projects/despesas/docs/credential-bootstrap.md`

## Como rodar os testes

### Suíte principal

```bash
./mvnw test
```

Essa suíte cobre testes unitários, slices web/API e integrações com H2.

### Suíte PostgreSQL/Flyway

```bash
bash scripts/run-postgres-it.sh
```

Esse script:
- sobe o PostgreSQL local via `docker-compose`
- cria um banco efêmero
- executa as integrações que validam migrations Flyway, backfill legado e persistência real

Ele depende de Docker funcional na máquina local.

## Resumo da arquitetura atual

Estrutura principal:

```text
src/main/java/com/gilrossi/despesas/
├── api/v1/          # API REST versionada
├── catalog/         # catálogo por household
├── expense/         # domínio de despesas, filtros e dashboard
├── identity/        # usuários, households e membros
├── payment/         # pagamentos
├── security/        # Spring Security, token service e providers
├── service/         # adaptador do fluxo web legado para o domínio atual
└── model/           # DTO/modelo usado pela casca MVC
```

Divisão de responsabilidades:

- static web: build oficial do Flutter Web servido a partir de `APP_FRONTEND_WEB_DIST`
- `api/v1/**`: contratos REST para auth, household, catálogo, despesas, pagamentos e dashboard
- `expense/`, `payment/`, `identity/`, `catalog/`: regras de negócio e persistência principal
- `financialassistant/`: analytics determinístico, insights, recomendações, intents e orquestração da consulta
- `financialassistant/ai/`: adapter do provedor, gateway desacoplado, tools compactas e captura de usage
- `security/`: autenticação, autorização e isolamento por household

Blocos oficiais do sistema final:

- backend Spring Boot: domínio, API, auth Bearer, household boundary e integração operacional
- Flutter Web: front-door oficial servido pelo backend em `/`
- Flutter Mobile: companion oficial consumindo a mesma API
- DeepSeek: provedor opcional da camada conversacional do assistente
- n8n: automação operacional de ingestão de e-mails, sem virar fonte de verdade de regra de negócio

## Autenticação atual

O sistema trabalha com um modo oficial de autenticação:

### API `/api/**`

- contrato Bearer-only
- endpoints públicos de auth:
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
- endpoint autenticado:
  - `GET /api/v1/auth/me`
- não existe cadastro público
- o primeiro `PLATFORM_ADMIN` nasce por bootstrap controlado de ambiente
- `POST /api/v1/admin/households` é restrito a `PLATFORM_ADMIN`
- `httpBasic` não é mais aceito na API
- o Flutter Web e o Flutter Mobile consomem o mesmo contrato

Payload de auth mobile/API:

- `tokenType`
- `accessToken`
- `accessTokenExpiresAt`
- `refreshToken`
- `refreshTokenExpiresAt`
- `user`

Limitação atual relevante:

- ainda não existe revogação/rotação persistida de tokens

## Assistente financeiro inteligente

O assistente financeiro expõe analytics determinístico e uma camada conversacional opcional:

- backend determinístico continua sendo a fonte de verdade para totais, KPIs, comparativos e alertas
- a IA não calcula números “na cabeça”; ela interpreta a pergunta, usa tools internas e responde em linguagem natural
- o domínio não depende diretamente do DeepSeek; a conversa passa por um gateway desacoplado em `financialassistant/ai`

Endpoints:

- `GET /api/v1/financial-assistant/summary`
- `GET /api/v1/financial-assistant/kpis`
- `GET /api/v1/financial-assistant/insights`
- `GET /api/v1/financial-assistant/recommendations`
- `POST /api/v1/financial-assistant/query`

### `mode=AI` vs `mode=FALLBACK`

- `FALLBACK`: perguntas diretas e baratas, respondidas só com a camada determinística
  - exemplo: total por categoria, maior categoria, top despesas, resumo simples
- `AI`: perguntas interpretativas, quando a linguagem natural agrega valor
  - exemplo: recomendações de economia, aumentos relevantes, recorrência, comparação explicada

O payload de `/query` mantém:

- `question`
- `mode`
- `intent`
- `answer`
- blocos determinísticos de apoio (`summary`, `monthComparison`, `highestSpendingCategory`, `topExpenses`, `increaseAlerts`, `recurringExpenses`, `recommendations`)
- `aiUsage` apenas quando o provedor foi realmente usado

### Degradação segura

Se a IA estiver desabilitada, sem chave, com chave inválida ou se o provedor falhar:

- o endpoint continua respondendo `200`
- `mode` volta para `FALLBACK`
- o cliente não recebe stacktrace nem erro cru do provedor
- logs internos registram apenas categoria sanitizada de falha (`AUTH_ERROR`, `TIMEOUT`, `NETWORK_ERROR`, `PROVIDER_ERROR`, `UNEXPECTED_ERROR`)

### Nota de custo/tokens

O desenho atual prioriza baixo custo:

- intents diretas não chamam IA
- prompts e tools são compactos
- `deepseek-chat` é usado como modelo padrão
- `maxCompletionTokens`, `temperature`, `timeout` e `retries` têm defaults conservadores
- `aiUsage` captura tokens de entrada/saída, total, cache hit/miss e número de tools executadas

Para web ou Flutter, a recomendação é consumir `/summary`, `/kpis`, `/insights` e `/recommendations` como fonte de verdade da interface e usar `/query` como camada de interpretação conversacional.

## Relatórios e assistente no Flutter Web

O Flutter Web oficial consome a mesma base determinística do backend e oferece:

- filtro simples por mês
- comparação opcional com o mês anterior
- KPIs de total, pago, pendente, maior categoria e variação mensal
- breakdown por categoria com peso relativo e delta
- insights acionáveis com maiores despesas, recorrências, aumentos e recomendações
- entrada dedicada para o assistente financeiro oficial do household

Essa frente não cria uma segunda fonte de verdade: os números vêm da camada determinística existente, e o assistente continua respeitando `mode=AI` vs `mode=FALLBACK`.

## Ingestão por e-mail e review operations

O projeto também já fecha o ciclo operacional web + n8n para ingestão de e-mails financeiros:

- o n8n captura e-mails, faz triagem barata e só envia candidatos estruturados ao backend
- o backend decide `AUTO_IMPORTED`, `REVIEW_REQUIRED`, `IGNORED` ou duplicado
- candidatos conservadores ou incompletos permanecem honestamente em review
- a fila humana fica no Flutter Web oficial, com approve/reject reaproveitando o mesmo backend determinístico

O fluxo validado é:

- `n8n -> POST /api/v1/operations/email-ingestions -> REVIEW_REQUIRED -> review operations no Flutter Web -> aprovar/rejeitar`

Detalhes operacionais e exports dos workflows estão em [`docs/n8n-email-ingestion.md`](/home/gil/workspace/claude/despesas/docs/n8n-email-ingestion.md).

Source of truth operacional:

- workflows versionados em [`n8n/workflows/email-ingestion-v1`](/home/gil/workspace/claude/despesas/n8n/workflows/email-ingestion-v1)
- `n8n-local/` apenas como workspace de runtime local, nunca como fonte canônica de definição

## Modelo de household

O sistema é multi-household por desenho de domínio:

- um usuário se registra com nome, email, senha e nome do household
- o registro cria:
  - `users`
  - `households`
  - `household_members`
- o usuário inicial entra como `OWNER`
- novos membros podem ser adicionados via API
- isolamento por household é aplicado nos principais fluxos de catálogo, despesas, pagamentos e dashboard

## Catálogo, despesas e pagamentos

### Catálogo

- categorias e subcategorias são próprias de cada household
- household novo recebe catálogo inicial automático
- o catálogo alimenta a API consumida pelo Flutter Web e Flutter Mobile

### Despesas

O modelo atual usa estratégia híbrida:

- referência viva:
  - `category_id`
  - `subcategory_id`
- snapshot histórico:
  - `category_name_snapshot`
  - `subcategory_name_snapshot`

Isso permite:

- integridade relacional para filtros e validações
- preservação do nome histórico da categoria/subcategoria na despesa

Cada despesa também possui:

- `household_id`
- `description`
- `amount`
- `due_date`
- `context`
- `notes`
- timestamps de criação/atualização/exclusão lógica

### Pagamentos

- pagamentos são vinculados a despesas
- o total pago alimenta o status calculado da despesa
- exclusão de despesa respeita regras de negócio associadas ao histórico de pagamentos

## Migrações e backfill legado

As migrations atuais estão em [`src/main/resources/db/migration`](/home/gil/workspace/claude/despesas/src/main/resources/db/migration):

- `V1__create_tb_despesas.sql`
- `V2__create_households_users_and_catalog.sql`
- `V3__create_expenses_and_payments.sql`
- `V4__identity_security_support.sql`
- `V5__enforce_catalog_integrity_for_expenses.sql`
- `V6__enforce_single_active_household_membership.sql`
- `V7__enforce_subcategory_category_household_integrity.sql`
- `V8__formalize_expense_snapshot_columns.sql`
- `V9__backfill_legacy_tb_despesas_into_expenses.sql`

Observações importantes:

- `tb_despesas` é o legado original
- `expenses` é a verdade operacional atual
- o backfill de `V9` migra dados legados para `expenses`
- a tabela legada não deve ser dropada sem nova rodada explícita de reconciliação e validação

## O que foi validado recentemente

No estado atual do projeto, o QA final validou:

- front-door do Flutter Web
- auth Bearer na API
- fluxo principal de despesas no Flutter
- sanity do Flutter Mobile contra a mesma API
- regras críticas de API para household, catálogo, despesas e pagamentos
- backfill legado com validação em PostgreSQL/Flyway
- review operations por API
- assistente financeiro por API
- n8n ponta a ponta até review operations no Flutter Web

## Ordem recomendada de subida

1. subir PostgreSQL
2. buildar o Flutter Web oficial em `/home/gil/StudioProjects/despesas_frontend`
3. subir o backend com `APP_FRONTEND_WEB_DIST` apontando para `build/web`
4. opcionalmente habilitar DeepSeek com `FINANCIAL_ASSISTANT_AI_ENABLED=true` e `DEEPSEEK_API_KEY`
5. subir o `n8n-local` com as credenciais e variáveis `DESPESAS_*`
6. executar `Mailbox Bootstrap V1` antes dos triggers reais

## Legado removido da frente oficial

- front-door MVC/Thymeleaf
- login web por sessão
- controllers e templates legados de despesas, relatórios e revisões

O backend permaneceu como API e domínio do produto.

## Limitações atuais e próximos passos recomendados

O produto está estável no escopo já validado, mas estes próximos passos fazem sentido:

### Hardening futuro de auth

- revogação persistida de refresh token
- rotação persistida de refresh token
- rate limiting para auth pública
- trilha de auditoria para login, refresh e falhas de autenticação

### E2E adicionais recomendados

- formalizar a suíte browser do Flutter Web no repositório
- ampliar cenários para múltiplas personas e sessões concorrentes
- adicionar regressão automática para catálogo e smoke do front-door

### UX e produto

- superfícies Flutter para gestão explícita de categorias/subcategorias
- refinamento visual do dashboard
- feedbacks mais ricos para estados vazios e erros de negócio

### Observabilidade

- logs estruturados com correlation/trace id
- métricas operacionais adicionais além de `health` e `info`
- auditoria de eventos críticos de domínio

### Evolução funcional

- expandir os relatórios analíticos para novos recortes e séries históricas
- indicadores por período/contexto/categoria
- fluxo mais completo para gestão de pagamentos via web

## Resumo prático para outro desenvolvedor

Se você acabou de clonar o repositório:

1. suba o PostgreSQL com `docker-compose up -d`
2. defina `APP_SECURITY_TOKEN_SECRET`
3. rode `./mvnw spring-boot:run`
4. acesse `http://localhost:8080/`
5. para validar a suíte padrão, rode `./mvnw test`
6. para validar migrations e persistência real, rode `bash scripts/run-postgres-it.sh`

## Autor

**Gil Rossi Aguiar**

- Email: [gilrossi.aguiar@live.com](mailto:gilrossi.aguiar@live.com)
- LinkedIn: [gil-rossi-5814659b](https://www.linkedin.com/in/gil-rossi-5814659b/)
- GitHub: [GilRossi](https://github.com/GilRossi)
