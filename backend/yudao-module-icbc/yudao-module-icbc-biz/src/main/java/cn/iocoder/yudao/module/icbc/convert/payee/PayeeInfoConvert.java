package cn.iocoder.yudao.module.icbc.convert.payee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 工行收方信息 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface PayeeInfoConvert {

    PayeeInfoConvert INSTANCE = Mappers.getMapper(PayeeInfoConvert.class);

    PayeeInfoDO convert(PayeeInfoSaveReqVO bean);

    PayeeInfoRespVO convert(PayeeInfoDO bean);

    List<PayeeInfoRespVO> convertList(List<PayeeInfoDO> list);

    PageResult<PayeeInfoRespVO> convertPage(PageResult<PayeeInfoDO> page);

    /**
     * 将工行收方新增请求VO转换为PayeeInfoDO
     */
    default PayeeInfoDO convert(PayeeAddReqVO reqVO) {
        if (reqVO == null) {
            return null;
        }
        
        PayeeInfoDO payeeInfo = new PayeeInfoDO();
        payeeInfo.setPartnerPayeeId(reqVO.getOutUserId());
        payeeInfo.setName(reqVO.getReceiverName());
        payeeInfo.setIdCardNo(reqVO.getIdNo());
        payeeInfo.setMobile(reqVO.getMobile());
        payeeInfo.setBankCardNo(reqVO.getReceiverAccount());
        payeeInfo.setAddress(reqVO.getAddress());
        payeeInfo.setOccupation(reqVO.getOccupation());
        payeeInfo.setBusinessType("RECYCLE"); // 默认再生资源业务
        payeeInfo.setStatus(0); // 默认待审核状态
        
        return payeeInfo;
    }

} 