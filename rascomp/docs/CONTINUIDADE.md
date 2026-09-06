# Continuidade — RasComp Backend

Última atualização: **06/09/2026**

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
ETAPA 1  🚧 atual — lógica, integridade e testes de fluxo
ETAPA 2+ ⏳ não iniciadas
```

Em 04/09/2026 houve um checkpoint exclusivamente documental.

Em 06/09/2026 foi consolidado o contrato competitivo da ETAPA 1.

Também em 06/09/2026 foi concluído o bloco funcional de **`Competition + Registration`**, cobrindo:

- máquina de estados da competição;
- reativação/cancelamento/desistência;
- solicitação persistida de cancelamento de inscrição aprovada;
- prorrogação/reabertura auditável da janela de inscrições;
- compatibilidade física de robôs híbridos;
- integração dos fluxos com o portal participante e a Gestão;
- initializers/testdata alinhados às novas invariantes.

Este checkpoint **não encerra a ETAPA 1**. O próximo bloco é **Follow Line**; depois permanecem Sumô, chaveamentos e testes integrados de competição completa.

---

# 2. Estado funcional conhecido

```text
AUTENTICAÇÃO / JWT                       ✅
OWNERSHIP PARTICIPANTE                   ✅
MYSQL + FLYWAY V1–V9                     ✅
COMPETIÇÕES                              ✅ base + transições + prorrogação/reabertura
EQUIPES / COMPETIDORES / ROBÔS           ✅
INSCRIÇÕES + REVISÃO                     ✅ invariantes + cancelamento solicitado + híbridos
FOTOS DOS ROBÔS                          ✅
FOLLOW LINE                              ✅ base atual
RANKING FOLLOW                           ✅ base atual
SUMÔ / INSPEÇÃO / ROUNDS                 ✅ base atual
2 PENALIDADES = DERROTA DO ROUND         ✅
SUICÍDIO/WO                              ✅
CHAVES / BYE / PROGRESSÃO                ✅ base atual
HISTÓRICO DE CHAVES                      ✅
API PARTICIPANTE                         ✅ base + cancelamento/reativação
API PÚBLICA                              ✅
PROFILE TESTDATA                         ✅
```

Checkpoint automatizado atual:

```text
73 testes
0 falhas
0 erros
0 skipped
MySQL + Flyway V9 + testdata ✅
```

O workflow compilou a aplicação e inicializou o cenário completo `testdata` contra MySQL real no CI.

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
V1 — schema competitivo principal
V2 — inspeções de Sumô
V3 — rounds de Sumô
V4 — remoção de estrutura legada Follow/chaves
V5 — usuários / ownership / fotos
V6 — histórico de chaves
V7 — regras estendidas de round/penalidades
V8 — solicitações de cancelamento + histórico da janela de inscrições
V9 — classe física de Sumô nas categorias
```

Regra:

```text
V1–V9 nunca são reescritas
próxima mudança estrutural = V10+
```

A adição de `DESISTENTE` não exigiu migration própria porque `registrations.status` já é textual. A V8 persistiu os novos históricos sem sobrecarregar `Registration` ou `Competition` com flags transitórias. A V9 adicionou `sumo_physical_class` a `competition_categories` e fez backfill das categorias Sumô legadas a partir de `config_sumo.peso_max` apenas durante a migração.

No runtime atual, a classe física **não é inferida pelo nome da categoria**.

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

Robot
→ um único robô físico da equipe

CompetitionCategory.sumoPhysicalClass
→ classe física competitiva da categoria de Sumô
```

Não implementar transferências administrativas como simples troca genérica de FK.

---

# 7. Registration — contrato e implementação atual

Estados atuais:

```text
PENDENTE
APROVADA
REJEITADA
CANCELADA
DESISTENTE
DESCLASSIFICADA
```

## Implementado em 06/09/2026

```text
PENDENTE
→ participante pode cancelar diretamente
→ edição comum permanece permitida

APROVADA
→ participante não pode cancelar diretamente
→ participante abre solicitação de cancelamento
→ Registration permanece APROVADA enquanto a solicitação está PENDENTE
→ organização APROVA ou REJEITA a solicitação

REJEITADA
→ organização pode reabrir para correção

