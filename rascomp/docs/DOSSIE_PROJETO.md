# Dossiê do Projeto RasComp — Ponteiro

Última revisão: **19/09/2026**

O Dossiê Mestre canônico e cross-repo está em:

```text
gbsalermo/Rascomp-FRONT/docs/DOSSIE_PROJETO_RASCOMP.md
```

Ele consolida backend, gestão, participante, Landing, galeria, segurança, domínio competitivo, storage, roadmap relacionado, Avisos+Telegram, Ajustes Gerais, portabilidade, CMS/Mídia, Regras, Futebol, riscos e mapa de manutenção.

Este arquivo é somente um ponteiro e não deve receber uma segunda cópia do dossiê.

Para ordem de trabalho e etapa atual:

```text
gbsalermo/Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md
```

Estado oficial:

```text
ETAPA 0  ✅ concluída / validada
ETAPA 1  ✅ concluída / validada
ETAPA 2  ✅ concluída / validada
ETAPA 3  ✅ concluída / validada
ETAPA 4   🚧 EM ANDAMENTO — Consolidação funcional e polimento do MVP — BLOCO 1
```

As ETAPAS 1, 2 e 3 estão encerradas/validadas. O roadmap foi reorganizado por maturidade do produto e a ETAPA 4 — Consolidação funcional e polimento do MVP — é a próxima etapa, ainda não iniciada.

Checkpoint backend:

```text
rascomp/docs/CONTINUIDADE.md
```

Índice global:

```text
gbsalermo/Rascomp-FRONT/docs/README.md
```

## Checkpoint técnico da ETAPA 2 — 13/09/2026

O backend concluiu a limpeza estrutural prevista e o frontend concluiu a modularização incremental de APIs/tipos e a consolidação segura do CSS administrativo. Backend Tests #297 e Frontend Checks #57–#62 estão verdes. A ETAPA 2 está concluída/validada no escopo técnico; smoke visual e testes práticos ficam para etapas funcionais posteriores.


## ETAPA 3 — checkpoint integrado — 13/09/2026

- `UserRole = DEV | GESTAO | MIDIA | PARTICIPANTE`;
- V13 migra `ORGANIZACAO → DEV`;
- `DEV` possui administração de usuários;
- `DEV/GESTAO` possuem operação competitiva;
- `MIDIA` não herda operação competitiva;
- bootstrap inicial cria DEV;
- verificações de negócio usam capacidade semântica;
- Backend Tests #298 (PR) e #299 (main) verdes;
- MySQL + Flyway V13 + `testdata` verdes.


Validação adicional da ETAPA 3:

- `SecurityAuthorizationFlowTest` — autorização HTTP para DEV/GESTAO/MIDIA/PARTICIPANTE;
- `DemoShowcaseDataInitializerTest` — quatro usuários de demonstração;
- 120 testes / 0 falhas / 0 erros / 0 skipped;
- profile `testdata` verde em MySQL + Flyway V13;
- Frontend Checks #64–#66 verdes.


## Política de criação de contas — ETAPA 3

- cadastro público cria sempre `PARTICIPANTE`;
- o cliente não escolhe role no cadastro comum;
- contas `DEV | GESTAO | MIDIA` são criadas explicitamente por DEV;
- a rota administrativa rejeita criação de `PARTICIPANTE`;
- não existe promoção automática da conta pessoal para conta institucional nesta etapa;
- a mesma pessoa pode ter conta pessoal de participante e conta institucional separada;
- como `UserAccount.email` é único, as duas contas usam e-mails diferentes;
- edição de permissão interna entre `DEV | GESTAO | MIDIA` é suportada na ETAPA 3;
- `PARTICIPANTE` não pode ser convertido para conta interna nem o inverso;
- a conta autenticada não altera a própria role durante a sessão;
- o último DEV ativo não pode ser rebaixado ou desativado;
- operações genéricas de identidade continuam reservadas aos Ajustes Gerais.

Validação: Backend Tests #309 com 125 testes verdes e Frontend Checks #68 com typecheck + build verdes.


## Fechamento da ETAPA 3 — 19/09/2026

A matriz `DEV | GESTAO | MIDIA | PARTICIPANTE` foi validada em uso prático e a ETAPA 3 foi encerrada.

A verificação global de permissões foi incorporada à ETAPA 15 — Validação final completa, imediatamente antes do deploy da ETAPA 16.


## Roadmap reorganizado — 19/09/2026

O planejamento passa a priorizar maturidade do produto:

- **PRIORIDADE 1 / ETAPAS 4–10:** consolidação do MVP, Ajustes Gerais, Futebol, Portal do Participante, CMS, Landing/Galeria e validação do MVP;
- **PRIORIDADE 2 / ETAPAS 11–15:** Avisos+Telegram, portabilidade, Regras/Ajuda/Segurança, hardening com testes físicos mobile e validação final;
- **ETAPA 16:** deploy, última etapa.


## Mobile no novo roadmap

A otimização de interfaces para celular/tablet é um **checkpoint transversal da PRIORIDADE 1**.

A ETAPA 4 pode corrigir problemas encontrados no sistema atual, mas o trabalho continua junto das interfaces alteradas nas ETAPAS 7, 8 e 9 e precisa estar consolidado antes da ETAPA 10.

O login já possui tratamento responsivo dedicado; Gestão e Portal do Participante ainda precisam revisão sistemática.

A ETAPA 14 permanece como validação física/hardening em aparelhos reais.


## Checkpoint de início da ETAPA 4 — 22/09/2026

Branch de trabalho:

```text
etapa-4-consolidacao-mvp
```

Estado:

- ETAPA 4 autorizada e iniciada;
- BLOCO 1 em andamento;
- baseline técnico, autenticação, Shell e UX global são o escopo atual;
- ETAPA 5 permanece bloqueada;
- nenhuma nova funcionalidade estrutural deve ser antecipada.
