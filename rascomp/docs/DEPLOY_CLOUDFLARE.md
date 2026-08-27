# RasComp — Deploy Cloudflare

O guia canônico de deploy do projeto inteiro está no repositório de frontend:

```text
gbsalermo/Rascomp-FRONT
docs/DEPLOY_CLOUDFLARE.md
```

O documento cobre:

```text
modo local preservado
Docker do Spring Boot
Cloudflare Containers
Worker de entrada da API
Worker Secrets
Cloudflare R2
MySQL persistente
Workers Static Assets para Vue/Vite
CORS
custom domains
Flyway
CI/CD
backup
rollback
smoke tests
go-live
```

Decisão atual para o primeiro deploy:

```text
backend Spring Boot → Cloudflare Container
frontends Vue/Vite  → Workers Static Assets
mídias/fotos cloud  → Cloudflare R2
banco               → MySQL gerenciado persistente externo inicialmente
```

Não migrar MySQL/JPA/Hibernate/Flyway para D1 durante o primeiro deploy. Essa mudança, se desejada no futuro, deve ser tratada como uma migração de persistência separada.
