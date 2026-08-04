# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-03T22:45:54-03:00

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

O mantenedor implementa e realiza os commits.

O assistente orienta, revisa e prepara códigos de referência.

Toda etapa concluída deve ser registrada neste documento.

Testes e validações só devem ser marcados como concluídos após execução local.


4. Status atual

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

armazenar configurações específicas de categorias da modalidade SUMO;

possuir relacionamento OneToOne com CompetitionCategory;

impedir associação com categoria de outra modalidade;

não existir como configuração solta sem categoria.

Campos iniciais:

id

competitionCategory

pesoMin

pesoMax

exigeInspecao

maxTentativasInspecao

5.3 Implementar ConfigFollow

Status: pendente

Responsabilidade:

armazenar configurações específicas de categorias da modalidade SEGUIDOR_LINHA;

possuir relacionamento OneToOne com CompetitionCategory;

impedir associação com categoria de outra modalidade;

não existir como configuração solta sem categoria.

Campos iniciais:

id

competitionCategory

maxTempoSegundos

numeroCheckpoints

Os códigos de referência dessas duas configurações estão no documento:

CODIGOS_REFERENCIA_CONFIGURACOES.md

6. Ordem de implementação

Testar CompetitionCategory com H2.

Corrigir problemas encontrados nos testes.

Implementar ConfigSumo.

Testar ConfigSumo.

Implementar ConfigFollow.

Testar ConfigFollow.

Integrar as configurações à resposta de CompetitionCategory, caso necessário.

Criar testes automatizados do módulo de categorias.

Implementar Institution.

Implementar Team.

Implementar Competitor.

Implementar Robot.

Implementar Competition.

Implementar Registration.

Implementar Bracket.

Implementar Match e MatchResult.

Integrar Camunda.

Implementar segurança JWT.

Preparar PostgreSQL, Docker e Flyway.

## 7. Backlog de regras futuras

### Sumô

inspeção obrigatória conforme configuração;

limite de tentativas de inspeção;

desclassificação após tentativas reprovadas;

validação de peso;

critérios de vitória, penalidade e desclassificação.

Seguidor de Linha

tempo máximo;

checkpoints;

penalidades;

critério de classificação por menor tempo;

desclassificação por exceder limite definido.

Chaveamento e partidas

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

2026-08-04T19:00:48-03:00 — Planejamento atualizado: incluída a entidade futura TentativaSeguidorLinha, vinculada a Registration, e o serviço de apuração do melhor tempo e ranking do Seguidor de Linha.