# Continuidade — RasComp Backend

Última atualização: **06/10/2026**

Este arquivo registra o checkpoint funcional do backend. Não define roadmap próprio.

Fontes canônicas:

```text
gbsalermo/Rascomp-FRONT/docs/README.md
gbsalermo/Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md
gbsalermo/Rascomp-FRONT/docs/DOSSIE_PROJETO_RASCOMP.md
gbsalermo/Rascomp-FRONT/docs/CONTRATO_REGRAS_COMPETITIVAS.md
```

Ponteiro local do contrato:

```text
rascomp/docs/CONTRATO_REGRAS_COMPETITIVAS.md
```

---

# 1. Marco atual

```text
ETAPAS 0–4    ✅ concluídas / validadas
V1-BETA A     ✅ concluída / validada — Landing pública
V1-BETA B     🚧 Bloco 1 ✅ · Landing remota ✅ · base Docker/Container cloud em preparação
V1-BETA C     ⏳ acesso/inscrições reais
V1-BETA D     ⏳ smoke + estabilização
```

O roadmap oficial será retomado após a publicação/estabilização da V1 Beta.

Ajustes Gerais DEV avançados são pós-Beta e não bloqueiam o primeiro go-live.

Migrations atuais na branch V1-BETA B: **V1–V31**. Próxima migration estrutural: **V32+**.

---

# 2. Estado funcional conhecido

```text
AUTENTICAÇÃO / JWT                       ✅ + verificação de e-mail/reset na branch B
OWNERSHIP PARTICIPANTE                   ✅
MYSQL + FLYWAY V1–V30                    ✅ schema versionado
COMPETIÇÕES                              ✅ transições + prorrogação/reabertura
EQUIPES / COMPETIDORES / ROBÔS           ✅
LOGO PÚBLICA DA EQUIPE                   ✅ V28 + upload pelo líder + endpoint público
INSCRIÇÕES + REVISÃO                     ✅ invariantes + cancelamento + híbridos
FOTOS DOS ROBÔS                          ✅
FOLLOW LINE                              ✅ contrato RRC operacional
RANKING FOLLOW                           ✅
AUSÊNCIA DE TOMADA FOLLOW                ✅ auditável
SUMÔ / INSPEÇÃO / ROUNDS                 ✅ Bloco 3 alinhado
INSPEÇÃO HUMANA APTO/INAPTO              ✅
SUMÔ AUTONOMO / RC                       ✅
ROUNDS EXTRAS JUSTIFICADOS                ✅
FALHA DE INICIALIZAÇÃO                    ✅
JUÍZES DE COMPETIÇÃO                      ✅
DECISÃO DE JUIZ                           ✅ auditável
2 PENALIDADES = DERROTA DO ROUND         ✅
SUICÍDIO/WO                              ✅
CHAVES / BYE / PROGRESSÃO                ✅ Bloco 4 alinhado
AGENDA OPERACIONAL DE PARTIDAS           ✅ separada da árvore lógica
HISTÓRICO DE CHAVES                      ✅
API PARTICIPANTE                         ✅ equipe + robôs + inscrição normal PENDENTE
API PÚBLICA                              ✅
PROFILE TESTDATA                         ✅
```

Checkpoint automatizado final da V1-BETA A:

```text
Backend Tests #527
211 testes / 0 falhas / 0 erros / 0 skipped ✅
portal-testdata ✅
compilação da aplicação ✅
```

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

---

# 4. Migrations

```text
V1  — schema competitivo principal
V2  — inspeções de Sumô
V3  — rounds de Sumô
V4  — remoção de estrutura legada Follow/chaves
V5  — usuários / ownership / fotos
V6  — histórico de chaves
V7  — regras estendidas de round/penalidades
V8  — solicitações de cancelamento + histórico da janela de inscrições
V9  — classe física de Sumô nas categorias
V10 — alinhamento Follow 3×3 + parâmetros operacionais + ausência de tomada
V11 — modo de controle Sumô + rounds extras + auditoria de inspeção + juízes/decisão de juiz
V12 — separação da agenda operacional da estrutura lógica das partidas
V13 — migração da matriz de permissões ORGANIZACAO → DEV
```

Regra:

```text
V1–V13 nunca são reescritas
próxima mudança estrutural = V14+
```

A V10:

- normaliza `config_follow` para 3 tomadas × 3 tentativas;
- adiciona `penalidade_padrao_segundos` com default 10;
- adiciona `tempo_apresentacao_segundos` com default 60;
- cria `ausencias_tomada_seguidor_linha` com unicidade por `registration + tomada`;
- registra usuário da organização, observação e data/hora.

A V11 consolida a estrutura necessária ao Bloco 3 do Sumô, incluindo metadata de modo de controle, limite de rounds extras, auditoria da inspeção e entidades de juiz/decisão de juiz.

A V12 separa a agenda operacional da estrutura competitiva da partida, permitindo armazenar pista, ordem de execução e estado de convocação sem reescrever rodada, ordem lógica ou participantes da chave.

---

# 5. Segurança atual

```text
UserRole
├─ DEV
├─ GESTAO
├─ MIDIA
└─ PARTICIPANTE
```

```text
/api/v1/public/**       → público
/api/v1/participante/** → PARTICIPANTE
/api/v1/usuarios/**     → DEV
/api/v1/**              → DEV | GESTAO
```

`MIDIA` permanece autenticado sem herdar os namespaces competitivos atuais; os módulos editoriais entram nas etapas próprias de mídia.

Conta inativa já é rejeitada nas autenticações subsequentes pela validação JWT.

---

## 5.1 Política de criação de contas

Regra vigente:

```text
POST /api/v1/auth/register
→ sempre PARTICIPANTE

POST /api/v1/usuarios/internos?role=...
→ somente DEV
→ DEV | GESTAO | MIDIA
→ rejeita PARTICIPANTE
```

O DTO público de cadastro não define privilégios. O backend força `PARTICIPANTE` independentemente de campos extras enviados pelo cliente.

Uma pessoa pode possuir duas contas separadas, por exemplo:

```text
pessoal@...       → PARTICIPANTE
institucional@... → GESTAO
```

`UserAccount.email` continua único, portanto duas contas exigem e-mails distintos. Isso impede que a conta usada para competir herde automaticamente privilégios institucionais.

A ETAPA 3 permite editar apenas a permissão de contas internas entre `DEV | GESTAO | MIDIA`. `PARTICIPANTE` permanece identidade separada e não participa dessa conversão. O backend também impede alterar a própria role durante a sessão e protege o último DEV ativo contra rebaixamento ou desativação. Operações administrativas genéricas de identidade continuam fora deste escopo.

Checkpoint após a regra:

```text
Backend Tests #309
135 testes
0 falhas
0 erros
0 skipped
MySQL + Flyway V14 + testdata ✅
```

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

Robot
→ um único robô físico da equipe

CompetitionCategory.sumoPhysicalClass
→ classe física competitiva da categoria de Sumô

CompetitionCategory.sumoControlMode
→ AUTONOMO | RC
```

Não implementar transferências administrativas como simples troca genérica de FK.

---

# 7. Registration + Competition — Bloco 1 concluído

Estados atuais de inscrição:

```text
PENDENTE
APROVADA
REJEITADA
CANCELADA
DESISTENTE
DESCLASSIFICADA
```

Regras centrais já protegidas:

```text
PENDENTE
→ participante pode cancelar diretamente

APROVADA
→ participante solicita cancelamento
→ organização aprova/rejeita

REJEITADA
→ organização pode reabrir para correção

CANCELADA / REJEITADA
→ reabertura somente com competição/janela válidas
→ retorna PENDENTE

APROVADA sem atividade competitiva
→ cancelamento = CANCELADA

APROVADA com atividade competitiva
→ cancelamento = DESISTENTE
```

Atividade competitiva atualmente detectada para `CANCELADA/DESISTENTE`:

```text
tentativa Follow
ou
tomada Follow perdida por ausência
ou
inspeção de Sumô
ou
participação em Match
```

Uma ausência de Follow, portanto, já compromete histórico competitivo e não pode ser apagada por um cancelamento comum.

Reabertura da janela de inscrições também é bloqueada depois de:

- tentativa Follow;
- tomada Follow perdida por ausência;
- round de Sumô;
- resultado de partida;
- partida `EM_ANDAMENTO` ou `FINALIZADA`.

Robôs híbridos:

```text
mesmo Robot + mesma Competition
├─ Follow + Mini                         ✅
├─ Follow + 3 kg                         ✅
├─ Mini Auto + Mini R/C                  ✅
├─ 3 kg Auto + 3 kg R/C                  ✅
└─ Mini + 3 kg                           ❌
```

Pagamento ainda não existe e permanece reservado para evolução futura conforme contrato.

---

# 8. Follow Line — Bloco 2 concluído

## 8.1 Estrutura competitiva

A estrutura do RRC deixou de ser apenas configurável e passou a ser invariante do serviço:

```text
3 tomadas
×
3 tentativas por tomada
```

`ConfigFollowService` rejeita criação/edição com estrutura diferente de 3×3.

Continuam configuráveis:

```text
maxTempoSegundos
numeroCheckpoints
penalidadePadraoSegundos
tempoApresentacaoSegundos
```

Defaults operacionais:

```text
penalidadePadraoSegundos   = 10
tempoApresentacaoSegundos  = 60
```

O valor da penalidade é sugestão operacional configurável; não é consequência competitiva rígida embutida no ranking.

## 8.2 Estados de tentativa

Estados aceitos:

```text
CLASSIFICÁVEL
concluida=true
valida=true
tempoSegundos!=null

CONCLUÍDA INVALIDADA
concluida=true
valida=false
tempoSegundos!=null

NÃO CONCLUÍDA
concluida=false
valida=false
tempoSegundos=null
```

Combinações impossíveis são rejeitadas pelo backend, incluindo:

```text
concluida=false + valida=true       ❌
concluida=false + tempo!=null       ❌
concluida=true  + tempo=null        ❌
valida=true     + tempo=null        ❌
```

Tempo acima de `maxTempoSegundos` continua sendo persistido para auditoria, mas a tentativa é marcada como inválida.

## 8.3 Ranking

Permanece:

```text
tempoFinal = tempoSegundos + penalidadeSegundos
→ melhor tentativa classificável de cada tomada
→ melhor tomada da inscrição/robô
→ menor tempo final
```

`checkpointsAlcancados` continua informativo e **não altera o ranking**.

## 8.4 Tomada perdida por ausência

Nova entidade:

```text
AusenciaTomadaSeguidorLinha
├─ registration
├─ tomada
├─ observacao
├─ registradoPor
└─ dataCadastro
```

Regras:

- somente `DEV` ou `GESTAO` registra;
- inscrição precisa estar ativa, `APROVADA` e em `FOLLOW_LINE`;
- tomada deve existir no 1..3;
- não pode ser marcada depois de existir tentativa na mesma tomada;
- não pode haver duplicidade na mesma tomada;
- uma tomada ausente não aceita tentativa posterior;
- não são criadas três tentativas fictícias;
- ausência entra no histórico competitivo para desistência e reabertura de inscrições.

Endpoints:

```text
POST /api/v1/ausencias-tomada-seguidor-linha
GET  /api/v1/ausencias-tomada-seguidor-linha/por-inscricao
GET  /api/v1/ausencias-tomada-seguidor-linha/por-contexto
```

## 8.5 UX e operação

O frontend Gestão agora possui:

```text
cronômetro da tentativa
→ INICIAR
→ PARAR
→ preenche tempo
→ ajuste manual continua possível

cronômetro de apresentação
→ usa tempo configurado
→ somente após expirar permite marcar ausência

NÃO PAROU
→ aplica penalidade padrão configurada
→ adiciona observação

