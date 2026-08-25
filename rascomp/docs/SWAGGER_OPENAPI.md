# Swagger / OpenAPI — Rascomp

Este documento registra como validar a documentação OpenAPI da API Rascomp antes do merge da branch `swagger-openapi`.

## Endereços locais

Com o backend em execução:

```text
Swagger UI
http://localhost:8080/swagger-ui.html

OpenAPI completo
http://localhost:8080/v3/api-docs

Grupo completo
http://localhost:8080/v3/api-docs/completa

Grupo público / landing
http://localhost:8080/v3/api-docs/publica

Grupo participante
http://localhost:8080/v3/api-docs/participante

Grupo organização
http://localhost:8080/v3/api-docs/organizacao
```

## Grupos esperados na Swagger UI

```text
API completa
Público / Landing
Participante
Organização
```

A separação não cria novos endpoints. Ela apenas organiza o mesmo contrato HTTP congelado.

## Autenticação

O Swagger possui o esquema:

```text
bearerAuth
HTTP Bearer
JWT
```

Fluxo de teste:

1. executar `POST /api/v1/auth/register` ou `POST /api/v1/auth/login`;
2. copiar apenas o valor de `token` retornado;
3. clicar em **Authorize**;
4. colar o JWT no campo `bearerAuth`;
5. testar um endpoint protegido.

O Swagger adiciona o prefixo Bearer de acordo com o esquema HTTP configurado.

## Separação de acesso documentada

```text
/api/v1/public/**
  sem JWT

/api/v1/auth/register
/api/v1/auth/login
  sem JWT

/api/v1/auth/me
  usuário autenticado

/api/v1/participante/**
  PARTICIPANTE + ownership

/api/v1/**
  ORGANIZACAO, exceto rotas públicas/autenticação já descritas
```

## Checklist visual

Validar na Swagger UI:

```text
[ ] título "Rascomp API"
[ ] versão v1
[ ] botão Authorize disponível
[ ] bearerAuth identificado como JWT
[ ] grupos completa/publica/participante/organizacao
[ ] endpoints organizados por tags
[ ] parâmetros de path/query exibidos
[ ] request bodies exibidos
[ ] schemas dos DTOs exibidos
[ ] exemplos de autenticação exibidos
[ ] exemplos de payload do participante exibidos
[ ] exemplos de ConfigFollow/ConfigSumo exibidos
[ ] exemplos de TentativaSeguidorLinha/InspecaoSumo/RoundSumo exibidos
[ ] ApiErrorResponse presente em Schemas
[ ] respostas 400/404/405/409/500 documentadas
[ ] respostas 401/403 documentadas nos endpoints protegidos
[ ] 413 documentado para fotos
[ ] 415 documentado para operações com corpo/upload
```

## Testes funcionais mínimos pelo Swagger

### Público

Executar sem Authorize:

```http
GET /api/v1/public/competicoes
```

Esperado: `200`.

### Participante

Autenticar com conta `PARTICIPANTE` e executar:

```http
GET /api/v1/auth/me
GET /api/v1/participante/equipes
```

Esperado: `200`.

Um PARTICIPANTE tentando rota administrativa, por exemplo:

```http
GET /api/v1/competicoes
```

Esperado: `403`.

### Organização

Autenticar com conta `ORGANIZACAO` e executar:

```http
GET /api/v1/auth/me
GET /api/v1/usuarios?role=PARTICIPANTE
GET /api/v1/competicoes
```

Esperado: `200`.

Esta etapa também fecha a pendência manual do smoke anterior referente ao token de `ORGANIZACAO`.

## Fotos de robôs

No endpoint multipart:

```http
POST /api/v1/participante/robos/{robotId}/fotos
```

A Swagger UI deve apresentar o campo `arquivo` como upload de arquivo.

Formatos aceitos pelo backend:

```text
JPEG
PNG
WEBP
máximo 5 MB
```

## FOLLOW_LINE

A documentação deve deixar explícito:

```text
ConfigFollow
→ TentativaSeguidorLinha
→ RankingFollow
```

FOLLOW_LINE não utiliza:

```text
Bracket
Match
RoundSumo
MatchResult
```

O ranking considera a melhor tentativa válida e concluída e usa:

```text
tempoFinal = tempoBrutoSegundos + penalidadeSegundos
```

## SUMO

A documentação deve refletir:

```text
ConfigSumo
→ InspecaoSumo
→ Bracket
→ Match
→ RoundSumo
→ MatchResult automático
```

`MatchResult` é somente leitura na API externa. Na Swagger UI, a tag **Resultados de Partida** deve conter somente operações `GET`.

## Erros

Erros tratados pelos controllers/services usam `ApiErrorResponse`:

```json
{
  "timestamp": "2026-08-24T21:30:00",
  "status": 400,
  "error": "Regra de negócio inválida",
  "message": "O robô não pertence à equipe informada.",
  "path": "/api/v1/..."
}
```

Erros `401` e `403` emitidos diretamente pelo Spring Security mantêm o formato compacto atual do contrato:

```json
{
  "status": 401,
  "error": "Não autenticado"
}
```

ou:

```json
{
  "status": 403,
  "error": "Acesso negado"
}
```

## Critério de conclusão

Swagger só deve ser mergeado na `main` quando:

```text
CI verde                              ✅ obrigatório
Swagger UI abre                       ⏳ manual
OpenAPI JSON abre                     ⏳ manual
Authorize JWT funciona                ⏳ manual
PARTICIPANTE validado pelo Swagger    ⏳ manual
ORGANIZACAO validada pelo Swagger     ⏳ manual
upload multipart aparece corretamente ⏳ manual
MatchResult somente GET               ⏳ manual
Follow/Sumô coerentes                  ⏳ revisão visual
```
