<a id="readme-top"></a>

<!-- HERO SECTION -->
<div align="center">
  <a href="https://github.com/gbsalermo/Rascomp">
    <img src="docs/LOGO-RASCOMP.webp" alt="Rascomp Logo" width="720" height="auto">
  </a>

  <h1 align="center">Rascomp — Gestão de Competições de Robótica</h1>

  <p align="center">
    <strong>Da inscrição ao pódio: equipes, robôs, competidores, autenticação, inspeções, tentativas, rankings, chaveamentos e resultados em uma única plataforma.</strong>
  </p>

  <p align="center">
    <a href="rascomp/docs/FLUXO_DO_SISTEMA.md"><strong>Ver Fluxo do Sistema »</strong></a>
    ·
    <a href="rascomp/docs/CONTINUIDADE.md">Acompanhar Continuidade</a>
    ·
    <a href="rascomp/docs/TESTES_POSTMAN.md">Roteiro de Testes</a>
  </p>

  <!-- BADGES -->
  <p align="center">
    <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
    <img src="https://img.shields.io/badge/Spring_Boot-3.5.3-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot">
    <img src="https://img.shields.io/badge/MySQL-Persistente-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
    <img src="https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white" alt="Flyway">
    <img src="https://img.shields.io/badge/Security-JWT%20%2B%20BCrypt-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white" alt="JWT e BCrypt">
    <img src="https://img.shields.io/badge/Tests-28%20passed-brightgreen?style=for-the-badge&logo=junit5&logoColor=white" alt="Testes">
    <img src="https://img.shields.io/badge/Camunda-7.22-FC5D0D?style=for-the-badge" alt="Camunda 7">
    <img src="https://img.shields.io/badge/Status-Validação%20Final-blueviolet?style=for-the-badge" alt="Status">
  </p>
</div>

<br />

<div align="center">
  <a href="#-sobre-o-projeto">Sobre</a> •
  <a href="#-arquitetura">Arquitetura</a> •
  <a href="#-usuários-e-segurança">Usuários</a> •
  <a href="#-modalidades">Modalidades</a> •
  <a href="#-principais-funcionalidades">Funcionalidades</a> •
  <a href="#-tecnologias">Tecnologias</a> •
  <a href="#-como-executar">Execução</a> •
  <a href="#-estado-do-projeto">Status</a> •
  <a href="#-roadmap">Roadmap</a>
</div>

<br />

---

<details>
  <summary>📋 <strong>Tabela de Conteúdos</strong></summary>
  <ol>
    <li><a href="#-sobre-o-projeto">Sobre o Projeto</a></li>
    <li><a href="#-arquitetura">Arquitetura</a></li>
    <li><a href="#-usuários-e-segurança">Usuários e Segurança</a></li>
    <li><a href="#-modalidades">Modalidades</a></li>
    <li><a href="#-principais-funcionalidades">Principais Funcionalidades</a></li>
    <li><a href="#-tecnologias">Tecnologias</a></li>
    <li><a href="#-como-executar">Como Executar</a></li>
    <li><a href="#-testes-e-qualidade">Testes e Qualidade</a></li>
    <li><a href="#-camunda">Camunda</a></li>
    <li><a href="#-estado-do-projeto">Estado do Projeto</a></li>
    <li><a href="#-roadmap">Roadmap</a></li>
    <li><a href="#-documentação-técnica">Documentação Técnica</a></li>
  </ol>
</details>

---

## 📌 Sobre o Projeto

O **Rascomp** é uma plataforma para gestão de competições de robótica desenvolvida no contexto da **IEEE Robotics & Automation Society — UFRB**.

O projeto centraliza o ciclo completo de uma competição: usuários, equipes, competidores, robôs, inscrições, regras por categoria, inspeções, execução das provas, rankings, chaveamentos e resultados.

> [!NOTE]
> O Rascomp foi desenhado para atender dois públicos diferentes: o **participante**, que cadastra e acompanha sua equipe, e a **organização**, que administra a competição. Um segundo frontend público funcionará como vitrine de resultados, equipes, robôs, rankings e chaveamentos sem expor dados sensíveis.

### 💡 O problema e a solução

Competições de robótica costumam espalhar informações entre formulários, planilhas, mensagens e controles manuais. Isso dificulta auditoria, validação de inscrições, organização de categorias, acompanhamento das provas e publicação dos resultados.

O Rascomp transforma esse fluxo em um domínio único e rastreável:

```text
Usuário / responsável
        ↓
      Equipe
   ┌────┼────┐
   ↓    ↓    ↓
Competidores Robôs Inscrições
             │       │
             │       └── competição + categoria + participantes
             └── fotos

Inscrição aprovada
        ↓
   regra da modalidade
        ↓
 execução da competição
        ↓
 classificação / campeão
```

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## 🏛️ Arquitetura

O backend segue arquitetura em camadas com regras de domínio concentradas em services e contratos REST separados por finalidade.

