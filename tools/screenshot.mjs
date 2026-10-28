#!/usr/bin/env node
/**
 * 功能截图脚本（无第三方依赖）。
 *
 * 原理：以无头模式启动系统 Chrome，开启 CDP 远程调试，通过 WebSocket 直接调用
 * DevTools 协议（Node 22 内置 WebSocket + fetch），完成：
 *   1) 调用后端接口登录（验证码从 Redis 旁路读取）；
 *   2) 打开登录页截图（未登录态）；
 *   3) 注入 token 到 localStorage，逐页导航并截图；
 *   4) 保存到 docs/images/。
 *
 * 依赖：系统 Chrome、Node >= 22、Docker（读取验证码）、本机已启动前后端。
 * 环境变量：REDIS_PASSWORD（Windows 用户级环境变量已注入）；
 *          CHROME_PATH 可选，默认系统 Chrome 路径。
 * 用法（仓库根目录）：node tools/screenshot.mjs
 */
import { spawn, execSync } from 'node:child_process'
import { mkdirSync, writeFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { setTimeout as sleep } from 'node:timers/promises'

const APP = process.env.DRS_APP_BASE || 'http://127.0.0.1:5173'
const API = process.env.DRS_API_BASE || 'http://127.0.0.1:8080/api/v1'
const CHROME = process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const OUT = 'docs/images'
const PORT = Number(process.env.CDP_PORT || 9333)
const WIDTH = 1680
const HEIGHT = 900

// [路由, 输出文件名]
const PAGES = [
  ['/dashboard', 'dashboard'],
  ['/screening/upload', 'screening-upload'],
  ['/screening/records', 'screening-records'],
  ['/screening/todos', 'screening-todos'],
  ['/screening/patients', 'screening-patients'],
  ['/screening/statistics', 'screening-statistics'],
  ['/model-info', 'model-info'],
  ['/admin/users', 'admin-users'],
  ['/admin/dicts', 'admin-dicts'],
  ['/admin/logs', 'admin-logs'],
  ['/guide', 'guide'],
  ['/about', 'about'],
  ['/profile', 'profile']
]

async function login() {
  const cap = await (await fetch(`${API}/auth/captcha`)).json()
  const key = cap.data.captchaKey
  const code = execSync(
    `docker exec fjzhmz-redis redis-cli -p 26379 -a ${process.env.REDIS_PASSWORD} --no-auth-warning get drs:auth:captcha:${key}`
  ).toString().trim()
  const res = await (await fetch(`${API}/auth/sessions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123', captchaKey: key, captchaCode: code })
  })).json()
  if (!res?.data?.token) throw new Error('登录失败：' + JSON.stringify(res))
  return res.data.token
}

async function firstRecordId(token) {
  const res = await (await fetch(`${API}/biz/screening-records?current=1&pageSize=1`, {
    headers: { 'X-Access-Token': token }
  })).json()
  return res?.data?.list?.[0]?.id
}

class CDP {
  constructor(ws) {
    this.ws = ws
    this.seq = 0
    this.pending = new Map()
    ws.addEventListener('message', (ev) => {
      const msg = JSON.parse(ev.data)
      if (msg.id && this.pending.has(msg.id)) {
        const { resolve, reject } = this.pending.get(msg.id)
        this.pending.delete(msg.id)
        msg.error ? reject(new Error(msg.error.message)) : resolve(msg.result)
      }
    })
  }
  send(method, params = {}) {
    const id = ++this.seq
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject })
      this.ws.send(JSON.stringify({ id, method, params }))
    })
  }
  close() { try { this.ws.close() } catch { /* ignore */ } }
}

async function waitForCDP() {
  for (let i = 0; i < 50; i++) {
    try {
      const r = await fetch(`http://127.0.0.1:${PORT}/json/version`)
      if (r.ok) return
    } catch { /* not ready */ }
    await sleep(200)
  }
  throw new Error('Chrome CDP 未就绪')
}

async function main() {
  mkdirSync(OUT, { recursive: true })
  const profile = `${tmpdir()}\\chrome-shot-${Date.now()}`
  const chrome = spawn(CHROME, [
    '--headless=new',
    `--remote-debugging-port=${PORT}`,
    '--remote-allow-origins=*',
    `--user-data-dir=${profile}`,
    '--no-first-run',
    '--no-default-browser-check',
    '--disable-gpu',
    '--hide-scrollbars',
    'about:blank'
  ], { stdio: 'ignore' })

  try {
    await waitForCDP()
    const target = await (await fetch(`http://127.0.0.1:${PORT}/json/new?url=about:blank`, { method: 'PUT' })).json()
    const ws = new WebSocket(target.webSocketDebuggerUrl)
    await new Promise((res, rej) => { ws.addEventListener('open', res); ws.addEventListener('error', rej) })
    const cdp = new CDP(ws)

    await cdp.send('Page.enable')
    await cdp.send('Runtime.enable')
    await cdp.send('Emulation.setDeviceMetricsOverride', {
      width: WIDTH, height: HEIGHT, deviceScaleFactor: 1, mobile: false
    })

    async function goto(url, wait = 2200) {
      await cdp.send('Page.navigate', { url })
      await sleep(wait)
    }
    async function shot(name) {
      const { data } = await cdp.send('Page.captureScreenshot', { format: 'png', fromSurface: true })
      const file = `${OUT}/${name}.png`
      writeFileSync(file, Buffer.from(data, 'base64'))
      console.log('  ✓', file)
    }

    // 1) 登录页（未登录态）
    await goto(`${APP}/login`, 1800)
    await shot('login')

    // 2) 登录并注入 token
    const token = await login()
    await cdp.send('Runtime.evaluate', { expression: `localStorage.setItem('token', ${JSON.stringify(token)})` })

    // 3) 逐页截图
    for (const [route, name] of PAGES) {
      await goto(`${APP}${route}`)
      await shot(name)
    }

    // 4) 诊断报告（需要一条记录 id）
    const id = await firstRecordId(token)
    if (id) {
      await goto(`${APP}/screening/records/${id}/report`, 2600)
      await shot('screening-report')
    }

    cdp.close()
  } finally {
    chrome.kill()
    await sleep(300)
    try { rmSync(profile, { recursive: true, force: true }) } catch { /* ignore */ }
  }
  console.log('==> 截图完成，输出目录：', OUT)
}

main().catch((e) => { console.error('截图失败：', e.message); process.exit(1) })
