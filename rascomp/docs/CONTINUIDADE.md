# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-06T20:38:00-03:00

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
- DataInitializer disponível para dados de teste;
- PostgreSQL, Flyway, Docker, JWT e Camunda planejados para etapas posteriores.

### `CompetitionCategory`, `ConfigSumo` e `ConfigFollow`

Status: **implementados — testes pendentes**

Decisões principais:

- categoria define a modalidade;
- `ConfigSumo` e `ConfigFollow` são configurações dependentes da categoria;
- exclusão e validações específicas permanecem conforme o padrão já implementado.

### `Institution`

Status: **implementado — testes pendentes**

Regras principais:

- sigla única;
- normalização de dados;
- exclusão lógica e reativação;
- listagem completa e somente ativas.

### `Team`

Status: **implementado — testes pendentes**

Regras principais:

- relacionamento `ManyToOne` com `Institution`;
- nome único dentro da instituição;
- instituição deve estar ativa;
- exclusão lógica e reativação.

### `Competitor`

Status: **implementado — testes pendentes**

Regras principais:

- relacionamento `ManyToOne` com `Team`;
- e-mail único e normalizado;
- equipe e instituição devem estar ativas;
- exclusão lógica e reativação.

### `Robot`

Status: **implementado — testes pendentes**

Regras principais:

- relacionamento `ManyToOne` com `Team`;
- nome único dentro da equipe;
- equipe e instituição devem estar ativas;
- categoria não é armazenada diretamente no robô;
- categoria será definida por meio de `Registration`;
- exclusão lógica e reativação.

Correção realizada:

- `serialVersionUID` corrigido na entidade `Robot`.

### `Competition`

Status: **implementado — testes pendentes**

Arquivos:

- `Competition`
- `CompetitionDTO`
- `CompetitionRepository`
- `CompetitionService`
- `CompetitionController`
- `StatusCompetition`

Regras principais:

- período de inscrições;
- período de realização;
- validação da ordem das datas;
- nome único;
- status da competição;
- exclusão lógica e reativação;
- listagem por status.

### `Registration`

Status: **implementado — testes pendentes**

Arquivos:

- `Registration`
- `RegistrationDTO`
- `RegistrationRepository`
- `RegistrationService`
- `RegistrationController`
- `StatusRegistration`

Relacionamentos:

- `Competition`;
- `CompetitionCategory`;
- `Team`;
- `Robot`.

Regras principais:

- competição, categoria, equipe, instituição e robô devem estar ativos;
- criação permitida apenas com inscrições abertas e dentro do período configurado;
- robô deve pertencer à equipe informada;
- um robô não pode ser inscrito duas vezes na mesma categoria da mesma competição;
- exclusão lógica altera o status para `CANCELADA`;
- reativação retorna a inscrição para `PENDENTE`.

### `TentativaSeguidorLinha`

Status: **implementado — testes pendentes**

Arquivos:

- `TentativaSeguidorLinha`
- `TentativaSeguidorLinhaDTO`
- `TentativaSeguidorLinhaRepository`
- `TentativaSeguidorLinhaService`
- `TentativaSeguidorLinhaController`

Regras principais:

- pertence a uma `Registration`;
- inscrição deve estar ativa e aprovada;
- categoria deve possuir modalidade `FOLLOW_LINE`;
- combinação de inscrição, tomada e número da tentativa é única;
- armazena tempo, checkpoints, penalidade, conclusão e validade;
- exclusão física adotada por ser registro dependente da inscrição.

Pendente para evolução:

- validar quantidade máxima de tomadas e tentativas usando `ConfigFollow`;
- validar tempo máximo e quantidade máxima de checkpoints;
- criar serviço de melhor tempo e ranking.

### `Bracket`

Status: **implementado — testes pendentes**

Arquivos:

- `Bracket`
- `BracketDTO`
- `BracketRepository`
- `BracketService`
- `BracketController`
- `StatusBracket`

Regras principais:

- pertence a uma competição e categoria;
- somente um chaveamento por competição e categoria;
- competição e categoria devem estar ativas;
- exclusão lógica altera o status para `CANCELADO`;
- geração automática das partidas ainda não foi implementada.

### `Match`

Status: **implementado — testes pendentes**

Arquivos:

- `Match`
- `MatchDTO`
- `MatchRepository`
- `MatchService`
- `MatchController`
- `StatusMatch`

Regras principais:

- pertence a um `Bracket`;
- combinação de chaveamento, rodada e ordem é única;
- participantes são inscrições aprovadas da mesma competição e categoria do chaveamento;
- participantes devem ser diferentes;
- permite um participante nulo para representar `BYE`;
- exclusão lógica altera o status para `CANCELADA`.

### `MatchResult`

Status: **implementado — testes pendentes**

Arquivos:

- `MatchResult`
- `MatchResultDTO`
- `MatchResultRepository`
- `MatchResultService`
- `MatchResultController`

Regras principais:

- relacionamento `OneToOne` com `Match`;
- uma partida possui no máximo um resultado;
- vencedor deve ser um dos participantes;
- resultado com pontos diferentes exige vencedor;
- resultado empatado não aceita vencedor;
- salvar resultado finaliza a partida;
- exclusão física do resultado restaura o status inicial da partida.

Pendente para evolução:

- modelagem específica de rounds do Sumô;
- avanço automático do vencedor no chaveamento;
- tratamento de anulação, desclassificação e vitória por ausência.

## 5. Decisões de modelagem

- `Robot` não possui modalidade nem categoria diretamente.
- `Registration` é o ponto de ligação entre competição, categoria, equipe e robô.
- `TentativaSeguidorLinha` é dependente de uma inscrição aprovada.
- `Bracket` representa um chaveamento de uma categoria dentro de uma competição.
- `MatchResult` foi separado de `Match` para não sobrecarregar a entidade da partida.
- partidas aceitam participante nulo para suportar `BYE`.
- a geração automática do chaveamento e o avanço de vencedores serão serviços específicos posteriores, não parte do CRUD básico.

## 6. Próximas etapas

1. Executar compilação completa do projeto.
2. Corrigir erros de métodos derivados ou imports encontrados durante a inicialização.
3. Testar todos os CRUDs com H2 e Postman.
4. Atualizar o `DataInitializer` com dados coerentes para todos os relacionamentos.
5. Criar tratamento global de exceções.
6. Criar testes automatizados.
7. Implementar geração automática de chaveamento.
8. Implementar avanço automático de vencedores.
9. Implementar apuração e ranking do Seguidor de Linha.
10. Implementar regras detalhadas de rounds e inspeção do Sumô.
11. Integrar Camunda.
12. Implementar segurança JWT.
13. Preparar PostgreSQL, Docker e Flyway.

## 7. Histórico resumido

- 2026-07-28 — Documento inicial e planejamento do backend.
- 2026-07-29 — Projeto renomeado para Rascomp e package root ajustado.
- 2026-08-03 — CRUD `CompetitionCategory` implementado.
- 2026-08-04 — `ConfigSumo`, `ConfigFollow` e `TentativaSeguidorLinha` planejados.
- 2026-08-05 — CRUD `Institution` concluído.
- 2026-08-06 — CRUDs `Team`, `Competitor` e `Robot` implementados.
- 2026-08-06 — CRUDs `Competition`, `Registration`, `TentativaSeguidorLinha`, `Bracket`, `Match` e `MatchResult` implementados.
- 2026-08-06 — Todos os módulos permanecem com testes locais pendentes.
