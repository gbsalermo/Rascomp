Continuidade do Projeto — Rascomp

Última atualização: 2026-08-04T19:00:48-03:00

1. Objetivo

Plataforma para gestão de competições de robôs da RAS-UFRB, com:

backend Spring Boot;

painel de gestão autenticado;

vitrine pública;

categorias para Sumô e Seguidor de Linha;

inscrições, equipes, robôs, chaveamentos e partidas;

integração futura com Camunda;

PostgreSQL na etapa de produção.

2. Stack e convenções

Java 21

Spring Boot 3

Spring Data JPA

Jakarta Validation

Lombok

H2 durante o desenvolvimento

PostgreSQL na produção

Package root: br.edu.ufrb.rascomp

Estrutura por camadas: controller, dto, model, repository, service, exception

3. Regra de trabalho

O mantenedor implementa e realiza os commits.

O assistente orienta, revisa e prepara códigos de referência.

Toda etapa concluída deve ser registrada neste documento.

Testes e validações só devem ser marcados como concluídos após execução local.

4. Status atual

Projeto base

Status: concluído

Projeto Rascomp criado.

Package root definido.

Estrutura inicial de pacotes criada.

H2 definido como banco inicial.

PostgreSQL e Flyway planejados para etapa posterior.

Camunda planejado para depois dos CRUDs e da geração de chaveamento.

CRUD CompetitionCategory

Status: implementado — aguardando testes

Arquivos considerados concluídos:

CompetitionCategory

Modalidade

CompetitionCategoryDTO

CompetitionCategoryRepository

CompetitionCategoryService

CompetitionCategoryController

Decisões aplicadas:

Category foi substituída por CompetitionCategory.

Uma modalidade pode possuir várias categorias.

Exemplos:

Sumô 500 g → modalidade SUMO

Sumô 3 kg → modalidade SUMO

Seguidor de Linha Open → modalidade SEGUIDOR_LINHA

O campo codigo não será utilizado.

O campo ativo foi mantido.

A exclusão é lógica, definindo ativo = false.

Consultas por modalidade e por modalidade ativa foram mantidas.

ConfigSumo e ConfigFollow serão configurações dependentes da categoria.

Endpoints previstos:

POST   /api/v1/categorias
GET    /api/v1/categorias
GET    /api/v1/categorias/{id}
GET    /api/v1/categorias/por-modalidade?modalidade=SUMO
GET    /api/v1/categorias/por-modalidade/ativas?modalidade=SUMO
PUT    /api/v1/categorias/{id}
DELETE /api/v1/categorias/{id}

5. Próxima etapa

5.1 Testar CompetitionCategory com H2

Status: próximo passo

Validar:

Inicialização da aplicação.

Criação das tabelas pelo Hibernate.

Cadastro de categoria Sumô.

Cadastro de categoria Seguidor de Linha.

Listagem completa.

Busca por ID.

Filtro por modalidade.

Filtro por modalidade ativa.

Atualização.

Exclusão lógica.

Comportamento das validações do DTO.

Mensagem de categoria não encontrada.

Registrar depois dos testes:

comandos executados;

endpoints testados;

payloads principais;

erros encontrados;

correções aplicadas;

resultado final.

5.2 Implementar ConfigSumo

Status: pendente

Responsabilidade:

armazenar configurações específicas de categorias da modalidade SUMO;

possuir relacionamento OneToOne com CompetitionCategory;

impedir associação com categoria de outra modalidade;

não existir como configuração solta sem categoria.

Campos definidos:

id

competitionCategory

pesoMax

exigeInspecao

maxTentativasInspecao

numeroRounds

roundsParaVencer

permiteRoundDesempate

Decisões:

não haverá pesoMin;

o peso real do robô será registrado futuramente no módulo de inspeção;

numeroRounds representa apenas os rounds regulares;

rounds adicionais poderão ocorrer quando houver empate, anulação, cancelamento ou problema técnico.

5.3 Implementar ConfigFollow

Status: pendente

Responsabilidade:

armazenar configurações específicas de categorias da modalidade SEGUIDOR_LINHA;

possuir relacionamento OneToOne com CompetitionCategory;

impedir associação com categoria de outra modalidade;

não existir como configuração solta sem categoria.

Campos definidos:

id

competitionCategory

numeroTomadas

tentativasPorTomada

maxTempoSegundos

numeroCheckpoints

Decisões:

ConfigFollow guarda apenas regras fixas da categoria;

tempos realizados, conclusão, penalidades e validade pertencem ao módulo de resultados;

o melhor tempo não será armazenado diretamente na configuração.

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

Implementar TentativaSeguidorLinha vinculada à inscrição.

Implementar o serviço de apuração e ranking do Seguidor de Linha.

Implementar Bracket.

Implementar Match e MatchResult.

Integrar Camunda.

Implementar segurança JWT.

Preparar PostgreSQL, Docker e Flyway.

7. Backlog de regras futuras

Sumô

inspeção obrigatória conforme configuração;

limite de tentativas de inspeção;

desclassificação após tentativas reprovadas;

validação de peso;

critérios de vitória, penalidade e desclassificação.

Seguidor de Linha

criar futuramente a entidade TentativaSeguidorLinha;

vincular cada tentativa a uma Registration;

registrar número da tomada e número da tentativa;

registrar se a tentativa foi concluída;

registrar o tempo obtido;

prever status como válida, anulada, cancelada ou não concluída;

registrar checkpoints alcançados e penalidades quando a regra for definida;

calcular o melhor tempo sob demanda;

selecionar a menor tentativa válida de cada tomada;

selecionar o menor tempo entre as tomadas válidas;

usar o resultado calculado para classificação e ranking;

desclassificar ou invalidar resultados que excedam as regras da categoria.

Chaveamento e partidas

impedir participação de inscrições inelegíveis;

gerar chaveamento;

registrar resultados;

avançar vencedores;

emitir atualizações para a vitrine;

integrar fluxo BPMN do Camunda.

8. Histórico resumido

2026-07-28 — Documento inicial e planejamento do backend.

2026-07-29 — Projeto renomeado para Rascomp e package root ajustado.

2026-07-29 — Estrutura inicial de pacotes e códigos de referência criada.

2026-07-29 — Categoria redesenhada para separar modalidade e configurações específicas.

2026-08-03T22:45:54-03:00 — CRUD CompetitionCategory marcado como implementado, com campo ativo mantido. Próximas etapas definidas: testes com H2, ConfigSumo e ConfigFollow.

2026-08-04T19:00:48-03:00 — Planejamento atualizado: incluída a entidade futura TentativaSeguidorLinha, vinculada a Registration, e o serviço de apuração do melhor tempo e ranking do Seguidor de Linha.