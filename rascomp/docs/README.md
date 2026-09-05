# RasComp Backend — Índice da Documentação

Última revisão: **04/09/2026**

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

Estado em 04/09/2026:

```text
ETAPA 0  ✅ concluída / validada
ETAPA 1  🚧 atual
ETAPA 2+ ⏳ não iniciadas
```

O checkpoint documental de 04/09 não representa avanço de etapa.

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
V1–V7 = migrations imutáveis
V8+ = próxima mudança estrutural
roles atuais = ORGANIZACAO | PARTICIPANTE
ETAPA atual = ETAPA 1
```

Último checkpoint documentado:

```text
48 testes / 0 falhas / 0 erros
MySQL + Flyway + testdata ✅
```

Não atualizar a contagem por inferência.

---

## O que continua reservado à ETAPA 2

```text
rascomp/bin/
.classpath
.project
.gitkeep desnecessários em packages de código
TODOs/comentários obsoletos
código morto/duplicado
```

Esses itens não foram removidos neste checkpoint porque a tarefa atual é exclusivamente documental.

---

## Próximo trabalho

Após a revisão documental, retomar a **ETAPA 1 — correções de lógica e integridade**.