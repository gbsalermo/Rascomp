# RasComp — Gestão de Competições de Robótica

Backend da plataforma **RasComp**, desenvolvida para apoiar a organização e a divulgação das competições de robótica da **RAS UFRB**. O evento é o **RRC**; RasComp é o sistema.

A API cobre o ciclo competitivo completo: contas, equipes, competidores, robôs, fotos, inscrições, análise da organização, Follow Line, Sumô, chaveamentos, resultados, histórico e projeções públicas.

## Estado atual

- Java 21 + Spring Boot 3.5.3
- MySQL persistente
- Flyway **V1–V7**
- Spring Security + JWT + BCrypt
- ownership real para participantes
- API administrativa, API de participante e API pública separadas
- fotos de robôs com armazenamento local configurável
- Follow Line com tomadas, tentativas e ranking backend-first
- Sumô com inspeção, BYE, chaveamento, rounds, penalidades e progressão automática
- histórico versionado de chaves
- **45 testes automatizados, 0 falhas, 0 erros** na CI da `main`

## Arquitetura de acesso

```text
/api/v1/public/**
└─ leitura pública sanitizada

/api/v1/participante/**
└─ PARTICIPANTE + ownership da equipe

/api/v1/**
└─ ORGANIZACAO
```

O backend é a fonte de verdade para elegibilidade, ranking, progressão, vencedores e resultados oficiais.

## Follow Line

```text
Robô / inscrição
├─ Tomada 1
│  ├─ Tentativa 1
│  ├─ Tentativa 2
│  └─ Tentativa 3
├─ Tomada 2
└─ ...
```

Cada tentativa pode registrar tempo bruto, penalidade, checkpoints, conclusão, validade e observação.

```text
tempoFinal = tempoSegundos + penalidadeSegundos
```

A classificação segue explicitamente:

```text
tentativas válidas + concluídas
        ↓
melhor tentativa de cada tomada
        ↓
melhor tomada do robô
        ↓
ranking
```

`checkpointsAlcancados` permanece como dado operacional. Até confirmação da regra oficial, checkpoints não alteram o ranking.

## Sumô

As categorias competitivas permanecem independentes por `categoryId`. Mini/3 kg e RC/Autônomo podem existir como categorias diferentes sem criar motores de regra diferentes.

```text
Registration APROVADA
        ↓
Inspeção apta
        ↓
Bracket
        ↓
Match
        ↓
RoundSumo
        ↓
MatchResult automático
        ↓
progressão
```

### Regras suportadas

- quantidade configurável de rounds;
- vitórias necessárias para fechar a batalha;
- round extra quando permitido;
- BYE automático;
- `SUICIDIO_WO`: perda do round pelo robô que sofreu a ocorrência;
- penalidades separadas para robô A e B;
- limite provisório: **2 penalidades por robô/round**;
- atingir 2 penalidades ainda não produz consequência automática;
- partidas históricas são read-only.

### Histórico de chaves

```text
nova geração
   ↓
nova chave      atual=true
chave anterior  atual=false
```

Partidas, rounds e resultados anteriores permanecem preservados.

## Fotos dos robôs

O backend possui fluxo completo de imagens:

- JPEG / PNG / WEBP;
- validação da assinatura real do arquivo;
- até 5 MB;
- múltiplas fotos;
- uma principal;
- ownership no portal participante;
- leitura pública;
- armazenamento configurável por `ROBOT_IMAGES_DIR`.

Padrão local:

```text
./uploads/robots
```

## Migrations

```text
V1 — schema competitivo principal
V2 — inspeções de Sumô
V3 — rounds de Sumô
V4 — limpeza de artefatos legados
V5 — usuários, ownership, participantes e fotos
V6 — histórico/versionamento de chaveamentos
V7 — motivo do round e penalidades de Sumô
```

Migrations aplicadas não devem ser reescritas; mudanças futuras entram em `V8+`.

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
```

## Profile de demonstração

Existe um profile opt-in para apresentações e validação visual. Ele não roda em execução normal.

```powershell
$env:SPRING_PROFILES_ACTIVE="testdata"
.\mvnw spring-boot:run
```

### Credenciais de demonstração

```text
ORGANIZAÇÃO
organizacao.demo@rascomp.local
Rascomp@2026

PARTICIPANTE
lider.demo@rascomp.local
Rascomp@2026
```

### Cenário ao vivo

**RRC 2026 · Demonstração ao vivo**

As datas são relativas ao dia da execução para o evento aparecer em andamento aproximadamente no meio do período.

Inclui:

- inscrições aprovadas;
- 3 inscrições pendentes para aprovação ao vivo;
- inscrição rejeitada;
- Follow com ranking pré-estabelecido;
- `Chronos Demo` com **2 de 3 tomadas preenchidas** e a terceira disponível;
- Mini Sumô com chave parcialmente executada;
- `Titan Demo` com vitória registrada;
- round com penalidade;
- round por Suicídio/WO;
- categoria 3 kg com **10 participantes**, gerando chave de 16 e **6 BYEs**;
- fotos locais de Chronos e Titan.

### Cenário histórico

**RRC 2025 · Histórico completo**

- competição finalizada;
- 32 robôs;
- chave completa com 31 partidas;
- 16 avos, oitavas, quartas, semifinal e final;
- resultados preservados;
- exemplos de Suicídio/WO e penalidade.

> O initializer é idempotente. A inicialização do profile contra o MySQL local deve ser validada na máquina de demonstração antes da apresentação.

## Testes automatizados

A CI executa:

```powershell
.\mvnw test
```

Estado validado na `main` em **26/08/2026**:

```text
Tests run: 45
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Cobertura relevante:

- ownership e política de acesso;
- usuários/autenticação;
- aprovação e rejeição de inscrições + auditoria;
- configuração de categorias;
- inspeção de Sumô;
- geração/regeneração de chaves;
- chave de 32 participantes;
- chave de 10 participantes com BYEs;
- proteção de chave histórica;
- rounds e resultados de Sumô;
- penalidades;
- Suicídio/WO;
- fechamento de batalha;
- limites/duplicidade de tentativas Follow;
- tempo acima do limite;
- penalidade no tempo final;
- melhor tentativa da tomada e melhor tomada do ranking.

## Documentação

- `rascomp/docs/CONTINUIDADE.md` — estado e próximos passos
- `rascomp/docs/FLUXO_DO_SISTEMA.md` — fluxo de domínio
- `rascomp/docs/ENTIDADES_E_CRUDS.md` — entidades e contratos
- `rascomp/docs/CONGELAMENTO_API.md` — decisões de API
- `rascomp/docs/TESTES_POSTMAN.md` — validações manuais
- Swagger local: `http://localhost:8080/swagger-ui/index.html`

## Próximas frentes

1. validar o profile `testdata` contra o MySQL da máquina de apresentação;
2. consolidar regras oficiais restantes de penalidades/checkpoints;
3. concluir o portal participante;
4. consolidar o frontend ADMIN;
5. desenvolver a landing pública RAS UFRB + RRC.
