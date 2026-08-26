# Continuidade do Projeto — RasComp

Última atualização: **26/08/2026**

## 1. Marco atual

O backend está em fase de **consolidação para demonstração do ADMIN e início do portal PARTICIPANTE**.

```text
AUTENTICAÇÃO / OWNERSHIP                 ✅
MYSQL + FLYWAY V1–V7                     ✅
INSCRIÇÕES + REVISÃO                     ✅
FOLLOW LINE                              ✅ domínio implementado
SUMÔ                                     ✅ domínio implementado
HISTÓRICO DE CHAVES                      ✅
FOTOS DOS ROBÔS                          ✅ backend completo
API PARTICIPANTE                         ✅ base + consultas de desempenho
API PÚBLICA                              ✅ projeções read-only
TESTES AUTOMATIZADOS                     ✅ 45 / 45
PROFILE DE DEMONSTRAÇÃO                  ✅ implementado
VALIDAÇÃO LOCAL DO PROFILE TESTDATA      ⏳ executar na máquina de demo
```

CI validada em 26/08/2026:

```text
Tests run: 45
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

## 2. Regra oficial atual — Follow Line

O domínio deve ser lido como:

```text
inscrição / robô
    ↓
tomadas
    ↓
tentativas dentro de cada tomada
```

Ranking:

```text
1. considerar somente tentativas válidas e concluídas;
2. calcular tempo final = tempo + penalidade;
3. selecionar a melhor tentativa de cada tomada;
4. selecionar a melhor tomada do robô;
5. ordenar os robôs pelo tempo final da melhor tomada.
```

Portanto:

- a unidade competitiva mostrada no ranking é a **melhor tomada**;
- a tomada é representada pelo resultado da sua melhor tentativa;
- o histórico deve ser apresentado por tomada, expandindo suas tentativas;
- checkpoints continuam como dado operacional até confirmação de impacto oficial.

Configuração atual:

```text
ConfigFollow
├─ numeroTomadas
├─ tentativasPorTomada
├─ maxTempoSegundos
└─ numeroCheckpoints
```

## 3. Regra oficial atual — Sumô

### Categorias

`RC`, `AUTÔNOMO`, `MINI` e `3 KG` são tratados como categorias competitivas/configurações, não como motores independentes.

Exemplo:

```text
SUMÔ
├─ Mini Sumô RC
├─ Mini Sumô Autônomo
├─ Sumô 3 kg RC
└─ Sumô 3 kg Autônomo
```

O isolamento já ocorre por:

```text
competitionId + categoryId
```

Um robô híbrido pode possuir inscrições distintas em categorias diferentes e cada uma participa somente da própria chave.

### Rounds

Motor comum:

- vitória normal;
- empate;
- anulação;
- cancelamento;
- `SUICIDIO_WO`: derrota do robô que sofreu a ocorrência;
- penalidades A/B por round;
- máximo provisório de 2 penalidades por robô/round;
- consequência automática de 2 penalidades ainda pendente da regra oficial.

O backend continua responsável por:

```text
rounds
→ fechamento da partida
→ MatchResult
→ vencedor
→ progressão
→ campeão
```

### Chaves

- geração usa apenas inscrições aprovadas/ativas e aptas;
- tamanho é a próxima potência de dois;
- BYE é resolvido automaticamente;
- chave nova substitui a vigente sem apagar a anterior;
- chave histórica não aceita alterações;
- API pública mostra apenas a chave vigente/ativa.

## 4. Fotos dos robôs

Fluxo consolidado:

```text
Robot
  ↓
RobotImage (0..N)
  ↓
