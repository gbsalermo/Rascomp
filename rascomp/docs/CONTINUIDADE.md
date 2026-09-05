# Continuidade — RasComp Backend

Última atualização: **04/09/2026**

Este arquivo registra o checkpoint funcional do backend. Não define roadmap próprio.

Fontes canônicas:

```text
gbsalermo/Rascomp-FRONT/docs/README.md
gbsalermo/Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md
gbsalermo/Rascomp-FRONT/docs/DOSSIE_PROJETO_RASCOMP.md
```

---

# 1. Marco atual

```text
ETAPA 0  ✅ concluída / validada
ETAPA 1  🚧 atual — lógica e integridade
ETAPA 2+ ⏳ não iniciadas
```

Em 04/09/2026 houve um checkpoint exclusivamente documental: READMEs, dossiê, roadmap e continuidades foram revisados e documentos obsoletos foram removidos. **Nenhuma correção funcional da ETAPA 1 foi marcada como concluída e a ETAPA 2 não foi iniciada.**

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

Não presumir que a contagem continua igual após futuras mudanças; atualizar somente depois de execução real.

---

# 3. Stack

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

**Camunda e PostgreSQL não fazem parte da arquitetura ativa.**

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
V4 — remoção de estrutura legada Follow/chaves
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

# 5. Segurança atual

```text
UserRole
├─ ORGANIZACAO
└─ PARTICIPANTE
```

```text
/api/v1/public/**       → público
/api/v1/participante/** → PARTICIPANTE
/api/v1/**              → ORGANIZACAO
```

Nova matriz aprovada para ETAPA 3:

```text
DEV | GESTAO | MIDIA | PARTICIPANTE
```

Conta inativa já é rejeitada nas autenticações subsequentes pela validação JWT.

---

# 6. Distinções de domínio

```text
UserAccount
→ autenticação / role / ativo

Competitor
→ pessoa que compete
→ pertence a Team
→ pode opcionalmente vincular UserAccount

Team.responsibleUser
→ responsável da equipe no portal
```

Não implementar transferências administrativas como simples troca genérica de FK.

---

# 7. Registration

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

Unicidade:

```text
competition + category + robot
```

Arquivos centrais:

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

# 8. ETAPA 1 — riscos ainda abertos

## 8.1 Reativação

`RegistrationService.reativar()` reativa como `PENDENTE` sem chamar a validação de inscrições abertas.

## 8.2 Cancelamento

Precisa política explícita depois de aprovação, geração de chave, início da competição ou existência de histórico competitivo.

## 8.3 Chave

`BracketGenerationService` precisa restringir estados de `Competition` válidos para geração/regeneração.

## 8.4 MatchResult após progressão

Mudar vencedor depois de alimentar a próxima fase pode corromper a árvore. Implementar bloqueio ou rollback/reprocessamento explícito.

## 8.5 Follow

Formalizar combinações válidas de:

```text
concluida
valida
tempoSegundos
checkpointsAlcancados
```

Impacto de checkpoints depende de regulamento oficial.

Nenhum destes itens deve ser marcado como corrigido antes de implementação + testes + validação.

---

# 9. Follow Line

```text
Registration
└─ Tomadas
   └─ Tentativas
```

Ranking:

```text
válida + concluída + tempo
→ melhor tentativa da tomada
→ melhor tomada da inscrição
→ menor tempo final
```

```text
tempoFinal = tempoSegundos + penalidadeSegundos
```

`checkpointsAlcancados` é persistido/exibido e atualmente não altera ranking.

---

# 10. Sumô

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

- `SUICIDIO_WO` = adversário vence;
- 0/1 penalidade = disputa normal;
- 2 penalidades = derrota automática do round;
- BYE automático;
- chave histórica read-only.

Categorias ficam isoladas por `competitionId + categoryId`.

---

# 11. Fotos e storage

```text
RobotImageService
→ RobotImageStorageService
→ ./uploads/robots
```

Para mídia futura:

```text
ObjectStorageService
R2ObjectStorageService
```

Não criar terceiro mecanismo de upload.

---

# 12. ETAPA 4 — Avisos + Telegram

A decisão de planejamento atual concentra avisos IN_APP e Telegram na mesma etapa.

```text
GESTAO/DEV
→ publica aviso por Competition
→ backend persiste IN_APP
→ Telegram distribui a mesma comunicação se habilitado
```

Regras:

- IN_APP é fonte de verdade;
- integração Telegram fica no backend;
- falha externa não invalida o aviso;
- token não é versionado;
- integração pode ser desligada;
- vínculo `UserAccount ↔ Telegram` não é obrigatório inicialmente;
- código competitivo futuro da `Registration` pode ser usado como identificação opcional.

---

# 13. Dívida técnica reservada à ETAPA 2

Ainda existem:

```text
rascomp/bin/
.classpath
.project
.gitkeep desnecessários em packages
```

Além de TODOs/comentários antigos e possíveis duplicações.

O checkpoint documental de 04/09 não removeu esses itens para não antecipar ETAPA 2.

---

# 14. Evoluções futuras resumidas

A ordem oficial está somente no roadmap canônico:

```text
ETAPA 3  nova matriz de roles
ETAPA 4  Avisos IN_APP + Telegram
ETAPA 5  Ajustes Gerais + auditoria
ETAPA 6  portabilidade institucional
ETAPA 7  CMS/Mídia + Landing real
ETAPA 8  Regras
ETAPA 9  Futebol de Robôs
ETAPA 10 participante completo + identificador competitivo
ETAPA 11 Landing + Galeria
ETAPA 12 Hardening
ETAPA 13 testes manuais completos
ETAPA 14 deploy cloud
```

Futebol exigirá alteração real do domínio porque `Registration.robot` é obrigatório hoje. **Não criar robô fake para satisfazer FK.**

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

Testdata:

```powershell
$env:SPRING_PROFILES_ACTIVE="testdata"
.\mvnw spring-boot:run
```

Nunca habilitar `testdata` em produção.

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

---

# 16. Próximo passo / handoff

```text
1. ler Rascomp-FRONT/docs/README.md
2. conferir ETAPA 1 no roadmap
3. ler Dossiê Mestre
4. usar este arquivo como checkpoint backend
5. verificar código real
6. implementar correções da ETAPA 1
7. criar/ajustar testes
8. manter V1–V7 imutáveis
9. manter modo local
10. atualizar documentação no checkpoint
11. aguardar validação antes da ETAPA 2
```

Prioridade imediata: integridade de `Registration`, estado competitivo de chaves/resultados e regras válidas do Follow.