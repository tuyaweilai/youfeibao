package cn.iocoder.yudao.module.icbc.dal.mysql.payee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeInfoPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工行收方信息 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface PayeeInfoMapper extends BaseMapperX<PayeeInfoDO> {

    default PageResult<PayeeInfoDO> selectPage(PayeeInfoPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PayeeInfoDO>()
                .likeIfPresent(PayeeInfoDO::getName, reqVO.getName())
                .eqIfPresent(PayeeInfoDO::getIdCardNo, reqVO.getIdCardNo())
                .eqIfPresent(PayeeInfoDO::getMobile, reqVO.getMobile())
                .eqIfPresent(PayeeInfoDO::getStatus, reqVO.getStatus())
                .eqIfPresent(PayeeInfoDO::getBusinessType, reqVO.getBusinessType())
                .betweenIfPresent(PayeeInfoDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PayeeInfoDO::getId));
    }

    default List<PayeeInfoDO> selectList(PayeeInfoPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<PayeeInfoDO>()
                .likeIfPresent(PayeeInfoDO::getName, reqVO.getName())
                .eqIfPresent(PayeeInfoDO::getIdCardNo, reqVO.getIdCardNo())
                .eqIfPresent(PayeeInfoDO::getMobile, reqVO.getMobile())
                .eqIfPresent(PayeeInfoDO::getStatus, reqVO.getStatus())
                .eqIfPresent(PayeeInfoDO::getBusinessType, reqVO.getBusinessType())
                .betweenIfPresent(PayeeInfoDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PayeeInfoDO::getId));
    }

    default PayeeInfoDO selectByPayeeNo(String payeeNo) {
        return selectOne(PayeeInfoDO::getPayeeNo, payeeNo);
    }

    default PayeeInfoDO selectByPartnerPayeeId(String partnerPayeeId) {
        return selectOne(PayeeInfoDO::getPartnerPayeeId, partnerPayeeId);
    }

    /**
     * 按自然人主体取本租户的收方档案。
     *
     * <p>身份是平台级的、收方档案是租户级的，所以本方法**天然带租户条件**：同一个自然人在别的
     * 回收企业有档案，本租户查不到（与企业间不可见一致，见 CONTEXT「交易可见性边界」）。
     */
    default PayeeInfoDO selectByNaturalPersonId(Long naturalPersonId) {
        if (naturalPersonId == null) {
            return null;
        }
        return selectOne(PayeeInfoDO::getNaturalPersonId, naturalPersonId);
    }

    default java.util.List<PayeeInfoDO> selectListByNaturalPersonId(Long naturalPersonId) {
        if (naturalPersonId == null) {
            return java.util.Collections.emptyList();
        }
        return selectList(PayeeInfoDO::getNaturalPersonId, naturalPersonId);
    }

    default PayeeInfoDO selectByIdCardNo(String idCardNo) {
        return selectOne(PayeeInfoDO::getIdCardNo, idCardNo);
    }

    default PayeeInfoDO selectByMobile(String mobile) {
        return selectOne(PayeeInfoDO::getMobile, mobile);
    }

} 