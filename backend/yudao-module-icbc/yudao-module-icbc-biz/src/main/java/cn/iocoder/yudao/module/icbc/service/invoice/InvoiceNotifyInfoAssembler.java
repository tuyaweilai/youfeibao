package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 工行开票类异步通知报文 → {@link InvoiceInfo} 的装配器。
 *
 * <p>通知（{@code notifyType=01/03/04/05}）与预查询返回的字段名一致，都是工行开票状态机的
 * 原生字段。把通知也装配成同一个 {@link InvoiceInfo}，平台侧就能用<strong>同一条收敛逻辑</strong>
 * 处理通知与查询两条路径，天然得到一致结果。
 */
public final class InvoiceNotifyInfoAssembler {

    private InvoiceNotifyInfoAssembler() {
    }

    /**
     * 从通知报文明文 JSON 装配 {@link InvoiceInfo}；报文缺哪个字段，装配结果里就是 {@code null}，
     * 收敛时自然跳过、不覆盖本地快照。
     */
    public static InvoiceInfo fromNotify(JSONObject payload) {
        if (payload == null) {
            return InvoiceInfo.builder().build();
        }
        List<InvoiceInfo.LevyItem> levyItems = parseLevyItems(payload.getJSONArray("invoiceLevyItemResponse"));
        return InvoiceInfo.builder()
                .outOrderId(payload.getString("outOrderId"))
                .outInvoiceId(payload.getString("outInvoiceId"))
                .outRedOffsetId(payload.getString("outRedOffsetId"))
                .confirmStatus(payload.getString("confirmStatus"))
                .payStatus(payload.getString("payStatus"))
                .invoiceStatus(payload.getString("invoiceStatus"))
                .uploadStatus(payload.getString("uploadStatus"))
                .taxStatus(payload.getString("taxStatus"))
                .redOffsetStatus(payload.getString("redOffsetStatus"))
                .redOffsetInvoiceCode(payload.getString("redOffsetInvoiceCode"))
                .redOffsetReason(payload.getString("redOffsetReason"))
                .redOffsetAmount(payload.getString("redOffsetAmount"))
                .redOffsetTax(payload.getString("redOffsetTax"))
                .redOffsetAmountTax(payload.getString("redOffsetAmountTax"))
                .invoiceCode(payload.getString("invoiceCode"))
                .invoiceNo(StrUtil.blankToDefault(payload.getString("invoiceNo"), payload.getString("invoiceCode")))
                .invoiceDate(payload.getString("invoiceDate"))
                .taxAmount(payload.getString("taxAmount"))
                .taxRealAmount(payload.getString("taxRealAmount"))
                .tradeTime(payload.getString("tradeTime"))
                .taxPaymentMethod(payload.getString("taxPaymentMethod"))
                .supplementaryTax(payload.getString("supplementaryTax"))
                .payAmount(payload.getString("payAmount"))
                .actuallyReceivedAmount(payload.getString("actuallyReceivedAmount"))
                .serialNo(payload.getString("serialNo"))
                .icbcOrderId(payload.getString("icbcOrderId"))
                .levyItems(levyItems)
                .build();
    }

    private static List<InvoiceInfo.LevyItem> parseLevyItems(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return new ArrayList<>();
        }
        List<InvoiceInfo.LevyItem> items = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item == null) {
                continue;
            }
            items.add(InvoiceInfo.LevyItem.builder()
                    .levyItemCode(item.getString("levyItemCode"))
                    .levyItemName(item.getString("levyItemName"))
                    .levyGradeCode(item.getString("levyGradeCode"))
                    .levyGradeName(item.getString("levyGradeName"))
                    .taxBasis(item.getString("taxBasis"))
                    .taxRate(item.getString("taxRate"))
                    .taxPayable(item.getString("taxPayable"))
                    .voucherNum(item.getString("voucherNum"))
                    .taxStartDate(item.getString("taxStartDate"))
                    .taxEndDate(item.getString("taxEndDate"))
                    .build());
        }
        return items;
    }
}
