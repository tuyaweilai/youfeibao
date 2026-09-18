#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os

# VO类配置
vo_configs = [
    # 订单相关
    {
        "module": "order",
        "class_name": "OrderPriceAdjustment",
        "table_name": "waste_order_price_adjustment",
        "description": "订单价格调整记录",
        "fields": [
            {"name": "orderId", "type": "Long", "required": True, "description": "订单ID"},
            {"name": "adjustmentType", "type": "Integer", "required": True, "description": "调整类型"},
            {"name": "originalPrice", "type": "BigDecimal", "description": "原价格"},
            {"name": "adjustedPrice", "type": "BigDecimal", "description": "调整后价格"},
            {"name": "adjustmentReason", "type": "String", "required": True, "description": "调整原因", "max_length": 500}
        ]
    },
    # 付款相关
    {
        "module": "payment",
        "class_name": "PaymentStatusHistory",
        "table_name": "waste_payment_status_history",
        "description": "付款状态变更历史",
        "fields": [
            {"name": "orderId", "type": "Long", "required": True, "description": "订单ID"},
            {"name": "statusFrom", "type": "Integer", "description": "原状态"},
            {"name": "statusTo", "type": "Integer", "required": True, "description": "目标状态"},
            {"name": "changeReason", "type": "String", "description": "变更原因", "max_length": 500}
        ]
    },
    # 报价相关
    {
        "module": "quotation",
        "class_name": "AppointmentQuotation",
        "table_name": "waste_appointment_quotation",
        "description": "预约报价记录",
        "fields": [
            {"name": "appointmentId", "type": "Long", "required": True, "description": "预约单ID"},
            {"name": "recyclingEnterpriseId", "type": "Long", "required": True, "description": "回收企业ID"},
            {"name": "quotedPrice", "type": "BigDecimal", "required": True, "description": "报价金额"},
            {"name": "quotationRemark", "type": "String", "description": "报价备注", "max_length": 500}
        ]
    }
]

def create_vo_class(config, vo_type):
    """创建VO类"""
    module = config["module"]
    class_name = config["class_name"]
    description = config["description"]
    fields = config["fields"]
    
    # 创建目录
    vo_dir = f"ruoyi-vue-pro/yudao-module-waste/yudao-module-waste-biz/src/main/java/cn/iocoder/yudao/module/waste/controller/admin/{module}/vo"
    os.makedirs(vo_dir, exist_ok=True)
    
    if vo_type == "CreateReqVO":
        create_create_req_vo(vo_dir, class_name, description, fields)
    elif vo_type == "UpdateReqVO":
        create_update_req_vo(vo_dir, class_name, description)
    elif vo_type == "PageReqVO":
        create_page_req_vo(vo_dir, class_name, description, fields)
    elif vo_type == "RespVO":
        create_resp_vo(vo_dir, class_name, description, fields)

def create_create_req_vo(vo_dir, class_name, description, fields):
    """创建CreateReqVO"""
    file_path = f"{vo_dir}/{class_name}CreateReqVO.java"
    
    imports = [
        "import io.swagger.v3.oas.annotations.media.Schema;",
        "import lombok.Data;",
        "",
        "import javax.validation.constraints.NotNull;",
        "import javax.validation.constraints.NotBlank;",
        "import javax.validation.constraints.DecimalMin;",
        "import javax.validation.constraints.Size;",
        "import java.math.BigDecimal;",
        "import java.time.LocalDateTime;"
    ]
    
    content = f"""package cn.iocoder.yudao.module.waste.controller.admin.{module}.vo;

{chr(10).join(imports)}

@Schema(description = "管理后台 - {description}创建 Request VO")
@Data
public class {class_name}CreateReqVO {{

"""
    
    for field in fields:
        field_name = field["name"]
        field_type = field["type"]
        field_desc = field["description"]
        required = field.get("required", False)
        max_length = field.get("max_length")
        
        # 添加Schema注解
        if required:
            content += f'    @Schema(description = "{field_desc}", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")\n'
        else:
            content += f'    @Schema(description = "{field_desc}", example = "示例值")\n'
        
        # 添加校验注解
        if required:
            if field_type == "String":
                content += f'    @NotBlank(message = "{field_desc}不能为空")\n'
            else:
                content += f'    @NotNull(message = "{field_desc}不能为空")\n'
        
        if field_type == "BigDecimal":
            content += f'    @DecimalMin(value = "0", message = "{field_desc}不能小于0")\n'
        
        if max_length:
            content += f'    @Size(max = {max_length}, message = "{field_desc}长度不能超过{max_length}个字符")\n'
        
        content += f"    private {field_type} {field_name};\n\n"
    
    content += "}\n"
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"Created: {file_path}")

# 执行创建
if __name__ == "__main__":
    for config in vo_configs:
        for vo_type in ["CreateReqVO", "UpdateReqVO", "PageReqVO", "RespVO"]:
            try:
                create_vo_class(config, vo_type)
            except Exception as e:
                print(f"Error creating {config['class_name']}{vo_type}: {e}") 