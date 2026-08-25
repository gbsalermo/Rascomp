# Pós-Swagger — Usuários, adesão a equipes e inscrição competitiva

Status: **PLANEJADO / REVISAR APÓS O SWAGGER E ANTES DO CONGELAMENTO FINAL DA API**

Este documento registra a evolução desejada para o primeiro acesso e para o vínculo entre usuários, equipes, robôs e inscrições.

---

## 1. Princípio principal

A conta do sistema deve ser independente da equipe e da competição.

```text
UserAccount
    ↓
pode existir sem equipe
    ↓
pode criar ou solicitar entrada em equipe
    ↓
pode participar de uma inscrição competitiva
```

O cadastro público inicial cria apenas:

```text
UserAccount(PARTICIPANTE)
```

Dados de equipe, robô e competição não fazem parte do registro de usuário.

---

## 2. Estado atual que já atende parte do objetivo

O backend atual já possui:

- `UserAccount` independente;
- `Team.responsibleUser`;
- criação de equipe pelo usuário autenticado, tornando-o responsável;
- `Robot` pertencente à equipe;
- `Registration` com um `robot` obrigatório;
- `Registration.competitors` permitindo múltiplos competidores na mesma inscrição/robô.

Fluxo atual relevante:

```text
UserAccount
    ↓ cria
Team(responsibleUser)
    ↓
Robot
    ↓
Registration
    ├── robot
    └── competitors[]
```

A lacuna atual é que os demais integrantes da equipe ainda são tratados principalmente como registros de `Competitor`, e não como usuários independentes vinculados à equipe.

---

## 3. Modelo alvo conceitual

Separar claramente:

```text
UserAccount   = identidade, login e perfil pessoal
Team          = equipe
TeamMember    = vínculo de um usuário com uma equipe
Robot         = robô da equipe
Registration  = inscrição competitiva de um robô
```

Modelo conceitual:

```text
UserAccount
    │
    ├── TeamMember ──> Team
    │                    │
    │                    └── Robot(s)
    │
    └── pode ser selecionado em uma Registration
```

### TeamMember

Entidade/associação a estudar:

```text
TeamMember
- id
- team
- userAccount
- roleNaEquipe
- ativo
- dataEntrada
```

Papéis mínimos:

```text
LEADER
MEMBER
```

Regra inicial:

```text
usuário cria equipe
    ↓
Team.responsibleUser = usuário
    ↓
TeamMember(usuario, equipe, LEADER)
```

O criador é o líder/responsável da equipe.

Os nomes finais das classes/enums não estão congelados.

---

## 4. Entrada em equipe — solicitação aprovada pelo líder

O fluxo preferencial deixa de ser anexar manualmente outro usuário.

No primeiro acesso o portal deve oferecer:

```text
Você já tem equipe?
├── NÃO → Criar equipe
└── SIM → Buscar equipe
           ↓
        selecionar
           ↓
     solicitar entrada
           ↓
      líder recebe
       ↙       ↘
   APROVAR    REJEITAR
      ↓
 TeamMember(MEMBER)
```

### Busca

O usuário informa parte do nome e escolhe entre equipes ativas.

A busca deve expor apenas dados suficientes para identificação, por exemplo:

```text
nome da equipe
instituição
sigla da instituição
```

Não expor dados privados do líder ou dos membros.

### TeamJoinRequest

Entidade/associação conceitual a estudar:

```text
TeamJoinRequest
- id
- team
- requestedByUser
- status
- requestedAt
- decidedByUser
- decidedAt
- observacao opcional
```

Status mínimos:

```text
PENDENTE
APROVADA
REJEITADA
CANCELADA
```

Regras mínimas:

- somente usuário autenticado pode solicitar para si;
- somente líder/responsável da equipe pode aprovar/rejeitar;
- não criar duas solicitações pendentes para `user + team`;
- não aprovar usuário já membro;
- aprovação cria `TeamMember(MEMBER)` de forma transacional;
- rejeição não cria vínculo;
- usuário pode cancelar uma solicitação ainda pendente;
- equipe inativa não recebe novas solicitações;
- auditar quem decidiu e quando.

A regra sobre participar simultaneamente de múltiplas equipes deve ser decidida na revisão pós-Swagger, e não presumida agora.

---

## 5. Usuário de equipe x competidor de robô

Ser membro de uma equipe não significa automaticamente competir.

A participação efetiva acontece na inscrição de um robô:

```text
Team
  ↓
Registration
  ├── Competition
  ├── Category
  ├── Robot
  └── participantes daquele robô
```

Isso permite:

- líder administrar equipe sem competir;
- membro participar da equipe sem estar em todo robô;
- mesmo usuário assumir papéis diferentes em inscrições diferentes;
- um robô possuir duas, três ou mais pessoas responsáveis pelo seu desenvolvimento/operação.

---

## 6. Competidor responsável e competidor suporte

A distinção deve pertencer à **inscrição do robô**, e não ao `UserAccount` permanentemente.

Papéis conceituais:

```text
RESPONSAVEL
SUPORTE
```

Regra recomendada por inscrição:

```text
Registration
├── Robot obrigatório
├── 1 RESPONSAVEL obrigatório
└── 0..N SUPORTE
```

Exemplo:

```text
Robot: Vespa
Categoria: Sumô RC

RESPONSAVEL
└── Gabriel

SUPORTE
├── João
└── Maria
```

