# Continuidade — RasComp Backend

Última atualização: **31/08/2026**

Este arquivo registra o **checkpoint funcional do backend**. Ele não define um roadmap próprio.

Planejamento canônico cross-repo:

```text
gbsalermo/Rascomp-FRONT
docs/ETAPAS_POS_PROJETO.md
```

Dossiê arquitetural canônico:

```text
gbsalermo/Rascomp-FRONT
docs/DOSSIE_PROJETO_RASCOMP.md
```

Índice/hierarquia da documentação:

```text
gbsalermo/Rascomp-FRONT
docs/README.md
```

---

# 1. Marco atual

O RasComp foi apresentado e aprovado. O backend forma uma base funcional do RRC e está agora no ciclo de estabilização pós-projeto.

Estado oficial:

```text
ETAPA 0  ✅ concluída / validada
ETAPA 1  🚧 atual — correções de lógica e riscos
ETAPA 2+ ⏳ não iniciadas
```

Não iniciar ETAPA 2 ou refatorações estruturais amplas antes da validação da ETAPA 1.

---

# 2. Estado funcional conhecido

```text
AUTENTICAÇÃO / JWT                       ✅
OWNERSHIP PARTICIPANTE                   ✅
MYSQL + FLYWAY V1–V7                     ✅
COMPETIÇÕES                              ✅
EQUIPES / COMPETIDORES / ROBÔS           ✅
INSCRIÇÕES + REVISÃO                     ✅
FOTOS DOS ROBÔS                          ✅
FOLLOW LINE                              ✅
RANKING FOLLOW                           ✅
SUMÔ / INSPEÇÃO / ROUNDS                 ✅
2 PENALIDADES = DERROTA DO ROUND         ✅
SUICÍDIO/WO                              ✅
CHAVES / BYE / PROGRESSÃO                ✅
HISTÓRICO DE CHAVES                      ✅
API PARTICIPANTE                         ✅ base funcional
API PÚBLICA                              ✅
PROFILE TESTDATA                         ✅
```

Último checkpoint automatizado documentado:

```text
48 testes
0 falhas
0 erros
0 skipped

MySQL + Flyway + testdata ✅
```

Não presumir que a contagem continua igual após mudanças futuras; atualizar somente depois de uma execução real.

---

# 3. Stack e estrutura

```text
Java 21
Spring Boot 3.5.x
Spring Security + JWT + BCrypt
JPA / Hibernate
MySQL
Flyway
Maven
Swagger/OpenAPI
Cloudflare R2 preparado para mídia futura
```

Código:

```text
rascomp/src/main/java/br/edu/ufrb/rascomp/
```

Pacotes principais:

```text
config
controller
dto
exception
model
repository
security
service
storage
teste
```

---

# 4. Migrations

```text
V1 — schema competitivo principal
V2 — inspeções de Sumô
V3 — rounds de Sumô
V4 — remoção/limpeza de schema legado de Follow/chaves
V5 — usuários / ownership / fotos
V6 — histórico de chaves
V7 — regras estendidas de round/penalidades
```

Regra congelada:

```text
V1–V7 nunca são reescritas
próxima mudança estrutural = V8+
```

Em 31/08/2026 ainda não existe migration V8 no branch principal.

---

# 5. Segurança atual

Modelo implementado:

```text
UserRole
├─ PARTICIPANTE
└─ ORGANIZACAO
```

Política predominante:

```text
/api/v1/public/**       → público
/api/v1/participante/** → PARTICIPANTE
/api/v1/**              → ORGANIZACAO
```

A nova matriz:

```text
DEV
GESTAO
MIDIA
PARTICIPANTE
```

é uma decisão aprovada para a **ETAPA 3** e ainda não está implementada.

Arquivos centrais futuros:

```text
model/Enum/UserRole.java
config/SecurityConfig.java
model/UserAccount.java
service/UserAccountService.java
controller/UserAccountController.java
controllers por domínio
```

Conta inativa já está protegida corretamente: `JwtService.tokenValido()` depende de `usuario.isEnabled()`.

---

# 6. Distinção de domínio crítica

```text
UserAccount
→ autenticação / role / ativo

Competitor
→ pessoa que compete
→ pertence a Team
→ pode vincular UserAccount

Team.responsibleUser
→ usuário responsável pela equipe no portal
```

Não implementar movimentações administrativas como troca genérica de FK.

Futuros Ajustes Gerais devem separar:

```text
transferirCompetidor
transferirResponsabilidade
transferirRobo
alterarRole
```

---

# 7. Registration

Modelo atual:

```text
Registration
├─ Competition obrigatória
├─ CompetitionCategory obrigatória
├─ Team obrigatória
├─ Robot obrigatório
├─ Competitor(s)
├─ status
├─ requestedByUser
├─ reviewedByUser
└─ ativo
```

Unicidade atual:

```text
competition + category + robot
```

Arquivos:

```text
model/Registration.java
dto/RegistrationDTO.java
dto/ParticipantRegistrationRequest.java
controller/RegistrationController.java
service/RegistrationService.java
repository/RegistrationRepository.java
service/ParticipantPortalService.java
```

---

# 8. ETAPA 1 — riscos atuais que ainda precisam ser fechados

A ETAPA 1 está em andamento. Não registrar estes itens como corrigidos até implementação + testes + validação.

## 8.1 Reativação

Problema confirmado:

