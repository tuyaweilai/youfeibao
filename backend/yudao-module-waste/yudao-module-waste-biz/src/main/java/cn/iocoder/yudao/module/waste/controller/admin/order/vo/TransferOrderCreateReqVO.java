package cn.iocoder.yudao.module.waste.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 危废转移订单创建 Request VO")
@Data
public class TransferOrderCreateReqVO {

    // ========== 关联信息 ==========
    @Schema(description = "关联预约单ID", example = "1024")
    private Long appointmentId;

    @Schema(description = "关联报价记录ID", example = "2048")
    private Long quotationId;

    // ========== 企业信息 ==========
    @Schema(description = "产废企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "产废企业ID不能为空")
    private Long producingEnterpriseId;

    @Schema(description = "产废门店ID", example = "512")
    private Long producingStoreId;

    @Schema(description = "回收企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "回收企业ID不能为空")
    private Long recyclingEnterpriseId;

    // ========== 废物信息 ==========
    @Schema(description = "危险废物代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    @NotBlank(message = "危险废物代码不能为空")
    @Size(max = 50, message = "危险废物代码长度不能超过50个字符")
    private String wasteCode;

    @Schema(description = "危险废物名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗废物")
    @NotBlank(message = "危险废物名称不能为空")
    @Size(max = 100, message = "危险废物名称长度不能超过100个字符")
    private String wasteName;

    @Schema(description = "预估数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.50")
    @NotNull(message = "预估数量不能为空")
    @DecimalMin(value = "0.01", message = "预估数量必须大于0")
    @Digits(integer = 8, fraction = 2, message = "预估数量格式不正确")
    private BigDecimal estimatedQuantity;

    @Schema(description = "数量单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "吨")
    @NotBlank(message = "数量单位不能为空")
    @Size(max = 10, message = "数量单位长度不能超过10个字符")
    private String quantityUnit;

    @Schema(description = "包装方式", example = "桶装")
    @Size(max = 50, message = "包装方式长度不能超过50个字符")
    private String packagingType;

    // ========== 价格信息 ==========
    @Schema(description = "单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "1200.00")
    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.01", message = "单价必须大于0")
    @Digits(integer = 8, fraction = 2, message = "单价格式不正确")
    private BigDecimal unitPrice;

    @Schema(description = "预估总金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "120000.00")
    @NotNull(message = "预估总金额不能为空")
    @DecimalMin(value = "0.01", message = "预估总金额必须大于0")
    @Digits(integer = 10, fraction = 2, message = "预估总金额格式不正确")
    private BigDecimal estimatedAmount;

    // ========== 付款管理 ==========
    @Schema(description = "关联的产废企业付款配置ID", example = "128")
    private Long paymentConfigId;

    @Schema(description = "实际付款方式 (1:对公结算, 2:个人结算)", example = "1")
    private Integer paymentMethodType;

    // ========== 订单来源 ==========
    @Schema(description = "订单来源类型 (0:预约转订单, 1:扫街临时订单, 2:补单)", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "订单来源类型不能为空")
    @Min(value = 0, message = "订单来源类型值无效")
    @Max(value = 2, message = "订单来源类型值无效")
    private Integer sourceType;

    @Schema(description = "关联订单ID (用于补单场景)", example = "256")
    private Long relatedOrderId;

    @Schema(description = "用户备注", example = "特殊要求说明")
    @Size(max = 255, message = "用户备注长度不能超过255个字符")
    private String userRemark;

    @Schema(description = "内部备注", example = "内部处理说明")
    @Size(max = 255, message = "内部备注长度不能超过255个字符")
    private String internalRemark;

} 