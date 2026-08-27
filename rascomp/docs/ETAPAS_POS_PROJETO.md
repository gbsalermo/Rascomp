# RasComp — Etapas Pós-Projeto

O roteiro pós-aprovação é mantido como documento canônico no repositório de frontend, pois ele coordena backend, gestão, participante, Landing, mídia, comunicação, uso institucional e deploy em um único plano.

Documento principal:

```text
gbsalermo/Rascomp-FRONT
docs/ETAPAS_POS_PROJETO.md
```

Este backend deve seguir especialmente:

```text
ETAPA 0  baseline da versão aprovada
ETAPA 1  correções de lógica
ETAPA 2  limpeza técnica
ETAPA 3  nova matriz DEV/GESTAO/MIDIA/PARTICIPANTE
ETAPA 4  Avisos — consultar Dossiê Mestre para detalhes
ETAPA 5  Ajustes Gerais + auditoria
ETAPA 6  portabilidade institucional
ETAPA 7  backend do CMS/mídia
ETAPA 8  regras públicas quando persistidas
ETAPA 9  Futebol de Robôs
ETAPA 10 suporte ao participante completo
ETAPA 12 hardening para uso externo
ETAPA 13 bateria manual completa
ETAPA 14 deploy em nuvem / Cloudflare
```

Guia detalhado de deploy:

```text
gbsalermo/Rascomp-FRONT
docs/DEPLOY_CLOUDFLARE.md
```

Ao concluir uma etapa que afete o backend, atualizar também `rascomp/docs/CONTINUIDADE.md` e os testes correspondentes.

O modo local atual deve continuar funcional mesmo após existir a implantação cloud.
