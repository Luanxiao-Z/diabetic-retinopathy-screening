package cn.edu.fzu.drs.module.screening.client;

import cn.edu.fzu.drs.module.common.exception.BusinessException;
import cn.edu.fzu.drs.module.screening.dto.ModelPredictResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * 模型推理服务客户端：调用 Python FastAPI 模型服务（独立部署，不走 /api/v1）。
 * <ul>
 *   <li>{@code POST /predict}：多分类推理，返回分级、置信度、各类概率、转诊建议。</li>
 *   <li>{@code POST /cam}：生成 Grad-CAM 热力图（PNG 字节）。</li>
 *   <li>{@code GET /model/info}：模型元信息与训练指标。</li>
 * </ul>
 * 服务不可达或返回非 2xx 时统一抛出业务异常，便于后端降级提示。
 */
@Component
public class ModelInferenceClient {

    private static final Logger log = LoggerFactory.getLogger(ModelInferenceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ModelInferenceClient(@Value("${drs.model-service.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 查询模型元信息与训练指标（转发模型服务 {@code GET /model/info}）。
     * 用于前端「模型信息」页展示模型来源与可信度。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> modelInfo() {
        try {
            return restTemplate.getForObject(baseUrl + "/model/info", Map.class);
        } catch (RestClientException e) {
            log.error("调用模型服务 /model/info 失败：{}", e.getMessage());
            throw new BusinessException(503, "无法连接模型推理服务，请确认服务已启动");
        }
    }

    /**
     * 调用推理接口，返回分类结果。
     */
    public ModelPredictResponse predict(byte[] imageBytes, String originalFilename) {        HttpEntity<MultiValueMap<String, Object>> request = buildMultipartRequest(imageBytes, originalFilename);
        try {
            ResponseEntity<ModelPredictResponse> response = restTemplate.postForEntity(
                    baseUrl + "/predict", request, ModelPredictResponse.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(503, "模型推理服务返回异常");
            }
            return response.getBody();
        } catch (RestClientException e) {
            log.error("调用模型推理服务 /predict 失败：{}", e.getMessage());
            throw new BusinessException(503, "无法连接模型推理服务，请确认服务已启动");
        }
    }

    /**
     * 调用 Grad-CAM 接口，返回热力图 PNG 字节。失败时抛出业务异常。
     */
    public byte[] generateCam(byte[] imageBytes, String originalFilename) {
        HttpEntity<MultiValueMap<String, Object>> request = buildMultipartRequest(imageBytes, originalFilename);
        try {
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    baseUrl + "/cam", request, byte[].class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(503, "模型热力图服务返回异常");
            }
            return response.getBody();
        } catch (RestClientException e) {
            log.error("调用模型热力图服务 /cam 失败：{}", e.getMessage());
            throw new BusinessException(503, "无法连接模型热力图服务，请确认服务已启动");
        }
    }

    private HttpEntity<MultiValueMap<String, Object>> buildMultipartRequest(byte[] imageBytes, String originalFilename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new NamedByteArrayResource(imageBytes, originalFilename));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(body, headers);
    }

    /**
     * 支持命名（携带原始文件名的）字节资源，确保 multipart 写入正确的 part filename。
     */
    private static class NamedByteArrayResource extends org.springframework.core.io.ByteArrayResource {

        private final String filename;

        NamedByteArrayResource(byte[] bytes, String filename) {
            super(bytes);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(getByteArray());
        }
    }
}
