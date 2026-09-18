package cn.iocoder.yudao.module.icbc.convert.invoice;

import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreOrderReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.OrderItemDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 工行反向开票订单 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface InvoiceOrderConvert {

    InvoiceOrderConvert INSTANCE = Mappers.getMapper(InvoiceOrderConvert.class);

    /**
     * 将请求VO转换为订单DO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNo", ignore = true)
    @Mapping(target = "partnerOrderId", source = "outOrderId")
    @Mapping(target = "payeeId", ignore = true)
    @Mapping(target = "payeeNo", source = "outUserId")
    @Mapping(target = "payerId", ignore = true)
    @Mapping(target = "payerNo", source = "outVendorId")
    @Mapping(target = "totalAmount", source = "orderAmount")
    @Mapping(target = "invoiceType", expression = "java(convertInvoiceType(reqVO.getInvoiceType()))")
    @Mapping(target = "businessType", expression = "java(convertBusinessType(reqVO.getSpecificElements()))")
    @Mapping(target = "orderStatus", constant = "0")
    @Mapping(target = "invoiceStatus", constant = "0")
    @Mapping(target = "paymentStatus", constant = "0")
    @Mapping(target = "taxStatus", constant = "0")
    @Mapping(target = "invoiceNo", ignore = true)
    @Mapping(target = "invoiceCode", ignore = true)
    @Mapping(target = "invoiceDate", ignore = true)
    @Mapping(target = "invoiceAmount", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "invoiceFileUrl", ignore = true)
    @Mapping(target = "remark", source = "notes")
    InvoiceOrderDO convert(InvoicePreOrderReqVO reqVO);

    /**
     * 将订单DO转换为查询响应VO
     */
    @Mapping(target = "returnCode", constant = "0")
    @Mapping(target = "returnMsg", constant = "成功")
    @Mapping(target = "partnerOrderId", source = "partnerOrderId")
    @Mapping(target = "redSerialNo", ignore = true)
    @Mapping(target = "redInvoiceNo", ignore = true)
    @Mapping(target = "redInvoiceDate", ignore = true)
    InvoiceQueryRespVO convert(InvoiceOrderDO orderDO);

    /**
     * 将商品信息VO转换为订单明细DO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "orderNo", ignore = true)
    @Mapping(target = "itemName", source = "projectName")
    @Mapping(target = "itemCode", ignore = true)
    @Mapping(target = "specification", source = "weight")
    @Mapping(target = "unit", source = "units")
    @Mapping(target = "quantity", source = "goodsNum")
    @Mapping(target = "unitPrice", source = "price")
    @Mapping(target = "amount", source = "goodsAmt")
    @Mapping(target = "taxRate", source = "taxRate")
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "category", ignore = true)
    OrderItemDO convert(InvoicePreOrderReqVO.GoodsInfoVO goodsInfoVO);

    /**
     * 批量转换商品信息
     */
    List<OrderItemDO> convertList(List<InvoicePreOrderReqVO.GoodsInfoVO> goodsInfoList);

    /**
     * 转换发票类型
     */
    default Integer convertInvoiceType(String invoiceType) {
        if ("01".equals(invoiceType)) {
            return 2; // 增值税专用发票
        } else if ("02".equals(invoiceType)) {
            return 1; // 增值税普通发票
        }
        return 1; // 默认普通发票
    }

    /**
     * 转换业务类型
     */
    default String convertBusinessType(String specificElements) {
        if ("16".equals(specificElements)) {
            return "AGRICULTURAL"; // 农产品收购
        } else if ("24".equals(specificElements)) {
            return "SCRAP"; // 报废产品收购
        }
        return "SCRAP"; // 默认报废产品收购
    }

} 