# Dossiê do Projeto RasComp — Ponteiro

Última revisão: **13/09/2026**

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
PRÓXIMA ETAPA FUNCIONAL ⏸ aguardando reorganização do roadmap
```

As ETAPAS 1, 2 e 3 estão encerradas/validadas. A matriz de permissões está integrada e validada; a próxima etapa funcional aguardará a reorganização do roadmap.

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

Uma nova verificação global de permissões foi movida para o checkpoint imediatamente anterior ao deploy, quando todos os módulos futuros já estiverem consolidados. Até a reorganização do roadmap, nenhuma próxima etapa funcional deve ser assumida automaticamente.