uma foto principal
```

Backend:

- JPEG/PNG/WEBP;
- assinatura real validada;
- 5 MB;
- storage local configurável;
- ownership participante;
- leitura pública;
- principal + galeria.

Configuração:

```text
ROBOT_IMAGES_DIR
padrão: ./uploads/robots
```

Uso esperado nos clientes:

- portal participante: upload/galeria;
- operação Follow: foto do robô da tomada;
- arena Sumô: fotos dos dois adversários;
- landing futura: cards e resultados públicos.

## 5. Profile de demonstração

Ativar:

```powershell
$env:SPRING_PROFILES_ACTIVE="testdata"
.\mvnw spring-boot:run
```

`application-testdata.properties` habilita:

```text
bracket-history seed
follow-line seed
demo-showcase seed
```

### Usuários demo

```text
ORGANIZAÇÃO
organizacao.demo@rascomp.local
Rascomp@2026

PARTICIPANTE
lider.demo@rascomp.local
Rascomp@2026
```

### Edição ao vivo

```text
RRC 2026 · Demonstração ao vivo
```

Estado preparado:

- competição EM_ANDAMENTO;
- datas relativas a hoje para demonstrar progresso ~50%;
- aprovadas + 3 pendentes + 1 rejeitada;
- Follow com ranking pré-carregado;
- Chronos Demo com Tomadas 1 e 2 preenchidas e Tomada 3 livre;
- Sumô de 8 robôs parcialmente avançado;
- Titan Demo já venceu uma partida;
- exemplos de penalidade e Suicídio/WO;
- 3 kg com 10 robôs para demonstrar chave 16 + 6 BYEs;
- fotos demo de Chronos e Titan.

### Edição histórica

```text
RRC 2025 · Histórico completo
```

Estado preparado:

- FINALIZADA;
- 32 robôs;
- 31 partidas;
- 16 avos → oitavas → quartas → semifinal → final;
- resultados consolidados;
- exemplo de WO e penalidade.

## 6. Bateria de testes

Cobertura atual relevante:

```text
AccessPolicy / ownership                   ✅
UserAccount                                ✅
Team ownership                             ✅
Registration ownership                    ✅
Aprovação/rejeição + reviewer              ✅
CompetitionCategory                       ✅
Inspeção Sumô                              ✅
Bracket generation                         ✅
Bracket history / current                  ✅
Bracket 32 participantes                   ✅
Bracket 10 participantes / 6 BYEs          ✅
Match                                      ✅
MatchResult                                ✅
Round Sumô                                 ✅
Penalidade 0..2                            ✅
Suicídio/WO                                ✅
Fechamento por vitórias                    ✅
Bloqueio de round histórico                ✅
Tentativa Follow                           ✅
Duplicidade tomada/tentativa               ✅
Limites tomada/tentativa/checkpoint        ✅
Tempo > máximo → inválida                  ✅
Penalidade no tempo final                  ✅
Ranking melhor tomada                      ✅
```

## 7. API participante — extensão atual

Além da gestão de equipe/competidores/robôs/inscrições/fotos, o participante pode consultar o próprio histórico competitivo de Follow com ownership:

```text
GET /api/v1/participante/inscricoes/{registrationId}/tentativas-follow
GET /api/v1/participante/inscricoes/{registrationId}/config-follow
```

A visualização de Sumô pode usar as projeções públicas de chave/resultados, sem expor endpoints administrativos.

## 8. Próximos passos após a demonstração

Ordem recomendada:

```text
1. validar localmente o profile testdata
2. consolidar visual/técnico do ADMIN
3. confirmar regra oficial das penalidades do Sumô
4. confirmar impacto oficial de checkpoints Follow
5. concluir funcionalidades de participante
6. implementar fluxo real de convite/entrada em equipe
7. revisar upload/gestão de múltiplas fotos no participante
8. iniciar Landing Page RAS UFRB + RRC
```

## 9. Cuidados

- não criar regra competitiva no frontend;
- não misturar categorias de Sumô;
- não apagar chaves históricas ao regenerar;
- não permitir PARTICIPANTE alterar resultado oficial;
- não versionar uploads ou segredos;
- migrations futuras entram em `V8+`;
- seed `testdata` nunca deve ser habilitado em produção.
