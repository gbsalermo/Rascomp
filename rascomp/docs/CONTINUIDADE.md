# Continuidade do Projeto — RasComp

Última atualização: **26/08/2026 — pós-aprovação da apresentação**

## 1. Marco atual

O projeto foi apresentado à equipe e aprovado. O backend atual forma uma base funcional para operação do RRC e agora entra em fase de **estabilização + evolução de governança**.

```text
AUTENTICAÇÃO / JWT                       ✅
OWNERSHIP PARTICIPANTE                   ✅
MYSQL + FLYWAY V1–V7                     ✅
INSCRIÇÕES + REVISÃO                     ✅
FOLLOW LINE                              ✅
SUMÔ                                     ✅
2 PENALIDADES = DERROTA DO ROUND         ✅
HISTÓRICO DE CHAVES                      ✅
FOTOS DOS ROBÔS                          ✅
API PARTICIPANTE                         ✅ base funcional
API PÚBLICA                              ✅
TESTES AUTOMATIZADOS                     ✅ 48 / 48
PROFILE TESTDATA                         ✅ MySQL + Flyway na CI
```

## 2. Roadmap pós-aprovação

```text
0. ESTABILIZAÇÃO / CORREÇÕES DA REVISÃO
1. PERMISSÕES: DEV | GESTAO | MIDIA | PARTICIPANTE
2. AJUSTES GERAIS DEV + AUDITORIA
3. CMS / MÍDIA
4. REGRAS
5. FUTEBOL DE ROBÔS
6. PARTICIPANTE COMPLETO
7. CONSOLIDAÇÃO LANDING/GALERIA
```

Não iniciar Mídia/Futebol antes de definir a nova matriz de autorização no backend.

## 3. Permissões aprovadas

### DEV

Acesso total e estrutural:

- criar/editar/desativar competição;
- alterar roles;
- manutenção de inscrições;
- transferir competidor entre equipes;
- transferir robô entre equipes;
- trocar responsável da equipe;
- Ajustes Gerais;
- ações excepcionais auditadas.

### GESTAO

Operação da competição:

- inspeção;
- Sumô;
- Follow;
- partidas e resultados operacionais;
- acompanhamento da edição.

Não cria competição nem executa manutenção estrutural DEV.

### MIDIA

Gestão editorial da Landing:

- upload de mídia;
- conteúdos/tópicos;
- slots/janelas;
- galeria;
- publicação conforme política a definir.

### PARTICIPANTE

Ownership da própria equipe, competidores, robôs, fotos, inscrições e acompanhamento.

### Estado atual que será substituído

```text
UserRole = PARTICIPANTE | ORGANIZACAO

/api/v1/participante/** → PARTICIPANTE
/api/v1/**              → ORGANIZACAO
```

Arquivos centrais da futura refatoração:

```text
model/Enum/UserRole.java
config/SecurityConfig.java
model/UserAccount.java
service/UserAccountService.java
controller/UserAccountController.java
controllers por domínio
```

## 4. Distinção importante: usuário × competidor × responsável

```text
UserAccount
→ autenticação e role

Competitor
→ pessoa inscrita como competidor
→ pertence a uma Team
→ pode opcionalmente vincular UserAccount

Team.responsibleUser
→ usuário que possui ownership da equipe no portal
```

Por isso uma operação DEV de “mover participante” deve deixar claro se transfere:

- `Competitor.team`;
- responsabilidade da `Team`;
- ou ambos de forma coordenada.

Não implementar isso como atualização genérica de FK.

## 5. Follow Line

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

### Pendente de regra

- impacto oficial de checkpoints;
- combinações válidas de `concluida`, `valida` e `tempoSegundos=null`;
- sanções/desclassificações oficiais além do que já é persistido.

Comentários obsoletos de `ConfigFollow` foram removidos e substituídos pela regra implementada.

## 6. Sumô

Motor comum para categorias Mini/3 kg e RC/Autônomo.

```text
APROVADA
→ inspeção
→ chave
→ partida
→ rounds
→ MatchResult automático
→ progressão
```

Regras consolidadas:

- `SUICIDIO_WO` = adversário vence o round;
- 0/1 penalidade = disputa normal;
- **2 penalidades = derrota automática**;
- `motivoResultado=PENALIDADES` nesse caso;
- BYE automático;
- chave histórica read-only.

Comentários obsoletos de `ConfigSumo` foram removidos e substituídos pela regra implementada.

## 7. Futebol de Robôs — nova modalidade

Requisito recebido:

```text
2 competidores
robôs fornecidos pela RAS
inscrição sem robô próprio
Team mantida no desenho por enquanto
```

### Incompatibilidade atual

Hoje:

```text
Registration.robot                    obrigatório
ParticipantRegistrationRequest.robotId @NotNull
RegistrationService                   sempre busca Robot
unicidade                              competition + category + robot
```

Portanto a nova modalidade exige migration e estratégia própria de elegibilidade/duplicidade.

Recomendação:

```text
Modalidade += FUTEBOL
Registration.robot opcional quando a modalidade permitir
regra de inscrição por modalidade
reaproveitar Match/MatchResult onde fizer sentido
```

### Ainda precisa de decisão

- equipe é obrigatória ou apenas recomendada;
- como Robô A/B é atribuído;
- tempo/gols/empate/desempate;
- formato de chave/grupos;
- eventual inspeção.

