# Dossiê — Plataforma RRC (Robot Competition Championship)
### RAS-UFRB — Projeto de Extensão
Versão de planejamento | Prazo: final de agosto de 2025

---

## 0. Histórico da ideia

O projeto nasceu da necessidade real do projeto de extensão **RAS-UFRB** (IEEE Robotics and Automation Society — Universidade Federal do Recôncavo da Bahia), do qual o desenvolvedor faz parte. O grupo realiza competições de robôs (sumo e follow-line) e sentia falta de um sistema próprio para gerenciar competidores, equipes, chaveamentos e resultados de forma organizada.

A ideia inicial era um sistema simples de gestão de competição. Durante o planejamento, percebeu-se que havia dois públicos completamente diferentes sendo atendidos:

- O **público interno** (organizadores, juízes, equipes e marketing do projeto) que precisa de uma ferramenta operacional para administrar a competição e publicar conteúdo.
- O **público externo** (visitantes, patrocinadores, imprensa, alunos interessados) que precisa de uma vitrine institucional bonita, com notícias, galeria e acompanhamento ao vivo dos chaveamentos.

Tentou-se inicialmente misturar os dois em um único front, mas ficou claro que isso criaria um sistema genérico demais para os dois fins. A decisão foi separar em **dois fronts** consumindo **uma única API**:

- **Vitrine** — landing page pública e institucional do RRC, somente leitura, alimentada via API pela equipe de gestão.
- **Gestão** — sistema autenticado onde a equipe opera a competição e publica conteúdo que se reflete na Vitrine.

Outro ponto de virada foi a decisão de usar **Camunda** como orquestrador das batalhas: em vez de escrever na mão a lógica de "quem venceu avança", o fluxo de cada partida é modelado como um processo BPMN, tornando o sistema auditável, visual e um diferencial técnico real no portfólio.

O projeto tem **dupla finalidade** explícita:
1. Ser uma ferramenta real e funcional para as edições do evento RRC da RAS-UFRB.
2. Ser uma peça de portfólio técnico que demonstra domínio de arquitetura full-stack, BPM/orquestração de processos e separação de responsabilidades entre sistemas.

O desenvolvedor quer **codar tudo na mão**, entendendo cada decisão pelo processo de fazer, errar e corrigir. O papel de qualquer IA nesse projeto é de **consultor e planejador**, não de gerador de código pronto.

---

## 1. Objetivos

### Objetivo geral
Construir uma plataforma web completa para gerenciamento de competições de robôs (sumo e follow-line), com uma landing page institucional pública e um sistema de gestão autenticado, servidos por uma API única com orquestração de processos via Camunda.

### Objetivos técnicos (portfólio)
- Demonstrar arquitetura full-stack com separação clara de responsabilidades.
- Implementar orquestração de processos com Camunda (BPMN) em um contexto real.
- Aplicar boas práticas: migrations versionadas (Flyway), testes, documentação de API (OpenAPI), autenticação JWT com controle de papéis.
- Entregar um sistema dockerizado, com README e documentação de arquitetura.

### Objetivos de produto (extensão)
- Permitir que a equipe RAS-UFRB cadastre competições, equipes, robôs e participantes.
- Gerar e gerenciar chaveamentos (com sorteio aleatório ou manual).
- Permitir que juízes lancem resultados de partidas e que o chaveamento avance automaticamente.
- Dar à equipe de marketing autonomia para publicar notícias, fotos e textos institucionais na Vitrine sem depender de deploys.
- Exibir publicamente o andamento da competição em tempo real.

### O que este projeto não é
- Não é uma plataforma multi-tenant (é para o RRC especificamente).
- Não é um CMS robusto (o módulo de conteúdo é intencional e propositalmente simples).
- Não é um sistema de streaming de vídeo ou live da arena.

---

## 2. Visão geral da arquitetura

