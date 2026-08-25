<a id="readme-top"></a>

<div align="center">
  <a href="https://github.com/gbsalermo/Rascomp">
    <img src="rascomp/docs/LogoRAS.png" alt="Rascomp Logo" width="430" height="auto">
  </a>

  <h1 align="center">Rascomp — Gestão de Competições de Robótica</h1>

  <p align="center">
    <strong>Da inscrição ao pódio: usuários, equipes, robôs, competidores, inspeções, tentativas, rankings, chaveamentos e resultados em uma única plataforma.</strong>
  </p>

  <p align="center">
    <a href="rascomp/docs/FLUXO_DO_SISTEMA.md"><strong>Ver Fluxo do Sistema »</strong></a>
    ·
    <a href="rascomp/docs/CONTINUIDADE.md">Acompanhar Continuidade</a>
    ·
    <a href="rascomp/docs/CONGELAMENTO_API.md">Contrato da API</a>
    ·
    <a href="rascomp/docs/TESTES_POSTMAN.md">Roteiro de Testes</a>
  </p>

  <p align="center">
    <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
    <img src="https://img.shields.io/badge/Spring_Boot-3.5.3-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot">
    <img src="https://img.shields.io/badge/MySQL-Persistente-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
    <img src="https://img.shields.io/badge/Flyway-V1--V5-CC0200?style=for-the-badge&logo=flyway&logoColor=white" alt="Flyway">
    <img src="https://img.shields.io/badge/Security-JWT%20%2B%20BCrypt-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white" alt="JWT e BCrypt">
    <img src="https://img.shields.io/badge/Tests-28%20passed-brightgreen?style=for-the-badge&logo=junit5&logoColor=white" alt="Testes">
    <img src="https://img.shields.io/badge/CI-main%20passing-brightgreen?style=for-the-badge&logo=github-actions&logoColor=white" alt="CI passing">
    <img src="https://img.shields.io/badge/Camunda-7.22-FC5D0D?style=for-the-badge" alt="Camunda 7">
    <img src="https://img.shields.io/badge/Next-Swagger%20%2F%20OpenAPI-blueviolet?style=for-the-badge" alt="Próxima etapa Swagger">
  </p>
</div>

<br />

<div align="center">
  <a href="#-sobre-o-projeto">Sobre</a> •
  <a href="#-arquitetura">Arquitetura</a> •
  <a href="#-usuários-e-segurança">Usuários</a> •
  <a href="#-modalidades">Modalidades</a> •
  <a href="#-funcionalidades">Funcionalidades</a> •
  <a href="#-tecnologias">Tecnologias</a> •
  <a href="#-como-executar">Execução</a> •
  <a href="#-testes-e-qualidade">Testes</a> •
  <a href="#-estado-do-projeto">Status</a> •
  <a href="#-roadmap">Roadmap</a>
</div>

<br />

---

## 📌 Sobre o Projeto

O **Rascomp** é uma plataforma para gestão de competições de robótica desenvolvida no contexto da **IEEE Robotics & Automation Society — UFRB**.

O projeto centraliza o ciclo completo de uma competição:

```text
conta / login
     ↓
equipe responsável
 ┌───┼──────────────┐
 ↓   ↓              ↓
competidores      robôs
                    ↓
                  fotos
     │               │
     └──────┬────────┘
            ↓
        inscrição
            ↓
      análise da organização
            ↓
      modalidade da prova
        ↙           ↘
 FOLLOW_LINE        SUMO
      ↓              ↓
   ranking      chaveamento
        \           /
         resultados
```

O sistema atende dois clientes diferentes:

```text
Frontend de Gestão
├─ PARTICIPANTE
└─ ORGANIZACAO

Frontend Público / Landing
└─ resultados e informações sanitizadas
```

O backend é a fonte de verdade para autenticação, ownership, inscrições, execução competitiva e resultados.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## 🏛️ Arquitetura

```text
┌──────────────────────┐       ┌──────────────────────┐
│  Frontend de Gestão  │       │   Landing Pública    │
│ login + participante │       │ resultados / equipes │
│ + organização        │       │ robôs / chaveamentos │
└──────────┬───────────┘       └──────────┬───────────┘
           │ JWT                          │ sem login
           ▼                              ▼
/api/v1/participante/**            /api/v1/public/**
/api/v1/** ORGANIZACAO                     │
           └──────────────┬────────────────┘
                          ▼
                 Spring Boot REST API
                          │
       ┌──────────────────┼──────────────────┐
       ▼                  ▼                  ▼
    Services       Spring Security       Camunda 7
       │              JWT + BCrypt         Engine
       ▼
Spring Data JPA / Hibernate
       │
       ▼
      MySQL
       │
       └── Flyway V1 ... V5
```

Responsabilidades principais:

