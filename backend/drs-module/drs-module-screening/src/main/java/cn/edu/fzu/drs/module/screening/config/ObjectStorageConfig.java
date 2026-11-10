package cn.edu.fzu.drs.module.screening.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 对象存储客户端配置：基于 MinIO Java 客户端访问 MinIO（S3 兼容）。
 * <p>Endpoint / AccessKey / SecretKey 由 {@link MinioProperties} 从 application.yml 注入。</p>
 *
 * <p>提供两个客户端：</p>
 * <ul>
 *   <li>{@code drsMinioClient}（主）：后端实际读写对象时使用，容器部署下为服务名地址（如 http://minio:9000）；</li>
 *   <li>{@code minioPresignClient}：仅用于生成预签名 URL，使用浏览器可达地址（如 http://localhost:9000）。
 *       预签名签名包含 Host，必须与浏览器实际访问的地址一致，否则会返回 SignatureDoesNotMatch。</li>
 * </ul>
 */
@Configuration
public class ObjectStorageConfig {

    @Bean
    @Primary
    public MinioClient drsMinioClient(MinioProperties properties) {
        return buildClient(properties.getEndpoint(), properties);
    }

    /**
     * 预签名专用客户端。{@code drs.minio.public-endpoint} 未配置时回退为 {@code endpoint}，
     * 保持本地单机运行的原有行为。
     * <p>显式指定 region：预签名时无需先连接服务端探测 region，因此该客户端可以使用
     * 容器内不可达、但浏览器可访问的地址（如 http://localhost:9000）。</p>
     */
    @Bean
    @Qualifier("minioPresignClient")
    public MinioClient minioPresignClient(MinioProperties properties) {
        String endpoint = properties.getPublicEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = properties.getEndpoint();
        }
        return MinioClient.builder()
                .endpoint(endpoint)
                .region(properties.getRegion())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    private MinioClient buildClient(String endpoint, MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