```
                         ┌────────────────────────────┐
                         │       API Spring Boot        │
                         │  + Camunda 7 (embarcado)     │
                         │  + Postgres + Flyway          │
                         └────────────┬───────────────┘
                    ┌─────────────────┴─────────────────┐
                    │                                     │
         ┌──────────▼────────────┐           ┌───────────▼──────────┐
         │   FRONT DE GESTÃO      │           │    FRONT VITRINE       │
         │   (Vue 3, autenticado) │           │   (Vue 3, público)     │
         │                        │           │                         │
         │  - login por papéis    │  alimenta │  - institucional RRC    │
         │  - cadastros           │  -------> │  - notícias e galeria   │
         │  - chaveamento         │           │  - chaveamento ao vivo  │
         │  - lançar resultados   │           │  - somente leitura      │
         │  - publicar conteúdo   │           │                         │
         └────────────────────────┘           └─────────────────────────┘
```

**Regra fundamental:** a Vitrine **nunca escreve** na API. É alimentada exclusivamente pelos conteúdos publicados via Gestão e pelos dados de competição expostos em endpoints públicos read-only. A Gestão é o único ponto de escrita e operação do sistema.

### Stack
| Camada | Tecnologia |
|---|---|
| Backend | Spring Boot 3 (Web, Data JPA, Validation, Security) |
| Orquestração | Camunda 7 (engine embarcado no Spring Boot) |
| Banco de dados | PostgreSQL |
| Migrations | Flyway |
| Autenticação | JWT com papéis (RBAC) |
| Tempo real | Server-Sent Events (SSE) — mais simples que WebSocket pro caso |
| Front Gestão | Vue 3 + Pinia + Vue Router + Axios |
| Front Vitrine | Vue 3 (mesmo ecossistema, facilita manutenção) |
| Documentação API | Springdoc OpenAPI (Swagger UI) |
| Infra local | Docker Compose (Postgres + backend) |

---

## 3. Modelo de domínio completo

### 3.1 Módulo Competição

**Competitor**
```
id, nome, email, matricula_ou_cpf, instituicao, telefone
```
Relação N:N com `Team` (um competidor pode participar de equipes diferentes em edições diferentes).

**Team**
```
id, nome, sigla, instituicao, tecnico_responsavel
```
Relação 1:N com `Robot`. Relação N:N com `Competitor`.

**Robot**
```
id, nome, team_id, category_id, peso, altura, largura, comprimento,
status_vistoria (PENDENTE | APROVADO | REPROVADO), observacoes
```

**Category**
```
id, nome (SUMO | FOLLOW_LINE), codigo, descricao,
peso_min, peso_max, tipo_pontuacao (ROUNDS | TEMPO)
```

**Competition**
```
id, nome, descricao, data_inicio, data_fim, local,
status (PLANEJAMENTO | INSCRICOES_ABERTAS | EM_ANDAMENTO | ENCERRADA)
```
Relação N:N com `Category` (categorias habilitadas naquela edição).

**Registration**
```
id, robot_id, competition_id, category_id,
status (PENDENTE | APROVADA | REJEITADA), data_inscricao
```

**Bracket**
```
id, competition_id, category_id,
tipo (ELIMINACAO_SIMPLES | ELIMINACAO_DUPLA | TODOS_CONTRA_TODOS),
status, seed
```

**Match**
```
id, bracket_id, fase (OITAVAS | QUARTAS | SEMI | FINAL | TERCEIRO_LUGAR),
robot_a_id, robot_b_id, vencedor_id,
status (AGENDADA | EM_ANDAMENTO | FINALIZADA | WALKOVER),
process_instance_id, data_hora
```
O campo `process_instance_id` é o vínculo entre a partida e a instância de processo do Camunda.

**MatchResult**
```
id, match_id, tipo (SUMO_ROUND | FOLLOW_LINE_TIME),
dados (JSON — para sumo: [{round: 1, vencedor_id: x}, ...];
       para follow line: {tempo_ms: 34200, penalidades: 2})
```

### 3.2 Módulo Usuários

**User**
```
id, nome, email, senha_hash,
papel (ADMIN | JUIZ | EQUIPE | MARKETING),
team_id (nullable — preenchido quando papel = EQUIPE)
```

