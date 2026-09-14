package cn.edu.fzu.drs.module.screening.service;

/**
 * 对象存储服务：封装 MinIO（S3 兼容）的桶初始化、上传、预签名下载、下载与删除。
 */
public interface ObjectStorageService {

    /**
     * 上传对象。
     *
     * @param key         对象键（含路径前缀）
     * @param data        字节内容
     * @param contentType 内容类型，如 image/jpeg、image/png
     */
    void upload(String key, byte[] data, String contentType);

    /**
     * 生成带时效的预签名 GET URL（用于私有桶临时访问）。
     *
     * @param key              对象键
     * @param expireMinutes    有效期（分钟）
     * @return 预签名 URL 字符串
     */
    String presignUrl(String key, int expireMinutes);

    /**
     * 下载对象字节。
     *
     * @param key 对象键
     * @return 对象字节
     */
    byte[] download(String key);

    /**
     * 删除对象（记录删除时清理存储）。
     *
     * @param key 对象键
     */
    void delete(String key);
}
