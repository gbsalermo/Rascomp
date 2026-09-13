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
ETAPA 3  🚧 em andamento
ETAPA 4+ ⏳ não iniciadas
```

As ETAPAS 1 e 2 estão encerradas/validadas. A ETAPA 3 está em andamento; o backend já migrou para a nova matriz de permissões com Flyway V13.

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


## ETAPA 3 — checkpoint inicial do backend — 13/09/2026

- `UserRole = DEV | GESTAO | MIDIA | PARTICIPANTE`;
- V13 migra `ORGANIZACAO → DEV`;
- `DEV` possui administração de usuários;
- `DEV/GESTAO` possuem operação competitiva;
- `MIDIA` não herda operação competitiva;
- bootstrap inicial cria DEV;
- verificações de negócio usam capacidade semântica;
- Backend Tests #298 (PR) e #299 (main) verdes;
- MySQL + Flyway V13 + `testdata` verdes.