NÃO CONCLUIU
→ concluida=false
→ valida=false
→ sem tempo oficial
```

A fonte de verdade continua sendo o backend.

## 8.6 Initializers

Foram alinhados ao contrato 3×3 e aos estados válidos:

```text
FollowLineTestDataInitializer
DemoShowcaseDataInitializer
DataInitializer / PostmanScenarioInitializer via defaults de ConfigFollow
```

Tentativas interrompidas nos cenários de teste passaram a usar:

```text
tempo=null
concluida=false
valida=false
```

---

# 9. Sumô — Bloco 3 concluído

Categorias previstas e suportadas pelo mesmo motor:

```text
Mini 500 g Auto
Mini 500 g R/C
3 kg Auto
3 kg R/C
```

Todas usam `Modalidade.SUMO`, isoladas por categoria.

## 9.1 Inspeção

`InspecaoSumoService` recebe explicitamente a decisão humana:

```text
aprovada=true  → APTO
aprovada=false → INAPTO
```

`pesoMedido` é opcional e informativo. Estar acima ou abaixo de `pesoMax` não substitui a decisão da organização.

O registro também preserva responsável e data/hora para auditoria.

## 9.2 Modo de controle

```text
SumoControlMode
├─ AUTONOMO
└─ RC
```

Categorias `SUMO` exigem modo de controle configurado. Categorias não-Sumô não usam essa metadata.

A regra regulamentar de 5 s do autônomo é orientação operacional; o backend não transforma o atraso/falha em resultado automático.

## 9.3 Rounds e motivos

Regras preservadas:

```text
3 rounds regulares
2 vitórias necessárias
0 penalidades → normal
1 penalidade  → normal
2 penalidades → derrota automática do round
SUICIDIO_WO  → adversário vence
BYE          → avanço automático
```

Motivos explícitos suportados:

```text
DISPUTA
SUICIDIO_WO
PENALIDADES
FALHA_INICIALIZACAO
DECISAO_JUIZ
```

`FALHA_INICIALIZACAO` exige justificativa. A consequência é informada pela operação humana; o sistema não escolhe automaticamente entre penalidade ou perda do round.

## 9.4 Rounds extras

`ConfigSumo.maxRoundsExtras` limita os extras.

Um round extra só é aceito se:

- não houver vencedor;
- os rounds regulares já tiverem sido consumidos;
- a categoria permitir desempate;
- o limite de extras não tiver sido alcançado;
- existir justificativa.

## 9.5 Juízes e decisão final

Modelo:

```text
CompetitionJudge
→ pertence à Competition
→ nome obrigatório
→ UserAccount opcional
→ ativo/inativo

MatchJudgeDecision
→ Match
→ vencedor
→ juiz
→ justificativa
→ data/hora
```

A decisão de juiz só é aceita depois de esgotar rounds regulares + extras disponíveis, sem vencedor pelos rounds.

A operação valida que:

- a partida/chave estão ativas e atuais;
- os dois participantes existem;
- o vencedor participa da partida;
- o juiz está ativo e pertence à mesma competição;
- não existe decisão anterior;
- justificativa é obrigatória.

A decisão cria o resultado oficial e alimenta a progressão pelo fluxo normal de resultado.

## 9.6 Initializers

Foram alinhados ao novo contrato:

```text
DataInitializer
DemoOitavasDataInitializer
BracketHistoryTestDataInitializer
DemoShowcaseDataInitializer
```

Categorias Sumô de demonstração possuem modo de controle e inspeções aprovadas enviam explicitamente a decisão humana.

---

# 10. Chaves, agenda e progressão — Bloco 4 concluído

## 10.1 Geração e regeneração

Fluxo comum permitido:

```text
Competition.status == INSCRICOES_ENCERRADAS ✅
demais estados                              ❌
```

`BracketIntegrityService` centraliza a proteção.

Regeneração continua possível enquanto a chave estiver apenas montada. Um BYE automático, isoladamente, **não conta como atividade competitiva real**.

Regeneração comum é bloqueada depois de existir qualquer um dos seguintes sinais:

```text
RoundSumo
MatchResult
Match EM_ANDAMENTO
Match FINALIZADA com os dois participantes reais
```

## 10.2 Estrutura lógica x agenda operacional

A árvore competitiva e a agenda são conceitos separados:

```text
estrutura lógica
→ rodada
→ ordem lógica
→ registrationA / registrationB
→ próxima partida

agenda operacional
→ dataHora
→ pista
→ ordemExecucao
→ statusConvocacao
```

Depois que a chave foi gerada, o fluxo comum não pode reescrever rodada, ordem lógica ou participantes por uma edição genérica de partida.

A agenda possui operação específica:

```text
PATCH /api/v1/partidas/{id}/agenda
```

Isso permite reorganizar horários, pistas, chamadas e execução simultânea sem alterar quem enfrenta quem.

## 10.3 Progressão e estado da chave

BYE continua avançando automaticamente.

Quando uma disputa real começa, a chave passa para `EM_ANDAMENTO`.

A progressão continua preenchendo somente o slot esperado da partida seguinte e preserva a árvore lógica.

## 10.4 Correção de resultado propagado

Política implementada:

```text
resultado anterior corrigido
+
próxima partida ainda sem atividade
→ remover/substituir vencedor propagado com segurança
→ manter restante da árvore

próxima partida já possui round, resultado ou começou/finalizou
→ bloquear correção comum
→ não reescrever histórico competitivo silenciosamente
```

Rollback competitivo excepcional, caso venha a existir, pertence a ferramentas administrativas futuras e deverá ser explícito/auditável.

## 10.5 Testdata

O cenário de demonstração foi corrigido sem bypass de regra de produção:

```text
1. Competition de demonstração entra no seed como INSCRICOES_ENCERRADAS
2. participantes e inspeções são montados
3. chave é gerada no estado permitido
4. disputas/histórico são preparados
5. estado final do cenário é restaurado para EM_ANDAMENTO ou FINALIZADA
```

O Mini Sumô ao vivo agora monta os 16 participantes antes da primeira geração, evitando regeneração de uma chave já disputada.

---

# 11. ETAPA 1 — estado dos blocos

```text
Bloco 1 — Competition + Registration       ✅ CONCLUÍDO
Bloco 2 — Follow Line                       ✅ CONCLUÍDO
Bloco 3 — Sumô                              ✅ CONCLUÍDO
Bloco 4 — Chaves e progressão               ✅ CONCLUÍDO
Bloco 5 — Testes integrados de competição   ✅ CONCLUÍDO
```

Bloco 4 concluído com:

```text
✅ geração restrita a INSCRICOES_ENCERRADAS
✅ regeneração protegida contra atividade competitiva
✅ BYE diferenciado de disputa real
✅ estrutura lógica protegida
✅ agenda operacional separada
✅ progressão protegida
✅ correção segura antes da dependência
✅ bloqueio depois da dependência iniciada
✅ testdata alinhado sem bypass
✅ frontend Gestão integrado
✅ 98 testes verdes
✅ MySQL/Flyway V12/testdata verdes
✅ frontend typecheck/build verdes
```

---

# 12. Estratégia de testes da ETAPA 1

O checkpoint atual possui **111 testes** no backend e smoke do profile `testdata` contra MySQL/Flyway V12.

A cobertura atual inclui regras de Competition/Registration, Follow, Sumô e, no Bloco 4, integridade de geração/regeneração, progressão, proteção da agenda e correção segura de dependências.

O profile completo valida também que os initializers continuam inicializando contra MySQL real sob as mesmas invariantes usadas em produção.

A camada integrada de competição completa foi adicionada no Bloco 5 e cobre:

```text
CompetitionLifecycleFlow
RegistrationFlow
FollowCompetitionFlow
SumoCompetitionFlow
CompetitionIntegrityFlow
```

Cada operação inválida deve comprovar:

```text
erro esperado
+
estado anterior preservado
+
nenhuma persistência parcial
```

---

# 13. ETAPA 11 — Avisos + Telegram

Planejamento futuro permanece separado das etapas já concluídas:

```text
GESTAO/DEV
→ publica aviso por Competition
→ backend persiste IN_APP
→ Telegram distribui a mesma comunicação se habilitado
```

IN_APP continua sendo fonte de verdade; falha externa não invalida o aviso.

---

# 14. ETAPA 2 — limpeza técnica

Checkpoint inicial de 13/09/2026:

```text
rascomp/bin/                                ✅ removido em commit isolado 775df79
.classpath / .project                       ✅ removidos
.gitkeep desnecessários em packages         ✅ removidos
TODO: / FIXME reais                         ✅ nenhum encontrado na varredura inicial
artefatos compilados/target versionados     ✅ nenhum remanescente
código morto/duplicado                      🔎 revisão incremental, sem remoção especulativa
```

Backend Tests #297 ficou verde após a limpeza estrutural. Nenhuma regra de negócio, migration ou package Java foi reorganizado sem ganho comprovado.

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

---

# 16. Próximo passo / handoff

Estado atual:

```text
ETAPA 1 — lógica/integridade             ✅ CONCLUÍDA
ETAPA 2 — limpeza/organização            ✅ CONCLUÍDA
ETAPA 3 — matriz de permissões
├─ backend                               ✅
├─ frontend                              ✅
├─ testes automatizados                  ✅
├─ MySQL/Flyway V13 + testdata           ✅
└─ checkpoint prático dos quatro perfis  ✅ validado em 19/09/2026
```

A ETAPA 4 está em andamento. BLOCO 1 e BLOCO 2 estão concluídos e validados; BLOCO 3 — Operação competitiva é o próximo e ainda não foi iniciado.

## Checkpoint cross-repo da ETAPA 2 — 13/09/2026

A implementação técnica cross-repo da ETAPA 2 foi concluída sem alterar regras competitivas:

- backend limpo e Backend Tests #297 verde;
- frontend modularizado por domínio;
- `api.ts` e `types.ts` reduzidos a fachadas;
- CSS administrativo consolidado preservando cascata;
- Frontend Checks #57–#62 verdes;
- sem TODO/FIXME reais, backups ou artefatos gerados versionados.

A ETAPA 2 está **concluída/validada**. Smoke visual e testes práticos não pertencem ao critério desta etapa.

## Checkpoint integrado da ETAPA 3 — 13/09/2026

```text
DEV           → operação competitiva + usuários + sistema
GESTAO        → operação competitiva
MIDIA         → autenticado, sem herdar operação competitiva
PARTICIPANTE  → namespace próprio do portal
```

Compatibilidade:

- V13 migra `ORGANIZACAO → DEV`;
- variáveis `RASCOMP_ORG_*` permanecem como fallback temporário do bootstrap;
- nenhuma regra competitiva da ETAPA 1 foi alterada.

Validação:

- `UserRoleTest` ✅
- `SecurityAuthorizationFlowTest` ✅
- `DemoShowcaseDataInitializerTest` ✅
- 135 testes / 0 falhas / 0 erros / 0 skipped ✅
- MySQL + Flyway V13 + `testdata` ✅
- Frontend Checks #64–#66 ✅
- CI valida presença e login real dos quatro perfis `testdata`.

## Usuários locais para verificação prática da ETAPA 3

Disponíveis somente quando o profile `testdata`/cenário de demonstração está habilitado:

```text
DEV
organizacao.demo@rascomp.local
Rascomp@2026

GESTAO
gestao.demo@rascomp.local
Rascomp@2026

MIDIA
midia.demo@rascomp.local
Rascomp@2026

PARTICIPANTE — líder
lider.demo@rascomp.local
Rascomp@2026

PARTICIPANTE — membro comum
membro.demo@rascomp.local
Rascomp@2026
```

O participante `lider.demo` possui equipe, robôs e inscrições no cenário para validar o portal. As contas DEV/GESTAO/MIDIA servem para comparar restrições de navegação e autorização. Nunca habilitar `testdata` em produção.

Validação automatizada da matriz:

- `UserRoleTest` → capacidades semânticas;
- `SecurityAuthorizationFlowTest` → autorização HTTP real;
- `DemoShowcaseDataInitializerTest` → criação dos quatro perfis;
- profile `testdata` → inicialização real contra MySQL + Flyway V13.


## Correção de compatibilidade do `testdata` — banco local reaproveitado

O `DemoOitavasDataInitializer` passou a preservar uma chave de demonstração já existente quando a competição local já está em `EM_ANDAMENTO`.

Motivo: bancos locais reaproveitados de versões anteriores do seed podem conter uma chave com estrutura antiga. O initializer não deve contornar a regra real de domínio que só permite geração/regeneração em `INSCRICOES_ENCERRADAS`.

Comportamento atual:

```text
chave demo esperada encontrada
→ segue preparação normal

