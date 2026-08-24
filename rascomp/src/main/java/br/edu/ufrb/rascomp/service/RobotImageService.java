package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.edu.ufrb.rascomp.dto.RobotImageDTO;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.RobotImage;
import br.edu.ufrb.rascomp.repository.RobotImageRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RobotImageService {

    private final RobotRepository robotRepository;
    private final RobotImageRepository robotImageRepository;
    private final RobotImageStorageService storageService;

    @Transactional
    public RobotImageDTO adicionar(Long robotId, MultipartFile arquivo) {
        Robot robot = buscarRobot(robotId);
        List<RobotImage> atuais = robotImageRepository
                .findByRobotIdAndAtivoTrueOrderByPrincipalDescOrdemAscIdAsc(robotId);

        String storageKey = storageService.armazenar(robotId, arquivo);

        RobotImage image = new RobotImage();
        image.setRobot(robot);
        image.setStorageKey(storageKey);
        image.setOriginalFilename(nomeSeguro(arquivo.getOriginalFilename()));
        image.setContentType(arquivo.getContentType());
        image.setPrincipal(atuais.isEmpty());
        image.setOrdem(atuais.size());
        image.setAtivo(true);

        return new RobotImageDTO(robotImageRepository.save(image));
    }

    @Transactional(readOnly = true)
    public List<RobotImageDTO> listar(Long robotId) {
        buscarRobot(robotId);
        return robotImageRepository
                .findByRobotIdAndAtivoTrueOrderByPrincipalDescOrdemAscIdAsc(robotId)
                .stream()
                .map(RobotImageDTO::new)
                .toList();
    }

    @Transactional
    public RobotImageDTO definirPrincipal(Long robotId, Long imageId) {
        buscarRobot(robotId);
        RobotImage selecionada = buscarImagem(robotId, imageId);

        robotImageRepository.findByRobotIdAndAtivoTrueOrderByPrincipalDescOrdemAscIdAsc(robotId)
                .forEach(image -> image.setPrincipal(image.getId().equals(selecionada.getId())));

        robotImageRepository.flush();
        return new RobotImageDTO(selecionada);
    }

    @Transactional
    public void remover(Long robotId, Long imageId) {
        RobotImage image = buscarImagem(robotId, imageId);
        boolean eraPrincipal = Boolean.TRUE.equals(image.getPrincipal());
        image.setAtivo(false);
        image.setPrincipal(false);
        robotImageRepository.save(image);
        storageService.remover(image.getStorageKey());

        if (eraPrincipal) {
            robotImageRepository.findByRobotIdAndAtivoTrueOrderByPrincipalDescOrdemAscIdAsc(robotId)
                    .stream().findFirst()
                    .ifPresent(proxima -> {
                        proxima.setPrincipal(true);
                        robotImageRepository.save(proxima);
                    });
        }
    }

    @Transactional(readOnly = true)
    public RobotImageFile carregarPublico(Long robotId, Long imageId) {
        RobotImage image = buscarImagem(robotId, imageId);
        if (!Boolean.TRUE.equals(image.getAtivo())) {
            throw new EntityNotFoundException("Imagem não encontrada.");
        }
        return new RobotImageFile(
                storageService.carregar(image.getStorageKey()),
                image.getContentType(),
                image.getOriginalFilename());
    }

    private Robot buscarRobot(Long id) {
        return robotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Robô não encontrado: " + id));
    }

    private RobotImage buscarImagem(Long robotId, Long imageId) {
        return robotImageRepository.findByIdAndRobotId(imageId, robotId)
                .orElseThrow(() -> new EntityNotFoundException("Imagem não encontrada para o robô informado."));
    }

    private String nomeSeguro(String original) {
        if (original == null || original.isBlank()) return "imagem";
        String nome = original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1);
        return nome.length() <= 255 ? nome : nome.substring(nome.length() - 255);
    }

    public record RobotImageFile(Resource resource, String contentType, String filename) {}
}
