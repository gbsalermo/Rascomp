# Pós-Swagger — Extensibilidade de Modalidades e Categorias

Status: **PLANEJADO / NÃO IMPLEMENTAR ANTES DO FECHAMENTO DO SWAGGER**

Este documento registra uma evolução arquitetural do domínio competitivo do RASCOMP para ser executada somente depois da etapa Swagger/OpenAPI e da revalidação do contrato atual.

---

## 1. Motivação

O domínio atual atende ao RRC existente com duas modalidades-base:

```text
SUMO
FOLLOW_LINE
```

Hoje `CompetitionCategory` possui `modalidade: Modalidade` e configurações especializadas por `ConfigSumo` ou `ConfigFollow`.

Isso atende o MVP, porém o projeto deve suportar uma utilização mais abrangente no futuro.

Necessidades já conhecidas:

```text
SUMO
├── RC
└── AUTÔNOMO

FOLLOW_LINE
├── tradicional/atual
└── CORES              // futuro

COMBATE                 // futuro
RESGATE                 // futuro
```

As diferenças entre `SUMO RC` e `SUMO AUTÔNOMO` tendem a compartilhar grande parte do fluxo competitivo (inspeção, chave, partidas, rounds e progressão), mas podem possuir pequenas regras/configurações diferentes.

---

## 2. Decisão atual

**Não adicionar `SUMO_RC` e `SUMO_AUTONOMO` diretamente ao enum `Modalidade` como se fossem modalidades independentes.**

Isso perderia a relação conceitual de que ambas pertencem à família Sumô e aumentaria a duplicação de regras.

A etapa pós-Swagger deverá separar claramente os conceitos:

```text
MODALIDADE / FAMÍLIA DE REGRA
        ↓
CATEGORIA / VARIANTE COMPETITIVA
        ↓
CONFIGURAÇÃO ESPECÍFICA
```

Exemplo conceitual:

```text
Modalidade: SUMO
Variante: RC
Categoria: Sumô 3 kg RC
Configuração: regras/peso/rounds/etc.

Modalidade: SUMO
Variante: AUTONOMO
Categoria: Sumô 3 kg Autônomo
Configuração: regras/peso/rounds/etc.
```

Os nomes finais das entidades/enums **não estão congelados** neste momento.

---

## 3. O que deve ser estudado após o Swagger

Antes de alterar código ou banco, revisar:

- `Modalidade`;
- `CompetitionCategory`;
- `CompetitionCategoryDTO`;
- `ConfigSumo`;
- `ConfigFollow`;
- services que hoje fazem `modalidade == SUMO` ou `modalidade == FOLLOW_LINE`;
- geração/progressão de bracket;
- inspeções;
- ranking Follow;
- endpoints públicos;
- endpoints do participante;
- inicializadores/testes;
- documentação OpenAPI já finalizada.

Objetivo: identificar todos os pontos acoplados às duas modalidades atuais antes de generalizar.

---

## 4. Direção de modelagem a avaliar

Uma direção provável é manter uma **família de regra** estável e permitir uma variante/subcategoria.

Exemplo conceitual, não definitivo:

```text
CompetitionCategory
- id
- nome
- descricao
- modalidade
- variante
- ativo
```

Possíveis famílias:

```text
SUMO
FOLLOW_LINE
COMBATE
RESGATE
```

Possíveis variantes:

```text
SUMO
├── RC
└── AUTONOMO

FOLLOW_LINE
├── PADRAO
└── CORES
```

Porém a etapa pós-Swagger deverá decidir se `variante` deve ser:

- enum;
- entidade configurável;
- campo simples controlado;
- hierarquia de categorias (`parentCategory`);
- ou outro modelo que evite excesso de hardcode.

Não escolher antecipadamente sem revisar os requisitos reais de cada modalidade futura.

---

## 5. Princípio para motores de regra

O sistema não deve virar uma sequência crescente de `if (modalidade == ...)` espalhados pelos services.

Após o Swagger, avaliar uma estratégia de domínio em que cada família competitiva tenha regras especializadas, por exemplo conceitualmente:

```text
CompetitionRule / CompetitionEngine
├── SumoRuleEngine
├── FollowLineRuleEngine
├── CombatRuleEngine       // futuro
└── RescueRuleEngine       // futuro
```

Não é obrigação usar exatamente Strategy/Factory; o objetivo é apenas manter as regras localizadas e extensíveis.

Para Sumô, `RC` e `AUTONOMO` devem reaproveitar o máximo possível do mesmo motor de chaveamento/partidas/rounds, especializando somente as diferenças reais.

---

