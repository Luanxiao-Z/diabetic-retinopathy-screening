package cn.edu.fzu.drs.module.system.convert;

import cn.edu.fzu.drs.module.system.entity.SysDictItemEntity;
import cn.edu.fzu.drs.module.system.vo.DictItemVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典项 Entity ↔ VO 转换（手写，避免引入 MapStruct 处理器）。
 */
public final class SysDictItemConvert {

    private SysDictItemConvert() {
    }

    public static DictItemVO toVO(SysDictItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return new DictItemVO(
                entity.getDomainCode(),
                entity.getItemCode(),
                entity.getItemName(),
                entity.getItemValue(),
                entity.getSort()
        );
    }

    public static List<DictItemVO> toVOList(List<SysDictItemEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(SysDictItemConvert::toVO).collect(Collectors.toList());
    }
}
