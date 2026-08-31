# RasComp Backend — Índice da Documentação

Última revisão: **31/08/2026**

Este diretório contém documentação viva e documentação histórica/técnica do backend.

A documentação global do projeto é coordenada no repositório frontend para evitar roadmaps e dossiês duplicados.

---

## Comece por aqui

```text
1. gbsalermo/Rascomp-FRONT/docs/README.md
   → índice global e hierarquia de autoridade

2. gbsalermo/Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md
   → único roadmap canônico + etapa atual

3. gbsalermo/Rascomp-FRONT/docs/DOSSIE_PROJETO_RASCOMP.md
   → arquitetura e decisões cross-repo

4. rascomp/docs/CONTINUIDADE.md
   → checkpoint vivo deste backend
```

Estado documentado em 31/08/2026:

```text
ETAPA 0  ✅ concluída / validada
ETAPA 1  🚧 atual
ETAPA 2+ ⏳ não iniciadas
```

---

## Documentos vivos / de decisão

### `CONTINUIDADE.md`

Checkpoint atual do backend: stack, migrations, testes, segurança, riscos da etapa atual e pendências.

### `ETAPAS_POS_PROJETO.md`

Ponteiro para o roadmap canônico cross-repo. Não deve duplicar a sequência completa.

### `DOSSIE_PROJETO.md`

Ponteiro para o Dossiê Mestre cross-repo. Não deve duplicar o dossiê.

### `CLOUDFLARE_R2.md`

Referência da abstração/storage R2 preparada para mídia futura.

### `DECISAO_DEPLOY_CLOUD.md`

Decisão de arquitetura do deploy futuro. O deploy continua reservado à ETAPA 14.

### `DEPLOY_CLOUDFLARE.md`

Ponteiro/guia relacionado ao deploy futuro.

---

## Referência técnica/histórica

Os arquivos abaixo são úteis para recuperar contratos e decisões de fases anteriores, mas **não devem ser tratados automaticamente como estado atual**:

```text
CONGELAMENTO_API.md
ENDPOINTS_INTERNOS.md
ENTIDADES_E_CRUDS.md
FLUXO_DO_SISTEMA.md
JSON_EXEMPLOS.md
POS_SWAGGER_MODALIDADES_E_CATEGORIAS.md
POS_SWAGGER_USUARIOS_EQUIPES_INSCRICAO.md
TESTES_POSTMAN.md
diagrama-uml-completo.puml
```

Antes de aplicar algo descrito neles, conferir:

```text
código atual
migrations atuais
testes atuais
CONTINUIDADE.md
Dossiê Mestre
```

Se houver conflito de ordem de execução, o documento que prevalece é:

```text
Rascomp-FRONT/docs/ETAPAS_POS_PROJETO.md
```

---

## Regras importantes do backend

```text
backend = fonte de verdade de domínio/autorização
V1–V7 = migrations já aplicadas, não reescrever
V8+ = próxima mudança estrutural
modo local deve continuar funcional
ETAPA atual = ETAPA 1
não avançar sem validação
```

Em 31/08/2026:

```text
roles implementadas = ORGANIZACAO | PARTICIPANTE
rascomp/bin/ ainda rastreado = sim, reservado à ETAPA 2
último checkpoint documentado = 48 testes / 0 falhas / 0 erros
```

Não atualizar status ou contagem de testes por inferência; registrar somente após validação real.