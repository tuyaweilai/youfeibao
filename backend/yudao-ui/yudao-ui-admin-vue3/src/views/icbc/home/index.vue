<!--
  首页：流程导航页（票 #114，形态照 参考/961790088107_.pic.jpg，另一家产品的后台首页）。

  这里的两组「第N步」是**操作顺序**，只是界面导览，不是领域概念：
  - 既不是「收购进度」的六档（ADR 0038：已登记 / 待自然人确认 / 待付款 / 已付款 / 已开票 / 已作废）；
  - 也不是「证据节点」（#107：出售者档案 / 货单 / 运输 / 合同 / 资金 / 收款确认）。
  三者粒度不同，不许互相套用；CONTEXT.md 也不为这五步加词条。

  ① 的顺序与参考件刻意不同：我们**先付款后反向开票**（工行一票一付，ADR 0038 的档位顺序是
  「待自然人确认 → 待付款 → 已付款 → 已开票」），参考件则是先开票后付款。
  ② 是我们还没有的一侧（ADR 0003 一期只做反向开票，用废企业是正向开票的受票方）：只排版，不给入口。
-->
<template>
  <div>
    <!-- 页头：不显示企业名——前端没有「本租户企业名称」这个值，要显示得加后端接口，另开一票 -->
    <div class="mb-15px flex items-start justify-between lt-sm:flex-col lt-sm:items-start">
      <div>
        <div class="text-18px font-bold">
          {{ username }}，按下面的流程从左到右一步步操作就行
        </div>
        <div class="mt-6px text-12px text-gray-500">
          点任意一步，直接进入对应功能。本页只做流程导览，今天要处理多少笔在「工作台」。
        </div>
      </div>
      <!-- 静态链接，不拉工作台接口：否则首页成了两个数据源拼的页面，且没有 icbc:workbench:query 的角色会缺半页 -->
      <el-button class="lt-sm:mt-10px" @click="go('/workbench')">
        <Icon icon="ep:list" class="mr-5px" /> 今日待办 → 工作台
      </el-button>
    </div>

    <!-- ① 对自然人的交易。注意 ContentWrap 的 #header 插槽是 v-if="title" 的下层，只给插槽不给 title 会整块不渲染 -->
    <ContentWrap title="① 对自然人的交易">
      <template #header>
        <span class="text-12px text-gray-500">
          从自然人手里收购报废产品，再给他付款、反向开票
        </span>
      </template>

      <div class="flex flex-wrap gap-8px">
        <el-card
          v-for="step in sellerSteps"
          :key="step.name"
          shadow="hover"
          class="min-w-[240px] flex-1"
          :class="checkPermi([step.permission]) ? 'cursor-pointer' : ''"
          :body-style="{ padding: '14px' }"
          @click="open(step)"
        >
          <div class="flex items-center justify-between">
            <span class="text-12px text-gray-400">{{ step.label }}</span>
            <Icon
              :icon="step.icon"
              :size="22"
              class="text-[var(--el-color-primary)]"
            />
          </div>
          <div class="mt-10px text-16px font-bold">{{ step.name }}</div>
          <div class="mt-6px min-h-32px text-12px text-gray-500 leading-16px">
            {{ step.summary }}
          </div>
          <div class="mt-8px text-12px">
            <span
              v-if="checkPermi([step.permission])"
              class="text-[var(--el-color-primary)]"
            >
              点这里进入 <Icon icon="ep:right" />
            </span>
            <span v-else class="text-gray-400">当前角色无此权限</span>
          </div>
        </el-card>
      </div>

      <!--
        结算确认与自然人确认**不占卡**（ADR 0018 / 0039）：这两步的操盘手是自然人本人与工行页面，
        企业这边只能等；在流程图上占格子会被读成「这里要我自己点一下」。
      -->
      <div class="mt-10px text-12px text-gray-400">
        注：出售者在结算单上「结算确认」后，系统按收购单逐张自动预下单，再由本人在工行页面确认开票信息
        ——这两件事都不用本企业操作，所以不占格子。付款按收购单逐笔进行，一笔款对应一张票。
      </div>
    </ContentWrap>

    <!-- ② 对用废企业的交易：只排版，不给入口 -->
    <ContentWrap title="② 对用废企业的交易">
      <template #header>
        <span class="text-12px text-gray-500">
          把收来的货卖给用废企业，再给用废企业开票
        </span>
        <span class="ml-10px text-12px text-gray-400">
          销售侧（正向开票）尚未开通，此处仅为示意
        </span>
      </template>

      <div class="flex flex-wrap gap-8px">
        <el-card
          v-for="card in buyerCards"
          :key="card.name"
          shadow="never"
          class="min-w-[320px] flex-1"
          :body-style="{ padding: '14px' }"
        >
          <div class="flex items-center justify-between">
            <span class="text-12px text-gray-400">{{ card.label }}</span>
            <Icon :icon="card.icon" :size="22" class="text-[var(--el-text-color-placeholder)]" />
          </div>
          <div class="mt-10px text-16px font-bold text-gray-500">{{ card.name }}</div>
          <div class="mt-6px min-h-32px text-12px text-gray-400 leading-16px">
            {{ card.summary }}
          </div>
          <div class="mt-8px text-12px text-gray-400">尚未开通</div>
        </el-card>
      </div>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { checkPermi } from '@/utils/permission'
