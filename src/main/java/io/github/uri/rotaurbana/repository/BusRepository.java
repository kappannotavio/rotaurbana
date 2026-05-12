package io.github.uri.rotaurbana.repository;

import io.github.uri.rotaurbana.entity.BusEntity;
import io.github.uri.rotaurbana.entity.DriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusRepository extends JpaRepository<BusEntity, Long> {

    BusEntity findBySign(String sign);

    BusEntity findByCode(String code);

    List<BusEntity> findByDriver(DriverEntity driver);

    List<BusEntity> findByPassengers_Id(Long userId);

}
