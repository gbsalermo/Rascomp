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


## Gate de higienização do primeiro banco cloud

O primeiro deploy **não reutiliza o banco local/testdata**.

Fluxo obrigatório:

```text
MySQL cloud NOVO e vazio
→ Flyway V1..atual cria somente o schema
→ profile cloud mantém TODOS os seeds/testdata desligados
→ bootstrap cria somente o primeiro DEV real
→ DEV entra no sistema
→ cria contas GESTAO/MIDIA reais necessárias
→ organização cadastra dados reais
```

### Dados permitidos no primeiro boot

```text
flyway_schema_history
schema/tabelas vazias
1 UserAccount DEV real (bootstrap)
```

Nenhum destes dados pode vir automaticamente:

```text
equipes demo
competidores demo
robôs demo
competições demo
inscrições demo
tentativas/rounds
chaves
instituições de exemplo
dev.b3/dev.b4
gestao.b3/gestao.b4
lider.demo/membro.demo
organizacao.demo
```

O antigo `DataInitializer` cria massa completa de desenvolvimento e por isso é explicitamente desligado em `application-cloud.properties`.

### Proteção de startup

`CloudProfileSafetyGuard` aborta o startup se:

- `cloud` e `testdata` forem ativados juntos;
- qualquer flag conhecida de seed/testdata estiver `true`.

Falhar no startup é preferível a popular silenciosamente um banco persistente.

### Primeiro DEV

O único bootstrap automático permitido é:

```text
RASCOMP_DEV_NOME
RASCOMP_DEV_EMAIL
RASCOMP_DEV_PASSWORD
```

Regras operacionais:

1. usar dados reais;
2. usar senha inicial forte;
3. confirmar o primeiro login;
4. criar as demais contas internas pelo módulo DEV;
5. após confirmar o DEV persistido, remover `RASCOMP_DEV_PASSWORD` dos secrets do ambiente quando operacionalmente possível.

O bootstrap não cria outra conta se já existir DEV.

### Auditoria antes de abrir tráfego real

Antes da divulgação/publicação real, conferir contagens:

```text
Team = 0
Competitor = 0
Robot = 0
Competition = 0
Registration = 0
Match/Bracket/Round = 0
UserAccount = somente contas reais aprovadas
```

Depois disso os dados reais são cadastrados normalmente pelo sistema.


## Primeiro acesso das contas internas

Depois do DEV de bootstrap, nenhuma conta interna criada pela UI/API recebe senha escolhida pelo administrador.

```text
DEV cria identidade
→ e-mail de convite
→ titular define a primeira senha
→ conta ativada
```

Isso deve ser validado no smoke cloud antes de criar contas reais de GESTAO/MIDIA.
