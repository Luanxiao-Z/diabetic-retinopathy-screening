package cn.edu.fzu.drs.module.screening.service.impl;

import cn.edu.fzu.drs.module.common.exception.BusinessException;
import cn.edu.fzu.drs.module.common.result.PageResult;
import cn.edu.fzu.drs.module.common.util.IdGenerator;
import cn.edu.fzu.drs.module.screening.client.ModelInferenceClient;
import cn.edu.fzu.drs.module.screening.convert.BizScreeningRecordConvert;
import cn.edu.fzu.drs.module.screening.dto.ModelPredictResponse;
import cn.edu.fzu.drs.module.screening.dto.ScreeningPageQuery;
import cn.edu.fzu.drs.module.screening.entity.BizScreeningRecordEntity;
import cn.edu.fzu.drs.module.screening.mapper.BizScreeningRecordMapper;
import cn.edu.fzu.drs.module.screening.service.ObjectStorageService;
import cn.edu.fzu.drs.module.screening.service.ScreeningRecordService;
import cn.edu.fzu.drs.module.screening.vo.DailyCountVO;
import cn.edu.fzu.drs.module.screening.vo.ScreeningRecordVO;
import cn.edu.fzu.drs.module.screening.vo.ScreeningStatisticsVO;
import cn.edu.fzu.drs.module.security.context.AuthContext;
import cn.edu.fzu.drs.module.security.util.DataScopeUtils;
import cn.edu.fzu.drs.module.security.model.AuthPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 筛查记录业务实现。
 */
@Service
public class ScreeningRecordServiceImpl implements ScreeningRecordService {

    private static final Logger log = LoggerFactory.getLogger(ScreeningRecordServiceImpl.class);

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int PRESIGN_MINUTES = 30;

    private final BizScreeningRecordMapper mapper;
    private final ObjectStorageService objectStorage;
    private final ModelInferenceClient inferenceClient;

    public ScreeningRecordServiceImpl(BizScreeningRecordMapper mapper,
                                     ObjectStorageService objectStorage,
                                     ModelInferenceClient inferenceClient) {
        this.mapper = mapper;
        this.objectStorage = objectStorage;
        this.inferenceClient = inferenceClient;
    }

