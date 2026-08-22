# Rascomp — Plataforma de Gestão de Competições de Robótica

**Da inscrição ao pódio: operação, regras e resultados de competições de robôs em uma única plataforma.**

O Rascomp é uma solução fullstack em desenvolvimento para organizar competições de robótica, com foco inicial em **Sumô** e **Seguidor de Linha**. O sistema centraliza cadastros, inscrições, regras por categoria, inspeções, tentativas, rankings, chaveamentos, partidas e resultados em uma API preparada para alimentar um painel de gestão e uma vitrine pública.

> Projeto desenvolvido no contexto da RAS-UFRB, com backend Java/Spring Boot, persistência MySQL, migrations Flyway e Camunda 7 preparado para orquestração BPMN.

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

O objetivo é reduzir retrabalho operacional e oferecer uma fonte única de verdade para organização, competidores e público.

---

## Duas modalidades, dois fluxos de competição

### Seguidor de Linha

O Seguidor de Linha é decidido por **ranking de tempo**, não por confronto direto.

```text
Inscrição aprovada
   ↓
ConfigFollow
   ↓
3 tomadas
   ↓
até 3 tentativas por tomada
   ↓
registro de tempo + penalidade + checkpoints
   ↓
melhor tentativa válida do robô
   ↓
RankingFollowService
   ↓
menor tempo final vence
```

```text
tempo final = tempo bruto + penalidade
```

`FOLLOW_LINE` não utiliza `Bracket`, `Match` nem `MatchResult`.

### Sumô

O Sumô usa confronto eliminatório com inspeção e rounds:

```text
Inscrição aprovada
   ↓
Inspeção de peso / aptidão
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

A geração da chave considera apenas inscrições ativas, aprovadas e aptas para competir.

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
- ativação/inativação de registros.

### Regras por categoria

- `ConfigFollow` para número de tomadas, tentativas, tempo máximo e checkpoints;
- `ConfigSumo` para peso máximo, inspeção e configuração de rounds.

### Seguidor de Linha

- registro de tentativas;
- validação de tomada/tentativa/checkpoints;
- invalidação automática de tempo acima do limite;
- penalidades;
- ranking sob demanda;
- seleção da melhor tentativa válida por inscrição.

### Sumô

- inspeção de peso;
- limite de tentativas de inspeção;
- desclassificação automática após limite de reprovações;
- consulta de aptidão;
- geração automática de chave eliminatória;
- suporte a BYE;
- árvore completa de partidas;
- avanço automático de vencedor;
- rounds finalizados, empatados, anulados ou cancelados;
- criação automática de `MatchResult` ao atingir o número de vitórias necessário.

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

O Camunda já está operacional como infraestrutura embarcada. Os processos BPMN específicos do Rascomp entram depois do primeiro fluxo funcional do Frontend de Gestão.

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
- tabelas `ACT_*` persistidas no mesmo MySQL

### Qualidade

- JUnit 5
- Mockito
- GitHub Actions para suíte de testes da branch `testes-automatizados`
- bateria manual no Postman antes do congelamento da API

### API

- REST
- Springdoc/OpenAPI presente como dependência
- documentação Swagger planejada para **depois do congelamento do contrato**

---

## Estado do projeto

```text
Infraestrutura                  ✅ validada
CRUDs principais                ✅ implementados
Follow: tentativas              ✅ implementado
Follow: ranking                 ✅ implementado
Sumô: inspeção                  ✅ implementado
Sumô: chaveamento/BYE           ✅ implementado
Sumô: rounds                    ✅ implementado
Sumô: MatchResult automático    ✅ implementado
Bateria manual                  🧪 em andamento
Testes automatizados            🧪 branch dedicada
Frontend de Gestão              ⏳ próximo grande bloco
Camunda BPMN funcional          ⏳ após 1º fluxo de gestão
Frontend Público                ⏳ posterior
```

---

## Roadmap imediato

```text
Postman
   ↓
Testes automatizados
   ↓
Correção de bloqueadores
   ↓
Congelamento da API
   ↓
Swagger / OpenAPI
   ↓
Frontend de Gestão
   ↓
1º fluxo administrativo completo
   ↓
Camunda BPMN funcional
   ↓
Completar Gestão
   ↓
Frontend Público
   ↓
Autenticação/JWT e refinamentos
```

O primeiro processo BPMN planejado é a análise de inscrição:

```text
PENDENTE
  ↓
Análise administrativa
 ↙                     ↘
APROVADA             REJEITADA
```

---

## Execução local

Entre no módulo:

```powershell
cd rascomp
```

Configure no Eclipse ou no ambiente de execução:

```text
DB_USERNAME
DB_PASSWORD
```

`DB_URL` é opcional porque existe fallback local para o banco `rascomp`.

Também existe o fluxo local:

```powershell
.\run-local.ps1
```

Para preparar dados extras da bateria manual, habilite:

```text
RASCOMP_SEED_POSTMAN=true
```

---

## Documentação técnica

```text
rascomp/docs/CONTINUIDADE.md
rascomp/docs/TESTES_POSTMAN.md
rascomp/docs/ENDPOINTS_INTERNOS.md
rascomp/docs/JSON_EXEMPLOS.md
rascomp/docs/FLUXO_DO_SISTEMA.md
rascomp/docs/ENTIDADES_E_CRUDS.md
rascomp/docs/diagrama-uml-completo.puml
```

---

## Visão de produto

O Rascomp não pretende ser apenas um CRUD de campeonato. A arquitetura separa cadastro, regras de modalidade, execução da prova e classificação para permitir que a plataforma evolua para operação completa do evento, automação de processos, painel administrativo e acompanhamento público em tempo real.

O foco atual é transformar o backend já implementado em um **contrato estável e testado**, para então construir os frontends sem carregar inconsistências de domínio para a interface.
