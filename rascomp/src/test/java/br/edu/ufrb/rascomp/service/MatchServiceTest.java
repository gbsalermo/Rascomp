package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.MatchAgendaDTO;
import br.edu.ufrb.rascomp.dto.MatchDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusBracket;
import br.edu.ufrb.rascomp.model.Enum.StatusConvocacaoPartida;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private BracketRepository bracketRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private InspecaoSumoService inspecaoSumoService;

    @InjectMocks
    private MatchService service;

    @Test
    void deveRejeitarPartidaEmBracketFollowLine() {
        CompetitionCategory follow = CompetitionCategory.builder()
                .id(3L)
                .modalidade(Modalidade.FOLLOW_LINE)
                .ativo(true)
                .build();

        Bracket bracket = new Bracket();
        bracket.setId(10L);
        bracket.setCategory(follow);
        bracket.setAtivo(true);
        bracket.setAtual(true);
        bracket.setStatus(StatusBracket.RASCUNHO);

        when(bracketRepository.findById(10L)).thenReturn(Optional.of(bracket));

        MatchDTO dto = new MatchDTO();
        dto.setBracketId(10L);
        dto.setRodada(1);
        dto.setOrdem(1);
        dto.setRegistrationAId(1L);

        assertThrows(IllegalArgumentException.class, () -> service.criar(dto));
        verifyNoInteractions(matchRepository, registrationRepository, inspecaoSumoService);
    }

    @Test
    void deveRejeitarParticipanteSumoNaoApto() {
        Competition competition = new Competition();
        competition.setId(1L);

        CompetitionCategory sumo = CompetitionCategory.builder()
                .id(1L)
                .modalidade(Modalidade.SUMO)
                .ativo(true)
                .build();

        Bracket bracket = new Bracket();
        bracket.setId(10L);
        bracket.setCompetition(competition);
        bracket.setCategory(sumo);
        bracket.setAtivo(true);
        bracket.setAtual(true);
        bracket.setStatus(StatusBracket.RASCUNHO);

        Team team = new Team();
        team.setId(1L);

        Robot robot = new Robot();
        robot.setId(1L);
        robot.setTeam(team);

        Registration registration = new Registration();
        registration.setId(1L);
        registration.setCompetition(competition);
        registration.setCategory(sumo);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setAtivo(true);
        registration.setStatus(StatusRegistration.APROVADA);

        when(bracketRepository.findById(10L)).thenReturn(Optional.of(bracket));
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(inspecaoSumoService.estaAptaParaCompetir(1L)).thenReturn(false);

        MatchDTO dto = new MatchDTO();
        dto.setBracketId(10L);
        dto.setRodada(1);
        dto.setOrdem(1);
        dto.setRegistrationAId(1L);

        assertThrows(IllegalArgumentException.class, () -> service.criar(dto));
        verifyNoInteractions(matchRepository);
    }

    @Test
    void deveAtualizarAgendaSemAlterarEstruturaDaChave() {
        Competition competition = new Competition();
        competition.setId(1L);
        competition.setNome("RRC 2026");

        CompetitionCategory sumo = CompetitionCategory.builder()
                .id(2L)
                .nome("Mini Sumô")
                .modalidade(Modalidade.SUMO)
                .ativo(true)
                .build();

        Bracket bracket = new Bracket();
        bracket.setId(10L);
        bracket.setNome("Chave principal");
        bracket.setCompetition(competition);
        bracket.setCategory(sumo);
        bracket.setAtivo(true);
        bracket.setAtual(true);
        bracket.setStatus(StatusBracket.GERADO);

        Registration a = registration(101L);
        Registration b = registration(102L);

        Match match = new Match();
        match.setId(20L);
        match.setBracket(bracket);
        match.setRodada(2);
        match.setOrdem(1);
        match.setRegistrationA(a);
        match.setRegistrationB(b);
        match.setStatus(StatusMatch.AGENDADA);
        match.setAtivo(true);

        when(matchRepository.findById(20L)).thenReturn(Optional.of(match));
        when(matchRepository.save(match)).thenReturn(match);

        MatchAgendaDTO agenda = new MatchAgendaDTO();
        LocalDateTime horario = LocalDateTime.of(2026, 10, 10, 14, 30);
        agenda.setDataHora(horario);
        agenda.setPista(" Arena A ");
        agenda.setOrdemExecucao(7);
        agenda.setStatusConvocacao(StatusConvocacaoPartida.CONVOCADA);

        MatchDTO atualizado = service.atualizarAgenda(20L, agenda);

        assertEquals(2, atualizado.getRodada());
        assertEquals(1, atualizado.getOrdem());
        assertSame(a, match.getRegistrationA());
        assertSame(b, match.getRegistrationB());
        assertEquals(horario, atualizado.getDataHora());
        assertEquals("Arena A", atualizado.getPista());
        assertEquals(7, atualizado.getOrdemExecucao());
        assertEquals(StatusConvocacaoPartida.CONVOCADA, atualizado.getStatusConvocacao());
        verify(matchRepository).save(match);
    }

    @Test
    void deveBloquearAgendaDepoisQuePartidaComecou() {
        CompetitionCategory sumo = CompetitionCategory.builder()
                .id(2L)
                .modalidade(Modalidade.SUMO)
                .build();
        Bracket bracket = new Bracket();
        bracket.setCategory(sumo);
        bracket.setAtivo(true);
        bracket.setAtual(true);

        Match match = new Match();
        match.setId(20L);
        match.setBracket(bracket);
        match.setAtivo(true);
        match.setStatus(StatusMatch.EM_ANDAMENTO);
        when(matchRepository.findById(20L)).thenReturn(Optional.of(match));

        assertThrows(IllegalArgumentException.class,
                () -> service.atualizarAgenda(20L, new MatchAgendaDTO()));
    }

    @Test
    void deveBloquearEdicaoEstruturalDepoisDaGeracao() {
        CompetitionCategory sumo = CompetitionCategory.builder()
                .id(2L)
                .modalidade(Modalidade.SUMO)
                .build();
        Bracket bracket = new Bracket();
        bracket.setId(10L);
        bracket.setCategory(sumo);
        bracket.setAtivo(true);
        bracket.setAtual(true);
        bracket.setStatus(StatusBracket.GERADO);
        when(bracketRepository.findById(10L)).thenReturn(Optional.of(bracket));

        MatchDTO dto = new MatchDTO();
        dto.setBracketId(10L);
        dto.setRodada(1);
        dto.setOrdem(1);
        dto.setRegistrationAId(1L);

        assertThrows(IllegalArgumentException.class, () -> service.criar(dto));
        verifyNoInteractions(registrationRepository, matchRepository, inspecaoSumoService);
    }

    private Registration registration(Long id) {
        Team team = new Team();
        team.setId(id + 1000);
        team.setNome("Equipe " + id);

        Robot robot = new Robot();
        robot.setId(id + 2000);
        robot.setNome("Robô " + id);
        robot.setTeam(team);

        Registration registration = new Registration();
        registration.setId(id);
        registration.setTeam(team);
        registration.setRobot(robot);
        return registration;
    }
}
