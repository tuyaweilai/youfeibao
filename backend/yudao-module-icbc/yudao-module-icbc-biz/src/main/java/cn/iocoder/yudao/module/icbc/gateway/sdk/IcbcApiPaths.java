package cn.iocoder.yudao.module.icbc.gateway.sdk;

/**
 * 工行接口路径
 *
 * 集中在这里、只被适配层引用。平台其余部分不得出现这些地址（issue #3 验收项）。
 */
public final class IcbcApiPaths {

    public static final String PAYEE_ONBOARDING_PAGE = "/ui/jft/ui/user/edpopenacct/submit/V1";
    public static final String PAYEE_ONBOARDING_QUERY = "/api/jft/api/user/edpopenacct/query/V1";
    public static final String ENTERPRISE_AUTHORIZATION = "/ui/jft/ui/invoice/authorization/V1";
    public static final String PRE_ORDER = "/ui/jft/ui/invoice/pre/order/V1";
    public static final String INVOICE_QUERY = "/api/jft/api/invoice/queryInvoiceInfo/V1";
    public static final String PAYMENT = "/ui/jft/ui/invoice/pay/V1";
    public static final String INVOICE_DOWNLOAD = "/api/jft/api/invoice/download/V1";
    public static final String INVOICE_CANCEL = "/api/jft/api/invoice/reversal/V1";
    public static final String RED_INVOICE_OFFSET = "/ui/jft/ui/red/invoice/offset/V1";
    public static final String RED_INVOICE_REVOKE = "/api/jft/api/red/invoice/offset/revoke/V1";
    /**
     * 智慧清分收方查询，用于真实连通性校验
     */
    public static final String EDPRECEIVE_QUERY = "/api/jft/api/user/edpreceive/query/V1";

    /**
     * 发票类接口的成功码
     */
    public static final int INVOICE_SUCCESS_CODE = 10100000;
    /**
     * 收方 / 智慧清分类接口的成功码
     */
    public static final int RECEIVE_SUCCESS_CODE = 0;

    private IcbcApiPaths() {
    }

}
