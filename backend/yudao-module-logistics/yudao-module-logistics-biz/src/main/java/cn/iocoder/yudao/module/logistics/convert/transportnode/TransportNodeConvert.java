package cn.iocoder.yudao.module.logistics.convert.transportnode;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.*;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.TransportNodeDO;
import cn.iocoder.yudao.module.logistics.enums.TransportNodeTypeEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 物流运输节点记录 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface TransportNodeConvert {

    TransportNodeConvert INSTANCE = Mappers.getMapper(TransportNodeConvert.class);

    @Mapping(target = "nodeTypeName", source = "nodeType", qualifiedByName = "nodeTypeToName")
    TransportNodeRespVO convert(TransportNodeDO bean);

    @Mapping(target = "nodeTypeName", source = "nodeType", qualifiedByName = "nodeTypeToName")
    TransportNodeExcelVO convertExcel(TransportNodeDO bean);

    List<TransportNodeRespVO> convertList(List<TransportNodeDO> list);

    List<TransportNodeExcelVO> convertExcelList(List<TransportNodeDO> list);

    PageResult<TransportNodeRespVO> convertPage(PageResult<TransportNodeDO> page);

    @Named("nodeTypeToName")
    default String nodeTypeToName(Integer nodeType) {
        if (nodeType == null) {
            return null;
        }
        for (TransportNodeTypeEnum typeEnum : TransportNodeTypeEnum.values()) {
            if (typeEnum.getType().equals(nodeType)) {
                return typeEnum.getName();
            }
        }
        return "未知节点类型";
    }

} 