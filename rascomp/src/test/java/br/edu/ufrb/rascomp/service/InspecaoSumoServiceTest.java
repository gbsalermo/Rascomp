package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.InspecaoSumoDTO;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.ConfigSumo;
import br.edu.ufrb.rascomp.model.InspecaoSumo;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.ConfigSumoRepository;
import br.edu.ufrb.rascomp.repository.InspecaoSumoRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;

@ExtendWith(MockitoExtension.class)
class InspecaoSumoServiceTest {

    @Mock private InspecaoSumoRepository inspecaoRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private ConfigSumoRepository configSumoRepository;

    @InjectMocks
    private InspecaoSumoService service;

    private CompetitionCategory category;
    private Registration registration;
    private ConfigSumo config;

    @BeforeEach
    void setUp() {
        category = CompetitionCategory.builder()
                .id(1L)
                .nome("Mini Sumo")
                .modalidade(Modalidade.SUMO)
                .ativo(true)
                .build();

        registration = new Registration();
        registration.setId(10L);
        registration.setCategory(category);
        registration.setStatus(StatusRegistration.APROVADA);
        registration.setAtivo(true);

        config = ConfigSumo.builder()
                .competitionCategory(category)
                .pesoMax(new BigDecimal("0.500"))
                .exigeInspecao(true)
                .maxTentativasInspecao(3)
                .numeroRounds(3)
                .roundsParaVencer(2)
                .permiteRoundDesempate(true)
                .build();

        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));
        when(configSumoRepository.findByCompetitionCategoryId(1L)).thenReturn(Optional.of(config));
    }

    @Test
    void deveAprovarInspecaoDentroDoPesoMaximo() {
        when(inspecaoRepository.existsByRegistrationIdAndAprovadaTrue(10L)).thenReturn(false);
        when(inspecaoRepository.countByRegistrationId(10L)).thenReturn(0L);
        when(inspecaoRepository.save(any(InspecaoSumo.class)))
                .thenAnswer(invocation -> {
                    InspecaoSumo entity = invocation.getArgument(0);
                    entity.setId(100L);
                    return entity;
                });

        InspecaoSumoDTO dto = novaInspecao(new BigDecimal("0.450"));
        InspecaoSumoDTO resultado = service.registrar(dto);

        assertTrue(resultado.getAprovada());
        assertEquals(1, resultado.getNumeroTentativa());
        assertEquals(0, new BigDecimal("0.450").compareTo(resultado.getPesoMedido()));
        verify(registrationRepository, never()).save(registration);
    }

    @Test
    void ultimaReprovacaoDeveDesclassificarInscricao() {
        when(inspecaoRepository.existsByRegistrationIdAndAprovadaTrue(10L)).thenReturn(false);
        when(inspecaoRepository.countByRegistrationId(10L)).thenReturn(2L);
        when(inspecaoRepository.save(any(InspecaoSumo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InspecaoSumoDTO resultado = service.registrar(novaInspecao(new BigDecimal("0.600")));

        assertFalse(resultado.getAprovada());
        assertEquals(3, resultado.getNumeroTentativa());
        assertEquals(StatusRegistration.DESCLASSIFICADA, registration.getStatus());
        verify(registrationRepository).save(registration);
    }

    @Test
    void inscricaoAprovadaDeveSerAptaQuandoCategoriaNaoExigeInspecao() {
        config.setExigeInspecao(false);

        boolean apta = service.estaAptaParaCompetir(10L);

        assertTrue(apta);
        verify(inspecaoRepository, never()).existsByRegistrationIdAndAprovadaTrue(10L);
    }

    private InspecaoSumoDTO novaInspecao(BigDecimal peso) {
        InspecaoSumoDTO dto = new InspecaoSumoDTO();
        dto.setRegistrationId(10L);
        dto.setPesoMedido(peso);
        dto.setObservacao("Teste automatizado");
        return dto;
    }
}
