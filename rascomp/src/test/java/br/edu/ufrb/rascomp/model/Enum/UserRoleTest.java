package br.edu.ufrb.rascomp.model.Enum;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    void devDeveAdministrarUsuariosEOperarCompeticao() {
        assertTrue(UserRole.DEV.podeAdministrarUsuarios());
        assertTrue(UserRole.DEV.podeOperarCompeticao());
    }

    @Test
    void gestaoDeveOperarCompeticaoSemAdministrarUsuarios() {
        assertTrue(UserRole.GESTAO.podeOperarCompeticao());
        assertFalse(UserRole.GESTAO.podeAdministrarUsuarios());
    }

    @Test
    void midiaNaoDeveHerdarOperacaoCompetitivaOuAdministracao() {
        assertFalse(UserRole.MIDIA.podeOperarCompeticao());
        assertFalse(UserRole.MIDIA.podeAdministrarUsuarios());
    }

    @Test
    void participanteNaoDeveHerdarOperacaoCompetitivaOuAdministracao() {
        assertFalse(UserRole.PARTICIPANTE.podeOperarCompeticao());
        assertFalse(UserRole.PARTICIPANTE.podeAdministrarUsuarios());
    }
}
