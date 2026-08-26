package br.edu.ufrb.rascomp.teste;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.InspecaoSumoDTO;
import br.edu.ufrb.rascomp.dto.RoundSumoDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.MotivoResultadoRoundSumo;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.StatusRoundSumo;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import br.edu.ufrb.rascomp.service.BracketGenerationService;
import br.edu.ufrb.rascomp.service.InspecaoSumoService;
import br.edu.ufrb.rascomp.service.RoundSumoService;
import lombok.RequiredArgsConstructor;

/**
 * Complementa o profile testdata depois que o cenário principal termina de subir.
 *
 * Objetivo da demonstração:
 * - manter a categoria Mini Sumô da competição ativa com 16 robôs;
 * - gerar oito confrontos de oitavas de final;
 * - deixar duas oitavas resolvidas para formar uma quarta de final;
 * - preservar as demais oitavas abertas para operações ao vivo.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rascomp.test-data.demo-showcase-enabled", havingValue = "true")
public class DemoOitavasDataInitializer {

    private static final String LIVE_COMPETITION = "RRC 2026 · Demonstração ao vivo";
    private static final String MINI_CATEGORY = "DEMO · Mini Sumô RC";
    private static final String ORGANIZATION_EMAIL = "organizacao.demo@rascomp.local";
    private static final String VISITOR_INSTITUTION = "ROBODEMO";
    private static final String TITAN = "Titan Demo";

    private final CompetitionRepository competitionRepository;
    private final CompetitionCategoryRepository categoryRepository;
    private final InstitutionRepository institutionRepository;
    private final TeamRepository teamRepository;
    private final RobotRepository robotRepository;
    private final RegistrationRepository registrationRepository;
    private final UserAccountRepository userAccountRepository;
    private final BracketRepository bracketRepository;
    private final MatchRepository matchRepository;
    private final RoundSumoRepository roundRepository;
    private final InspecaoSumoService inspecaoSumoService;
    private final BracketGenerationService bracketGenerationService;
    private final RoundSumoService roundSumoService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void prepararOitavasAoVivo() {
        Competition competition = competitionRepository.findAll().stream()
                .filter(item -> LIVE_COMPETITION.equals(item.getNome()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Competição de demonstração ao vivo não encontrada."));

        CompetitionCategory category = categoryRepository.findAll().stream()
                .filter(item -> MINI_CATEGORY.equals(item.getNome()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Categoria Mini Sumô de demonstração não encontrada."));

        Institution institution = institutionRepository.findAll().stream()
                .filter(item -> VISITOR_INSTITUTION.equalsIgnoreCase(item.getSigla()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Instituição de demonstração não encontrada."));

        UserAccount organizacao = userAccountRepository.findByEmailIgnoreCase(ORGANIZATION_EMAIL)
                .orElseThrow(() -> new IllegalStateException("Usuário de organização da demonstração não encontrado."));

        // O cenário principal já possui Titan + MiniBot 1..7 = 8 inscrições.
        // Acrescentamos MiniBot 8..15 para fechar 16 participantes sem BYE.
        for (int i = 8; i <= 15; i++) {
            Team team = garantirEquipe(i, institution);
            Robot robot = garantirRobo(i, team);
            Registration registration = garantirInscricao(competition, category, team, robot, organizacao);
            garantirInspecao(registration);
        }

        Bracket bracket = chaveAtualComOitavas(competition.getId(), category.getId());
        if (bracket == null) {
            bracketGenerationService.gerar(competition.getId(), category.getId());
            bracket = chaveAtualComOitavas(competition.getId(), category.getId());
        }

        if (bracket == null) {
            throw new IllegalStateException("Não foi possível gerar a chave de oitavas da demonstração.");
        }

        prepararQuartaDeFinal(bracket);

        long oitavas = partidasDaRodada(bracket, 1).size();
        long quartasAgendadas = partidasDaRodada(bracket, 2).stream()
                .filter(match -> match.getRegistrationA() != null && match.getRegistrationB() != null)
                .count();

        System.out.println("RASCOMP · DEMO SUMÔ: chave ao vivo com " + oitavas
                + " confrontos de oitavas; quartas já formadas: " + quartasAgendadas + ".");
    }

    private Team garantirEquipe(int indice, Institution institution) {
        String nome = "Equipe Mini Demo " + indice;
        Team team = teamRepository.findAll().stream()
                .filter(item -> nome.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .orElseGet(Team::new);
        team.setNome(nome);
        team.setInstitution(institution);
        team.setResponsibleUser(null);
        team.setAtivo(true);
        return teamRepository.save(team);
    }

    private Robot garantirRobo(int indice, Team team) {
        String nome = "MiniBot Demo " + indice;
        Robot robot = robotRepository.findAll().stream()
                .filter(item -> nome.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .orElseGet(Robot::new);
        robot.setNome(nome);
        robot.setDescricao("Mini Sumô de demonstração para completar as oitavas de final.");
        robot.setTeam(team);
        robot.setAtivo(true);
        return robotRepository.save(robot);
    }

    private Registration garantirInscricao(
            Competition competition,
            CompetitionCategory category,
            Team team,
            Robot robot,
            UserAccount organizacao) {
        Registration registration = registrationRepository.findAll().stream()
                .filter(item -> item.getCompetition().getId().equals(competition.getId()))
                .filter(item -> item.getCategory().getId().equals(category.getId()))
                .filter(item -> item.getRobot().getId().equals(robot.getId()))
                .findFirst()
                .orElseGet(Registration::new);

        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setCompetitors(new LinkedHashSet<>());
        registration.setStatus(StatusRegistration.APROVADA);
        registration.setObservacao("Inscrição automática para completar a chave de oitavas da demonstração.");
        registration.setRequestedByUser(null);
        registration.setReviewedByUser(organizacao);
        if (registration.getReviewedAt() == null) {
            registration.setReviewedAt(LocalDateTime.now().minusHours(4));
        }
        registration.setAtivo(true);
        return registrationRepository.save(registration);
    }

    private void garantirInspecao(Registration registration) {
        if (inspecaoSumoService.estaAptaParaCompetir(registration.getId())) {
            return;
        }

        InspecaoSumoDTO dto = new InspecaoSumoDTO();
        dto.setRegistrationId(registration.getId());
        dto.setPesoMedido(new BigDecimal("0.480"));
        dto.setObservacao("Inspeção aprovada automaticamente para a demonstração de oitavas.");
        inspecaoSumoService.registrar(dto);
    }

    private Bracket chaveAtualComOitavas(Long competitionId, Long categoryId) {
        return bracketRepository.findByCompetitionIdAndCategoryIdAndAtualTrue(competitionId, categoryId).stream()
                .filter(bracket -> partidasDaRodada(bracket, 1).size() == 8)
                .findFirst()
                .orElse(null);
    }

    private List<Match> partidasDaRodada(Bracket bracket, int rodada) {
        return matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracket.getId()).stream()
                .filter(match -> match.getRodada() == rodada)
                .toList();
    }

    private void prepararQuartaDeFinal(Bracket bracket) {
        Registration titan = registrationRepository.findAll().stream()
                .filter(item -> item.getCompetition().getId().equals(bracket.getCompetition().getId()))
                .filter(item -> item.getCategory().getId().equals(bracket.getCategory().getId()))
                .filter(item -> TITAN.equals(item.getRobot().getNome()))
                .findFirst()
                .orElse(null);

        if (titan == null) {
            return;
        }

        List<Match> oitavas = partidasDaRodada(bracket, 1);
        Match titanMatch = oitavas.stream()
                .filter(match -> titan.getId().equals(id(match.getRegistrationA()))
                        || titan.getId().equals(id(match.getRegistrationB())))
                .findFirst()
                .orElse(null);

        if (titanMatch == null) {
            return;
        }

        finalizarSeAberta(titanMatch, titan, "Titan classificado para as quartas na demonstração.");

        int ordemIrma = titanMatch.getOrdem() % 2 == 0
                ? titanMatch.getOrdem() - 1
                : titanMatch.getOrdem() + 1;

        Match partidaIrma = oitavas.stream()
                .filter(match -> match.getOrdem() == ordemIrma)
                .findFirst()
                .orElse(null);

        if (partidaIrma != null && partidaIrma.getRegistrationA() != null) {
            finalizarSeAberta(
                    partidaIrma,
                    partidaIrma.getRegistrationA(),
                    "Oitava resolvida para formar a quarta de final da demonstração.");
        }
    }

    private void finalizarSeAberta(Match match, Registration winner, String observacao) {
        Match atual = matchRepository.findById(match.getId()).orElseThrow();
        if (roundRepository.countByMatchId(atual.getId()) > 0) {
            return;
        }

        registrarVitoria(atual, winner, observacao + " Round 1.");
        registrarVitoria(atual, winner, observacao + " Round 2.");
    }

    private void registrarVitoria(Match match, Registration winner, String observacao) {
        RoundSumoDTO dto = new RoundSumoDTO();
        dto.setMatchId(match.getId());
        dto.setWinnerRegistrationId(winner.getId());
        dto.setStatus(StatusRoundSumo.FINALIZADO);
        dto.setMotivoResultado(MotivoResultadoRoundSumo.DISPUTA);
        dto.setPenalidadesA(0);
        dto.setPenalidadesB(0);
        dto.setObservacao(observacao);
        roundSumoService.registrar(dto);
    }

    private Long id(Registration registration) {
        return registration == null ? null : registration.getId();
    }
}
