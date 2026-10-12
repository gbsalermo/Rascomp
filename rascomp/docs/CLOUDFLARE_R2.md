# Cloudflare R2 — armazenamento de mídias

## Objetivo

Preparar o backend RasComp para armazenar futuramente fotos da galeria e outras mídias em Cloudflare R2 usando a API compatível com Amazon S3.

A integração foi adicionada de forma **opt-in** e não altera o comportamento atual da aplicação.

- `R2_ENABLED=false` por padrão.
- `RobotImageStorageService` continua usando a pasta local configurada por `ROBOT_IMAGES_DIR`.
- nenhuma entidade, tabela ou migration atual foi alterada.
- nenhuma rota existente foi substituída.
- nenhuma credencial R2 deve ser versionada.

## Implementação

Arquivos principais:

```text
config/R2StorageProperties.java
config/R2StorageConfiguration.java
storage/ObjectStorageService.java
storage/R2ObjectStorageService.java
```

O backend utiliza AWS SDK for Java v2 contra o endpoint S3 do R2.

Configuração específica do R2:

```text
region = auto
pathStyleAccessEnabled = true
chunkedEncodingEnabled = false
```

O `chunkedEncodingEnabled(false)` é importante para uploads via AWS SDK Java v2 no Cloudflare R2.

## Recursos já disponíveis no serviço

`R2ObjectStorageService` fornece:

```text
upload(...)
delete(...)
exists(...)
publicUrl(...)
presignedUploadUrl(...)
```

A URL pré-assinada está preparada para a futura tela de upload da Gestão, mas ainda não foi exposta por endpoint público/admin. Isso é proposital para não adicionar um fluxo incompleto à aplicação atual.

## Criar o bucket no Cloudflare

Sugestão de nome:

```text
ras-ufrb-media
```

Estrutura planejada:

```text
ras-ufrb-media/
└── gallery/
    ├── rrc-2026/
    │   ├── cover.webp
    │   ├── thumbs/
    │   └── photos/
    ├── oficinas/
    └── ras-nas-escolas/
```

No Cloudflare Dashboard:

1. abrir **Storage & databases > R2**;
2. criar o bucket;
3. criar um token/API credential com permissão de leitura e escrita limitada ao bucket da galeria;
4. copiar `Access Key ID`, `Secret Access Key`, `Account ID` e endpoint S3;
5. não colocar essas credenciais em Git/GitHub.

Endpoint padrão:

```text
https://<ACCOUNT_ID>.r2.cloudflarestorage.com
```

## Variáveis de ambiente

Enquanto não houver credenciais, manter:

```env
R2_ENABLED=false
```

Para ativar:

```env
R2_ENABLED=true
R2_ACCOUNT_ID=seu_account_id
R2_ACCESS_KEY_ID=sua_access_key
R2_SECRET_ACCESS_KEY=sua_secret_key
R2_BUCKET=ras-ufrb-media
R2_PUBLIC_BASE_URL=https://seu-dominio-publico-ou-r2-dev
```

`R2_ENDPOINT` é opcional. Se não for informado, o backend monta o endpoint pelo `R2_ACCOUNT_ID`.

Para endpoints especiais/jurisdictions, informar diretamente:

```env
R2_ENDPOINT=https://<ACCOUNT_ID>.<JURISDICTION>.r2.cloudflarestorage.com
```

## Segurança

Nunca expor no frontend:

```text
R2_ACCESS_KEY_ID
R2_SECRET_ACCESS_KEY
```

O frontend deverá receber apenas:

- URLs públicas de leitura; ou
- URLs pré-assinadas temporárias emitidas pelo backend para upload autorizado.

## Próxima etapa

Quando houver o primeiro conjunto real de imagens:

1. criar entidades `Album` e `Photo` + migration;
2. criar endpoints públicos de leitura de álbuns;
3. criar endpoints de Gestão para criar/editar álbuns;
4. gerar object keys no backend;
5. emitir URL pré-assinada para upload;
6. salvar metadados no MySQL;
7. substituir os dados estáticos de `photo-gallery` pela API;
8. manter a Landing carregando apenas capa e pequenas prévias.

Essa etapa deve ser implementada separadamente para preservar o funcionamento atual até o primeiro teste real com R2.


## Política de cotas do bucket `rascomp-storage` — decisão 12/10/2026

**Orçamento operacional planejado (capacidade de referência de 10 GB):**

| Classe | Cota máxima | Uso previsto |
|---|---:|---|
| Competição | 3 GiB | fotos de robôs, logos, comprovantes, arquivos de inscrições |
| Galeria e mídia editorial | 6 GiB | álbuns, fotografias, imagens públicas da landing, banners |
| Reserva técnica | 1 GiB | margem para correções e contingência; não é área de upload regular |

**Atenção**: a franquia gratuita do R2 é faturada em **GB-mês**, enquanto as cotas internas são limites de bytes em um instante; 10 GB-mês não representa automaticamente um teto de 10 GB permanentes nem impede cobrança por uso acima da franquia ou por operações. Adotar configuração conservadora e alertas de uso e faturamento. Verificar unidade contratual (GB decimal × GiB binário) ao definir os limites reais; não permitir que soma de cotas ultrapasse limite conservador ajustado à franquia. Esta política não está aplicada no bucket apenas pela documentação.

**Estratégia de enforcement obrigatória antes de uploads reais**:

- Categorizar todo objeto no upload: `competition/` versus `gallery/` (inclui `landing/`), reservando margem técnica fora desses prefixos.
- O backend é autoridade para impor cotas; não confiar em dados do frontend ou na pasta escolhida pelo cliente. Não oferecer credenciais S3 diretamente ao navegador.
- Registrar no MySQL tamanho real em bytes, object key, classe, estado do upload e dono; conciliar periodicamente com a listagem/tamanho reais do R2.
- Antes de enviar, impor tamanho máximo por arquivo e por categoria; reservar capacidade de forma **atômica** no MySQL para impedir que uploads concorrentes ultrapassem a mesma cota. Para URLs presigned, finalizar só após confirmar tamanho real do objeto; tratar uploads incompletos/expirados e objetos órfãos.
- Exclusões liberam espaço somente após exclusão confirmada no R2; substituições e reuploads consideram temporariamente o tamanho de ambos os objetos e limpeza posterior.
- Bloquear novos uploads da categoria ao atingir o limite, mantendo acesso/download dos arquivos e exibindo mensagem administrativa orientando liberar espaço.
- Limitar custos também por contagem de operações e tamanho máximo de arquivo; fornecer indicadores do consumo por categoria, global e reserva, com alertas antecipados (ex.: 70%, 85%, 95%).
- Não usar o 1 GiB reservado para arquivos comuns sem decisão administrativa explícita. Preferir não permitir exceder a cota de categoria mesmo que a outra ainda tenha espaço.
- Ao migrar conteúdo pré-existente, inventariar e contabilizar objetos antes de ativar a política.
- Testar concorrência, uploads interrompidos, tentativas de bypass e discrepância entre metadados do banco e objetos antes de ativar `R2_ENABLED=true` no Render.

**Estado:** decisão de produto documentada; ainda requer implementação e testes no backend/gestão. Acompanhar na conclusão da integração R2.
