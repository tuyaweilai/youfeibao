// playwright 不装在仓库里（只在临时目录），所以按候选路径动态解析，
// 让脚本能直接从仓库跑，不必先拷到别处。
async function loadPlaywright() {
  const candidates = [
    process.env.PLAYWRIGHT_HOME && `${process.env.PLAYWRIGHT_HOME}/node_modules/playwright/index.mjs`,
    '/tmp/yfb-e2e/node_modules/playwright/index.mjs'
  ].filter(Boolean)
  for (const c of candidates) {
    try { return await import(c) } catch {}
  }
  try { return await import('playwright') } catch {}
  console.error('找不到 playwright。先做一次性准备：')
  console.error('  mkdir -p /tmp/yfb-e2e && cd /tmp/yfb-e2e && npm i playwright')
  console.error('或用 PLAYWRIGHT_HOME=<含 node_modules 的目录> 指定。')
  process.exit(2)
}
const { chromium } = await loadPlaywright()

const HOSTS = ['yfbadmin','yfbwuliu','yfbgeren','yfbqiye'].map(h=>`${h}.baibaitan.com`)
const rules = HOSTS.map(h=>`MAP ${h} 47.99.49.104`).join(', ')
const browser = await chromium.launch({ channel:'chrome', args:[`--host-resolver-rules=${rules}`,'--no-sandbox'] })
let bad = 0
for (const host of HOSTS) {
  const ctx = await browser.newContext(); const page = await ctx.newPage()
  const reqs = []; page.on('request', r=>reqs.push(r.url()))
  const failed = []; page.on('requestfailed', r=>failed.push(r.url()))
  await page.goto(`http://${host}/`, { waitUntil:'networkidle', timeout:45000 })
  const offOrigin = reqs.filter(u => !u.startsWith(`http://${host}/`) && !u.startsWith('data:') && !u.startsWith('blob:'))
  const local = reqs.filter(u => /localhost|127\.0\.0\.1/.test(u))
  const title = await page.title()
  const ok = !offOrigin.length && !local.length && !failed.length
  if (!ok) bad++
  console.log(`${ok?'✅':'❌'} ${host.padEnd(26)} ${title.padEnd(22)} 请求=${reqs.length} 跨域=${offOrigin.length} localhost=${local.length} 失败=${failed.length}`)
  offOrigin.forEach(u=>console.log(`      跨域: ${u}`))
  local.forEach(u=>console.log(`      localhost: ${u}`))
  failed.forEach(u=>console.log(`      失败: ${u}`))
  await ctx.close()
}
await browser.close()
console.log(bad===0 ? '\n✅ 四端浏览器视角全部干净（同源、无 localhost、无失败请求）' : `\n❌ ${bad} 端有问题`)
process.exit(bad?1:0)
