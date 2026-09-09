package cn.edu.fzu.drs.module.system.controller;

import cn.edu.fzu.drs.module.common.result.Result;
import cn.edu.fzu.drs.module.system.service.DictService;
import cn.edu.fzu.drs.module.system.vo.DictDomainVO;
import cn.edu.fzu.drs.module.system.vo.DictItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典接口：查询字典项与字典域（仅需登录令牌）。
 */
@Tag(name = "字典管理", description = "字典域与字典项的查询")
@RestController
@RequestMapping("/api/v1/common")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @Operation(summary = "查询字典项", description = "按字典域编码返回字典项列表（按 sort 升序）")
    @GetMapping("/dicts/{domainCode}/items")
    public Result<List<DictItemVO>> listItems(
            @Parameter(description = "字典域编码，如 B_DR_LEVEL", required = true)
            @PathVariable("domainCode") String domainCode) {
        return Result.ok(dictService.listItems(domainCode));
    }

    @Operation(summary = "查询字典域", description = "返回全部字典域")
    @GetMapping("/dicts")
    public Result<List<DictDomainVO>> listDomains() {
        return Result.ok(dictService.listDomains());
    }
}
