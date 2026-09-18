package cn.iocoder.yudao.module.icbc.dal.mysql.payer;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.PayerInfoExportReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.PayerInfoPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 付款方信息 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface PayerInfoMapper extends BaseMapperX<PayerInfoDO> {

    /**
     * 根据统一社会信用代码查询付款方信息
     *
     * @param creditCode 统一社会信用代码
     * @return 付款方信息
     */
    default PayerInfoDO selectByCreditCode(String creditCode) {
        return selectOne(PayerInfoDO::getCreditCode, creditCode);
    }

    /**
     * 根据税号查询付款方信息
     *
     * @param taxNo 税号
     * @return 付款方信息
     */
    default PayerInfoDO selectByTaxNo(String taxNo) {
        return selectOne(PayerInfoDO::getTaxNo, taxNo);
    }

    /**
     * 根据合作方付款方ID查询付款方信息
     *
     * @param partnerPayerId 合作方付款方ID
     * @return 付款方信息
     */
    default PayerInfoDO selectByPartnerPayerId(String partnerPayerId) {
        return selectOne(PayerInfoDO::getPartnerPayerId, partnerPayerId);
    }

    /**
     * 分页查询付款方信息
     *
     * @param reqVO 查询条件
     * @return 付款方信息分页结果
     */
    default PageResult<PayerInfoDO> selectPage(PayerInfoPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PayerInfoDO>()
                .likeIfPresent(PayerInfoDO::getName, reqVO.getName())
                .eqIfPresent(PayerInfoDO::getCreditCode, reqVO.getCreditCode())
                .eqIfPresent(PayerInfoDO::getTaxNo, reqVO.getTaxNo())
                .eqIfPresent(PayerInfoDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(PayerInfoDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PayerInfoDO::getId));
    }

    /**
     * 根据导出条件查询付款方信息
     *
     * @param reqVO 导出请求
     * @return 付款方信息列表
     */
    default List<PayerInfoDO> selectList(PayerInfoExportReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<PayerInfoDO>()
                .likeIfPresent(PayerInfoDO::getName, reqVO.getName())
                .eqIfPresent(PayerInfoDO::getCreditCode, reqVO.getCreditCode())
                .eqIfPresent(PayerInfoDO::getTaxNo, reqVO.getTaxNo())
                .eqIfPresent(PayerInfoDO::getContactMobile, reqVO.getContactMobile())
                .eqIfPresent(PayerInfoDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(PayerInfoDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PayerInfoDO::getId));
    }
} 