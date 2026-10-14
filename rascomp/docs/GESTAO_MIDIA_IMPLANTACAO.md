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
