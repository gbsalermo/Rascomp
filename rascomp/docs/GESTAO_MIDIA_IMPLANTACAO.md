# Gestão de Mídia e Galeria — implantação segura

**Estado (14/10/2026):** o responsável aprovou o visual das interfaces e autorizou colocá-las em produção. **Implantação ainda não realizada**, pois somente as telas e as prévias localhost estão prontas; não existe API CMS persistente. Referência frontend: [PR #7](https://github.com/gbsalermo/Rascomp-FRONT/pull/7), branch `v1-pos-cloud-gestao-midia` (DRAFT).

## Infraestrutura existente aproveitável
- Backend Spring Boot 3/Java 21, autenticação JWT e Flyway/MySQL/Aiven.
- Serviço `R2ManagedFileStorage` para persistência de objetos com quota `R2QuotaService` (V32) e prefixos `gallery/`, `landing/`. Bucket permanece privado; chamadas de leitura exigem autorização anterior no controlador.
- Domínios existentes: `app.rasufrb.site` (Gestão), `rasufrb.site` (Landing) e `api.rasufrb.site` (API).
- A política vigente em `SecurityConfig` só libera `/api/v1/**` a DEV/GESTAO por fallback: o novo perfil **MIDIA** precisa receber permissão explícita nas rotas de CMS, sem ganhar permissão de Gestão competitiva. O controller público deve expor apenas projeções publicadas.

## Contrato de implementação e segurança
1. **Mídia:** entidade persistida com UUID público, chave R2 gerada no servidor, tipo MIME efetivo, tamanho, dimensões, autor/data, flags de autorização/publicação e estado. Upload com validação de tamanho/conteúdo, limite por lote, quotas, audit log; jamais receber chave R2 arbitrária do cliente. Não tornar objetos privados públicos por URLs brutas ou configuração de cache.
2. **Álbuns:** título, descrição, capa (somente foto do próprio álbum), associação N:N `AlbumMedia`, ordenação, edição e exclusão que preserva mídias; exclusão física exige verificar referências nos slots da Landing e tratar falhas de R2.
3. **Conteúdo editorial:** rascunho persistente versionado por seção, inclusive Intro/Hero, Sobre, Equipe/Conquistas, Robôs com nomes, Eventos, Patrocinadores, Competição editorial e Footer. Separar campos oficiais de competição: título, status, lotes, inscrição e categorias continuam sob API operacional.
4. **Publicação:** `Salvar rascunho`, `Prévia autenticada`, `Publicar alterações` com confirmação, versões e rollback. API pública somente leitura de snapshot **publicado**, sem expor rascunho e não publicados. Foto usada em qualquer Hero/slot **não é automaticamente pública na Galeria**. Destaques da Galeria: três IDs ordenados, apenas fotos explicitamente públicas.
5. **Endpoints candidatos** (nomes a ajustar à arquitetura existente): `/api/v1/media/assets`, `/api/v1/media/albums`, `/api/v1/media/editorial/drafts`, `/api/v1/media/editorial/preview`, `/api/v1/media/editorial/publish`; `GET /api/v1/public/media/gallery`, `/api/v1/public/media/editorial` e rota pública de bytes **somente para arquivos aprovados e autorizados**. Autorização DEV/MIDIA por método e servidor; operações sensíveis transacionais com auditoria.
6. **Sem dados fictícios na prod:** remover os 40 SVG ilustrativos e as fotos simuladas da Biblioteca da experiência publicada. Migrar/copiar conteúdo institucional real já versionado para o CMS com fallback seguro, idempotência e rollback. A Galeria pública só exibe foto efetivamente publicada.
7. **Implantação:** testes de repositório/migration/authz/quota/privacidade/consistência, smoke cloud em ambiente real e verificação de backup/restore; só depois PRs aptos a merge, Render atualizado, Cloudflare Workers do app e landing publicados e `/galeria` integrado, validando origem, cache e links. Preservar opção de execução local e plano de rollback.

## Casos mínimos de aceite
- Upload único/lote, com/sem álbuns e múltiplos álbuns; alterar associações sem reupload.
- Foto em Hero privada na Galeria; tentar acessá-la por endpoints públicos deve falhar mesmo conhecendo o ID/chave.
- Foto pública sem álbum aparece em Todas; mesma foto associada a dois álbuns não duplica em Todas.
- Escolher 3 fotos públicas nos destaques e confirmar ordem no site; despublicar uma remove-a dos destaques publicáveis.
- Salvar, recarregar Gestão e recuperar rascunho sem perda de dados.
- Prévia online somente autorizada; publicar requer confirmação; visitantes continuam na versão anterior até publicação.
- Excluir foto usada em slot/capa exige recusa ou ajuste transacional adequado; sem quota órfã.
- Deploy e smoke público sem revelar exemplos, uploads internos ou dados de teste.

**Regra:** aprovação visual e pedido de produção não autorizam publicar um CMS que descarta alterações. Não mesclar as branches ou acionar deploy até que estas condições sejam atendidas.

## Checkpoint de integração — 14/10/2026

**Implementado na branch `v1-pos-cloud-gestao-midia` (ainda sem merge/deploy):**
- Flyway `V33__media_cms_draft_publication.sql` (biblioteca, álbuns N:N, destaques, versão e snapshot publicado).
- `MediaCmsService`, `MediaCmsController`, `MediaPublicController`: endpoints para workspace, upload de imagem R2, vínculos/capas de álbuns, visibilidade de Galeria, rascunho, publicação com comparação de versão, leitura pública somente de conteúdo aprovado.
- `SecurityConfig` restringe /api/v1/media/** a DEV/MIDIA, mantendo endpoints competitivos em seus perfis existentes.
- `GlobalExceptionHandler` retorna HTTP 409 para colisões de publicação e preserva códigos de erro de mídia.
- `MediaCmsAuthorizationTest`: aplica o esquema V33 ao H2 modo MySQL de teste e verifica bloqueio sem autenticação, GESTAO impedido, MIDIA autorizado, isolamento rascunho/snapshot e conflito da versão.
- CI Backend Tests: test (sucesso) e portal-testdata com container MySQL (sucesso) no commit 1ce60dc9302d5db0ba504d85723a9ec0e0e0d7fa.

**Ainda exigido antes da produção:** confirmar Render com `R2_ENABLED=true` e variáveis `R2_ACCOUNT_ID`, `R2_ENDPOINT`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET`, além de MySQL persistente; executar upload/preview/publicação reais no ambiente homologado e validar consulta após recarregamento, acesso anônimo a fotos privadas, comportamento com falha R2, cache/CDN, backups e restauração, exclusão de ativos referenciados, e teste visual online. O rollback editorial versionado completo ainda não está implementado. Não executar deploy automaticamente com este checkpoint.


## Correção do gate R2 — configuração existente reutilizada (14/10/2026)

A integração de **Cloudflare R2** não está pendente de instalação: foi concluída em **12/10/2026**, conforme `rascomp/docs/CONTINUIDADE.md` e `rascomp/docs/CLOUDFLARE_R2.md`. O usuário confirmou anteriormente as variáveis no Render; a API no Render com `R2_ENABLED=true`, Flyway **V32** no Aiven, upload real em produção, leitura/exclusão e contabilização de cotas foram homologados. Incidente PKIX foi corrigido via truststore e novo upload foi confirmado.

**Cotas já implementadas na V32 e em `R2QuotaService`:** competição **2.700.000.000 bytes (2,7 GB)**; Galeria/Landing **5.400.000.000 bytes (5,4 GB)**; **0,9 GB** de reserva operacional proposta, mais **1 GB** de margem em relação à referência de 10 GB. Usar o bucket privado `rascomp-storage` e prefixos `competition/`, `gallery/`, `landing/`. Não criar novo bucket, novas chaves ou outra política de cotas para o CMS.

**O gate não é reconfigurar o R2.** A validação adicional necessária é específica da **nova funcionalidade CMS/V33**: deploy migrando MySQL para V33; upload real de imagem editorial com prefixo `gallery/`, persistência do rascunho, publish/undo seguro, autorização e leitura pública controlada, atualização de Landing/Galeria e verificação de retrocompatibilidade. O status das credenciais em execução pode ser confirmado durante o smoke do serviço, mas não exigir que o usuário repita a configuração nem envie valores secretos.

Limites R2 dizem respeito a **armazenamento em bytes**, e não ao limite de **RAM do Render**, que pertence ao dimensionamento do serviço Java.
