#!/usr/bin/env python3
"""生成本地演示用的「发票原件」PDF（不是真发票，只为了把下载链路跑通）。

配合 `scripts/demo/icbc-demo-invoices.sql` 使用：SQL 里的 icbc_invoice_file.file_path 指向这里
写出的文件，服务端下载时直接读磁盘上的这份 PDF，所以两边必须成对存在。

为什么放 /tmp：它只是本地联调数据，不进仓库、不进部署库；重开机后重跑本脚本 + 那条 SQL 即可。

用法：
    python3 scripts/demo/make-demo-invoice-pdf.py
"""

import os

OUT_DIR = "/tmp/icbc-demo"

# (文件名主体, 发票号码, 货品, 金额文案)
# 文件正文只改发票号 / 金额这类行，且用 ASCII：不依赖中文字体，尺寸稳定可复现。
INVOICES = [
    ("ACQ20260922DEMO01", "25500123456789012341", "scrap steel", "CNY 30420.00"),
    ("ACQ20260922DEMO02", "25500123456789012342", "scrap aluminium", "CNY 27950.00"),
    ("ACQ20260922DEMO04", "25500123456789012343", "scrap steel", "CNY 20000.00"),
    ("ACQ20260922DEMO05", "25500123456789012344", "scrap steel", "CNY 60000.00"),
]


def pdf(text_lines, path):
    lines = []
    y = 720
    for t in text_lines:
        lines.append(f"BT /F1 14 Tf 72 {y} Td ({t}) Tj ET")
        y -= 26
    content = "\n".join(lines).encode("latin-1", "replace")
    objs = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
        b"<< /Length " + str(len(content)).encode() + b" >>\nstream\n" + content + b"\nendstream",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
    ]
    out = bytearray(b"%PDF-1.4\n")
    offsets = []
    for i, body in enumerate(objs, start=1):
        offsets.append(len(out))
        out += f"{i} 0 obj\n".encode() + body + b"\nendobj\n"
    xref = len(out)
    out += f"xref\n0 {len(objs)+1}\n".encode()
    out += b"0000000000 65535 f \n"
    for off in offsets:
        out += f"{off:010d} 00000 n \n".encode()
    out += f"trailer\n<< /Size {len(objs)+1} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n".encode()
    open(path, "wb").write(bytes(out))
    print(f"{path} {len(out)} bytes")


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for acquisition_no, invoice_no, item, amount in INVOICES:
        pdf(
            [
                "DEMO INVOICE (local seed, not a real tax invoice)",
                f"Invoice No: {invoice_no}",
                "Seller: ZHOU RENFA",
                "Buyer: CHONGQING YUBEI RECYCLING CO., LTD.",
                f"Item: {item}",
                f"Amount: {amount}",
                "This PDF only exists to exercise the invoice download path.",
            ],
            f"{OUT_DIR}/{acquisition_no}-invoice.pdf",
        )


if __name__ == "__main__":
    main()
