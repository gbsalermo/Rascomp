# RasComp — Plataforma de Gestão do RRC

Backend da plataforma **RasComp**, usada para organizar e publicar as competições de robótica da **RAS UFRB**.

```text
RAS UFRB = organização
RRC      = evento/competição
RasComp  = plataforma de software
```

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

## Estado atual

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

Último checkpoint automatizado revisado:

```text
48 testes
0 failures
0 errors
0 skipped

Demo profile:
MySQL + Flyway + testdata ✅
```

## Arquitetura de acesso atual

O modelo atual ainda é legado e será refatorado:

```text
/api/v1/public/**       → público
/api/v1/participante/** → PARTICIPANTE
/api/v1/**              → ORGANIZACAO
```

Roles atuais:

```text
PARTICIPANTE
ORGANIZACAO
```

## Matriz de permissões aprovada para a próxima fase

```text
DEV
├─ acesso total
├─ criar/alterar competição
├─ permissões
├─ manutenção estrutural
└─ Ajustes Gerais

GESTAO
├─ operação de Follow
├─ operação de Sumô
├─ inspeção/partidas/resultados operacionais
└─ sem criação de competição/manutenção estrutural

MIDIA
└─ conteúdo e mídia da Landing

PARTICIPANTE
└─ própria equipe/robôs/inscrições/acompanhamento
```

A autorização será aplicada no backend por endpoint/operação; esconder tela no frontend não substitui segurança.

## Follow Line

Domínio:

```text
Registration
└─ Tomadas
   └─ Tentativas
```

Ranking:

```text
tentativas válidas + concluídas + com tempo
→ melhor tentativa de cada tomada
→ melhor tomada da inscrição/robô
→ ranking por menor tempo final
```

```text
tempo final = tempo bruto + penalidade
```

`checkpointsAlcancados` permanece como dado operacional enquanto o regulamento não definir impacto oficial.

## Sumô

Categorias como Mini/3 kg e RC/Autônomo usam o mesmo motor de Sumô e ficam isoladas por `competitionId + categoryId`.

Fluxo:

```text
Registration APROVADA
→ inspeção apta
→ Bracket
→ Match
→ RoundSumo
→ MatchResult automático
→ progressão
```

Regras atuais relevantes:

- rounds configuráveis;
- vitórias necessárias configuráveis;
- round adicional quando permitido;
- BYE automático;
- Suicídio/WO = vitória do adversário;
- penalidades A/B;
- **2 penalidades no mesmo round = derrota automática**;
- chave histórica somente leitura.

## Nova modalidade aprovada: Futebol de Robôs

Planejada para a próxima evolução.

Características recebidas:

- dois competidores;
- robôs fornecidos pela RAS;
- inscrição sem robô próprio;
- equipe permanece no desenho por enquanto;
- vencedor por confronto.

O schema atual **não suporta isso sem migration**, pois `Registration.robot` e `ParticipantRegistrationRequest.robotId` são obrigatórios. A solução não será criar robôs fake: a inscrição precisa aceitar legitimamente modalidade sem robô próprio.

## Mídia / CMS

O backend já possui uma abstração de object storage/R2:

```text
storage/ObjectStorageService.java
storage/R2ObjectStorageService.java
```

O fluxo atual de fotos de robôs continua usando storage local específico:

```text
RobotImageService
→ RobotImageStorageService
→ ./uploads/robots
```

A nova área MIDIA deverá usar uma arquitetura editorial própria, sugerida:

```text
MediaAsset
ContentSlot
ContentItem
```

para que a Landing deixe de depender de textos/imagens hardcoded.

## Regras

Nova frente aprovada para armazenar/publicar regras de:

- Follow Line;
- Sumô e textos específicos de subcategorias;
- Futebol de Robôs;
- ambiente/vestimenta/segurança.

Os textos finais dependem de confirmação do regulamento oficial.

## Ajustes Gerais DEV

Futura área de manutenção privilegiada. Não será um editor cru de tabelas: cada ação deve ter service próprio, validação e auditoria.

Exemplos:

```text
alterarRole
transferirCompetidor
transferirRobo
transferirResponsabilidadeDaEquipe
corrigirInscricao
ativar/desativar
```

## Achados principais da revisão estrutural

### Prioridade alta

1. `RegistrationService.reativar()` não revalida a janela de inscrições.
2. Cancelamento de inscrição pelo participante precisa política explícita depois de aprovação/chave/início.
3. Geração/regeneração de chave precisa estados da competição formalmente permitidos.
4. Edição futura de `MatchResult` depois de alimentar a próxima fase é perigosa e precisa ser bloqueada ou possuir rollback/reprocessamento.
5. Segurança `ORGANIZACAO` atual é ampla demais para DEV/GESTAO/MIDIA.

### Dívida técnica

- diretório rastreado `rascomp/bin/` contém artefatos antigos/compilados e deve ser removido em commit isolado;
- `.gitignore` já foi preparado para ignorar `bin/` daqui em diante;
- comentários antigos de `ConfigFollow` e `ConfigSumo` foram atualizados;
- avaliar remoção de `.classpath`, `.project` e `.gitkeep` desnecessários já rastreados.

## Migrations

```text
V1 — schema competitivo principal
V2 — inspeções de Sumô
V3 — rounds de Sumô
V4 — limpeza de artefatos legados de schema
V5 — usuários / ownership / fotos
V6 — histórico de chaves
V7 — motivos/penalidades de round
```

Mudanças futuras: **V8+**.

Nunca reescrever migration que já foi aplicada.

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

## Profile de demonstração/testes visuais

```powershell
$env:SPRING_PROFILES_ACTIVE="testdata"
.\mvnw spring-boot:run
```

O profile é opt-in e nunca deve ser ativado em produção.

## Fonte de verdade

O backend é responsável por:

- autorização real;
- ownership;
- elegibilidade;
- ranking;
- inspeção;
- BYE;
- vencedor;
- progressão;
- campeão;
- resultado oficial.

## Documentação

- `rascomp/docs/CONTINUIDADE.md` — checkpoint do backend e roadmap
- `rascomp/docs/FLUXO_DO_SISTEMA.md` — fluxo histórico de domínio
- `rascomp/docs/ENTIDADES_E_CRUDS.md` — entidades/CRUDs
- `rascomp/docs/CONGELAMENTO_API.md` — decisões de API
- `rascomp/docs/CLOUDFLARE_R2.md` — storage R2
- Dossiê canônico cross-repo: `Rascomp-FRONT/docs/DOSSIE_PROJETO_RASCOMP.md`

Swagger local:

```text
http://localhost:8080/swagger-ui/index.html
```

## Próxima ordem de trabalho

```text
0. estabilização dos achados da revisão
1. DEV | GESTAO | MIDIA | PARTICIPANTE
2. Ajustes Gerais DEV + auditoria
3. CMS/Mídia
4. Regras
5. Futebol de Robôs
6. participante completo
7. consolidação pública Landing/Galeria
```