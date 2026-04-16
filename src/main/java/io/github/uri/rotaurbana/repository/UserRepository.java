package io.github.uri.rotaurbana.repository;

import io.github.uri.rotaurbana.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity,Long> {
}