## 8. CMS / Mídia

Já existe base de R2:

```text
storage/ObjectStorageService.java
storage/R2ObjectStorageService.java
config/R2StorageConfiguration.java
config/R2StorageProperties.java
```

O fluxo de fotos dos robôs continua separado em storage local:

```text
RobotImageService
→ RobotImageStorageService
```

Estrutura sugerida para CMS:

```text
MediaAsset
ContentSlot
ContentItem
```

A nova API pública deve fornecer os conteúdos publicados para `landing-page/` e galeria.

## 9. Regras

Área futura com conteúdo oficial para:

- Follow;
- Sumô;
- textos específicos de RC/subcategorias;
- Futebol;
- ambiente/vestimenta.

Não codificar textos como regra oficial antes de confirmação da organização.

Modelo sugerido:

```text
RuleContent
├─ escopo (CATEGORIA | AMBIENTE)
├─ category opcional
├─ seção
├─ título
├─ conteúdo
├─ ordem
├─ ativo
└─ publicado
```

Definir depois se edição cabe a DEV, MIDIA ou ambos.

## 10. Ajustes Gerais DEV

Deve substituir operações manuais no banco, porém não será CRUD bruto de tabelas.

Exemplos de services explícitos:

```text
alterarRole(userId, role)
transferirCompetidor(competitorId, teamId)
transferirRobo(robotId, teamId)
transferirResponsabilidade(teamId, userId)
corrigirInscricao(...)
```

Antes de liberar essa área, criar auditoria de ações sensíveis.

## 11. Achados da revisão — prioridade

### P0 manutenção

#### `rascomp/bin/` rastreado

Existe uma árvore antiga com artefatos compilados/cópias de projeto dentro de `rascomp/bin/`.

`.gitignore` já foi atualizado para ignorar `bin/` daqui em diante, porém os arquivos já rastreados ainda precisam ser removidos num commit isolado:

```bash
git rm -r rascomp/bin
```

Depois rodar toda a CI.

Também avaliar `.classpath`, `.project` e `.gitkeep` obsoletos já rastreados.

### P1 lógica

1. `RegistrationService.reativar()` não chama `validarInscricoesAbertas()`.
2. Cancelamento de inscrição pelo participante precisa regra de estado após aprovação/chave/início.
3. `BracketGenerationService` não restringe explicitamente estados de `Competition` para geração/regeneração.
4. Alterar `MatchResult` depois que o vencedor já avançou pode deixar próxima fase inconsistente; isso é especialmente crítico antes de FUTEBOL e Ajustes Gerais DEV.
5. Autorização atual `ORGANIZACAO` é ampla e incompatível com a matriz nova.

### P2 arquitetura/manutenção

1. `AccessPolicyService` hoje trata ownership do participante, apesar do nome genérico; não misturar nele todas as permissões DEV/GESTAO sem reorganização.
2. Storage R2 e storage local de RobotImage estão paralelos; definir estratégia ao criar CMS.
3. documentação histórica deve apontar o dossiê mestre como referência.

## 12. Ponto correto já validado: usuário inativo

`UserAccount.isEnabled()` retorna `ativo`, e `JwtService.tokenValido()` exige `usuario.isEnabled()`.

Portanto:

```text
DEV/GESTAO desativa conta
→ token existente deixa de autenticar nas próximas requisições
```

Essa lógica não precisa ser refeita.

## 13. Migrations

```text
V1 schema principal
V2 inspeção Sumô
V3 rounds Sumô
V4 limpeza de schema legado
V5 usuários/ownership/fotos
V6 histórico de chaves
V7 penalidades/motivo de round
```

Próxima alteração estrutural: **V8+**.

Nunca reescrever migration já aplicada.

## 14. Testes

Último checkpoint revisado:

```text
48 testes
0 falhas
0 erros
```

CI também inicializa:

```text
MySQL real
→ Flyway
→ testdata
```

Novas features devem acrescentar testes especialmente para:

### Roles

- DEV acessa tudo;
- GESTAO opera evento e não cria competição;
- MIDIA acessa CMS e não opera competição;
- PARTICIPANTE mantém ownership;
- matriz de 403 por área.

### Futebol

- inscrição sem robot;
- inscrição com team/competidor válidos;
- duplicidade;
- chave/partida/resultado;
- publicação pública.

### Ajustes Gerais

- transferências preservam integridade;
- operações auditadas;
- bloqueios em entidades com histórico competitivo.

### CMS

- upload válido/inválido;
- autorização MIDIA/DEV;
- publicação/despublicação;
- ordenação/slots;
- conteúdo público sanitizado.

## 15. Próxima execução recomendada

```text
1. remover artefatos versionados em commit isolado
2. corrigir P1 de Registration/chave/resultado
3. CI verde
4. refatorar UserRole + SecurityConfig
5. implementar permissões por domínio
6. criar auditoria + Ajustes Gerais
7. CMS/Mídia
8. Regras
9. Futebol
```

## 16. Dossiê mestre

Mapa completo cross-repo, incluindo “quero alterar X, onde mexo?” e arquitetura da Landing:

```text
Rascomp-FRONT/docs/DOSSIE_PROJETO_RASCOMP.md
```

Atualizar esse documento quando responsabilidades arquiteturais mudarem.