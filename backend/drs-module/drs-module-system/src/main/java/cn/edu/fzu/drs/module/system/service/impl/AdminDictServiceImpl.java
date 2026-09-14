package cn.edu.fzu.drs.module.system.service.impl;

import cn.edu.fzu.drs.module.common.exception.BusinessException;
import cn.edu.fzu.drs.module.system.convert.SysDictDomainConvert;
import cn.edu.fzu.drs.module.system.convert.SysDictItemConvert;
import cn.edu.fzu.drs.module.system.dto.DictDomainCreateDTO;
import cn.edu.fzu.drs.module.system.dto.DictDomainUpdateDTO;
import cn.edu.fzu.drs.module.system.dto.DictItemCreateDTO;
import cn.edu.fzu.drs.module.system.dto.DictItemUpdateDTO;
import cn.edu.fzu.drs.module.system.entity.SysDictDomainEntity;
import cn.edu.fzu.drs.module.system.entity.SysDictItemEntity;
import cn.edu.fzu.drs.module.system.mapper.SysDictDomainMapper;
import cn.edu.fzu.drs.module.system.mapper.SysDictItemMapper;
import cn.edu.fzu.drs.module.system.service.AdminDictService;
import cn.edu.fzu.drs.module.system.vo.DictDomainVO;
import cn.edu.fzu.drs.module.system.vo.DictItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 字典管理实现。
 */
@Service
public class AdminDictServiceImpl implements AdminDictService {

    private final SysDictDomainMapper domainMapper;
    private final SysDictItemMapper itemMapper;

    public AdminDictServiceImpl(SysDictDomainMapper domainMapper, SysDictItemMapper itemMapper) {
        this.domainMapper = domainMapper;
        this.itemMapper = itemMapper;
    }

    @Override
    public List<DictDomainVO> listDomains() {
        List<SysDictDomainEntity> list = domainMapper.selectList(
                new LambdaQueryWrapper<SysDictDomainEntity>().orderByAsc(SysDictDomainEntity::getDomainCode));
        return SysDictDomainConvert.toVOList(list);
    }

    @Override
    public void createDomain(DictDomainCreateDTO dto) {
        if (!StringUtils.hasText(dto.getDomainCode())) {
            throw new BusinessException(400, "字典域编码不能为空");
        }
        if (!dto.getDomainCode().matches("[A-Za-z0-9_]+")) {
            throw new BusinessException(400, "编码仅限字母、数字、下划线");
        }
        if (!StringUtils.hasText(dto.getDomainName())) {
            throw new BusinessException(400, "字典域名称不能为空");
        }
        Long count = domainMapper.selectCount(
                new LambdaQueryWrapper<SysDictDomainEntity>().eq(SysDictDomainEntity::getDomainCode, dto.getDomainCode()));
        if (count != null && count > 0) {
            throw new BusinessException(409, "字典域编码已存在");
        }
        SysDictDomainEntity entity = new SysDictDomainEntity();
        entity.setDomainCode(dto.getDomainCode());
        entity.setDomainName(dto.getDomainName());
        entity.setRemark(dto.getRemark());
        domainMapper.insert(entity);
    }

    @Override
    public void updateDomain(String domainCode, DictDomainUpdateDTO dto) {
        SysDictDomainEntity entity = domainMapper.selectOne(
                new LambdaQueryWrapper<SysDictDomainEntity>().eq(SysDictDomainEntity::getDomainCode, domainCode));
        if (entity == null) {
            throw new BusinessException(404, "字典域不存在");
        }
        if (StringUtils.hasText(dto.getDomainName())) {
            entity.setDomainName(dto.getDomainName());
        }
        if (StringUtils.hasText(dto.getRemark())) {
            entity.setRemark(dto.getRemark());
        }
        domainMapper.updateById(entity);
    }

