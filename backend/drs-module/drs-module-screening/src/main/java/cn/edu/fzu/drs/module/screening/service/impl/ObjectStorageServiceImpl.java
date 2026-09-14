package cn.edu.fzu.drs.module.screening.service.impl;

import cn.edu.fzu.drs.module.common.exception.BusinessException;
import cn.edu.fzu.drs.module.screening.config.MinioProperties;
import cn.edu.fzu.drs.module.screening.service.ObjectStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * 对象存储服务实现（MinIO / S3 兼容）。
 * <p>启动后确保目标桶存在（不存在则创建为私有桶）。</p>
 */
@Service
public class ObjectStorageServiceImpl implements ObjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(ObjectStorageServiceImpl.class);

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public ObjectStorageServiceImpl(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @PostConstruct
    public void initBucket() {
        String bucket = properties.getBucket();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (exists) {
                log.info("MinIO 桶已存在：{}", bucket);
            } else {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("已创建 MinIO 私有桶：{}", bucket);
            }
        } catch (Exception e) {
            log.warn("连接 MinIO 失败（对象存储将在首次上传时重试）：{}", e.getMessage());
        }
    }

    @Override
    public void upload(String key, byte[] data, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
        } catch (Exception e) {
            log.error("上传对象到 MinIO 失败：{}", e.getMessage());
            throw new BusinessException(500, "上传对象到 MinIO 失败：" + e.getMessage());
        }
    }

    @Override
    public String presignUrl(String key, int expireMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.getBucket())
                    .object(key)
                    .expiry(expireMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("生成预签名 URL 失败：{}", e.getMessage());
            throw new BusinessException(500, "生成预签名 URL 失败：" + e.getMessage());
        }
    }

    @Override
    public byte[] download(String key) {
        try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(key)
                .build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            log.error("下载对象失败：{}", e.getMessage());
            throw new BusinessException(500, "下载对象失败：" + e.getMessage());
        }
    }

    @Override
    public void delete(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .build());
        } catch (Exception e) {
            log.error("删除对象失败：{}", e.getMessage());
            throw new BusinessException(500, "删除对象失败：" + e.getMessage());
        }
    }
}
