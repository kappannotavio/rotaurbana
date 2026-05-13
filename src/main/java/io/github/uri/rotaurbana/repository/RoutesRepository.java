package io.github.uri.rotaurbana.repository;

import io.github.uri.rotaurbana.entity.BusEntity;
import io.github.uri.rotaurbana.entity.RoutesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutesRepository extends JpaRepository<RoutesEntity, Long> {

    List<RoutesEntity> findByBus(BusEntity bus);

    RoutesEntity findByCode(String code);

    List<RoutesEntity> findByPassengers_Id(Long userId);

}
