# Continuidade — RasComp Backend

Última atualização: **19/09/2026**

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
ETAPA 0  ✅ concluída / validada
ETAPA 1  ✅ concluída / validada
ETAPA 2   ✅ concluída / validada
ETAPA 3   ✅ concluída / validada
ETAPA 4   🚧 EM ANDAMENTO — BLOCO 3 IMPLEMENTADO / AGUARDANDO VALIDAÇÃO
```

Blocos concluídos da ETAPA 1:

```text
Bloco 1 — Competition + Registration ✅
Bloco 2 — Follow Line                 ✅
Bloco 3 — Sumô                        ✅
Bloco 4 — Chaves                      ✅
Bloco 5 — Fluxos integrados           ✅
```

O Bloco 1 consolidou ciclo da competição, inscrições, cancelamento/desistência, prorrogação/reabertura e compatibilidade física de robôs híbridos.

O Bloco 2 consolidou o contrato operacional do Follow:

- exatamente 3 tomadas × 3 tentativas;
- estados coerentes de tentativa;
- tempo máximo por tentativa;
- penalidade temporal configurável;
- cronômetro operacional no frontend;
- tomada perdida por ausência como evento próprio e auditável;
- checkpoints apenas informativos;
- ranking preservado pela melhor tentativa válida da tomada e melhor tomada do robô;
- initializers/testdata alinhados.

O Bloco 3 consolidou o contrato operacional do Sumô:

- inspeção humana `APTO/INAPTO`;
- peso medido apenas informativo/auditável;
- modo de controle `AUTONOMO | RC` por categoria;
- 3 rounds regulares / 2 vitórias;
- rounds extras limitados e justificados;
- `FALHA_INICIALIZACAO` como motivo explícito sem consequência automática escolhida pelo sistema;
- juiz cadastrado no contexto da competição;
- decisão final de juiz identificada e justificada após esgotar os rounds disponíveis;
- initializers/testdata alinhados ao novo contrato;
- Flyway V11.

O Bloco 4 consolidou a integridade do chaveamento:

- geração/regeneração comum somente em `INSCRICOES_ENCERRADAS`;
- BYE automático não é tratado como disputa competitiva real;
- regeneração bloqueada depois de round, resultado ou partida realmente iniciada/finalizada;
- chave marcada `EM_ANDAMENTO` quando começa uma disputa real;
- estrutura lógica protegida contra edição comum após geração;
- agenda operacional separada da árvore competitiva;
- correção segura do vencedor propagado enquanto a próxima dependência ainda não começou;
- correção comum bloqueada depois que a partida dependente possui atividade competitiva;
- initializers/testdata alinhados à nova sequência;
- Flyway V12.

O Bloco 5 fechou a validação integrada da ETAPA 1:

- profile de teste `flowtest` com H2 em memória, services reais e repositories JPA reais;
- `CompetitionLifecycleFlowTest`;
- `RegistrationFlowTest`;
- `FollowCompetitionFlowTest`;
- `SumoCompetitionFlowTest`;
- `CompetitionIntegrityFlowTest`;
- verificação explícita de rollback: batalha com primeiro round válido e segundo inválido não persiste nenhum round nem alteração parcial;
- verificação de operações inválidas preservando o estado anterior;
- total da suíte: **111 testes / 0 falhas / 0 erros / 0 skipped**;
- `demo-profile` verde contra MySQL real + Flyway V12.

As **ETAPAS 1, 2 e 3 estão concluídas e validadas**. O novo roadmap coloca a **ETAPA 4 — Consolidação funcional e polimento do MVP** como próxima etapa, ainda não iniciada.

---

# 2. Estado funcional conhecido

```text
AUTENTICAÇÃO / JWT                       ✅
OWNERSHIP PARTICIPANTE                   ✅
MYSQL + FLYWAY V1–V13                    ✅
COMPETIÇÕES                              ✅ transições + prorrogação/reabertura
EQUIPES / COMPETIDORES / ROBÔS           ✅
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
API PARTICIPANTE                         ✅ base + cancelamento/reativação
API PÚBLICA                              ✅
PROFILE TESTDATA                         ✅
```

Checkpoint automatizado atual confirmado no CI:

```text
135 testes
0 falhas
0 erros
0 skipped
H2 flowtest integrado ✅
SecurityAuthorizationFlowTest ✅
DemoShowcaseDataInitializerTest ✅
MySQL + Flyway V14 + testdata ✅
```

O workflow também compilou a aplicação e inicializou o cenário completo `testdata` contra MySQL real.

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
2. definir representação de categoria Follow encerrada sem qualquer tentativa classificável.

BLOCO 3 não deve ser marcado como validado antes do reteste manual.