A mesma pessoa pode ser:

```text
RESPONSAVEL no Robot A
SUPORTE     no Robot B
```

se as regras da competição permitirem.

### Impacto no modelo atual

Hoje `Registration.competitors` é um `ManyToMany` simples e não consegue armazenar papel por participante.

Na revisão pós-Swagger, avaliar substituir a tabela de junção simples por uma entidade associativa, por exemplo:

```text
RegistrationParticipant
- id
- registration
- teamMember / competitor
- role
- dataVinculo
```

ou equivalente.

Regras mínimas:

- exatamente um `RESPONSAVEL` por inscrição;
- suporte é opcional e múltiplo;
- todos devem pertencer à equipe da inscrição;
- não duplicar a mesma pessoa na mesma inscrição;
- robô deve pertencer à mesma equipe;
- backend valida tudo; frontend apenas coleta as escolhas.

O nome final (`RegistrationParticipant`, `RegistrationCompetitor`, etc.) não está congelado.

---

## 7. Robô no fluxo de inscrição

A experiência desejada no frontend continua:

```text
Nova inscrição
    ↓
selecionar competição
    ↓
selecionar categoria
    ↓
selecionar robô existente OU cadastrar robô
    ↓
selecionar RESPONSAVEL
    ↓
selecionar 0..N SUPORTE
    ↓
confirmar inscrição
```

Embora `Robot` continue sendo entidade persistente da equipe, o usuário não precisa obrigatoriamente passar por uma tela isolada de cadastro antes de se inscrever.

O cadastro pode ocorrer dentro do wizard e, após criado, o backend usa seu `robotId` na `Registration`.

---

## 8. Regra "não existe competidor sem robô"

Interpretar no domínio competitivo como:

> ninguém é considerado competidor de uma inscrição sem estar associado a um robô naquela inscrição.

Não significa que uma conta ou membro precise ter robô permanente atribuído.

Associação correta:

```text
Registration
   ├── Robot obrigatório
   ├── RESPONSAVEL obrigatório
   └── SUPORTE opcional
```

A inscrição atual já exige `robotId` e ao menos um `competitorId`; a evolução deve preservar ou fortalecer essa regra.

---

## 9. Relação com a entidade Competitor atual

A etapa pós-Swagger deve decidir explicitamente se `Competitor`:

1. continua existindo como perfil competitivo vinculado 1:1 a `UserAccount`; ou
2. é absorvido pelo modelo `UserAccount + TeamMember`, deixando a inscrição apontar para membros da equipe.

Não remover `Competitor` sem análise de impacto.

Revisar antes da decisão:

- migrations;
- ownership;
- DTOs públicos;
- `Registration.competitors`;
- testes Follow Line;
- testes Sumô;
- API participante;
- API administrativa;
- API pública;
- histórico de inscrições.

---

## 10. Primeiro acesso alvo

```text
Tela de Login
├── Entrar
├── Criar conta
└── Recuperar senha

Criar conta
    ↓
UserAccount PARTICIPANTE
    ↓
Minha equipe
    ↓
Você já tem equipe?
    ├── NÃO
    │    ↓
    │  Criar equipe
    │    ↓
    │  usuário = LEADER
    │
    └── SIM
         ↓
       Buscar equipe
         ↓
       Solicitar entrada
         ↓
       líder aprova/rejeita
         ↓
       MEMBER

Equipe pronta
    ↓
Nova inscrição
    ├── competição/categoria
    ├── robô
    ├── 1 responsável
    └── 0..N suportes
```

---

## 11. Endpoints conceituais a avaliar

Não são contratos congelados; servem para orientar a etapa pós-Swagger.

```text
GET  /api/v1/participante/equipes/busca?nome=
POST /api/v1/participante/equipes/{teamId}/solicitacoes
GET  /api/v1/participante/solicitacoes-equipe
DELETE /api/v1/participante/solicitacoes-equipe/{id}

GET  /api/v1/participante/equipes/{teamId}/solicitacoes
PATCH /api/v1/participante/equipes/{teamId}/solicitacoes/{id}/aprovar
PATCH /api/v1/participante/equipes/{teamId}/solicitacoes/{id}/rejeitar
```

A busca pode reutilizar/projetar a API pública sanitizada, mas a criação e decisão da solicitação exigem autenticação e ownership.

---

## 12. Ordem de implementação

Após Swagger/OpenAPI:

1. revisar contrato atual;
2. decidir `TeamMember` e papel final de `Competitor`;
3. implementar solicitação de adesão à equipe;
4. definir cardinalidade usuário ↔ equipe;
5. modelar papel `RESPONSAVEL/SUPORTE` por inscrição;
6. criar migration `V6+` ou posterior;
7. adaptar services, ownership e auditoria;
8. adaptar DTOs/endpoints;
9. testes automatizados;
10. revalidar Follow Line e Sumô;
11. atualizar Swagger;
12. somente então considerar API final congelada.

---

## 13. Fora de escopo antes dessa revisão

Não implementar no backend antes do pós-Swagger:

- solicitação definitiva de adesão;
- recuperação de senha completa;
- remoção imediata de `Competitor`;
- vínculo permanente `Robot ↔ UserAccount` fora da inscrição;
- subclasses separadas `CompetidorResponsavel` / `CompetidorSuporte`.

A prioridade atual continua sendo concluir Swagger e manter o núcleo competitivo estável.
