package com.musicdreamer.media;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
public class MediaApplicationConfiguration {

    @Bean
    MediaFileValidator mediaFileValidator(
            @Value("${music-dreamer.media.max-file-bytes:52428800}") long maxFileBytes) {
        return new MediaFileValidator(maxFileBytes);
    }

    @Bean
    MediaStorageService mediaStorageService(
            JdbcTemplate jdbc,
            @Value("${music-dreamer.media.storage-root:E:/Zmkk/music-dreamer-media}") String storageRoot,
            MediaFileValidator validator) {
        return new MediaStorageService(jdbc, Path.of(storageRoot), validator);
    }
}
