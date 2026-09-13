package br.edu.ufrb.rascomp.model.Enum;

public enum UserRole {
    DEV,
    GESTAO,
    MIDIA,
    PARTICIPANTE;

    public boolean podeOperarCompeticao() {
        return this == DEV || this == GESTAO;
    }

    public boolean podeAdministrarUsuarios() {
        return this == DEV;
    }
}