### 3.3 Módulo CMS

**NewsPost**
```
id, titulo, slug, resumo, conteudo (markdown),
autor_id, capa_url, status (RASCUNHO | PUBLICADO), data_publicacao
```

**MediaAsset**
```
id, tipo (IMAGEM | DOCUMENTO), url, descricao_alt,
uploaded_by, contexto (GALERIA | CAPA_NOTICIA | GERAL)
```

**PageSection**
```
id, chave (ex: "sobre", "patrocinadores", "edicoes-anteriores"),
conteudo (rich text / markdown), ordem
```
As PageSections são os blocos de texto institucionais da Vitrine que a equipe de marketing pode editar sem precisar de deploy.

---

## 4. API — endpoints

### Públicos (sem autenticação — consumidos pela Vitrine)
```
GET  /api/public/news                                    lista notícias publicadas
GET  /api/public/news/{slug}                             notícia individual
GET  /api/public/pages/{chave}                           seção institucional
GET  /api/public/media?contexto=galeria                  galeria de mídia
GET  /api/public/teams                                   equipes participantes
GET  /api/public/competitions                            lista de competições
GET  /api/public/competitions/{id}/brackets/{catId}      chaveamento completo
GET  /api/public/matches/{id}                            detalhe de uma partida
GET  /api/public/competitions/{id}/stream                SSE — tempo real
```

### Administrativos (autenticados por papel)
```
CRUD /api/competitors
CRUD /api/teams
CRUD /api/robots
CRUD /api/categories
CRUD /api/competitions
CRUD /api/registrations
     POST /api/registrations/{id}/approve
     POST /api/registrations/{id}/reject
CRUD /api/news
     POST /api/news/{id}/publish
CRUD /api/pages/{chave}
POST /api/media                                          upload de arquivo

POST /api/competitions/{id}/brackets/{catId}/generate    gera chaveamento + dispara Camunda
POST /api/matches/{id}/result                            juiz lança resultado (completa task Camunda)
GET  /api/matches/{id}/process-status                    debug do processo Camunda
```

---

## 5. BPMN — fluxo de uma partida

Um processo Camunda **por partida individual**. Não um processo por bracket inteiro — isso mantém o BPMN legível no Cockpit e o processo encapsulado.

```
[Start Event: partida agendada]
         │
[User Task / External Task: "Aguardar resultado"]
  <- juiz aciona via POST /api/matches/{id}/result
         │
[Exclusive Gateway: houve vencedor claro?]
         │
    sim ─┤─ não (empate técnico / walkover duplo)
         │         │
[Service Task:     [Service Task:
 registrar          tratar caso
 vencedor +         excepcional
 avançar fase]      (reagendar/
         │           desclassificar)]
         │                   │
    [End Event]         [End Event]
```

**Service Task "avançar fase"** é um bean Java que:
1. Atualiza `Match.vencedor_id` e `Match.status = FINALIZADA`
2. Verifica se todas as `Match` da fase atual do bracket estão finalizadas
3. Se sim, cria as `Match` da próxima fase com os vencedores e dispara novas instâncias de processo
4. Emite um evento SSE para a Vitrine

Não há BPMN "pai" — o encadeamento do bracket acontece por criação sequencial de instâncias de processo, mantendo tudo simples e rastreável.

---

## 6. Estrutura de pastas

