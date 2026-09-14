package cn.edu.fzu.drs.module.system.service;

import cn.edu.fzu.drs.module.system.dto.DictDomainCreateDTO;
import cn.edu.fzu.drs.module.system.dto.DictDomainUpdateDTO;
import cn.edu.fzu.drs.module.system.dto.DictItemCreateDTO;
import cn.edu.fzu.drs.module.system.dto.DictItemUpdateDTO;
import cn.edu.fzu.drs.module.system.vo.DictDomainVO;
import cn.edu.fzu.drs.module.system.vo.DictItemVO;

import java.util.List;

/**
 * 字典管理（场景3：需 admin:dict:* 权限）。
 */
public interface AdminDictService {

    /**
     * 字典域列表。
     */
    List<DictDomainVO> listDomains();

    /**
     * 新增字典域。
     */
    void createDomain(DictDomainCreateDTO dto);

    /**
     * 修改字典域。
     */
    void updateDomain(String domainCode, DictDomainUpdateDTO dto);

    /**
     * 删除字典域（级联逻辑删除其下字典项）。
     */
    void deleteDomain(String domainCode);

    /**
     * 指定域的字典项列表（按 sort 升序）。
     */
    List<DictItemVO> listItems(String domainCode);

    /**
     * 新增字典项。
     */
    void createItem(String domainCode, DictItemCreateDTO dto);

    /**
     * 修改字典项。
     */
    void updateItem(String domainCode, String itemCode, DictItemUpdateDTO dto);

    /**
     * 删除字典项。
     */
    void deleteItem(String domainCode, String itemCode);
}