- **controllers**: contratos REST e status HTTP;
- **DTOs**: entrada/saída e exposição controlada;
- **services**: regras de domínio, ownership e transações;
- **repositories**: persistência Spring Data JPA;
- **security/config**: JWT, BCrypt e autorização;
- **Flyway**: evolução incremental do banco;
- **Camunda**: infraestrutura de orquestração, sem substituir regras competitivas Java.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## 🔐 Usuários e Segurança

O Rascomp possui dois papéis globais:

| Perfil | Responsabilidade |
|---|---|
| `PARTICIPANTE` | Gerenciar as próprias equipes, competidores, robôs, fotos e inscrições |
| `ORGANIZACAO` | Administrar a competição, inscrições, inspeções, provas, chaveamentos e resultados |

Responsabilidade por equipe é ownership:

```text
UserAccount(PARTICIPANTE)
        │
        └── Team.responsibleUser
```

Isso permite:

```text
Professor responsável  → UserAccount ✅ / Competitor ❌
Competidor líder        → UserAccount ✅ / Competitor ✅
Outro competidor        → UserAccount opcional / Competitor ✅
```

### Senhas

A aplicação não persiste a senha original:

```text
senha recebida
    ↓
BCryptPasswordEncoder(12)
    ↓
password_hash
```

`passwordHash` não é exposto por DTOs externos.

