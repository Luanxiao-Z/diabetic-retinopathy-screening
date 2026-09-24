package cn.edu.fzu.drs.module.screening.service.impl;

import cn.edu.fzu.drs.module.common.audit.OperationLogRecorder;
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
import cn.edu.fzu.drs.module.screening.vo.PatientFollowUpVO;
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
import java.util.Comparator;
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
    private final OperationLogRecorder audit;

    public ScreeningRecordServiceImpl(BizScreeningRecordMapper mapper,
                                     ObjectStorageService objectStorage,
                                     ModelInferenceClient inferenceClient,
                                     OperationLogRecorder audit) {
        this.mapper = mapper;
        this.objectStorage = objectStorage;
        this.inferenceClient = inferenceClient;
        this.audit = audit;
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
        long startedAt = System.currentTimeMillis();

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
        audit.record(OperationLogRecorder.MODULE_SCREENING, OperationLogRecorder.ACTION_UPLOAD,
                (patientName == null || patientName.isBlank() ? "未登记患者" : patientName)
                        + " · " + result.size() + " 张",
                true, null, System.currentTimeMillis() - startedAt);
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
        audit.record(OperationLogRecorder.MODULE_SCREENING, OperationLogRecorder.ACTION_DELETE,
                (entity.getPatientName() == null || entity.getPatientName().isBlank()
                        ? "未登记患者" : entity.getPatientName()) + " · " + id,
                true, null, 0L);
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
        // 环比：近 30 天 vs 前 30 天（与上方筛选条件同一口径）
        LocalDate today = LocalDate.now();
        LocalDateTime from30 = today.minusDays(29).atStartOfDay();
        LocalDateTime toToday = today.plusDays(1).atStartOfDay().minusNanos(1);
        LocalDateTime from60 = today.minusDays(59).atStartOfDay();
        LocalDateTime to30 = today.minusDays(29).atStartOfDay().minusNanos(1);
        long recentTotal = countInRange(query, from30, toToday);
        long prevTotal = countInRange(query, from60, to30);
        BigDecimal growthRate = prevTotal == 0
                ? (recentTotal == 0 ? BigDecimal.ZERO : BigDecimal.ONE)
                : BigDecimal.valueOf(recentTotal - prevTotal)
                        .divide(BigDecimal.valueOf(prevTotal), 4, RoundingMode.HALF_UP);

        vo.setRecentTotal(recentTotal);
        vo.setPrevTotal(prevTotal);
        vo.setGrowthRate(growthRate);

        vo.setNeedReviewCount(all.stream()
                .filter(e -> BizScreeningRecordConvert.needReview(e.getConfidence()))
                .filter(e -> !"CONFIRMED".equals(e.getReviewStatus()))
                .count());
        vo.setReviewThreshold(BizScreeningRecordConvert.REVIEW_CONFIDENCE_THRESHOLD);
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
                setCell(row, 4, BizScreeningRecordConvert.nameOf(BizScreeningRecordConvert.LEVEL_NAMES, e.getResultLevel()));
                setCell(row, 5, e.getConfidence() == null ? "" : e.getConfidence().toString());
                setCell(row, 6, e.getSuggestion());
                setCell(row, 7, BizScreeningRecordConvert.nameOf(BizScreeningRecordConvert.SUGGESTION_NAMES, e.getSuggestion()));
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
        audit.record(OperationLogRecorder.MODULE_SCREENING, OperationLogRecorder.ACTION_EXPORT,
                "导出 " + list.size() + " 条记录", true, null, 0L);
    }

    @Override
    public PageResult<PatientFollowUpVO> pagePatients(ScreeningPageQuery query) {
        // 随访只针对有患者标识的记录；数据权限与姓名模糊过滤在此生效
        LambdaQueryWrapper<BizScreeningRecordEntity> wrapper = new LambdaQueryWrapper<>();
        applyDataScope(wrapper);
        wrapper.isNotNull(BizScreeningRecordEntity::getPatientName)
                .ne(BizScreeningRecordEntity::getPatientName, "");
        if (query.getPatientName() != null && !query.getPatientName().isBlank()) {
            wrapper.like(BizScreeningRecordEntity::getPatientName, query.getPatientName());
        }
        wrapper.orderByAsc(BizScreeningRecordEntity::getCreateTime);

        List<BizScreeningRecordEntity> all = mapper.selectList(wrapper);

        Map<String, List<BizScreeningRecordEntity>> grouped = all.stream()
                .collect(Collectors.groupingBy(BizScreeningRecordEntity::getPatientName,
                        LinkedHashMap::new, Collectors.toList()));

        List<PatientFollowUpVO> patients = grouped.entrySet().stream()
                .map(e -> toFollowUp(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(PatientFollowUpVO::getLatestTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        long current = query.getCurrent() == null ? 1L : Math.max(1L, query.getCurrent());
        long size = query.getPageSize() == null ? 10L : Math.max(1L, query.getPageSize());
        int from = (int) Math.min((current - 1) * size, patients.size());
        int to = (int) Math.min(from + size, patients.size());

        PageResult<PatientFollowUpVO> result = new PageResult<>();
        result.setTotal(patients.size());
        result.setCurrent(current);
        result.setPageSize(size);
        result.setList(new ArrayList<>(patients.subList(from, to)));
        return result;
    }

    @Override
    public ScreeningRecordVO review(String id, String remark) {
        LambdaQueryWrapper<BizScreeningRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizScreeningRecordEntity::getId, id);
        applyDataScope(wrapper);
        BizScreeningRecordEntity entity = mapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(404, "记录不存在或无权限访问");
        }
        if (!BizScreeningRecordConvert.needReview(entity.getConfidence())) {
            throw new BusinessException(400, "该记录置信度已达阈值，无需人工复核");
        }
        if ("CONFIRMED".equals(entity.getReviewStatus())) {
            throw new BusinessException(400, "该记录已完成人工复核");
        }
        AuthPrincipal principal = requirePrincipal();
        entity.setReviewStatus("CONFIRMED");
        entity.setReviewer(principal.getUsername());
        entity.setReviewTime(LocalDateTime.now());
        entity.setReviewRemark(remark);
        mapper.updateById(entity);

        audit.record(OperationLogRecorder.MODULE_SCREENING, OperationLogRecorder.ACTION_REVIEW,
                (entity.getPatientName() == null || entity.getPatientName().isBlank()
                        ? "未登记患者" : entity.getPatientName())
                        + " · " + entity.getResultLevel() + " · 置信度 " + entity.getConfidence(),
                true, null, 0L);
        return toVoSafe(entity);
    }

    /** 单患者聚合：最近/上次分级、变化方向、复核需求 */    private PatientFollowUpVO toFollowUp(String patientName, List<BizScreeningRecordEntity> records) {
        PatientFollowUpVO vo = new PatientFollowUpVO();
        vo.setPatientName(patientName);
        vo.setTotalCount((long) records.size());
        vo.setFirstTime(records.get(0).getCreateTime());

        BizScreeningRecordEntity latest = records.get(records.size() - 1);
        vo.setLatestTime(latest.getCreateTime());
        vo.setPatientGender(latest.getPatientGender());
        vo.setPatientAge(latest.getPatientAge());
        vo.setLatestLevel(latest.getResultLevel());
        vo.setLatestLevelName(BizScreeningRecordConvert.nameOf(BizScreeningRecordConvert.LEVEL_NAMES, latest.getResultLevel()));
        vo.setLatestConfidence(latest.getConfidence());
        vo.setLatestSuggestion(latest.getSuggestion());
        vo.setLatestSuggestionName(BizScreeningRecordConvert.nameOf(BizScreeningRecordConvert.SUGGESTION_NAMES, latest.getSuggestion()));

        vo.setNeedReviewCount(records.stream()
                .filter(r -> BizScreeningRecordConvert.needReview(r.getConfidence()))
                .filter(r -> !"CONFIRMED".equals(r.getReviewStatus()))
                .count());

        if (records.size() >= 2) {
            BizScreeningRecordEntity previous = records.get(records.size() - 2);
            vo.setPreviousLevel(previous.getResultLevel());
            vo.setPreviousLevelName(BizScreeningRecordConvert.nameOf(BizScreeningRecordConvert.LEVEL_NAMES, previous.getResultLevel()));
            int latestIdx = levelIndex(latest.getResultLevel());
            int prevIdx = levelIndex(previous.getResultLevel());
            if (latestIdx < 0 || prevIdx < 0) {
                vo.setLevelChanged(null);
                vo.setTrendDirection("UNKNOWN");
            } else {
                vo.setLevelChanged(latestIdx != prevIdx);
                vo.setTrendDirection(latestIdx > prevIdx ? "UP" : latestIdx < prevIdx ? "DOWN" : "SAME");
            }
        } else {
            vo.setLevelChanged(null);
            vo.setTrendDirection("FIRST");
        }
        return vo;
    }

    /** LEVEL_n → n；无法识别返回 -1 */
    private static int levelIndex(String level) {
        if (level == null || !level.startsWith("LEVEL_")) {
            return -1;
        }
        try {
            return Integer.parseInt(level.substring("LEVEL_".length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ------------------------- 内部工具 -------------------------

    private LambdaQueryWrapper<BizScreeningRecordEntity> buildQueryWrapper(ScreeningPageQuery query) {
        LambdaQueryWrapper<BizScreeningRecordEntity> wrapper = applyFilters(query);
        if (query.getStartDate() != null && !query.getStartDate().isBlank()) {
            wrapper.ge(BizScreeningRecordEntity::getCreateTime, parseDateTime(query.getStartDate()));
        }
        if (query.getEndDate() != null && !query.getEndDate().isBlank()) {
            wrapper.le(BizScreeningRecordEntity::getCreateTime, parseDateTime(query.getEndDate()));
        }
        return wrapper;
    }

    /** 条件筛选（不含时间范围）：数据权限 + 患者 + 分级 + 复核 + 精确患者 */
    private LambdaQueryWrapper<BizScreeningRecordEntity> applyFilters(ScreeningPageQuery query) {
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
        // 人工复核过滤：true → 低置信度且尚未确认复核；false → 置信度达标
        if (query.getNeedReview() != null) {
            if (query.getNeedReview()) {
                wrapper.lt(BizScreeningRecordEntity::getConfidence,
                                BizScreeningRecordConvert.REVIEW_CONFIDENCE_THRESHOLD)
                        .and(w -> w.isNull(BizScreeningRecordEntity::getReviewStatus)
                                .or().eq(BizScreeningRecordEntity::getReviewStatus, "PENDING"));
            } else {
                wrapper.ge(BizScreeningRecordEntity::getConfidence,
                        BizScreeningRecordConvert.REVIEW_CONFIDENCE_THRESHOLD);
            }
        }
        // 随访时间线：按患者精确匹配（与模糊匹配的 patientName 区分）
        if (query.getExactPatientName() != null && !query.getExactPatientName().isBlank()) {
            wrapper.eq(BizScreeningRecordEntity::getPatientName, query.getExactPatientName());
        }
        return wrapper;
    }

    /** 统计指定时间区间内的记录数（复用同一套筛选条件，保证环比口径一致） */
    private long countInRange(ScreeningPageQuery query, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<BizScreeningRecordEntity> wrapper = applyFilters(query);
        wrapper.ge(BizScreeningRecordEntity::getCreateTime, from);
        wrapper.le(BizScreeningRecordEntity::getCreateTime, to);
        Long count = mapper.selectCount(wrapper);
        return count == null ? 0L : count;
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
