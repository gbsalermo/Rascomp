# RasComp — Backend da Plataforma de Gestão do RRC

Backend da plataforma **RasComp**, utilizada para organizar e publicar competições de robótica da RAS UFRB.

```text
RAS UFRB = organização / capítulo estudantil
RRC      = evento/competição
RasComp  = plataforma de software
```

---

## Estado atual — 31/08/2026

O projeto foi apresentado e aprovado. O ciclo atual é de estabilização antes das novas funcionalidades.

```text
ETAPA 0 — baseline/congelamento       ✅ concluída / validada
ETAPA 1 — correções de lógica         🚧 etapa atual
ETAPA 2+                              ⏳ não iniciadas
```

A ordem completa **não é mantida neste README**. O roadmap canônico cross-repo está em:

```text
gbsalermo/Rascomp-FRONT
docs/ETAPAS_POS_PROJETO.md
```

Não avançar de etapa sem validação explícita.

---

## Stack

- Java 21
- Spring Boot 3.5.x
- Spring Security + JWT + BCrypt
- JPA/Hibernate
- MySQL
- Flyway V1–V7
- Maven
- Swagger/OpenAPI
- Cloudflare R2 preparado para mídia futura

---

## Estado funcional conhecido

```text
Autenticação / JWT                     ✅
Ownership participante                 ✅
Competições                            ✅
Equipes / competidores / robôs         ✅
Inscrições / revisão                   ✅
Fotos de robôs                         ✅
Follow Line                            ✅
Ranking por melhor tomada              ✅
Sumô / inspeção / rounds               ✅
2 penalidades = derrota do round       ✅
Suicídio/WO                            ✅
Chaves / BYE / progressão              ✅
Histórico de chaves                    ✅
API pública                            ✅
API participante                       ✅ base funcional
Testdata/demo                          ✅
```

Último checkpoint automatizado documentado:

```text
48 testes
0 failures
0 errors
0 skipped

MySQL + Flyway + testdata ✅
```

Esse número é um checkpoint, não uma garantia permanente. Atualizar somente após nova execução real.

---

## Arquitetura

Código principal:

```text
rascomp/src/main/java/br/edu/ufrb/rascomp/
```

Fluxo predominante:

```text
Controller
→ DTO
→ Service
→ Repository
→ MySQL
```

O backend é a fonte de verdade para:

- autorização;
- ownership;
- elegibilidade;
- ranking;
- inspeção;
- BYE;
- vencedor;
- progressão;
- campeão;
- resultado competitivo.

O frontend pode refletir uma regra para UX, mas não substituí-la.

---

## Segurança atual

O modelo implementado ainda é legado:

```text
UserRole
├─ PARTICIPANTE
└─ ORGANIZACAO
```

```text
/api/v1/public/**       → público
/api/v1/participante/** → PARTICIPANTE
/api/v1/**              → ORGANIZACAO
```

A matriz aprovada para a **ETAPA 3** é:

```text
DEV
GESTAO
MIDIA
PARTICIPANTE
```

Ela ainda não está implementada.

A conta desativada já é invalidada corretamente porque o JWT é validado junto com `usuario.isEnabled()`.

---

## Migrations

```text
V1 — schema competitivo principal
V2 — inspeções de Sumô
V3 — rounds de Sumô
V4 — limpeza/remoção de estrutura legada de Follow/chaves
V5 — usuários / ownership / fotos
V6 — histórico de chaves
V7 — regras estendidas de round/penalidades
```

Regra:

```text
V1–V7 nunca são reescritas
próxima mudança estrutural = V8+
```

---

## ETAPA 1 — riscos em tratamento

A etapa atual existe para fechar integridade antes de novas features.

### Registration

`RegistrationService.reativar()` ainda precisa impedir reativação indevida fora da janela de inscrições.

Também falta política explícita de cancelamento depois de:

- aprovação;
- geração de chave;
- início da competição;
- histórico competitivo.

### Chaves

`BracketGenerationService` precisa trabalhar apenas com estados de `Competition` explicitamente permitidos para geração/regeneração.

### MatchResult

Alterar vencedor depois que ele já avançou pode deixar a próxima fase inconsistente. A correção deve usar bloqueio ou rollback/reprocessamento explícito.

### Follow

Precisam ser formalizadas as combinações válidas de:

```text
concluida
valida
tempoSegundos
checkpointsAlcancados
```

O impacto oficial de checkpoints continua pendente de regulamento.

---

## Follow Line

Domínio:

```text
Registration
└─ Tomadas
   └─ Tentativas
```

Ranking atual:

```text
tentativas válidas + concluídas + com tempo
→ melhor tentativa da tomada
→ melhor tomada da inscrição
→ menor tempo final
```

```text
tempo final = tempo bruto + penalidade
```

`checkpointsAlcancados` é persistido/exibido, mas não altera ranking atualmente.

---

## Sumô