```text
┌─────────────────────┐      ┌─────────────────────┐
│ Frontend de Gestão  │      │  Frontend Público   │
│ login + operação    │      │ landing / resultados│
└──────────┬──────────┘      └──────────┬──────────┘
           │ JWT                         │ sem login
           ▼                             ▼
 /api/v1/participante/**          /api/v1/public/**
 /api/v1/** ORGANIZACAO                  │
           └──────────────┬──────────────┘
                          ▼
                  Spring Boot REST API
                          │
       ┌──────────────────┼──────────────────┐
       ▼                  ▼                  ▼
   Services        Spring Security       Camunda 7
       │             JWT + BCrypt          engine
       ▼
 Spring Data JPA / Hibernate
       │
       ▼
      MySQL
       │
       └── Flyway V1...V5
```

Principais responsabilidades:

- **`controller`**: endpoints REST e status HTTP;
- **`dto`**: contratos de entrada/saída e exposição controlada de dados;
- **`service`**: regras de domínio, ownership, autenticação e transações;
- **`model`**: entidades JPA;
- **`repository`**: persistência Spring Data JPA;
- **`security` / `config`**: JWT, filtros, autorização e BCrypt;
- **`exception`**: erros HTTP padronizados;
- **Flyway**: evolução incremental do schema;
- **Camunda**: infraestrutura de orquestração de processos humanos, sem substituir regras competitivas Java.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## 🔐 Usuários e Segurança

O sistema possui **dois papéis globais**:

| Perfil | Responsabilidade |
|---|---|
| `PARTICIPANTE` | Cadastro/login, gestão das próprias equipes, competidores, robôs, fotos e inscrições |
| `ORGANIZACAO` | Administração da competição, inscrições, categorias, inspeções, provas, chaveamentos e resultados |

Responsabilidade por equipe não é uma role global. É ownership:

```text
UserAccount(PARTICIPANTE)
        │
        └── Team.responsibleUser
```

Isso permite cenários como:

```text
Professor responsável  → UserAccount ✅ / Competitor ❌
Competidor líder        → UserAccount ✅ / Competitor ✅
Competidor da equipe    → UserAccount opcional / Competitor ✅
```

### Senhas

A senha nunca é persistida em texto puro:

```text
senha recebida
    ↓
BCryptPasswordEncoder(12)
    ↓
password_hash
```

O hash não é exposto em DTOs de resposta.

