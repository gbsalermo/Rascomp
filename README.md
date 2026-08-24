# Rascomp — Plataforma de Gestão de Competições de Robótica

**Da inscrição ao pódio: operação, regras e resultados de competições de robôs em uma única plataforma.**

O Rascomp é uma solução fullstack para organizar competições de robótica, com foco inicial em **Sumô** e **Seguidor de Linha**. O sistema centraliza cadastros, inscrições, regras por categoria, inspeções, tentativas, rankings, chaveamentos, partidas e resultados em uma API preparada para alimentar um painel de gestão e uma vitrine pública.

> Projeto desenvolvido no contexto da RAS-UFRB, com backend Java/Spring Boot, persistência MySQL, migrations Flyway, suíte automatizada e Camunda 7 preparado para orquestração BPMN.

---

## O problema

Competições de robótica frequentemente espalham informações entre formulários, planilhas, mensagens e controles manuais. O Rascomp transforma esse processo em um domínio único e rastreável:

```text
Instituições
   ↓
Equipes
   ↓
Competidores e robôs
   ↓
Inscrições
   ↓
Regras da modalidade
   ↓
Execução da prova
   ↓
Classificação e resultados
```

---

## Duas modalidades, dois fluxos

### Seguidor de Linha

```text
Inscrição aprovada
   ↓
ConfigFollow
   ↓
3 tomadas
   ↓
até 3 tentativas por tomada
   ↓
tempo + penalidade + checkpoints
   ↓
melhor tentativa válida e concluída
   ↓
RankingFollowService
   ↓
menor tempo final vence
```

```text
tempo final = tempo bruto + penalidade
```

`FOLLOW_LINE` não utiliza `Bracket`, `Match`, `MatchResult` ou `RoundSumo`.

### Sumô

```text
Inscrição aprovada
   ↓
Inspeção / aptidão
   ↓
Chaveamento
   ↓
Partida
   ↓
Rounds
   ↓
MatchResult automático
   ↓
avanço do vencedor
   ↓
campeão
```

A geração da chave considera apenas inscrições ativas, aprovadas e aptas.

---

## Funcionalidades implementadas

### Gestão estrutural

- instituições;
- equipes;
- competidores;
- robôs;
- competições;
- categorias;
- inscrições;
- ativação/inativação.

### Regras por categoria

- `ConfigFollow`;
- `ConfigSumo`.

### Seguidor de Linha

- registro e consulta de tentativas;
- 3 tomadas × 3 tentativas no cenário padrão;
- validação de tomada/tentativa/checkpoints;
- invalidação automática por tempo máximo;
- penalidades;
- ranking;
- seleção da melhor tentativa válida e concluída;
- proteção contra uso indevido de chaveamento.

### Sumô

- inspeção de peso;
- limite de tentativas de inspeção;
- desclassificação automática;
- consulta de aptidão;
- chave eliminatória;
- suporte a BYE;
- árvore de partidas;
- avanço automático;
- rounds;
- consolidação automática de `MatchResult`;
- encerramento automático de partida e bracket;
- `MatchResult` somente leitura na API externa.

---

## Arquitetura atual

```text
Frontend de Gestão        Frontend Público
        \                    /
         \                  /
             REST API
                ↓
        Spring Boot 3.5.3
          /           \
     JPA/Hibernate   Camunda 7
          \           /
             MySQL
                ↓
             Flyway
```

O Camunda está operacional como infraestrutura embarcada. A decisão sobre o momento de ativar BPMN funcional será tomada após a etapa Swagger/OpenAPI.

---

## Stack

### Backend

- Java 21
- Spring Boot 3.5.3
- Spring Web
- Spring Data JPA / Hibernate
- Jakarta Validation
- Spring Security em configuração de desenvolvimento
- Lombok
- Maven

### Persistência

- MySQL
- HikariCP
- Flyway

Migrations atuais:

```text
V1 — schema principal
V2 — inspeções de Sumô
V3 — rounds de Sumô
V4 — limpeza de chaveamentos legados de FOLLOW_LINE
```

### Processos

- Camunda 7.22 embarcado
- Process Engine validado
- JobExecutor validado
- tabelas `ACT_*` persistidas no MySQL
- REST starter presente
- BPMN Rascomp ainda não implementado

### Qualidade

- JUnit 5
- Mockito
- GitHub Actions
- bateria manual de regressão
- branch de testes automatizados mergeada na `main`

### API

- REST
- Springdoc OpenAPI 2.8.9
- contrato funcional congelado em 23/08/2026
- Swagger/OpenAPI é a etapa atual

---

## Estado do projeto

```text
Infraestrutura                  ✅ validada
CRUDs principais                ✅ implementados
FOLLOW_LINE                     ✅ validado manualmente
SUMO                            ✅ validado manualmente
Testes automatizados            ✅ validados
GitHub Actions                  ✅ validado
Branch testes-automatizados     ✅ mergeada
Contrato da API                 🔒 congelado
Swagger / OpenAPI               ▶ etapa atual
Camunda BPMN funcional          ⏳ decisão pós-Swagger
Frontend de Gestão              ⏳
Frontend Público                ⏳
```

---

## Roadmap imediato

```text
BACKEND VALIDADO E CONGELADO ✅
            ↓
SWAGGER / OPENAPI           ▶
            ↓
CHECKPOINT ARQUITETURAL
   ├─ Camunda agora?
   ├─ Frontend de Gestão primeiro?
   └─ BPMN pós-MVP?
            ↓
CAMINHO ESCOLHIDO
            ↓
Frontend de Gestão
            ↓
Frontend Público
            ↓
Autenticação/JWT e refinamentos
```

O primeiro processo BPMN candidato continua sendo:

```text
PENDENTE
  ↓
análise administrativa
 ↙                     ↘
APROVADA             REJEITADA
```

As regras competitivas permanecem nos services Java; Camunda deverá orquestrar processos, não substituir o domínio validado.

---

## Execução local

Entre no módulo:

```powershell
cd rascomp
```

Configure no Eclipse ou ambiente de execução:

```text
DB_USERNAME
DB_PASSWORD
```

`DB_URL` é opcional porque existe fallback local para o banco `rascomp`.

Também existe:

```powershell
.\run-local.ps1
```

Para cenários extras de regressão manual:

```text
RASCOMP_SEED_POSTMAN=true
```

---

## Documentação técnica

```text
rascomp/docs/CONTINUIDADE.md
rascomp/docs/CONGELAMENTO_API.md
rascomp/docs/TESTES_POSTMAN.md
rascomp/docs/ENDPOINTS_INTERNOS.md
rascomp/docs/JSON_EXEMPLOS.md
rascomp/docs/FLUXO_DO_SISTEMA.md
rascomp/docs/ENTIDADES_E_CRUDS.md
rascomp/docs/diagrama-uml-completo.puml
```

`CONTINUIDADE.md` é a fonte principal do estado do projeto. `CONGELAMENTO_API.md` define o que pode e não pode mudar durante Swagger.

---

## Forma de execução

A partir do congelamento do backend, o projeto é conduzido por orquestração:

```text
planejar
 -> delegar implementação
 -> revisar contrato/diff
 -> validar por teste
 -> atualizar documentação
 -> avançar
```

A documentação deve permitir que uma IA, agente ou colaborador execute uma etapa sem depender de contexto informal anterior.

---

## Marco atual

O backend deixou de estar em fase de construção de regras e entrou em fase de **documentação formal do contrato via Swagger/OpenAPI**.
