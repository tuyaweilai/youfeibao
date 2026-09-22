<template>
  <div class="login-page">
    <header class="login-header">
      <div class="brand">
        <span class="brand-mark">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              fill="currentColor"
              d="m21.82 15.42l-2.5 4.33c-.49.86-1.4 1.31-2.32 1.25h-2v2l-2.5-4.5L15 14v2h2.82l-2.22-3.85l4.33-2.5l1.8 3.12c.52.77.59 1.8.09 2.65M9.21 3.06h5c.98 0 1.83.57 2.24 1.39l1 1.74l1.73-1l-2.64 4.41l-5.15.09l1.73-1l-1.41-2.45l-2.21 3.85l-4.34-2.5l1.8-3.12c.41-.83 1.26-1.41 2.25-1.41m-4.16 16.7l-2.5-4.33c-.49-.85-.42-1.87.09-2.64l1-1.73l-1.73-1l5.14.08l2.65 4.42l-1.73-1L6.56 16H11v5H7.4a2.51 2.51 0 0 1-2.35-1.24"
            />
          </svg>
        </span>
        <span>{{ underlineToHump(appStore.getTitle) }}</span>
        <span class="brand-caption">企业经营管理平台</span>
      </div>
      <div class="login-tools">
        <ThemeSwitch aria-label="切换深色模式" />
        <LocaleDropdown />
      </div>
    </header>

    <main class="login-main">
      <section class="brand-panel" aria-labelledby="brand-heading">
        <div class="brand-intro">
          <div class="eyebrow"><span></span> 让资源循环，让经营有序</div>
          <h1 id="brand-heading">每一份资源，<br />都有新的价值<span>。</span></h1>
          <p>连接收购、库存与结算，<br />让再生资源回收的每一步清晰可循。</p>
        </div>
        <div class="resource-visual" aria-hidden="true">
          <div class="orbit orbit-outer"></div>
          <div class="orbit orbit-inner"></div>
          <div class="resource-core">
            <svg viewBox="0 0 24 24">
              <path
                fill="currentColor"
                d="m21.82 15.42l-2.5 4.33c-.49.86-1.4 1.31-2.32 1.25h-2v2l-2.5-4.5L15 14v2h2.82l-2.22-3.85l4.33-2.5l1.8 3.12c.52.77.59 1.8.09 2.65M9.21 3.06h5c.98 0 1.83.57 2.24 1.39l1 1.74l1.73-1l-2.64 4.41l-5.15.09l1.73-1l-1.41-2.45l-2.21 3.85l-4.34-2.5l1.8-3.12c.41-.83 1.26-1.41 2.25-1.41m-4.16 16.7l-2.5-4.33c-.49-.85-.42-1.87.09-2.64l1-1.73l-1.73-1l5.14.08l2.65 4.42l-1.73-1L6.56 16H11v5H7.4a2.51 2.51 0 0 1-2.35-1.24"
              />
            </svg>
          </div>
          <span class="orbit-label label-purchase"><i></i> 收购登记</span>
          <span class="orbit-label label-stock"><i></i> 库存管理</span>
          <span class="orbit-label label-payment"><i></i> 结算开票</span>
          <span class="orbit-dot"></span>
        </div>
        <div class="brand-bottom">
          <span>业务有据</span><span>流转有序</span><span>全程可溯</span>
        </div>
      </section>

      <section class="login-panel" aria-label="账号登录">
        <div class="login-card">
          <div class="card-eyebrow">WELCOME BACK</div>
          <LoginForm />
          <MobileForm />
          <QrCodeForm />
          <RegisterForm />
          <SSOLoginVue />
          <ForgetPasswordForm />
          <div class="login-help">账号由企业管理员分配，如需帮助请联系管理员</div>
        </div>
        <p class="platform-note">有废宝 · 再生资源回收企业经营平台</p>
      </section>
    </main>
    <footer class="login-footer"
      ><span>让回收更高效，让价值再生</span><span>YOUFEIBAO</span></footer
    >
  </div>
</template>

<script lang="ts" setup>
import { underlineToHump } from '@/utils'
import { useAppStore } from '@/store/modules/app'
import { ThemeSwitch } from '@/layout/components/ThemeSwitch'
import { LocaleDropdown } from '@/layout/components/LocaleDropdown'
import {
  LoginForm,
  MobileForm,
  QrCodeForm,
  RegisterForm,
  SSOLoginVue,
  ForgetPasswordForm
} from './components'

