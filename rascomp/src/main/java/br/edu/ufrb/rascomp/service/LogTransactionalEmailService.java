package br.edu.ufrb.rascomp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "app.email.provider",
        havingValue = "log",
        matchIfMissing = true)
public class LogTransactionalEmailService implements TransactionalEmailService {

    private static final Logger log = LoggerFactory.getLogger(LogTransactionalEmailService.class);

    @Override
    public void send(String to, String subject, String html) {
        log.info("E-mail transacional local | para={} | assunto={} | conteudo={}", to, subject, html);
    }
}
