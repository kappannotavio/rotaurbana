package io.github.uri.rotaurbana.repository;

import io.github.uri.rotaurbana.entity.DriverEntity;
import io.github.uri.rotaurbana.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<DriverEntity, Long> {

    Optional<DriverEntity> findByUser(UserEntity user);

}
