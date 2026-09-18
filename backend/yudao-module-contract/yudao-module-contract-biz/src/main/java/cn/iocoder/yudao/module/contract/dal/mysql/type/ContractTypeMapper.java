package cn.iocoder.yudao.module.contract.dal.mysql.type;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.contract.controller.admin.type.vo.ContractTypePageReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.type.ContractTypeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同类型 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractTypeMapper extends BaseMapperX<ContractTypeDO> {

    default PageResult<ContractTypeDO> selectPage(ContractTypePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ContractTypeDO>()
                .likeIfPresent(ContractTypeDO::getCode, reqVO.getCode())
                .likeIfPresent(ContractTypeDO::getName, reqVO.getName())
                .eqIfPresent(ContractTypeDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ContractTypeDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ContractTypeDO::getId));
    }

    default ContractTypeDO selectByCode(String code) {
        return selectOne(ContractTypeDO::getCode, code);
    }

} 