package com.printforge.repository;

import com.printforge.entity.PasswordResetToken;
import com.printforge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
  Optional<PasswordResetToken> findByTokenHash(String tokenHash);
  List<PasswordResetToken> findByUserAndUsedAtIsNull(User user);
}
