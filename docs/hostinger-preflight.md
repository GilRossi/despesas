# Hostinger preflight

Guia de preflight para primeira subida manual e base de CI/CD controlado.

## Premissas desta fase

- VPS Hostinger ja contratada
- Ubuntu 24.04 com Docker e `docker compose`
- Traefik ja existe no host e continua sendo o proxy oficial
- sem deploy automatico em producao nesta fase
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
- `CD` ainda nao entra em producao
- o passo intermediario desta fase e um preflight manual por `workflow_dispatch`
- o deploy real so deve nascer depois da primeira subida manual e do smoke externo

## Gate recomendado para o proximo passo

- repositório backend usa `main` protegida e continua `PR-first`
- quando o deploy real nascer, ele deve usar `workflow_dispatch` e `environment` de producao
- enquanto a primeira subida manual nao passar, o workflow manual fica restrito a:
  - render de compose
  - build local do backend
  - build do artefato web
  - validacoes de preflight
