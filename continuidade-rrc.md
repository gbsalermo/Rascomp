# Documento de Continuidade — RRC
Última atualização: 2026-07-28T11:08:00-03:00

## Estado atual
- PDF do projeto (Prompt_Inicial_RRC.pdf) lido integralmente e registrado como fonte autoritativa.
- Documento de Continuidade (continuidade-rrc.md) criado na raiz do projeto.
- Planejamento inicial do backend definido (detalhado abaixo).
- Estrutura básica do backend (pacotes e artefatos) documentada neste arquivo. Ainda não foram aplicadas alterações de código.
- Decisão: desenvolvimento inicial será feito usando Hibernate auto DDL com H2 (memória/arquivo) em vez de Flyway; justificativa registrada abaixo.

## Plano detalhado — Backend (ordem e estimativa)
1. Inicialização do projeto (Spring Initializr) com dependências do PDF — 0.5 dia
2. Gerar estrutura de pacotes e artefatos base (classes, DTOs, entidades vazias) — 0.5 dia
3. Usar Hibernate auto DDL (spring.jpa.hibernate.ddl-auto=update/create-drop) e H2 para desenvolvimento/testes iniciais — 0.25 dia
4. Implementar Entity Category (JPA + Lombok) — 0.25 dia
5. Implementar CategoryRepository, CategoryService, CategoryController (CRUD completo) — 1 dia
6. Testes básicos via Postman/Swagger e ajustes — 0.5 dia
7. Adicionar Camunda processes (após CRUD base) e adaptar Match quando for o caso — 1 dia
8. Preparar docker-compose e infra local (Postgres + pgAdmin) — 0.5 dia (ao final do backend funcional)

Estimativa total backend (com H2 dev): ~4.25 dias

## Estrutura de diretórios e pacotes (base proposta)
Raiz do backend gerado via Spring Initializr: `backend/`

backend/
├── src/main/java/br/edu/ufrb/rrc/
│   ├── config                 # Configurações gerais (Security, DataSource, Camunda)
│   ├── controller             # Controllers REST
│   ├── dto                    # DTOs de entrada/saída
│   ├── entity                 # Entidades JPA
│   ├── exception              # Exceções customizadas e handlers
│   ├── repository             # Interfaces JPA (Spring Data)
│   ├── service                # Serviços de negócio
│   └── util                   # Utilitários (mappers, validators)
└── src/main/resources/
    ├── processes/             # Arquivos BPMN do Camunda
    └── db/migration/          # (Pasta mantida para futuras migrations, atualmente não usada)

Observação: nomes e paths seguem o PDF. Package root: `br.edu.ufrb.rrc`.

## Entidades básicas (esqueleto inicial)
A seguir, definições simplificadas das entidades iniciais a incluir (campos essenciais):

- Category (categories)
  - id: Long (PK)
  - nome: String (not null)
  - codigo: String (not null, único)
  - descricao: String
  - pesoMin: Integer (nullable)
  - pesoMax: Integer (nullable)
  - tipoPontuacao: String (ROUNDS/TEMPO)

- Institution (institutions)  <-- nova entidade adicionada
  - id: Long
  - nome: String (not null)
  - sigla: String
  - instituicaoEndereco: String
  - contato: String
  - Observação: Institution tem N Teams (1:N relationship)

- Team (teams)
  - id: Long
  - nome: String
  - sigla: String
  - instituicao_id: Long (FK para Institution)
  - tecnico_responsavel: String

- User, Competitor, Robot, Competition, Registration, Bracket, Match, MatchResult (descrições como no PDF) — criar esqueleto posteriormente.

## Artefatos (classes) a criar imediatamente — esqueleto e códigos de referência
Abaixo estão os snippets de referência criados e que você deve copiar para os caminhos indicados. O objetivo é que você implemente manualmente no IDE seguindo esses exemplos.

1) DTO: src/main/java/br/edu/ufrb/rrc/dto/CategoryDTO.java
```java
package br.edu.ufrb.rrc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long id;

    @NotBlank(message = "nome é obrigatório")
    private String nome;

    @NotBlank(message = "codigo é obrigatório")
    private String codigo;

    private String descricao;
    private Integer pesoMin;
    private Integer pesoMax;
    private String tipoPontuacao; // ROUNDS / TEMPO
}
```

2) Entity: src/main/java/br/edu/ufrb/rrc/entity/Category.java
```java
package br.edu.ufrb.rrc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(columnNames = "codigo"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(columnDefinition = "text")
    private String descricao;

    private Integer pesoMin;
    private Integer pesoMax;

    private String tipoPontuacao;

    @CreationTimestamp
    private Instant createdAt;
}
```

3) Repository: src/main/java/br/edu/ufrb/rrc/repository/CategoryRepository.java
```java
package br.edu.ufrb.rrc.repository;

import br.edu.ufrb.rrc.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByCodigo(String codigo);
}
```

4) Exception: src/main/java/br/edu/ufrb/rrc/exception/ResourceNotFoundException.java
```java
package br.edu.ufrb.rrc.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
```

5) Service + Impl: src/main/java/br/edu/ufrb/rrc/service/CategoryService.java
```java
package br.edu.ufrb.rrc.service;

import br.edu.ufrb.rrc.dto.CategoryDTO;

import java.util.List;

public interface CategoryService {
    List<CategoryDTO> findAll();
    CategoryDTO findById(Long id);
    CategoryDTO create(CategoryDTO dto);
    CategoryDTO update(Long id, CategoryDTO dto);
    void delete(Long id);
}
```

