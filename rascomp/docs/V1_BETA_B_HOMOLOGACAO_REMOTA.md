# V1-BETA B — Bloco 2 — Homologação remota

Status: **🚧 EM IMPLEMENTAÇÃO**.

Fonte operacional canônica cross-repo:

```text
gbsalermo/Rascomp-FRONT/docs/V1_BETA_B_HOMOLOGACAO_REMOTA.md
```

O backend continua executando localmente em:

```text
127.0.0.1:8080
```

e é alcançado externamente somente pelo proxy `/api` da aplicação Gestão/Participante publicada pelo Tunnel.

O MySQL permanece local/privado e **não deve ser publicado**.

Para testar links de confirmação e recuperação por uma URL externa, configurar por ambiente:

```text
IDENTITY_FRONTEND_BASE_URL=https://<hostname-de-homologacao>
```

Nenhum hostname ou token Cloudflare deve ser hardcoded no backend.

Checkpoint:

```text
Bloco 1 identidade/e-mail/recuperação  ✅ validado 15/15
Bloco 2A Quick Tunnel protegido        🚧 preparado no frontend / aguardando execução externa
Bloco 2B Tunnel estável + Access       ⏳ aguardando conta/domínio/hostname
```
