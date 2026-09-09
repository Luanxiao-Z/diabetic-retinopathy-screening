package cn.edu.fzu.drs.module.system.service;

import cn.edu.fzu.drs.module.system.vo.DictDomainVO;
import cn.edu.fzu.drs.module.system.vo.DictItemVO;

import java.util.List;

/**
 * 字典服务：查询字典域与字典项。
 */
public interface DictService {

    /**
     * 按字典域编码查询字典项（按 sort 升序）。
     */
    List<DictItemVO> listItems(String domainCode);

    /**
     * 查询全部字典域。
     */
    List<DictDomainVO> listDomains();
}
