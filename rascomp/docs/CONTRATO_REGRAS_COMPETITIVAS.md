# RasComp — Contrato de Regras Competitivas — Ponteiro do Backend

Última revisão: **06/09/2026**

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
- inspeção de Sumô;
- Mini Sumô Auto/R/C;
- Sumô 3 kg Auto/R/C;
- rounds, penalidades, WO e BYE;
- rounds extras justificados;
- decisão e identificação de juízes;
- geração/regeneração de chave;
- agenda de partidas separada da estrutura lógica;
- correção de resultado após progressão;
- matriz de alterações necessárias;
- cenários de testes automatizados de fluxo.

O backend deve usar esse contrato antes de alterar regra competitiva.

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
