#!/bin/bash

# 工商银行接口测试脚本
# 使用方法: ./test-icbc-api.sh [BASE_URL] [TOKEN]

BASE_URL=${1:-"http://localhost:48080"}
TOKEN=${2:-""}

echo "=========================================="
echo "工商银行接口测试脚本"
echo "=========================================="
echo "基础URL: $BASE_URL"
echo "Token: ${TOKEN:0:20}..."
echo ""

# 设置请求头
if [ -n "$TOKEN" ]; then
    AUTH_HEADER="Authorization: Bearer $TOKEN"
else
    AUTH_HEADER=""
fi

# 1. 健康检查（无需权限）
echo "1. 健康检查测试..."
curl -s -X GET "$BASE_URL/admin-api/icbc/test/health" \
     -H "Content-Type: application/json" | jq '.' || echo "健康检查失败"
echo ""

# 如果没有提供Token，跳过需要权限的测试
if [ -z "$TOKEN" ]; then
    echo "⚠️  未提供Token，跳过需要权限的测试"
    echo "使用方法: $0 $BASE_URL YOUR_TOKEN"
    exit 0
fi

# 2. 配置信息检查
echo "2. 配置信息检查..."
curl -s -X GET "$BASE_URL/admin-api/icbc/test/config" \
     -H "Content-Type: application/json" \
     -H "$AUTH_HEADER" | jq '.' || echo "配置检查失败"
echo ""

# 3. SDK连接测试
echo "3. SDK连接测试..."
curl -s -X GET "$BASE_URL/admin-api/icbc/test/connection" \
     -H "Content-Type: application/json" \
     -H "$AUTH_HEADER" | jq '.' || echo "连接测试失败"
echo ""

# 4. 签名验证测试
echo "4. 签名验证测试..."
curl -s -X GET "$BASE_URL/admin-api/icbc/test/signature" \
     -H "Content-Type: application/json" \
     -H "$AUTH_HEADER" | jq '.' || echo "签名测试失败"
echo ""

# 5. 发票查询测试
echo "5. 发票查询测试..."
ORDER_ID="test_order_$(date +%s)"
USER_ID="test_user_$(date +%s)"
curl -s -X GET "$BASE_URL/admin-api/icbc/test/invoice-query?outOrderId=$ORDER_ID&outUserId=$USER_ID" \
     -H "Content-Type: application/json" \
     -H "$AUTH_HEADER" | jq '.' || echo "发票查询测试失败"
echo ""

# 6. 支付表单生成测试（JSON格式）
echo "6. 支付表单生成测试..."
PAY_ORDER_ID="pay_order_$(date +%s)"
PAY_USER_ID="pay_user_$(date +%s)"
curl -s -X GET "$BASE_URL/admin-api/icbc/test/payment-form-json?outOrderId=$PAY_ORDER_ID&outUserId=$PAY_USER_ID" \
     -H "Content-Type: application/json" \
     -H "$AUTH_HEADER" | jq '.' || echo "支付表单测试失败"
echo ""

# 7. 综合测试
echo "7. 综合测试..."
curl -s -X GET "$BASE_URL/admin-api/icbc/test/all" \
     -H "Content-Type: application/json" \
     -H "$AUTH_HEADER" | jq '.' || echo "综合测试失败"
echo ""

echo "=========================================="
echo "测试完成！"
echo "==========================================" 