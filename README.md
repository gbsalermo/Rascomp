# RasComp — Backend

Backend da plataforma **RasComp**, responsável pelas regras de domínio, persistência e APIs da competição RRC.

```text
RAS UFRB = organização / capítulo estudantil
RRC      = evento/competição
RasComp  = plataforma de software
```

## Estado atual — 08/09/2026

```text
ETAPA 0  ✅ baseline concluída / validada
ETAPA 1  🚧 atual — correções de lógica e integridade
ETAPA 2+ ⏳ não iniciadas

Bloco 1 — Competition + Registration  ✅
Bloco 2 — Follow Line                  ✅
Bloco 3 — Sumô                         ✅
Bloco 4 — Chaves                       ⏭️ próximo / não iniciado
Bloco 5 — Fluxos integrados            ⏳
```

O Bloco 3 foi encerrado após alinhamento do backend, frontend, testdata e documentação competitiva. A ETAPA 1 continua aberta; a limpeza técnica da ETAPA 2 não foi antecipada.

Roadmap canônico cross-repo:

```text
gbsalermo/Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md
```

Dossiê Mestre:

```text
gbsalermo/Rascomp-FRONT/docs/DOSSIE_PROJETO_RASCOMP.md
```

Contrato competitivo:

```text
gbsalermo/Rascomp-FRONT/docs/CONTRATO_REGRAS_COMPETITIVAS.md
```

---

## Stack

- Java 21
- Spring Boot 3.5.x
- Spring Security + JWT + BCrypt
- JPA/Hibernate
- **MySQL**
- Flyway V1–V11
- Maven
- Swagger/OpenAPI
- Cloudflare R2 preparado para mídia futura