chave demo esperada ausente
+ competição INSCRICOES_ENCERRADAS
→ pode gerar

chave demo esperada ausente
+ competição EM_ANDAMENTO/FINALIZADA/etc.
→ preserva estado local
→ não tenta regenerar
→ aplicação continua subindo
```

A regra de `BracketIntegrityService` não foi afrouxada.

Validação adicionada em `DemoOitavasDataInitializerTest`.
Checkpoint da suíte: 126 testes, 0 falhas, 0 erros, 0 skipped.


## Portal participante — liderança e participação

A autorização distingue responsabilidade da equipe de participação competitiva:

```text
Team.responsibleUser
→ líder
→ acesso integral ao contexto da equipe no portal

Competitor.userAccount
→ membro
→ acesso à equipe
→ somente Registration que contém esse Competitor
→ somente Robot dessas Registration
```

Leituras permitidas ao membro:
- equipe à qual seu Competitor pertence;
- roster da equipe;
- próprias inscrições;
- robôs das próprias inscrições;
- fotos desses robôs;
- tentativas/configuração Follow das próprias inscrições.

Escritas administrativas continuam reservadas ao responsável da equipe, incluindo edição de equipe, competidores, robôs, fotos e ações de inscrição.

Testes:
- `AccessPolicyServiceTest` cobre acesso do membro à própria equipe;
- `ParticipantPortalServiceTest` cobre visão integral do líder e filtro do membro;
- Backend Tests #315: 135 testes verdes;
- MySQL + Flyway V13 + profile `testdata` verdes.

Conta prática adicional:

```text
membro.demo@rascomp.local / Rascomp@2026
```

Ela pertence à mesma Equipe Demo RAS do líder, mas está associada somente à inscrição do Chronos Demo.


## Fechamento da matriz de permissões — 19/09/2026

A ETAPA 3 foi validada pelo usuário e encerrada.

Estado preservado:

```text
DEV          ✅
GESTAO       ✅
MIDIA        ✅
PARTICIPANTE ✅
líder x membro comum ✅
```

A validação final de permissões foi incorporada à **ETAPA 15 — Validação final completa**, imediatamente antes do deploy da ETAPA 16.


## Roadmap reorganizado — 19/09/2026

O backend acompanha o novo planejamento cross-repo:

- ETAPA 4 — consolidação funcional e polimento do MVP;
- ETAPA 5 — Ajustes Gerais DEV + auditoria;
- ETAPA 6 — Futebol de Robôs;
- ETAPA 7 — Portal do Participante completo;
- ETAPA 8 — CMS/Mídia;
- ETAPA 9 — Landing/Galeria;
- ETAPA 10 — fechamento do MVP;
- ETAPA 11 — Avisos IN_APP + Telegram;
- ETAPA 12 — portabilidade institucional;
- ETAPA 13 — Regras, Ajuda e Segurança;
- ETAPA 14 — hardening + testes físicos mobile;
- ETAPA 15 — validação final completa + permissões;
- ETAPA 16 — deploy.

A ordem detalhada continua canônica no frontend.


## Checkpoint transversal — Otimização Mobile do MVP

A otimização mobile não redefine a ETAPA 4. Ela é um checkpoint transversal da PRIORIDADE 1, acompanhado ao longo das etapas que alteram interfaces.

Estado conhecido:

```text
Login                         ✅ responsividade dedicada já revisada
Gestão autenticada             ⏳ otimização mobile pendente
Portal do Participante         ⏳ otimização mobile pendente
Tabelas/filtros/diálogos       ⏳ otimização mobile pendente
```

Fluxo:

```text
ETAPA 4 → corrigir problemas mobile encontrados no sistema atual
ETAPA 7 → Portal do Participante responsivo
ETAPA 8 → CMS/Mídia considera telas menores
ETAPA 9 → Landing/Galeria responsivas
CHECKPOINT MOBILE → consolidar antes da ETAPA 10
ETAPA 14 → testes físicos finais em aparelhos reais
```

O backend deve permanecer estável durante esse trabalho, alterando contratos apenas se algum fluxo mobile revelar necessidade funcional real.


## Checkpoint de início da ETAPA 4 — 22/09/2026

Branch de trabalho:

```text
etapa-4-consolidacao-mvp
```

Estado:

- ETAPA 4 autorizada e iniciada;
- BLOCO 1 em andamento;
- baseline técnico, autenticação, Shell e UX global são o escopo atual;
- ETAPA 5 permanece bloqueada;
- nenhuma nova funcionalidade estrutural deve ser antecipada.


## ETAPA 4 — BLOCO 1 — baseline 22/09/2026

Branch: `etapa-4-consolidacao-mvp`.

Checkpoint automatizado real:

```text
Backend Tests #325
142 testes
0 falhas
0 erros
0 skipped
MySQL + Flyway V14 + testdata ✅
Logins reais dos perfis testdata ✅
```

Foram adicionados testes de autenticação para:

- normalização de e-mail no login;
- persistência solicitada por `lembrarDeMim`;
- validade maior do JWT quando `lembrarDeMim=true`;
- invalidação do token quando o usuário é desativado.

Nenhuma migration nova foi necessária. V1–V13 permanecem imutáveis; a V14 foi criada na ETAPA 4 para controle de sessão única. Próxima migration estrutural: V15+.


## Decisão — recuperação e redefinição de senha

O backend ainda não possui endpoint definitivo de recuperação de senha. A ETAPA 4 não deve introduzir uma solução parcial ou insegura.

A implementação foi planejada para a **ETAPA 13 — Regras, Ajuda e Segurança**, incluindo solicitação não enumerável, token/código de uso único e expiração curta, invalidação segura, redefinição de senha, proteção contra abuso, política de sessão pós-reset, canal configurável de entrega e testes dos casos de token inválido/expirado/reutilizado e conta inativa.

A ETAPA 14 fará a revisão de hardening e a ETAPA 15 repetirá os cenários na validação final.


## ETAPA 4 — BLOCO 1 — política de sessão única

Durante a validação prática foi identificado que a mesma conta permanecia autenticada simultaneamente em aparelhos diferentes.

Decisão aplicada no BLOCO 1:

```text
uma conta → uma sessão ativa
novo login → invalida token anterior
logout → invalida a sessão ativa no servidor
```

Implementação:

- V14 adiciona `user_accounts.session_version`;
- cada novo login incrementa `session_version`;
- o JWT carrega a versão da sessão;
- o filtro só aceita token cuja versão coincida com a versão atual da conta;
- logout incrementa novamente a versão;
- novo login em outro aparelho faz o aparelho anterior receber 401 na próxima requisição;
- frontend já trata 401 limpando estado e retornando ao login.

V1–V13 permanecem imutáveis.


### Recuperação assistida pelo DEV

A ETAPA 13 deverá implementar recuperação de senha com dois caminhos complementares.

Fluxo preferencial:

- usuário solicita recuperação diretamente;
- backend emite token/código de uso único e curta duração;
- entrega por canal configurável, preferencialmente e-mail;
- usuário define a nova senha sem intervenção humana.

Fallback assistido:

- usuário informa perda de acesso à organização;
- DEV recebe/abre a ação administrativa após verificar a identidade por procedimento definido;
- sistema emite ou registra uma credencial temporária;
- credencial possui expiração curta e uso único;
- primeiro login com credencial temporária força cadastro + confirmação de nova senha;
- senha definitiva é conhecida somente pelo usuário;
- emissão da credencial temporária é auditada;
- redefinição invalida sessões anteriores conforme política de segurança.

Não permitir reset silencioso para uma senha permanente conhecida pelo DEV.


## Fechamento do BLOCO 1 da ETAPA 4 — 22/09/2026

BLOCO 1 concluído e validado pelo usuário.

Checkpoint final:

```text
Backend Tests #329
142 testes / 0 falhas / 0 erros / 0 skipped
MySQL + Flyway V14 + testdata ✅
Frontend Checks #97 ✅
```

Consolidações backend do bloco:

- autenticação revalidada;
- sessão única por conta;
- V14 adiciona `user_accounts.session_version`;
- novo login invalida sessão anterior;
- logout invalida sessão no servidor;
- recuperação definitiva de senha permanece na ETAPA 13;
- fluxo assistido por DEV será fallback auditável com credencial temporária e troca obrigatória.

Próximo: BLOCO 2 — Gestão administrativa.


## ETAPA 4 — Agenda competitiva e gestão de competidores

Achados do BLOCO 2:

### Agenda

O backend atual possui agenda operacional para partidas de Sumô via `Match`:

```text
dataHora
pista
ordemExecucao
statusConvocacao
```

Não existe equivalente de agendamento para uma tomada de Follow Line.

A agenda futura deve ser multimodal e será modelada no BLOCO 3C:

- Sumô → partidas/batalhas;
- Follow Line → tomadas de tempo por categoria;
- Dashboard deve consumir visão unificada, não usar Match como sinônimo de agenda.

### Competidores

O backend já possui `CompetitorController` e `CompetitorService` com:

- listar todos/ativos;
- buscar por id/e-mail;
- listar por equipe;
- criar/atualizar;
- desativar/reativar.

A lacuna é principalmente no frontend administrativo e foi alocada no BLOCO 2.4.

Relacionamento atual:

```text
Competitor → Team
Robot      → Team
Registration → Robot + Competitor(s)
```

Portanto, detalhes de "robôs do competidor" devem ser derivados das Registration em que ele participa, e não por vínculo direto Competitor → Robot.


### Agenda Follow — chamada geral da tomada

Contrato funcional aprovado para implementação futura no BLOCO 3C:

```text
Follow:
Category + tomada + dataHora + pista + ordem + estado da chamada
→ contém/coordena convocações individuais das Registration da categoria
→ ausência de uma inscrição na sua vez registra AusenciaTomadaSeguidorLinha

