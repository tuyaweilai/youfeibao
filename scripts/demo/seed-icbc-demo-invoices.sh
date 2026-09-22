#!/usr/bin/env bash
#
# 本地演示数据：一组「已付款 + 已开票 + 已下载 PDF」的收购单（ACQ20260922DEMO01/02/04/05）。
#
# 干三件事：
#   1. 生成演示用 PDF 到 /tmp/icbc-demo（不是真发票，只为把下载链路跑通）；
#   2. 跑 scripts/demo/icbc-demo-invoices.sql（幂等，重复跑不会重复插入）；
#   3. 把 icbc_invoice_download / icbc_invoice_file 的 file_size 校准成磁盘上真实的字节数
#      —— downloadFile 拿它当 Content-Length，对不上浏览器会截断下载。
#
# 依赖：docker compose 起的 MySQL 容器（youfeibao-mysql）、根目录 .env 里的 MYSQL_USERNAME /
# MYSQL_PASSWORD。数据库名固定 ruoyi-vue-pro（见 backend/sql/mysql/README.md）。
#
# 用法：bash scripts/demo/seed-icbc-demo-invoices.sh

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

if [[ ! -f .env ]]; then
  echo "缺少 .env：先 cp .env.example .env 并填 MYSQL_PASSWORD 等" >&2
  exit 1
fi
set -a
# shellcheck disable=SC1091
source .env
set +a

DB_CONTAINER="${DB_CONTAINER:-youfeibao-mysql}"
DB_NAME="ruoyi-vue-pro"
DEMO_NO='ACQ20260922DEMO01|ACQ20260922DEMO02|ACQ20260922DEMO04|ACQ20260922DEMO05'

mysql_exec() {
  docker exec -i "$DB_CONTAINER" mysql --default-character-set=utf8mb4 \
    -u"$MYSQL_USERNAME" -p"$MYSQL_PASSWORD" "$DB_NAME" "$@"
}

echo "==> 1/3 生成演示 PDF"
python3 scripts/demo/make-demo-invoice-pdf.py

echo "==> 2/3 写入演示数据"
mysql_exec < scripts/demo/icbc-demo-invoices.sql

echo "==> 3/3 校准 file_size（按磁盘真实大小）"
for no in ACQ20260922DEMO01 ACQ20260922DEMO02 ACQ20260922DEMO04 ACQ20260922DEMO05; do
  pdf="/tmp/icbc-demo/${no}-invoice.pdf"
  if [[ ! -f "$pdf" ]]; then
    echo "缺少 $pdf：先跑 python3 scripts/demo/make-demo-invoice-pdf.py" >&2
    exit 1
  fi
  size=$(stat -f%z "$pdf" 2>/dev/null || stat -c%s "$pdf")
  mysql_exec -e "
    UPDATE icbc_invoice_download SET file_size = ${size} WHERE partner_order_id = '${no}';
    UPDATE icbc_invoice_file f
      JOIN icbc_invoice_download d ON d.id = f.download_id
      SET f.file_size = ${size}
      WHERE d.partner_order_id = '${no}';" >/dev/null
  echo "    ${no} -> ${size} bytes"
done

echo
echo "完成。页面上看：后台「财务票务 / 发票下载与证据」列表应出现上述 ${DEMO_NO} 四条，可直接「下载 PDF」。"