Categorias usam o mesmo motor e ficam isoladas por:

```text
competitionId + categoryId
```

Fluxo:

```text
Registration APROVADA
→ inspeção apta
→ Bracket
→ Match
→ RoundSumo
→ MatchResult
→ progressão
```

Regras atuais relevantes:

- rounds configuráveis;
- BYE automático;
- Suicídio/WO = vitória do adversário;
- 0/1 penalidade = disputa normal;
- **2 penalidades = derrota automática do round**;
- histórico de chaves somente leitura.

---

## Fotos e storage

Fotos de robô hoje usam:

```text
RobotImageService
→ RobotImageStorageService
→ ./uploads/robots
```

Também existe a abstração:

```text
storage/ObjectStorageService.java
storage/R2ObjectStorageService.java
```

Ela será reutilizada pelo futuro CMS/Mídia. Não criar terceiro mecanismo de upload.

---

## Dívida técnica já mapeada para ETAPA 2

O branch principal ainda possui:

```text
rascomp/bin/
```

com artefatos antigos/versionados. A remoção é parte da ETAPA 2 e deve ocorrer em commit isolado com CI verde.

Também serão avaliados:

```text
.classpath
.project
.gitkeep desnecessários
TODOs/comentários antigos
código morto/duplicado
```

Não antecipar ETAPA 2 enquanto a ETAPA 1 não estiver validada.

---

## Evoluções aprovadas para etapas futuras

Sem substituir o roadmap canônico:

```text
ETAPA 3  DEV | GESTAO | MIDIA | PARTICIPANTE
ETAPA 4  Avisos IN_APP + Telegram complementar futuro
ETAPA 5  Ajustes Gerais DEV + auditoria
ETAPA 6  Portabilidade — uma instalação por instituição
ETAPA 7  CMS/Mídia usando ObjectStorageService/R2
ETAPA 8  Regras
ETAPA 9  Futebol de Robôs
ETAPA 10 Participante completo + identificador de Registration
ETAPA 11 Consolidação pública Landing/Galeria
ETAPA 12 Hardening
ETAPA 13 Testes manuais completos
ETAPA 14 Deploy cloud
```

A lista acima é somente resumo. A autoridade de sequência é `Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md`.

---

## Futebol de Robôs

A modalidade futura não exigirá robô próprio do participante.

O schema atual ainda é incompatível porque:

```text
Registration.robot                     obrigatório
ParticipantRegistrationRequest.robotId obrigatório
unicidade                               baseada em robot
```

A solução deverá permitir legitimamente `robot` opcional conforme modalidade.

**Não criar robôs fake para satisfazer FK.**

As regras competitivas ainda precisam ser fechadas antes da migration.

---

## Executar localmente

```powershell
cd rascomp
.\mvnw spring-boot:run
```

Variáveis principais:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
ROBOT_IMAGES_DIR
R2_ENABLED
R2_*
```

Profile de demonstração/testdata:

```powershell
$env:SPRING_PROFILES_ACTIVE="testdata"
.\mvnw spring-boot:run
```

O profile é opt-in e nunca deve ser ativado em produção.

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

---

## Documentação — ordem recomendada

### Antes de alterar o projeto

```text
1. Rascomp-FRONT/docs/README.md
2. Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md
3. Rascomp-FRONT/docs/DOSSIE_PROJETO_RASCOMP.md
4. rascomp/docs/CONTINUIDADE.md
```

### Documentos deste backend

```text
rascomp/docs/CONTINUIDADE.md
→ checkpoint vivo do backend

rascomp/docs/ETAPAS_POS_PROJETO.md
→ ponteiro para o roadmap canônico

rascomp/docs/DOSSIE_PROJETO.md
→ ponteiro para o dossiê canônico

rascomp/docs/CLOUDFLARE_R2.md
→ referência de storage

rascomp/docs/DECISAO_DEPLOY_CLOUD.md
rascomp/docs/DEPLOY_CLOUDFLARE.md
→ referências de deploy futuro
```

Documentos como `CONGELAMENTO_API.md`, `ENDPOINTS_INTERNOS.md`, `ENTIDADES_E_CRUDS.md`, `FLUXO_DO_SISTEMA.md`, `JSON_EXEMPLOS.md`, arquivos `POS_SWAGGER_*` e `TESTES_POSTMAN.md` continuam como **referência técnica/histórica** e devem ser confrontados com código/migrations atuais antes de reutilização.

---

## Regra de continuidade

Outra IA deve:

```text
ler o índice e o roadmap canônico
confirmar a etapa atual
verificar o código real
trabalhar somente na etapa atual
criar/ajustar testes junto com regras
usar V8+ para novo schema
preservar execução local
atualizar documentação
parar no checkpoint e aguardar validação
```

No estado atual, isso significa: **continuar a ETAPA 1, não iniciar novas features nem a limpeza da ETAPA 2**.