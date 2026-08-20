package br.edu.ufrb.rascomp.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.BracketDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusBracket;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
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
    private final InspecaoSumoService inspecaoSumoService;

    @Transactional
    public BracketDTO gerar(Long competitionId, Long categoryId) {
        Competition competition = buscarCompetition(competitionId);
        CompetitionCategory category = buscarCategory(categoryId);

        validarAtivos(competition, category);
        validarCategoriaSumo(category);
        validarChaveamentoInexistente(competitionId, categoryId);

        List<Registration> participantes = buscarParticipantesElegiveis(competitionId, categoryId);
        if (participantes.size() < 2) {
            throw new IllegalArgumentException(
                    "São necessárias pelo menos duas inscrições de Sumô ativas, aprovadas e aptas para gerar o chaveamento.");
        }

        List<Registration> participantesSorteados = new ArrayList<>(participantes);
        Collections.shuffle(participantesSorteados);

        Bracket bracket = criarBracket(competition, category);
        List<Match> primeiraRodada = gerarArvoreCompleta(bracket, participantesSorteados);

        bracket.setStatus(StatusBracket.GERADO);
        Bracket salvo = bracketRepository.save(bracket);

        primeiraRodada.stream()
                .filter(match -> match.getStatus() == StatusMatch.BYE)
                .forEach(bracketProgressionService::avancarBye);

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

    private Bracket criarBracket(Competition competition, CompetitionCategory category) {
        Bracket bracket = new Bracket();
        bracket.setCompetition(competition);
        bracket.setCategory(category);
        bracket.setNome("Chaveamento - " + competition.getNome() + " - " + category.getNome());
        bracket.setStatus(StatusBracket.RASCUNHO);
        bracket.setAtivo(true);
        return bracketRepository.save(bracket);
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
                match.setRegistrationA(null);
                match.setRegistrationB(null);
                match.setStatus(StatusMatch.AGUARDANDO_PARTICIPANTES);
                match.setAtivo(true);
                partidas.add(match);
            }
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

    private void validarChaveamentoInexistente(Long competitionId, Long categoryId) {
        if (bracketRepository.existsByCompetitionIdAndCategoryId(competitionId, categoryId)) {
            throw new IllegalArgumentException("Já existe um chaveamento para esta competição e categoria.");
        }
    }

    private Competition buscarCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada: " + id));
    }

    private CompetitionCategory buscarCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada: " + id));
    }
}