    @Override
    public List<ScreeningRecordVO> upload(List<MultipartFile> files, String patientName, Integer patientAge,
                                         String patientGender, String remark) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(400, "请至少上传一张眼底图片");
        }
        AuthPrincipal principal = requirePrincipal();
        List<ScreeningRecordVO> result = new ArrayList<>();
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String id = IdGenerator.nextId();
            String ext = extractExt(file.getOriginalFilename());
            String imageKey = "fundus/" + datePath + "/" + id + ext;
            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException e) {
                log.error("读取上传文件失败：{}", e.getMessage());
                throw new BusinessException(500, "读取上传文件失败");
            }
            objectStorage.upload(imageKey, bytes, file.getContentType());

            ModelPredictResponse predict = inferenceClient.predict(bytes, file.getOriginalFilename());

            String gradCamKey = null;
            try {
                byte[] camBytes = inferenceClient.generateCam(bytes, file.getOriginalFilename());
                gradCamKey = "cam/" + datePath + "/" + id + ".png";
                objectStorage.upload(gradCamKey, camBytes, "image/png");
            } catch (BusinessException e) {
                // 热力图生成失败不阻断主流程，仅记录告警（记录仍落库）
                log.warn("热力图生成失败，跳过（记录仍落库）：{}", e.getMessage());
            }

            BizScreeningRecordEntity entity = new BizScreeningRecordEntity();
            entity.setId(id);
            entity.setUserId(principal.getUsername());
            entity.setPatientName(patientName);
            entity.setPatientAge(patientAge);
            entity.setPatientGender(patientGender);
            entity.setImageKey(imageKey);
            entity.setResultLevel(predict.getResultLevel());
            entity.setConfidence(predict.getConfidence() == null ? null
                    : BigDecimal.valueOf(predict.getConfidence()).setScale(4, RoundingMode.HALF_UP));
            entity.setProbabilities(BizScreeningRecordConvert.toProbabilitiesJson(predict.getProbabilities()));
            entity.setSuggestion(predict.getSuggestion());
            entity.setGradCamKey(gradCamKey);
            entity.setModelVersion(predict.getModelVersion());
            entity.setRemark(remark);
            mapper.insert(entity);

            result.add(toVoSafe(entity));
        }
        return result;
    }

    @Override
    public PageResult<ScreeningRecordVO> page(ScreeningPageQuery query) {
        LambdaQueryWrapper<BizScreeningRecordEntity> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(BizScreeningRecordEntity::getCreateTime);
        long current = query.getCurrent() == null ? 1L : query.getCurrent();
        long size = query.getPageSize() == null ? 10L : query.getPageSize();
        Page<BizScreeningRecordEntity> page = new Page<>(current, size);
        mapper.selectPage(page, wrapper);

        List<ScreeningRecordVO> list = page.getRecords().stream()
                .map(this::toVoSafe)
                .collect(Collectors.toList());
        PageResult<ScreeningRecordVO> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setList(list);
        return result;
    }

    @Override
    public ScreeningRecordVO detail(String id) {
        LambdaQueryWrapper<BizScreeningRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizScreeningRecordEntity::getId, id);
        applyDataScope(wrapper);
        BizScreeningRecordEntity entity = mapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(404, "记录不存在或无权限访问");
        }
        return toVoSafe(entity);
    }

    @Override
    public void remove(String id) {
        LambdaQueryWrapper<BizScreeningRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizScreeningRecordEntity::getId, id);
        applyDataScope(wrapper);
        BizScreeningRecordEntity entity = mapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(404, "记录不存在或无权限访问");
        }
        mapper.deleteById(id);
        // 清理存储对象（失败仅告警，不阻断）
        try {
            if (entity.getImageKey() != null) {
                objectStorage.delete(entity.getImageKey());
            }
            if (entity.getGradCamKey() != null) {
                objectStorage.delete(entity.getGradCamKey());
            }
        } catch (Exception e) {
            log.warn("删除记录关联存储对象失败（已逻辑删除记录）：{}", e.getMessage());
        }
    }

    @Override
    public ScreeningStatisticsVO statistics(ScreeningPageQuery query) {
        LambdaQueryWrapper<BizScreeningRecordEntity> wrapper = buildQueryWrapper(query);
        List<BizScreeningRecordEntity> all = mapper.selectList(wrapper);

        long total = all.size();
        Map<String, Long> levelDistribution = all.stream()
                .filter(e -> e.getResultLevel() != null)
                .collect(Collectors.groupingBy(BizScreeningRecordEntity::getResultLevel, Collectors.counting()));
        Map<String, Long> suggestionDistribution = all.stream()
                .filter(e -> e.getSuggestion() != null)
                .collect(Collectors.groupingBy(BizScreeningRecordEntity::getSuggestion, Collectors.counting()));
        long referralCount = all.stream()
                .filter(e -> "REFERRAL".equals(e.getSuggestion()))
                .count();
        BigDecimal referralRate = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(referralCount).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);

        ScreeningStatisticsVO vo = new ScreeningStatisticsVO();
        vo.setTotal(total);
        vo.setLevelDistribution(levelDistribution);
        vo.setSuggestionDistribution(suggestionDistribution);
        vo.setReferralRate(referralRate);
        vo.setTrend(buildTrend(all));
        return vo;
    }

    @Override
    public void export(List<String> ids, ScreeningPageQuery query, HttpServletResponse response) {
        LambdaQueryWrapper<BizScreeningRecordEntity> wrapper = buildQueryWrapper(query);
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(BizScreeningRecordEntity::getId, ids);
        }
        wrapper.orderByDesc(BizScreeningRecordEntity::getCreateTime);
        List<BizScreeningRecordEntity> list = mapper.selectList(wrapper);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("筛查记录");
            String[] headers = {"患者姓名", "性别", "年龄", "DR分级", "分级说明", "置信度",
                    "转诊建议", "建议说明", "模型版本", "创建时间", "图片链接", "热力图链接"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (BizScreeningRecordEntity e : list) {
                Row row = sheet.createRow(rowIdx++);
                setCell(row, 0, e.getPatientName());
                setCell(row, 1, e.getPatientGender());
                setCell(row, 2, e.getPatientAge() == null ? "" : e.getPatientAge().toString());
                setCell(row, 3, e.getResultLevel());
                setCell(row, 4, BizScreeningRecordConvert.LEVEL_NAMES.get(e.getResultLevel()));
                setCell(row, 5, e.getConfidence() == null ? "" : e.getConfidence().toString());
                setCell(row, 6, e.getSuggestion());
                setCell(row, 7, BizScreeningRecordConvert.SUGGESTION_NAMES.get(e.getSuggestion()));
                setCell(row, 8, e.getModelVersion());
                setCell(row, 9, e.getCreateTime() == null ? "" : e.getCreateTime().format(DATE_TIME_FMT));
                setCell(row, 10, presignSafe(e.getImageKey()));
                setCell(row, 11, presignSafe(e.getGradCamKey()));
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            String filename = "screening_export_" + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("导出 Excel 失败：{}", e.getMessage());
            throw new BusinessException(500, "导出 Excel 失败");
        }
    }

    // ------------------------- 内部工具 -------------------------

    private LambdaQueryWrapper<BizScreeningRecordEntity> buildQueryWrapper(ScreeningPageQuery query) {
        LambdaQueryWrapper<BizScreeningRecordEntity> wrapper = new LambdaQueryWrapper<>();
        applyDataScope(wrapper);
        if (query.getPatientName() != null && !query.getPatientName().isBlank()) {
            wrapper.like(BizScreeningRecordEntity::getPatientName, query.getPatientName());
        }
        if (query.getLevel() != null && !query.getLevel().isBlank()) {
            wrapper.eq(BizScreeningRecordEntity::getResultLevel, query.getLevel());
        }
        if (query.getStartDate() != null && !query.getStartDate().isBlank()) {
            wrapper.ge(BizScreeningRecordEntity::getCreateTime, parseDateTime(query.getStartDate()));
        }
        if (query.getEndDate() != null && !query.getEndDate().isBlank()) {
            wrapper.le(BizScreeningRecordEntity::getCreateTime, parseDateTime(query.getEndDate()));
        }
        return wrapper;
    }

    /**
     * 追加数据权限过滤：SELF → 仅本人（user_id = 当前登录用户名）；ALL → 不限制。
     */
    private void applyDataScope(LambdaQueryWrapper<BizScreeningRecordEntity> wrapper) {
        if (DataScopeUtils.isSelf()) {
            AuthPrincipal principal = AuthContext.get();
            if (principal == null || principal.getUsername() == null) {
                throw new BusinessException(401, "未登录或登录已过期");
            }
            wrapper.eq(BizScreeningRecordEntity::getUserId, principal.getUsername());
        }
    }

    private AuthPrincipal requirePrincipal() {
        AuthPrincipal principal = AuthContext.get();
        if (principal == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return principal;
    }

    private ScreeningRecordVO toVoSafe(BizScreeningRecordEntity entity) {
        String imageUrl = presignSafe(entity.getImageKey());
        String camUrl = presignSafe(entity.getGradCamKey());
        return BizScreeningRecordConvert.toVO(entity, imageUrl, camUrl);
    }

    private String presignSafe(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            return objectStorage.presignUrl(key, PRESIGN_MINUTES);
        } catch (Exception e) {
            log.warn("生成预签名 URL 失败（对象存储可能未启动）：{}", e.getMessage());
            return null;
        }
    }

    private List<DailyCountVO> buildTrend(List<BizScreeningRecordEntity> all) {
        Map<String, Long> actual = all.stream()
                .filter(e -> e.getCreateTime() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getCreateTime().toLocalDate().format(DATE_FMT),
                        Collectors.counting()));
        // 近 30 天（含今日），缺失日期补 0，按日期升序
        Map<String, Long> trendMap = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            String date = today.minusDays(i).format(DATE_FMT);
            trendMap.put(date, actual.getOrDefault(date, 0L));
        }
        List<DailyCountVO> trend = new ArrayList<>();
        trendMap.forEach((date, count) -> trend.add(new DailyCountVO(date, count)));
        return trend;
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FMT);
        } catch (Exception e) {
            throw new BusinessException(400, "时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private static String extractExt(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        String ext = filename.substring(filename.lastIndexOf('.'));
        if (ext.length() > 10) {
            return ".jpg";
        }
        return ext;
    }

    private static void setCell(Row row, int index, String value) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value);
    }
}
