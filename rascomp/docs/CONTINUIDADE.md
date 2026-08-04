# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-04T19:07:00-03:00

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

## 3. Regra de trabalho

- O mantenedor implementa e realiza os commits.
- O assistente orienta, revisa e prepara códigos de referência.
- Toda etapa concluída deve ser registrada neste documento.
- Testes só devem ser marcados como concluídos após execução local.

## 4. Status atual

### Projeto base

Status: **concluído**

- Projeto Rascomp criado.
- Package root definido.
- Estrutura inicial de pacotes criada.
- H2 definido como banco inicial.
- PostgreSQL, Flyway e Camunda planejados para etapas posteriores.

### CRUD `CompetitionCategory`

Status: **implementado — aguardando testes com H2**

Arquivos concluídos:

- `CompetitionCategory`
- `Modalidade`
- `CompetitionCategoryDTO`
- `CompetitionCategoryRepository`
- `CompetitionCategoryService`
- `CompetitionCategoryController`

Decisões:

- uma modalidade pode possuir várias categorias;
- exemplos: Mini Sumô, Sumô 3 kg e Seguidor de Linha;
- campo `ativo` mantido;
- exclusão lógica;
- consultas por modalidade e por modalidade ativa;
- `ConfigSumo` e `ConfigFollow` são configurações dependentes da categoria.

### `ConfigSumo`

Status: **em implementação**

Campos definidos:

- `id`
- `competitionCategory`
- `pesoMax`
- `exigeInspecao`
- `maxTentativasInspecao`
- `numeroRounds`
- `roundsParaVencer`
- `permiteRoundDesempate`

Decisões:

- não haverá peso mínimo;
- o peso real do robô será registrado futuramente no módulo de inspeção;
- `numeroRounds` representa apenas os rounds regulares;
- rounds adicionais poderão ocorrer quando houver empate, anulação, cancelamento ou problema técnico;
- rounds sem vencedor não contam para `roundsParaVencer`.

### `ConfigFollow`

Status: **entity definida — CRUD pendente**

Campos definidos:

- `id`
- `competitionCategory`
- `numeroTomadas`
- `tentativasPorTomada`
- `maxTempoSegundos`
- `numeroCheckpoints`

Decisões:

- `ConfigFollow` guarda apenas regras fixas da categoria;
- cada robô poderá possuir várias tomadas e várias tentativas por tomada;
- tempos realizados, conclusão, validade, checkpoints alcançados e penalidades pertencem ao módulo de resultados;
- o melhor tempo será calculado sob demanda, não armazenado diretamente na configuração.

## 5. Entidade futura — `TentativaSeguidorLinha`

Status: **planejada para depois de `Registration`**

Responsabilidade:

- registrar cada tentativa real de um robô no Seguidor de Linha;
- ser vinculada a uma `Registration`;
- armazenar número da tomada;
- armazenar número da tentativa;
- registrar se a tentativa foi concluída;
- registrar o tempo obtido;
- prever status como válida, anulada, cancelada ou não concluída;
- registrar checkpoints alcançados e penalidades quando essas regras forem formalizadas.

Regra de apuração planejada:

1. Agrupar tentativas por tomada.
2. Em cada tomada, selecionar o menor tempo válido.
3. Entre as tomadas válidas, selecionar o menor tempo final do robô.
4. Usar esse resultado para classificação e ranking.

Também será criado futuramente um serviço de apuração/ranking do Seguidor de Linha.

## 6. Próximos passos

1. Testar `CompetitionCategory` com H2.
2. Finalizar e testar `ConfigSumo`.
3. Implementar e testar o CRUD de `ConfigFollow`.
4. Integrar as configurações à resposta de `CompetitionCategory`, se necessário.
5. Criar testes automatizados do módulo de categorias.
6. Implementar `Institution`.
7. Implementar `Team`.
8. Implementar `Competitor`.
9. Implementar `Robot`.
10. Implementar `Competition`.
11. Implementar `Registration`.
12. Implementar `TentativaSeguidorLinha`.
13. Implementar o serviço de apuração e ranking do Seguidor de Linha.
14. Implementar `Bracket`.
15. Implementar `Match` e `MatchResult`.
16. Integrar Camunda.
17. Implementar segurança JWT.
18. Preparar PostgreSQL, Docker e Flyway.

## 7. Backlog de regras futuras

### Sumô

- inspeção obrigatória conforme configuração;
- limite de tentativas de inspeção;
- validação de peso;
- entidade de round;
- vitória, empate, anulação, cancelamento e desclassificação.

### Seguidor de Linha

- entidade `TentativaSeguidorLinha`;
- apuração do melhor tempo;
- checkpoints e penalidades;
- timeout;
- classificação e ranking.

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
- 2026-07-29 — Estrutura inicial e códigos de referência criados.
- 2026-08-03 — CRUD `CompetitionCategory` marcado como implementado.
- 2026-08-04 — Planejamento atualizado com `ConfigSumo`, `ConfigFollow` e futura entidade `TentativaSeguidorLinha` vinculada a `Registration`.
