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

Em 06/09/2026 foram concluídos dois blocos funcionais da ETAPA 1:

```text
Bloco 1 — Competition + Registration ✅
Bloco 2 — Follow Line                 ✅
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

Este checkpoint **não encerra a ETAPA 1**. O próximo bloco é **Sumô**; depois permanecem Chaves e testes integrados de competição completa.

---

# 2. Estado funcional conhecido

```text
AUTENTICAÇÃO / JWT                       ✅
OWNERSHIP PARTICIPANTE                   ✅
MYSQL + FLYWAY V1–V10                    ✅
COMPETIÇÕES                              ✅ transições + prorrogação/reabertura
EQUIPES / COMPETIDORES / ROBÔS           ✅
INSCRIÇÕES + REVISÃO                     ✅ invariantes + cancelamento + híbridos
FOTOS DOS ROBÔS                          ✅
FOLLOW LINE                              ✅ contrato RRC operacional
RANKING FOLLOW                           ✅
AUSÊNCIA DE TOMADA FOLLOW                ✅ auditável
SUMÔ / INSPEÇÃO / ROUNDS                 ✅ base atual; Bloco 3 pendente
2 PENALIDADES = DERROTA DO ROUND         ✅ base atual
SUICÍDIO/WO                              ✅ base atual
CHAVES / BYE / PROGRESSÃO                ✅ base atual; Bloco 4 pendente
HISTÓRICO DE CHAVES                      ✅
API PARTICIPANTE                         ✅ base + cancelamento/reativação
API PÚBLICA                              ✅
PROFILE TESTDATA                         ✅
```

Checkpoint automatizado atual confirmado no CI:

```text
86 testes
0 falhas
0 erros
0 skipped
MySQL + Flyway V10 + testdata ✅
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
```

Regra:

```text
V1–V10 nunca são reescritas
próxima mudança estrutural = V11+
```

A V10:

- normaliza `config_follow` para 3 tomadas × 3 tentativas;
- adiciona `penalidade_padrao_segundos` com default 10;
- adiciona `tempo_apresentacao_segundos` com default 60;
- cria `ausencias_tomada_seguidor_linha` com unicidade por `registration + tomada`;
- registra usuário da organização, observação e data/hora.

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

- somente `ORGANIZACAO` registra;
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

# 9. Sumô — contrato aprovado / Bloco 3 próximo

Categorias previstas:

```text
Mini 500 g Auto
Mini 500 g R/C
3 kg Auto
3 kg R/C
```

Todas usam `Modalidade.SUMO`, isoladas por categoria.

O próximo bloco deve alinhar principalmente:

```text
inspeção humana APTO/INAPTO
peso apenas informativo
autônomo × R/C
falha de inicialização
rounds extras justificados
identificação/decisão do juiz
```

Regras-base já existentes que devem ser preservadas:

```text
3 rounds regulares
2 vitórias necessárias
0 penalidades → normal
1 penalidade  → normal
2 penalidades → derrota automática do round
SUICIDIO_WO  → adversário vence
BYE          → avanço automático
```

---

# 10. Chaves, agenda e progressão — Bloco 4 pendente

Contrato já aprovado:

```text
INSCRICOES_ENCERRADAS ✅ para geração comum
demais estados       ❌
```

Regeneração comum só antes de atividade competitiva dependente.

Separar futuramente:

```text
estrutura lógica da chave
≠
agenda operacional de execução
```

Correção de resultado antes da dependência iniciar deve ser transacional; depois da dependência iniciada, correção comum deve ser bloqueada.

---

# 11. ETAPA 1 — estado dos blocos

```text
Bloco 1 — Competition + Registration       ✅ CONCLUÍDO
Bloco 2 — Follow Line                       ✅ CONCLUÍDO
Bloco 3 — Sumô                              ← PRÓXIMO
Bloco 4 — Chaves e progressão               ⏳
Bloco 5 — Testes integrados de competição   ⏳
```

Bloco 2 concluído com:

```text
✅ estrutura 3×3 protegida
✅ estados válidos de tentativa
✅ tempo máximo por tentativa
✅ penalidade configurável
✅ ação operacional “não parou”
✅ cronômetro de tentativa
✅ cronômetro de apresentação
✅ perda de tomada por ausência
✅ ausência auditável e sem tentativas fictícias
✅ ausência tratada como atividade competitiva
✅ ranking preservado
✅ checkpoints sem efeito no ranking
✅ initializers alinhados
✅ frontend Gestão integrado
✅ 86 testes verdes
✅ MySQL/Flyway V10/testdata verdes
✅ frontend typecheck/build verdes
```

---

# 12. Estratégia de testes da ETAPA 1

O checkpoint atual possui **86 testes unitários** de services e smoke do profile `testdata` contra MySQL/Flyway V10.

O núcleo do Follow já possui cobertura para:

- 3×3;
- tentativa duplicada;
- limites de tomada/tentativa/checkpoint;
- tempo acima do máximo;
- penalidade no tempo final;
- estados impossíveis;
- ausência após tentativa;
- ausência duplicada;
- participante tentando registrar ausência;
- tentativa depois de ausência;
- ausência contando como atividade competitiva;
- reabertura de inscrições depois de ausência.

A camada integrada de competição completa ainda será adicionada no Bloco 5 para simular:

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

# 13. ETAPA 4 — Avisos + Telegram

Planejamento futuro permanece separado da ETAPA 1:

```text
GESTAO/DEV
→ publica aviso por Competition
→ backend persiste IN_APP
→ Telegram distribui a mesma comunicação se habilitado
```

IN_APP continua sendo fonte de verdade; falha externa não invalida o aviso.

---

# 14. Dívida técnica reservada à ETAPA 2

Ainda existem itens como:

```text
rascomp/bin/
.classpath
.project
.gitkeep desnecessários em packages
```

Além de TODOs/comentários antigos e possíveis duplicações.

Não misturar essas tarefas com a implementação das regras da ETAPA 1.

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

Próximo bloco da ETAPA 1:

```text
SUMÔ
1. comparar contrato × InspecaoSumo / ConfigSumo / RoundSumo
2. trocar decisão automática por peso por inspeção humana APTO/INAPTO
3. manter peso medido apenas informativo/auditável
4. formalizar falha de inicialização Auto/R/C
5. proteger rounds extras e exigir justificativa
6. modelar identificação/decisão do juiz
7. adicionar testes derivados do contrato
8. integrar frontend somente após o contrato backend estar consistente
```

Não iniciar ETAPA 2 sem conclusão e validação explícita da ETAPA 1.
