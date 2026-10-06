# RasComp — Deploy Cloudflare

Atualização 05/10/2026: a **V1-BETA A foi concluída e validada**. Este ponteiro passa a orientar a discussão da V1-BETA B; a B ainda não foi iniciada. ETAPA 16 permanece como consolidação/hardening final.

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


### Gate zero da V1-BETA B

Antes de provisionar produção definitiva, decidir:

- verificação de e-mail e ativação de conta;
- recuperação segura de senha;
- provedor de e-mail transacional;
- ambiente remoto temporário/homologação para acesso externo imediato;
- separação entre homologação e produção real;
- manutenção do modo local e da opção Cloudflare Tunnel.

Esse gate deve ser discutido e aprovado antes do início efetivo da B.
