package br.edu.ufrb.rascomp.service;

import org.springframework.stereotype.Service;

import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {
	
	
	private final TeamRepository teamRepository;
	private final InstitutionRepository institutionRepository;
	
	
	

}
