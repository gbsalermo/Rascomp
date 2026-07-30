# Rascomp — Robot Competition Championship

Java Spring Boot • Vue.js • PostgreSQL

📋 Sobre o Projeto
Plataforma para gerenciar competições de robôs (sumo e follow-line). Backend em Spring Boot com Camunda para orquestração de partidas e dois front‑ends: Gestão (autenticado) e Vitrine pública (somente leitura).

⚠️ Projeto em desenvolvimento
Leia o arquivo rascomp/docs/CONTINUIDADE.md (Documento de Continuidade do projeto) para status atual, histórico de mudanças e decisões.

🎯 Funcionalidades Principais
- Gestão de competições, categorias, equipes, competidores e robôs
- Inscrições e aprovação de participações
- Geração de chaveamentos (eliminação / round-robin)
- Registro e publicação de resultados (integração com Camunda BPMN)
- CMS simples para notícias e seções da Vitrine
- Endpoints públicos para Vitrine e SSE para atualizações em tempo real

🏗️ Arquitetura (resumo)

FRONTEND (Vue.js)  ▶  BACKEND (Spring Boot + Camunda)  ▶  DATABASE (PostgreSQL)

🚀 Tecnologias
- Frontend: Vue.js 3
- Backend: Java 21 + Spring Boot 3.x
- Orquestração: Camunda 7 (embarcado)
- Banco de Dados: H2 (dev) / PostgreSQL (prod)
- Migrations: Flyway (opcional)
- API: REST + OpenAPI (springdoc)
- Tempo real: Server-Sent Events (SSE)

📁 Estrutura do projeto (sintética)
rascomp/
├── backend/             # API Spring Boot
├── management-frontend/ # Vue 3 — Gestão
├── public-frontend/     # Vue 3 — Vitrine
├── docs/                # Documentação e BPMN
├── continudade-rrc.md   # Documento de Continuidade (status e histórico)
├── README.md
└── docker-compose.yml

🔧 Pré-requisitos (dev)
- Java 21+
- Node.js 16+
- Maven 3.8+
- (Opcional) Docker & Docker Compose

📦 Instalação rápida (desenvolvimento)
Backend (H2 dev):

cd backend
mvn clean install
mvn spring-boot:run

Frontend (exemplo - management):

cd management-frontend
npm install
npm run dev

Postgres (opcional):
# criar banco
psql -U postgres -c "CREATE DATABASE rascomp;"
# aplicar migrations (quando prontas)
psql -U postgres -d rascomp -f database/init.sql

📚 Documentação
- CONTINUIDADE.md — Documento principal do projeto (status, histórico, decisões)
- API docs (Swagger) — /swagger-ui.html quando o backend estiver rodando

📝 Licença
A definir.

👥 Contribuidores
[preencher]

