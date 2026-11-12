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

## Ficha de contingencia em papel
Competicao: ____ Categoria: ____ Partida ID: ____ Data/hora: ____
Robo A: ____ Robo B: ____ Juiz: ____
Rounds / placares: ____ Vencedor: ____ Motivo/ocorrencia: ____
Responsavel pelo registro: ____ Confirmado no sistema por: ____ Horario: ____

## Escalonamento
Gestor registra evidencia -> coordenacao confirma o impacto -> suporte tecnico avalia logs e dependencias -> responsavel autorizado executa recuperacao -> gestor confere resultados e publico.

**Antes do evento:** preencher nomes e contatos dos responsaveis, imprimir fichas e validar cada procedimento durante a homologacao.
