package io.github.uri.rotaurbana.repository;

import io.github.uri.rotaurbana.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    UserDetails findByEmail(String email);

}
