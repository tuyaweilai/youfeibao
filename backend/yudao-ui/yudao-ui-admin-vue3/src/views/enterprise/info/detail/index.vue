<template>
  <div v-loading="loading">
    <el-row :gutter="10">
      <!-- 左上角：基本信息 -->
      <el-col :span="14" class="detail-info-item">
        <el-card class="h-full" shadow="never">
          <template #header>
            <div class="card-header">
              <CardTitle title="基本信息" />
              <el-button size="small" text type="primary" @click="openForm('update')">
                编辑
              </el-button>
            </div>
          </template>
          <div class="enterprise-basic-info">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="企业名称">{{ enterprise.name }}</el-descriptions-item>
              <el-descriptions-item label="统一社会信用代码">{{ enterprise.creditCode }}</el-descriptions-item>
              <el-descriptions-item label="企业类型">
                <dict-tag :type="DICT_TYPE.ENTERPRISE_TYPE_ENUM" :value="enterprise.enterpriseType || 0" />
              </el-descriptions-item>
              <el-descriptions-item label="法定代表人">{{ enterprise.legalPersonName }}</el-descriptions-item>
              <el-descriptions-item label="注册资本">{{ enterprise.registeredCapital }} 万元</el-descriptions-item>
              <el-descriptions-item label="成立日期">{{ enterprise.establishmentDate ? formatDate(enterprise.establishmentDate, 'YYYY-MM-DD') : '-' }}</el-descriptions-item>
              <el-descriptions-item label="注册地址" :span="2">
                {{ enterprise.registeredAddressDetail || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="经营范围" :span="2">
                {{ enterprise.businessScope || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>
      <!-- 右上角：联系信息 -->
      <el-col :span="10" class="detail-info-item">
        <el-card class="h-full" shadow="never">
          <template #header>
            <CardTitle title="联系信息" />
          </template>
          <div class="enterprise-contact-info">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="联系人姓名">{{ enterprise.contactName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="联系人电话">{{ enterprise.contactPhone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="企业状态">
                <dict-tag :type="DICT_TYPE.ENTERPRISE_INFO_STATUS" :value="enterprise.status || 0" />
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ enterprise.createTime ? formatDate(enterprise.createTime) : '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>
      <!-- 下边：关联信息 -->
      <el-card shadow="never" style="width: 100%; margin-top: 20px">
        <template #header>
          <CardTitle title="关联信息" />
        </template>
        <el-tabs>
          <el-tab-pane label="企业资质">
            <EnterpriseQualificationList :enterprise-id="id" />
          </el-tab-pane>
          <el-tab-pane label="企业门店" lazy>
            <EnterpriseStoreList :enterprise-id="id" />
          </el-tab-pane>
          <el-tab-pane label="关联用户" lazy>
            <EnterpriseUserList :enterprise-id="id" />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </el-row>
  </div>

  <!-- 表单弹窗：编辑 -->
  <InfoForm ref="formRef" @success="getEnterpriseData(id)" />
</template>
<script lang="ts" setup>
import { ref, reactive, onMounted, unref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { InfoApi, InfoVO } from '@/api/enterprise/info'
import { useTagsViewStore } from '@/store/modules/tagsView'
import InfoForm from '@/views/enterprise/info/InfoForm.vue'
import { DICT_TYPE } from '@/utils/dict'
import { dateFormatter, dateFormatter2, formatDate } from '@/utils/formatTime'
import { CardTitle } from '@/components/Card/index'
import { ElMessage } from 'element-plus'
import EnterpriseQualificationList from './EnterpriseQualificationList.vue'
import EnterpriseStoreList from './EnterpriseStoreList.vue'
import EnterpriseUserList from './EnterpriseUserList.vue'

defineOptions({ name: 'EnterpriseDetail' })

const loading = ref(true) // 加载中
const enterprise = ref<InfoVO & { status?: number; createTime?: string }>({} as InfoVO)

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string) => {
  formRef.value.open(type, id)
}

/** 获取企业数据 */
const getEnterpriseData = async (id: number) => {
  loading.value = true
  try {
    enterprise.value = await InfoApi.getInfo(id)
  } finally {
    loading.value = false
  }
}

/** 初始化 */
const { currentRoute } = useRouter() // 路由
const { delView } = useTagsViewStore() // 视图操作
const route = useRoute()
const id = Number(route.params.id)

onMounted(() => {
  if (!id) {
    ElMessage.warning('参数错误，企业编号不能为空！')
    delView(unref(currentRoute))
    return
  }
  getEnterpriseData(id)
})
</script>
<style lang="css" scoped>
.detail-info-item:first-child {
  padding-left: 0 !important;
}

.detail-info-item:nth-child(2) {
  padding-right: 0 !important;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style> 