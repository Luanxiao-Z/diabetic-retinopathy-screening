package cn.edu.fzu.drs.module.screening.service;

import cn.edu.fzu.drs.module.common.result.PageResult;
import cn.edu.fzu.drs.module.screening.dto.ScreeningPageQuery;
import cn.edu.fzu.drs.module.screening.vo.ScreeningRecordVO;
import cn.edu.fzu.drs.module.screening.vo.ScreeningStatisticsVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 筛查记录业务接口：上传（推理 + 落库）、分页、详情、删除、统计、导出。
 * <p>数据权限：DOCTOR=SELF（仅本人记录），ADMIN=ALL（全量），由 Service 层依据登录主体拼接过滤。</p>
 */
public interface ScreeningRecordService {

    /**
     * 批量上传眼底图：逐张上传 MinIO → 调用模型推理 → 生成热力图 → 落库。
     * 每张图生成一条记录（共享传入的患者元数据）。
     *
     * @return 已创建的记录 VO 列表（含预签名 URL）
     */
    List<ScreeningRecordVO> upload(List<MultipartFile> files, String patientName, Integer patientAge,
                                   String patientGender, String remark);

    /**
     * 分页查询（含数据权限与条件过滤）。
     */
    PageResult<ScreeningRecordVO> page(ScreeningPageQuery query);

    /**
     * 详情查询（数据权限隔离）。
     */
    ScreeningRecordVO detail(String id);

    /**
     * 删除记录（逻辑删除 + 清理存储对象）。删除受数据权限约束。
     */
    void remove(String id);

    /**
     * 统计（分级分布、建议分布、转诊率、趋势）。
     */
    ScreeningStatisticsVO statistics(ScreeningPageQuery query);

    /**
     * 导出 Excel（依据筛选范围与可选 id 列表，受数据权限约束）。
     */
    void export(List<String> ids, ScreeningPageQuery query, HttpServletResponse response);
}
