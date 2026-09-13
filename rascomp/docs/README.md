# RasComp Backend — Índice da Documentação

Última revisão: **13/09/2026**

A documentação global do RasComp é coordenada no repositório frontend para evitar roadmaps e dossiês duplicados.

## Comece por aqui

```text
1. gbsalermo/Rascomp-FRONT/docs/README.md
   → índice global

2. gbsalermo/Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md
   → roadmap canônico + etapa atual

3. gbsalermo/Rascomp-FRONT/docs/DOSSIE_PROJETO_RASCOMP.md
   → arquitetura e decisões cross-repo

4. rascomp/docs/CONTINUIDADE.md
   → checkpoint vivo deste backend
```

Estado oficial em 13/09/2026:

```text
ETAPA 0  ✅ concluída / validada
ETAPA 1  ✅ concluída / validada
ETAPA 2  🚧 em andamento
ETAPA 3+ ⏳ não iniciadas
```

A ETAPA 1 foi concluída e validada após os cinco blocos funcionais. A ETAPA 2 foi autorizada em 13/09/2026 e está em andamento.

---

## Documentos ativos neste diretório

### `CONTINUIDADE.md`

Checkpoint do backend: stack, migrations, segurança, estado funcional e riscos atuais.

### `ETAPAS_POS_PROJETO.md`

Ponteiro para o roadmap cross-repo. Não duplica a sequência.

### `DOSSIE_PROJETO.md`

Ponteiro para o Dossiê Mestre cross-repo.

### `CLOUDFLARE_R2.md`

Referência técnica da abstração R2 preparada para mídia futura.

### `DECISAO_DEPLOY_CLOUD.md` / `DEPLOY_CLOUDFLARE.md`

Referências da ETAPA 14. O modo local continuará existindo.

---

## Documentação histórica removida

A revisão de 04/09/2026 removeu arquivos antigos de endpoints, Swagger/Postman, fluxo/UML e congelamento de API que já estavam desatualizados e, em alguns casos, ainda descreviam componentes que não pertencem à arquitetura atual.

As informações ainda válidas foram consolidadas em:

```text
código atual
Swagger gerado pela aplicação
migrations
Dossiê Mestre
CONTINUIDADE.md
```

**Camunda não faz parte do RasComp atual.**

---

## Regras importantes

```text
backend = fonte de verdade de domínio/autorização
banco ativo = MySQL
V1–V12 = migrations imutáveis
V13+ = próxima mudança estrutural
roles atuais = ORGANIZACAO | PARTICIPANTE
ETAPA 1 = concluída / validada
ETAPA 2 = próxima / não iniciada
```

Último checkpoint documentado:

```text
111 testes / 0 falhas / 0 erros / 0 skipped
H2 flowtest integrado ✅
MySQL + Flyway V12 + testdata ✅
```

Não atualizar a contagem por inferência.

---

## O que continua reservado à ETAPA 2

```text
rascomp/bin/                                  ✅ removido
.classpath / .project                           ✅ removidos
.gitkeep desnecessários em packages de código   ✅ removidos
TODOs/FIXMEs reais                              ✅ nenhum encontrado na varredura inicial
código morto/duplicado                          🔎 revisão incremental
```

Esses itens permanecem reservados à ETAPA 2 e não foram antecipados durante a ETAPA 1.

---

## Próximo trabalho

Próximo trabalho do roadmap: **ETAPA 2 — Limpeza técnica e organização de código**, ainda não iniciada. Confirmar autorização antes de começar.