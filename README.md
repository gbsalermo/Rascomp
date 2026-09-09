<a id="readme-top"></a>

<div align="center">
  <a href="https://github.com/gbsalermo/Rascomp">
    <img src="rascomp/docs/LogoRAS.png" alt="RasComp" width="430">
  </a>

  <h1 align="center">RasComp — Gestão de Competições de Robótica</h1>

  <p align="center">
    <strong>Backend da plataforma que centraliza inscrições, equipes, robôs, provas, rankings, chaveamentos e resultados do RRC.</strong>
  </p>

  <p align="center">
    <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
    <img src="https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot">
    <img src="https://img.shields.io/badge/MySQL-Persistência-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
    <img src="https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white" alt="Flyway">
    <img src="https://img.shields.io/badge/Security-JWT%20%2B%20BCrypt-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white" alt="JWT e BCrypt">
    <img src="https://img.shields.io/badge/OpenAPI-Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="Swagger OpenAPI">
  </p>

  <p align="center">
    <a href="#-sobre-o-projeto">Sobre</a> •
    <a href="#-arquitetura">Arquitetura</a> •
    <a href="#-funcionalidades">Funcionalidades</a> •
    <a href="#-modalidades">Modalidades</a> •
    <a href="#-apis-e-segurança">APIs</a> •
    <a href="#-tecnologias">Tecnologias</a> •
    <a href="#-como-executar">Execução</a> •
    <a href="#-documentação">Documentação</a>
  </p>
</div>

---

## 📌 Sobre o Projeto

O **RasComp** é uma plataforma desenvolvida para apoiar a organização e a divulgação das competições de robótica promovidas no contexto da **IEEE Robotics & Automation Society — UFRB**.

O sistema transforma o ciclo competitivo em um fluxo único e rastreável, reduzindo a dependência de formulários, planilhas e controles paralelos.

```text
Responsável / participante
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
execução da prova
        ↓
ranking / chave / campeão
```

O backend é a **fonte de verdade** para autorização, ownership, elegibilidade, inscrições, inspeções, rankings, resultados, BYEs, progressão de chave e campeões.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## 🏛️ Arquitetura

O backend segue uma arquitetura em camadas com regras de domínio concentradas nos services e contratos REST separados por finalidade.

```text
┌──────────────────────┐      ┌──────────────────────┐
│ Frontend de Gestão   │      │ Frontend Público     │
│ organização/portal   │      │ landing/resultados   │
└──────────┬───────────┘      └──────────┬───────────┘
           │ JWT                          │ leitura pública
           ▼                              ▼
 /api/v1/**                        /api/v1/public/**
 /api/v1/participante/**                  │
           └──────────────┬───────────────┘
                          ▼
                   Spring Boot REST API
                          │
                 Controllers + DTOs
                          │
                       Services
                          │
                 Spring Data JPA
                          │
                      Hibernate
                          │
                        MySQL
                          │
                       Flyway
```

### Responsabilidades principais

- **Controller** — endpoints REST, contratos HTTP e status de resposta;
- **DTO** — entrada e saída controladas da API;
- **Service** — regras de negócio, ownership, validações e transações;
- **Repository** — persistência com Spring Data JPA;
- **Model** — entidades do domínio competitivo;
- **Security** — autenticação JWT, BCrypt e autorização;
- **Exception handling** — respostas de erro padronizadas;
- **Flyway** — evolução versionada do banco de dados.

O RasComp não depende de Camunda para executar suas regras competitivas. Os fluxos de Follow Line, Sumô, inscrições e chaveamentos são controlados diretamente pelo domínio Java/Spring.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## ✨ Funcionalidades

### Gestão competitiva

- [x] Competições e categorias;
- [x] Equipes, competidores e robôs;
- [x] Fotos de robôs;
- [x] Inscrições e revisão pela organização;
- [x] Cancelamento e reativação controlados;
- [x] Follow Line com tomadas, tentativas e ranking;
- [x] Registro auditável de ausência em tomada;
- [x] Sumô com inspeção, partidas e rounds;
- [x] Chaveamento, BYE e progressão automática;
- [x] Histórico de chaves;
- [x] Penalidades e ocorrências de round;
- [x] Decisão de juiz identificada e justificada;
- [x] Resultados e campeão;
- [x] Dados de demonstração/teste isolados por profile.

### Identidade e acesso

- [x] Cadastro e autenticação de usuários;
- [x] JWT stateless;
- [x] BCrypt para senhas;
- [x] Ownership de equipes;
- [x] API administrativa;
- [x] API do participante;
- [x] API pública sanitizada.

### Infraestrutura

- [x] MySQL persistente;
- [x] migrations com Flyway;
- [x] Swagger/OpenAPI;
- [x] testes automatizados;
- [x] CI com GitHub Actions;
- [x] abstração de object storage preparada para mídia.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## 🤖 Modalidades

