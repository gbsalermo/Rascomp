package br.edu.ufrb.rascomp.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.edu.ufrb.rascomp.dto.CompetitionDTO;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.service.CompetitionService;

@SpringBootTest
@ActiveProfiles("flowtest")
class CompetitionLifecycleFlowTest extends IntegrationFlowTestSupport {

    @Autowired private CompetitionService competitionService;

    @Test
    void devePercorrerCicloNormalCompletoDaCompeticao() {
        CompetitionDTO criada = competitionService.criar(novaCompeticaoDto());
        assertEquals(StatusCompetition.PLANEJADA, criada.getStatus());

        organizacaoAutenticada();
        try {
            CompetitionDTO dto = competitionService.alterarStatus(
                    criada.getId(), StatusCompetition.INSCRICOES_ABERTAS);
            assertEquals(StatusCompetition.INSCRICOES_ABERTAS, dto.getStatus());

            dto = competitionService.alterarStatus(
                    criada.getId(), StatusCompetition.INSCRICOES_ENCERRADAS);
            assertEquals(StatusCompetition.INSCRICOES_ENCERRADAS, dto.getStatus());

            dto = competitionService.alterarStatus(
                    criada.getId(), StatusCompetition.EM_ANDAMENTO);
            assertEquals(StatusCompetition.EM_ANDAMENTO, dto.getStatus());

            dto = competitionService.alterarStatus(
                    criada.getId(), StatusCompetition.FINALIZADA);

            assertEquals(StatusCompetition.FINALIZADA, dto.getStatus());
            assertEquals(StatusCompetition.FINALIZADA,
                    competitionRepository.findById(criada.getId()).orElseThrow().getStatus());
        } finally {
            limparAutenticacao();
        }
    }

    @Test
    void transicaoInvalidaNaoPodeAlterarEstadoPersistido() {
        CompetitionDTO criada = competitionService.criar(novaCompeticaoDto());
        CompetitionDTO dto = competitionService.buscarPorId(criada.getId());
        dto.setStatus(StatusCompetition.EM_ANDAMENTO);

        assertThrows(IllegalArgumentException.class,
                () -> competitionService.atualizar(criada.getId(), dto));

        assertEquals(StatusCompetition.PLANEJADA,
                competitionRepository.findById(criada.getId()).orElseThrow().getStatus());
    }

    private CompetitionDTO novaCompeticaoDto() {
        LocalDate hoje = LocalDate.now();
        CompetitionDTO dto = new CompetitionDTO();
        dto.setNome(unique("Competicao Lifecycle"));
        dto.setDescricao("Fluxo integrado do ciclo de Competition.");
        dto.setInicioInscricoes(hoje.minusDays(1));
        dto.setFimInscricoes(hoje.plusDays(1));
        dto.setDataInicio(hoje.plusDays(2));
        dto.setDataFim(hoje.plusDays(3));
        dto.setStatus(StatusCompetition.PLANEJADA);
        dto.setAtivo(true);
        return dto;
    }
}
