# Plano de migração de mídia — RasComp (planejamento, sem execução)

Estado: PLANEJADO. Branch de trabalho: `feat/futebol-inscricao-sem-robo` (backend e frontend). **Não implementar, publicar ou fazer merge nesta etapa.**

## Motivo e objetivo
Em 15/10/2026 o Render registrou repetidos encerramentos por falta de memória (instância de 512 MB) durante operações de Gestão de Mídia. A implementação atual de `R2ManagedFileStorage.read()` utiliza `S3Client.getObjectAsBytes()`, e `MediaCmsService.content()` passa o conteúdo ao controlador Spring Boot. A Gestão solicita imagens privadas por `/api/v1/media/assets/{id}/content`; o público utiliza `/api/v1/public/media/assets/{id}/content`. Mesmo com o limite de duas transferências paralelas no frontend, cada leitura ainda materializa o arquivo inteiro na memória do processo Java.

**Meta:** retirar a transferência de bytes de imagens públicas e prévias administrativas do Render; manter no Spring Boot autorização, metadados, cadastro, publicação e regras de negócio. Preservar R2 inicialmente e evitar qualquer exposição de comprovantes privados.

## Arquitetura-alvo
- **Imagens publicadas da Landing/Galeria:** URLs de mídia publicamente acessíveis por domínio CDN próprio (proposto `media.rasufrb.site`), conectadas a área/namespace de distribuição PUBLICADA no R2. Navegador -> CDN/R2; sem proxy do Render.
- **Mídias ainda privadas do CMS:** backend verifica JWT/papel e gera URL **GET temporária e restrita ao objeto**, usando presigned URL S3/R2 (ou gateway Worker privado com token curto se o CORS/SDK exigir); navegador transfere diretamente. Nunca reutilizar URL pública para rascunho.
- **Upload:** avaliar presigned PUT com autorização, limite de tamanho e tipo, confirmação e validação no backend; primeira entrega pode preservar upload atual com limites rígidos se o escopo exigir. Evitar memória integral no Render também na gravação na segunda fase.
- **Comprovantes e outros arquivos privados:** caminho privado permanece protegido; não incluir no domínio público.
- **Metadados:** Render/Aiven; respostas do CMS devem retornar referências e flags de publicação, não bytes.

## Etapa zero obrigatória — backup do estado de produção (competição em andamento)
**Bloqueante:** antes de modificar armazenamento, contratos de mídia, deploy ou executar migração, obter uma cópia recuperável do estado operacional de produção, preservando competidores, inscrições, comprovantes, equipes, robôs, categorias, chaves, partidas, resultados, rankings e configuração da competição atual.

1. Registrar data/hora e versões exatas implantadas (SHA backend e frontend), versões Flyway, variáveis de ambiente **sem registrar segredos em texto puro**, serviços e respectivas configurações. Fazer exportação segura da configuração sensível para cofre restrito.
2. Gerar **snapshot/backup consistente do MySQL de produção (Aiven)**, validando transações e consistência, sem apagar nem sobrescrever dados ativos. Guardar cópia externa criptografada com controle de acesso.
3. Fazer **inventário e cópia de segurança dos objetos R2 existentes**, tanto mídias públicas quanto documentos/comprovantes privados, incluindo chaves, metadados e mapeamento com registros do banco. Nunca publicar ou expor o backup dos comprovantes.
4. Exportar snapshots editoriais publicados e rascunhos do CMS (separando-os e preservando versões), configurações de publicação da Landing/Galeria e referências das imagens.
5. Conferir contagem, integridade, hashes/amostras, espaço ocupado e correspondência banco↔objetos. **Testar restauração em ambiente isolado**, com banco e armazenamento separados de produção, sem disparar notificações, integrações ou ações sobre a competição real.
6. Definir ponto de retorno: instruções documentadas para restaurar versões de aplicação, banco e arquivos de maneira coordenada. Para rollback durante competição ativa, avaliar gravações ocorridas após o snapshot: **não restaurar banco antigo por cima de inscrições ou resultados novos**. Pausar operações somente com janela autorizada e considerar backup incremental/captura de alterações até o corte.
7. Registrar responsável, horário, local seguro do backup, confirmação da restauração e autorização expressa antes da execução da migração. Nenhuma alteração disruptiva deve ocorrer durante partidas ou operações críticas.

**Critério de início da migração:** backup integral verificado e restauração isolada comprovada, plano de retorno viável e janela operacional aprovada. Um commit Git, sozinho, não faz backup do banco ou dos objetos R2.

