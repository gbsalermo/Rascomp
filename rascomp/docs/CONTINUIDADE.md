# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-06T11:20:00-03:00

## 1. Objetivo

Plataforma para gestão de competições de robôs da RAS-UFRB, com backend Spring Boot, painel de gestão, vitrine pública, inscrições, equipes, robôs, categorias, resultados, chaveamentos e integração futura com Camunda.

## 2. Stack e convenções

- Java 21
- Spring Boot 3
- Spring Data JPA
- Jakarta Validation
- Lombok
- H2 durante o desenvolvimento
- PostgreSQL na produção
- Package root: `br.edu.ufrb.rascomp`
- Estrutura por camadas: `controller`, `dto`, `model`, `repository`, `service`, `exception`

## 3. Estratégia de desenvolvimento

O mantenedor implementa e realiza os commits.

O assistente orienta, revisa e prepara referências conceituais ou implementações completas quando isso for solicitado explicitamente para acelerar o projeto.

O único arquivo local de referência será:

`docs/CODIGOS_REFERENCIA.md`

Esse arquivo deve permanecer no `.gitignore` e não deve ser versionado.

Por padrão, as referências devem priorizar raciocínio, regras, relacionamentos, métodos esperados e checklist. Classes completas podem ser fornecidas quando o usuário pedir explicitamente um CRUD pronto para implementar.

Toda etapa concluída deve ser registrada neste documento.

Testes e validações só devem ser marcados como concluídos após execução local.

## 4. Status atual

### Projeto base

Status: **concluído**

- Projeto Rascomp criado.
- Package root definido.
- Estrutura inicial de pacotes criada.
- H2 definido como banco inicial.
- DataInitializer criado para dados de teste.
- PostgreSQL, Flyway e Camunda planejados para etapas posteriores.

### CRUD `CompetitionCategory`

Status: **implementado — testes pendentes**

Arquivos concluídos:

- `CompetitionCategory`
- `Modalidade`
- `CompetitionCategoryDTO`
- `CompetitionCategoryRepository`
- `CompetitionCategoryService`
- `CompetitionCategoryController`

### CRUD `ConfigSumo`

Status: **implementado — testes pendentes**

Campos principais:

- `competitionCategory`
- `pesoMax`
- `exigeInspecao`
- `maxTentativasInspecao`
- `numeroRounds`
- `roundsParaVencer`
- `permiteRoundDesempate`

### CRUD `ConfigFollow`

Status: **implementado — testes pendentes**

Campos principais:

- `competitionCategory`
- `numeroTomadas`
- `tentativasPorTomada`
- `maxTempoSegundos`
- `numeroCheckpoints`

### CRUD `Institution`

Status: **implementado — testes pendentes**

Arquivos concluídos:

- `Institution`
- `InstitutionDTO`
- `InstitutionRepository`
- `InstitutionService`
- `InstitutionController`

Regras implementadas:

- sigla única, ignorando diferença entre maiúsculas e minúsculas;
- normalização de nome, sigla, cidade e estado;
- listagem completa e apenas de instituições ativas;
- busca por ID e por sigla;
- exclusão lógica;
- reativação;
- atualização com validação de duplicidade da sigla.

### CRUD `Team`

Status: **implementado — ajustes finais e testes pendentes**

Arquivos existentes:

- `Team`
- `TeamDTO`
- `TeamRepository`
- `TeamService`
- `TeamController`

Regras implementadas:

- relacionamento `ManyToOne` com `Institution`;
- instituição obrigatória e validada no Service;
- bloqueio de vínculo com instituição inativa;
- nome único dentro da mesma instituição;
- listagem completa e apenas de equipes ativas;
- filtro por instituição;
- exclusão lógica;
- reativação condicionada à instituição ativa;
- atualização com validação de duplicidade.

Ajustes ainda recomendados antes dos testes:

- usar tabela `teams` em minúsculo;
- definir `ativo` como `nullable = false`;
- inicializar `ativo` com `true` ou garantir sempre seu preenchimento pelo Service;
- remover import não utilizado de `Builder` em `Team`;
- executar a aplicação para validar os métodos derivados do `TeamRepository`.

Próximo CRUD: **Competitor**.

## 5. Entidades futuras

### `TentativaSeguidorLinha`

Status: **planejada para depois de `Registration`**

Responsabilidade:

- registrar tomadas e tentativas de uma inscrição;
- armazenar tempo, validade, checkpoints e penalidades;
- permitir apuração do melhor tempo sob demanda.

## 6. Ordem de implementação

1. Ajustar e testar `Team`.
2. Implementar `Competitor`.
3. Implementar `Robot`.
4. Implementar `Competition`.
5. Implementar `Registration`.
6. Implementar `Bracket`.
7. Implementar `Match` e `MatchResult`.
8. Criar tratamento global de exceções.
9. Criar testes automatizados.
10. Integrar Camunda.
11. Implementar segurança JWT.
12. Preparar PostgreSQL, Docker e Flyway.

Os testes manuais de `CompetitionCategory`, `ConfigSumo`, `ConfigFollow`, `Institution` e `Team` continuam pendentes e devem ser executados antes de considerar esses módulos validados.

## 7. Backlog de regras futuras

### Sumô

- inspeção obrigatória conforme configuração;
- limite de tentativas de inspeção;
- desclassificação após tentativas reprovadas;
- validação de peso;
- critérios de vitória, penalidade e desclassificação.

### Seguidor de Linha

- tempo máximo;
- checkpoints;
- penalidades;
- critério de classificação por menor tempo;
- desclassificação por exceder o limite definido.

### Chaveamento e partidas

- impedir participação de inscrições inelegíveis;
- gerar chaveamento;
- registrar resultados;
- avançar vencedores;
- emitir atualizações para a vitrine;
- integrar fluxo BPMN do Camunda.

## 8. Histórico resumido

- 2026-07-28 — Documento inicial e planejamento do backend.
- 2026-07-29 — Projeto renomeado para Rascomp e package root ajustado.
- 2026-08-03 — CRUD `CompetitionCategory` implementado.
- 2026-08-04 — `ConfigSumo`, `ConfigFollow` e futura `TentativaSeguidorLinha` planejados e implementados parcialmente.
- 2026-08-05 — CRUD `Institution` concluído.
- 2026-08-05 — Estratégia de códigos de referência alterada para orientação conceitual.
- 2026-08-06 — CRUD `Team` implementado, com ajustes finais e testes pendentes.
- 2026-08-06 — Próximo módulo definido: `Competitor`.
