# RasComp — Roadmap Pós-Projeto — Ponteiro do Backend

O roadmap pós-aprovação é cross-repo e possui uma única fonte canônica:

`gbsalermo/Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md`

Este arquivo existe apenas para impedir duplicação de planejamento no backend.

## Estado atual

- ETAPA 0 ✅ concluída / validada — baseline;
- ETAPA 1 ✅ concluída / validada — lógica e integridade;
- ETAPA 2 ✅ concluída / validada — limpeza técnica;
- ETAPA 3 ✅ concluída / validada — matriz de permissões;
- ETAPA 4 ⏳ próxima / não iniciada — Consolidação funcional e polimento do MVP.

## Ordem resumida atual

### PRIORIDADE 1 — Finalização e polimento do MVP

- ETAPA 4 — Consolidação funcional e polimento do MVP;
- ETAPA 5 — Ajustes Gerais DEV + auditoria;
- ETAPA 6 — Futebol de Robôs;
- ETAPA 7 — Portal do Participante completo + identificação competitiva;
- ETAPA 8 — Gestor de Mídia / CMS;
- ETAPA 9 — Landing + Galeria + conteúdo público real;
- ETAPA 10 — Validação e fechamento do MVP.

### PRIORIDADE 2 — Adições, testes e portabilidade

- ETAPA 11 — Avisos IN_APP + Telegram;
- ETAPA 12 — Portabilidade institucional;
- ETAPA 13 — Regras, Ajuda e Segurança;
- ETAPA 14 — Hardening + preparação para uso externo + testes físicos mobile;
- ETAPA 15 — Validação final completa, incluindo permissões;
- ETAPA 16 — Deploy em nuvem / Cloudflare.

**Deploy é a última etapa do ciclo.**

Regras de continuidade:

- não criar roadmap paralelo neste repositório;
- consultar o documento canônico antes de iniciar qualquer etapa;
- business/security rule: backend primeiro;
- V1–V13 são migrations aplicadas e não devem ser reescritas;
- próxima mudança estrutural usa V14+;
- a ETAPA 4 ainda não está iniciada até autorização explícita.

Observação mobile:

- A otimização mobile é um **checkpoint transversal da PRIORIDADE 1**, não uma redefinição da ETAPA 4.
- Ela começa a ser tratada quando a ETAPA 4 revisar as telas existentes e acompanha Portal (ETAPA 7), CMS/Mídia (ETAPA 8) e Landing/Galeria (ETAPA 9).
- O checkpoint deve estar concluído antes da ETAPA 10 — fechamento do MVP.
- No estado atual, apenas o login possui tratamento responsivo dedicado já revisado.
- Na ETAPA 14, essa experiência é revalidada fisicamente em smartphones/tablets reais.
