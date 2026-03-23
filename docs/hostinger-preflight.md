# Hostinger preflight

Guia de preflight para produção documentada, convivendo com os workflows de CD já existentes para backend e Flutter Web.

## Premissas desta fase

- VPS Hostinger ja contratada
- Ubuntu 24.04 com Docker e `docker compose`
- Traefik ja existe no host e continua sendo o proxy oficial
- backend e Flutter Web ja possuem workflows de CD com deploy real para a VPS
- sem segredos reais no repositório

## Runtime oficial no host atual

- `backend` e `n8n` entram atras do Traefik existente
- `postgres` permanece apenas na rede interna
- o projeto nao sobe mais um proxy proprio dentro da stack
- o build do Flutter Web continua montado em `/srv/despesas/frontend-web/current`
- a auditoria shell real do host confirmou que o Traefik:
  - usa provider Docker
  - roda em `host network`
  - publica `80/443`
  - usa entrypoints `web` e `websecure`
  - usa `certresolver` nomeado `letsencrypt`

## Variaveis novas de runtime

- `TRAEFIK_ENTRYPOINTS`
- `TRAEFIK_CERTRESOLVER`
- `N8N_BLOCK_ENV_ACCESS_IN_NODE=false` no runtime do n8n

## Preflight manual do host

1. rodar `scripts/runtime/prepare-production-host.sh`
2. materializar os envs reais em `~/envs/despesas/prod`
3. validar o compose com `scripts/runtime/validate-production-compose.sh`
4. confirmar no host que o Traefik existente segue com provider Docker, `host network`, entrypoints `web/websecure` e `certresolver` `letsencrypt`

## Estrategia de CI/CD

- `CI` automatico em PR/push para backend e Flutter
- `CD` real ja existe para backend e Flutter Web
- o backend e publicado pela imagem Spring Boot em container
- o Flutter Web e publicado separadamente em `/srv/despesas/frontend-web/current/`
- o n8n continua fora da esteira de deploy automatizado desta fase
- o preflight manual por `workflow_dispatch` continua util para validar compose, imagem e artefato antes de acionar ou confiar no deploy real
- ainda nao existe release atomica unica para backend + Flutter Web + n8n

## Gate recomendado para o proximo passo

- repositório backend usa `main` protegida e continua `PR-first`
- os workflows reais de deploy devem evoluir para usar `environment` de producao e gates mais fortes
- o preflight manual deve continuar restrito a:
  - render de compose
  - build local do backend
  - build do artefato web
  - validacoes de preflight
- a maturidade seguinte exige smoke coordenado e criterio unico de release para backend + Flutter Web + n8n
