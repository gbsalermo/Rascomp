# RasComp — Incidentes, soluções e decisões após entrada em produção

> **Registro vivo iniciado em 18/10/2026.** Documento técnico-histórico do MVP V1 em uso real. Os relatos observados pelo responsável são distinguidos de diagnósticos confirmados. Não usar este arquivo como substituto de logs, métricas ou tickets.

## Objetivo e limites

Preservar **o que ocorreu depois da publicação**, quais sintomas atingiram o uso real, as correções e decisões tomadas e o que isso ensinou sobre operar um sistema para uma competição em infraestrutura de baixo custo. O documento servirá de fonte para o futuro dossiê técnico/portfólio, **mas o levantamento quantitativo de usuários, latência, simultaneidade, recursos e responsividade será feito mais perto da competição**, quando houver medições verificáveis.

**Premissa:** o MVP V1 está operacional, com escopo funcional congelado para concentrar esforços em correções, estabilidade e contingência. Não atribuir a um único recurso todos os problemas de performance sem evidências de observabilidade.

## 1. Ponto de virada: galeria, desempenho e congelamento do escopo

**Período:** outubro/2026; experiência relatada pelo responsável após disponibilizar o sistema.

**Contexto:** o produto incluía uma ambição mais ampla de galeria e gestão de mídias. Durante o uso em produção, a comparação percebida entre a experiência **com a galeria e com os elementos/rotas da galeria suspensos** chamou atenção para o custo operacional de funcionalidades adicionais no ambiente disponível.

**Sintomas/impacto percebidos:** degradação de desempenho e maior preocupação com capacidade e estabilidade; o episódio levou à avaliação de que o sistema deveria priorizar o funcionamento do fluxo competitivo sobre a ampliação de recursos editoriais.

**Medidas/decisões verificáveis no estado documental atual:**
- Foi mantida apenas a prévia institucional da galeria na Landing; acessos à galeria completa e a entradas de biblioteca/álbuns da Gestão foram ocultados/suspensos.
- A galeria independente de fotos e vídeos foi adiada para a V2, após a competição; a suspensão **não** significa apagar antecipadamente o acervo.
- O MVP V1 passou a ter escopo congelado, com prioridade para erros encontrados em produção e validação do uso local/contingência.

**Cautela técnica:** a melhora observada pelo responsável não é, por si só, prova de que o processamento da galeria era a única causa dos problemas nem mede seu peso relativo. É necessário distinguir peso do frontend, downloads/arquivos, consultas, backend, banco, cold starts e recursos compartilhados. **Não existem, neste registro, percentuais ou tempos antes/depois instrumentados.**

**Lição:** novas funcionalidades têm custos indiretos (consultas, memória, rede, manutenção e regressões). Em produção, especialmente perto do evento e em planos gratuitos, o controle do escopo é uma decisão de confiabilidade.

**Evidências futuras:** capturas de versões com/sem galeria, perfis de rede, timings de carregamento, uso de CPU/RAM e commits que suspenderam a navegação, se recuperáveis com segurança.

## 2. Consultas de inscritos: lentidão e falhas de carregamento

**Registro relatado:** 17/10/2026.

**Sintoma:** a Gestão não conseguia visualizar inscritos em algumas ocasiões; o carregamento da lista era lento. Após correção e publicação, o responsável confirmou que a listagem voltou a funcionar e recarregava consideravelmente mais rápido.

**Hipóteses levantadas no diagnóstico:** filtros que passaram a exigir mais dados/status, operações de consulta e associações volumosas; uma pessoa podia estar ligada a 5–10 robôs, aumentando a multiplicação de relações retornadas/processadas. O volume total aparentemente pequeno (ordem de dezenas de participantes segundo relato) não exclui custo elevado de consultas com joins e relacionamentos.

**Resultado confirmado pelo responsável:** a listagem voltou a funcionar e a recarga ficou mais rápida após o deploy. **Não registrar como confirmadas** uma causa específica, índice SQL, implementação interna ou ganho percentual sem revisar diff/SQL/planos de execução.

