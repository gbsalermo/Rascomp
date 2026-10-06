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


## Preservação obrigatória do modo local

A decisão cloud não substitui o modo local.

O RasComp deve continuar podendo operar com Spring Boot + MySQL + storage locais, inclusive como contingência de evento.

A V1-BETA B deve documentar:

- variáveis para local/staging/produção;
- backup/restore cloud → local;
- restauração de uploads;
- acesso LAN;
- retorno controlado à cloud.

Também evitar acoplar toda chamada dinâmica a Worker/limite diário específico da Cloudflare. Limites comerciais devem ser revalidados antes do provisionamento e antes da competição.
