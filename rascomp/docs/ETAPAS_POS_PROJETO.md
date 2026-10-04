# RasComp — Roadmap Pós-Projeto — Ponteiro do Backend

Última sincronização: **03/10/2026**

O roadmap pós-aprovação é cross-repo e possui uma única fonte canônica:

```text
gbsalermo/Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md
```

Este arquivo existe apenas para impedir duplicação de planejamento no backend.

## Estado atual

```text
ETAPA 0  ✅ concluída / validada
ETAPA 1  ✅ concluída / validada
ETAPA 2  ✅ concluída / validada
ETAPA 3  ✅ concluída / validada
ETAPA 4  ✅ concluída / validada — merge autorizado em 03/10/2026
```

Migrations aplicadas nesta linha de evolução: **V1–V27**. Próxima migration estrutural: **V28+**.

## Fronteira após a ETAPA 4

O antigo BLOCO 4.4 não segue como bloco separado.

O polimento da Landing será consolidado com a entrega já planejada de Landing/Galeria/conteúdo público. A numeração, o nome definitivo e o escopo dessa etapa única serão decididos após o merge da ETAPA 4.

Não iniciar uma nova etapa com base neste ponteiro; consultar sempre o roadmap canônico do frontend.

## Decisões futuras preservadas

- Ajustes DEV: inclusão/correção administrativa auditável e operação explícita para encerrar/cancelar chave vigente e gerar outra quando necessário, preservando histórico.
- Futebol de Robôs: placar por gols, cronômetro operacional e **2 minutos como referência atual/configurável**.
- Follow Pro/Júnior: hipótese somente de pós-produção, condicionada à confirmação da competição; Follow permanece categoria única no MVP atual.
- Deploy continua etapa final do ciclo até nova revisão explícita do roadmap.

## Referências

- Backend checkpoint: `rascomp/docs/CONTINUIDADE.md`
- Dossiê cross-repo: `gbsalermo/Rascomp-FRONT/docs/DOSSIE_PROJETO_RASCOMP.md`
- Regras do participante: `gbsalermo/Rascomp-FRONT/docs/REGRAS_PARTICIPANTE.md`

---

## Trilho prioritário V1 Beta

A ordem canônica está no frontend `docs/ETAPAS_POS_PROJETO.md`.

Resumo:

```text
V1-BETA A — Landing
V1-BETA B — produção/cloud + banco
V1-BETA C — cadastro/acesso/inscrições reais
V1-BETA D — smoke/estabilização
→ retorno ao roadmap oficial
```

Após a Beta, qualquer evolução deve ocorrer fora de produção e só ser promovida após testes.