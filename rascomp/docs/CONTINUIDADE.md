# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-17T23:10:00-03:00

## 1. Objetivo

Plataforma para gestão de competições de robôs da RAS-UFRB, com backend Spring Boot, inscrições, equipes, robôs, categorias, resultados, chaveamentos e integração futura com Camunda.

Meta imediata: concluir o projeto base com **backend + dois frontends até 30/08/2026**. Por isso, as próximas decisões devem priorizar fechamento do núcleo funcional, integração com frontend e redução de escopo não essencial.

## 2. Stack e convenções

- Java 21
- Spring Boot 3
- Spring Data JPA / Hibernate
- Jakarta Validation
- Lombok
- MySQL como banco persistente principal
- Flyway como responsável pelas migrations do schema da aplicação
- H2 mantido apenas como apoio para testes/perfis específicos
- Camunda 7 embarcado
- Swagger / OpenAPI
- Package root: `br.edu.ufrb.rascomp`
- Estrutura por camadas: `controller`, `dto`, `model`, `repository`, `service`, `exception`
- relacionamentos carregados com `FetchType.LAZY` por padrão;
- exclusão lógica para entidades principais;
- exclusão física apenas para registros dependentes sem histórico próprio, quando adequado;
- testes e validações só são marcados como concluídos após execução local.

### Persistência definitiva

Decisão fechada: **MySQL + Spring Data JPA + Flyway**.

A arquitetura de persistência fica:

```text
Spring Boot
    -> Spring Data JPA / Hibernate
    -> JDBC
    -> MySQL
```

JDBC puro não será usado como substituto do JPA, pois aumentaria SQL e mapeamento manual sem benefício relevante para o escopo do Rascomp.

Alterações já aplicadas:

- removido driver PostgreSQL;
- adicionado `mysql-connector-j`;
- substituído módulo Flyway PostgreSQL por `flyway-mysql`;
- `application.properties` passou a apontar para MySQL;
- Hibernate agora usa `ddl-auto=validate`;
- Flyway habilitado em `classpath:db/migration`;
- criada migration inicial `V1__create_rascomp_schema.sql`;
- tabela `Institution` normalizada para `institutions` para evitar diferença de case entre ambientes MySQL.

Configuração padrão atual:

```text
DB_URL=jdbc:mysql://localhost:3306/rascomp?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bahia
DB_USERNAME=root
DB_PASSWORD=
```

Os valores podem ser sobrescritos por variáveis de ambiente.

O Camunda continua usando o mesmo datasource e mantém o gerenciamento de suas próprias tabelas.

## 3. Estratégia de desenvolvimento

O mantenedor implementa e realiza testes locais.

O assistente orienta, revisa e, quando solicitado, implementa diretamente no repositório.

O arquivo local de referência permanece:

`docs/CODIGOS_REFERENCIA.md`

Esse arquivo deve permanecer no `.gitignore` e não deve ser versionado.

Arquivo versionado de testes:

`docs/TESTES_POSTMAN.md`

Ele concentra endpoints, bodies JSON, casos positivos/negativos e será usado na bateria final. A decisão atual é executar os testes completos depois de finalizar as regras avançadas restantes.

## 4. Status atual

### Projeto base

Status: **backend estruturalmente avançado; fechamento funcional em andamento**

- CRUDs principais implementados;
- DataInitializer expandido;
- aplicação já validada anteriormente com H2/JPA/Camunda;
- persistência migrada para MySQL + Flyway, ainda aguardando validação local no novo banco;
- Swagger/OpenAPI já presente;
- autenticação/JWT permanece pós-fechamento do núcleo funcional caso o prazo exija priorização.

### CRUDs principais

Status: **implementados e smoke tests manuais iniciais executados**

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

Status: **implementação/conferência final pendente na bateria de testes**

Objetivo:

- substituir respostas genéricas do Spring por erros padronizados para recurso inexistente, regra de negócio, validação, parâmetros inválidos e conflitos de integridade.

### ETAPA A — `TentativaSeguidorLinha` + `ConfigFollow`

Status: **implementado — testes finais pendentes**

Regras:

- busca obrigatória do `ConfigFollow` da categoria;
- tomada limitada por `numeroTomadas`;
- tentativa limitada por `tentativasPorTomada`;
- checkpoints limitados por `numeroCheckpoints`;
- tempo acima de `maxTempoSegundos` registra a tentativa, mas força `valida = false`;
- regras aplicadas em criação e atualização.

### ETAPA B — Ranking do Seguidor de Linha

Status: **implementado — testes finais pendentes**

Arquivos:

- `RankingFollowDTO`;
- `RankingFollowService`;
- `RankingFollowController`.

Regras:

- ranking por competição/categoria;
- categoria ativa `FOLLOW_LINE`;
- inscrições ativas e aprovadas;
- tentativas válidas, concluídas e com tempo;
- tempo final = tempo bruto + penalidade;
- melhor tentativa de cada inscrição;
- desempate por tempo final, tempo bruto e `registrationId`;
- endpoint `GET /api/v1/ranking/seguidor-linha?competitionId={id}&categoryId={id}`.