**Evidência de referência:** [commit backend 4ed539e](https://github.com/gbsalermo/Rascomp/commit/4ed539ef237fdbc3a198f791fb8330e8e4c00a74) foi mencionado no ciclo de correções, mas a vinculação exata entre cada alteração e o incidente precisa de conferência no diff.

**Lição:** mesmo bases pequenas podem sofrer com consultas multiplicativas, N+1, filtros e agregações mal dimensionados; medir chamadas por tela e cardinalidade real, não só contar usuários.

## 3. Dashboard, login e conexões intermitentes

**Registro:** 17/10/2026.

**Sintomas relatados:** dashboard às vezes demorava ou falhava ao carregar; o login também apresentava lentidão/intermitência; usuários relataram perda de conexão com o servidor.

**Estado de diagnóstico:** associação com pressão sobre backend/banco, concorrência, inicialização a frio ou instabilidade de hospedagem é **hipótese**, não conclusão. A correção da consulta de inscritos não comprova que todos os problemas de login/dashboard tenham a mesma causa.

**Situação:** acompanhamento e revisão de performance/observabilidade permanecem relevantes. Recolher de forma sanitizada códigos HTTP, tempos, logs de exceção, conexões MySQL e consumo de recursos nas janelas de erro.

**Lição:** separar incidentes correlacionados por horário dos que compartilham causa comprovada.

## 4. Recarregamento excessivo de páginas após ações administrativas

**Registro:** 16/10/2026.

**Sintoma:** comandos como “manter” ou “recusar” na Gestão provocavam recarregamento amplo da página e novas buscas, às vezes em múltiplas áreas de participante/organização. O responsável percebeu custo desnecessário e relatou que uma primeira otimização não trouxe melhora perceptível suficiente.

**Direção das correções:** reduzir atualização global desnecessária, preferindo mudanças localizadas e revalidação apenas dos dados afetados. Parte do trabalho ocorreu no frontend; **não afirmar otimização concluída em todas as telas** sem inspeção e reteste.

**Validação desejada:** comparar número de requests, volume transferido e latência total por ação antes/depois e verificar consistência das contagens/listas sem refresh integral.

**Lição:** performance percebida depende tanto do número de requisições geradas pela UI quanto da velocidade individual das APIs.

## 5. Tela branca e problemas de renderização inicial

**Registros:** 14–17/10/2026.

**Sintomas:** relatos de interface em branco em dispositivo móvel e renderização momentânea do rodapé antes da Intro/Landing; também houve falha de build TypeScript no `App.vue` reportada em 14/10/2026.

**Estado:** separar cada ocorrência, suas versões e correções; **não tratar todos como um único bug**. Verificar exceções JavaScript, ordem de carregamento, estado inicial, bundles, testes mobile e pipelines de build. Sem logs/diffs completos aqui, a causa e a resolução de cada manifestação não são declaradas confirmadas.

**Lição:** sucesso do deploy e backend acessível não garantem uma primeira renderização correta em todos os dispositivos.

## 6. Risco de capacidade nos serviços gratuitos e contingência

**Contexto:** outubro/2026, MVP publicado com frontend em Cloudflare, API Render, MySQL Aiven, arquivos R2 e envio de e-mails Resend.

**Ocorrências/limites relatados:** episódio de memória cheia/indisponibilidade no Render e necessidade de evitar gastos até a competição. Passou-se a priorizar contingência local via Docker, com exposição controlada de API e dependências preservadas.

**Decisões:** manter infra atual enquanto suficiente, evitar novas features, preparar execução local e testar troca operacional sem duas fontes concorrentes de escrita. Guardar variáveis e segredos em local seguro fora do repositório; ensaiar backup/restore isolado do MySQL e arquivos R2.

**Não concluído por padrão:** backup existir não equivale a restauração comprovada; ter Dockerfile não equivale a contingência validada; testes de carga simulados não equivalem a uso simultâneo real.

**Lição:** portabilidade, backups e escopo são componentes de confiabilidade, não apenas escolhas de infraestrutura.

## 7. Outros riscos e episódios para investigação histórica

- Comportamento de inscrição/aprovação de robôs e alterações de responsáveis apresentadas como se fossem feitas pelo usuário, mesmo quando a ação partiu da Gestão: avaliar atribuição e auditabilidade.
- Desaparecimento de filtro de participantes na tela de inscrições após publicação: revisar regressões de interface e estados.
- Uploads/comprovantes e integrações de mídia: diferenciar falhas de persistência, permissões, apresentação e segurança; conferir cronologia e evidências antes de descrever correções definitivas.
- Instabilidade transitória anterior de login/recuperação após implantação do storage, resolvida sem rollback ou causa raiz determinada segundo checkpoint de 12/10: preservar como incidente **sem causalidade confirmada**.

## 8. Modelo para novos registros

### INC-AAAA-MM-DD-NN — Título
- **Data e ambiente:** 
- **Sintoma e impacto real:** 
- **Evidência:** print/gráfico/log sanitizado, link de commit/PR, versão/deploy
- **Reprodução:** 
- **Hipóteses:** 
- **Causa confirmada:** ou *não determinada*
- **Medida tomada:** 
- **Validação após correção:** 
- **Medição antes/depois:** fonte, janela e unidade; ou *não disponível*
- **Estado:** aberto / mitigado / corrigido / monitorando
- **Lição e prevenção:** 

## 9. Como transformar em estudo de caso depois do evento

Organizar uma linha do tempo, registrar capturas anonimizadas, referenciar commits e separar claramente:
- **observação do usuário/gestor**;
- **causa demonstrada por logs, métricas ou diff**;
- **intervenção realizada**;
- **resultado efetivamente verificado**.

O dossiê de competição planejado para depois da validação das chaves e da operação local deverá linkar este histórico, sem reescrevê-lo nem inventar indicadores. Qualquer painel ou print deve omitir dados pessoais, e-mails, comprovantes, tokens, nomes identificáveis sem autorização e segredos.
