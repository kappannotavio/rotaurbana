package io.github.uri.rotaurbana.repository;

import io.github.uri.rotaurbana.entity.LogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<LogEntity, Long> {

    List<LogEntity> findAllByOrderByTimestampDesc();

    List<LogEntity> findByEntityTypeOrderByTimestampDesc(String entityType);

}
