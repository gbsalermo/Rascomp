# Homologacao operacional — RASCOMP 2026

**Estado:** plano preparado; testes NAO executados nem homologados.
**Regra de seguranca:** nao testar escritas no Aiven/Render de producao. Usar banco isolado, usuarios ficticios e API de homologacao. Nunca usar tokens ou comprovantes reais.

## Objetivo
Reproduzir os volumes e os relacionamentos atuais da competicao sem copiar dados pessoais. Priorizar Sumô em dupla eliminacao e fazer uma regressao reduzida do Follow Line.

## Massa sintetica
- Sumô 13 robos: 16 posicoes, 3 BYEs.
- Sumô 15 robos: 16 posicoes, 1 BYE.
- Sumô 20 robos: 32 posicoes, 12 BYEs.
- Follow Line: 4 a 6 robos.
- Equipes, usuarios de gestao, juizes, categorias, inscricoes, responsaveis e inspecoes com relacionamentos validos.
- Incluir pelo menos um robo reprovado na inspecao e um inscrito nao aprovado para confirmar exclusao da chave.
- A massa precisa respeitar Flyway, restricoes e validacoes de dominio. Nao inserir diretamente em tabelas sem verificar as entidades e servicos.

## Testes automatizados — criterios de aceite
Para cada quantidade de Sumô (13, 15, 20):
1. Gerar chave DOUBLE_ELIMINATION e conferir unicidade de (bracket, rodada, ordem), arestas e quantidade de BYEs.
2. Simular resultados ate o fim, sem chamadas HTTP de producao.
3. Apos cada partida, verificar que vencedor avanca, perdedor de Winners vai a Losers e perdedor de Losers e eliminado.
4. Nao admitir o mesmo robo em A e B; nenhuma inscricao elegivel desaparece; BYE nao conta como derrota.
5. Testar campeao invicto (sem reset) e campeao oriundo da Losers (reset obrigatorio).
6. Verificar que pódio so fica CONCLUIDO com final decisiva e terceiro lugar definidos.
7. Repetir com sementes de sorteio diferentes e registrar falhas reproduziveis.
8. Validar bloqueio de correcoes quando dependentes ja possuem rounds/resultados; testar correcao permitida antes de iniciar dependentes.
9. Verificar persistencia em MySQL de homologacao, transacoes e recarga apos reinicio.
10. Validar permissoes de gestao e rejeicao de chamadas sem autorizacao.

**Oraculo de partidas reais:** para N inscritos, sem desistencia/desclassificacao, sao 2N-2 partidas disputadas sem reset e 2N-1 com reset. BYEs e nos vazios nao contam. Para 13: 24/25; 15: 28/29; 20: 38/39.

## Roteiro manual ponta a ponta (registrar evidencia por passo)
- [ ] M01 Login de gestor e conferencia da competicao/categoria
- [ ] M02 Conferencia de equipes, robos, inscricoes e responsaveis
- [ ] M03 Cadastro/vinculacao de juiz e validacao de permissao
- [ ] M04 Inspecao: apto, inapto, correcao e verificacao dos elegiveis
- [ ] M05 Encerramento de inscricoes e bloqueio de geracao antecipada
- [ ] M06 Geracao DOUBLE_ELIMINATION e conferencia visual de Winners, Losers, Grand Final e Reset
- [ ] M07 Agenda, convocacao e atribuicao de juiz conforme interface disponivel
- [ ] M08 Inicio de partida, rounds, placar, conclusao e proxima partida
- [ ] M09 Primeira derrota envia para Losers; segunda derrota elimina
- [ ] M10 BYEs nao eliminam nem criam partidas reais
- [ ] M11 Grand Final invicto vence: encerra sem reset
- [ ] M12 Em segunda simulacao, Losers vence Grand Final: reset ocorre
- [ ] M13 Correcao de resultado antes de dependentes e bloqueio apos disputa
- [ ] M14 Desistencia/desclassificacao e decisao administrativa
- [ ] M15 Podio, resultados publicos e historico
- [ ] M16 Reinicio da API e reconexao; dados permanecem consistentes

## Follow Line — regressao curta
- [ ] F01 Cadastrar/selecionar 4 a 6 robos elegiveis
- [ ] F02 Registrar tomadas e tempos validos
- [ ] F03 Conferir ranking, desempates e resultado publico
- [ ] F04 Testar uma correcao/decisao administrativa autorizada

## Registro de execucao
Para cada ID: data/hora; responsavel; ambiente e commit; massa/semente; resultado esperado; resultado observado; evidencias (prints/logs sem dados pessoais); PASSOU/FALHOU/BLOQUEADO; issue e acao corretiva.

## Portoes de liberacao
- Nao homologar apenas por testes unitarios com Mockito.
- Bloqueio: erro de eliminacao, avancos, BYE, reset, podio ou perda de dados.
- Corrigir primeiro em ambiente isolado; executar regressao de simples e Follow antes de publicar.
- Somente liberar a operacao apos os cenarios criticos automatizados e manuais passarem.
