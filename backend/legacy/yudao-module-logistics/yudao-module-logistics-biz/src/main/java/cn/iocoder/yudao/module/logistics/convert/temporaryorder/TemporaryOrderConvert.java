package cn.iocoder.yudao.module.logistics.convert.temporaryorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.temporaryorder.vo.*;
import cn.iocoder.yudao.module.logistics.dal.dataobject.temporaryorder.TemporaryOrderDO;
import cn.iocoder.yudao.module.logistics.enums.TemporaryOrderPaymentStatusEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 物流临时订单 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface TemporaryOrderConvert {

    TemporaryOrderConvert INSTANCE = Mappers.getMapper(TemporaryOrderConvert.class);

    TemporaryOrderDO convert(TemporaryOrderCreateReqVO bean);

    TemporaryOrderDO convert(TemporaryOrderUpdateReqVO bean);

    @Mapping(target = "paymentStatusName", source = "paymentStatus", qualifiedByName = "paymentStatusToName")
    TemporaryOrderRespVO convert(TemporaryOrderDO bean);

    @Mapping(target = "paymentStatusName", source = "paymentStatus", qualifiedByName = "paymentStatusToName")
    TemporaryOrderExcelVO convertExcel(TemporaryOrderDO bean);

    List<TemporaryOrderRespVO> convertList(List<TemporaryOrderDO> list);

    List<TemporaryOrderExcelVO> convertExcelList(List<TemporaryOrderDO> list);

    PageResult<TemporaryOrderRespVO> convertPage(PageResult<TemporaryOrderDO> page);

    @Named("paymentStatusToName")
    default String paymentStatusToName(Integer paymentStatus) {
        if (paymentStatus == null) {
            return null;
        }
        for (TemporaryOrderPaymentStatusEnum statusEnum : TemporaryOrderPaymentStatusEnum.values()) {
            if (statusEnum.getStatus().equals(paymentStatus)) {
                return statusEnum.getName();
            }
        }
        return "未知状态";
    }

} 