# RasComp — Contrato de Regras Competitivas — Ponteiro do Backend

Última revisão: **08/09/2026**

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
- matriz de alterações necessárias;
- cenários de testes automatizados de fluxo.

Checkpoint atual da ETAPA 1:

```text
Bloco 1 — Competition + Registration  ✅
Bloco 2 — Follow Line                  ✅
Bloco 3 — Sumô                         ✅
Bloco 4 — Chaves                       ⏭️ próximo / não iniciado
Bloco 5 — Fluxos integrados            ⏳
```

O Bloco 3 está consolidado no contrato com Flyway V11, inspeção humana `APTO/INAPTO`, modo `AUTONOMO | RC`, rounds extras justificados e decisão de juiz auditável.

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
