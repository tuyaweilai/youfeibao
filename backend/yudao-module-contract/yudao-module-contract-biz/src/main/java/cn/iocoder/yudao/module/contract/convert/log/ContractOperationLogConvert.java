package cn.iocoder.yudao.module.contract.convert.log;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.log.vo.ContractOperationLogRespVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.log.ContractOperationLogDO;
import cn.iocoder.yudao.module.contract.enums.ContractOperationTypeEnum;
import cn.iocoder.yudao.module.contract.enums.ContractStatusEnum;
import cn.iocoder.yudao.module.contract.util.ContractUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 合同操作日志 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractOperationLogConvert {

    ContractOperationLogConvert INSTANCE = Mappers.getMapper(ContractOperationLogConvert.class);

    @Mapping(target = "operationTypeDesc", ignore = true)
    @Mapping(target = "oldStatusDesc", ignore = true) 
    @Mapping(target = "newStatusDesc", ignore = true)
    ContractOperationLogRespVO convert(ContractOperationLogDO bean);

    List<ContractOperationLogRespVO> convertList(List<ContractOperationLogDO> list);

    PageResult<ContractOperationLogRespVO> convertPage(PageResult<ContractOperationLogDO> page);

    /**
     * 补充枚举值描述
     */
    @AfterMapping
    default void afterMapping(ContractOperationLogDO bean, @MappingTarget ContractOperationLogRespVO respVO) {
        // 设置操作类型描述
        for (ContractOperationTypeEnum typeEnum : ContractOperationTypeEnum.values()) {
            if (typeEnum.getType().equals(bean.getOperationType())) {
                respVO.setOperationTypeDesc(typeEnum.getDescription());
                break;
            }
        }
        
        // 设置状态描述
        if (bean.getOldStatus() != null) {
            respVO.setOldStatusDesc(ContractUtils.getStatusDescription(bean.getOldStatus()));
        }
        if (bean.getNewStatus() != null) {
            respVO.setNewStatusDesc(ContractUtils.getStatusDescription(bean.getNewStatus()));
        }
    }

} 