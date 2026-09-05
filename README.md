# RasComp — Backend

Backend da plataforma **RasComp**, responsável pelas regras de domínio, persistência e APIs da competição RRC.

```text
RAS UFRB = organização / capítulo estudantil
RRC      = evento/competição
RasComp  = plataforma de software
```

## Estado atual — 04/09/2026

```text
ETAPA 0  ✅ baseline concluída / validada
ETAPA 1  🚧 atual — correções de lógica e integridade
ETAPA 2+ ⏳ não iniciadas
```

Em 04/09/2026 foi realizado um checkpoint de limpeza/revisão **documental**. Não houve mudança funcional nem antecipação da ETAPA 2.

Roadmap canônico cross-repo:

```text
gbsalermo/Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md
```

Dossiê Mestre:

```text
gbsalermo/Rascomp-FRONT/docs/DOSSIE_PROJETO_RASCOMP.md
```

---

## Stack

- Java 21
- Spring Boot 3.5.x
- Spring Security + JWT + BCrypt
- JPA/Hibernate
- **MySQL**
- Flyway V1–V7
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

O backend é fonte de verdade para autorização, ownership, elegibilidade, inscrições, ranking, inspeção, BYE, vencedor, progressão, campeão e resultados competitivos.

---

## Estado funcional conhecido

```text
Autenticação/JWT                        ✅
Ownership participante                 ✅
Competições                            ✅
Equipes / competidores / robôs         ✅
Inscrições / revisão                   ✅
Fotos de robôs                         ✅
Follow Line / ranking                  ✅
Sumô / inspeção / rounds               ✅
2 penalidades = derrota do round       ✅
Suicídio/WO                            ✅
Chaves / BYE / progressão              ✅
Histórico de chaves                    ✅
API pública                            ✅
API participante                       ✅ base funcional
Testdata                               ✅
```

Último checkpoint automatizado documentado:

```text
48 testes
0 falhas
0 erros
MySQL + Flyway + testdata ✅
```

Não atualizar essa contagem sem nova execução real.

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
V1 — schema competitivo principal
V2 — inspeções de Sumô
V3 — rounds de Sumô
V4 — remoção de estrutura legada Follow/chaves
V5 — usuários / ownership / fotos
V6 — histórico de chaves
V7 — regras estendidas de round/penalidades
```

Regra congelada:

```text
V1–V7 nunca são reescritas
próxima mudança estrutural = V8+
```

---

## ETAPA 1 — riscos em tratamento

1. `RegistrationService.reativar()` não revalida a janela de inscrições.
2. Cancelamento precisa política explícita conforme estado competitivo.
3. Geração/regeneração de chave precisa restringir estados de `Competition`.
4. Alteração de `MatchResult` após progressão precisa bloqueio ou rollback/reprocessamento.
5. Estados válidos de tentativa Follow precisam formalização.

O efeito oficial dos checkpoints do Follow depende de regulamento e não deve ser inventado.

---

## Follow Line

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

---

## Sumô

```text
Registration APROVADA
→ inspeção apta
→ Bracket
→ Match
→ RoundSumo
→ MatchResult
→ progressão
```

Regras relevantes:

- BYE automático;
- Suicídio/WO = adversário vence;
- 0/1 penalidade = disputa normal;
- 2 penalidades = derrota automática do round;
- chave histórica é read-only.

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

O repositório ainda contém:

```text
rascomp/bin/
.classpath
.project
.gitkeep desnecessários em alguns packages
```

Essa limpeza **não foi antecipada** pelo checkpoint documental e permanece ETAPA 2.

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
4. rascomp/docs/CONTINUIDADE.md
```

Depois do checkpoint documental, o próximo trabalho é **retomar a ETAPA 1**.