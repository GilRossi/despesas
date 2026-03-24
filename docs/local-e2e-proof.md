# Prova local fim a fim

Esta prova local existe para demonstrar, de forma reproduzível, o fluxo real entre:

- backend Spring Boot
- Flutter Web oficial
- n8n local com request assinada
- fluxo autenticado por papéis
- fluxo operacional com `sourceAccount` resolvendo household no backend

## Pré-requisitos

- envs locais governados em `~/envs/despesas/local`
- Docker disponível
- Flutter disponível
- `jq`, `curl` e `python3`

## Execução mínima

No backend:

```bash
scripts/proof/run_local_macro3_smoke.sh
```

O smoke:

1. garante `postgres` local + criação do banco esperado mesmo com volume antigo
2. sobe o backend com chave operacional temporária apenas no processo local, sem editar env real
   - usando o profile `local-proof` para habilitar apenas o CORS necessário ao Flutter Web local
3. sobe o n8n local com a mesma chave operacional temporária
4. garante/importa/publica os workflows canônicos de replay e processor
5. roda o E2E web real no Flutter com screenshots em viewport reduzido
6. valida por API:
   - provisionamento e logins
   - bloqueios por papel
   - isolamento do assistente por household
   - ingestão operacional assinada via n8n
   - replay simples rejeitado
   - payload com `householdId` rejeitado
7. grava um resumo em `build/local_e2e/proof-summary.json`

## Evidências locais

- resumo estruturado: `build/local_e2e/proof-summary.json`
- log do backend: `build/local_e2e/backend.log`
- screenshots web: `/home/gil/StudioProjects/despesas_frontend/build/local_e2e/screenshots`

## Observações

- o script não toca em produção nem em env real
- a chave operacional local usada para a prova é efêmera no processo quando os envs oficiais não a trazem
- se já existir um backend externo rodando em `localhost:8080` sem o profile local da prova, o smoke falha de forma explícita em vez de mascarar o problema
- o backend continua sendo a fonte de verdade para tenancy, papéis, assistente e ingestão operacional
