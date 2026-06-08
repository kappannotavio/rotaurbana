package io.github.uri.rotaurbana.repository;

import io.github.uri.rotaurbana.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.email) = LOWER(?1)")
    UserDetails findByEmail(String email);

}