### Autenticação

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/me
```

Após o login:

```http
Authorization: Bearer <JWT>
```

O segredo JWT é externo ao repositório através de `JWT_SECRET`.

### Separação das APIs

```text
/api/v1/public/**       → público e somente leitura
/api/v1/participante/** → PARTICIPANTE + ownership
/api/v1/**              → ORGANIZACAO
/engine-rest/**         → ORGANIZACAO
```

A API pública utiliza DTOs sanitizados e não expõe senha/hash, telefone/e-mail de competidores ou informações administrativas.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## 🤖 Modalidades

### Seguidor de Linha — `FOLLOW_LINE`

```text
Registration APROVADA
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
tempoFinal = tempoSegundos + penalidadeSegundos
```

`FOLLOW_LINE` **não utiliza** `Bracket`, `Match`, `MatchResult` ou `RoundSumo`.

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
avanço do vencedor
       ↓
campeão
```

A chave considera apenas inscrições ativas, aprovadas e aptas. `MatchResult` é somente leitura na API externa e é consolidado automaticamente pelos rounds.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## ✨ Principais Funcionalidades

- [x] **Contas de usuário** com `PARTICIPANTE` e `ORGANIZACAO`;
- [x] **Autenticação JWT** e senhas protegidas com BCrypt;
- [x] **Ownership de equipe** por responsável autenticado;
- [x] **Vínculo opcional UserAccount ↔ Competitor**;
- [x] **Instituições, equipes, competidores e robôs**;
- [x] **Fotos de robôs** com upload multipart e armazenamento de arquivo fora do banco;
- [x] **Inscrições com competidores participantes**, autor da solicitação, revisor e data de análise;
- [x] **Portal do participante** restrito às próprias equipes;
- [x] **API administrativa** para a organização;
- [x] **API pública sanitizada** para landing page e resultados;
- [x] **ConfigFollow / ConfigSumo** por categoria;
- [x] **Follow Line** com tomadas, tentativas, checkpoints, penalidade e ranking;
- [x] **Sumô** com inspeção, aptidão, bracket, BYE, partidas, rounds e progressão automática;
- [x] **Tratamento global de erros HTTP**;
- [x] **MySQL persistente + Flyway**;
- [x] **Camunda 7** operacional como infraestrutura;
- [x] **JUnit 5 + Mockito + GitHub Actions**;
- [ ] **Swagger/OpenAPI documentado** — próxima etapa após o smoke da nova arquitetura;
- [ ] **BPMN Rascomp funcional**;
- [ ] **Frontend de Gestão**;
- [ ] **Frontend Público / Landing Page**.

---

## 🛠️ Tecnologias

| Categoria | Tecnologia | Finalidade |
|---|---|---|
| Linguagem | Java 21 | Base do backend |
| Framework | Spring Boot 3.5.3 | API REST e configuração |
| Persistência | Spring Data JPA / Hibernate | ORM e transações |
| Banco | MySQL | Persistência principal |
| Migrations | Flyway | Evolução incremental do schema |
| Segurança | Spring Security | Autorização por perfil |
| Autenticação | JWT | Sessão stateless |
| Senhas | BCrypt | Hash irreversível de senha |
| Validação | Jakarta Validation | Validação de payloads |
| Processos | Camunda 7.22 | Orquestração BPMN futura |
| Documentação | Springdoc OpenAPI 2.8.9 | Swagger/OpenAPI — próxima etapa |
| Testes | JUnit 5 + Mockito | Testes automatizados |
| CI | GitHub Actions | `mvn -B test` em Java 21 |
| Build | Maven | Dependências e build |

### Migrations

```text
V1 — schema competitivo principal
V2 — inspeções de Sumô
V3 — rounds de Sumô
V4 — limpeza de chaveamentos legados de FOLLOW_LINE
V5 — usuários, ownership, auditoria de inscrição e fotos de robôs
```

Migrations aplicadas não são alteradas; mudanças futuras usam `V6+`.

---

## 🚀 Como Executar

Entre no módulo:

```powershell
cd rascomp
```

Configure as variáveis de ambiente:

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
```

Também existe o script:

```powershell
.\run-local.ps1
```

> [!IMPORTANT]
> Segredos e credenciais permanecem fora do Git. `.env.local` e `/uploads/` são ignorados pelo repositório.

---

## 🧪 Testes e Qualidade

O núcleo competitivo passou por bateria manual completa de **Follow Line e Sumô** antes da inclusão da camada de identidade e segurança.

A branch de arquitetura adicionou testes específicos para:

- BCrypt sem persistir senha em texto puro;
- usuários e perfis;
- ownership de equipe;
- acesso a recursos de outra equipe;
- criação de inscrição pelo participante;
- competidores pertencentes à equipe inscrita;
- regressão das regras já existentes de Follow e Sumô.

Última execução automatizada da arquitetura:

```text
28 testes
0 falhas
GitHub Actions: SUCCESS
```

Ainda falta o **smoke local com MySQL/Flyway V5 + JWT + upload**, antes do merge do PR #4.

---

## 🔄 Camunda

Estado atual:

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

As regras competitivas continuam nos services Java. Camunda deverá **orquestrar processo humano**, não calcular ranking, inspeção, rounds ou chaveamentos.

---

## 📊 Estado do Projeto

```text
Núcleo competitivo Follow/Sumô      ✅ validado
MySQL + Flyway V1–V4                 ✅ validado
Testes automatizados anteriores      ✅

Arquitetura UserAccount              ✅ implementada
PARTICIPANTE / ORGANIZACAO           ✅
JWT + BCrypt                         ✅
Ownership de equipe                  ✅
Fotos dos robôs                      ✅
Registration com participantes      ✅
API participante                     ✅
API pública sanitizada               ✅
Migration V5                         ✅
CI da nova arquitetura               ✅ 28 testes
Smoke local V5/JWT/upload            ⏳ próximo checkpoint
PR #4                                🔄 draft
Novo congelamento da API             ⏳
Swagger / OpenAPI                    ⏳ próxima etapa
Camunda BPMN funcional               ⏳ checkpoint pós-Swagger
Frontend de Gestão                   ⏳ trabalho paralelo
Frontend Público / Landing           ⏳ trabalho paralelo
```

---

## 🗺️ Roadmap

```text
NÚCLEO COMPETITIVO VALIDADO ✅
             ↓
ARQUITETURA DE USUÁRIOS     ✅ implementação
             ↓
CI                          ✅
             ↓
SMOKE LOCAL V5/JWT/UPLOAD   ◀ PRÓXIMO
             ↓
MERGE PR #4
             ↓
NOVO CONGELAMENTO DA API
             ↓
SWAGGER / OPENAPI
             ↓
CHECKPOINT
   ├─ BPMN mínimo de inscrição?
   ├─ integrar Camunda com Gestão?
   └─ priorizar MVP visual?
             ↓
FRONTEND DE GESTÃO + LANDING
             ↓
ENTREGA DO MVP
```

O frontend pode avançar em paralelo, mas o contrato definitivo só deve ser considerado fechado depois do smoke da V5 e do merge da arquitetura.

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

`CONTINUIDADE.md` é a fonte principal do estado do projeto e deve ser lido antes de uma nova etapa ou por qualquer IA/agente que assuma a implementação.

---

## 🧭 Forma de Trabalho

O projeto é conduzido por orquestração:

```text
objetivo
  ↓
implementação delegada
  ↓
revisão de arquitetura e contrato
  ↓
CI
  ↓
smoke objetivo
  ↓
documentação
  ↓
merge
```

A prioridade é manter **contrato, domínio e qualidade verificáveis**, independentemente de quem executa o código.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>