### Autenticação

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/me
```

Após login:

```http
Authorization: Bearer <JWT>
```

O segredo JWT é fornecido por `JWT_SECRET` e não deve ser versionado.

### Separação das APIs

```text
/api/v1/public/**       → público / read-only
/api/v1/participante/** → PARTICIPANTE + ownership
/api/v1/**              → ORGANIZACAO
/engine-rest/**         → ORGANIZACAO
```

A API pública usa DTOs próprios e não expõe dados sensíveis de competidores/usuários.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## 🤖 Modalidades

### Seguidor de Linha — `FOLLOW_LINE`

```text
Registration APROVADA
       ↓
ConfigFollow
       ↓
tomadas / tentativas
       ↓
tempo + penalidade + checkpoints
       ↓
melhor tentativa válida e concluída
       ↓
RankingFollowService
       ↓
classificação / campeão
```

```text
tempoFinal = tempoSegundos + penalidadeSegundos
```

`FOLLOW_LINE` não utiliza `Bracket`, `Match`, `RoundSumo` ou `MatchResult`.

### Sumô — `SUMO`

```text
Registration APROVADA
       ↓
InspecaoSumo
       ↓
aptidão
       ↓
Bracket
       ↓
Match
       ↓
RoundSumo
       ↓
MatchResult automático
       ↓
progressão / campeão
```

A chave considera somente inscrições ativas, aprovadas e aptas. `MatchResult` é read-only externamente e nasce da consolidação dos rounds.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## ✨ Funcionalidades

- [x] contas `PARTICIPANTE` e `ORGANIZACAO`;
- [x] autenticação JWT;
- [x] senha protegida por BCrypt;
- [x] ownership de equipe;
- [x] vínculo opcional `UserAccount ↔ Competitor`;
- [x] instituições, equipes, competidores e robôs;
- [x] upload de fotos de robôs;
- [x] validação real de JPEG/PNG/WEBP por assinatura do arquivo;
- [x] inscrições com competidores participantes;
- [x] autoria e revisão de inscrição;
- [x] API autenticada do participante;
- [x] API administrativa da organização;
- [x] API pública sanitizada;
- [x] ConfigFollow / ConfigSumo;
- [x] Follow Line com tentativas, checkpoints, penalidade e ranking;
- [x] Sumô com inspeção, bracket, BYE, partidas, rounds e progressão;
- [x] tratamento global de erros HTTP;
- [x] MySQL + Flyway;
- [x] Camunda 7 operacional como infraestrutura;
- [x] JUnit 5 + Mockito + GitHub Actions;
- [ ] Swagger/OpenAPI completo — **próxima etapa**;
- [ ] BPMN Rascomp funcional;
- [ ] Frontend de Gestão concluído;
- [ ] Landing Page concluída.

---

## 🛠️ Tecnologias

| Categoria | Tecnologia | Finalidade |
|---|---|---|
| Linguagem | Java 21 | Backend |
| Framework | Spring Boot 3.5.3 | API REST |
| Persistência | Spring Data JPA / Hibernate | ORM e transações |
| Banco | MySQL | Persistência principal |
| Migrations | Flyway | Versionamento do schema |
| Segurança | Spring Security | Autorização |
| Autenticação | JWT | Sessão stateless |
| Senhas | BCrypt | Hash irreversível |
| Validação | Jakarta Validation | Validação de payloads |
| Processos | Camunda 7.22 | Engine BPMN |
| Documentação | Springdoc OpenAPI 2.8.9 | Swagger/OpenAPI |
| Testes | JUnit 5 + Mockito | Testes automatizados |
| CI | GitHub Actions | `mvn -B test` |
| Build | Maven | Build e dependências |

### Migrations

```text
V1 — schema competitivo principal
V2 — inspeções de Sumô
V3 — rounds de Sumô
V4 — limpeza de artefatos legados de Follow em bracket
V5 — usuários, ownership, participantes da inscrição e fotos
```

Migrations aplicadas não são reescritas; novas mudanças estruturais usam `V6+`.

---

## 🚀 Como Executar

Entre no módulo:

```powershell
cd rascomp
```

Variáveis principais:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET
```

Opcionais:

```text
DB_URL
ROBOT_IMAGES_DIR
RASCOMP_SEED_POSTMAN=true

RASCOMP_ORG_NOME
RASCOMP_ORG_EMAIL
RASCOMP_ORG_PASSWORD
```

Também existe:

```powershell
.\run-local.ps1
```

> [!IMPORTANT]
> Credenciais e segredos devem permanecer fora do Git. `.env.local` e `/uploads/` são ignorados.

---

## 🧪 Testes e Qualidade

O núcleo competitivo passou por bateria manual completa de **Follow Line e Sumô**.

A arquitetura de acesso também foi testada manualmente em:

```text
Flyway V5                         ✅
PARTICIPANTE register/login       ✅
/auth/me                          ✅
401 sem autenticação              ✅
403 / ownership                   ✅
equipe / competidor / robô        ✅
upload de foto                    ✅
inscrição                         ✅
competidor de outra equipe        ✅ rejeitado
API pública sanitizada            ✅
```

Testes automatizados:

```text
28 testes
0 falhas
```

Após o merge do PR #4, o workflow **Backend Tests #43** executou novamente na `main` e terminou com **success**.

### Pendência manual de fechamento

Antes de declarar o backend definitivamente encerrado, repetir:

```text
login ORGANIZACAO
→ /auth/me = ORGANIZACAO
→ acesso administrativo
→ aprovar/rejeitar Registration
→ validar reviewedByUser + reviewedAt
```

Essa pendência não bloqueou o merge da arquitetura e será fechada junto da revisão Swagger/final.

---

## 🔄 Camunda

Infraestrutura atual:

```text
Process Engine   ✅
JobExecutor      ✅
tabelas ACT_*    ✅
REST starter     ✅
BPMN Rascomp     ⏳
```

Primeiro processo candidato:

```text
PARTICIPANTE envia inscrição
          ↓
       PENDENTE
          ↓
 tarefa da ORGANIZACAO
       ↙       ↘
 APROVADA     REJEITADA
```

Camunda deverá orquestrar processos/human tasks. Ranking, inspeção, rounds e chaveamentos continuam nos services Java.

---

## 📊 Estado do Projeto

```text
Núcleo competitivo Follow/Sumô      ✅
MySQL + Flyway V1–V5                 ✅
UserAccount                          ✅
PARTICIPANTE / ORGANIZACAO           ✅
JWT + BCrypt                         ✅
Ownership                            ✅
Fotos dos robôs                      ✅
Registration com participantes      ✅
API participante                     ✅
API pública sanitizada               ✅
PR #4                                ✅ mergeado
CI pós-merge                         ✅ Backend Tests #43
Congelamento da API                  ✅ renovado
Swagger / OpenAPI                    ⏳ PRÓXIMA ETAPA
Camunda BPMN funcional               ⏳ pós-Swagger
Frontend de Gestão                   🔄 paralelo
Frontend Público / Landing           🔄 paralelo
```

---

## 🗺️ Roadmap

```text
NÚCLEO COMPETITIVO                  ✅
          ↓
USUÁRIOS + JWT + OWNERSHIP          ✅
          ↓
SMOKE LOCAL                         ✅
          ↓
MERGE PR #4                         ✅
          ↓
CI PÓS-MERGE                        ✅
          ↓
NOVO CONGELAMENTO                   ✅
          ↓
SWAGGER / OPENAPI                   ◀ PRÓXIMO
          ↓
REVALIDAR ORGANIZACAO
          ↓
REVISÃO FINAL DO BACKEND
          ↓
CHECKPOINT CAMUNDA
          ↓
FRONTEND DE GESTÃO + LANDING
```

O contrato atual está registrado em [`CONGELAMENTO_API.md`](rascomp/docs/CONGELAMENTO_API.md).

---

## 📚 Documentação Técnica

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

`CONTINUIDADE.md` é a fonte principal para retomada do projeto por outra sessão/agente.

---

## 🧭 Forma de Trabalho

```text
objetivo
  ↓
implementação delegada
  ↓
revisão do contrato
  ↓
CI
  ↓
smoke objetivo
  ↓
documentação
  ↓
merge
```

A prioridade do Rascomp é manter **domínio, contrato, segurança e qualidade verificáveis** enquanto os dois frontends avançam em paralelo.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>