## Etapas previstas (executar amanhã após revisão de configurações)
1. **Inventário e medição:** mapear todos os consumidores da mídia (editor, prévia, Sobre, Hero, Galeria, eventos, equipes, comprovantes); medir tamanho/quantidade, memória do Render, códigos 5xx e tempos de resposta. Conferir quotas/limites reais do R2 e Cloudflare, origem do CORS e comportamento do proxy.
2. **Segurança e distribuição:** definir duas classes de objetos (público publicado x privado), namespace e políticas sem tornar o bucket de origem indiscriminadamente público. Configurar domínio CDN, CORS restrito a `https://rasufrb.site` e `https://app.rasufrb.site` onde aplicável, cache de conteúdo versionado, invalidação/alteração de visibilidade. **Exigirá configuração externa Cloudflare/R2 e possível segredo/variável de ambiente**; não inserir chaves no repositório.
3. **Contrato backend:** API fornece URL estável versionada para publicado e endpoint autenticado de URL temporária para prévia; manter autorização de acesso antes de assinar. Diferenciar conteúdo publicado de rascunho; adicionar expiração, restrição de métodos e política de cache. Planejar fallback controlado para mídia legada.
4. **Migração dos objetos:** preservar IDs, associação a álbuns e referências dos rascunhos; copiar/organizar objetos existentes quando necessário, com conferência de integridade e sem excluir originais antes de validar. Publicação, despublicação e troca de imagem devem refletir visibilidade na CDN e não vazar rascunhos.
5. **Frontend:** Gestão deixa de fazer download de cada imagem via JWT + Blob; usa URLs autorizadas e carregamento sob demanda (miniaturas primeiro, imagem completa só ao ajustar). Landing, Hero, Sobre, Eventos e Galeria usam URLs CDN; limitar preloads, evitar reflow com dimensões/aspect-ratio e estados de erro. Preservar comportamento da prévia editorial e do carrossel.
6. **Testes e rollout:** validar autorização (DEV/Gestão vs não autorizado), rascunho não publicado, expiração, CORS, cache/revogação, upload, sequência de Hero, Galeria, imagens antigas, mobile, desempenho e regressão das inscrições/robôs. Verificar build frontend/backend e testes relevantes. Publicar em etapas com feature flag/fallback, observar memória e logs em cenário representativo; desativar endpoints de bytes somente após confirmação.
7. **Documentação:** atualizar contratos de API, variáveis, configuração DNS/R2, runbook de falha/rollback, custos/limites e roadmaps dos dois repositórios antes do merge conjunto.

## Critérios de aceite
- A visualização e ajuste de imagens não provoca encerramento do Render por memória; registrar testes comparativos.
- Imagens públicas não atravessam o Render; conteúdo administrativo privado só é acessível com autorização e link de curta duração.
- Sem exposição de rascunhos/comprovantes; remoção/despublicação propagada de forma previsível.
- Hero e seções renderizam sem atraso perceptível causado por chamadas de mídia desnecessárias; imagens têm layout estável.
- Frontend e backend passam builds/testes relevantes, e o fluxo atual de futebol não sofre regressões.
- Mantido rollback e sem alterações em produção até autorização para integrar.

## Limites desta entrega
Somente documento de planejamento na branch de futebol. **Nenhuma modificação de código, credencial, infraestrutura, produção ou merge.** Necessário validar o desenho exato da CDN privada/pública antes de executar.

## Revisão de arquitetura e sequência para fechamento do MVP — 15/10/2026

**Decisão:** a Galeria passa a ser uma aplicação exclusivamente de fotografias e vídeos, Vue 3 + Vite em Cloudflare Pages, proposta `galeria.rasufrb.site`. Preservar estilo visual da Galeria atual. O botão da Landing redireciona para esse domínio. Visitantes não precisam de login; administração usa autenticação do RasComp/Render com autorização específica para gestão de mídia, sem conceder poderes de upload a participantes comuns.

**Separação de infraestrutura:** Cloudflare Pages serve a interface; armazenamento/distribuição de mídia pelo R2/CDN, sem encaminhar bytes pelo Render. Avaliar Worker + D1 para metadados, álbuns, destaques e autorização de transferência, integrando autenticação sem expor segredos JWT; validar acesso com backend ou chaves de verificação apropriadas. Manter apenas imagens de seções institucionais/Hero na Gestão de Mídia do RasComp; seus arquivos também devem sair do proxy Render, conforme plano original. Comprovantes permanecem privados e separados.