```
rrc-platform/
├── backend/
│   ├── src/main/java/br/edu/ufrb/rrc/
│   │   ├── domain/
│   │   │   ├── competitor/
│   │   │   ├── team/
│   │   │   ├── robot/
│   │   │   ├── category/
│   │   │   ├── competition/
│   │   │   ├── registration/
│   │   │   ├── bracket/
│   │   │   ├── match/
│   │   │   ├── user/
│   │   │   └── cms/
│   │   ├── api/
│   │   │   ├── public/
│   │   │   └── admin/
│   │   ├── camunda/
│   │   │   ├── delegates/
│   │   │   └── listeners/
│   │   ├── infra/
│   │   │   ├── storage/
│   │   │   └── sse/
│   │   └── config/
│   │       ├── SecurityConfig.java
│   │       ├── CamundaConfig.java
│   │       └── OpenApiConfig.java
│   ├── src/main/resources/
│   │   ├── processes/
│   │   │   └── match-process.bpmn
│   │   └── db/migration/
│   │       ├── V1__create_category.sql
│   │       ├── V2__create_team_competitor.sql
│   │       ├── V3__create_robot.sql
│   │       ├── V4__create_competition_registration.sql
│   │       ├── V5__create_bracket_match.sql
│   │       ├── V6__create_user.sql
│   │       └── V7__create_cms.sql
│   └── pom.xml
│
├── management-frontend/          (Vue 3 — Gestão)
│   ├── src/
│   │   ├── views/
│   │   ├── components/
│   │   ├── stores/               (Pinia)
│   │   ├── router/
│   │   └── api/                  (funções Axios)
│   └── package.json
│
├── public-frontend/              (Vue 3 — Vitrine)
│   ├── src/
│   │   ├── views/
│   │   ├── components/
│   │   └── api/
│   └── package.json
│
└── docker-compose.yml
```

---

## 7. Cronograma — julho a agosto

| Semana | Período | Meta | Entregável concreto |
|---|---|---|---|
| 1 | 01–07/07 | Setup geral + entidades simples | docker-compose rodando, Spring Boot conectado ao Postgres, Flyway configurado, entidades `Category` e `User` com migration + CRUD completo testado |
| 2 | 08–14/07 | Entidades com relacionamento | `Competitor`, `Team`, `Robot` com N:N e 1:N, migrations e CRUDs, testes de integração básicos |
| 3 | 15–21/07 | Módulo de competição e inscrição | `Competition`, `Registration` (com aprovação/rejeição), endpoints de listagem pública, Swagger documentado |
| 4 | 22–28/07 | Lógica de chaveamento (puro Java, sem Camunda ainda) | Serviço de geração de bracket, sorteio aleatório, lógica de avanço de fase — tudo testado unitariamente antes de integrar com BPM |
| 5 | 29/07–04/08 | Integração Camunda | BPMN modelado, processo de partida rodando, Service Task de avanço chamando o serviço de bracket, `process_instance_id` persistido no `Match` |
| 6 | 05–11/08 | Auth + início do Front Gestão | JWT com papéis funcionando, telas de login, cadastro de equipes/robôs/inscrições no Vue |
| 7 | 12–18/08 | Operação da competição no Front Gestão | Tela de geração de chaveamento, visualização do bracket, tela do juiz para lançar resultado, módulo de CMS (notícias, mídia, seções) |
| 8 | 19–25/08 | Front Vitrine | Institucional com seções editáveis, notícias, galeria, chaveamento ao vivo via SSE |
| 9 | 26–31/08 | Buffer e portfólio | Correções, README completo, diagrama de arquitetura, vídeo demo |

**Princípio de cada semana:** sempre `entidade → migration → repository → service → controller → teste`, nunca front antes da API estar funcional na mesma entidade.

---

## 8. Prompts por etapa

Esses prompts servem para você (ou qualquer IA) retomar o contexto do projeto em qualquer ponto. Cole o prompt no início da conversa junto com trechos relevantes do seu código atual.

---

### Semana 1 — Setup e primeiras entidades

```
Contexto do projeto:
Estou desenvolvendo a plataforma RRC (Robot Competition Championship) da RAS-UFRB.
É um sistema full-stack com Spring Boot 3 + Camunda 7 (embarcado) + PostgreSQL + Flyway no backend,
e dois fronts Vue 3: um de Gestão (autenticado) e uma Vitrine (pública, somente leitura).
O Camunda orquestra o fluxo de cada partida como processo BPMN.

Estou na Semana 1: configurando o projeto e criando as primeiras entidades (Category e User).
Preciso de ajuda com: [descreva o que está travado — ex: configurar Flyway, mapear a entidade Category com JPA, criar o CRUD de Category com controller REST, etc.]

Código atual: [cole o trecho relevante]
```