import { useUserStore } from '@/store/modules/user'

// 组件名要与路由 name 一致（remaining.ts 的 /index），否则 tagsView 的 keep-alive 缓存匹配不上
defineOptions({ name: 'IcbcHome' })

interface HomeStep {
  /** 第N步 */
  label: string
  name: string
  summary: string
  icon: string
  /** 目标页面；空串表示本票不给入口（② 的三个示意卡） */
  route: string
  /** 目标页面的查询权限 */
  permission: string
}

// 图标取各步目标页面菜单上的同一枚（icbc-menu.sql）：同一功能两处两枚图标，
// 回头一定有人问是不是两件事。
const sellerSteps: HomeStep[] = [
  {
    label: '第1步',
    name: '建档',
    summary: '收方档案、框架协议与首次授权',
    icon: 'ep:user-filled',
    route: '/counterparty/payee',
    permission: 'icbc:payee-info:query'
  },
  {
    label: '第2步',
    name: '收购登记',
    summary: '拍照、过磅、扣杂，建收购单',
    icon: 'ep:edit-pen',
    route: '/recycling/acquisition',
    permission: 'icbc:acquisition:query'
  },
  {
    label: '第3步',
    name: '付款',
    summary: '按人结账、传回单',
    icon: 'ep:wallet',
    route: '/finance/payment',
    permission: 'icbc:payment:query'
  },
  {
    label: '第4步',
    name: '反向开票',
    summary: '给出售者开反向发票',
    icon: 'ep:document-add',
    route: '/finance/invoice',
    permission: 'icbc:invoice-order:query'
  },
  {
    label: '第5步',
    name: '一票一档',
    summary: '五流证据、链式视图',
    icon: 'ep:folder-opened',
    route: '/trace/evidence',
    permission: 'icbc:evidence:query'
  }
]

// 文字照抄参考件；这三样我们一个页面都没有（ADR 0003），所以 route 给空串，点不动
const buyerCards: HomeStep[] = [
  {
    label: '第1步',
    name: '用废企业建档',
    summary: '录入客户档案',
    icon: 'ep:office-building',
    route: '',
    permission: ''
  },
  {
    label: '第2步',
    name: '创建送货单',
    summary: '发货、运输、签收',
    icon: 'ep:van',
    route: '',
    permission: ''
  },
  {
    label: '第3步',
    name: '正向开票',
    summary: '给用废企业开票',
    icon: 'ep:ticket',
    route: '',
    permission: ''
  }
]

const router = useRouter()
const userStore = useUserStore()
const username = userStore.getUser.nickname

const go = (path: string) => {
  if (!path) return
  router.push(path)
}

const open = (step: HomeStep) => {
  // 无权限时卡片照旧显示（流程图缺一格比多点一下更让人困惑），只是不跳转
  if (!checkPermi([step.permission])) return
  go(step.route)
}
</script>
