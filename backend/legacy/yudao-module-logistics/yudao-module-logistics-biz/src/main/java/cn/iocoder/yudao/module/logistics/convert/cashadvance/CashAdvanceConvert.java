package cn.iocoder.yudao.module.logistics.convert.cashadvance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.cashadvance.vo.*;
import cn.iocoder.yudao.module.logistics.dal.dataobject.cashadvance.CashAdvanceDO;
import cn.iocoder.yudao.module.logistics.enums.CashAdvanceNotifyStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.CashAdvanceReconcileStatusEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 物流现金代付记录 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface CashAdvanceConvert {

    CashAdvanceConvert INSTANCE = Mappers.getMapper(CashAdvanceConvert.class);

    CashAdvanceDO convert(CashAdvanceCreateReqVO bean);

    CashAdvanceDO convert(CashAdvanceUpdateReqVO bean);

    @Mapping(target = "notifyStatusName", source = "notifyStatus", qualifiedByName = "notifyStatusToName")
    @Mapping(target = "reconcileStatusName", source = "reconcileStatus", qualifiedByName = "reconcileStatusToName")
    CashAdvanceRespVO convert(CashAdvanceDO bean);

    @Mapping(target = "notifyStatusName", source = "notifyStatus", qualifiedByName = "notifyStatusToName")
    @Mapping(target = "reconcileStatusName", source = "reconcileStatus", qualifiedByName = "reconcileStatusToName")
    CashAdvanceExcelVO convertExcel(CashAdvanceDO bean);

    List<CashAdvanceRespVO> convertList(List<CashAdvanceDO> list);

    List<CashAdvanceExcelVO> convertExcelList(List<CashAdvanceDO> list);

    PageResult<CashAdvanceRespVO> convertPage(PageResult<CashAdvanceDO> page);

    @Named("notifyStatusToName")
    default String notifyStatusToName(Integer notifyStatus) {
        if (notifyStatus == null) {
            return null;
        }
        for (CashAdvanceNotifyStatusEnum statusEnum : CashAdvanceNotifyStatusEnum.values()) {
            if (statusEnum.getStatus().equals(notifyStatus)) {
                return statusEnum.getName();
            }
        }
        return "未知状态";
    }

    @Named("reconcileStatusToName")
    default String reconcileStatusToName(Integer reconcileStatus) {
        if (reconcileStatus == null) {
            return null;
        }
        for (CashAdvanceReconcileStatusEnum statusEnum : CashAdvanceReconcileStatusEnum.values()) {
            if (statusEnum.getStatus().equals(reconcileStatus)) {
                return statusEnum.getName();
            }
        }
        return "未知状态";
    }

} 