package cn.iocoder.yudao.module.icbc.dal.mysql.invoice;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 红字发票 Mapper
 */
@Mapper
public interface RedInvoiceMapper extends BaseMapperX<RedInvoiceDO> {

    default RedInvoiceDO selectByRedOffsetNo(String redOffsetNo) {
        return selectOne(RedInvoiceDO::getRedOffsetNo, redOffsetNo);
    }

    /**
     * 取某张蓝票最近一次红冲记录（红蓝一一对应，一次只有一张生效中的红票），
     * 撤销后再冲会新增一条，因此按 id 倒序取最近一条。
     */
    default RedInvoiceDO selectLatestByPartnerOrderId(String partnerOrderId) {
        return selectOne(new LambdaQueryWrapperX<RedInvoiceDO>()
                .eq(RedInvoiceDO::getPartnerOrderId, partnerOrderId)
                .orderByDesc(RedInvoiceDO::getId)
                .last("LIMIT 1"));
    }

}