defineOptions({ name: 'Login' })
const appStore = useAppStore()
</script>

<style lang="scss" scoped>
.login-page {
  --login-surface: #fff;
  --login-text: #203b33;
  --login-muted: #65776f;
  --login-border: #dce5df;
  --el-color-primary: #24745b;
  --el-color-primary-light-3: #428d74;
  --el-color-primary-dark-2: #18563f;
  --el-color-primary-light-9: #edf5f0;
  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 0 5%;
  color: var(--login-text);
  background: #f5f7f3;
  flex-direction: column;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.login-header {
  display: flex;
  width: 100%;
  max-width: 1320px;
  min-height: 104px;
  margin: 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 23px;
  font-weight: 650;
  letter-spacing: 1px;
}
.brand-mark {
  display: grid;
  width: 42px;
  height: 42px;
  color: #24745b;
  place-items: center;
}
.brand-mark svg {
  width: 34px;
  height: 34px;
}
.brand-caption {
  padding-left: 18px;
  margin-left: 6px;
  border-left: 1px solid var(--login-border);
  color: var(--login-muted);
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 2px;
}
.login-tools {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 22px;
}
.login-main {
  display: grid;
  width: 100%;
  max-width: 1240px;
  min-height: 680px;
  margin: auto;
  grid-template-columns: 1.05fr 1fr;
  align-items: stretch;
  gap: 64px;
}
.brand-panel {
  position: relative;
  display: flex;
  overflow: hidden;
  min-width: 0;
  padding: 54px 48px 32px;
  border-radius: 24px;
  color: #f3f8f1;
  background: radial-gradient(ellipse at 90% 80%, #316e53 0, transparent 60%), #173f32;
  flex-direction: column;
}
.brand-intro {
  position: relative;
  z-index: 1;
}
.eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #ceded0;
  font-size: 12px;
  letter-spacing: 3px;
}
.eyebrow span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c5dd97;
}
h1 {
  margin: 28px 0 20px;
  font-size: clamp(32px, 3.2vw, 46px);
  font-weight: 550;
  line-height: 1.5;
  letter-spacing: 2px;
}
h1 span {
  color: #c8e2a0;
}
.brand-intro p {
  margin: 0;
  color: #c3d5ca;
  font-size: 14px;
  line-height: 1.9;
  letter-spacing: 1px;
}
.resource-visual {
  position: relative;
  height: 280px;
  margin: 14px -20px 0;
}
.orbit {
  position: absolute;
  top: 50%;
  left: 50%;
  border: 1px solid rgb(219 238 210 / 18%);
  border-radius: 50%;
  transform: translate(-50%, -50%) rotate(-25deg);
}
.orbit-outer {
  width: 350px;
  height: 220px;
}
.orbit-inner {
  width: 270px;
  height: 170px;
  transform: translate(-50%, -50%) rotate(30deg);
}
.resource-core {
  position: absolute;
  top: 50%;
  left: 50%;
  display: grid;
  width: 124px;
  height: 124px;
  border: 1px solid rgb(218 241 203 / 35%);
  border-radius: 32px;
  color: #d6e9b7;
  background: linear-gradient(145deg, #588062, #274f3c);
  box-shadow:
    0 20px 50px #0d2f3266,
    inset 0 1px 0 #c8e2a044;
  transform: translate(-50%, -50%) rotate(-10deg);
  place-items: center;
}
.resource-core svg {
  width: 70px;
  height: 70px;
  transform: rotate(10deg);
}
.orbit-label {
  position: absolute;
  display: flex;
  padding: 10px 14px;
  border: 1px solid #5d806b;
  border-radius: 8px;
  color: #e0eadd;
  background: #28523f;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}
.orbit-label i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #c5dd97;
}
.label-purchase {
  top: 34px;
  left: 8%;
}
.label-stock {
  top: 92px;
  right: 0;
}
.label-payment {
  bottom: 16px;
  left: 20%;
}
.orbit-dot {
  position: absolute;
  right: 20%;
  bottom: 42px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #c4da9e;
  box-shadow: 0 0 0 6px #c4da9e12;
}
.brand-bottom {
  display: flex;
  padding-top: 22px;
  margin-top: auto;
  border-top: 1px solid #ffffff26;
  color: #cbdbd0;
  justify-content: space-between;
  font-size: 12px;
  letter-spacing: 2px;
}
.login-panel {
  display: flex;
  min-width: 0;
  padding: 32px 0;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}
.login-card {
  width: 100%;
  max-width: 440px;
  padding: 38px;
  border: 1px solid var(--login-border);
  border-radius: 20px;
  background: var(--login-surface);
  box-shadow: 0 16px 48px #1a403306;
}
.card-eyebrow {
  margin-bottom: 12px;
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 2px;
}
.login-help {
  padding-top: 24px;
  margin-top: 24px;
  border-top: 1px solid var(--login-border);
  color: var(--login-muted);
  font-size: 12px;
  line-height: 1.8;
  text-align: center;
}
.platform-note {
  margin: 24px 0 0;
  color: var(--login-muted);
  font-size: 12px;
  letter-spacing: 1px;
}
.login-footer {
  display: flex;
  width: 100%;
  max-width: 1320px;
  padding: 30px 0;
  margin: 0 auto;
  color: var(--login-muted);
  justify-content: space-between;
  font-size: 11px;
  letter-spacing: 2px;
}
:deep(.login-form) {
  width: 100%;
}
:deep(.login-form h2) {
  margin: 0;
  color: var(--login-text);
  font-size: 28px;
  font-weight: 600;
  text-align: left;
}
:deep(.login-form .el-form-item) {
  margin-bottom: 22px;
}
:deep(.login-form .el-form-item__label) {
  padding-bottom: 8px;
  color: var(--login-text);
  font-size: 13px;
  line-height: 20px;
}
:deep(.login-form .el-input__wrapper) {
  min-height: 48px;
  padding: 0 14px;
  border-radius: 8px;
  background: var(--login-surface);
  box-shadow: 0 0 0 1px var(--login-border) inset;
  transition: box-shadow 180ms;
}
:deep(.login-form .el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--el-color-primary) inset,
    0 0 0 3px #43866620;
}
:deep(.login-form .el-input__inner) {
  color: var(--login-text);
}
:deep(.login-form .el-input__inner::placeholder) {
  color: var(--login-muted);
}
:deep(.login-form .el-input__prefix) {
  margin-right: 8px;
  color: var(--login-muted);
}
:deep(.login-form .el-button--primary:not(.is-link)) {
  min-height: 48px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 3px;
  transition: background-color 180ms;
}
:deep(.login-form .el-checkbox__label) {
  color: var(--login-muted);
}
:deep(.el-link),
:deep(.el-checkbox),
:deep(.login-tools .el-switch),
:deep(.login-tools .el-dropdown) {
  min-height: 44px;
}
:deep(.el-button:focus-visible),
:deep(.el-link:focus-visible) {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 4px;
}
:global(.dark .login-page) {
  --login-surface: #192e27;
  --login-text: #ecf2ec;
  --login-muted: #aec1b5;
  --login-border: #3a5145;
  --el-color-primary: #8ac9a7;
  --el-color-primary-light-3: #a0d5b8;
  --el-color-primary-dark-2: #70ae8d;
  background: #10231c;
}
:global(.dark .login-page .el-button--primary:not(.is-link)) {
  color: #102d20;
}
:global(.dark .login-page .brand-mark) {
  color: #8ac9a7;
}
@media (min-width: 1600px) {
  .login-main {
    min-height: 740px;
  }
  .resource-visual {
    margin-top: 40px;
  }
}
@media (max-width: 1100px) {
  .login-main {
    gap: 28px;
  }
  .brand-panel {
    padding: 42px 30px 28px;
  }
  .login-card {
    padding: 30px;
  }
}
@media (max-width: 800px) {
  .login-header {
    min-height: 88px;
  }
  .brand-caption {
    display: none;
  }
  .login-main {
    max-width: 460px;
    min-height: auto;
    grid-template-columns: 1fr;
    gap: 0;
  }
  .brand-panel {
    display: none;
  }
  .login-panel {
    padding: 24px 0;
  }
  .login-footer {
    justify-content: center;
  }
  .login-footer span:last-child {
    display: none;
  }
}
@media (max-width: 480px) {
  .login-page {
    padding: 0 20px;
  }
  .brand {
    font-size: 21px;
    gap: 6px;
  }
  .login-tools {
    gap: 16px;
  }
  .login-card {
    padding: 28px 22px;
    border-radius: 16px;
  }
  :deep(.login-form .el-input__inner) {
    font-size: 16px;
  }
  .login-help {
    font-size: 11px;
  }
}
@media (prefers-reduced-motion: reduce) {
  :deep(*) {
    transition: none !important;
    animation: none !important;
  }
}
</style>
