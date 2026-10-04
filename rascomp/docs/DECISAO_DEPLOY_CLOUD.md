# RasComp — Decisão de Deploy em Nuvem

Atualização 03/10/2026: o primeiro deploy foi antecipado para o **TRILHO V1 BETA**. A arquitetura abaixo continua como referência técnica; a ETAPA 16 passa a representar consolidação final da produção.

A decisão congelada do primeiro deploy é mantida como documento canônico no repositório de frontend:

```text
gbsalermo/Rascomp-FRONT
docs/DECISAO_DEPLOY_CLOUD.md
```

Arquitetura congelada:

```text
Cloudflare
├─ Landing
├─ Gestão/Participante
├─ Spring Boot em Container
└─ R2

Aiven MySQL Free
└─ banco persistente externo acessado via JDBC/TLS
```

Regras:

- manter MySQL/JPA/Hibernate/Flyway;
- não colocar MySQL dentro do Container;
- não remover o modo local;
- não migrar para D1/PostgreSQL nesta primeira implantação sem bloqueio técnico real;
- meta inicial desta frente: **30/08/2026**.

Guia operacional detalhado:

```text
gbsalermo/Rascomp-FRONT
docs/DEPLOY_CLOUDFLARE.md
```
