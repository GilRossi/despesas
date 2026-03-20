# Controle de Despesas

Aplicação Spring Boot para gestão de despesas por household, com interface web em Thymeleaf e API REST em `/api/v1`.

O projeto evoluiu de um CRUD simples de despesas para uma base com:
- identidade por household
- catálogo de categorias e subcategorias por household
- despesas com snapshot histórico de categoria/subcategoria
- pagamentos vinculados às despesas
- autenticação web por sessão e autenticação de API por Bearer token
- assistente financeiro com camada determinística + camada conversacional
- migrations Flyway, incluindo backfill do legado `tb_despesas`

## Objetivo do sistema

Oferecer uma base consistente para controle de despesas domésticas, com separação por household, catálogo configurável, rastreabilidade histórica e superfície pronta para evolução de web e mobile.

## Funcionalidades implementadas

- cadastro de usuário com criação de household
- bootstrap automático de catálogo inicial para household novo
- login web com sessão Spring Security
- login/refresh/me na API com Bearer token
- gestão de membros do household via API
- gestão de categorias e subcategorias por household via API
- criação, listagem, edição e exclusão lógica de despesas
- registro de pagamentos e cálculo de status da despesa
- dashboard resumido por status
- resumo financeiro, insights, recomendações e consulta conversacional por linguagem natural
- envelope de erro unificado em `/api/v1`
- backfill legado de `tb_despesas` para `expenses`
- fluxo web `/despesas` adaptado ao domínio atual

## Stack

- Java 21
- Spring Boot 3.5.5
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Thymeleaf
- Flyway
- LangChain4j
- PostgreSQL 15
- DeepSeek como provedor configurável de LLM
- H2 para parte da suíte de testes
- JUnit 5, Spring Test, Spring Security Test, Testcontainers
- Bootstrap 5
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
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SHOW_SQL`
- `FINANCIAL_ASSISTANT_AI_ENABLED`
- `DEEPSEEK_API_KEY`
- `DEEPSEEK_BASE_URL`
- `DEEPSEEK_MODEL`
- `DEEPSEEK_TEMPERATURE`
- `DEEPSEEK_MAX_COMPLETION_TOKENS`
- `DEEPSEEK_TIMEOUT_SECONDS`
- `DEEPSEEK_MAX_RETRIES`

Configuração padrão em [`application.properties`](/home/gil/workspace/claude/despesas/src/main/resources/application.properties):

- `DB_URL` default: `jdbc:postgresql://localhost:5432/despesasdb`
- `DB_USERNAME` default: `postgres`
- `DB_PASSWORD` default: `postgres`
- `SHOW_SQL` default: `false`

Importante:

- `APP_SECURITY_TOKEN_SECRET` é obrigatória.
- A aplicação falha no startup se `app.security.token-secret` não estiver configurado.
- A API Bearer-only depende desse secret para emitir e validar access/refresh tokens.
- O assistente conversacional só usa provedor externo se `FINANCIAL_ASSISTANT_AI_ENABLED=true` e `DEEPSEEK_API_KEY` estiver configurada.
- Sem IA configurada, o endpoint conversacional continua funcional via fallback determinístico.

Exemplo de execução local:

```bash
APP_SECURITY_TOKEN_SECRET=dev-secret-local ./mvnw spring-boot:run
```

Se quiser apontar para outro banco:

```bash
APP_SECURITY_TOKEN_SECRET=dev-secret-local \
DB_URL=jdbc:postgresql://localhost:5432/despesasdb \
DB_USERNAME=postgres \
DB_PASSWORD=postgres \
./mvnw spring-boot:run
```

### 3. Acesse a aplicação

- Web: `http://localhost:8080/login`
- Lista web de despesas: `http://localhost:8080/despesas`
- Healthcheck: `http://localhost:8080/actuator/health`

## Como rodar os testes

### Suíte principal

```bash
./mvnw test
```

Essa suíte cobre testes unitários, slices MVC/API e integrações com H2.

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
├── controller/      # controllers MVC/web
├── expense/         # domínio de despesas, filtros e dashboard
├── identity/        # usuários, households e membros
├── payment/         # pagamentos
├── security/        # Spring Security, token service e providers
├── service/         # adaptador do fluxo web legado para o domínio atual
└── model/           # DTO/modelo usado pela casca MVC
```

Divisão de responsabilidades:

- `controller/`: superfície web Thymeleaf (`/login`, `/despesas`, redirecionamentos)
- `api/v1/**`: contratos REST para auth, household, catálogo, despesas, pagamentos e dashboard
- `expense/`, `payment/`, `identity/`, `catalog/`: regras de negócio e persistência principal
- `financialassistant/`: analytics determinístico, insights, recomendações, intents e orquestração da consulta
- `financialassistant/ai/`: adapter do provedor, gateway desacoplado, tools compactas e captura de usage
- `security/`: autenticação, autorização e isolamento por household
- `service/DespesaService`: adaptador da UI web para o domínio novo

## Autenticação atual

O sistema trabalha com dois modos distintos:

### Web

- login por formulário Spring Security
- sessão baseada em cookie
- rotas web protegidas por autenticação

### API `/api/**`

- contrato Bearer-only
- endpoints públicos de auth:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
- endpoint autenticado:
  - `GET /api/v1/auth/me`
- `httpBasic` não é mais aceito na API

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
- o catálogo alimenta tanto a API quanto o formulário web

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

- login web
- proteção de rotas web
- auth Bearer na API
- onboarding de household novo
- fluxo principal de despesas na web
- regras críticas de API para household, catálogo, despesas e pagamentos
- backfill legado com validação em PostgreSQL/Flyway
- bateria destrutiva final de navegação, formulários, CRUD e autorização

## Limitações atuais e próximos passos recomendados

O produto está estável no escopo já validado, mas estes próximos passos fazem sentido:

### Hardening futuro de auth

- revogação persistida de refresh token
- rotação persistida de refresh token
- rate limiting para auth pública
- trilha de auditoria para login, refresh e falhas de autenticação

### E2E adicionais recomendados

- formalizar a suíte Playwright no repositório
- ampliar cenários browser para múltiplas personas e sessões concorrentes
- adicionar regressão automática para dashboard e catálogo no fluxo web

### UX e produto

- superfícies web para gestão explícita de categorias/subcategorias
- refinamento visual do dashboard
- feedbacks mais ricos para estados vazios e erros de negócio

### Observabilidade

- logs estruturados com correlation/trace id
- métricas operacionais adicionais além de `health` e `info`
- auditoria de eventos críticos de domínio

### Evolução funcional

- relatórios analíticos mais ricos
- indicadores por período/contexto/categoria
- fluxo mais completo para gestão de pagamentos via web

## Resumo prático para outro desenvolvedor

Se você acabou de clonar o repositório:

1. suba o PostgreSQL com `docker-compose up -d`
2. defina `APP_SECURITY_TOKEN_SECRET`
3. rode `./mvnw spring-boot:run`
4. acesse `http://localhost:8080/login`
5. para validar a suíte padrão, rode `./mvnw test`
6. para validar migrations e persistência real, rode `bash scripts/run-postgres-it.sh`

## Autor

**Gil Rossi Aguiar**

- Email: [gilrossi.aguiar@live.com](mailto:gilrossi.aguiar@live.com)
- LinkedIn: [gil-rossi-5814659b](https://www.linkedin.com/in/gil-rossi-5814659b/)
- GitHub: [GilRossi](https://github.com/GilRossi)
