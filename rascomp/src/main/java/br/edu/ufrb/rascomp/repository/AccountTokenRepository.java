package br.edu.ufrb.rascomp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.AccountToken;
import br.edu.ufrb.rascomp.model.Enum.AccountTokenType;

@Repository
public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {

    List<AccountToken> findByUserAccountIdAndTypeAndUsedAtIsNull(
            Long userAccountId,
            AccountTokenType type);

    Optional<AccountToken> findByTokenHashAndTypeAndUsedAtIsNull(
            String tokenHash,
            AccountTokenType type);

    Optional<AccountToken> findFirstByUserAccountIdAndTypeOrderByCreatedAtDesc(
            Long userAccountId,
            AccountTokenType type);
}
