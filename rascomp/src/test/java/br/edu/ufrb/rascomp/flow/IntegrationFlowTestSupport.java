package br.edu.ufrb.rascomp.flow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.ConfigSumo;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.SumoControlMode;
import br.edu.ufrb.rascomp.model.Enum.SumoPhysicalClass;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.ConfigSumoRepository;
import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;

abstract class IntegrationFlowTestSupport {

    @Autowired protected CompetitionRepository competitionRepository;
    @Autowired protected CompetitionCategoryRepository categoryRepository;
    @Autowired protected InstitutionRepository institutionRepository;
    @Autowired protected TeamRepository teamRepository;
    @Autowired protected RobotRepository robotRepository;
    @Autowired protected RegistrationRepository registrationRepository;
    @Autowired protected UserAccountRepository userAccountRepository;
    @Autowired protected ConfigFollowRepository configFollowRepository;
    @Autowired protected ConfigSumoRepository configSumoRepository;
    @Autowired protected BracketRepository bracketRepository;
    @Autowired protected MatchRepository matchRepository;
    @Autowired protected MatchResultRepository matchResultRepository;
    @Autowired protected RoundSumoRepository roundSumoRepository;
    @Autowired protected TentativaSeguidorLinhaRepository tentativaRepository;

    protected String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected UserAccount organizacaoAutenticada() {
        UserAccount user = new UserAccount();
        user.setNome(unique("Organizacao"));
        user.setEmail(unique("org").toLowerCase() + "@flow.local");
        user.setPasswordHash("not-used-in-flow-test");
        user.setRole(UserRole.ORGANIZACAO);
        user.setAtivo(true);
        user = userAccountRepository.save(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));
        return user;
    }

    protected void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    protected Competition competition(StatusCompetition status) {
        LocalDate hoje = LocalDate.now();
        Competition c = new Competition();
        c.setNome(unique("RRC Flow"));
        c.setDescricao("Competicao criada por teste integrado.");
        c.setInicioInscricoes(hoje.minusDays(2));
        c.setFimInscricoes(hoje.plusDays(1));
        c.setDataInicio(hoje.plusDays(2));
        c.setDataFim(hoje.plusDays(3));
        c.setStatus(status);
        c.setAtivo(true);
        return competitionRepository.save(c);
    }

    protected CompetitionCategory followCategory() {
        CompetitionCategory category = CompetitionCategory.builder()
                .nome(unique("Follow"))
                .descricao("Categoria Follow de teste integrado.")
                .modalidade(Modalidade.FOLLOW_LINE)
                .ativo(true)
                .build();
        category = categoryRepository.save(category);

        ConfigFollow config = ConfigFollow.builder()
                .competitionCategory(category)
                .numeroTomadas(3)
                .tentativasPorTomada(3)
                .maxTempoSegundos(120)
                .numeroCheckpoints(5)
                .penalidadePadraoSegundos(10)
                .tempoApresentacaoSegundos(60)
                .build();
        configFollowRepository.save(config);
        return category;
    }

    protected CompetitionCategory sumoCategory() {
        CompetitionCategory category = CompetitionCategory.builder()
                .nome(unique("Mini Sumo"))
                .descricao("Categoria Sumo de teste integrado.")
                .modalidade(Modalidade.SUMO)
                .sumoPhysicalClass(SumoPhysicalClass.MINI_500G)
                .sumoControlMode(SumoControlMode.RC)
                .ativo(true)
                .build();
        category = categoryRepository.save(category);

        ConfigSumo config = ConfigSumo.builder()
                .competitionCategory(category)
                .pesoMax(new BigDecimal("0.500"))
                .exigeInspecao(true)
                .maxTentativasInspecao(2)
                .numeroRounds(3)
                .roundsParaVencer(2)
                .permiteRoundDesempate(true)
                .maxRoundsExtras(2)
                .build();
        configSumoRepository.save(config);
        return category;
    }

    protected Team team() {
        Institution institution = Institution.builder()
                .nome(unique("Instituicao"))
                .sigla(unique("IF").substring(0, Math.min(20, unique("IF").length())))
                .cidade("Cruz das Almas")
                .estado("BA")
                .ativo(true)
                .build();
        institution = institutionRepository.save(institution);

        Team team = new Team();
        team.setNome(unique("Equipe"));
        team.setInstitution(institution);
        team.setAtivo(true);
        return teamRepository.save(team);
    }

    protected Robot robot(Team team) {
        Robot robot = new Robot();
        robot.setNome(unique("Robo"));
        robot.setDescricao("Robo de teste integrado.");
        robot.setTeam(team);
        robot.setAtivo(true);
        return robotRepository.save(robot);
    }

    protected Registration approvedRegistration(
            Competition competition,
            CompetitionCategory category,
            Team team,
            Robot robot) {

        Registration registration = new Registration();
        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setCompetitors(new LinkedHashSet<>());
        registration.setStatus(StatusRegistration.APROVADA);
        registration.setAtivo(true);
        return registrationRepository.save(registration);
    }
}