Sumô:
Match + dataHora + pista/dohyo + ordemExecucao + statusConvocacao
→ rounds permanecem internos ao Match
```

A tela unificada de Agenda será a visão organizacional principal; Follow e Sumô manterão edição contextual de suas próprias atividades.


## ETAPA 4 — BLOCO 2.2 — contexto da competição

Foi introduzido `CompetitionContextService` para centralizar a edição operável.

Política:

```text
DEV    → qualquer edição
GESTAO → somente competição vigente
```

A vigente não é mais inferida por status. O DEV a define explicitamente e o backend persiste essa escolha.

Proteções aplicadas:

- listagem de competições filtrada para GESTAO;
- busca por id fora da vigente bloqueada para GESTAO;
- criação, PUT estrutural, desativação e reativação DEV-only;
- transição de status separada em endpoint explícito;
- prorrogação/reabertura e histórico de janela passam pelo contexto operável.

A identificação de DEV/GESTAO no `CompetitionContextService` usa authorities do Spring Security, compatível com JWT real e testes de segurança.

Cobertura adicionada:
- DEV vê todas as edições;
- GESTAO vê somente vigente;
- GESTAO não opera histórica;
- DEV pode operar histórica;
- transição explícita de status;
- GESTAO não cria nova edição.


### Checkpoint técnico — BLOCO 2.2

```text
Backend Tests #342 ✅
149 testes / 0 falhas / 0 erros / 0 skipped
MySQL + Flyway V14 + testdata ✅
Frontend Checks #114 ✅
```

A regra de competição vigente está pronta para validação prática com DEV e GESTAO.


### Ajuste pós-validação — V15

A validação prática da 2.2 mostrou que a competição vigente precisa ser uma escolha explícita do DEV.

V15 adiciona:

```text
competitions.vigente BOOLEAN NOT NULL DEFAULT FALSE
```

Regras:

- apenas DEV define a competição vigente;
- a troca limpa a flag anterior e marca a nova edição;
- GESTAO lista e opera somente a edição marcada;
- status não troca a vigente automaticamente;
- finalização oficial (`FINALIZADA`) é DEV-only;
- GESTAO pode operar transições anteriores permitidas;
- criação de competição não altera a vigente automaticamente.

Próxima migration estrutural: V16+.


### Semântica de contexto — foco DEV x vigente global

O backend persiste somente o conceito global de **competição vigente**.

O conceito de **competição em foco** é local ao frontend do DEV e não é uma decisão de domínio.

```text
DEV troca foco local     → nenhum efeito global
DEV define vigente       → atualiza competitions.vigente
GESTAO                    → opera somente competitions.vigente = true
```

Permissões do ciclo:

- criar competição: DEV;
- abrir inscrições: DEV | GESTAO;
- encerrar inscrições: DEV | GESTAO;
- iniciar competição: DEV | GESTAO;
- finalizar competição: DEV;
- definir competição vigente: DEV.


## ETAPA 4 — BLOCO 2 — implementação administrativa concluída

O escopo implementado está pronto para validação manual, mas o bloco ainda não está encerrado.

### Identidades / usuários

- `PUT /api/v1/usuarios/{id}` permite DEV editar nome, e-mail e telefone;
- PARTICIPANTE permanece PARTICIPANTE;
- e-mail duplicado é bloqueado;
- alterar e-mail incrementa `session_version`;
- desativar conta incrementa `session_version`;
- conta autenticada não pode se desativar;
- último DEV ativo continua protegido.

### Catálogo administrativo contextual

Foi criado `CompetitionAdminCatalogService` e:

```text
GET /api/v1/competicoes/{id}/catalogo-administrativo
GET /api/v1/competicoes/{id}/robos/{robotId}/fotos
```

O catálogo deriva Teams, Competitors e Robots das Registration da edição.

Regras:

- DEV pode consultar catálogos globais;
- GESTAO usa apenas a vigente;
- GETs administrativos globais de Team/Robot/Competitor são DEV-only;
- POST/PUT/DELETE/reactivação administrativa de Team/Robot/Competitor são DEV-only;
- mutações de CompetitionCategory e RobotImage no namespace administrativo são DEV-only;
- Portal do Participante mantém endpoints próprios para ações autorizadas do líder.

### Inscrições

`RegistrationService` passou a exigir `CompetitionContextService` nas operações administrativas:

- criar administrativamente;
- buscar por id;
- listar por competição;
- atualizar/revisar;
- cancelar;
- reativar.

Listagens globais e por status são DEV-only.

`RegistrationCancellationRequestService` também restringe GESTAO à competição vigente para listar, aprovar e rejeitar solicitações.

### Checkpoint

```text
Backend Tests #371 ✅
155 testes / 0 falhas / 0 erros / 0 skipped
MySQL + Flyway V15 + testdata ✅
Frontend Checks #137 ✅
```

V1–V15 permanecem imutáveis. Próxima migration estrutural: V16+.

### Decisões de produto encerradas

- `CompetitionCategory` permanece global;
- UserAccount PARTICIPANTE sincroniza identidade e ativo/inativo com Competitor vinculado;
- Team/Robot/Registration não sofrem cascata automática;
- catálogo de categorias permanece DEV-only.

BLOCO 2 validado e encerrado. BLOCO 3 é o próximo checkpoint e ainda não foi iniciado.


## ETAPA 4 — BLOCO 2 — correções finais pós-validação

Correções backend:

- `CompetitionAdminCatalogService.buscar()` agora é `@Transactional(readOnly = true)`, evitando LazyInitializationException na montagem de TeamDTO/RobotDTO/CompetitorDTO;
- V16 adiciona `registrations.review_reason VARCHAR(500)`;
- REJEITADA exige motivo;
- reativação administrativa de CANCELADA/REJEITADA exige status INSCRICOES_ABERTAS, mas não repete a restrição de datas históricas;
- reativação pelo participante continua exigindo a janela temporal válida;
- rejeição de solicitação de cancelamento exige resposta;
- UserAccount PARTICIPANTE sincroniza nome/e-mail/telefone/ativo com Competitor vinculado;
- Competitor ligado a conta PARTICIPANTE não pode ser ativado/desativado diretamente pelo CompetitorService;
- reativação da conta é bloqueada se Team/Institution estiver inativa;
- UserAccountDTO expõe metadados do vínculo para permitir aviso de equipe sem competidores ativos;
- teste integrado `CompetitionAdminCatalogFlowTest` cobre o catálogo com relações LAZY reais.

Decisões consolidadas:

- CompetitionCategory permanece global;
- nenhuma associação Competition ↔ Category será criada no BLOCO 2;
- catálogo de categorias é DEV-only;
- desativar PARTICIPANTE também desativa Competitor, mas não Team/Robot/Registration;
- ausência de competidores ativos gera alerta administrativo; não existe cascata destrutiva automática.

DESCLASSIFICADA:
- Sumô já aplica automaticamente após esgotar tentativas de inspeção sem aprovação;
- demais regras/manualização ficam no BLOCO 3.

V1–V18 imutáveis. Próxima migration estrutural: V19+.


### Checkpoint pós-correções

```text
Backend Tests #388 ✅
161 testes / 0 falhas / 0 erros / 0 skipped
MySQL + Flyway V16 + testdata ✅
Frontend Checks #149 ✅
```

Próxima migration estrutural: V18+.


## ETAPA 4 — BLOCO 2 — auditoria final V17

V17 cria a tabela `registration_status_history`.

Cada transição auditada registra:

- Registration;
- status anterior;
- novo status;
- tipo da mudança;
- UserAccount responsável quando disponível;
- motivo;
- data/hora.

Tipos atuais:

```text
CRIACAO
APROVACAO
REJEICAO
CANCELAMENTO
DESISTENCIA
REATIVACAO
DESCLASSIFICACAO
```

Integrações:

- RegistrationService registra criação, revisão, cancelamento/desistência e reativação;
- InspecaoSumoService registra a desclassificação automática por esgotamento das tentativas de inspeção;
- solicitações de cancelamento aprovadas propagam o motivo original do participante;
- `GET /api/v1/inscricoes/{id}/historico-status` respeita CompetitionContextService;
- não há backfill fictício das transições anteriores à V17.

Checkpoint:

```text
Backend Tests #414 ✅
161 testes / 0 falhas / 0 erros / 0 skipped
MySQL + Flyway V17 + testdata ✅
Frontend Checks #164 ✅
```

V1–V18 imutáveis. Próxima migration estrutural: V19+.


## Fechamento formal do BLOCO 2 — 23/09/2026

Validação manual final aprovada pelo usuário.

Resultado:

```text
BLOCO 2 — Gestão administrativa
✅ CONCLUÍDO
✅ VALIDADO
```

Backend consolidado:

- competição vigente persistida;
- contexto DEV/GESTAO protegido no backend;
- usuários/participantes com regras de identidade consolidadas;
- catálogos administrativos contextualizados;
- inscrições, cancelamentos, reativações e auditoria V17;
- UserAccount PARTICIPANTE sincronizado com Competitor vinculado;
- categorias globais DEV-only;
- histórico de status e solicitações de cancelamento preservados.

Checkpoint final:

```text
Backend Tests #415 ✅
161 testes / 0 falhas / 0 erros / 0 skipped
MySQL + Flyway V17 + testdata ✅
Frontend Checks #170 ✅
```

V1–V18 permanecem imutáveis. Próxima migration estrutural: V19+.

Próximo: BLOCO 3 — Operação competitiva, ainda não iniciado.


## ETAPA 4 — BLOCO 3 / 3A Follow Line

Primeiro checkpoint do BLOCO 3.

Hardening aplicado ao Follow:

- `TentativaSeguidorLinhaService` exige CompetitionContextService em criação, leitura, atualização e exclusão;
- listagem por contexto valida competitionId;
- atualização de tentativa não pode trocar a Registration original;
- `AusenciaTomadaSeguidorLinhaService` exige CompetitionContextService para registrar/listar ausência;
- `RankingFollowController` valida CompetitionContextService no endpoint administrativo;
- `RankingFollowService` permanece reutilizável pelo endpoint público, portanto a restrição administrativa não foi colocada dentro do service;
- fluxo integrado comprova GESTAO bloqueada fora da competição vigente e DEV livre para operar outra edição permitida.

Checkpoint:

```text
Backend Tests #429 ✅
162 testes / 0 falhas / 0 erros / 0 skipped
MySQL + Flyway V17 + testdata ✅
Frontend Checks #177 ✅
```

Nenhuma migration nova foi necessária. V1–V17 permanecem imutáveis; próxima migration estrutural V18+.

Agenda Follow continua na 3C.


## ETAPA 4 — BLOCO 3 implementado

As frentes Follow, Sumô e Chaves/Agenda/Resultados estão implementadas e aguardam validação manual final.

### Agenda competitiva V18

V18 cria:

```text
follow_take_schedules
follow_take_schedule_entries
```

Contrato:

```text
FOLLOW_LINE
Competition + Category + Tomada
→ chamada geral
→ data/hora
→ pista
→ ordem
→ status
→ fila de Registration

