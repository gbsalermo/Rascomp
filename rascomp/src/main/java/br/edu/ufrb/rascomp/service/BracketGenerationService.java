package br.edu.ufrb.rascomp.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.BracketDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusBracket;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.TipoPartidaSumo;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BracketGenerationService {

    private final BracketRepository bracketRepository;
    private final MatchRepository matchRepository;
    private final RegistrationRepository registrationRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionCategoryRepository categoryRepository;
    private final BracketProgressionService bracketProgressionService;
    private final BracketIntegrityService bracketIntegrityService;
    private final InspecaoSumoService inspecaoSumoService;
    private final CompetitionContextService competitionContextService;
    private final UserAccountService userAccountService;

    @Transactional
    public BracketDTO gerar(Long competitionId, Long categoryId) {
        return gerar(competitionId, categoryId, "SINGLE_ELIMINATION");
    }

    @Transactional
    public BracketDTO gerar(Long competitionId, Long categoryId, String format) {
        competitionContextService.exigirOperavel(competitionId);
        Competition competition = buscarCompetitionParaAtualizacao(competitionId);
        CompetitionCategory category = buscarCategory(categoryId);

        validarAtivos(competition, category);
        validarCategoriaSumo(category);
        bracketIntegrityService.validarEstadoParaGeracao(competition);

        validarFormato(format);
        return gerarInterno(competition, category, null, null, format);
    }

    @Transactional
    public BracketDTO regenerarExcepcionalDev(
            Long competitionId,
            Long categoryId,
            String justificativa) {

        UserAccount dev = userAccountService.buscarAtual();
        if (dev.getRole() != UserRole.DEV) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "A regeneração excepcional de chave é exclusiva do DEV.");
        }

        competitionContextService.exigirOperavel(competitionId);
        Competition competition = buscarCompetitionParaAtualizacao(competitionId);
        CompetitionCategory category = buscarCategory(categoryId);

        validarAtivos(competition, category);
        validarCategoriaSumo(category);

        if (competition.getStatus() != StatusCompetition.INSCRICOES_ENCERRADAS
                && competition.getStatus() != StatusCompetition.EM_ANDAMENTO) {
            throw new IllegalArgumentException(
                    "A regeneração excepcional só é permitida com inscrições encerradas ou competição EM_ANDAMENTO.");
        }

        String motivo = justificativa == null ? "" : justificativa.trim();
        if (motivo.isBlank()) {
            throw new IllegalArgumentException("Informe a justificativa da regeneração excepcional.");
        }
        if (motivo.length() > 500) {
            motivo = motivo.substring(0, 500);
        }

        List<Bracket> vigentes = bracketRepository.findByCompetitionIdAndCategoryIdAndAtualTrue(competitionId, categoryId);
        String format = vigentes.isEmpty() ? "SINGLE_ELIMINATION" : vigentes.get(0).getBracketFormat();
        return gerarInterno(competition, category, dev, motivo, format);
    }

    private BracketDTO gerarInterno(
            Competition competition,
            CompetitionCategory category,
            UserAccount generatedBy,
            String generationReason,
            String format) {

        Long competitionId = competition.getId();
        Long categoryId = category.getId();

        List<Bracket> chavesAtuais = bracketRepository
                .findByCompetitionIdAndCategoryIdAndAtualTrue(competitionId, categoryId);

        // Mesmo no fluxo excepcional, nunca apagamos/reescrevemos uma chave que já teve disputa real.
        bracketIntegrityService.validarRegeneracaoPermitida(chavesAtuais);

        List<Registration> participantes = buscarParticipantesElegiveis(competitionId, categoryId);
        if (participantes.size() < 2) {
            throw new IllegalArgumentException(
                    "São necessárias pelo menos duas inscrições de Sumô ativas, aprovadas e aptas para gerar o chaveamento.");
        }

        marcarChavesAtuaisComoHistoricas(chavesAtuais);

        List<Registration> participantesSorteados = new ArrayList<>(participantes);
        Collections.shuffle(participantesSorteados);

        Bracket bracket = criarBracket(competition, category);
        bracket.setGeneratedByUser(generatedBy);
        bracket.setGenerationReason(generationReason);
        bracket.setBracketFormat(format);

        List<Match> primeiraRodada = "DOUBLE_ELIMINATION".equals(format)
                ? gerarArvoreDupla(bracket, participantesSorteados)
                : gerarArvoreCompleta(bracket, participantesSorteados);

        bracket.setStatus(StatusBracket.GERADO);
        Bracket salvo = bracketRepository.save(bracket);

        primeiraRodada.stream()
                .filter(match -> match.getStatus() == StatusMatch.BYE)
                .forEach(bracketProgressionService::avancarBye);
        if ("DOUBLE_ELIMINATION".equals(format)) {
            bracketProgressionService.resolverAvancosAutomaticos(salvo.getId());
        }

        return new BracketDTO(salvo);
    }

    private List<Registration> buscarParticipantesElegiveis(Long competitionId, Long categoryId) {
        return registrationRepository
                .findByCompetitionIdAndCategoryIdAndStatusAndAtivoTrueOrderByIdAsc(
                        competitionId,
                        categoryId,
                        StatusRegistration.APROVADA)
                .stream()
                .filter(registration -> inspecaoSumoService.estaAptaParaCompetir(registration.getId()))
                .toList();
    }

    private void marcarChavesAtuaisComoHistoricas(List<Bracket> atuais) {
        atuais.forEach(item -> {
            item.setAtual(false);
            if (item.getStatus() != StatusBracket.FINALIZADO) {
                item.setStatus(StatusBracket.CANCELADO);
            }
        });
        if (!atuais.isEmpty()) bracketRepository.saveAll(atuais);
    }

    private Bracket criarBracket(Competition competition, CompetitionCategory category) {
        Bracket bracket = new Bracket();
        bracket.setCompetition(competition);
        bracket.setCategory(category);
        bracket.setNome("Chaveamento - " + competition.getNome() + " - " + category.getNome());
        bracket.setStatus(StatusBracket.RASCUNHO);
        bracket.setAtivo(true);
        bracket.setAtual(true);
        return bracketRepository.save(bracket);
    }


    private void validarFormato(String format) {
        if (!"SINGLE_ELIMINATION".equals(format) && !"DOUBLE_ELIMINATION".equals(format)) {
            throw new IllegalArgumentException("Formato de chaveamento inválido.");
        }
    }

    /**
     * Cria Winners e Losers dentro do MESMO bracket. Os números de rodada são
     * globais para respeitar a chave única (bracket_id, rodada, ordem).
     * O avanço depende exclusivamente das referências, nunca da aritmética da rodada.
     */
    private List<Match> gerarArvoreDupla(Bracket bracket, List<Registration> participantes) {
        int size = proximaPotenciaDeDois(participantes.size());
        int k = calcularTotalRodadas(size);
        Map<String, Match> winners = new HashMap<>();
        Map<String, Match> losers = new HashMap<>();
        List<Match> all = new ArrayList<>();
        List<Match> first = criarPrimeiraRodada(bracket, participantes, size);
        for (Match m : first) winners.put(key(1, m.getOrdem()), m);
        all.addAll(first);

        for (int r = 2; r <= k; r++) {
            int count = size >> r;
            for (int j = 1; j <= count; j++) {
                Match m = novaPartida(bracket, r, j, "WINNERS");
                winners.put(key(r, j), m);
                all.add(m);
            }
        }

        for (int s = 1; s <= 2 * (k - 1); s++) {
            int phase = (s + 1) / 2;
            int count = size >> (phase + 1);
            for (int j = 1; j <= count; j++) {
                Match m = novaPartida(bracket, k + s, j, "LOSERS");
                losers.put(key(s, j), m);
                all.add(m);
            }
        }

        Match grand = novaPartida(bracket, 3 * k - 1, 1, "GRAND_FINAL");
        Match reset = novaPartida(bracket, 3 * k, 1, "GRAND_FINAL_RESET");
        all.add(grand);
        all.add(reset);

        // Primeiro persiste os nós, depois as arestas (FKs self-referential).
        matchRepository.saveAll(all);

        for (int r = 1; r <= k; r++) {
            int count = size >> r;
            for (int j = 1; j <= count; j++) {
                Match source = winners.get(key(r, j));
                Match winnerTarget = r == k ? grand : winners.get(key(r + 1, (j + 1) / 2));
                String winnerSlot = r == k ? "A" : slot(j);
                Match loserTarget;
                String loserSlot;
                if (k == 1) {
                    loserTarget = grand;
                    loserSlot = "B";
                } else if (r == 1) {
                    loserTarget = losers.get(key(1, (j + 1) / 2));
                    loserSlot = slot(j);
                } else {
                    loserTarget = losers.get(key(2 * r - 2, j));
                    loserSlot = "B";
                }
                ligar(source, winnerTarget, winnerSlot, loserTarget, loserSlot);
            }
        }

        for (int s = 1; s <= 2 * (k - 1); s++) {
            int phase = (s + 1) / 2;
            int count = size >> (phase + 1);
            for (int j = 1; j <= count; j++) {
                Match source = losers.get(key(s, j));
                Match target = s == 2 * (k - 1)
                        ? grand
                        : losers.get(key(s + 1, s % 2 == 1 ? j : (j + 1) / 2));
                String targetSlot = s == 2 * (k - 1) ? "B" : (s % 2 == 1 ? "A" : slot(j));
                ligar(source, target, targetSlot, null, null);
            }
        }

        ligar(grand, reset, "A", reset, "B");
        matchRepository.saveAll(all);
        return first;
    }

    private String key(int r, int j) { return r + ":" + j; }
    private String slot(int j) { return j % 2 == 1 ? "A" : "B"; }

    private Match novaPartida(Bracket bracket, int round, int order, String section) {
        Match m = new Match();
        m.setBracket(bracket);
        m.setRodada(round);
        m.setOrdem(order);
        m.setBracketSection(section);
        m.setTipoPartida(TipoPartidaSumo.ELIMINATORIA);
        m.setStatus(StatusMatch.AGUARDANDO_PARTICIPANTES);
        m.setAtivo(true);
        return m;
    }

    private void ligar(Match origin, Match winner, String winnerSlot, Match loser, String loserSlot) {
        origin.setWinnerNextMatch(winner);
        origin.setWinnerNextSlot(winnerSlot);
        origin.setLoserNextMatch(loser);
        origin.setLoserNextSlot(loserSlot);
    }

    private List<Match> gerarArvoreCompleta(Bracket bracket, List<Registration> participantes) {
        int tamanhoChave = proximaPotenciaDeDois(participantes.size());
        int totalRodadas = calcularTotalRodadas(tamanhoChave);

        List<Match> partidas = new ArrayList<>();
        List<Match> primeiraRodada = criarPrimeiraRodada(bracket, participantes, tamanhoChave);
        partidas.addAll(primeiraRodada);

        for (int rodada = 2; rodada <= totalRodadas; rodada++) {
            int quantidadePartidas = tamanhoChave / (int) Math.pow(2, rodada);

            for (int ordem = 1; ordem <= quantidadePartidas; ordem++) {
                Match match = new Match();
                match.setBracket(bracket);
                match.setRodada(rodada);
                match.setOrdem(ordem);
                match.setTipoPartida(TipoPartidaSumo.ELIMINATORIA);
                match.setRegistrationA(null);
                match.setRegistrationB(null);
                match.setStatus(StatusMatch.AGUARDANDO_PARTICIPANTES);
                match.setAtivo(true);
                partidas.add(match);
            }
        }

        if (participantes.size() >= 4 && totalRodadas >= 2) {
            Match terceiroLugar = new Match();
            terceiroLugar.setBracket(bracket);
            terceiroLugar.setRodada(totalRodadas);
            terceiroLugar.setOrdem(2);
            terceiroLugar.setTipoPartida(TipoPartidaSumo.TERCEIRO_LUGAR);
            terceiroLugar.setRegistrationA(null);
            terceiroLugar.setRegistrationB(null);
            terceiroLugar.setStatus(StatusMatch.AGUARDANDO_PARTICIPANTES);
            terceiroLugar.setAtivo(true);
            partidas.add(terceiroLugar);
        }

        matchRepository.saveAll(partidas);
        return primeiraRodada;
    }

    private List<Match> criarPrimeiraRodada(
            Bracket bracket,
            List<Registration> participantes,
            int tamanhoChave) {

        int quantidadePartidas = tamanhoChave / 2;
        int quantidadeByes = tamanhoChave - participantes.size();
        List<Match> partidas = new ArrayList<>();
        int indiceParticipante = 0;

        for (int ordem = 1; ordem <= quantidadePartidas; ordem++) {
            Match match = new Match();
            match.setBracket(bracket);
            match.setRodada(1);
            match.setOrdem(ordem);
            match.setTipoPartida(TipoPartidaSumo.ELIMINATORIA);
            match.setAtivo(true);

            Registration participanteA = participantes.get(indiceParticipante++);
            match.setRegistrationA(participanteA);

            if (ordem <= quantidadeByes) {
                match.setRegistrationB(null);
                match.setStatus(StatusMatch.BYE);
            } else {
                Registration participanteB = participantes.get(indiceParticipante++);
                match.setRegistrationB(participanteB);
                match.setStatus(StatusMatch.AGENDADA);
            }

            partidas.add(match);
        }

        return partidas;
    }

    private int proximaPotenciaDeDois(int quantidade) {
        int potencia = 1;
        while (potencia < quantidade) {
            potencia *= 2;
        }
        return potencia;
    }

    private int calcularTotalRodadas(int tamanhoChave) {
        int rodadas = 0;
        int tamanhoAtual = tamanhoChave;

        while (tamanhoAtual > 1) {
            tamanhoAtual /= 2;
            rodadas++;
        }

        return rodadas;
    }

    private void validarAtivos(Competition competition, CompetitionCategory category) {
        if (!Boolean.TRUE.equals(competition.getAtivo())) {
            throw new IllegalArgumentException("Competição inativa.");
        }
        if (!Boolean.TRUE.equals(category.getAtivo())) {
            throw new IllegalArgumentException("Categoria inativa.");
        }
    }

    private void validarCategoriaSumo(CompetitionCategory category) {
        if (category.getModalidade() != Modalidade.SUMO) {
            throw new IllegalArgumentException(
                    "Chaveamento é exclusivo da modalidade SUMO. FOLLOW_LINE é definido pelo ranking de tempos.");
        }
    }

    private Competition buscarCompetitionParaAtualizacao(Long id) {
        return competitionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada: " + id));
    }

    private CompetitionCategory buscarCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada: " + id));
    }
}
