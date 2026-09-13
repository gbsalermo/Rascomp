# RasComp — Etapas Pós-Projeto — Ponteiro do Backend

Última revisão: **13/09/2026**

O roadmap pós-aprovação é cross-repo e possui uma única fonte canônica:

```text
gbsalermo/Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md
```

Este arquivo não duplica a sequência para evitar divergências.

Estado oficial:

```text
ETAPA 0  ✅ concluída / validada
ETAPA 1  ✅ concluída / validada
ETAPA 2  ✅ concluída / validada
ETAPA 3  🚧 em andamento
ETAPA 4+ ⏳ não iniciadas
```

A ETAPA 1 foi concluída após Competition/Registration, Follow, Sumô, Chaves e Fluxos integrados. Checkpoint final: 111 testes verdes + MySQL/Flyway V12/testdata.

Marco atual:

```text
ETAPA 2 — Limpeza técnica e organização de código
→ concluída / validada em 13/09/2026

ETAPA 3 — Nova matriz de permissões
→ backend integrado
→ frontend integrado por capacidades semânticas
→ validação automatizada da matriz em andamento
```

A ETAPA 3 está em andamento. Backend e frontend já aplicam `DEV | GESTAO | MIDIA | PARTICIPANTE`; o próximo checkpoint é a validação consolidada da autorização antes do encerramento da etapa.

Referências:

```text
rascomp/docs/CONTINUIDADE.md
→ checkpoint backend

gbsalermo/Rascomp-FRONT/docs/DOSSIE_PROJETO_RASCOMP.md
→ arquitetura e decisões

gbsalermo/Rascomp-FRONT/docs/README.md
→ índice documental
```

Avisos IN_APP e Telegram estão consolidados na ETAPA 4. Deploy permanece ETAPA 14, preservando o modo local.

> Checkpoint 13/09/2026: ETAPA 2 concluída/validada no escopo técnico cross-repo. Smoke visual e testes práticos ficam para etapas funcionais posteriores. A fonte canônica detalhada continua em `Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md`.

> ETAPA 3 — checkpoint 13/09/2026: backend integrado com `DEV | GESTAO | MIDIA | PARTICIPANTE`; V13 migra ORGANIZACAO → DEV; Backend Tests #298/#299 verdes; frontend integrado por capacidades semânticas e Frontend Checks #64 verde. Validação HTTP explícita adicionada em seguida.