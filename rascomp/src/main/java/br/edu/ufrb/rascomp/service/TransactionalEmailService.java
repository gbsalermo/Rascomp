package br.edu.ufrb.rascomp.service;

public interface TransactionalEmailService {

    void send(String to, String subject, String html);
}
