package io.github.uri.rotaurbana.controller.restController;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Arquivo vazio"));
        }

        try {
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

            String imageUrl = "/images/profiles/" + fileName;

            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao salvar imagem: " + e.getMessage()));
        }
    }

}
