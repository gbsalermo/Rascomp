# V1-BETA B — Bloco 1 — Backend de identidade

Status: **implementado na branch `v1-beta-b-identidade-cloud` e aguardando validação manual**.

Fonte canônica cross-repo:

```text
gbsalermo/Rascomp-FRONT/docs/V1_BETA_B_IDENTIDADE_ACESSO.md
```

## Backend implementado

- migration `V31__add_verified_identity_and_account_tokens.sql`;
- `UserAccount.emailVerifiedAt` mapeado como `emailVerificadoEm`;
- tokens de `EMAIL_VERIFICATION` e `PASSWORD_RESET`;
- token bruto aleatório de 32 bytes enviado ao usuário;
- somente hash SHA-256 persistido;
- uso único, expiração e invalidação do token anterior;
- cadastro público não gera JWT;
- login de conta pública exige e-mail verificado;
- reset de senha incrementa `sessionVersion`;
- JWT anterior deixa de autenticar;
- respostas de recuperação não enumeram contas;
- adapter local `log` e adapter `resend` para e-mail transacional.

## Compatibilidade

A V31 marca registros preexistentes como verificados para não quebrar contas DEV/GESTAO/MIDIA, demos e dados já existentes.

Novas contas internas criadas por DEV são consideradas provisionadas e verificadas. Novas contas públicas de participante exigem confirmação.

## Configuração

```text
EMAIL_PROVIDER=log
EMAIL_FROM=RasComp <no-reply@localhost>
RESEND_API_KEY=
IDENTITY_FRONTEND_BASE_URL=http://localhost:5173
EMAIL_VERIFICATION_HOURS=24
PASSWORD_RESET_MINUTES=30
IDENTITY_RESEND_COOLDOWN_SECONDS=60
```

Para a validação local, manter `EMAIL_PROVIDER=log`. O link aparece no console e não exige credencial externa.

O próximo bloco só começa após validação manual: **homologação remota por Cloudflare Tunnel + Cloudflare Access**, preservando execução local.