---

### Semana 2 — Entidades com relacionamento (Team, Competitor, Robot)

```
Contexto do projeto:
Plataforma RRC (Robot Competition Championship) — Spring Boot 3 + Camunda 7 + Postgres + Flyway + Vue 3.
Entidades base já existem (Category, User). Agora estou mapeando as entidades com relacionamento:
- Team e Competitor têm relação N:N (tabela associativa)
- Robot tem relação N:1 com Team e N:1 com Category

Preciso de ajuda com: [ex: mapear o @ManyToMany entre Team e Competitor, escrever a migration V2, 
tratar o endpoint de criação de Robot validando se o category_id existe, etc.]

Código atual: [cole as entidades e migrations relevantes]
```

---

### Semana 3 — Módulo de competição, inscrição e endpoints públicos

```
Contexto do projeto:
Plataforma RRC — Spring Boot 3 + Camunda 7 + Postgres + Vue 3.
Entidades Team, Competitor, Robot e Category já estão funcionando.
Agora estou construindo o módulo de Competition e Registration.

Registration tem um campo status (PENDENTE/APROVADA/REJEITADA) e endpoints específicos
POST /api/registrations/{id}/approve e POST /api/registrations/{id}/reject.
Os endpoints públicos em /api/public/** são sem autenticação e retornam apenas dados read-only.

Preciso de ajuda com: [ex: como filtrar os endpoints públicos no SecurityConfig sem bloquear os outros,
lógica de aprovação de inscrição, etc.]

Código atual: [cole o relevante]
```

---

### Semana 4 — Lógica de chaveamento (sem Camunda)

```
Contexto do projeto:
Plataforma RRC — Spring Boot 3 + Postgres. Módulos de cadastro e inscrição já funcionando.
Agora estou implementando a lógica de chaveamento como serviço Java puro,
sem integração com Camunda ainda — quero testar a lógica de forma isolada primeiro.

As entidades envolvidas são Bracket e Match.
O chaveamento é eliminação simples. A geração recebe uma lista de robot_ids inscritos e aprovados
e monta as partidas da primeira fase com sorteio aleatório (ou seedado).
A lógica de avanço pega os vencedores de uma fase e cria as partidas da fase seguinte.

Preciso de ajuda com: [ex: algoritmo de montagem do bracket com número ímpar de competidores (bye),
como representar as fases no enum, testes unitários do serviço de bracket, etc.]

Código atual: [cole BracketService e as entidades Bracket/Match]
```

---

### Semana 5 — Integração com Camunda

```
Contexto do projeto:
Plataforma RRC — Spring Boot 3 + Camunda 7 embarcado + Postgres.
A lógica de chaveamento já funciona de forma isolada (BracketService testado).
Agora estou integrando o Camunda: cada partida (Match) tem um process_instance_id
que referencia uma instância do processo BPMN match-process.bpmn.

O fluxo BPMN de uma partida é:
Start → User Task "Aguardar resultado" → Gateway "houve vencedor?" 
→ Service Task "Registrar vencedor e avançar fase" → End

O endpoint POST /api/matches/{id}/result deve completar a User Task correspondente no Camunda.
O Service Task "avançar fase" é um JavaDelegate que chama o BracketService.

Preciso de ajuda com: [ex: como mapear o JavaDelegate, como completar uma User Task via API REST do Camunda,
como buscar a instância de processo pelo process_instance_id persistido no Match, etc.]

Código atual: [cole o BPMN, o MatchService, o JavaDelegate e o controller]
```

---

### Semana 6 — Autenticação JWT e início do Front Gestão

```
Contexto do projeto:
Plataforma RRC — Spring Boot 3 + Camunda 7 + Postgres + Vue 3.
Backend funcionando com todos os módulos (cadastro, chaveamento, Camunda).
Agora estou implementando autenticação JWT com Spring Security e iniciando o front Vue de Gestão.

Os papéis são: ADMIN, JUIZ, EQUIPE, MARKETING.
- ADMIN acessa tudo
- JUIZ acessa apenas telas de operação de partida
- EQUIPE acessa apenas seus próprios robôs/competidores
- MARKETING acessa apenas o módulo de CMS

No Vue, uso Pinia para guardar o token e os dados do usuário logado.

Preciso de ajuda com: [ex: configurar o SecurityConfig com as regras de papel por endpoint,
como guardar o JWT no Pinia e interceptar requests com Axios, tela de login no Vue, etc.]

Código atual: [cole o relevante]
```

