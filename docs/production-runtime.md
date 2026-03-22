# Runtime de producao

Este documento define o runtime oficial de producao sem ainda executar deploy real.

## Objetivo

Concentrar no repositório backend a topologia oficial de deploy:

- proxy reverso
- backend Spring Boot
- PostgreSQL
- n8n
- artefato do Flutter Web servido pelo backend

## Topologia final

- `proxy`: Caddy termina HTTPS e roteia por host
- `backend`: API + front-door Flutter Web no mesmo processo
- `postgres`: persistencia principal da aplicacao
- `n8n`: automacao operacional e webhooks em subdominio proprio

## Servicos versionados

- [compose.base.yml](/home/gil/workspace/claude/despesas/deploy/compose.base.yml)
- [compose.prod.yml](/home/gil/workspace/claude/despesas/deploy/compose.prod.yml)
- [Caddyfile](/home/gil/workspace/claude/despesas/deploy/proxy/Caddyfile)

## Diretorios esperados na VPS

- `~/envs/despesas/prod/backend.env`
- `~/envs/despesas/prod/postgres.env`
- `~/envs/despesas/prod/n8n.env`
- `~/envs/despesas/prod/google.env`
- `~/envs/despesas/prod/microsoft.env`
- `/srv/despesas/frontend-web/current`
- `/srv/despesas/n8n/files`

## Flutter Web no runtime oficial

O build oficial do Flutter Web continua vindo do repositório Flutter, mas em producao ele entra no backend por mount somente leitura:

- host: `/srv/despesas/frontend-web/current`
- container backend: `/app/frontend-web`
- config do Spring: `APP_FRONTEND_WEB_DIST=file:/app/frontend-web/`

O proxy nao serve o build diretamente. Quem entrega `/` continua sendo o backend.

## Variaveis obrigatorias

### Backend e banco

- `BACKEND_IMAGE`
- `DB_NAME`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `APP_SECURITY_TOKEN_SECRET`
- `APP_OPERATIONAL_EMAIL_INGESTION_TOKEN`
- `APP_FRONTEND_WEB_DIST`

### Publicas de runtime/proxy

- `APP_PUBLIC_HOST`
- `N8N_PUBLIC_HOST`
- `ACME_EMAIL`

### n8n

- `N8N_ENCRYPTION_KEY`
- `N8N_HOST`
- `N8N_PROTOCOL`
- `N8N_PORT`
- `WEBHOOK_URL`
- `N8N_PROXY_HOPS`
- `DESPESAS_BACKEND_BASE_URL`
- `DESPESAS_OPERATIONAL_EMAIL_INGESTION_TOKEN`
- `DESPESAS_GMAIL_SOURCE_ACCOUNT`
- `DESPESAS_OUTLOOK_SOURCE_ACCOUNT`
- `DESPESAS_SMOKE_GMAIL_TO`
- `DESPESAS_SMOKE_OUTLOOK_TO`
- `GOOGLE_GEMINI_MODEL`

## Variaveis opcionais

- `POSTGRES_IMAGE`
- `N8N_IMAGE`
- `PROXY_IMAGE`
- `APP_SHOW_SQL`
- `FINANCIAL_ASSISTANT_AI_ENABLED`
- `DEEPSEEK_API_KEY`
- `DEEPSEEK_BASE_URL`
- `DEEPSEEK_MODEL`
- `DEEPSEEK_LOG_REQUESTS`
- `DEEPSEEK_LOG_RESPONSES`
- `DEEPSEEK_TEMPERATURE`
- `DEEPSEEK_MAX_COMPLETION_TOKENS`
- `DEEPSEEK_TIMEOUT_SECONDS`
- `DEEPSEEK_MAX_RETRIES`

## Valores recomendados em producao

- `APP_FRONTEND_WEB_DIST=file:/app/frontend-web/`
- `DESPESAS_BACKEND_BASE_URL=http://backend:8080`
- `N8N_PROTOCOL=https`
- `N8N_PORT=5678`
- `WEBHOOK_URL=https://n8n.<dominio>/`
- `N8N_PROXY_HOPS=1`

O `n8n` continua escutando internamente em `5678`. O proxy e quem publica `443` externamente.

## Ordem de subida

1. garantir envs de producao presentes na VPS
2. publicar o build do Flutter Web em `/srv/despesas/frontend-web/current`
3. materializar a imagem do backend em `BACKEND_IMAGE`
4. subir `postgres`, `backend` e `n8n`
5. subir `proxy`
6. executar smoke minimo

## Smoke minimo pos-subida

- `GET https://<app>/actuator/health`
- `GET https://<app>/`
- `POST https://<app>/api/v1/auth/login`
- `GET https://<app>/api/v1/expenses`
- `POST https://<app>/api/v1/financial-assistant/query`
- abrir `https://<n8n>/`
- executar replay controlado do workflow principal do n8n

## Boundary oficial

- `main` do backend = source of truth do runtime/deploy
- `main` do Flutter = source of truth do app e do artefato web
- `main` do repo privado do n8n = source of truth de workflows, docs e scripts seguros

O repo do n8n nao e o lugar do compose final da aplicacao.

## Politica de segredos

Fica fora do Git:

- todos os arquivos reais em `~/envs/despesas/prod`
- `APP_SECURITY_TOKEN_SECRET`
- `APP_OPERATIONAL_EMAIL_INGESTION_TOKEN`
- `DB_PASSWORD`
- `DEEPSEEK_API_KEY`
- `N8N_ENCRYPTION_KEY`
- secrets OAuth Google e Microsoft

Os arquivos `google.env` e `microsoft.env` continuam na VPS para bootstrap e recuperacao de credenciais do n8n. Eles nao entram no compose steady-state desta fase.

Vai para a VPS:

- envs reais
- build do Flutter Web
- volumes persistentes do `postgres`, `n8n` e `proxy`

Nunca pode ir para nenhum repositório:

- exports decryptados de credenciais do n8n
- banco/volume do n8n
- chaves privadas
- arquivos equivalentes a `secrets.txt` e `azure.txt`

## Fluxo futuro de CI/CD

1. merge em `main` do backend ou Flutter
2. GitHub Actions builda:
   - backend: `./mvnw -DskipTests spring-boot:build-image`
   - Flutter Web: `flutter build web --release --dart-define=API_BASE_URL=https://<app>`
3. pipeline publica:
   - imagem do backend
   - artefato `build/web`
4. VPS atualiza a stack Compose
5. smoke pos-deploy valida app, API, assistente e n8n

### GitHub Secrets esperados

- `VPS_HOST`
- `VPS_PORT`
- `VPS_USER`
- `VPS_SSH_KEY`
- `BACKEND_IMAGE`
- `APP_PUBLIC_HOST`
- `N8N_PUBLIC_HOST`
- `ACME_EMAIL`
