<template>
  <view class="page" :class="{ 'page--empty': !naturalPersonId }">
    <SellerNoProfile v-if="!naturalPersonId" />

    <template v-else>
      <view class="card">
        <view class="card__title">发票与税费</view>
        <view class="year-row">
          <text class="link" @click="changeYear(-1)">上一年</text>
          <text class="year">{{ invoiceYear }} 年</text>
          <text class="link" @click="changeYear(1)">下一年</text>
        </view>
        <view class="group-summary">
          开票 {{ invoiceSummary.invoiceCount ?? 0 }} 张 · 金额 {{ invoiceSummary.totalInvoiceAmount ?? 0 }} 元
          · 税额 {{ invoiceSummary.totalTaxAmount ?? 0 }} 元
        </view>
        <view class="scope-note">{{ invoiceSummary.taxScopeNote }}</view>
      </view>
      <view v-if="!invoiceSummary.invoices?.length" class="card muted">这一年还没有开票记录。</view>
      <view v-for="invoice in invoiceSummary.invoices" :key="invoice.invoiceOrderId" class="card">
        <view class="record__row">
          <text class="record__cat">{{ invoice.acquirerName }}</text>
          <text class="record__amount">{{ invoice.invoiceAmount ?? 0 }} 元</text>
        </view>
        <view class="record__meta">发票号：{{ invoice.invoiceNo || '—' }}</view>
        <view class="three-lines">
          <view class="status-line">
            <text class="status-line__label">开票</text><text>{{ invoice.invoiceStatusName }}</text>
          </view>
          <view class="status-line">
            <text class="status-line__label">税费</text><text>{{ invoice.taxStatusName }}</text>
          </view>
          <view class="status-line">
            <text class="status-line__label">上传</text><text>{{ invoice.uploadStatusName }}</text>
          </view>
        </view>
        <view class="record__actions">
          <text class="link" @click="downloadInvoice(invoice.invoiceOrderId)">下载发票 PDF</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import SellerNoProfile from '@/components/SellerNoProfile.vue'
import { getInvoices, SellerInvoiceSummary } from '@/api/seller'
import { useSellerAuthStore } from '@/store/auth'
import { downloadWithAuth } from '@/utils/download'

defineOptions({ name: 'SellerInvoice' })

const auth = useSellerAuthStore()
const naturalPersonId = computed(() => auth.subject?.naturalPersonId || 0)
const invoiceYear = ref(new Date().getFullYear())
const invoiceSummary = reactive<SellerInvoiceSummary>({})

onLoad(() => {
  if (!auth.token) {
    uni.reLaunch({ url: '/pages/login/index' })
  }
})

onShow(() => {
  load()
})

async function load() {
  if (!naturalPersonId.value) {
    return
  }
  try {
    Object.assign(invoiceSummary, await getInvoices(naturalPersonId.value, invoiceYear.value))
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: 'none' })
  }
}

function changeYear(delta: number) {
  invoiceYear.value += delta
  load()
}

function downloadInvoice(invoiceOrderId: number) {
  downloadWithAuth(
    `/icbc/seller/portal/invoice/download?naturalPersonId=${naturalPersonId.value}&invoiceOrderId=${invoiceOrderId}`,
    '发票.pdf'
  ).catch((e) => uni.showToast({ title: (e as Error).message, icon: 'none' }))
}
</script>

<style lang="scss" scoped>
@import '../../styles/seller-page.scss';
</style>