### ETAPA C — geração automática da primeira rodada

Status: **implementado — testes finais pendentes**

Novo serviço:

- `BracketGenerationService`.

Novo endpoint:

```text
POST /api/v1/chaveamentos/gerar?competitionId={id}&categoryId={id}
```

Regras:

- competição e categoria devem existir e estar ativas;
- impede geração se já existir chaveamento para competição/categoria;
- considera apenas inscrições `APROVADA` e ativas;
- exige no mínimo duas inscrições elegíveis;
- ordenação inicial determinística por `registrationId`;
- calcula a próxima potência de dois para definir o tamanho da chave;
- cria BYEs quando a quantidade de participantes não completa a potência de dois;
- partidas com dois participantes começam `AGENDADA`;
- partidas com um participante começam `BYE`;
- chaveamento passa para `GERADO`.

### ETAPA D — árvore completa do chaveamento

Status: **implementado — testes finais pendentes**

Evoluções:

- a geração não cria apenas a primeira rodada;
- todas as rodadas até a final são criadas de uma vez;
- rodadas futuras nascem sem participantes;
- adicionado `StatusMatch.AGUARDANDO_PARTICIPANTES` para esses slots;
- a estrutura futura permite que a ETAPA E apenas preencha os slots conforme os vencedores avançarem.

Exemplo para chave de 8:

```text
Rodada 1: 4 partidas
Rodada 2: 2 partidas
Rodada 3: 1 final
```

### ETAPA E — avanço automático de vencedor / BYE

Status: **próxima implementação**

Objetivo:

- preencher automaticamente a próxima partida;
- partida de ordem ímpar alimenta `registrationA` da próxima partida;
- partida de ordem par alimenta `registrationB`;
- BYE avança sem exigir resultado manual;
- quando ambos os slots estiverem preenchidos, partida passa de `AGUARDANDO_PARTICIPANTES` para `AGENDADA`;
- finalização da última partida encerra o chaveamento.

### Sumô

Status: **regras avançadas pendentes**

Próxima modelagem esperada:

- inspeção por inscrição;
- limite de tentativas de inspeção;
- peso máximo conforme `ConfigSumo`;
- rounds por partida;
- consolidação automática do vencedor;
- desclassificação, ausência e desempate.

## 5. Estratégia de testes

Os testes completos serão executados ao final das regras avançadas para evitar retrabalho durante a corrida para o frontend.

Arquivo principal:

`docs/TESTES_POSTMAN.md`

A bateria final deverá validar:

- CRUDs;
- filtros e consultas por relacionamento;
- soft delete e reativação;
- tratamento global de exceções;
- ConfigFollow;
- ranking;
- geração automática da árvore de chaveamento;
- BYEs;
- avanço de vencedores;
- inspeção/rounds do Sumô;
- persistência real após reiniciar a aplicação com MySQL.

## 6. Prioridade até 30/08/2026

Para chegar aos dois frontends sem deixar o backend aberto, a ordem passa a ser:

1. **ETAPA E** — avanço automático de vencedores + BYE.
2. Implementar inspeção do Sumô na versão mínima necessária.
3. Implementar rounds do Sumô e resultado consolidado.
4. Fazer uma revisão curta dos endpoints necessários pelos frontends.
5. Subir e validar MySQL + Flyway localmente.
6. Executar bateria final essencial de `TESTES_POSTMAN.md`.
7. Congelar o contrato da API base.
8. Ir imediatamente para os dois frontends.
9. Testes automatizados, JWT e refinamentos que não bloqueiem o frontend podem ser feitos em paralelo/depois do contrato principal estar estável.

## 7. Histórico resumido

- 2026-07-28 — documento inicial e planejamento do backend.
- 2026-07-29 — projeto renomeado para Rascomp e package root ajustado.
- 2026-08-03 — CRUD `CompetitionCategory` implementado.
- 2026-08-04 — `ConfigSumo`, `ConfigFollow` e `TentativaSeguidorLinha` planejados.
- 2026-08-05 — CRUD `Institution` concluído.
- 2026-08-06 — CRUDs `Team`, `Competitor` e `Robot` implementados.
- 2026-08-06 — CRUDs `Competition`, `Registration`, `TentativaSeguidorLinha`, `Bracket`, `Match` e `MatchResult` implementados.
- 2026-08-10 — aplicação validada subindo com H2, JPA, Camunda e DataInitializer.
- 2026-08-17 — `ConfigFollow` passou a validar `TentativaSeguidorLinha`.
- 2026-08-17 — primeira versão do ranking do Seguidor de Linha implementada.
- 2026-08-17 — bateria final consolidada em `docs/TESTES_POSTMAN.md`.
- 2026-08-17 — persistência definitiva alterada para MySQL + Flyway.
- 2026-08-17 — ETAPA C implementada: geração automática da primeira rodada com BYE.
- 2026-08-17 — ETAPA D implementada: geração da árvore completa do chaveamento.
