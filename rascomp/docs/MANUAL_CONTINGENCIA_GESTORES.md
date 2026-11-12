# Manual de contingencia dos gestores — RASCOMP 2026

**Versao preliminar.** Este guia nao substitui a validacao manual. Acoes excepcionais devem ser executadas apenas por pessoas autorizadas.

## Principios
1. Nao repetir cliques de registro de resultado sem confirmar se a operacao foi salva.
2. Nao regenerar chave apos inicio de atividade competitiva. A integridade do backend bloqueia a regeneracao quando ja existem rounds ou resultados.
3. Nao corrigir resultados se partidas dependentes ja tiverem sido disputadas; interromper a sequencia e acionar o suporte tecnico.
4. Nunca alterar o banco diretamente durante a competicao.
5. Se a conexao cair, manter registro em papel com horario, categoria, partida, robos, juiz, rounds e decisao; conciliar uma unica vez apos retorno.

## Situacoes e procedimentos

### Robo nao aparece na chave
- Confirmar competicao e categoria corretas.
- Conferir inscricao APROVADA e ativa, e aptidao da inspecao de Sumô.
- Se a chave ja tiver disputas, nao regenerar: acionar coordenacao tecnica para avaliar a situacao.

### Partida nao esta disponivel / proximo confronto nao aparece
- Conferir se a partida anterior possui resultado consolidado e rounds finalizados.
- Conferir se o outro participante ainda depende de partida da Winners ou Losers.
- BYE nao significa derrota. Nao registrar vencedor manual para um confronto vazio.
- Se continuar inconsistente, suspender essa trilha e registrar ID da partida e screenshot para suporte.

### Resultado incorreto
- Nao iniciar partidas descendentes.
- Registrar partida, resultado esperado, resultado registrado e justificativa.
- Encaminhar ao responsavel com permissao de correcao excepcional; o sistema pode bloquear se ja houver rounds/resultados dependentes.
- Nao apagar dados nem fazer edicao SQL manual.

### Robo desiste ou e desclassificado
- Confirmar status da inscricao e as regras de resolucao administrativa da partida.
- A resolucao automatica de indisponibilidade exige exatamente um dos dois participantes indisponivel.
- Se ambos estiverem indisponiveis, o proprio backend indica necessidade de correcao excepcional: acionar suporte, nao escolher vencedor ficticio.

### Final Geral e reset
- Campeao da Winners chega invicto como participante A da Final Geral.
- Se A vencer, encerra sem reset.
- Se B (oriundo da Losers) vencer, a Final de Reset deve ser disputada.
- Nao publicar campeao definitivo enquanto reset necessario estiver pendente.

### Sistema lento, sem conexao ou com erro de login
- Confirmar conexao e verificar com suporte o estado do backend e do tunnel.
- Nao atualizar repetidamente durante salvamento de rounds ou resultados.
- Registrar ocorrencias em papel e manter um unico operador responsavel pela conciliacao.
- Nunca trocar o banco ou DNS por conta propria: pode haver divergencia de escritas.

### Juiz ausente / agenda atrasada
- Registrar a ocorrencia e informar a coordenacao.
- Verificar no sistema quem pode atribuir/substituir o juiz e reagendar; nao usar conta de outro juiz.
- Registrar qualquer decisao administrativa com identificacao e justificativa.


## Operacao da Agenda — roteiro prioritario para os gestores

A interface **Agenda** consolida chamadas de Follow Line e partidas de Sumô. Nao confundir agendamento com geracao de chave ou com lancamento de resultado.

### Sumô: agendar e convocar partidas
1. Apos gerar a chave, abrir **Agenda**, selecionar a competicao e conferir a categoria.
2. Conferir quais confrontos ja possuem **dois participantes definidos**. Partidas aguardando participantes, BYEs e canceladas nao aparecem na agenda consolidada.
3. Abrir **Agenda** na linha da batalha; informar **data/hora, pista, ordem de execucao e convocacao**. Salvar.
4. Conferir a linha atualizada e o confronto correto; verificar que a edicao **nao alterou a chave**.
5. Quando a partida estiver pronta, marcar **Convocada**; conferir se os robos e o juiz designado estao presentes antes de iniciar o confronto na tela de Sumô.
6. Apos concluir o resultado, conferir o surgimento do confronto sucessor e **agendar a nova partida quando os participantes estiverem definidos**.
7. Se um confronto ainda depender de partidas da Winners ou Losers, nao forcar nomes nem registrar BYE manualmente.
8. Partidas iniciadas, finalizadas ou BYE **nao podem ter a agenda alterada** pelo fluxo comum. A UI bloqueia a edicao e o backend valida essa regra.

