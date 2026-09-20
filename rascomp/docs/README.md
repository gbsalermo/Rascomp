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
ETAPA 2  ✅ concluída / validada
ETAPA 3  ✅ concluída / validada
PRÓXIMA ETAPA FUNCIONAL ⏸ aguardando reorganização do roadmap
```

A ETAPA 1 foi concluída e validada após os cinco blocos funcionais. A ETAPA 2 foi concluída/validada em 13/09/2026. A ETAPA 3 foi concluída/validada em 19/09/2026 após integração, testes automatizados e validação prática da matriz de permissões.

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
V1–V13 = migrations imutáveis
V14+ = próxima mudança estrutural
roles atuais = DEV | GESTAO | MIDIA | PARTICIPANTE
ETAPA 1 = concluída / validada
ETAPA 2 = concluída / validada
```

Último checkpoint documentado:

```text
111 testes / 0 falhas / 0 erros / 0 skipped
H2 flowtest integrado ✅
MySQL + Flyway V12 + testdata ✅
```

Não atualizar a contagem por inferência.

---

## O que foi concluído na ETAPA 2

```text
rascomp/bin/                                  ✅ removido
.classpath / .project                           ✅ removidos
.gitkeep desnecessários em packages de código   ✅ removidos
TODOs/FIXMEs reais                              ✅ nenhum encontrado na varredura inicial
código morto/duplicado                          🔎 revisão incremental
```

Esses itens foram executados na ETAPA 2 sem antecipação durante a ETAPA 1.

---

## Próximo trabalho

Checkpoint atual: **ETAPA 3 — Nova matriz de permissões ✅**. Backend, frontend, autorização HTTP e perfis `testdata` validados. Há uma validação final de permissões planejada imediatamente antes do deploy.