---

### Semana 7 — Telas de operação e módulo CMS no Front Gestão

```
Contexto do projeto:
Plataforma RRC — Vue 3 (front Gestão) consumindo API Spring Boot.
Login e cadastros já funcionando no front.
Agora estou construindo as telas de:
1. Geração de chaveamento (chamar POST /api/competitions/{id}/brackets/{catId}/generate)
2. Visualização do bracket (representação visual do bracket de eliminação)
3. Tela do juiz: lista de partidas do dia, botão para abrir partida em andamento e lançar resultado
4. Módulo CMS: editor de notícias (com markdown ou rich text), upload de imagem, edição de PageSections

Preciso de ajuda com: [ex: como renderizar um bracket de eliminação simples visualmente no Vue,
como fazer upload de arquivo para a API, como usar um editor rich text no Vue 3, etc.]

Código atual: [cole o relevante]
```

---

### Semana 8 — Front Vitrine

```
Contexto do projeto:
Plataforma RRC — Vue 3 (front Vitrine, público, somente leitura).
O front de Gestão está funcional. Agora estou construindo a Vitrine:
landing page institucional do RRC (RAS-UFRB) que consome a API em endpoints públicos.

Endpoints que a Vitrine consome:
- GET /api/public/pages/{chave} → seções editáveis (sobre, patrocinadores, etc.)
- GET /api/public/news → lista de notícias
- GET /api/public/competitions/{id}/brackets/{catId} → chaveamento
- GET /api/public/competitions/{id}/stream → SSE para atualizações em tempo real

A referência visual de inspiração é a landing page do ERBASE 2026 (erbase.sbc.org.br/2026).
A Vitrine deve ter: seção hero, sobre o projeto RAS-UFRB, edições anteriores, notícias, galeria, chaveamento ao vivo.

Preciso de ajuda com: [ex: como consumir SSE com Vue 3 (EventSource API), estrutura da página hero,
como montar o componente de chaveamento somente leitura, etc.]

Código atual: [cole o relevante]
```

---

### Semana 9 — Deploy e documentação de portfólio

```
Contexto do projeto:
Plataforma RRC completa — Spring Boot + Camunda 7 + Postgres + Vue 3 (dois fronts).
Estou na etapa final: dockerizar tudo e preparar a documentação de portfólio.

A stack completa é:
- backend Spring Boot (com Camunda embarcado) em uma imagem Docker
- Postgres em container separado
- Os dois fronts Vue podem servidos como build estático (nginx) ou dev server

Preciso de ajuda com: [ex: escrever o Dockerfile do backend Spring Boot,
montar o docker-compose.yml com as variáveis de ambiente corretas,
como servir o build Vue via nginx no Docker, escrever o README de portfólio, etc.]

Arquivos atuais: [cole docker-compose.yml e Dockerfile se já existirem]
```

---

## 9. Referências e recursos úteis

- **Camunda 7 Docs:** https://docs.camunda.org/manual/7.21/
- **Camunda Modeler** (para modelar o BPMN): https://camunda.com/download/modeler/
- **Spring Boot + Camunda starter:** `camunda-bpm-spring-boot-starter-rest` e `camunda-bpm-spring-boot-starter-webapp`
- **Flyway docs:** https://documentation.red-gate.com/fd
- **Springdoc OpenAPI (Swagger):** https://springdoc.org/
- **Referência visual da Vitrine:** https://erbase.sbc.org.br/2026/
- **Vue 3 + Pinia:** https://pinia.vuejs.org/
- **EventSource API (SSE no browser):** https://developer.mozilla.org/en-US/docs/Web/API/EventSource
