package cn.iocoder.yudao.module.icbc.controller.admin.payee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import com.alibaba.excel.annotation.*;

@Schema(description = "管理后台 - 工行收方信息 Response VO")
@Data
@ExcelIgnoreUnannotated
public class PayeeInfoRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "收方编号（工行返回）", example = "ICBC001")
    @ExcelProperty("收方编号")
    private String payeeNo;

    @Schema(description = "合作方收方编号（我方生成）", requiredMode = Schema.RequiredMode.REQUIRED, example = "PARTNER001")
    @ExcelProperty("合作方收方编号")
    private String partnerPayeeId;

    @Schema(description = "收方姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @ExcelProperty("收方姓名")
    private String name;

    @Schema(description = "身份证号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "110101199001011234")
    @ExcelProperty("身份证号码")
    private String idCardNo;

    @Schema(description = "手机号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @ExcelProperty("手机号码")
    private String mobile;

    @Schema(description = "银行卡号", example = "6222021234567890123")
    @ExcelProperty("银行卡号")
    private String bankCardNo;

    @Schema(description = "开户银行", example = "中国工商银行")
    @ExcelProperty("开户银行")
    private String bankName;

    @Schema(description = "开户支行", example = "北京分行营业部")
    @ExcelProperty("开户支行")
    private String bankBranch;

    @Schema(description = "地址", example = "北京市朝阳区xxx街道")
    @ExcelProperty("地址")
    private String address;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "审核信息", example = "审核通过")
    @ExcelProperty("审核信息")
    private String auditMsg;

    @Schema(description = "业务类型", example = "RECYCLE")
    @ExcelProperty("业务类型")
    private String businessType;

    @Schema(description = "工行收方状态", example = "1")
    @ExcelProperty("工行收方状态")
    private String icbcReceiverStatus;

    @Schema(description = "工行电子账户账号", example = "ICBC123456")
    @ExcelProperty("工行电子账户账号")
    private String icbcMediumId;

    @Schema(description = "开户状态", example = "02")
    @ExcelProperty("开户状态")
    private String icbcOpenacctStatus;

    @Schema(description = "职业", example = "001")
    @ExcelProperty("职业")
    private String occupation;

    @Schema(description = "关联企业名称", example = "某某回收公司")
    @ExcelProperty("关联企业名称")
    private String companyName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private Date createTime;

} 