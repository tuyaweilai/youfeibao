package cn.iocoder.yudao.module.icbc.controller.admin.payee.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 工行收方信息分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PayeeInfoPageReqVO extends PageParam {

    @Schema(description = "收方姓名", example = "张三")
    private String name;

    @Schema(description = "身份证号码", example = "110101199001011234")
    private String idCardNo;

    @Schema(description = "手机号码", example = "13800138000")
    private String mobile;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "业务类型", example = "RECYCLE")
    private String businessType;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private Date[] createTime;

} 