CANCELADA / REJEITADA
→ reabertura valida competição ativa + janela de inscrições
→ retorna PENDENTE

APROVADA sem atividade competitiva
→ cancelamento aprovado = CANCELADA

APROVADA com atividade competitiva
→ cancelamento aprovado = DESISTENTE
```

Atividade competitiva atualmente detectada para a distinção `CANCELADA/DESISTENTE`:

```text
tentativa Follow
ou
inspeção de Sumô
ou
participação em Match
```

O `PUT` genérico deixou de ser uma forma de contornar essas regras:

- somente `PENDENTE` pode ser editada pelo fluxo comum;
- `ativo=false` não é aceito como atalho de cancelamento;
- no fluxo comum, `PENDENTE` só muda para `APROVADA` ou `REJEITADA`;
- aprovação/rejeição continuam exigindo ORGANIZACAO.

## Solicitação de cancelamento persistida

Modelo implementado:

```text
RegistrationCancellationRequest
├─ registration
├─ requestedByUser
├─ status: PENDENTE | APROVADA | REJEITADA
├─ motivo
├─ reviewedByUser
├─ reviewedAt
├─ resposta
└─ dataCadastro
```

Regras:

- somente inscrição ativa e `APROVADA` aceita solicitação;
- somente PARTICIPANTE usa o fluxo do portal;
- não pode haver duas solicitações `PENDENTE` para a mesma inscrição;
- competição `FINALIZADA` ou `CANCELADA` não aceita nova solicitação;
- aprovação pela organização conclui o cancelamento de forma transacional;
- rejeição mantém a inscrição `APROVADA`.

Endpoints principais:

```text
POST /api/v1/participante/inscricoes/{id}/solicitacoes-cancelamento
GET  /api/v1/participante/inscricoes/{id}/solicitacoes-cancelamento

GET   /api/v1/solicitacoes-cancelamento-inscricao
PATCH /api/v1/solicitacoes-cancelamento-inscricao/{id}/aprovar
PATCH /api/v1/solicitacoes-cancelamento-inscricao/{id}/rejeitar
```

O portal participante também possui reativação segura:

```text
PATCH /api/v1/participante/inscricoes/{registrationId}/reativar
```

## Robôs híbridos — implementado

`Robot` continua representando um único robô físico. A classe física pertence à categoria de Sumô:

```text
SumoPhysicalClass
├─ MINI_500G
└─ SUMO_3KG
```

Regras aplicadas por `RegistrationService` na criação, edição e reativação:

```text
mesmo Robot + mesma Competition
├─ Follow + Mini                         ✅
├─ Follow + 3 kg                         ✅
├─ Mini Auto + Mini R/C                  ✅
├─ 3 kg Auto + 3 kg R/C                  ✅
└─ Mini + 3 kg                           ❌
```

A validação compara apenas inscrições `PENDENTE` ou `APROVADA` do mesmo robô na mesma edição. Inscrições Follow não participam do conflito de classe física.

Categoria `SUMO` sem `sumoPhysicalClass` não recebe nova inscrição pelo fluxo normal.

A metadata é explícita em `CompetitionCategory`; não há inferência runtime por nome como `"Mini"` ou `"3 kg"`.

Initializers alinhados:

```text
DataInitializer
PostmanScenarioInitializer
BracketHistoryTestDataInitializer
DemoShowcaseDataInitializer
```

Pagamento ainda não existe e **não é bloqueador deste bloco**. Quando habilitado futuramente, deverá ser condição opcional de aprovação conforme contrato.

---

# 8. Competition — contrato e implementação atual

Fluxo normal protegido no backend:

```text
PLANEJADA
→ INSCRICOES_ABERTAS
→ INSCRICOES_ENCERRADAS
→ EM_ANDAMENTO
→ FINALIZADA
```

`CANCELADA` pode ser alcançada pelos estados operacionais permitidos.

Implementado:

- nova competição nasce obrigatoriamente `PLANEJADA`;
- fluxo comum não permite pular estados;
- `FINALIZADA` e `CANCELADA` não voltam para estados anteriores por simples `PUT`;
- inativação não pode ser disfarçada usando `ativo=false` no update comum;
- frontend Gestão oferece somente o estado atual e as próximas transições permitidas;
- prorrogação/reabertura usa operação explícita e histórica.

Operação:

```text
POST /api/v1/competicoes/{id}/prorrogar-inscricoes
GET  /api/v1/competicoes/{id}/historico-inscricoes
```

Histórico persistido:

```text
CompetitionRegistrationWindowChange
├─ competition
├─ tipo: PRORROGACAO | REABERTURA
├─ dataFimAnterior
├─ novaDataFim
├─ motivo
├─ realizadoPor
└─ dataCadastro
```

Regras atuais:

```text
INSCRICOES_ABERTAS
→ nova data posterior à atual
→ PRORROGACAO
→ continua INSCRICOES_ABERTAS

