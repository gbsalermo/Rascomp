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
