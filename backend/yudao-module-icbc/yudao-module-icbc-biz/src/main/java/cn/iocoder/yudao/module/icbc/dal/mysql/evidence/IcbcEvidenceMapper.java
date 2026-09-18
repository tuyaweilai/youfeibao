package cn.iocoder.yudao.module.icbc.dal.mysql.evidence;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.evidence.IcbcEvidenceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 一票一档证据 Mapper
 */
@Mapper
public interface IcbcEvidenceMapper extends BaseMapperX<IcbcEvidenceDO> {

    /**
     * 按合作方订单号查询证据，按创建时间升序。
     */
    default List<IcbcEvidenceDO> selectListByPartnerOrderId(String partnerOrderId) {
        return selectList(IcbcEvidenceDO::getPartnerOrderId, partnerOrderId);
    }

    /**
     * 按多个合作方订单号批量查询证据，供批量齐备率与批量导出使用，避免 N+1。
     */
    default List<IcbcEvidenceDO> selectListByPartnerOrderIds(Collection<String> partnerOrderIds) {
        return selectList(IcbcEvidenceDO::getPartnerOrderId, partnerOrderIds);
    }

}
