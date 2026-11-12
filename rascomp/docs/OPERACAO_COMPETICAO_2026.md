# Operacao RASCOMP — competicao 2026 (14/11)

## Escopo e estado

- Frontend de gestao: https://app.rasufrb.site (nuvem, sem migracao).
- Landing: https://rasufrb.site (inalterada).
- API atual: https://api.rasufrb.site (Render; preservar ate o corte).
- Tunnel permanente: rascomp-competicao-2026; teste: https://api-teste.rasufrb.site -> http://localhost:8080.
- Servidor futuro: Windows com Spring Boot local, porta 8080.
- Banco na fase 1: Aiven atual. MySQL local e uma migracao posterior, NAO automatica.

**Nenhum passo deste documento deve alterar o DNS de api.rasufrb.site antes da homologacao.** A rota de teste e publica; so ligar o backend quando autenticacao, CORS e dados estiverem protegidos.

## Preparacao do ambiente (sem alterar o Render)

1. Copiar `.env.competicao.example` para `.env.competicao` no computador servidor. Nunca commitar esse arquivo ou divulgar credenciais.
2. Preencher DB_URL, DB_USERNAME, DB_PASSWORD e JWT_SECRET com valores do ambiente real. Confirmar TLS do Aiven conforme as exigencias do provedor. Reutilizar a chave JWT da producao apenas por canal seguro, se for necessaria continuidade de sessoes.
3. Configurar CORS_ALLOWED_ORIGINS para o frontend hospedado, IDENTITY_FRONTEND_BASE_URL e o provedor de e-mail real.
4. Confirmar os caminhos dos uploads e copiar os arquivos necessarios; banco SQL sozinho nao contem fotos e comprovantes.
5. O Spring Boot NAO le `.env.competicao` automaticamente. Para PowerShell, carregar cada par KEY=VALUE no processo com metodo seguro (sem imprimir segredos) antes de iniciar o JAR; ou usar mecanismo equivalente de injecao de variaveis. Nao confundir arquivo de exemplo com ambiente ativo.
6. Iniciar o backend com Java 21 e profile `cloud`, sem habilitar dados de teste. Verificar porta 8080 e logs de startup sem expor segredos.

## Cloudflare Tunnel — Windows

- O DNS CNAME de `api-teste.rasufrb.site` ja foi criado no painel do Tunnel.
- No computador servidor, instalar o conector como servico Windows com o comando **fornecido pelo painel Cloudflare**. O token e segredo: nunca incluir em logs, chat, Git ou screenshots.
- O Quick Tunnel usado em testes e diferente do Tunnel permanente.
- A rede da UFRB apresentou conectividade parcial com regioes Cloudflare; o Quick Tunnel funcionou com `--protocol http2`. Antes da competicao, verificar como forcar HTTP/2 no servico permanente instalado, inspecionar os logs e confirmar reconexao apos reinicio. Nao presumir que a flag do Quick Tunnel se aplique automaticamente ao servico.
- Validar `Get-Service cloudflared`, status Connected no painel e acesso externo por rede movel.
- `/actuator/health` confirma resposta HTTP da aplicacao, **nao** confirma que Aiven, login, inscricoes ou arquivos funcionam: no profile cloud a checagem DB do actuator esta desabilitada.

## Testes de homologacao antes da troca de DNS

1. Em `api-teste.rasufrb.site`, verificar HTTPS, resposta de health e disponibilidade da API com autenticacao.
2. Testar CORS com Origin `https://app.rasufrb.site`, inclusive preflight OPTIONS de login. Nao abrir CORS com wildcard.
3. Testar login, `/auth/me`, leitura de inscricoes pendentes e aprovadas separadamente, aprovar/recusar, consulta de robos e comprovantes.
4. Testar chaveamento de dupla eliminacao, persistencia dos resultados e bloqueios do modo competicao em ambiente controlado. Nao executar operacoes destrutivas sobre dados reais.
5. Reiniciar backend e conector; verificar recuperacao, desempenho e acesso externo.
6. Confirmar que o frontend da nuvem aponta para a API desejada antes da virada. Se a URL estiver embutida no build, o DNS de `api.rasufrb.site` pode permitir manter o frontend intacto.

## Backup, migracao de banco e retorno

**Antes de qualquer migracao para MySQL local:**
1. Identificar versao MySQL e privilegios do Aiven; confirmar ferramentas compativeis.
2. Planejar janela sem escritas e produzir dump consistente (`mysqldump` com `--single-transaction` para tabelas InnoDB, incluindo triggers/routines/events conforme existirem). Proteger credenciais e arquivo.
3. Salvar copia independente dos uploads operacionais e verificar tamanho/quantidade.
4. Restaurar em banco local **isolado**, verificar Flyway, contagens, usuarios, inscricoes e integridade; testar login e fluxos reais de leitura.
5. Fazer backup final na janela de corte e impedir escritas concorrentes no Aiven e no MySQL local. Nao permitir duas fontes de verdade.
6. Criar rotina de backups frequentes durante a competicao, com copia em outro dispositivo e teste de restauracao.

**Rollback:** antes de liberar escritas no banco local, pode-se retornar ao Render/Aiven restaurando o DNS antigo. Depois que houver novas escritas locais, voltar ao Aiven requer conciliacao ou restauracao controlada desses dados; simplesmente trocar DNS perderia resultados. Registrar o momento do corte e as copias disponiveis.

## Checklist da virada

- [ ] API de teste validada por rede externa
- [ ] Conector Windows em servico e HTTP/2 validado
- [ ] CORS, JWT, login e permissoes verificados
- [ ] Inscricoes, chaveamentos e uploads verificados
- [ ] Banco e arquivos com backup restauravel
- [ ] Plano de retorno testado e responsavel definido
- [ ] Somente entao alterar `api.rasufrb.site`