INSCRICOES_ENCERRADAS
→ somente sem atividade competitiva
→ REABERTURA
→ INSCRICOES_ABERTAS
```

A nova data não pode estar no passado nem ultrapassar `dataInicio` da competição.

Para decidir se uma reabertura é segura, são verificados:

- tentativas Follow;
- rounds de Sumô;
- resultados de partida;
- partidas `EM_ANDAMENTO` ou `FINALIZADA`.

Se existir chave atual ainda sem atividade competitiva, a reabertura:

```text
preserva a chave historicamente
→ atual = false
→ status = CANCELADO
```

A chave deverá ser gerada novamente depois do novo fechamento das inscrições.

---

# 9. Follow Line — contrato aprovado

```text
3 tomadas
×
3 tentativas por tomada
```

Sem janela total obrigatória da tomada.

Tempo máximo é por tentativa, configurável; referência inicial:

```text
120 s
```

Durante uma tomada:

```text
alteração física    ❌
alteração de código ❌
```

Entre tomadas:

```text
alteração física    ✅
alteração de código ✅
```

Ranking:

```text
tempoFinal = tempoSegundos + penalidades
→ melhor tentativa da tomada
→ melhor tomada do robô
→ menor tempo final
```

Penalidade é entrada em segundos, com referência comum de `+10 s`, não valor rígido de código.

Falha em parar corretamente após concluir pode gerar penalidade temporal sem invalidar automaticamente a tentativa.

Estados classificáveis:

```text
concluida=true
valida=true
tempoSegundos!=null
```

Combinações impossíveis devem ser bloqueadas.

Ausência na chamada:

```text
cronômetro de apresentação configurável
referência 60 s
→ não compareceu
→ tomada PERDIDA_POR_AUSENCIA
```

Checkpoints continuam informativos e não alteram ranking.

---

# 10. Sumô — contrato aprovado

Categorias previstas:

```text
Mini 500 g Auto
Mini 500 g R/C
3 kg Auto
3 kg R/C
```

Todas usam `Modalidade.SUMO`, isoladas por categoria.

## Inspeção

A decisão passa a ser humana:

```text
organização inspeciona fisicamente
→ informa APTO/INAPTO
```

Peso medido pode ser informativo, mas não deve decidir automaticamente aprovação.

Follow não usa essa inspeção.

## Partida

```text
3 rounds regulares
2 vitórias necessárias
```

```text
0 penalidades → normal
1 penalidade  → normal
2 penalidades → derrota automática do round
SUICIDIO_WO  → adversário vence
BYE          → avanço automático
```

## Rounds extras

Rounds extras não transformam a partida em melhor de cinco.

Só existem quando os rounds regulares não produziram vencedor por existirem rounds anulados/cancelados/não decisivos.

Configuração inicial recomendada:

```text
3 rounds regulares
2 vitórias
máximo 2 rounds extras
```

Round extra exige justificativa e só pode existir enquanto ninguém tiver 2 vitórias.

Se ainda não houver vencedor após o limite, usar decisão do juiz.

## Auto/R/C

Autônomo:

```text
5 s regulamentares após ativação
```

Falha de inicialização depois do procedimento pode gerar penalidade ou derrota do round conforme decisão do juiz; backend não decide automaticamente a consequência.

R/C inicia ao comando do juiz e não usa o atraso regulamentar dos autônomos.

## Juiz

Decisão do juiz deve registrar:

```text
vencedor
juiz
justificativa
data/hora
```

O juiz poderá possuir `UserAccount` ou ser cadastrado apenas no contexto da competição.

---

# 11. Chaves, agenda e progressão — contrato aprovado

Geração normal:

```text
INSCRICOES_ENCERRADAS ✅
demais estados       ❌ para geração comum
```

Regeneração comum só antes de partida/round/resultado iniciado.

Separar:

```text
estrutura lógica da chave
≠
agenda operacional de execução
```

A agenda futura pode possuir pista, horário e ordem de execução, permitindo adiar/adiantar uma batalha sem mudar sua posição lógica.

Permissões futuras:

```text
GESTAO → agenda operacional dentro das regras
DEV    → correção estrutural excepcional e segura
```

## Correção após progressão

Se a próxima partida ainda não começou:

```text
corrigir resultado
→ desfazer slot anterior
→ inserir vencedor correto
→ tudo em uma transação
```

Se a dependência já começou/terminou:

```text
correção comum ❌
```

Rollback competitivo em cadeia fica para ferramenta DEV futura, com auditoria explícita.

---

# 12. ETAPA 1 — estado das alterações

## Bloco 1 — Competition + Registration ✅ CONCLUÍDO

```text
✅ reativação respeita janela
✅ participante não cancela APROVADA diretamente
✅ solicitação persistida de cancelamento APROVADA
✅ análise de cancelamento pela organização
✅ CANCELADA x DESISTENTE protegidas pelo histórico
✅ edição genérica de Registration restringida
✅ reabertura de REJEITADA pela organização
✅ transições normais de Competition protegidas
✅ prorrogação/reabertura auditável
✅ reabertura bloqueada após atividade competitiva
✅ chave atual sem disputa é preservada e invalidada na reabertura
✅ classe física de Sumô explícita na categoria
✅ robô híbrido permitido dentro da mesma classe física
✅ Mini + 3 kg bloqueado para o mesmo Robot na mesma edição
✅ initializers alinhados à classe física
✅ frontend exibe a classe física das categorias Sumô
✅ 73 testes unitários verdes
✅ MySQL/Flyway V9/testdata verdes
```

Reservado para evolução futura, sem implementação antecipada:

```text
pagamento como pré-condição opcional de aprovação
```

Próximos blocos da ETAPA 1:

```text
2. Follow
   - estados válidos
   - 3×3
   - ausência
   - penalidades/cronômetro