PostgreSQL e Camunda **não fazem parte da arquitetura ativa**.

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
→ JPA/Hibernate
→ MySQL
```

O backend é fonte de verdade para autorização, ownership, elegibilidade, inscrições, ranking, inspeção, BYE, vencedor, progressão, campeão, rounds e resultados competitivos.

---

## Estado funcional conhecido

```text
Autenticação/JWT                        ✅
Ownership participante                 ✅
Competições                            ✅
Equipes / competidores / robôs         ✅
Inscrições / revisão                   ✅
Fotos de robôs                         ✅
Follow Line / ranking                  ✅ Bloco 2 alinhado
Ausência de tomada Follow              ✅ auditável
Sumô / inspeção / rounds               ✅ Bloco 3 alinhado
Inspeção humana APTO/INAPTO            ✅
Modo Sumô AUTONOMO / RC                ✅
Rounds extras justificados             ✅
Falha de inicialização                 ✅
Juízes / decisão de juiz               ✅ auditável
2 penalidades = derrota do round       ✅
Suicídio/WO                            ✅
Chaves / BYE / progressão              ✅ base atual; Bloco 4 pendente
Histórico de chaves                    ✅
API pública                            ✅
API participante                       ✅ base funcional
Testdata                               ✅
```

Último checkpoint automatizado confirmado no CI:

```text
87 testes
0 falhas
0 erros
0 skipped
MySQL + Flyway V11 + testdata ✅
```

O workflow também inicializou o cenário completo `testdata` contra MySQL real.

---

## Segurança atual

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

ETAPA 3 migrará para:

```text
DEV | GESTAO | MIDIA | PARTICIPANTE
```

A conta inativa já deixa de autenticar nas requisições seguintes.

---

## Migrations

```text
V1  — schema competitivo principal
V2  — inspeções de Sumô
V3  — rounds de Sumô
V4  — remoção de estrutura legada Follow/chaves
V5  — usuários / ownership / fotos
V6  — histórico de chaves
V7  — regras estendidas de round/penalidades
V8  — cancelamento de inscrição + histórico de janela
V9  — classe física das categorias de Sumô
V10 — Follow 3×3 + parâmetros operacionais + ausência
V11 — modo Sumô + rounds extras + inspeção auditável + juízes/decisão
```

Regra congelada:

```text
V1–V11 nunca são reescritas
próxima mudança estrutural = V12+
```

---

## ETAPA 1 — estado dos riscos

```text
Reativação de inscrição                         ✅ Bloco 1
Cancelamento / CANCELADA x DESISTENTE           ✅ Bloco 1
Estados válidos Follow                          ✅ Bloco 2
Contrato operacional Follow                     ✅ Bloco 2
Contrato operacional Sumô                       ✅ Bloco 3
Geração/regeneração de chave                    ⏭️ Bloco 4
Correção de resultado após progressão           ⏭️ Bloco 4
Fluxos integrados completos                     ⏳ Bloco 5
```

O efeito de checkpoints do Follow continua apenas informativo e não deve ser alterado sem nova regra competitiva aprovada.

---

## Follow Line

```text
3 tomadas
×
3 tentativas por tomada
```

Ranking:

```text
tentativa classificável
→ melhor tentativa da tomada
→ melhor tomada da inscrição
→ menor tempo final
```

```text
tempoFinal = tempoSegundos + penalidadeSegundos
```

Também existe tomada perdida por ausência como evento próprio e auditável, sem tentativas fictícias.

---

## Sumô

```text
Registration APROVADA
→ inspeção humana APTO/INAPTO
→ Bracket
→ Match
→ RoundSumo
→ MatchResult
→ progressão
```

Regras relevantes:

- peso medido é opcional/informativo e não decide inspeção;
- categorias Sumô usam `AUTONOMO | RC`;
- perfil padrão: 3 rounds regulares / 2 vitórias;
- rounds extras somente quando necessários, limitados e justificados;
- falha de inicialização possui motivo explícito e decisão humana;
- 0/1 penalidade = disputa normal;
- 2 penalidades = derrota automática do round;
- Suicídio/WO = adversário vence;
- BYE = avanço automático;
- decisão de juiz é operação específica, identificada e justificada;
- chave histórica é read-only.

---

## Próximo bloco — Chaves

O Bloco 4 ainda **não foi iniciado**. Ele deverá fechar:

- estado de `Competition` permitido para geração comum;
- regeneração somente antes de atividade competitiva;
- estrutura lógica da chave × agenda operacional;
- correção transacional antes da dependência seguinte iniciar;
- bloqueio de correção comum quando a dependência já iniciou.

---

## Storage

Fotos de robôs hoje:

```text
RobotImageService
→ RobotImageStorageService
→ ./uploads/robots
```

Mídia futura deve reutilizar:

```text
ObjectStorageService
R2ObjectStorageService
```

Não criar terceiro mecanismo de upload.

---

## Avisos e Telegram — futuro ETAPA 4

```text
GESTAO/DEV
→ publica aviso por Competition
→ Aviso IN_APP é persistido
→ Telegram entrega a mesma comunicação quando habilitado
```

O backend será responsável pela integração Telegram. Vínculo obrigatório `UserAccount ↔ Telegram` não faz parte da primeira versão planejada; o futuro código competitivo da `Registration` poderá ser usado opcionalmente para identificação.

---

## Dívida técnica reservada à ETAPA 2

O repositório ainda contém itens como:

```text
rascomp/bin/
.classpath
.project
.gitkeep desnecessários em alguns packages
```

Essa limpeza permanece na ETAPA 2.

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

Testdata opt-in:

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

## Documentação

Leia:

```text
1. Rascomp-FRONT/docs/README.md
2. Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md
3. Rascomp-FRONT/docs/DOSSIE_PROJETO_RASCOMP.md
4. Rascomp-FRONT/docs/CONTRATO_REGRAS_COMPETITIVAS.md
5. rascomp/docs/CONTINUIDADE.md
```

Próximo trabalho, quando explicitamente autorizado: **ETAPA 1 · Bloco 4 — Chaves**.
