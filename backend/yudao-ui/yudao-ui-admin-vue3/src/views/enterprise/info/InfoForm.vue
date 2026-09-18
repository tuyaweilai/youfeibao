<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
    >
      <!-- 基本信息 -->
      <el-divider content-position="left">基本信息</el-divider>
      <el-form-item label="企业名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入企业名称" />
      </el-form-item>
      <el-form-item label="统一社会信用代码" prop="creditCode">
        <el-input v-model="formData.creditCode" placeholder="请输入统一社会信用代码" />
      </el-form-item>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="企业类型" prop="enterpriseType">
            <el-select v-model="formData.enterpriseType" placeholder="请选择企业类型" class="w-full">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.ENTERPRISE_TYPE_ENUM)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
        </el-col>
        <el-col :span="12">
      <el-form-item label="成立日期" prop="establishmentDate">
        <el-date-picker
          v-model="formData.establishmentDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择成立日期"
              class="w-full"
            />
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="法定代表人" prop="legalPersonName">
            <el-input v-model="formData.legalPersonName" placeholder="请输入法定代表人姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="身份证号" prop="legalPersonIdCardNo">
            <el-input v-model="formData.legalPersonIdCardNo" placeholder="请输入法定代表人身份证号" />
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-form-item label="注册资本 (万元)" prop="registeredCapital">
        <el-input v-model="formData.registeredCapital" placeholder="请输入注册资本 (万元)" />
      </el-form-item>
      
      <el-form-item label="经营范围" prop="businessScope">
        <el-input 
          v-model="formData.businessScope" 
          type="textarea" 
          placeholder="请输入经营范围"
          :rows="3" 
        />
      </el-form-item>
      
      <!-- 地址信息 -->
      <el-divider content-position="left">注册地址</el-divider>
      <el-form-item label="所在地区" prop="areaId">
        <el-cascader
          v-model="selectedAreaId"
          :options="areaList"
          :props="areaProps"
          class="w-full"
          clearable
          filterable
          placeholder="请选择所在地区"
          @change="handleAreaChange"
        />
      </el-form-item>
      
      <el-form-item label="详细地址" prop="registeredAddressDetail">
        <el-input v-model="formData.registeredAddressDetail" placeholder="请输入详细地址" />
      </el-form-item>
      
      <!-- 联系人信息 -->
      <el-divider content-position="left">联系人信息</el-divider>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="联系人姓名" prop="contactName">
        <el-input v-model="formData.contactName" placeholder="请输入企业联系人姓名" />
      </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系人电话" prop="contactPhone">
        <el-input v-model="formData.contactPhone" placeholder="请输入企业联系人电话" />
      </el-form-item>
        </el-col>
      </el-row>
      
      <el-form-item label="营业执照附件" prop="businessLicenseFileId">
        <UploadImg v-model="formData.businessLicenseFileId" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { InfoApi, InfoVO } from '@/api/enterprise/info'
import * as AreaApi from '@/api/system/area'

/** 企业信息 表单 */
defineOptions({ name: 'InfoForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  name: undefined,
  creditCode: undefined,
  enterpriseType: undefined,
  legalPersonName: undefined,
  legalPersonIdCardNo: undefined,
  registeredCapital: undefined,
  establishmentDate: undefined,
  businessScope: undefined,
  registeredAddressProvinceCode: undefined,
  registeredAddressCityCode: undefined,
  registeredAddressDistrictCode: undefined,
  registeredAddressDetail: undefined,
  contactName: undefined,
  contactPhone: undefined,
  businessLicenseFileId: undefined
})
const formRules = reactive({
  name: [{ required: true, message: '企业名称不能为空', trigger: 'blur' }],
  creditCode: [{ required: true, message: '统一社会信用代码不能为空', trigger: 'blur' }],
  enterpriseType: [{ required: true, message: '企业类型不能为空', trigger: 'change' }],
  legalPersonName: [{ required: true, message: '法定代表人姓名不能为空', trigger: 'blur' }],
  legalPersonIdCardNo: [{ required: true, message: '法定代表人身份证号不能为空', trigger: 'blur' }]
})
const formRef = ref() // 表单 Ref

// 省市区级联选择相关
const areaList = ref([]) // 地区列表
const selectedAreaId = ref() // 选中的地区ID
const areaProps = {
  checkStrictly: true,
  value: 'id',
  label: 'name',
  emitPath: false, // 只返回选中节点的值
  expandTrigger: 'click' as const // 使用类型断言确保类型正确
}

// 初始化地区数据
const initAreaData = async () => {
  try {
    // 获取地区树
    const res = await AreaApi.getAreaTree()
    areaList.value = res
  } catch (error) {
    console.error('获取地区数据失败', error)
  }
}

