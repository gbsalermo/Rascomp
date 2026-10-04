package br.edu.ufrb.rascomp.dto;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotResponsibilityUpdateRequest {

    @NotNull
    private Set<Long> competitorIds = new LinkedHashSet<>();
}
