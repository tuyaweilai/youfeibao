#!/bin/bash
# 有废宝每日数据库备份，保留 14 天。由 root crontab 调用。
set -euo pipefail
cd /opt/youfeibao
set -a; . ./.env.prod; set +a
DIR=/opt/youfeibao/backup
mkdir -p "$DIR"
F="$DIR/ruoyi-vue-pro-$(date +%Y%m%d_%H%M%S).sql.gz"
docker exec yfb-mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --quick --routines --triggers --events \
  --default-character-set=utf8mb4 --set-gtid-purged=OFF --no-tablespaces --hex-blob \
  'ruoyi-vue-pro' | gzip -6 > "$F"
echo "$(date '+%F %T') backup ok: $F ($(du -h "$F" | cut -f1))" >> "$DIR/backup.log"
find "$DIR" -name 'ruoyi-vue-pro-*.sql.gz' -mtime +14 -delete