// 处理地区选择变化
const handleAreaChange = (value) => {
  if (!value) {
    // 如果清空选择，同时清空相关的省市区编码
    formData.value.registeredAddressProvinceCode = undefined
    formData.value.registeredAddressCityCode = undefined
    formData.value.registeredAddressDistrictCode = undefined
    return
  }
  
  // 通过级联选择器获取到区域的ID后，需要从地区树中找到对应的省市区编码
  const findAreaCodesById = (id: number, areas: any[], parentInfo: { provinceId?: number, cityId?: number } = { provinceId: undefined, cityId: undefined }) => {
    for (const area of areas) {
      // 根据地区层级判断是省市区
      const level = area.level || (parentInfo.provinceId ? (parentInfo.cityId ? 3 : 2) : 1)
      
      if (area.id === id) {
        // 根据层级返回对应的省市区编码
        if (level === 1) { // 省级
          return {
            provinceCode: area.id.toString(),
            cityCode: undefined,
            districtCode: undefined
          }
        } else if (level === 2) { // 市级
          return {
            provinceCode: parentInfo.provinceId?.toString(),
            cityCode: area.id.toString(),
            districtCode: undefined
          }
        } else { // 区级
          return {
            provinceCode: parentInfo.provinceId?.toString(),
            cityCode: parentInfo.cityId?.toString(),
            districtCode: area.id.toString()
          }
        }
      }
      
      // 递归搜索子节点
      if (area.children?.length) {
        // 传递当前节点信息给子节点
        const newParentInfo = { 
          provinceId: level === 1 ? area.id : parentInfo.provinceId,
          cityId: level === 2 ? area.id : parentInfo.cityId
        }
        const result = findAreaCodesById(id, area.children, newParentInfo)
        if (result) return result
      }
    }
    return null
  }
  
  const areaCodes = findAreaCodesById(value, areaList.value)
  if (areaCodes) {
    formData.value.registeredAddressProvinceCode = areaCodes.provinceCode
    formData.value.registeredAddressCityCode = areaCodes.cityCode
    formData.value.registeredAddressDistrictCode = areaCodes.districtCode
  }
}

// 根据省市区编码反向设置级联选择器的值
const setSelectedAreaIdFromCodes = () => {
  // 按照优先级：区编码 > 市编码 > 省编码，确定应该选中的ID
  let targetId: number | undefined = undefined
  
  if (formData.value.registeredAddressDistrictCode) {
    targetId = parseInt(formData.value.registeredAddressDistrictCode)
  } else if (formData.value.registeredAddressCityCode) {
    targetId = parseInt(formData.value.registeredAddressCityCode)
  } else if (formData.value.registeredAddressProvinceCode) {
    targetId = parseInt(formData.value.registeredAddressProvinceCode)
  }
  
  if (targetId) {
    selectedAreaId.value = targetId
  }
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  
  // 加载地区数据
  await initAreaData()
  
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      const res = await InfoApi.getInfo(id)
      
      // 处理日期格式问题，确保能正确回显
      if (res.establishmentDate) {
        // 检查日期格式，可能是Date对象、时间戳或ISO字符串
        if (typeof res.establishmentDate === 'object' && res.establishmentDate instanceof Date) {
          // 如果是Date对象，转为YYYY-MM-DD格式
          const date = res.establishmentDate
          res.establishmentDate = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
        } else if (typeof res.establishmentDate === 'string') {
          // 如果是字符串但不是YYYY-MM-DD格式，需要转换
          // 尝试解析ISO格式、时间戳字符串等
          if (res.establishmentDate.includes('T') || res.establishmentDate.includes('/')) {
            const date = new Date(res.establishmentDate)
            res.establishmentDate = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
          }
          // 如果已经是YYYY-MM-DD格式，不需要转换
        } else if (typeof res.establishmentDate === 'number') {
          // 如果是时间戳
          const date = new Date(res.establishmentDate)
          res.establishmentDate = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
        }
      }
      
      formData.value = res
      
      // 设置级联选择器的值
      setSelectedAreaIdFromCodes()
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  await formRef.value.validate()
  // 提交请求
  formLoading.value = true
  try {
    const data = { ...formData.value } as unknown as InfoVO
    // 不需要手动转换日期格式，因为已经在日期控件中设置了value-format
    
    if (formType.value === 'create') {
      await InfoApi.createInfo(data)
      message.success(t('common.createSuccess'))
    } else {
      await InfoApi.updateInfo(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    name: undefined,
    creditCode: undefined,
    enterpriseType: undefined,
    legalPersonName: undefined,
    legalPersonIdCardNo: undefined,
    registeredCapital: undefined,
    establishmentDate: undefined,
    businessScope: undefined,
    registeredAddressProvinceCode: undefined,
    registeredAddressCityCode: undefined,
    registeredAddressDistrictCode: undefined,
    registeredAddressDetail: undefined,
    contactName: undefined,
    contactPhone: undefined,
    businessLicenseFileId: undefined
  }
  selectedAreaId.value = undefined
  formRef.value?.resetFields()
}
</script>