### Ordem de entrega e gates
0. **Antes de publicar novas funcionalidades na competição ativa:** executar backup mínimo consistente (MySQL/Aiven + objetos R2 relevantes, com confirmação de integridade), registrar SHAs e rollback. Esta salvaguarda não substitui o backup integral e restauração isolada exigidos antes da migração de mídia.
1. **Estabilização/controle de risco:** suspender temporariamente **edição/novos uploads da Galeria no CMS do RasComp**, por configuração/flag reversível e comunicação aos operadores; **não** retirar visualização pública, apagar acervo, bloquear editor de Heroes/Sobre/Eventos ou quebrar publicação das seções. Avaliar operação de upload de imagens institucionais com limite e segurança; nenhuma ação disruptiva em competição.
2. **Entrega funcional da branch:** concluir e validar inscrição de Futebol sem robô, fluxos de aprovação, regras de suspensão/associação e área Regras e Ajuda (documentos: regras de competição, conduta e manual; upload/gestão apenas DEV). Garantir testes de segurança, fluxos de participante, regressão Sumô/Follow e builds. **Não fazer merge/deploy antes de validação**.
3. **Deploy conjunto e monitorado da etapa funcional:** após backup e testes, janela apropriada e autorização; acompanhar memória do Render, erros e integridade de inscrições/competição. Manter rollback sem perder dados novos.
4. **Pré-migração da Galeria:** executar backup completo com teste de restauração em ambiente isolado (etapa zero detalhada abaixo), inventariar álbuns, capas, fotos, vídeos e referências. Validar estratégia de corte sincronizado e dados gerados durante competição.
5. **Nova Galeria:** implantar aplicação separada, armazenamento e operações próprias, upload direto seguro por URL temporária, miniaturas/derivados, suporte controlado a vídeo (limites, formatos, custos; eventual streaming dedicado), metadados/álbuns/destaques, links e autenticação administrativa. Transferir e validar acervo e referências sem perda.
6. **Corte gradual:** apontar botão Galeria para novo domínio após smoke e aceite; manter rota antiga com redirecionamento e rollback temporário; retirar gestão legada somente quando comprovada migração e sem perda de acesso.
7. **Fechamento MVP:** verificar estabilidade da competição e Galeria independentemente, métricas de memória, segurança/autorização, custo, documentação e roteiro operacional.

**Não é agendamento automático nem execução antecipada:** esta é a sequência registrada para a próxima sessão de trabalho. A suspensão administrativa e o novo domínio ainda não estão implantados.

### Critérios adicionais
- Operador consegue ajustar imagens das **seções** sem usar a administração da Galeria; nenhum dado do CMS desaparece.
- Galeria acessível sem sessão RasComp; somente papel autorizado administra fotos/vídeos; upload não atravessa Render.
- Falha da Galeria não derruba API de competição; publicação não expõe rascunhos, documentos ou comprovantes.
- Backups anteriores aos deploys funcionais e anteriores à migração, com restauração verificável e rollback que preserve escritas novas.


## Estado da contenção pré-lote
**Implementado apenas na branch:** leitura de objetos R2 por `InputStreamResource` (sem `getObjectAsBytes`), bloqueio reversível das mutações administrativas de Galeria pelo parâmetro `app.media.gallery-management-enabled` (padrão `false`; testes específicos do CMS habilitam o modo antigo). Upload de imagem de seção por `/api/v1/media/assets` com `galleryDraft=false` e publicação editorial preservados. **Não implementado:** bypass completo de mídia via CDN e URLs assinadas. **Não publicado em produção.** O CI geral mantém falhas anteriores de testes; não fazer merge sem verificação de migrations e backup pré-deploy.


## DECISÃO DE ESCOPO — MVP V1 ENCERRADO / GALERIA V2.0 (15/10/2026)

**Decisão atual, prevalece sobre cronogramas anteriores:** o MVP V1 do RasComp fica limitado à operação da competição, inscrições (incluindo Futebol), gestão, regras/manuais do participante e Landing institucional. A **Galeria independente de fotografias e vídeos deixa de ser requisito do MVP V1**. A implementação da nova Galeria, extração do acervo e mudança para `galeria.rasufrb.site` serão tratadas **depois da competição**, em uma **feature/branch própria de V2.0**, com planejamento, testes, backup e migração sem perda de acervo.

**Até a V2.0:** manter a **prévia institucional da Galeria na Landing**, sem link no cabeçalho e sem acesso à Galeria completa; manter ocultas as entradas Biblioteca/Álbuns da Gestão. Preservar o editor das seções (Heroes, Sobre, Eventos etc.) e os arquivos operacionais de robôs, comprovantes e logos. Não apagar dados, objetos R2 nem tabelas legadas da Galeria. Contenção de memória já aplicada permanece, e a estabilidade do Render/inscrições continua prioridade operacional da V1.

**V2.0 planejada, não iniciada:** frontend Vue/Vite em Cloudflare Pages, R2/CDN para fotos/vídeos, autenticação administrativa reaproveitada do RasComp, metadados/álbuns em camada própria conforme validação de arquitetura. Nenhum deploy, agendamento automático ou migração de acervo decorre desta decisão documental.