```text
RegistrationService.reativar()
→ reativa Registration como PENDENTE
→ não chama validarInscricoesAbertas()
```

Risco: reativação fora da janela de inscrição.

## 8.2 Cancelamento

Precisa regra explícita para cancelamento após:

- aprovação;
- geração de chave;
- início da competição;
- geração de histórico competitivo.

Ownership existe, mas não resolve sozinho a consistência do estado.

## 8.3 Chave

`BracketGenerationService` precisa restringir explicitamente os estados de `Competition` válidos para geração/regeneração.

## 8.4 MatchResult após progressão

Alterar um vencedor depois de ele alimentar a próxima fase pode corromper a árvore.

Decidir e implementar:

```text
bloqueio
ou
rollback/reprocessamento explícito
```

## 8.5 Follow

Formalizar combinações válidas de:

```text
concluida
valida
tempoSegundos
checkpointsAlcancados
```

O impacto de checkpoints no ranking ainda depende do regulamento oficial.

---

# 9. Follow Line

Domínio:

```text
Registration
└─ Tomadas
   └─ Tentativas
```

Ranking atual:

```text
válida + concluída + tempo
→ melhor tentativa da tomada
→ melhor tomada da inscrição
→ menor tempo final
```

```text
tempoFinal = tempoSegundos + penalidadeSegundos
```

`checkpointsAlcancados` é persistido/exibido, mas não altera ranking atualmente.

---

# 10. Sumô

Fluxo atual:

```text
Registration APROVADA
→ inspeção apta
→ chave
→ partida
→ rounds
→ MatchResult
→ progressão
```

Regras consolidadas:

- `SUICIDIO_WO` = adversário vence o round;
- 0/1 penalidade = disputa normal;
- **2 penalidades = derrota automática do round**;
- `motivoResultado=PENALIDADES` nesse caso;
- BYE automático;
- chave histórica read-only.

Categorias compartilham o motor e se isolam por:

```text
competitionId + categoryId
```

---

# 11. Fotos e storage

Fotos de robô hoje:

```text
RobotImageService
→ RobotImageStorageService
→ ./uploads/robots
```

Em paralelo existe:

```text
storage/ObjectStorageService.java
storage/R2ObjectStorageService.java
config/R2StorageConfiguration.java
config/R2StorageProperties.java
```

A abstração R2 será reaproveitada pelo futuro CMS/Mídia. Não criar terceiro mecanismo de upload.

---

# 12. Dívida técnica reservada para ETAPA 2

Em 31/08/2026 o repositório ainda contém:

```text
rascomp/bin/
```

com árvore antiga/artefatos versionados. `.gitignore` já impede novos `bin/`, mas os arquivos rastreados continuam no branch principal.

A remoção deve ocorrer na ETAPA 2, em commit isolado, seguida de CI.

Também avaliar nessa etapa:

```text
.classpath
.project
.gitkeep desnecessários
TODOs/comentários antigos
código morto/duplicado
```

Não usar a existência dessa pendência para antecipar ETAPA 2 enquanto ETAPA 1 não estiver validada.

---

# 13. Evoluções já aprovadas, mas ainda não iniciadas

A sequência oficial está somente no roadmap canônico. Para contexto técnico:

## ETAPA 3 — roles

```text
DEV | GESTAO | MIDIA | PARTICIPANTE
```

## ETAPA 4 — avisos

IN_APP persistido como fonte de verdade; Telegram complementar no futuro.

## ETAPA 5 — Ajustes Gerais + auditoria

Operações de domínio explícitas, não CRUD bruto/SQL.

## ETAPA 6 — portabilidade

Uma instalação por instituição organizadora. Não é multi-tenant.

## ETAPA 7 — CMS/Mídia

```text
MediaAsset
ContentSlot
ContentItem
```

Usar `ObjectStorageService`/R2.

## ETAPA 8 — Regras

Conteúdo oficial de Follow, Sumô, Futebol e ambiente/vestimenta.

## ETAPA 9 — Futebol de Robôs

Hoje o schema é incompatível porque `Registration.robot`/`robotId` são obrigatórios.

A solução deverá permitir legitimamente inscrição sem robô próprio conforme modalidade. **Não criar robô fake.**

## ETAPA 10 — participante completo

Inclui identificador competitivo por `Registration` aprovada.

## ETAPA 12–14

Hardening → testes manuais → deploy cloud.

---

# 14. Futebol — decisões ainda abertas

Antes de modelar/migrar:

- Team obrigatória ou não;
- como os robôs da organização são atribuídos;
- tempo/placar;
- empate/desempate;
- formato competitivo;
- inspeção/penalidades.

---

# 15. Executar localmente

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

`testdata` é opt-in e não deve ser habilitado em produção.

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

---

# 16. Como outra IA deve continuar o backend

```text
1. ler Rascomp-FRONT/docs/README.md
2. conferir ETAPA atual em ETAPAS_POS_PROJETO.md
3. ler o Dossiê Mestre
4. usar este arquivo como checkpoint do backend
5. verificar o código atual antes de concluir qualquer coisa
6. trabalhar somente na etapa atual
7. criar/ajustar testes junto com regras
8. migration nova somente V8+
9. manter modo local
10. atualizar documentação ao encerrar o checkpoint
11. parar e aguardar validação antes de avançar
```

Para a ETAPA 1 atual, a prioridade é integridade de `Registration`, estado competitivo de chaves/resultados e regras válidas do Follow — não novas features.