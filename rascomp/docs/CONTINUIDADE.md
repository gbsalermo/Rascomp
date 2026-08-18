# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-17T22:59:00-03:00

## 1. Objetivo

Plataforma para gestão de competições de robôs da RAS-UFRB, com backend Spring Boot, inscrições, equipes, robôs, categorias, resultados, chaveamentos e integração futura com Camunda.

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
- relacionamentos carregados com `FetchType.LAZY` por padrão;
- exclusão lógica para entidades principais;
- exclusão física apenas para registros dependentes sem histórico próprio, quando adequado;
- testes e validações só são marcados como concluídos após execução local.

## 3. Estratégia de desenvolvimento

O mantenedor implementa e realiza testes locais.

O assistente orienta, revisa e, quando solicitado, implementa diretamente no repositório.

O arquivo local de referência permanece:

`docs/CODIGOS_REFERENCIA.md`

Esse arquivo deve permanecer no `.gitignore` e não deve ser versionado.

## 4. Status atual

### Projeto base

Status: **concluído**

- estrutura inicial criada;
- H2 configurado como banco de desenvolvimento;
- DataInitializer expandido com cenário integrado para testes;
- aplicação já subiu corretamente com os relacionamentos JPA e dados de teste;
- PostgreSQL, Flyway, Docker e JWT permanecem para etapas posteriores.

### CRUDs principais

Status: **implementados e smoke tests manuais executados**

Módulos:

- `CompetitionCategory`, `ConfigSumo`, `ConfigFollow`;
- `Institution`;
- `Team`;
- `Competitor`;
- `Robot`;
- `Competition`;
- `Registration`;
- `TentativaSeguidorLinha`;
- `Bracket`;
- `Match`;
- `MatchResult`.

Decisões principais mantidas:

- `Robot` não possui modalidade/categoria diretamente;
- `Registration` liga competição, categoria, equipe e robô;
- `Bracket` representa o chaveamento de uma categoria dentro de uma competição;
- `MatchResult` permanece separado de `Match`;
- partidas aceitam participante nulo para suportar `BYE`.

### Tratamento global de exceções

Status: **etapa iniciada/conforme implementação local**

Objetivo:

- substituir respostas genéricas do Spring por erros padronizados para recurso inexistente, regra de negócio, validação, parâmetros inválidos e conflitos de integridade.

### `TentativaSeguidorLinha` + `ConfigFollow`

Status: **implementado — testes locais pendentes**

Regras agora aplicadas no `TentativaSeguidorLinhaService`:

- busca obrigatória do `ConfigFollow` da categoria da inscrição;
- tomada deve estar entre 1 e `numeroTomadas`;
- número da tentativa deve estar entre 1 e `tentativasPorTomada`;
- checkpoints devem estar entre 0 e `numeroCheckpoints`;
- tentativa acima de `maxTempoSegundos` continua registrada, mas é marcada automaticamente como inválida;
- as mesmas regras são aplicadas na criação e atualização;
- validade final deixa de depender somente do valor enviado pelo cliente.

### Ranking do Seguidor de Linha

Status: **implementado — testes locais pendentes**

Arquivos adicionados:

- `RankingFollowDTO`;
- `RankingFollowService`;
- `RankingFollowController`.

Regras da primeira versão:

- ranking por competição e categoria;
- categoria deve ser ativa e `FOLLOW_LINE`;
- considera somente inscrições ativas e aprovadas;
- considera somente tentativas válidas, concluídas e com tempo registrado;
- tempo final = tempo bruto + penalidade;
- seleciona a melhor tentativa de cada inscrição;
- ordena por menor tempo final;
- em empate, usa menor tempo bruto e depois `registrationId`;
- atribui posição sequencial;
- endpoint: `GET /api/v1/ranking/seguidor-linha?competitionId={id}&categoryId={id}`.

### `Bracket`, `Match` e `MatchResult`

Status: **CRUD implementado; automações pendentes**

Ainda faltam:

- geração automática do chaveamento;
- geração das rodadas futuras;
- avanço automático de vencedores;
- tratamento automático de `BYE`;
- regras específicas de Sumô.

### Sumô

Status: **regras avançadas pendentes**

Próxima modelagem esperada:

- inspeção do robô por inscrição;
- limite de tentativas de inspeção;
- peso máximo conforme `ConfigSumo`;
- rounds por partida;
- consolidação automática do vencedor;
- desclassificação, ausência e desempate.

## 5. Próximas etapas

1. Compilar/subir o projeto após as ETAPAS A e B.
2. Testar limites de `ConfigFollow` via Postman.
3. Testar ranking do Seguidor de Linha com múltiplos robôs e penalidades.
4. Criar testes automatizados para as regras já implementadas.
5. Implementar geração automática de chaveamento.
6. Implementar geração da árvore completa de partidas.
7. Implementar avanço automático de vencedores e `BYE`.
8. Implementar inspeção e rounds do Sumô.
9. Integrar Camunda aos fluxos que realmente precisarem de processo/orquestração.
10. Implementar segurança JWT.
11. Preparar PostgreSQL, Docker e Flyway.

## 6. Histórico resumido

- 2026-07-28 — Documento inicial e planejamento do backend.
- 2026-07-29 — Projeto renomeado para Rascomp e package root ajustado.
- 2026-08-03 — CRUD `CompetitionCategory` implementado.
- 2026-08-04 — `ConfigSumo`, `ConfigFollow` e `TentativaSeguidorLinha` planejados.
- 2026-08-05 — CRUD `Institution` concluído.
- 2026-08-06 — CRUDs `Team`, `Competitor` e `Robot` implementados.
- 2026-08-06 — CRUDs `Competition`, `Registration`, `TentativaSeguidorLinha`, `Bracket`, `Match` e `MatchResult` implementados.
- 2026-08-10 — aplicação validada subindo com H2, JPA, Camunda e DataInitializer.
- 2026-08-17 — `ConfigFollow` passou a validar `TentativaSeguidorLinha`.
- 2026-08-17 — primeira versão do ranking do Seguidor de Linha implementada.
