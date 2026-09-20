import { reactive, ref, watch } from 'vue'
import {
  FrameworkAgreementVO,
  SellerAuthorizationVO,
  SellerOnboardingVO,
  authorizeSeller,
  getOnboarding,
  leaveContactFallback,
  saveAgreement,
  startRealName,
  submitOnboarding,
  syncOnboarding,
  syncRealName
} from '../api/onboarding'
import { openIcbcFormHtml } from '../utils/icbcForm'

/**
 * 自然人准入四步的**业务逻辑与状态**（V7 #74）。
 *
 * 这一步是「分享逻辑、不分享模板」：四步的接口编排、状态、校验（协议必填项）、工行表单承载都在这里；
 * 各端只写自己的模板与文案。为什么不共享 `.vue`，见 `packages/field-shared/README.md`。
 *
 * 三条刻意的边界：
 *  1. **不代点确认**：这里没有、也不会有「替出售者确认」的动作，只提供「把链接交给本人」；
 *  2. **不代填身份**：人脸由本人在场做（ADR 0007：自然人零录入，数据由办理人录入）；
 *  3. **失败有出口**：入驻被拒可留联系方式走人工，不把现场卡死。
 */
export function useSellerOnboarding(payeeId: () => number | undefined) {
  const busy = ref(false)
  const overview = ref<SellerOnboardingVO>()
  const fallbackMobile = ref('')
  const agreement = reactive<FrameworkAgreementVO>({
    productName: '',
    quantity: '',
    specification: '',
    recyclePeriod: '',
    settlementMethod: '按净重结算'
  })
  const authorization = reactive<SellerAuthorizationVO>({
    reverseInvoiceAuthorized: true,
    taxAgencyAuthorized: true
  })

  function tips(message: string) {
    // 现场端只有 toast 这一种提示方式：两端一致，避免各写一套
    uni.showToast({ title: message, icon: 'none' })
  }

  async function load() {
    if (!payeeId()) {
      return
    }
    try {
      overview.value = await getOnboarding(payeeId()!)
      fallbackMobile.value = overview.value?.mobile || ''
      if (overview.value?.frameworkAgreement) {
        Object.assign(agreement, overview.value.frameworkAgreement)
      }
      if (overview.value?.authorization) {
        Object.assign(authorization, overview.value.authorization)
      }
    } catch (e) {
      tips((e as Error).message || '加载建档状态失败')
    }
  }

  /** 实人认证：工行 UI 页面接口返回自动提交表单，由新窗口承载（ADR 0016） */
  async function startRealNameStep() {
    busy.value = true
    try {
      const step = await startRealName(payeeId()!)
      openIcbcFormHtml(step.formHtml || '', '实人认证')
      tips('请在打开的页面里由本人完成人脸')
    } catch (e) {
      tips((e as Error).message || '发起实名认证失败')
    } finally {
      busy.value = false
    }
  }

  async function syncRealNameStep() {
    busy.value = true
    try {
      overview.value = await syncRealName(payeeId()!)
      tips('已同步认证结果')
    } catch (e) {
      tips((e as Error).message || '查询失败')
    } finally {
      busy.value = false
    }
  }

  async function submitOnboardingStep() {
    busy.value = true
    try {
      const step = await submitOnboarding({ payeeId: payeeId()! })
      openIcbcFormHtml(step.formHtml || '', '收方入驻')
      tips('请在打开的页面里绑定出售者本人银行卡')
    } catch (e) {
      tips((e as Error).message || '发起收方入驻失败')
    } finally {
      busy.value = false
    }
  }

  async function syncOnboardingStep() {
    busy.value = true
    try {
      overview.value = await syncOnboarding(payeeId()!)
      tips('已同步入驻结果')
    } catch (e) {
      tips((e as Error).message || '查询失败')
    } finally {
      busy.value = false
    }
  }

  async function leaveContact() {
    if (!fallbackMobile.value) {
      tips('请填手机号')
      return
    }
    busy.value = true
    try {
      await leaveContactFallback({ payeeId: payeeId()!, mobile: fallbackMobile.value })
      tips('已留下联系方式')
    } catch (e) {
      tips((e as Error).message || '提交失败')
    } finally {
      busy.value = false
    }
  }

  async function saveAgreementStep() {
    // 5 号公告第十七条要求收购合同或协议载明这几项，缺一项这张协议就不成立
    if (!agreement.productName || !agreement.quantity || !agreement.recyclePeriod || !agreement.settlementMethod) {
      tips('名称、数量、回收期次、结算方式都要填（税总 5 号公告第十七条）')
      return
    }
    busy.value = true
    try {
      await saveAgreement({ ...agreement, payeeId: payeeId()! })
      await load()
      tips('协议已保存')
    } catch (e) {
      tips((e as Error).message || '保存失败')
    } finally {
      busy.value = false
    }
  }

  async function authorizeStep() {
    busy.value = true
    try {
      await authorizeSeller({ ...authorization, payeeId: payeeId()! })
      await load()
      tips('授权已留痕')
    } catch (e) {
      tips((e as Error).message || '保存失败')
    } finally {
      busy.value = false
    }
  }

  watch(payeeId, load, { immediate: true })

  return {
    busy,
    overview,
    fallbackMobile,
    agreement,
    authorization,
    load,
    startRealNameStep,
    syncRealNameStep,
    submitOnboardingStep,
    syncOnboardingStep,
    leaveContact,
    saveAgreementStep,
    authorizeStep,
    tips
  }
}
