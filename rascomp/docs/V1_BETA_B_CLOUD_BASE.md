# V1-BETA B — Base Cloud do Backend

Status em 06/10/2026: **🚧 estrutura de deploy preparada; recursos remotos ainda não provisionados**.

## Componentes

```text
Dockerfile
application-cloud.properties
Actuator /actuator/health
cloudflare-api/
  ├─ wrangler.jsonc
  └─ src/index.js
```

## Fluxo pretendido

```text
Internet
→ Worker rascomp-api
→ Durable Object nomeado "primary"
→ Container lite
→ Spring Boot :8080
→ MySQL externo
```

Uma única instância nomeada é suficiente para o primeiro Beta e evita adicionar balanceamento prematuro. O backend continua stateless em autenticação; persistência real pertence ao MySQL/R2.

## Secrets

Nenhum segredo fica no `wrangler.jsonc`.

Antes do primeiro deploy remoto serão configurados como Worker Secrets/variáveis:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
CORS_ALLOWED_ORIGINS
IDENTITY_FRONTEND_BASE_URL
RASCOMP_DEV_EMAIL
RASCOMP_DEV_PASSWORD
EMAIL_PROVIDER
EMAIL_FROM
RESEND_API_KEY
R2_*
```

O Worker repassa esses valores ao processo Java somente no startup do Container.

## Profile cloud

`application-cloud.properties`:

- exige DB externo;
- exige JWT, CORS e URL de identidade;
- desliga SQL detalhado;
- mantém testdata desligado;
- desliga Swagger por padrão;
- expõe somente health/info do Actuator;
- usa forwarded headers.

## Health

Endpoint:

```text
GET /actuator/health
```

Ele é público apenas para readiness/healthcheck e não exibe detalhes internos.

## Docker

Build local futuro:

```bash
cd rascomp
docker build -t rascomp-api .
```

A imagem usa Java 21 e executa como usuário não-root.

## Cloudflare Container

O arquivo `cloudflare-api/wrangler.jsonc` segue a configuração atual de Containers com scheduling `durable_object` e imagem construída pelo `Dockerfile`.

O Container possui acesso de saída porque precisa alcançar MySQL, e-mail e R2.

## Bloqueio de storage

O R2 genérico já existe, mas os fluxos atuais de:

```text
fotos de robôs
logos de equipes
comprovantes de inscrição
```

ainda usam adapters/filesystem locais.

Portanto:

```text
deploy técnico / smoke cloud → permitido
dados reais / inscrições reais → BLOQUEADO até storage persistente desses fluxos
```

Não confundir `R2_ENABLED=true` com migração automática desses três fluxos.

## Próximo gate

Quando a conta Cloudflare temporária for configurada:

1. autenticar Wrangler;
2. publicar os dois frontends em `workers.dev`;
3. criar/configurar MySQL externo;
4. inserir secrets do Worker;
5. testar build do Docker/Container;
6. publicar API em `workers.dev`;
7. alinhar CORS e URLs;
8. configurar e-mail real;
9. integrar storage operacional ao R2;
10. smoke ponta a ponta.
