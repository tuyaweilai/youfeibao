import { ref } from 'vue'
import { createAgreementSignUrl } from '@/api/public'
import { mintAgreementSignToken } from '@/api/seller'
import { openExternalUrl } from '@/utils/external'

/**
 * 「去签署」待签框架收购协议（#95）。
 *
 * <p>① 用登录态换一枚绑定收方的 ONBOARDING 一次性令牌；② 拿令牌调公开端点**现取**第三方签署链接
 * （不缓存、不复用、不发短信）；③ 打开签署页。首页的「最近待办」与交易页的「待我确认」都会点它，
 * 所以抽出来——两份复制迟早只剩一份是对的。
 */
export function useAgreementSign() {
  /** 进行中标记：防连点重复现取链接 */
  const signing = ref(false)

  async function signAgreement(naturalPersonId: number, payeeId: number) {
    if (signing.value) {
      return
    }
    signing.value = true
    uni.showLoading({ title: '正在打开签署页…', mask: true })
    try {
      const link = await mintAgreementSignToken(naturalPersonId, payeeId)
      if (!link.token) {
        throw new Error('未取到签署入口，请稍后重试')
      }
      const sign = await createAgreementSignUrl(link.token)
      if (!sign.signUrl) {
        throw new Error('未取到签署链接，请稍后重试')
      }
      uni.hideLoading()
      openExternalUrl(sign.signUrl)
    } catch (e) {
      uni.hideLoading()
      uni.showToast({ title: (e as Error).message || '去签署失败，请稍后重试', icon: 'none' })
    } finally {
      signing.value = false
    }
  }

  return { signing, signAgreement }
}
