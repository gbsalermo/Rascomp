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

Em 06/09/2026 foi consolidado o contrato competitivo da ETAPA 1. **As regras estão decididas/documentadas, mas ainda não devem ser marcadas como implementadas enquanto código e testes não forem atualizados.**

---

# 2. Estado funcional conhecido

```text
AUTENTICAÇÃO / JWT                       ✅
OWNERSHIP PARTICIPANTE                   ✅
MYSQL + FLYWAY V1–V7                     ✅
COMPETIÇÕES                              ✅ base atual
EQUIPES / COMPETIDORES / ROBÔS           ✅
INSCRIÇÕES + REVISÃO                     ✅ base atual
FOTOS DOS ROBÔS                          ✅
FOLLOW LINE                              ✅ base atual
RANKING FOLLOW                           ✅ base atual
SUMÔ / INSPEÇÃO / ROUNDS                 ✅ base atual
2 PENALIDADES = DERROTA DO ROUND         ✅
SUICÍDIO/WO                              ✅
CHAVES / BYE / PROGRESSÃO                ✅ base atual
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

Esse checkpoint é anterior às novas regras formalizadas em 06/09/2026. Não presumir que elas já estão cobertas.

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

# 7. Registration — contrato aprovado

Regras principais agora consolidadas:

```text
PENDENTE
→ participante pode editar/cancelar dentro da janela

APROVADA
→ participante não cancela diretamente
→ solicita cancelamento
→ organização decide

REJEITADA
→ somente organização pode reabrir para correção

CANCELADA
→ retirada antes de comprometimento competitivo relevante

DESISTENTE
→ já existe comprometimento/histórico competitivo e a inscrição deixa a competição
```

Reativação comum:

```text
somente inscrições abertas
+
dentro da janela
→ retorna PENDENTE
```

Pagamento ainda não existe, mas quando habilitado deverá ser condição de aprovação conforme contrato.

Robôs híbridos:

```text
mesmo Robot pode Auto + R/C da mesma classe física
mesmo Robot pode também Follow
mesmo Robot não pode Mini + 3 kg na mesma edição
```

A implementação atual ainda usa unicidade `competition + category + robot`; revisar compatibilidade física sem destruir o conceito de um único robô da equipe.

---

# 8. Competition — contrato aprovado

Fluxo normal:

```text
PLANEJADA
→ INSCRICOES_ABERTAS
→ INSCRICOES_ENCERRADAS
→ EM_ANDAMENTO
→ FINALIZADA
```

Prorrogação/reabertura deve ser operação explícita com nova data e motivo.

Não permitir simples alteração arbitrária de enum para voltar estados competitivos.

Se uma chave já existir e ainda não houver atividade, reabertura pode exigir invalidar/regenerar a chave. Depois de atividade competitiva iniciada, reabertura comum é bloqueada.

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

# 12. ETAPA 1 — alterações necessárias

Prioridade derivada do contrato:

```text
1. Registration / Competition
   - reativação
   - cancelamento e desistência
   - solicitação de cancelamento APROVADA
   - prorrogação/reabertura
   - compatibilidade de robô híbrido

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

5. Testes automatizados de fluxo
```

Nenhum item acima está marcado como implementado apenas por estar documentado.

---

# 13. Estratégia de testes da ETAPA 1

Hoje há testes unitários de services e smoke do profile `testdata`.

Adicionar camada de testes de fluxo com Spring/Repositories reais para simular:

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

A ordem oficial está somente no roadmap canônico:

```text
ETAPA 3  nova matriz de roles
ETAPA 4  Avisos IN_APP + Telegram
ETAPA 5  Ajustes Gerais + auditoria
ETAPA 6  portabilidade institucional
ETAPA 7  CMS/Mídia + Landing real
ETAPA 8  Regras públicas derivadas do contrato competitivo
ETAPA 9  Futebol de Robôs
ETAPA 10 participante completo + identificador competitivo
ETAPA 11 Landing + Galeria
ETAPA 12 Hardening
ETAPA 13 testes manuais completos
ETAPA 14 deploy cloud
```

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

```text
1. ler contrato competitivo canônico
2. comparar regra aprovada com código atual
3. implementar por domínio, backend primeiro
4. adicionar testes unitários e testes de fluxo
5. usar V8+ se houver mudança de schema
6. integrar frontend quando contrato/API mudar
7. manter modo local e CI
8. atualizar documentação no checkpoint
9. aguardar validação antes da ETAPA 2
```

Prioridade imediata recomendada: **Registration + Competition**, pois esses estados determinam a elegibilidade de todos os fluxos competitivos seguintes.
