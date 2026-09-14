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

    /** MinIO 服务地址（含端口），如 http://127.0.0.1:9000 */
    private String endpoint;

    /** AccessKey（环境变量注入） */
    private String accessKey;

    /** SecretKey（环境变量注入） */
    private String secretKey;

    /** 私有桶名（眼底图与热力图均存于此桶） */
    private String bucket = "dr-screening";

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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
}