SUMO
Match
→ dataHora
→ pista
→ ordemExecucao
→ statusConvocacao
```

`CompetitionAgendaService` unifica os dois tipos.

Regras protegidas:

- CompetitionContextService em toda operação administrativa do BLOCO 3;
- chamada Follow única por competição/categoria/tomada;
- fila criada/sincronizada a partir de inscrições APROVADAS e ativas;
- registro histórico de fila não é apagado quando inscrição fica indisponível;
- inscrição indisponível não pode receber nova convocação;
- CONCLUIDA/AUSENTE/EM_EXECUCAO não podem ser forjados pelo endpoint genérico de convocação;
- conclusão e ausência vêm do domínio competitivo;
- chamada FINALIZADA/CANCELADA é somente leitura;
- tentativa e ausência sincronizam automaticamente a fila;
- partida futura AGUARDANDO_PARTICIPANTES não aparece como batalha real na Agenda;
- Match EM_ANDAMENTO/FINALIZADA prevalece sobre status de convocação na Agenda.

### Resultados

- Follow: vencedor somente quando todas as tomadas dos participantes ativos/aprovados estiverem encerradas por tentativas ou ausência;
- ranking parcial não é apresentado como campeão;
- Sumô: vencedor da final da chave atual;
- desclassificação/desistência podem resolver administrativamente uma partida quando exatamente um lado está indisponível;
- resolução administrativa preserva a árvore e não cria round fictício.

### Desclassificação

- automática ao esgotar inspeções quando aplicável;
- manual por DEV/GESTAO com motivo obrigatório;
- auditada em `registration_status_history`;
- inscrição permanece no histórico;
- partidas comprometidas usam resolução administrativa específica.

### Checkpoint

```text
Backend Tests #493 ✅
166 testes / 0 falhas / 0 erros / 0 skipped
MySQL + Flyway V18 + testdata ✅
Frontend Checks #213 ✅
```

V1–V18 imutáveis. Próxima migration estrutural: V19+.

Decisões ainda abertas para o fechamento manual:

1. restringir ou não tentativa/ausência Follow exclusivamente a Competition EM_ANDAMENTO;
2. ✅ RESOLVIDO — categoria Follow sem tentativa classificável oferece Tomada Extra ou decisão administrativa auditada.

BLOCO 3 não deve ser marcado como validado antes do reteste manual.


## ETAPA 4 — BLOCO 3 — resolução Follow sem classificação

Regra fechada em 24/09/2026.

Quando o programa normal do Follow termina sem qualquer tentativa válida/classificável:

- nenhum vencedor é inferido automaticamente;
- a organização pode criar uma Tomada Extra;
- ou pode registrar decisão administrativa do vencedor.

### Tomada Extra

- número excepcional = `ConfigFollow.numeroTomadas + 1`;
- não altera `ConfigFollow.numeroTomadas`;
- é criada explicitamente em `POST /api/v1/agenda-follow/tomada-extra`;
- reutiliza `FollowTakeSchedule`/fila/convocação da V18;
- tentativas e ausências só aceitam a tomada extra se a chamada excepcional estiver autorizada/ativa;
- Agenda identifica explicitamente `Tomada Extra N`;
- ranking usa normalmente uma tentativa classificável feita na extra.

### Decisão administrativa

V19 cria `follow_manual_results`:

- competition;
- category;
- winnerRegistration;
- decidedByUser;
- justificativa;
- dataCadastro;
- unique por Competition + Category.

Endpoint:

```text
POST /api/v1/resultados-competicao/follow/decisao-organizacao
```

Regras:

- somente DEV/GESTAO;
- mesma competição/categoria;
- Registration ativa e APROVADA;
- justificativa obrigatória;
- só após programa encerrado sem tentativa classificável;
- se existir Tomada Extra ativa, a decisão espera a extra encerrar/cancelar;
- checkpoints são evidência operacional, nunca cálculo automático de vencedor;
- não é criado tempo fictício.

`CompetitionResultsService` devolve metadados de resolução e trata resultado manual como oficial da categoria.

Checkpoint:

```text
Backend Tests #517 ✅
169 testes / 0 falhas / 0 erros / 0 skipped
MySQL + Flyway V19 + testdata ✅
Frontend Checks #224 ✅
```

V1–V19 imutáveis. Próxima migration estrutural: V20+.

Única decisão restante para o fechamento manual do BLOCO 3: bloquear ou não tentativa/ausência Follow fora de Competition `EM_ANDAMENTO`.


## Checkpoint de QA manual do BLOCO 3 — 29/09/2026

A bateria manual foi pausada após o teste 23 para eliminar o excesso de dados de demonstração e tornar os cenários reproduzíveis.

O profile `testdata` agora usa banco dedicado `rascomp_b3_validation`, desliga os seeds antigos e habilita somente `Block3ValidationDataInitializer`.

Cenário único:

- competição `ETAPA 4 · BLOCO 3 · VALIDAÇÃO`, vigente e em andamento;
- `B3 · Follow Operação`: 4 robôs livres;
- `B3 · Follow Exceção`: programa normal encerrado sem tentativa classificável para Tomada Extra/decisão administrativa;
- `B3 · Mini Sumô RC`: 5 inscritos aptos na chave + 2 sem inspeção;
- uma chave histórica + uma vigente, com BYE;
- contas `dev.b3@rascomp.local` e `gestao.b3@rascomp.local`, senha `Rascomp@2026`.

Seeds antigos permanecem disponíveis no código, mas ficam desligados neste profile. O `DataInitializer` base ganhou flag `rascomp.seed.base` (default true) para poder ser desligado somente no cenário de QA.


## Fechamento manual do BLOCO 3 — 30/09/2026

A bateria 1–56 foi percorrida. Regra pendente de janela operacional foi encerrada: tentativas e ausências Follow passam a exigir `Competition.status == EM_ANDAMENTO`.

Pódio oficial consolidado como requisito:

- Follow normal/extra: posições 1–3 do ranking;
- Follow sem tentativa classificável: decisão administrativa deve definir pódio ordenado, com justificativa/auditoria e sem criar tempo fictício;
- Sumô: campeão = vencedor da final, vice = perdedor da final, 3º = vencedor de disputa própria entre os perdedores das semifinais.

A disputa de terceiro lugar, correção DEV auditável, estado derivado ELIMINADO, independência da tela Chaves e histórico/pódio consolidado em Resultados são correções necessárias antes de marcar o BLOCO 3 como CONCLUÍDO.


## ETAPA 4 / BLOCO 3 — correções finais para re-smoke — 30/09/2026

Novos fluxos de domínio:

### Entrada manual DEV

`ManualCompetitionEntryService` implementa:

```text
UserAccount PARTICIPANTE ativa
→ Competitor existente ou criado/vinculado na Team escolhida
→ Robot criado na Team
→ Registration APROVADA
→ RegistrationStatusHistory = ENTRADA_MANUAL
```

A operação exige DEV + justificativa e pode ocorrer com inscrições abertas/encerradas ou competição `EM_ANDAMENTO`, sem reabrir inscrições públicas.

No Sumô, a inspeção continua obrigatória. No Follow, a nova inscrição pode entrar na sincronização das próximas chamadas.

### Regeneração excepcional de chave

V20 adiciona auditoria em `brackets` (`generation_reason`, `generated_by_user_id`).

DEV pode regenerar durante `EM_ANDAMENTO` com justificativa somente enquanto a chave atual não tiver atividade competitiva real. A chave anterior vira histórica; nunca é apagada.

### Terceiro lugar

V21 adiciona `matches.match_type`:

- `ELIMINATORIA`;
- `TERCEIRO_LUGAR`.

A geração cria a disputa de 3º para chaves com semifinais. Os perdedores das duas semifinais alimentam automaticamente seus slots. Correções seguras de semifinal também corrigem o participante correspondente na disputa de 3º.

### Pódio Follow

V22 amplia `follow_manual_results` com segundo e terceiro lugares. Ranking normal/extra usa as três primeiras posições; decisão administrativa exige pódio ordenado conforme quantidade de elegíveis.

### Correção extrema DEV

V23 audita correções de `match_results` com motivo, DEV e data/hora. A operação:

- preserva os rounds originais;
- altera o vencedor consolidado;
- mantém o placar coerente;
- corrige propagação/3º lugar quando seguro;
- bloqueia se a dependência seguinte já iniciou.

A bateria automatizada ganhou cobertura para entrada manual, regeneração excepcional, terceiro lugar, pódio manual e correção DEV.


## Correções finais implementadas — aguardando re-smoke

Após a bateria 1–56, foram implementadas as correções estruturais principais:

- entrada manual DEV de participante/robô vinculada a conta PARTICIPANTE + equipe/competidor;
- inscrição manual já aprovada e auditada;
- Follow: novo robô entra nas próximas filas/tomadas após sincronização;
- Sumô: novo robô exige inspeção antes de entrar em nova chave;
- regeneração excepcional DEV de chave com justificativa e histórico, bloqueada após atividade competitiva real;
- Chaves passou a exibir a árvore no próprio módulo;
- correção excepcional DEV de resultado Sumô com justificativa, auditoria e proteção de dependências;
- pódio completo em Resultados;
- decisão administrativa Follow passou a aceitar 1º/2º/3º;
- disputa de 3º lugar Sumô criada junto da chave e alimentada pelos perdedores das semifinais;
- estado competitivo derivado (Campeão, Vice, 3º, Eliminado etc.) no Sumô;
- melhorias anteriores de g/kg, juízes, BYE, filtros, chamada e bloqueio Follow fora de EM_ANDAMENTO.

Estado: **CORREÇÕES IMPLEMENTADAS · RE-SMOKE PENDENTE**.


## Revisão de identidade PARTICIPANTE — 30/09/2026

Regra canônica revisada:

- conta `PARTICIPANTE` representa pessoa competidora;
- após associação a uma equipe deve existir exatamente um `Competitor` ligado à conta;
- criação de equipe pelo participante cria também seu Competitor na mesma transação;
- entrada manual DEV exige participante já associado a Competitor/equipe e deriva a equipe desse vínculo;
- participante pode cadastrar instituição pelo Portal para não depender de catálogo prévio;
- fluxo manual permanece exceção, não substitui o onboarding normal.

Ingresso em equipe existente e aprovação do vínculo serão concluídos no BLOCO 4 e deverão criar o Competitor automaticamente.


## Fronteira BLOCO 3 → BLOCO 4 — 30/09/2026

O domínio de operação competitiva do BLOCO 3 está congelado. Convites, ingresso em equipe e responsabilidade de robôs passam ao BLOCO 4.

BLOCO 4 deve consolidar:
- convite/aceite e solicitação/aprovação de ingresso em equipe;
- `PARTICIPANTE → Competitor → Team`;
- vínculo N:N `Robot ↔ Competitor responsável`;
- visibilidade "Meus robôs" por responsabilidade, enquanto o líder administra todos;
- associação de responsáveis pelo líder ao criar/editar robôs;
- inscrição usando os responsáveis como sugestão inicial, sem confundir responsabilidade permanente com competidores daquela Registration.

O cadastro manual DEV permanece apenas como contingência operacional descoberta no BLOCO 3.


## BLOCO 4 iniciado — 01/10/2026

V24 adiciona:
- `team_membership_requests`;
- `robot_responsibles`.

4.1 implementado:
- CONVITE líder → participante;
- SOLICITACAO participante → equipe;
- aceite/aprovação cria/reaproveita Competitor vinculado à UserAccount e Team;
- conta já associada a outra equipe é bloqueada.

4.2 base implementada:
- RobotResponsible;
- criador do robô vira responsável inicial;
- líder pode definir responsáveis;
- autorização/visibilidade do membro passa a usar responsabilidade por robô.


---

## ETAPA 4 / BLOCO 4.3 — inscrição normal pelo Portal — 01/10/2026

### Regra consolidada

```text
Team possui Robot
Robot possui RobotResponsible (N:N com Competitor)

