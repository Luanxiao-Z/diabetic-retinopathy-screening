package cn.edu.fzu.drs.module.screening.controller;

import cn.edu.fzu.drs.module.common.constant.PermissionConstants;
import cn.edu.fzu.drs.module.common.result.PageResult;
import cn.edu.fzu.drs.module.common.result.Result;
import cn.edu.fzu.drs.module.screening.dto.ScreeningPageQuery;
import cn.edu.fzu.drs.module.screening.service.ScreeningRecordService;
import cn.edu.fzu.drs.module.screening.vo.PatientFollowUpVO;
import cn.edu.fzu.drs.module.screening.vo.ScreeningRecordVO;
import cn.edu.fzu.drs.module.screening.vo.ScreeningStatisticsVO;
import cn.edu.fzu.drs.module.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 筛查记录接口（阶段4 业务核心）：上传→推理→落库→查询→统计→导出。
 */
@Tag(name = "筛查记录", description = "眼底图上传、推理结果落库、分页/详情/删除、统计与 Excel 导出")
@RestController
@RequestMapping("/api/v1/biz/screening-records")
public class ScreeningRecordController {

    private final ScreeningRecordService screeningRecordService;

    public ScreeningRecordController(ScreeningRecordService screeningRecordService) {
        this.screeningRecordService = screeningRecordService;
    }

    @Operation(summary = "上传眼底图并筛查", description = "上传一张或多张眼底图（共享患者元数据），服务依次完成：上传 MinIO → 模型推理 → 生成热力图 → 落库，返回记录列表。")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(PermissionConstants.BIZ_SCREENING_CREATE)
    public Result<List<ScreeningRecordVO>> upload(
            @Parameter(description = "眼底图片（支持多张）", required = true)
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @Parameter(description = "患者姓名") @RequestParam(value = "patientName", required = false) String patientName,
            @Parameter(description = "患者年龄") @RequestParam(value = "patientAge", required = false) Integer patientAge,
            @Parameter(description = "患者性别（字典 C_GENDER：MALE/FEMALE）") @RequestParam(value = "patientGender", required = false) String patientGender,
            @Parameter(description = "备注") @RequestParam(value = "remark", required = false) String remark) {
        return Result.ok(screeningRecordService.upload(files, patientName, patientAge, patientGender, remark));
    }

    @Operation(summary = "分页查询筛查记录", description = "支持按患者姓名（模糊）、DR 分级、时间范围筛选；数据权限隔离（医生仅本人，管理员全量）。")
    @GetMapping
    @RequirePermission(PermissionConstants.BIZ_SCREENING_VIEW)
    public Result<PageResult<ScreeningRecordVO>> page(@ModelAttribute ScreeningPageQuery query) {
        return Result.ok(screeningRecordService.page(query));
    }

    @Operation(summary = "筛查记录详情", description = "返回单条记录（含图片与热力图预签名 URL）；数据权限隔离。")
    @GetMapping("/{id}")
    @RequirePermission(PermissionConstants.BIZ_SCREENING_VIEW)
    public Result<ScreeningRecordVO> detail(
            @Parameter(description = "记录主键", required = true) @PathVariable("id") String id) {
        return Result.ok(screeningRecordService.detail(id));
    }

    @Operation(summary = "删除筛查记录", description = "逻辑删除并清理存储对象；数据权限隔离（医生仅能删除本人记录，管理员可删除全部）。")
    @DeleteMapping("/{id}")
    @RequirePermission(PermissionConstants.BIZ_SCREENING_DELETE)
    public Result<Void> remove(
            @Parameter(description = "记录主键", required = true) @PathVariable("id") String id) {
        screeningRecordService.remove(id);
        return Result.ok();
    }

    @Operation(summary = "筛查统计", description = "分级分布、转诊建议分布、转诊率、近 30 天趋势；数据权限隔离。")
    @GetMapping("/statistics")
    @RequirePermission(PermissionConstants.BIZ_SCREENING_VIEW)
    public Result<ScreeningStatisticsVO> statistics(@ModelAttribute ScreeningPageQuery query) {
        return Result.ok(screeningRecordService.statistics(query));
    }

    @Operation(summary = "导出筛查记录 Excel", description = "按筛选范围与可选 id 列表导出（本期仅 Excel）；数据权限隔离，导出受限于当前用户可见范围。")
    @GetMapping("/exports")
    @RequirePermission(PermissionConstants.BIZ_SCREENING_EXPORT)
    public void export(
            @Parameter(description = "指定记录 id 列表（可选，缺省导出范围内全部）")
            @RequestParam(value = "ids", required = false) List<String> ids,
            @ModelAttribute ScreeningPageQuery query,
            HttpServletResponse response) {
        screeningRecordService.export(ids, query, response);
    }

    @Operation(summary = "患者随访分页",
            description = "按患者归并历次筛查，返回最近分级、分级变化方向（进展/好转/持平）与复核需求；仅统计填写了患者姓名的记录。")
    @GetMapping("/patients")
    @RequirePermission(PermissionConstants.BIZ_SCREENING_VIEW)
    public Result<PageResult<PatientFollowUpVO>> pagePatients(@ModelAttribute ScreeningPageQuery query) {
        return Result.ok(screeningRecordService.pagePatients(query));
    }

    @Operation(summary = "人工复核确认",
            description = "将置信度低于阈值的记录标记为已复核，记录复核人、时间与意见；已复核或无需复核的记录会被拒绝。")
    @PatchMapping("/{id}/review")
    @RequirePermission(PermissionConstants.BIZ_SCREENING_REVIEW)
    public Result<ScreeningRecordVO> review(
            @Parameter(description = "记录 id") @PathVariable("id") String id,
            @Parameter(description = "复核意见（选填）")
            @RequestParam(value = "remark", required = false) String remark) {
        return Result.ok(screeningRecordService.review(id, remark));
    }

    @Operation(summary = "模型元信息与训练指标",
            description = "转发模型服务 /model/info：模型版本、骨干、推理设备、是否已训练，以及训练指标（准确率、宏平均 F1、逐类 F1）。")
    @GetMapping("/model-info")
    @RequirePermission(PermissionConstants.BIZ_SCREENING_VIEW)
    public Result<java.util.Map<String, Object>> modelInfo() {
        return Result.ok(screeningRecordService.modelInfo());
    }
}
