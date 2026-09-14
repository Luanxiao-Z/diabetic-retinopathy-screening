package cn.edu.fzu.drs.module.system.controller;

import cn.edu.fzu.drs.module.common.result.Result;
import cn.edu.fzu.drs.module.security.annotation.RequirePermission;
import cn.edu.fzu.drs.module.system.dto.DictDomainCreateDTO;
import cn.edu.fzu.drs.module.system.dto.DictDomainUpdateDTO;
import cn.edu.fzu.drs.module.system.dto.DictItemCreateDTO;
import cn.edu.fzu.drs.module.system.dto.DictItemUpdateDTO;
import cn.edu.fzu.drs.module.system.service.AdminDictService;
import cn.edu.fzu.drs.module.system.vo.DictDomainVO;
import cn.edu.fzu.drs.module.system.vo.DictItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典管理接口（场景3：认证 + 功能权限 admin:dict:*）。
 */
@Tag(name = "字典管理", description = "字典域与字典项的维护（需 admin:dict:* 权限）")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminDictController {

    private final AdminDictService dictService;

    public AdminDictController(AdminDictService dictService) {
        this.dictService = dictService;
    }

    @Operation(summary = "字典域列表")
    @GetMapping("/dict-domains")
    @RequirePermission("admin:dict:view")
    public Result<List<DictDomainVO>> listDomains() {
        return Result.ok(dictService.listDomains());
    }

    @Operation(summary = "新增字典域")
    @PostMapping("/dict-domains")
    @RequirePermission("admin:dict:edit")
    public ResponseEntity<Result<Void>> createDomain(@Valid @RequestBody DictDomainCreateDTO dto) {
        dictService.createDomain(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.ok());
    }

    @Operation(summary = "修改字典域")
    @PutMapping("/dict-domains/{domainCode}")
    @RequirePermission("admin:dict:edit")
    public Result<Void> updateDomain(@PathVariable("domainCode") String domainCode,
                                      @Valid @RequestBody DictDomainUpdateDTO dto) {
        dictService.updateDomain(domainCode, dto);
        return Result.ok();
    }

    @Operation(summary = "删除字典域（级联删除其下字典项）")
    @DeleteMapping("/dict-domains/{domainCode}")
    @RequirePermission("admin:dict:edit")
    public Result<Void> deleteDomain(@PathVariable("domainCode") String domainCode) {
        dictService.deleteDomain(domainCode);
        return Result.ok();
    }

    @Operation(summary = "字典项列表（按 sort 升序）")
    @GetMapping("/dict-domains/{domainCode}/items")
    @RequirePermission("admin:dict:view")
    public Result<List<DictItemVO>> listItems(@PathVariable("domainCode") String domainCode) {
        return Result.ok(dictService.listItems(domainCode));
    }

    @Operation(summary = "新增字典项")
    @PostMapping("/dict-domains/{domainCode}/items")
    @RequirePermission("admin:dict:edit")
    public ResponseEntity<Result<Void>> createItem(@PathVariable("domainCode") String domainCode,
                                                   @Valid @RequestBody DictItemCreateDTO dto) {
        dictService.createItem(domainCode, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.ok());
    }

    @Operation(summary = "修改字典项")
    @PutMapping("/dict-domains/{domainCode}/items/{itemCode}")
    @RequirePermission("admin:dict:edit")
    public Result<Void> updateItem(@PathVariable("domainCode") String domainCode,
                                   @PathVariable("itemCode") String itemCode,
                                   @Valid @RequestBody DictItemUpdateDTO dto) {
        dictService.updateItem(domainCode, itemCode, dto);
        return Result.ok();
    }

    @Operation(summary = "删除字典项")
    @DeleteMapping("/dict-domains/{domainCode}/items/{itemCode}")
    @RequirePermission("admin:dict:edit")
    public Result<Void> deleteItem(@PathVariable("domainCode") String domainCode,
                                   @PathVariable("itemCode") String itemCode) {
        dictService.deleteItem(domainCode, itemCode);
        return Result.ok();
    }
}
