package io.github.uri.rotaurbana.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileUploadService {

    public String uploadImage(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;

        Path[] dirs = {
            Paths.get("src/main/resources/static/images/profiles"),
            Paths.get("target/classes/static/images/profiles")
        };

        for (Path dir : dirs) {
            Files.createDirectories(dir);
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return "/images/profiles/" + fileName;
    }
}
