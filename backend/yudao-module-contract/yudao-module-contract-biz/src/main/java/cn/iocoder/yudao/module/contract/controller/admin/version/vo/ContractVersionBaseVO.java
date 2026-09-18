package cn.iocoder.yudao.module.contract.controller.admin.version.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * 合同版本 Base VO，提供给添加、修改、详情共用的字段
 */
@Data
public class ContractVersionBaseVO {

} 