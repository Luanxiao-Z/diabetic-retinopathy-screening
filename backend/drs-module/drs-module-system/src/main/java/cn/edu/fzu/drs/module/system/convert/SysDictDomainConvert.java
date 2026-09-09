package cn.edu.fzu.drs.module.system.convert;

import cn.edu.fzu.drs.module.system.entity.SysDictDomainEntity;
import cn.edu.fzu.drs.module.system.vo.DictDomainVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典域 Entity ↔ VO 转换（手写）。
 */
public final class SysDictDomainConvert {

    private SysDictDomainConvert() {
    }

    public static DictDomainVO toVO(SysDictDomainEntity entity) {
        if (entity == null) {
            return null;
        }
        return new DictDomainVO(entity.getDomainCode(), entity.getDomainName(), entity.getRemark());
    }

    public static List<DictDomainVO> toVOList(List<SysDictDomainEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(SysDictDomainConvert::toVO).collect(Collectors.toList());
    }
}