RobotResponsible
≠
Registration.competitors
```

Responsabilidade permanente pelo robô continua separada da composição específica de uma inscrição.

Fluxo normal:

```text
PARTICIPANTE associado a Team
→ escolhe Competition com inscrições abertas
→ escolhe Robot gerenciável
→ escolhe Category
→ responsáveis permanentes vêm como sugestão inicial
→ ajusta competidores ativos da mesma Team
→ envia
→ Registration = PENDENTE
→ GESTAO aprova/rejeita
→ somente APROVADA vira participação oficial
```

### Autorização

`ParticipantPortalService.inscrever(...)` não exige mais que o usuário seja líder.

Agora:

- líder pode administrar qualquer Robot da própria Team;
- membro comum pode administrar Robot quando possui `RobotResponsible` ativo;
- estar apenas em `Registration.competitors` permite visualizar aquela participação, mas não concede administração permanente do Robot;
- cancelamento, reativação e solicitação de cancelamento pelo Portal usam a mesma regra de administração do Robot.

### Reuso do domínio existente

O 4.3 não criou segundo fluxo de Registration.

Continua usando `RegistrationService.criarPorParticipante(...)`, portanto permanecem centralizadas:

- janela e estado de inscrições;
- vínculo Robot → Team;
- Competitor ativo e pertencente à Team;
- ao menos um competidor;
- duplicidade Competition + Category + Robot;
- compatibilidade física do Sumô;
- criação em `PENDENTE`;
- histórico de status.

O fluxo administrativo de aprovação/rejeição também não foi duplicado.

### Listagem do membro

Para membro comum, o Portal reúne e deduplica:

1. Registration em que sua UserAccount está entre os competidores específicos;
2. Registration dos Robots pelos quais possui responsabilidade permanente.

O líder continua recebendo todas as inscrições da equipe.

### Testes automatizados adicionados

`ParticipantPortalServiceTest`:
- membro responsável pode enviar inscrição sem ser líder;
- listagem reúne participação específica + responsabilidade por Robot sem duplicar.

`AccessPolicyServiceTest`:
- responsável permanente pode administrar a Registration do Robot;
- membro sem responsabilidade permanente não pode administrá-la.

As suítes pré-existentes de `RegistrationService` preservam cobertura de PENDENTE e invariantes de equipe.

### Profile `testdata` do 4.3

O profile passou a usar banco dedicado:

```text
rascomp_b4_validation
```

Initializer:

```text
Block4PortalValidationDataInitializer
```

Contas:

```text
dev.b4@rascomp.local
gestao.b4@rascomp.local
lider.b4@rascomp.local
membro.b4@rascomp.local
senha: Rascomp@2026
```

Cenário principal:

- Competition `ETAPA 4 · BLOCO 4.3 · INSCRIÇÕES`, vigente e com inscrições abertas;
- Team `B4 · Equipe Portal`;
- `B4 · Vespa` sob responsabilidade de Membro B4 + Apoio B4;
- `B4 · Atlas` disponível ao líder, mas não ao membro comum;
- categorias Follow, Mini Sumô RC e Sumô 3 kg RC;
- nenhuma Registration pré-criada.

O initializer do BLOCO 3 permanece no código, porém desativado no profile atual.

### CI do 4.3

O job de MySQL/testdata foi atualizado para exercitar o fluxo real:

- login de membro/líder/GESTAO;
- visibilidade diferente de Robots;
- criação via endpoint PARTICIPANTE;
- status `PENDENTE`;
- ausência na API pública antes da aprovação;
- bloqueio de duplicidade;
- aprovação pelo fluxo administrativo;
- presença na API pública somente após `APROVADA`.

Estado documental deste checkpoint histórico: **supersedido pela revisão canônica de 01/10/2026 antes da validação manual. BLOCO 4.3 foi reaberto; BLOCO 4.4 continua não iniciado.**


---

## Revisão canônica 01/10/2026 — inscrição pessoal + inscrição de Robot

O modelo anterior de inscrição direta do Robot foi **supersedido antes da validação manual**.

Agora existem duas aprovações independentes por Competition:

```text
Competitor → ParticipantCompetitionRegistration
Robot      → Registration
```

Fluxo pessoal:

```text
Competitor
→ Competition
→ dados
→ comprovante
→ PENDENTE
→ GESTAO aprova/rejeita
```

Fluxo do Robot:

```text
Robot
→ Competition + Category
→ 1+ competidores
→ comprovante
→ PENDENTE
→ GESTAO aprova/rejeita
```

Invariantes:

- pertencer à Team não depende de estar inscrito em Competition;
- ser `RobotResponsible` não depende de estar inscrito em Competition;
- cadastrar Robot não exige aprovação administrativa;
- `Registration.competitors` deve conter apenas `RobotResponsible` ativos daquele Robot;
- liderança da Team concede administração do cadastro, mas não responsabilidade competitiva automática;
- o líder só pode constar como competidor de um Robot quando estiver explicitamente ligado a ele como `RobotResponsible`;
- a inscrição do Robot pode ser enviada enquanto inscrições pessoais ainda estão `PENDENTE`;
- a inscrição do Robot pode ser `APROVADA` quando existir ao menos um responsável pessoalmente `APROVADO` na mesma Competition; responsáveis `PENDENTE` não bloqueiam outro elegível;
- aprovação pessoal e aprovação do Robot nunca propagam automaticamente uma para a outra;
- comprovantes e auditorias das duas inscrições permanecem independentes.

### UX da GESTAO

Ao aprovar Competitor, mostrar os Robots pelos quais ele é responsável e o status das inscrições desses Robots na mesma Competition.

Ao aprovar Robot, mostrar seus competidores selecionados e, para cada um, o status da inscrição pessoal na mesma Competition.

A interface deve destacar dependências. O backend deve bloquear qualquer aprovação inconsistente mesmo se o request for adulterado.

Exemplo bloqueado:

```text
Robot Vespa
competidor selecionado = João
João não é RobotResponsible do Vespa
→ aprovação proibida
```

Exemplo bloqueado:

```text
Robot Vespa
competidor = Gabriel
Gabriel é RobotResponsible
inscrição pessoal de Gabriel = PENDENTE
→ Robot permanece PENDENTE
```

O fluxo manual DEV continua exceção auditável e deve formalizar as relações reais necessárias.

Estado após esta revisão:

```text
BLOCO 4.3 = REABERTO / EM IMPLEMENTAÇÃO
BLOCO 4.4 = NÃO INICIADO
```


### Cardinalidade N:N Robot ↔ Competitor

A relação de responsabilidade é obrigatoriamente **muitos-para-muitos**:

```text
1 Robot
→ 1..N RobotResponsible

1 Competitor
→ 0..N Robots como RobotResponsible
```

Exemplos válidos:

```text
Vespa
→ Gabriel
→ João
→ Maria

Gabriel
→ Vespa
→ Atlas
→ LineBot
```

As exigências de aprovação definem apenas o mínimo necessário para uma participação válida; elas **não limitam** Robot a um único Competitor nem Competitor a um único Robot.

Regras de alteração posterior:

- um Robot já cadastrado pode receber novos `RobotResponsible`;
- um Competitor pode ser associado como responsável a vários Robots da própria Team;
- antes do início da Competition, alterações feitas pelo líder em `RobotResponsible` devem refletir automaticamente na composição competitiva da Registration correspondente;
- uma `Registration` já `APROVADA` **não volta para PENDENTE** só porque a composição foi ajustada antes da competição;
- novo responsável com inscrição pessoal `APROVADA` na mesma Competition pode entrar automaticamente em `Registration.competitors`;
- novo responsável cuja inscrição pessoal ainda esteja `PENDENTE` pode existir como `RobotResponsible`, mas só passa a integrar oficialmente a composição competitiva quando sua inscrição pessoal ficar `APROVADA`;
- remover um responsável antes do início da competição remove automaticamente essa pessoa da composição competitiva daquela Registration;
- a GESTAO recebe aviso/auditoria da alteração de composição e pode **vetar a mudança competitiva específica**, com justificativa, sem obrigar o Robot inteiro a passar por nova aprovação;
- o veto da GESTAO afeta a associação competitiva daquela Competition/Registration; não precisa apagar o vínculo permanente `RobotResponsible`, que pode continuar válido para outras competições;
- quando a Competition atingir `EM_ANDAMENTO` ou sua data de início, a composição `Registration.competitors` fica congelada no fluxo normal;
- depois desse bloqueio, líder/participantes não podem ficar trocando responsáveis competitivos durante a prova.

Portanto:

```text
RobotResponsible
= vínculo permanente N:N

Registration.competitors
= recorte competitivo daquele Robot naquela Competition/Category
```


---

## Checkpoint 01/10/2026 — BLOCO 4.3 / inscrição dupla e N:N implementados

> **Histórico superado pelas regras V26/V27.** A fonte funcional atual está no frontend `docs/REGRAS_PARTICIPANTE.md`.

O 4.3 foi reaberto antes da validação manual e a implementação principal foi alinhada ao fluxo real definido com o cliente.

### Migration V25

`V25__participant_competition_registration_and_payment_receipts.sql` adiciona:

- `participant_competition_registrations`;
- unicidade `Competition + Competitor`;
- status pessoal `PENDENTE | APROVADA | REJEITADA | CANCELADA`;
- solicitante/revisor/data/motivo;
- metadata do comprovante pessoal;
- metadata do comprovante da Registration do Robot.

Migrations V1–V25 permanecem imutáveis. Próxima migration estrutural: **V27+**.

### Inscrição pessoal

Fluxo:

```text
PARTICIPANTE associado a Competitor/Team
→ Competition com inscrições abertas
→ comprovante
→ ParticipantCompetitionRegistration PENDENTE
→ GESTAO aprova/rejeita
```

A associação à Team e a responsabilidade por Robot continuam independentes desta aprovação.

### Inscrição de Robot

Fluxo normal:

```text
Robot
→ Competition + Category
→ selecionar 1..N RobotResponsible
→ comprovante próprio
→ Registration PENDENTE
→ GESTAO analisa
```

A aprovação exige simultaneamente:

- pelo menos um competidor;
- todos os `Registration.competitors` são `RobotResponsible` ativos do Robot;
- todos possuem inscrição pessoal `APROVADA` na mesma Competition;
- comprovante do Robot presente;
- invariantes anteriores de Registration preservadas.

A Registration pode ser enviada enquanto as inscrições pessoais estão PENDENTE; apenas a aprovação fica bloqueada.

### Relação N:N

```text
Robot → 1..N RobotResponsible
Competitor → 0..N Robots
```

O cenário QA passa a provar os dois sentidos:

```text
Vespa → Membro B4 + Apoio B4
Apoio B4 → Vespa + Atlas
```

Adicionar responsabilidade permanente não reescreve uma Registration existente.

Remover um `RobotResponsible` usado em Registration `PENDENTE` ou `APROVADA` é bloqueado até regularização.

O fluxo manual DEV formaliza a responsabilidade escolhida antes de criar a entrada competitiva excepcional.

### Aprovação cruzada na GESTAO

A interface administrativa passa a mostrar:

```text
Competitor → Robots associados + status das inscrições dos Robots
Robot → Competitors selecionados + status da inscrição pessoal
```

Os comprovantes da pessoa e do Robot são independentes e podem ser consultados pela GESTAO.

O frontend antecipa bloqueios para UX, mas o backend repete as validações no momento da aprovação.

### Storage de comprovantes

Storage local atual:

```text
./uploads/registration-receipts
```

Formatos aceitos:

- PDF;
- JPEG;
- PNG;
- WEBP.

Limite: 10 MB.

O diretório `uploads/` já permanece ignorado pelo Git.

### QA automatizado preparado

O profile `testdata` usa:

```text
membro.b4@rascomp.local → Vespa
apoio.b4@rascomp.local  → Vespa + Atlas
lider.b4@rascomp.local  → administra a Team
gestao.b4@rascomp.local → aprovação
```

O job `portal-testdata` foi atualizado para:

1. criar duas inscrições pessoais PENDENTE com comprovante;
2. criar Vespa PENDENTE com dois RobotResponsible;
3. comprovar que aprovação precoce do Robot falha;
4. aprovar as duas pessoas;
5. confirmar contexto cruzado;
6. aprovar o Robot;
7. confirmar que apenas APROVADA entra na API pública.

Também foram adicionados/adaptados testes unitários e de fluxo para inscrição pessoal, comprovante, responsabilidade e remoção protegida.

### Estado

```text
BLOCO 4.3
→ implementação principal adiantada
→ build/CI atual ainda NÃO confirmado
→ validação manual ainda NÃO realizada

BLOCO 4.4
→ NÃO INICIADO
```

A bateria manual canônica está em `docs/VALIDACAO_ETAPA4_BLOCO4.md`.


### Sequenciamento obrigatório — inscrição pessoal antes do robô

Para garantir cadastro separado e em ordem sem obrigar o participante a aguardar análise administrativa:

```text
1. PARTICIPANTE envia Minha inscrição
   → ParticipantCompetitionRegistration = PENDENTE

2. A existência da inscrição pessoal PENDENTE ou APROVADA
   → libera Inscrever robô

3. PARTICIPANTE envia Registration do Robot
   → Registration = PENDENTE

4. GESTAO analisa
   → primeiro aprova as pessoas
   → depois pode aprovar o Robot
```

Regras:

- antes de existir inscrição pessoal `PENDENTE` ou `APROVADA` na Competition, o botão de inscrição de Robot fica bloqueado;
- não é necessário aguardar a aprovação pessoal para criar a inscrição do Robot;
- cada responsável considerado para a competição precisa possuir inscrição pessoal na mesma Competition;
- para aprovar o Robot, é suficiente existir **pelo menos um** responsável com inscrição pessoal `APROVADA`;
- responsáveis `PENDENTE` não bloqueiam a aprovação do Robot, mas ainda não entram na composição oficial;
- responsáveis `REJEITADA` ou `CANCELADA` não entram na composição oficial e também não bloqueiam o Robot enquanto existir pelo menos um responsável `APROVADA`;
- inscrição pessoal `REJEITADA` ou `CANCELADA` não libera nova inscrição de Robot;
- o backend repete todas essas validações, independentemente da interface.


### Regra revisada — sincronização automática da composição competitiva

A regra anterior que exigia nova aprovação completa do Robot após alteração de responsáveis foi descartada.

Fluxo canônico:

```text
Competition ainda não iniciou
+
líder altera RobotResponsible
↓
sistema sincroniza Registration.competitors automaticamente
↓
Registration APROVADA permanece APROVADA
↓
GESTAO recebe aviso/auditoria
↓
GESTAO pode vetar a mudança específica com justificativa
```

Elegibilidade:

- responsável com inscrição pessoal `APROVADA` entra automaticamente na composição oficial;
- responsável com inscrição pessoal `PENDENTE` pode permanecer associado ao Robot, porém só entra oficialmente na composição competitiva quando sua inscrição pessoal for aprovada;
- responsável removido antes do início da competição sai automaticamente da composição daquela Registration;
- se a composição ficar sem nenhum competidor elegível, aplicam-se as regras de rejeição/regularização da Registration do Robot.

Limite temporal:

```text
Competition.status == EM_ANDAMENTO
OU
data atual >= Competition.dataInicio

