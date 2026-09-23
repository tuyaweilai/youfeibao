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

const HOSTS = ['yfbqiye','yfbwuliu','yfbgeren'].map(h=>`${h}.baibaitan.com`)
const browser = await chromium.launch({
  channel:'chrome',
  args:[`--host-resolver-rules=${HOSTS.map(h=>`MAP ${h} 47.99.49.104`).join(', ')}`,'--no-sandbox']
})
const targets = [
  { host:'yfbqiye.baibaitan.com', name:'企业收货端', btn:'进入工作台', home:/工作台|首页|收货/ },
  { host:'yfbwuliu.baibaitan.com', name:'司机端',     btn:'登录并查看任务', home:/任务|首页/ },
]
let fail = 0
for (const t of targets) {
  console.log('='.repeat(72))
  console.log(`${t.name}   ${t.host}`)
  console.log('='.repeat(72))
  const ctx = await browser.newContext({ viewport:{width:420,height:900} })
  const page = await ctx.newPage()
  const api = []
  page.on('response', r => { if (r.url().includes('/admin-api/')) api.push(`${r.status()} ${new URL(r.url()).pathname}`) })
  const jsErrs = []; page.on('pageerror', e => jsErrs.push(e.message))
  const netFail = []; page.on('requestfailed', r => netFail.push(r.url()))

  await page.goto(`http://${t.host}/`, { waitUntil:'networkidle', timeout:45000 })
  await page.waitForTimeout(2000)

  const inputs = page.locator('input')
  const n = await inputs.count()
  console.log(`输入框数量: ${n}`)
  // 0=租户编号 1=账号 2=密码
  await inputs.nth(0).fill('1')
  await inputs.nth(1).fill('driver01')
  await inputs.nth(2).fill('admin123')
  console.log('已填入: 租户=1 账号=driver01 密码=***')

  // uni-app 的按钮不是原生 button，用文本定位可点元素
  await page.getByText(t.btn, { exact:false }).last().click({ timeout:15000 })
  await page.waitForTimeout(7000)

  const url = page.url()
  const body = (await page.textContent('body')) || ''
  const goHome = url.indexOf('/login') === -1
  console.log(`\n登录后 URL: ${url}`)
  console.log(`关键接口: ${api.join('  |  ') || '(无)'}`)
  console.log(`已离开登录页: ${goHome ? '是' : '否'}`)
  console.log(`页面文本: ${body.replace(/\s+/g,' ').slice(0,260)}`)
  if (netFail.length) console.log(`失败请求: ${netFail.slice(0,3).join(' | ')}`)
  if (jsErrs.length) console.log(`JS 报错: ${jsErrs.slice(0,3).join(' | ')}`)

  const loginOk = api.some(a => a.startsWith('200') && a.includes('/auth/login'))
  if (!loginOk) { console.log('❌ 未见 200 的 /auth/login'); fail++ }
  else if (!goHome) { console.log('❌ 登录接口成功但没跳走'); fail++ }
  else console.log('✅ 登录成功并进入业务页')
  await page.screenshot({ path:`/tmp/yfb-e2e/${t.host}.png` })
  await ctx.close()
  console.log()
}
await browser.close()
console.log(fail===0 ? '✅ 两端浏览器登录均通过' : `❌ ${fail} 端失败`)
process.exit(fail?1:0)
