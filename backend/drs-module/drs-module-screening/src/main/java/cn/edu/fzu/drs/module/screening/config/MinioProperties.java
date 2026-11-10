package cn.edu.fzu.drs.module.screening.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 对象存储配置绑定（对应 application.yml 的 {@code drs.minio.*}）。
 * <p>AccessKey / SecretKey 经环境变量 {@code MINIO_ACCESS_KEY} / {@code MINIO_SECRET_KEY} 注入，不落明文。</p>
 */
@Component
@ConfigurationProperties(prefix = "drs.minio")
public class MinioProperties {

    /** MinIO 服务地址（含端口），如 http://127.0.0.1:9000；后端访问对象存储使用此地址 */
    private String endpoint;

    /**
     * 预签名 URL 使用的对外地址（可选）。
     * <p>容器部署时后端通过服务名访问 MinIO（如 http://minio:9000），但该主机名在浏览器侧不可解析；
     * 此时应配置为浏览器可达地址（如 http://localhost:9000）。留空则回退 {@link #endpoint}。</p>
     */
    private String publicEndpoint;

    /** AccessKey（环境变量注入） */
    private String accessKey;

    /** SecretKey（环境变量注入） */
    private String secretKey;

    /** 私有桶名（眼底图与热力图均存于此桶） */
    private String bucket = "dr-screening";

    /**
     * 对象存储 region，默认 {@code us-east-1}（MinIO/Silo 默认值）。
     * <p>显式指定后，预签名客户端无需先连接服务端探测 region 即可生成 URL，
     * 从而允许 public-endpoint 使用容器内不可达、但浏览器可达的地址。</p>
     */
    private String region = "us-east-1";

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getPublicEndpoint() {
        return publicEndpoint;
    }

    public void setPublicEndpoint(String publicEndpoint) {
        this.publicEndpoint = publicEndpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