## 6. Regras que NÃO devem ser duplicadas sem necessidade

A introdução de subcategorias não deve criar versões paralelas de:

- bracket;
- match;
- progressão;
- BYE;
- resultado automático;
- autenticação;
- ownership;
- inscrição;
- projeção pública;

quando o comportamento for o mesmo.

Especializar apenas o que realmente variar, como por exemplo:

- critérios de inspeção;
- parâmetros técnicos;
- restrições específicas;
- formato de prova;
- limites/configurações;
- critérios de pontuação/ranking quando aplicável.

---

## 7. Migração de banco

As migrations existentes permanecem imutáveis.

Qualquer alteração estrutural decorrente dessa evolução deverá entrar em uma migration nova (`V6+`, conforme a numeração disponível no momento da implementação).

A migração deve preservar dados existentes de:

```text
SUMO atual
FOLLOW_LINE atual
```

sem exigir recriação manual das competições já cadastradas.

---

## 8. Compatibilidade de API

Como essa etapa ocorrerá **depois do Swagger**, toda mudança no contrato deverá ser deliberada.

Antes de alterar payloads:

- comparar o OpenAPI congelado;
- preservar campos existentes quando possível;
- avaliar compatibilidade retroativa;
- versionar/migrar somente quando necessário;
- atualizar exemplos e schemas;
- revisar Frontend de Gestão e Landing.

Não generalizar o modelo apenas internamente e deixar Swagger/DTOs inconsistentes.

---

## 9. Impacto esperado no Frontend de Gestão

A Gestão não deve ficar permanentemente codificada para apenas dois itens fixos.

Depois da generalização do backend, revisar a UI para que:

```text
Competição
  ↓
Categorias habilitadas
  ↓
modalidade/família
  ↓
variante/subcategoria
  ↓
centro operacional compatível
```

Exemplos:

```text
Sumô RC       -> fluxo Sumô
Sumô Autônomo -> fluxo Sumô com diferenças específicas
Follow Line   -> fluxo ranking atual
Follow Cores  -> fluxo futuro específico/reaproveitado
Combate       -> fluxo futuro
Resgate       -> fluxo futuro
```

Não implementar telas vazias para modalidades futuras antes de existirem regras e endpoints reais.

---

## 10. Impacto esperado na Landing

A Landing deve receber categorias/modalidades pela API pública e apresentá-las dinamicamente.

Evitar no futuro:

```text
if modalidade == SUMO
else FOLLOW_LINE
```

como única arquitetura pública.

A página do RRC deverá conseguir apresentar novas categorias sem precisar redesenhar todo o site, embora experiências específicas (bracket, ranking, prova por missão etc.) possam ter componentes próprios.

---

## 11. Etapa oficial pós-Swagger

Quando o Swagger for concluído, executar uma etapa específica:

### BACKEND PÓS-SWAGGER — Generalização de modalidades/categorias

Checklist inicial:

- [ ] mapear regras reais de Sumô RC;
- [ ] mapear regras reais de Sumô Autônomo;
- [ ] registrar diferenças e regras compartilhadas;
- [ ] revisar `Modalidade` e `CompetitionCategory`;
- [ ] definir modelo de variante/subcategoria;
- [ ] decidir mecanismo de configuração por família;
- [ ] reduzir acoplamento de services aos enums atuais;
- [ ] garantir reutilização do bracket Sumô;
- [ ] preparar extensão futura para Combate;
- [ ] preparar extensão futura para Follow Line de cores;
- [ ] preparar extensão futura para Resgate;
- [ ] criar migration nova;
- [ ] atualizar DTOs/endpoints somente se necessário;
- [ ] atualizar Swagger/OpenAPI após a alteração;
- [ ] adicionar testes de regressão SUMO/FOLLOW_LINE atuais;
- [ ] adicionar testes RC/AUTÔNOMO;
- [ ] executar smoke completo do backend;
- [ ] revisar impacto no frontend.

### Critério de conclusão

O backend deve continuar atendendo exatamente o RRC atual e, ao mesmo tempo, permitir adicionar uma nova variante/modalidade sem duplicar o núcleo competitivo inteiro.

---

## 12. Ordem de trabalho

```text
Swagger/OpenAPI atual
        ↓
revalidação do contrato atual
        ↓
BACKEND PÓS-SWAGGER
Generalização modalidades/categorias
        ↓
regressão completa
        ↓
congelamento final do backend
        ↓
Gestão consolidada
        ↓
Landing
```

Este documento é um **checkpoint de arquitetura**, não autorização para implementar essas mudanças antes do fechamento do Swagger.
