# Roteiro de execucao — do Cloudflare Tunnel a homologacao

**Status:** preparado, nao executado. **Nao modificar Render nem api.rasufrb.site ate o aceite.**
Referencia: `OPERACAO_COMPETICAO_2026.md`, `HOMOLOGACAO_COMPETICAO_2026.md` e `MANUAL_CONTINGENCIA_GESTORES.md`.

## Fase A — no computador Windows servidor
1. Atualizar clone do backend e conferir Java 21, Maven e cloudflared; executar `powershell -ExecutionPolicy Bypass -File .\scripts\verificar-preparacao-competicao.ps1` dentro de `rascomp`.
2. No painel Cloudflare Zero Trust, abrir tunnel **rascomp-competicao-2026** e copiar **somente no servidor** o comando de instalacao Windows com token. Executar PowerShell como Administrador. **Nunca colar token no chat, em commits ou em screenshots.**
3. Verificar `Get-Service cloudflared` e status **Healthy/Connected** no painel. Se nao conectar na rede UFRB, confirmar logs e configuracao do conector para transporte **HTTP/2**; o Quick Tunnel anterior exigiu `--protocol http2`. Nao presumir que a configuracao do Quick Tunnel vale para o servico.
4. Manter a rota **api-teste.rasufrb.site → http://localhost:8080**. **Nao alterar api.rasufrb.site**.
5. Confirmar politica de reinicio do servico e testar reinicio controlado da maquina. Confirmar acesso por 4G.

## Fase B — homologacao isolada (antes de qualquer escrita)
1. Instalar MySQL local ou disponibilizar MySQL isolado por Docker. Criar usuario e schema exclusivos de homologacao; **nao apontar para Aiven**.
2. Preparar variaveis de ambiente locais, banco e JWT exclusivos; nao usar usuarios/segredos de producao. **Spring Boot nao importa automaticamente arquivos .env**.
3. Subir backend local na porta 8080 com Flyway e configuracoes de teste, sem habilitar initializers de demonstracao em ambiente de producao. Confirmar migrations e acesso autenticado.
4. Usar uma URL de frontend de homologacao apontando para `api-teste.rasufrb.site`, ou chamadas autenticadas controladas por ferramenta de API. **O frontend de producao continua apontando para api.rasufrb.site** e nao deve ser usado para fazer testes de escrita no banco isolado.
5. Criar massa sintetica por fluxos de dominio: 13, 15 e 20 robos Sumô; 4–6 Follow; equipes, responsaveis, inscricoes, juizes e inspecoes. Registrar IDs e seed.
6. Testar health, preflight CORS, login, permissoes, leitura e escrita apenas no banco de homologacao.
7. Executar testes Maven e guardar saida de Surefire. Exemplo na pasta `rascomp`: `mvn test -Dtest=DoubleEliminationGenerationTest,DoubleEliminationProgressionTest,DoubleEliminationPodiumTest`. Testes de integracao e ponta a ponta ainda precisam ser implementados e executados; **nao tratar os testes Mockito como homologacao completa**.

## Fase C — operacao manual completa
- [ ] Gestor acessa e seleciona competicao; confere participantes/robos
- [ ] Cadastra juiz e valida suas permissoes
- [ ] Inspeciona e confirma aptidao dos robos
- [ ] Encerra inscricoes e gera dupla eliminacao
- [ ] Abre **Agenda** e agenda apenas partidas com participantes definidos: horario, pista, ordem e convocacao
- [ ] Confere **conflitos de pista/juiz/robo** manualmente
- [ ] Convoca partida, registra rounds, encerra resultado
- [ ] Confere a progressao Winners → Losers, BYEs e surgimento de novas partidas na Agenda
- [ ] Repete ate Final Geral; testa vitoria do invicto e em nova simulacao o Reset
- [ ] Confere podium, resultados publicos e persistencia apos reinicio
- [ ] Follow Line: cria tomada, sincroniza fila, altera ordem, convoca, registra tempos/ausencias e confere ranking
- [ ] Simula atraso, ausencia, resultado incorreto e queda de conexao com fichas de contingencia

**Nao iniciar manualmente todas as partidas de uma vez.** Na dupla eliminacao, a Agenda e dinamica e depende da resolucao dos confrontos anteriores.

## Fase D — decisao de entrada em producao
1. Conferir todos os itens criticos PASSOU com evidencias; erros de chave, agenda, final/reset ou persistencia bloqueiam o corte.
2. Se o objetivo for usar **Aiven no dia**, subir uma **segunda instancia local de producao**, separada do banco de homologacao e com configuracao correta; fazer somente verificacoes nao destrutivas antes da virada.
3. Se o objetivo for **MySQL local**, planejar janela de congelamento das escritas, backup consistente de Aiven e arquivos, restauracao e conciliacao. Nao manter dois bancos recebendo escritas.
4. Confirmar que o frontend publicado usa `https://api.rasufrb.site`; alterar a rota DNS/public hostname para o tunnel **somente apos aprovacao**. O hostname de teste permanece independente.
5. Validar login, CORS, inscricoes, agenda, chave, rounds e publico via rede externa apos a virada.
6. Registrar hora do corte, responsavel, backups e procedimento de retorno. **Depois de novas escritas locais, trocar DNS de volta sem reconciliar dados nao e rollback seguro.**

## Evidencias e criterios
Registrar para cada teste: ID, commit, horario, ambiente, seed, resultado esperado/observado, PASSOU/FALHOU/BLOQUEADO, screenshot/log e responsavel. Nunca anexar tokens, credenciais ou dados pessoais.


## Decisao operacional de infraestrutura - 12/11/2026

**Sequencia aprovada:** (1) finalizar configuracao do backend local, Cloudflare Tunnel e homologacao completa no notebook atual; (2) somente apos validacao, transferir a configuracao testada para o PC mais estavel, planejado como servidor principal; (3) organizar e testar os planos B e C: Render como contingencia externa e notebook como contingencia local. Nao antecipar a migracao nem modificar DNS de producao ou dados reais durante homologacao.

**Pontos de validacao antes da troca:** conectividade HTTP/2 do tunnel, reinicio automatico do servico, Java/build, acesso ao banco, login/CORS, operabilidade real de inscricoes e aprovacoes, inspecao, chaveamento de Sumô, agenda de Sumô e Follow, filas, convocacoes, resultados, podio, carga e recuperacao. O fluxo manual deve ser executavel pelas opcoes existentes na interface, sem SQL corretivo ou dados artificiais inseridos para suprir controles ausentes.

**Contingencia ainda nao implementada:** documentar passo a passo de failover/failback, evitar processamento concorrente por dois backends, verificar dependencia comum do Aiven, disponibilidade de internet/energia do PC principal e comportamento de inicializacao do Render. Nao tratar o Render ou notebook como failover automatico antes de teste explicito.