Impl: src/main/java/br/edu/ufrb/rrc/service/impl/CategoryServiceImpl.java
```java
package br.edu.ufrb.rrc.service.impl;

import br.edu.ufrb.rrc.dto.CategoryDTO;
import br.edu.ufrb.rrc.entity.Category;
import br.edu.ufrb.rrc.exception.ResourceNotFoundException;
import br.edu.ufrb.rrc.repository.CategoryRepository;
import br.edu.ufrb.rrc.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repo;

    public CategoryServiceImpl(CategoryRepository repo) {
        this.repo = repo;
    }

    private CategoryDTO toDto(Category e) {
        return new CategoryDTO(
            e.getId(),
            e.getNome(),
            e.getCodigo(),
            e.getDescricao(),
            e.getPesoMin(),
            e.getPesoMax(),
            e.getTipoPontuacao()
        );
    }

    private void updateEntityFromDto(Category e, CategoryDTO d) {
        e.setNome(d.getNome());
        e.setCodigo(d.getCodigo());
        e.setDescricao(d.getDescricao());
        e.setPesoMin(d.getPesoMin());
        e.setPesoMax(d.getPesoMax());
        e.setTipoPontuacao(d.getTipoPontuacao());
    }

    @Override
    public List<CategoryDTO> findAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public CategoryDTO findById(Long id) {
        Category c = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        return toDto(c);
    }

    @Override
    public CategoryDTO create(CategoryDTO dto) {
        Category e = Category.builder().build();
        updateEntityFromDto(e, dto);
        Category saved = repo.save(e);
        return toDto(saved);
    }

    @Override
    public CategoryDTO update(Long id, CategoryDTO dto) {
        Category e = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        updateEntityFromDto(e, dto);
        return toDto(repo.save(e));
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Category not found: " + id);
        repo.deleteById(id);
    }
}
```

6) Controller: src/main/java/br/edu/ufrb/rrc/controller/CategoryController.java
```java
package br.edu.ufrb.rrc.controller;

import br.edu.ufrb.rrc.dto.CategoryDTO;
import br.edu.ufrb.rrc.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> create(@Valid @RequestBody CategoryDTO dto) {
        CategoryDTO created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/categories/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

7) Exception handler: src/main/java/br/edu/ufrb/rrc/exception/RestExceptionHandler.java
```java
package br.edu.ufrb.rrc.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field = err instanceof FieldError ? ((FieldError) err).getField() : err.getObjectName();
            String msg = err.getDefaultMessage();
            errors.put(field, msg);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("errors", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
    }
}
```

8) application.properties (H2 dev): src/main/resources/application.properties
```properties
spring.datasource.url=jdbc:h2:mem:rrcdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

springdoc.api-docs.path=/v3/api-docs
server.port=8080
```

9) Institution entity (novo) — Entity + DTO + Repository snippets
Entity: src/main/java/br/edu/ufrb/rrc/entity/Institution.java
```java
package br.edu.ufrb.rrc.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "institutions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Institution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String sigla;
    private String instituicaoEndereco;
    private String contato;

    @OneToMany(mappedBy = "instituicao")
    private List<Team> teams;
}
```

DTO: src/main/java/br/edu/ufrb/rrc/dto/InstitutionDTO.java
```java
package br.edu.ufrb.rrc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionDTO {
    private Long id;
    private String nome;
    private String sigla;
    private String instituicaoEndereco;
    private String contato;
}
```

Repository: src/main/java/br/edu/ufrb/rrc/repository/InstitutionRepository.java
```java
package br.edu.ufrb.rrc.repository;

import br.edu.ufrb.rrc.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {}
```

## Convenções e decisões locais (registradas)
- Java 21 e Spring Boot 3.5.3 obrigatórios conforme PDF.
- Lombok será usado para reduzir boilerplate nas entidades.
- Optou-se por usar Hibernate auto DDL (hibernate.ddl-auto) em ambiente de desenvolvimento com H2 para ganhar agilidade inicial e permitir testes rápidos sem dependência externa.
- A pasta `src/main/resources/db/migration` será mantida para histórico e futura adoção de Flyway somente se necessário.
- Ordem ao criar uma entidade: entity → repository → service → controller → teste (ajustada para eliminar uso de migrations nesta fase).
- Pacotes organizados por camada (não por domínio) conforme PDF.

### Justificativa para não usar Flyway neste momento
- Objetivo atual: acelerar prototipagem e validação local do CRUD com mínima fricção (sem configurar banco externo).
- H2 + hibernate.ddl-auto permite criar/esquecer esquemas rapidamente para testes manuais e automação de desenvolvimento.
- Projeto não deverá usar este esquema em produção; antes de deploy em Postgres será necessário revisar esquema e considerar Flyway se for desejado para controle de versão do DB.
- Decisão documentada e registrada para rastreabilidade e futura reversão se necessário.

## Próximo passo imediato (a executar agora)
- Copiar os snippets de referência para os caminhos indicados e rodar a aplicação em IDE (STS). Hibernate criará as tabelas automaticamente.
- Validar via H2 console e Swagger/Postman os endpoints /api/categories e endpoints futuros para Institution.

## Tarefas registradas (para acompanhamento)
- backend/skeleton — criar estrutura de pacotes e arquivos esqueleto (pendente)
- backend/h2-config — configurar H2 para desenvolvimento (pendente)
- backend/institution — adicionar entidade Institution + endpoints (pendente)

## Histórico de atualizações
- 2026-07-28T10:30:07-03:00 — Documento inicial criado; PDF lido e registrado como fonte autoritativa.
- 2026-07-28T10:37:26-03:00 — Plano detalhado do backend e estrutura inicial adicionados conforme solicitação do desenvolvedor.
- 2026-07-28T11:02:16-03:00 — Decisão registrada: usar Hibernate auto DDL + H2 para desenvolvimento inicial; justificativa e próximos passos adicionados.
- 2026-07-28T11:08:00-03:00 — Adicionados snippets de referência para Category e Institution; continuidade atualizada.
