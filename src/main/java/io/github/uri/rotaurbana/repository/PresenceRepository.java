package io.github.uri.rotaurbana.repository;

import io.github.uri.rotaurbana.entity.PresenceEntity;
import io.github.uri.rotaurbana.entity.RoutesEntity;
import io.github.uri.rotaurbana.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PresenceRepository extends JpaRepository<PresenceEntity, Long> {

    Optional<PresenceEntity> findByUserAndRouteAndDate(UserEntity user, RoutesEntity route, LocalDate date);

}