    @Override
    public void deleteDomain(String domainCode) {
        SysDictDomainEntity entity = domainMapper.selectOne(
                new LambdaQueryWrapper<SysDictDomainEntity>().eq(SysDictDomainEntity::getDomainCode, domainCode));
        if (entity == null) {
            throw new BusinessException(404, "字典域不存在");
        }
        // 级联逻辑删除该域下所有字典项（全局逻辑删除配置生效）
        itemMapper.delete(new LambdaQueryWrapper<SysDictItemEntity>().eq(SysDictItemEntity::getDomainCode, domainCode));
        domainMapper.deleteById(entity.getId());
    }

    @Override
    public List<DictItemVO> listItems(String domainCode) {
        List<SysDictItemEntity> list = itemMapper.selectList(
                new LambdaQueryWrapper<SysDictItemEntity>()
                        .eq(SysDictItemEntity::getDomainCode, domainCode)
                        .orderByAsc(SysDictItemEntity::getSort));
        return SysDictItemConvert.toVOList(list);
    }

    @Override
    public void createItem(String domainCode, DictItemCreateDTO dto) {
        SysDictDomainEntity domain = domainMapper.selectOne(
                new LambdaQueryWrapper<SysDictDomainEntity>().eq(SysDictDomainEntity::getDomainCode, domainCode));
        if (domain == null) {
            throw new BusinessException(404, "字典域不存在");
        }
        if (!StringUtils.hasText(dto.getItemCode())) {
            throw new BusinessException(400, "字典项编码不能为空");
        }
        if (!dto.getItemCode().matches("[A-Za-z0-9_]+")) {
            throw new BusinessException(400, "编码仅限字母、数字、下划线");
        }
        if (!StringUtils.hasText(dto.getItemName())) {
            throw new BusinessException(400, "字典项名称不能为空");
        }
        Long count = itemMapper.selectCount(new LambdaQueryWrapper<SysDictItemEntity>()
                .eq(SysDictItemEntity::getDomainCode, domainCode)
                .eq(SysDictItemEntity::getItemCode, dto.getItemCode()));
        if (count != null && count > 0) {
            throw new BusinessException(409, "字典项编码在该域内已存在");
        }
        SysDictItemEntity entity = new SysDictItemEntity();
        entity.setDomainCode(domainCode);
        entity.setItemCode(dto.getItemCode());
        entity.setItemName(dto.getItemName());
        entity.setItemValue(dto.getItemValue());
        entity.setSort(dto.getSort());
        entity.setRemark(dto.getRemark());
        itemMapper.insert(entity);
    }

    @Override
    public void updateItem(String domainCode, String itemCode, DictItemUpdateDTO dto) {
        SysDictItemEntity entity = itemMapper.selectOne(new LambdaQueryWrapper<SysDictItemEntity>()
                .eq(SysDictItemEntity::getDomainCode, domainCode)
                .eq(SysDictItemEntity::getItemCode, itemCode));
        if (entity == null) {
            throw new BusinessException(404, "字典项不存在");
        }
        if (StringUtils.hasText(dto.getItemName())) {
            entity.setItemName(dto.getItemName());
        }
        if (dto.getItemValue() != null) {
            entity.setItemValue(dto.getItemValue());
        }
        if (dto.getSort() != null) {
            entity.setSort(dto.getSort());
        }
        if (StringUtils.hasText(dto.getRemark())) {
            entity.setRemark(dto.getRemark());
        }
        itemMapper.updateById(entity);
    }

    @Override
    public void deleteItem(String domainCode, String itemCode) {
        SysDictItemEntity entity = itemMapper.selectOne(new LambdaQueryWrapper<SysDictItemEntity>()
                .eq(SysDictItemEntity::getDomainCode, domainCode)
                .eq(SysDictItemEntity::getItemCode, itemCode));
        if (entity == null) {
            throw new BusinessException(404, "字典项不存在");
        }
        itemMapper.deleteById(entity.getId());
    }
}
