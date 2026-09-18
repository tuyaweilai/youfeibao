package cn.iocoder.yudao.module.icbc.convert.payer;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.PayerAddReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.PayerInfoRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.PayerInfoSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 工行付方信息 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface PayerInfoConvert {

    PayerInfoConvert INSTANCE = Mappers.getMapper(PayerInfoConvert.class);

    /**
     * 保存请求VO转换为DO
     *
     * @param saveReqVO 保存请求VO
     * @return DO对象
     */
    PayerInfoDO convert(PayerInfoSaveReqVO saveReqVO);

    /**
     * DO转换为响应VO
     *
     * @param payerInfoDO DO对象
     * @return 响应VO
     */
    PayerInfoRespVO convert(PayerInfoDO payerInfoDO);

    /**
     * DO批量转换为响应VO
     *
     * @param payerInfoDOs DO对象列表
     * @return 响应VO列表
     */
    List<PayerInfoRespVO> convertList(List<PayerInfoDO> payerInfoDOs);

    /**
     * 分页结果转换
     *
     * @param pageResult 分页结果
     * @return 分页响应VO
     */
    PageResult<PayerInfoRespVO> convertPage(PageResult<PayerInfoDO> pageResult);

    /**
     * 付方新增请求VO转换为DO
     *
     * @param addReqVO 付方新增请求VO
     * @return DO对象
     */
    PayerInfoDO convert(PayerAddReqVO addReqVO);

} 