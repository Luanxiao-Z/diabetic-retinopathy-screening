package cn.edu.fzu.drs.module.screening.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 对象存储客户端配置：基于 MinIO Java 客户端访问 MinIO（S3 兼容）。
 * <p>Endpoint / AccessKey / SecretKey 由 {@link MinioProperties} 从 application.yml 注入。</p>
 */
@Configuration
public class ObjectStorageConfig {

    @Bean
    public MinioClient drsMinioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
