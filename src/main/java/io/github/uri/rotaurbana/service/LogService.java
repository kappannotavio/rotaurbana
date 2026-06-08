package io.github.uri.rotaurbana.service;

import io.github.uri.rotaurbana.entity.LogEntity;
import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogService {

    @Autowired
    private LogRepository logRepository;

    public void log(String action, String entityType, Long entityId, String description) {
        UserEntity user = getCurrentUser();
        if (user != null) {
            log(action, entityType, entityId, description, user.getId(), user.getFullName());
        } else {
            log(action, entityType, entityId, description, null, "Sistema");
        }
    }

    public void log(String action, String entityType, Long entityId, String description,
                    Long performedById, String performedByName) {
        LogEntity log = new LogEntity();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setPerformedById(performedById);
        log.setPerformedByName(performedByName);
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
    }

    public List<LogEntity> getAllLogs() {
        return logRepository.findAllByOrderByTimestampDesc();
    }

    public List<LogEntity> getLogsByEntityType(String entityType) {
        return logRepository.findByEntityTypeOrderByTimestampDesc(entityType);
    }

    private UserEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserEntity user) {
            return user;
        }
        return null;
    }

}