**Atencao:** na dupla eliminacao a agenda evolui conforme os resultados. Nao e correto exigir que todas as partidas de Losers e da Final de Reset estejam agendadas no inicio. O reset so sera necessario se o representante da Losers vencer a Final Geral.

### Follow Line: agendar tomada e gerir fila
1. Na **Agenda**, usar **Agendar tomada Follow** e selecionar competicao, categoria e numero da tomada; informar data/hora, pista e ordem.
2. Nao criar uma segunda chamada para a mesma categoria/tomada: o sistema bloqueia duplicatas. Editar a existente.
3. Abrir **Fila** da tomada; conferir inscricoes aprovadas e ativas. Se houve alteracao de elegibilidade, usar **Sincronizar fila** e revisar a lista.
4. Ajustar a ordem de convocacao antes da execucao e registrar as chamadas: **Aguardando → Convocada → Em apresentacao**.
5. A situacao **Em execucao** e **Concluida** decorre do fluxo de tentativas; **Ausente** decorre do registro de ausencia. Nao tentar forcar esses estados pela edicao comum da fila.
6. Inscricoes marcadas **Ausente** ou **Concluida** ficam somente leitura no fluxo comum.
7. Ao concluir a tomada, verificar contagem de concluidos/ausentes, ranking e necessidade de tomada extra conforme regras competitivas.
8. A Tomada Extra nao e livre: depende de autorizacao/regras do sistema. Nao improvisar chamada adicional.

### Agenda vazia ou atividade ausente
- Confirmar a competicao selecionada e os filtros da tela.
- Em Sumô, verificar se a chave e **atual**, se a partida esta ativa e se **ambos os participantes estao definidos**. BYE, cancelada e aguardando participantes sao filtradas da agenda consolidada.
- Em Follow, conferir se a chamada foi criada, esta ativa e pertence a competicao correta.
- Se a agenda estiver lenta, nao duplicar cadastros; atualizar uma vez e conferir o resultado antes de repetir.

### Reagendamento e conflitos de horario
- Antes de alterar, confirmar com a coordenacao **pista, juiz, ordem e horario**; verificar se o mesmo robo ou juiz ja esta escalado em outra atividade.
- A listagem da agenda ordena por data/hora e ordem de execucao; **isso nao garante ausencia de conflitos de recursos**. A validacao manual de conflitos e obrigatoria.
- Em caso de atraso, comunicar a alteracao e registrar quem a autorizou.
- Nunca reagendar partida iniciada ou finalizada. Se houve erro, interromper e acionar suporte.

### Conferencia de abertura e fechamento de turno
- **Antes:** revisar chamadas do turno, juiz, pista, robos, sequencia de convocacao e conectividade.
- **Durante:** manter uma unica pessoa responsavel por alterar cada chamada; apos salvar, conferir na lista.
- **Depois:** comparar agenda, resultados, ausencias e fichas de contingencia; nenhuma partida pode ficar com resultado em papel sem conciliacao no sistema.

## Ficha de contingencia em papel
Competicao: ____ Categoria: ____ Partida ID: ____ Data/hora: ____
Robo A: ____ Robo B: ____ Juiz: ____
Rounds / placares: ____ Vencedor: ____ Motivo/ocorrencia: ____
Responsavel pelo registro: ____ Confirmado no sistema por: ____ Horario: ____

## Escalonamento
Gestor registra evidencia -> coordenacao confirma o impacto -> suporte tecnico avalia logs e dependencias -> responsavel autorizado executa recuperacao -> gestor confere resultados e publico.

**Antes do evento:** preencher nomes e contatos dos responsaveis, imprimir fichas e validar cada procedimento durante a homologacao.
