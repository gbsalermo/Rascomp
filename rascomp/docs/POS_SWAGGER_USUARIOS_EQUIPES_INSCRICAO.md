# Pós-Swagger — Usuários, membros de equipe e inscrição competitiva

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
pode criar ou ingressar em equipe
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

A lacuna atual é que os demais integrantes da equipe ainda são tratados principalmente como registros de `Competitor`, e não como usuários independentes anexados à equipe.

---

## 3. Modelo alvo conceitual

Separar claramente:

```text
UserAccount   = identidade, login e perfil pessoal
Team          = equipe
TeamMember    = vínculo de um usuário com uma equipe
Robot         = robô da equipe
Registration  = inscrição competitiva
```

Modelo conceitual:

```text
UserAccount
    │
    ├── TeamMember ──> Team
    │                    │
    │                    └── Robot(s)
    │
    └── selecionado na Registration como participante
```

### TeamMember

Entidade/associação a estudar:

```text
TeamMember
- id
- team
- userAccount
- roleNaEquipe
- status
- dataEntrada
```

Papéis mínimos possíveis:

```text
LEADER
MEMBER
```

Os nomes finais não estão congelados.

Regra inicial:

```text
usuário cria equipe
    ↓
Team.responsibleUser = usuário
    ↓
TeamMember(usuario, equipe, LEADER)
```

O criador é o líder/responsável da equipe.

---

## 4. Entrada de outros usuários na equipe

Não duplicar dados pessoais manualmente quando o integrante já possui conta.

Fluxo desejado:

```text
Usuário A cria equipe
      ↓
Usuário A = LEADER
      ↓
convida/anexa Usuário B
      ↓
Usuário B aceita ou é vinculado conforme regra definida
      ↓
Usuário B = MEMBER da mesma equipe
```

Mecanismos possíveis a avaliar:

- convite por e-mail;
- código de equipe;
- link/token de convite;
- busca de usuário por e-mail + confirmação.

Não escolher mecanismo definitivo antes da revisão pós-Swagger.

Requisitos obrigatórios:

- não permitir anexar conta inexistente sem fluxo claro;
- não expor busca pública irrestrita de usuários;
- evitar duplicidade `userAccount + team`;
- preservar ownership/autorização;
- definir se um usuário pode participar de mais de uma equipe simultaneamente.

---

## 5. Quem é competidor

A conta e a associação à equipe não devem, por si só, significar participação competitiva.

A participação efetiva acontece na inscrição:

```text
Team
  ↓
Registration
  ├── Competition
  ├── Category
  ├── Robot
  └── participantes selecionados da equipe
```

Isso permite:

- líder que administra a equipe sem necessariamente competir;
- usuário membro da equipe que não participa de determinada categoria;
- mesmo usuário competir com robôs/categorias diferentes quando permitido;
- um mesmo robô possuir mais de um competidor associado na mesma inscrição.

---

## 6. Robô no fluxo de inscrição

Experiência desejada no frontend:

```text
Nova inscrição
    ↓
selecionar competição
    ↓
selecionar categoria
    ↓
selecionar robô existente OU cadastrar robô
    ↓
selecionar membros/competidores da equipe
    ↓
confirmar inscrição
```

Embora `Robot` continue sendo uma entidade persistente da equipe, o usuário não precisa obrigatoriamente passar por uma tela isolada de cadastro de robô antes de se inscrever.

O cadastro de robô pode ocorrer dentro do wizard de inscrição e, após criado, o backend usa seu `robotId` na `Registration`.

Isso mantém o contrato de domínio normalizado sem prejudicar a experiência de primeiro acesso.

---

## 7. Regra "não existe competidor sem robô"

Interpretar no domínio competitivo como:

> ninguém é considerado competidor de uma inscrição sem estar associado a um robô naquela inscrição.

Não significa que uma conta de usuário ou um membro de equipe precise ter um robô permanente atribuído.

Associação correta:

```text
Registration
   ├── Robot obrigatório
   └── participantes/competidores obrigatórios
```

A inscrição atual já exige `robotId` e ao menos um `competitorId`; a evolução deve preservar essa regra ao trocar a origem dos participantes para usuários/membros da equipe, caso essa refatoração seja aprovada.

---

## 8. Relação com a entidade Competitor atual

A etapa pós-Swagger deve decidir explicitamente se `Competitor`:

1. continua existindo como perfil competitivo vinculado 1:1 a `UserAccount`; ou
2. é absorvido pelo modelo `UserAccount + TeamMember`, deixando a inscrição apontar diretamente para membros/usuários.

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

## 9. Primeiro acesso alvo

```text
Tela de Login
├── Entrar
├── Criar conta
└── Recuperar senha

Criar conta
    ↓
UserAccount PARTICIPANTE
    ↓
Dashboard do participante
    ↓
sem equipe?
    ├── Criar equipe
    └── Entrar/aceitar convite de equipe
           ↓
Equipe
    ↓
Nova inscrição
    ├── categoria
    ├── robô
    └── membros que competirão
```

---

## 10. Ordem de implementação

Após Swagger/OpenAPI:

1. revisar contrato atual;
2. decidir `TeamMember` e papel de `Competitor`;
3. definir regra de convite/entrada em equipe;
4. definir cardinalidade usuário ↔ equipe;
5. criar migration `V6+` ou posterior, se necessária;
6. adaptar services e ownership;
7. adaptar DTOs/endpoints;
8. testes automatizados;
9. revalidar Follow Line e Sumô;
10. atualizar Swagger;
11. somente então considerar API final congelada.

---

## 11. Fora de escopo neste momento

Não implementar antes dessa revisão:

- convite definitivo por e-mail;
- recuperação de senha completa;
- múltiplos papéis complexos de equipe;
- remoção imediata de `Competitor`;
- vínculo permanente `Robot ↔ UserAccount` fora da inscrição.

A prioridade atual continua sendo concluir Swagger e manter o núcleo competitivo estável.
