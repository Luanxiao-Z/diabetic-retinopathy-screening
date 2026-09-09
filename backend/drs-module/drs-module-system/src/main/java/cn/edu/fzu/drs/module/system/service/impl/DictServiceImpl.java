package cn.edu.fzu.drs.module.system.service.impl;

import cn.edu.fzu.drs.module.system.convert.SysDictDomainConvert;
import cn.edu.fzu.drs.module.system.convert.SysDictItemConvert;
import cn.edu.fzu.drs.module.system.entity.SysDictDomainEntity;
import cn.edu.fzu.drs.module.system.entity.SysDictItemEntity;
import cn.edu.fzu.drs.module.system.mapper.SysDictDomainMapper;
import cn.edu.fzu.drs.module.system.mapper.SysDictItemMapper;
import cn.edu.fzu.drs.module.system.service.DictService;
import cn.edu.fzu.drs.module.system.vo.DictDomainVO;
import cn.edu.fzu.drs.module.system.vo.DictItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典服务实现。
 */
@Service
public class DictServiceImpl implements DictService {

    private final SysDictItemMapper dictItemMapper;
    private final SysDictDomainMapper dictDomainMapper;

    public DictServiceImpl(SysDictItemMapper dictItemMapper, SysDictDomainMapper dictDomainMapper) {
        this.dictItemMapper = dictItemMapper;
        this.dictDomainMapper = dictDomainMapper;
    }

    @Override
    public List<DictItemVO> listItems(String domainCode) {
        List<SysDictItemEntity> items = dictItemMapper.selectList(
                new LambdaQueryWrapper<SysDictItemEntity>()
                        .eq(SysDictItemEntity::getDomainCode, domainCode)
                        .orderByAsc(SysDictItemEntity::getSort));
        return SysDictItemConvert.toVOList(items);
    }

    @Override
    public List<DictDomainVO> listDomains() {
        List<SysDictDomainEntity> domains = dictDomainMapper.selectList(
                new LambdaQueryWrapper<SysDictDomainEntity>().orderByAsc(SysDictDomainEntity::getDomainCode));
        return SysDictDomainConvert.toVOList(domains);
    }
}
