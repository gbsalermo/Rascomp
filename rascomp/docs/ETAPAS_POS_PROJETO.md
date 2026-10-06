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

Migrations aplicadas nesta linha de evolução: **V1–V30**. Próxima migration estrutural: **V31+**.

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
V1-BETA A — Landing ✅ concluída / validada
V1-BETA B — identidade/e-mail + produção/cloud + banco ⏭️ próxima
V1-BETA C — cadastro/acesso/inscrições reais
V1-BETA D — smoke/estabilização
→ retorno ao roadmap oficial
```

Após a Beta, qualquer evolução deve ocorrer fora de produção e só ser promovida após testes.

## Gate obrigatório antes de inscrições reais

Consultar o roadmap canônico do frontend. Resumo bloqueante:

```text
banco persistente
backup/restore
Flyway
storage de comprovantes
contas verificadas reais
Competition/categorias/janela
smoke conta nova do zero
```

Ajustes Gerais DEV avançados não bloqueiam o primeiro go-live.

A V1 Beta também antecipa inscrição simples de Futebol de Robôs sem Robot próprio obrigatório. O domínio da partida fica para a ETAPA 6.


## Gate pré-competição oficial — segurança e carga

Além do gate da V1 Beta, consultar no roadmap canônico:

- ETAPA 14 — hardening de segurança + preparação dos cenários de carga;
- ETAPA 15 — carga genérica + simulação de competição com 300–500 participantes;
- checkpoint obrigatório antes da primeira competição oficial.

Escopo inclui proteção contra injection, autenticação abusiva, rajadas/rate limiting e validação de integridade sob concorrência em inscrições, Follow, Sumô, chaves e rankings.


## Checkpoint de transição — V1-BETA A → V1-BETA B — 05/10/2026

A **V1-BETA A está concluída e validada**. A próxima fase é a **V1-BETA B**, mas seu desenvolvimento ainda não foi iniciado.

Antes do primeiro commit da B, deve haver uma decisão explícita sobre identidade e acesso real. O primeiro checkpoint obrigatório da B será:

```text
conta criada
→ e-mail real verificável
→ verificação de e-mail
→ ativação da conta
→ login
→ recuperação segura de senha
```

Objetivos:

- reduzir contas descartáveis/falsas sem exigir dados pessoais desnecessários;
- garantir que o usuário controle de fato o endereço de e-mail informado;
- permitir recuperação de senha sem intervenção manual como fluxo principal;
- não expor se um e-mail existe no sistema;
- tokens/códigos de verificação e recuperação devem ser de uso único e expirar;
- não armazenar token de recuperação reutilizável em texto puro;
- DEV pode continuar com fluxo assistido excepcional, auditado, sem conhecer a senha definitiva;
- a escolha do provedor de envio de e-mail será decidida antes da implementação.

Requisito operacional imediato para discussão da B:

- permitir acesso remoto funcional ao RasComp em ambiente de teste/homologação já no início da fase;
- esse acesso não significa abrir inscrições reais nem declarar produção;
- manter modo local e contingência por servidor local + Cloudflare Tunnel como opções oficiais.

**Não iniciar a implementação da V1-BETA B antes dessa decisão de arquitetura/identidade.**
