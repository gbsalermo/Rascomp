# Congelamento da API — Rascomp

Data do marco: 23/08/2026

## Objetivo

Este documento registra o ponto em que o backend foi considerado funcionalmente validado e passou a ser tratado como **contrato estável** para Swagger/OpenAPI e para os futuros clientes da API.

Não significa que o projeto nunca mais muda. Significa que qualquer mudança de contrato daqui em diante precisa ter motivo objetivo e impacto conhecido.

---

## Estado validado

```text
MySQL/Flyway                     ✅
JPA/Hibernate                    ✅
Camunda infraestrutura           ✅
CRUDs essenciais                 ✅
FOLLOW_LINE                      ✅
SUMO                             ✅
tratamento HTTP                  ✅
testes automatizados             ✅
GitHub Actions                   ✅
testes manuais                   ✅
```

A branch `testes-automatizados` foi mergeada na `main` pelo PR #2 após a suíte automatizada ficar verde e após as baterias manuais de Sumô e Seguidor de Linha passarem.

---

## Contratos que ficam congelados

### Rotas

Preservar os caminhos atuais dos controllers e seus métodos HTTP.

### DTOs

Evitar remoção/renomeação de campos já utilizados pela API. Campos novos só quando forem necessários e preferencialmente de forma compatível.

### Enums

Não alterar significado de valores existentes sem uma decisão explícita de migração.

### Regras de modalidade

#### FOLLOW_LINE

```text
ConfigFollow
 -> TentativaSeguidorLinha
 -> melhor tentativa válida/concluída
 -> RankingFollowService
```

Não criar `Bracket`, `Match`, `RoundSumo` ou `MatchResult` para Follow.

#### SUMO

```text
InspecaoSumo
 -> aptidão
 -> Bracket
 -> Match
 -> RoundSumo
 -> MatchResult automático
```

`MatchResult` continua somente leitura na API externa.

---

## Mudanças permitidas durante Swagger

A etapa Swagger pode adicionar:

- configuração OpenAPI;
- `@Tag`;
- `@Operation`;
- `@ApiResponse`;
- `@Parameter`;
- `@Schema`;
- exemplos de payload;
- descrições de enums;
- descrições de erros;
- configuração de Swagger UI;
- testes/documentação relacionados à exposição OpenAPI.

A etapa Swagger **não deve**, por padrão:

- mudar regra de negócio;
- alterar persistência;
- criar migration;
- renomear endpoint;
- mudar status HTTP já validado;
- alterar comportamento de Follow/Sumô;
- introduzir autenticação JWT;
- implementar BPMN Rascomp.

Se a documentação revelar inconsistência real do contrato, a alteração deve ser registrada como exceção ao congelamento e acompanhada de teste de regressão.

---

## Política de migrations após congelamento

Nunca editar migrations já aplicadas:

```text
V1
V2
V3
V4
```

Qualquer mudança estrutural futura deve entrar como nova migration incremental (`V5`, `V6`, ...).

---

## Critério de aceite do Swagger

Swagger só é considerado concluído quando:

1. `/v3/api-docs` responder corretamente;
2. `/swagger-ui/index.html` abrir;
3. todos os controllers de negócio estiverem organizados por tags;
4. operações possuírem resumo e descrição úteis;
5. parâmetros de path/query estiverem claros;
6. request bodies estiverem documentados;
7. respostas de sucesso estiverem documentadas;
8. erros relevantes `400`, `404`, `405` e `409` estiverem documentados onde aplicável;
9. DTOs estiverem legíveis na seção de schemas;
10. exemplos JSON úteis estiverem presentes nos fluxos principais;
11. FOLLOW_LINE e SUMO aparecerem como fluxos diferentes;
12. `MatchResult` estiver explícito como somente leitura;
13. uma conferência visual endpoint a endpoint não revelar rota faltante ou descrição enganosa;
14. nenhuma alteração de comportamento tiver sido introduzida sem justificativa e regressão.

---

## Camunda no ponto de congelamento

Camunda está validado como infraestrutura:

```text
Engine          ✅
JobExecutor     ✅
ACT_*           ✅
REST starter    ✅
BPMN Rascomp    ⏳
```

Nenhuma regra central de competição será migrada para BPMN durante Swagger.

Após concluir Swagger haverá um checkpoint arquitetural para escolher o momento de implementar Camunda funcional.

Opções que deverão ser avaliadas depois:

```text
A. BPMN mínimo de aprovação de inscrição antes do frontend completo
B. primeiro fluxo do Frontend de Gestão e Camunda integrado logo depois
C. Camunda funcional adiado para depois do MVP visual
```

O critério principal será prazo x valor entregue x risco de integração.

---

## Forma de execução a partir daqui

O projeto passa a ser conduzido por orquestração:

```text
planejar
 -> delegar implementação
 -> revisar diff/contrato
 -> executar teste objetivo
 -> atualizar continuidade
 -> avançar
```

A documentação deve ser suficiente para que outra IA/agente implemente uma etapa sem depender de memória informal da conversa.

Arquivos principais:

```text
docs/CONTINUIDADE.md
docs/CONGELAMENTO_API.md
docs/ENDPOINTS_INTERNOS.md
docs/JSON_EXEMPLOS.md
docs/FLUXO_DO_SISTEMA.md
docs/ENTIDADES_E_CRUDS.md
docs/TESTES_POSTMAN.md
```

---

## Próxima etapa autorizada

```text
Swagger / OpenAPI
```

O contrato funcional permanece congelado durante essa etapa.