→ Registration.competitors congelado
→ líder/participantes não alteram composição pelo fluxo normal
```

A Gestão não precisa reaprender/reaprovar o Robot inteiro a cada ajuste. O controle é por **notificação + auditoria + veto justificado da alteração específica**.

O veto administrativo é contextual à Competition/Registration. Ele não precisa apagar o vínculo permanente `RobotResponsible`, pois esse vínculo pode continuar relevante para futuras competições.


### Regra consolidada — aprovação do Robot com elegibilidade parcial

A aprovação da Registration do Robot **não exige aprovação pessoal de todos os RobotResponsible**.

Exemplo:

```text
Vespa
├─ Gabriel → REJEITADA
├─ João    → PENDENTE
└─ Maria   → APROVADA
```

Resultado:

```text
Maria é responsável elegível ✅
→ Vespa pode ser APROVADO
```

Composição oficial naquele instante:

```text
Registration.competitors
└─ Maria
```

Gabriel permanece fora da composição oficial porque sua inscrição pessoal foi rejeitada.

João continua associado ao Robot como `RobotResponsible`, porém não integra a composição oficial enquanto sua inscrição pessoal estiver `PENDENTE`. Se João for aprovado antes do início da Competition, ele entra automaticamente na composição e a GESTAO recebe aviso/auditoria da alteração.

Regra de decisão:

```text
>= 1 responsável com inscrição pessoal APROVADA
→ Robot pode ser APROVADO

0 APROVADOS + existe ao menos 1 PENDENTE
→ Robot permanece PENDENTE

0 APROVADOS + todos os responsáveis REJEITADOS/CANCELADOS
→ Robot Registration é REJEITADA automaticamente
→ motivo: sem responsável elegível
```

A rejeição pessoal nunca remove automaticamente o vínculo permanente `RobotResponsible`; ela apenas retira a elegibilidade naquela Competition.


---

## Checkpoint canônico 01/10/2026 — regras do participante / V26

Fonte funcional específica cross-repo: frontend `docs/REGRAS_PARTICIPANTE.md`.

Implementação consolidada no BLOCO 4.3:

- V25 separa inscrição individual e Registration do Robot, com comprovantes independentes;
- V26 adiciona `Robot.createdByUser`, auditoria/veto de composição, histórico da inscrição individual e histórico de liderança;
- membro comum só inicia/administra Registration de Robot que ele cadastrou;
- líder pode iniciar/administra Registration de qualquer Robot da própria Team;
- responsabilidade N:N não transfere ownership da Registration;
- Minha inscrição `PENDENTE` ou `APROVADA` libera o fluxo de Robot sem esperar análise;
- composição oficial do Robot é derivada automaticamente dos responsáveis elegíveis;
- **um único responsável pessoalmente APROVADO já basta para o Robot poder ser aprovado**;
- responsáveis `PENDENTE` não bloqueiam outro aprovado;
- aprovação posterior de responsável antes da prova o adiciona automaticamente à composição;
- mudança de responsáveis antes da prova não devolve Robot aprovado para análise completa quando ainda existe elegível;
- GESTAO recebe alteração de composição e pode MANTER/VETAR a mudança específica, com auditoria e justificativa no veto;
- nova proposta posterior a veto é permitida;
- se nenhum elegível existir, Robot permanece PENDENTE quando houver caso recuperável ou é REJEITADO automaticamente quando todos forem inelegíveis;
- Robot rejeitado pode ser conscientemente reinscrito pelo líder/criador quando as condições voltarem a ser válidas;
- líder atual não pode sofrer rejeição pessoal definitiva sem correção ou transferência DEV;
- `CORRECAO_SOLICITADA` permite reenvio sem perder Team;
- DEV pode transferir liderança para participante ativo da mesma Team com inscrição individual PENDENTE/APROVADA, com histórico;
- durante Competition iniciada, mudanças normais de responsáveis/composição ficam bloqueadas.

Bateria canônica: `docs/VALIDACAO_ETAPA4_BLOCO4.md`.

Estado:

```text
4.3 → IMPLEMENTAÇÃO REVISADA / AGUARDANDO BUILD + VALIDAÇÃO MANUAL
4.4 → NÃO INICIADO
```

Não considerar suíte/build verdes sem execução real nos heads atuais.


---

## Checkpoint pós-bateria manual — 03/10/2026

A bateria principal do BLOCO 4.3 foi executada até o Teste 40. Os testes 35–40 foram validados após correção do isolamento do profile `testdata`.

Os achados da bateria geraram uma rodada focal de correções:

- V27 adiciona `registrations.robot_description` para preservar a descrição enviada com a inscrição do Robot;
- criador/líder podem editar nome e descrição simples do Robot;
- cadastro de Robot pode ser removido/desativado com segurança quando não houver Registration PENDENTE/APROVADA;
- duplicidade de nome dentro da mesma Team permanece protegida por serviço + constraint;
- seção de inscrições de Robot e alertas receberam maior destaque;
- cards da GESTAO agora identificam explicitamente métricas de **Robots**;
- filtro de Competition foi movido para o topo da página Inscrições;
- aba do navegador passou a usar título por perfil;
- líder da Team passou a ser identificado no Portal, Competidores e análise de inscrição pessoal;
- reinclusão de responsável gera novo evento de composição;
- veto de remoção restaura `RobotResponsible`;
- veto de adição desfaz a associação;
- mudanças não vetadas não exigem aprovação: são consolidadas automaticamente ao iniciar a Competition;
- edição comum da Competition não altera status;
- ciclo operacional obrigatório permanece `INSCRICOES_ABERTAS → INSCRICOES_ENCERRADAS → EM_ANDAMENTO`;
- entrada manual DEV em `EM_ANDAMENTO` continua excepcional, justificada e auditada, exigindo PARTICIPANTE já associado a Team;
- criação administrativa completa de pessoa/competidor sem vínculo prévio continua no roadmap da ferramenta DEV ampliada.

Validação focal vigente: frontend `docs/VALIDACAO_ETAPA4_BLOCO4.md`, seção **Bateria curta de regressão dos achados (R1–R15)**.

Estado:

```text
BLOCO 4.3
→ bateria 1–40 executada
→ correções pós-bateria implementadas
→ AGUARDANDO regressão R1–R15 + build/testes automatizados