3. Sumô
   - inspeção humana
   - rounds extras justificados
   - decisão/identificação de juiz
   - falha de inicialização

4. Chaves
   - status permitido
   - regeneração protegida
   - agenda x estrutura
   - correção transacional antes da dependência
   - bloqueio depois da dependência

5. Testes automatizados de fluxo completo
```

---

# 13. Estratégia de testes da ETAPA 1

Agora há 73 testes unitários de services e smoke do profile `testdata` contra MySQL/Flyway V9.

A camada de fluxo integrado ainda será adicionada para simular:

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

O CI deve continuar validando MySQL/Flyway além dos testes rápidos locais.

---

# 14. ETAPA 4 — Avisos + Telegram

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

# 15. Dívida técnica reservada à ETAPA 2

Ainda existem:

```text
rascomp/bin/
.classpath
.project
.gitkeep desnecessários em packages
```

Além de TODOs/comentários antigos e possíveis duplicações.

Não misturar essas tarefas com a implementação das regras da ETAPA 1.

---

# 16. Evoluções futuras resumidas

A ordem oficial está somente no roadmap canônico.

Não usar esta seção para redefinir numeração de etapas; consultar `docs/ETAPAS_POS_PROJETO.md` antes de avançar.

---

# 17. Executar localmente

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

# 18. Próximo passo / handoff

Próximo bloco recomendado da ETAPA 1:

```text
FOLLOW LINE
1. comparar contrato × TentativaSeguidorLinha/ConfigFollow/RankingFollowService
2. formalizar estados válidos de tentativa
3. fixar o perfil RRC 3 tomadas × 3 tentativas
4. modelar perda de tomada por ausência
5. alinhar penalidades e cronômetro operacional
6. adicionar testes derivados do contrato
7. integrar a UX focada na tomada/robô
```

Não iniciar ETAPA 2 sem conclusão e validação explícita da ETAPA 1.
