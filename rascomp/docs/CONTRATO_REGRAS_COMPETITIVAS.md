# RasComp — Contrato de Regras Competitivas — Ponteiro do Backend

Última revisão: **06/10/2026**

O contrato competitivo canônico do RasComp é cross-repo e está mantido em:

```text
gbsalermo/Rascomp-FRONT
docs/CONTRATO_REGRAS_COMPETITIVAS.md
```

Esse documento consolida as regras aprovadas durante a ETAPA 1 para:

- Competition e transições de estado;
- prorrogação/reabertura de inscrições;
- Registration, cancelamento e desistência;
- pagamento futuro como invariante de aprovação;
- robôs híbridos e compatibilidade física Mini/3 kg;
- Follow Line;
- inspeção humana de Sumô;
- Mini Sumô Auto/R/C;
- Sumô 3 kg Auto/R/C;
- rounds, penalidades, WO e BYE;
- falha de inicialização;
- rounds extras justificados;
- decisão e identificação de juízes;
- geração/regeneração de chave;
- agenda de partidas separada da estrutura lógica;
- correção de resultado após progressão;
- matriz final de implementação da ETAPA 1;
- cenários de testes automatizados de fluxo.

Checkpoint final da ETAPA 1:

```text
Bloco 1 — Competition + Registration  ✅
Bloco 2 — Follow Line                  ✅
Bloco 3 — Sumô                         ✅
Bloco 4 — Chaves                       ✅
Bloco 5 — Fluxos integrados            ✅
ETAPA 1                                ✅ concluída / validada
```

O contrato canônico está consolidado até o Bloco 5. Checkpoint final da ETAPA 1: **111 testes**, H2 flowtest integrado e MySQL + Flyway V12 + `testdata` verdes.

O backend deve usar o contrato canônico antes de alterar regra competitiva.

Regra de manutenção:

```text
regra competitiva alterada
→ contrato canônico
→ backend
→ testes
→ frontend operacional
→ futuro regulamento público
```

Não duplicar integralmente o contrato neste repositório para evitar divergência entre cópias.


## Evolução competitiva aprovada pós-Beta

O contrato canônico passou a registrar a evolução de Sumô/Mini Sumô para **dupla eliminação**, a ser implementada somente após deploy/estabilização da V1 Beta.

Resumo:

```text
WINNERS → 1ª derrota → LOSERS
LOSERS  → 2ª derrota → eliminado
vencedor WINNERS + vencedor LOSERS → GRAND_FINAL
invicto perde GRAND_FINAL → GRAND_FINAL_RESET obrigatória
```

Pódio aprovado no formato:

```text
1º = vencedor da Final Geral decisiva
2º = perdedor da Final Geral decisiva
3º = perdedor da Final da Losers
```

A eliminação simples atual continua sendo o comportamento implementado da Beta até essa camada ser desenvolvida.
