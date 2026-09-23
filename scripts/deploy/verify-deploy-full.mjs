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


const IP = '47.99.49.104'
const DOMAINS = [
  { host: 'yfbadmin.baibaitan.com', name: 'PC 管理后台' },
  { host: 'yfbwuliu.baibaitan.com', name: '司机端' },
  { host: 'yfbgeren.baibaitan.com', name: '自然人出售者端' },
  { host: 'yfbqiye.baibaitan.com', name: '企业收货端' }
]

// 本机 DNS 被代理的 fake-ip(198.18.0.x) 劫持，用 host-resolver-rules 强制指到真实 IP
const resolverRules = DOMAINS.map((d) => `MAP ${d.host} ${IP}`).join(', ')

const browser = await chromium.launch({
  channel: 'chrome', // 用系统已装的 Google Chrome，避开 playwright 浏览器版本缓存不匹配
  args: [`--host-resolver-rules=${resolverRules}`, '--no-sandbox']
})

let failures = 0
const fail = (msg) => { console.log(`  ❌ ${msg}`); failures++ }

console.log('='.repeat(78))
console.log('一、四端：浏览器视角的请求全部必须同源，且不得出现 localhost')
console.log('='.repeat(78))

for (const { host, name } of DOMAINS) {
  const ctx = await browser.newContext()
  const page = await ctx.newPage()
  const reqs = []
  page.on('request', (r) => reqs.push({ url: r.url(), method: r.method() }))
  const bad = []
  page.on('requestfailed', (r) => bad.push(`${r.url()} :: ${r.failure()?.errorText}`))
  const resp = []

  try {
    await page.goto(`http://${host}/`, { waitUntil: 'networkidle', timeout: 45000 })
  } catch (e) {
    fail(`${host} 打开失败: ${e.message.split('\n')[0]}`)
    await ctx.close()
    continue
  }

  const offOrigin = reqs.filter((r) => !r.url.startsWith(`http://${host}/`) && !r.url.startsWith('data:') && !r.url.startsWith('blob:'))
  const localhostHits = reqs.filter((r) => /localhost|127\.0\.0\.1/.test(r.url))

  const title = await page.title()
  console.log(`\n${host}  (${name})`)
  console.log(`  标题: ${title}`)
  console.log(`  请求总数: ${reqs.length}`)

  if (localhostHits.length) {
    fail(`${host} 有 ${localhostHits.length} 个请求打到 localhost：`)
    localhostHits.slice(0, 5).forEach((r) => console.log(`      ${r.method} ${r.url}`))
  } else {
    console.log('  ✅ 无 localhost / 127.0.0.1 请求')
  }
  if (offOrigin.length) {
    fail(`${host} 有跨域请求：${offOrigin.slice(0, 3).map((r) => r.url).join(', ')}`)
  } else {
    console.log('  ✅ 全部请求同源')
  }
  if (bad.length) {
    fail(`${host} 有失败请求：`)
    bad.slice(0, 5).forEach((b) => console.log(`      ${b}`))
  } else {
    console.log('  ✅ 无失败请求')
  }

  const apiCalls = reqs.filter((r) => r.url.includes('/admin-api/') || r.url.includes('/app-api/'))
  console.log(`  接口调用 ${apiCalls.length} 个:`)
  for (const c of apiCalls.slice(0, 4)) {
    const u = new URL(c.url)
    console.log(`      ${c.method} ${u.pathname}${u.search}`)
  }

  await ctx.close()
}

console.log('\n' + '='.repeat(78))
console.log('二、PC 后台：真实浏览器登录 → 进控制台')
console.log('='.repeat(78))

const ctx = await browser.newContext({ viewport: { width: 1600, height: 1000 } })
const page = await ctx.newPage()
const apiLog = []
page.on('response', async (r) => {
  const u = r.url()
  if (u.includes('/admin-api/')) {
    const p = new URL(u).pathname
    // 只记关键接口，避免刷屏
    if (/login|get-by-website|captcha|permission|menu|user\/get|dict|tenant/.test(p)) {
      apiLog.push(`${r.status()} ${p}`)
    }
  }
})
const pageErrors = []
page.on('pageerror', (e) => pageErrors.push(e.message))

try {
  await page.goto('http://yfbadmin.baibaitan.com/', { waitUntil: 'networkidle', timeout: 45000 })

  // 等登录表单
  await page.waitForSelector('input[placeholder*="用户名"], input[type="text"]', { timeout: 20000 })

  const usernameVal = await page.locator('input[placeholder*="用户名"]').first().inputValue().catch(() => '(取不到)')
  const passwordVal = await page.locator('input[type="password"]').first().inputValue().catch(() => '(取不到)')
  console.log(`\n  登录框预填值: 用户名="${usernameVal}"  密码="${'*'.repeat(passwordVal.length)}"`)
  if (usernameVal || passwordVal) {
    fail(`登录框仍有预填内容（用户名=${usernameVal}, 密码长度=${passwordVal.length}）`)
  } else {
    console.log('  ✅ 登录框为空，未预填任何口令')
  }

  // 真实填写并登录
  await page.locator('input[placeholder*="用户名"]').first().fill('admin')
  await page.locator('input[type="password"]').first().fill('admin123')
  const loginBtn = page.locator('button:has-text("登录")').first()
  await loginBtn.click()

  // 等离开登录页 / 进控制台
  await page.waitForURL((u) => !u.pathname.includes('/login'), { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(4000)

  const urlNow = page.url()
  const bodyText = (await page.textContent('body')) || ''
  const hasDashboard = /首页|工作台|系统管理|欢迎/.test(bodyText)
  const stillLogin = urlNow.includes('/login')

  console.log(`\n  登录后 URL: ${urlNow}`)
  console.log(`  页面含控制台字样: ${hasDashboard ? '是' : '否'}`)

  if (stillLogin || !hasDashboard) {
    fail('登录后没有进入控制台')
    console.log('  页面文本片段: ' + bodyText.replace(/\s+/g, ' ').slice(0, 300))
  } else {
    console.log('  ✅ 已进入控制台')
  }

  console.log('\n  关键接口:')
  apiLog.forEach((l) => console.log(`      ${l}`))

  const unauthorized = apiLog.filter((l) => l.startsWith('401') || l.startsWith('403'))
  if (unauthorized.length) fail(`有鉴权失败接口: ${unauthorized.join(', ')}`)

  await page.screenshot({ path: '/tmp/yfb-e2e/admin-after-login.png', fullPage: false })
  console.log('\n  截图: /tmp/yfb-e2e/admin-after-login.png')
} catch (e) {
  fail(`浏览器流程异常: ${e.message.split('\n')[0]}`)
}

if (pageErrors.length) {
  console.log('\n  页面 JS 报错:')
  pageErrors.slice(0, 5).forEach((e) => console.log(`      ${e}`))
} else {
  console.log('  ✅ 无页面 JS 报错')
}

await browser.close()

console.log('\n' + '='.repeat(78))
console.log(failures === 0 ? '✅ 全部通过' : `❌ 共 ${failures} 项失败`)
console.log('='.repeat(78))
process.exit(failures === 0 ? 0 : 1)