BLOCO 4.4
→ NÃO INICIADO
```

Migrations atuais: **V1–V30**. Próxima migration estrutural: **V31+**.


### Ajustes finais pós-regressão — 03/10/2026

- Entrada manual DEV agora suporta conta PARTICIPANTE existente ainda sem Competitor/Team.
- DEV seleciona Team; o backend cria o vínculo competitivo e uma ParticipantCompetitionRegistration APROVADA com histórico.
- O fluxo de Robot avulso reaproveita a mesma resolução de vínculo e garante também a inscrição pessoal manual antes de Robot + Registration.
- Participante já vinculado não pode ser transferido silenciosamente de Team pela entrada manual.
- A exceção funciona em Competition operável, inclusive EM_ANDAMENTO para DEV.
- Essa operação não altera automaticamente Registration.competitors já congelado durante a prova.


---

## Encerramento final da ETAPA 4 — 03/10/2026

```text
ETAPA 4             ✅ CONCLUÍDA / VALIDADA / MERGE AUTORIZADO
Bateria Portal      ✅ 1–40
Regressão final     ✅ R1–R17
Migrations          V1–V27
Próxima migration   V28+
```

Domínio final validado:
- inscrição individual separada de Registration do Robot;
- RobotResponsible N:N separado de ownership;
- elegibilidade parcial para aprovação de Robot;
- composição automática + veto auditável;
- congelamento no início da Competition;
- proteção/troca de líder;
- edição/remoção segura de Robot;
- entrada manual DEV de participante e Robot;
- conta PARTICIPANTE sem Team pode receber vínculo Competitor → Team pelo fluxo excepcional;
- entrada tardia não altera silenciosamente composição congelada.

Fonte funcional cross-repo: frontend `docs/REGRAS_PARTICIPANTE.md`.

Histórico de QA: frontend `docs/VALIDACAO_ETAPA4_BLOCO4.md`.

O antigo 4.4 foi retirado da ETAPA 4. O próximo trabalho de Landing será consolidado futuramente com a entrega pública já prevista, após decisão específica de roadmap.

Não declarar CI remoto verde: não havia execução nova registrada do GitHub Actions nos heads finais.

---

## Próximo ciclo — V1 Beta em produção

A primeira publicação real foi antecipada para um trilho Beta.

Ordem:

```text
Landing
→ backend/API em cloud
→ MySQL de produção
→ autenticação e inscrições reais
→ smoke/estabilização
→ retorno ao roadmap oficial
```

Política obrigatória após a Beta:

```text
branch → staging/homologação → testes → merge → produção
```

Produção não é ambiente de desenvolvimento.

Requisitos mínimos do backend para abertura:
- profile de produção sem testdata;
- banco separado;
- Flyway aplicado de forma controlada;
- secrets externos ao código;
- CORS/URLs HTTPS;
- healthcheck/logs;
- backup/restore mínimo;
- bootstrap seguro de DEV;
- storage/configuração necessária a comprovantes e mídia sem depender do ambiente local.

### Gate backend da V1 Beta

Não liberar inscrições reais até confirmar:

- banco MySQL de produção persistente;
- backup + estratégia de restore;
- Flyway íntegro no banco real;
- comprovantes em storage persistente, nunca filesystem efêmero;
- contas verificadas reais de DEV/GESTAO;
- Competition/categorias/janela corretas;
- profile de produção sem testdata/demo;
- smoke ponta a ponta com uma conta nova criada do zero.

Ajustes Gerais DEV avançados não bloqueiam este gate.

#### Futebol de Robôs — contrato mínimo da Beta

A Beta deve aceitar uma inscrição em categoria Futebol **sem Robot próprio obrigatório**, porque o Robot pode ser fornecido/atribuído pela organização.

O backend não deve criar Robot fictício apenas para atender a FK atual.

Nesta frente implementar apenas o mínimo necessário para criar/revisar a inscrição. Partida, gols, cronômetro, desempate, chaveamento, inspeção e penalidades permanecem na ETAPA 6.


### Portabilidade da infraestrutura Beta

A V1-BETA B pode ser provisionada inicialmente em uma conta temporária do mantenedor para acelerar o go-live.

Isso é **provisório**.

A infraestrutura deve ser configurada desde o início para permitir migração posterior para:

```text
conta própria do projeto/organização
+
domínio próprio
+
secrets próprios
+
banco/storage próprios
```

Regras:

- não hardcodar IDs, URLs ou credenciais da conta temporária no código;
- domínio/API/frontend devem ser configuráveis;
- secrets devem permanecer externos ao repositório;
- banco e object storage devem possuir estratégia de exportação/migração;
- DNS/TLS devem permitir troca de conta/provedor sem alteração do domínio funcional da aplicação;
- documentar recursos criados, ownership e passos de transferência.

A conta temporária não pode virar dependência estrutural permanente.


### Gate técnico antes da primeira competição oficial

A Beta online não substitui hardening/capacidade.

Antes do primeiro RRC oficial no RasComp:

- revisar SQL injection e toda construção de query;
- validar autorização/ownership em endpoints;
- aplicar/testar rate limiting e proteção de rajadas;
- limitar payloads/uploads;
- revisar timeouts e pool de conexões;
- validar menor privilégio do usuário MySQL;
- configurar logs/observabilidade;
- executar carga genérica;
- executar cenário realista com **300–500 participantes**.

O cenário 300–500 deve incluir concorrência entre:

```text
inscrições + comprovantes
GESTAO aprovando
consultas do Portal
Follow registrando tempos
Sumô registrando rounds/partidas
chaves/progressão
ranking/resultados públicos
```

Medir p50/p95/p99, erros, CPU/memória, MySQL/pool, timeouts e recuperação.

Falha de carga, corrupção, corrida ou proteção inadequada bloqueia o uso na primeira competição oficial até correção e repetição dos testes.


### Modo local é requisito permanente

O backend deve continuar executável fora da cloud:

```text
Spring Boot local
→ MySQL local/LAN
→ storage local
```

Cloudflare, R2 e o provedor MySQL de produção não podem contaminar as regras de domínio com dependências obrigatórias.

A configuração deve continuar por ambiente/profile.

#### Contingência no dia da competição

Antes da primeira competição oficial:

- restaurar um snapshot representativo em MySQL local;
- disponibilizar uploads necessários localmente;
- subir Spring Boot em máquina da organização;
- permitir acesso da Gestão/Portal pela LAN;
- executar smoke de inscrições existentes, Follow, Sumô, chaves e ranking;
- documentar mudança de URLs/variáveis;
- definir fonte de verdade durante o modo contingência.

Nunca executar cloud e local como dois bancos graváveis independentes e depois tentar "juntar" manualmente.

#### Cloudflare

Evitar exigir Worker para cada request dinâmica apenas por conveniência de roteamento. Limites/preços vigentes devem ser conferidos novamente na V1-BETA B; a operação do backend não deve ficar presa a uma quota diária específica.


### Segunda via oficial — servidor local + Tunnel

Além do backend hospedado em cloud, o RasComp deve suportar operação oficial com:

```text
Cloudflare Tunnel
→ Spring Boot local
→ MySQL local
→ storage local
```

A aplicação continua acessível pela internet através do domínio/Tunnel e também pode ser acessada pela LAN.

Essa via pode ser escolhida caso os testes de carga mostrem pouca confiança na capacidade/limites da infraestrutura cloud ou no caminho que utilize Workers.

Nenhuma regra de negócio pode depender exclusivamente da VIA A.

A troca deve ser feita por configuração/profile, não por alteração de código.


---

## Logo pública de equipe — Beta A

Implementada para permitir que a Landing identifique visualmente as equipes participantes.

Fluxo:

```text
líder da Team
→ PUT /api/v1/participante/equipes/{teamId}/logo
→ JPEG/PNG/WEBP até 5 MB
→ TEAM_LOGOS_DIR
→ teams.logo_*
→ PublicTeamDTO.logoUrl
→ GET /api/v1/public/equipes/{teamId}/logo
→ Landing
```

A logo é opcional. A ausência não bloqueia cadastro, inscrição ou competição.


---

## Showcase completo para validação da Landing

Existe um cenário local opt-in preparado por `DemoShowcaseDataInitializer`.

Ativação:

```text
RASCOMP_DEMO_SHOWCASE_ENABLED=true
```

Ao iniciar o backend com a flag habilitada, o cenário cria/garante:

- `RRC 2026 · Demonstração ao vivo` como competição `EM_ANDAMENTO` e `vigente=true`;
- várias equipes e robôs com inscrições aprovadas;
- mistura de equipes com logo pública e equipes sem logo, para validar o fallback da Landing;
- Follow Line com ranking e tentativas/tomadas já registradas;
- Mini Sumô com 16 participantes e chave parcialmente avançada;
- Sumô 3 kg com 10 participantes e BYEs;
- inscrições pendentes e rejeitadas para validar Gestão;
- histórico finalizado com chave completa de 32 robôs;
- usuários demo DEV, GESTAO, MIDIA e PARTICIPANTE.

Senha comum dos usuários demo:

```text
Rascomp@2026
```

A flag permanece `false` por padrão e não deve ser ativada em produção.


### Pódio público por categoria — 05/10/2026

A Landing passa a consumir uma projeção pública consolidada dos resultados oficiais:

```text
GET /api/v1/public/podios?competitionId={id}
```

Contrato público:

- somente leitura;
- disponível durante o ciclo público e preenchido em `EM_ANDAMENTO`;
- retorna apenas identificação da categoria/modalidade e 1º, 2º e 3º colocados;
- não expõe justificativas, ator de decisão ou metadados administrativos;
- `podiumCompleto=true` somente quando as três posições estiverem definidas;
- Sumô reutiliza Final + disputa de 3º lugar;
- Follow reutiliza ranking oficial ou decisão administrativa já consolidada pelo domínio.

Regra de apresentação da Landing:

```text
pódio incompleto
→ continua exibindo ranking/chave/histórico

1º + 2º + 3º definidos
→ esconde o histórico como destaque daquela categoria
→ mostra somente o pódio oficial
```

A regra é independente por categoria; uma modalidade pode já exibir pódio enquanto outra continua em disputa.


### Lotes de inscrição — 05/10/2026

Implementação funcional adicionada antes da V1-BETA B.

Modelo:

```text
Competition
→ 0..N RegistrationLot
   → nome
   → dataInicio
   → dataFim
   → ativo
```

Regras:

- lote é uma janela temporal nomeada dentro do período geral de inscrições;
- lotes ativos da mesma competição não podem se sobrepor;
- configuração permitida em `PLANEJADA` e `INSCRICOES_ABERTAS`;
- depois do encerramento das inscrições/início da competição, lotes ficam somente para consulta;
- lotes não criam preço/taxa automaticamente;
- se a competição não possuir lotes, o fluxo de inscrição continua compatível e sem lote;
- se existir pelo menos um lote ativo, nova inscrição normal exige que exista um lote vigente na data atual;
- inscrição pessoal e inscrição de robô preservam `registration_lot_id` como histórico;
- entrada manual DEV não depende de lote vigente;
- remoção de lote é lógica (`ativo=false`) para não destruir histórico.

Migrations:

```text
V29__add_registration_lots.sql
V30__allow_reused_registration_lot_names.sql
```

Endpoints operacionais:

```text
GET    /api/v1/competicoes/{id}/lotes
POST   /api/v1/competicoes/{id}/lotes
PUT    /api/v1/competicoes/{id}/lotes/{lotId}
DELETE /api/v1/competicoes/{id}/lotes/{lotId}
```

Endpoint público:

```text
GET /api/v1/public/competicoes/{competitionId}/lote-atual
```

A Landing usa esse endpoint no Hero competitivo. Nenhum nome de lote é hardcoded.


### Fechamento dos lotes de inscrição — 05/10/2026

Status: **✅ CONCLUÍDO E VALIDADO MANUALMENTE**

Validação realizada:

- criação de múltiplos lotes;
- identificação automática do lote vigente pela data;
- sincronização automática do lote vigente na Landing;
- Hero exibindo o lote atual corretamente;
- lotes futuros preservados e assumindo vigência automaticamente quando a data chegar;
- prorrogação da janela geral de inscrições não altera automaticamente as datas dos lotes;
- ausência de lote vigente em competição que usa lotes bloqueia novas inscrições normais;
- histórico do lote permanece associado à inscrição já realizada.

Regra consolidada:

```text
janela geral de inscrições
→ pode ser prorrogada/reaberta separadamente

lotes
→ permanecem independentes
→ não são estendidos automaticamente
→ mudam de vigente pela data
→ inscrição preserva o lote histórico
```

Nenhum ajuste adicional é necessário neste checkpoint.


## Checkpoint de transição — V1-BETA A → V1-BETA B — 05/10/2026

A **V1-BETA A está concluída e validada**. A próxima fase é a **V1-BETA B**, mas seu desenvolvimento ainda não foi iniciado.

Antes do primeiro commit da B, deve haver uma decisão explícita sobre identidade e acesso real. O primeiro checkpoint obrigatório da B será:

```text
conta criada
→ e-mail real verificável
→ verificação de e-mail
→ ativação da conta
→ login
→ recuperação segura de senha
```

Objetivos:

- reduzir contas descartáveis/falsas sem exigir dados pessoais desnecessários;
- garantir que o usuário controle de fato o endereço de e-mail informado;
- permitir recuperação de senha sem intervenção manual como fluxo principal;
- não expor se um e-mail existe no sistema;
- tokens/códigos de verificação e recuperação devem ser de uso único e expirar;
- não armazenar token de recuperação reutilizável em texto puro;
- DEV pode continuar com fluxo assistido excepcional, auditado, sem conhecer a senha definitiva;
- a escolha do provedor de envio de e-mail será decidida antes da implementação.

Requisito operacional imediato para discussão da B:

- permitir acesso remoto funcional ao RasComp em ambiente de teste/homologação já no início da fase;
- esse acesso não significa abrir inscrições reais nem declarar produção;
- manter modo local e contingência por servidor local + Cloudflare Tunnel como opções oficiais.

**Não iniciar a implementação da V1-BETA B antes dessa decisão de arquitetura/identidade.**


### Checkpoint automatizado final da V1-BETA A — 05/10/2026

Validação executada no PR de fechamento:

```text
Backend Tests #527
→ 211 testes
→ 0 falhas
→ 0 erros
→ 0 skipped
→ BUILD SUCCESS
→ portal-testdata ✅

Frontend Checks #233
→ Gestão typecheck ✅
→ Gestão build ✅

Landing Checks #21
→ Landing typecheck ✅
→ Landing build ✅
```

Durante o fechamento, o primeiro run do backend revelou testes antigos desalinhados com regras já consolidadas. Os testes foram corrigidos para refletir os contratos atuais — sem relaxar as regras de negócio — e a suíte completa voltou a ficar verde.

Com validação manual + CI final verde, a V1-BETA A está autorizada para merge em `main`.


---

## Checkpoint cloud base — 06/10/2026

Preparação adicionada na V1-BETA B:

```text
Dockerfile                         ✅
profile cloud                      ✅
Actuator health                    ✅
Worker/Container scaffold          ✅
secrets fora do código             ✅
testdata bloqueado no profile       ✅
MySQL externo                      ⏳ configurar
e-mail real                        ⏳ configurar
storage operacional R2             ⏳ integrar/configurar
provisionamento Cloudflare          ⏳ próxima sessão
```

A Landing externa por Quick Tunnel foi validada no frontend. O próximo objetivo é conectar a conta Cloudflare temporária e publicar primeiro os frontends, depois API/Container.


---

## Dupla eliminação pós-Beta — modelagem aprovada em 06/10/2026

Não implementar nesta branch de deploy.

Após V1-BETA D, Sumô/Mini Sumô evolui para:

```text
Bracket DOUBLE_ELIMINATION
├─ WINNERS
├─ LOSERS
├─ GRAND_FINAL
└─ GRAND_FINAL_RESET condicional
```

Invariante: 1 derrota não elimina; 2 derrotas eliminam. A Final da Winners não define campeão.

Pódio: vencedor/perdedor da Final Geral decisiva e, em 3º, o perdedor da Final da Losers.

Fonte canônica: `gbsalermo/Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md` + `docs/CONTRATO_REGRAS_COMPETITIVAS.md`.


---

## Gate de banco limpo antes do go-live — 06/10/2026

Decisão: **não migrar/copiar o banco local/testdata para cloud**.

O MySQL cloud deve nascer vazio e receber apenas:

```text
Flyway
+
primeiro DEV real via bootstrap
```

Todos os seeds de desenvolvimento/QA ficam explicitamente `false` no profile cloud, e `CloudProfileSafetyGuard` impede startup com `testdata` ou qualquer seed habilitado.

Equipes, robôs, competidores, inscrições, competições, chaves e contas demo não entram no primeiro deploy.