### Seguidor de Linha — Follow Line

O fluxo de Follow Line trabalha com **3 tomadas**, cada uma permitindo até **3 tentativas**.

```text
Registration APROVADA
        ↓
      Tomada
        ↓
    Tentativas
        ↓
tempo + penalidade
        ↓
melhor tentativa da tomada
        ↓
melhor tomada da inscrição
        ↓
      ranking
```

```text
tempoFinal = tempoSegundos + penalidadeSegundos
```

O ranking oficial é calculado no backend. Checkpoints permanecem como informação operacional e não substituem o critério oficial de classificação.

### Sumô

```text
Registration APROVADA
        ↓
  inspeção humana
    APTO / INAPTO
        ↓
      Bracket
        ↓
       Match
        ↓
    RoundSumo
        ↓
   MatchResult
        ↓
    progressão
```

O domínio suporta, entre outros comportamentos:

- categorias físicas distintas;
- modo `AUTONOMO` ou `RC`;
- rounds regulares e extras justificados;
- falha de inicialização;
- penalidades por competidor;
- derrota automática ao atingir o limite competitivo de penalidades;
- Suicídio/WO;
- BYE;
- decisão de juiz;
- preservação de histórico de chave.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## 🔐 APIs e Segurança

O acesso é separado por finalidade:

```text
/api/v1/public/**       → leitura pública
/api/v1/participante/** → participante + ownership
/api/v1/**              → operação administrativa
```

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

A API pública utiliza DTOs próprios para evitar exposição de informações administrativas ou dados sensíveis.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## 🛠️ Tecnologias

| Categoria | Tecnologia | Finalidade |
|---|---|---|
| Linguagem | Java 21 | Base do backend |
| Framework | Spring Boot 3.5.x | API REST e configuração |
| Persistência | Spring Data JPA / Hibernate | ORM e transações |
| Banco | MySQL | Persistência principal |
| Migrations | Flyway | Evolução incremental do schema |
| Segurança | Spring Security | Autorização e filtros |
| Autenticação | JWT | Sessão stateless |
| Senhas | BCrypt | Hash de senha |
| Validação | Jakarta Validation | Validação dos payloads |
| Documentação | Springdoc OpenAPI | Swagger/OpenAPI |
| Testes | JUnit 5 + Mockito | Testes automatizados |
| CI | GitHub Actions | Validação automatizada |
| Build | Maven | Dependências e build |

### Persistência e migrations

O schema é versionado com Flyway. Migrations já aplicadas são preservadas e novas alterações estruturais entram em novas versões, mantendo a evolução do banco rastreável.

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## 📦 Storage

As fotos de robôs utilizam uma camada própria de armazenamento:

```text
RobotImageService
        ↓
RobotImageStorageService
        ↓
armazenamento configurável
```

O projeto também possui abstração para object storage compatível com a evolução da camada de mídia, sem acoplar as regras de domínio a um provedor específico.

---

## 🚀 Como Executar

### Pré-requisitos

- Java 21+
- Maven Wrapper incluído no projeto
- MySQL

Entre no módulo backend:

```powershell
cd rascomp
```

Configure as principais variáveis de ambiente:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
ROBOT_IMAGES_DIR
```

Execute:

```powershell
.\mvnw spring-boot:run
```

### Swagger

Com a aplicação em execução:

```text
http://localhost:8080/swagger-ui/index.html
```

### Testes

```powershell
.\mvnw test
```

<p align="right">(<a href="#readme-top">voltar ao topo ⬆</a>)</p>

---

## 🌐 Frontend

A interface do RasComp vive em um repositório separado:

**[gbsalermo/Rascomp-FRONT](https://github.com/gbsalermo/Rascomp-FRONT)**

Ela reúne a Gestão autenticada, o Portal do Participante, a Landing pública e a experiência de galeria.

---

## 📚 Documentação

A documentação técnica detalhada permanece separada da apresentação do projeto.

- [`rascomp/docs/README.md`](rascomp/docs/README.md) — índice da documentação do backend;
- [`rascomp/docs/DOSSIE_PROJETO.md`](rascomp/docs/DOSSIE_PROJETO.md) — visão consolidada do projeto;
- [`rascomp/docs/CONTRATO_REGRAS_COMPETITIVAS.md`](rascomp/docs/CONTRATO_REGRAS_COMPETITIVAS.md) — contrato das regras competitivas;
- [`rascomp/docs/CLOUDFLARE_R2.md`](rascomp/docs/CLOUDFLARE_R2.md) — referência de object storage.

---

<div align="center">
  <strong>RasComp</strong><br>
  Gestão de competições de robótica — IEEE RAS UFRB
</div>
