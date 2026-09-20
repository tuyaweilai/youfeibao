<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="一个运输任务 = 一车 + 一司机 + 一次执行，可含多个停靠点（一车提三家）。每个停靠点各自交接、各自推进、各自凭证；一次集货不构成把几个出售者合并结算的依据，整车复磅只核对总运输量。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="任务单号" prop="taskNo">
        <el-input v-model="queryParams.taskNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="车牌号" prop="plateNo">
        <el-input v-model="queryParams.plateNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-160px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-160px">
          <el-option v-for="(label, value) in STATUS_NAME" :key="value" :label="label" :value="Number(value)" />
        </el-select>
      </el-form-item>
      <el-form-item label="提货点" prop="pickupAddress">
        <el-input v-model="queryParams.pickupAddress" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['logistics:transport-task:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 派车
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="任务单号" prop="taskNo" min-width="190" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] || 'info'">{{ row.statusName || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="车牌号" prop="plateNo" min-width="110" />
      <el-table-column label="司机" prop="driverName" min-width="100" />
      <el-table-column label="提货点（首站）" prop="pickupAddress" min-width="180" show-overflow-tooltip />
      <el-table-column label="待提" align="center" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.pendingStopCount > 0" type="warning">还剩 {{ row.pendingStopCount }} 家</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="时间窗" min-width="200">
        <template #default="{ row }">
          {{ formatTime(row.expectedStartTime) }} ~ {{ formatTime(row.expectedEndTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情与时间线</el-button>
          <el-button
            v-if="row.status === 0"
            link
            type="primary"
            @click="openAssign(row)"
            v-hasPermi="['logistics:transport-task:assign']"
          >派车</el-button>
          <el-button
            v-if="row.status === 1"
            link
            type="primary"
            @click="handleAccept(row.id)"
            v-hasPermi="['logistics:transport-task:update']"
          >接单</el-button>
          <el-button
            v-if="row.status === 1 || row.status === 2 || row.status === 3"
            link
            type="primary"
            @click="openReassign(row)"
            v-hasPermi="['logistics:transport-task:reassign']"
          >改派</el-button>
          <el-button
            v-if="row.status === 3 || row.status === 2"
            link
            type="success"
            @click="handleComplete(row.id)"
            v-hasPermi="['logistics:transport-task:update']"
          >完成</el-button>
          <el-button
            v-if="row.status !== 4 && row.status !== 5"
            link
            type="danger"
            @click="openCancel(row)"
            v-hasPermi="['logistics:transport-task:cancel']"
          >取消</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 派车 / 新建 -->
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="760px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" v-loading="formLoading">
      <el-form-item label="提货点地址" prop="pickupAddress">
        <el-input v-model="formData.pickupAddress" placeholder="单点 / 兼容口径；填了停靠点则以停靠点为准" />
      </el-form-item>
      <el-form-item label="出发地" prop="departureAddress">
        <el-input v-model="formData.departureAddress" placeholder="通常是场站或车队所在地" />
      </el-form-item>
      <el-form-item label="时间窗" prop="expectedStartTime">
        <el-date-picker
          v-model="timeWindow"
          type="datetimerange"
          value-format="x"
          start-placeholder="开始"
          end-placeholder="结束"
          class="!w-100%"
        />
      </el-form-item>
      <el-form-item label="车辆" prop="vehicleId">
        <el-select v-model="formData.vehicleId" placeholder="留空即待分配" clearable filterable class="!w-100%">
          <el-option
            v-for="v in vehicleOptions"
            :key="v.id"
            :label="`${v.plateNo}${v.vehicleType ? ' · ' + v.vehicleType : ''}`"
            :value="v.id!"
            :disabled="v.status === 2"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="司机" prop="driverId">
        <el-select v-model="formData.driverId" placeholder="留空即待分配" clearable filterable class="!w-100%">
          <el-option
            v-for="d in driverOptions"
            :key="d.id"
            :label="`${d.name}${d.source === 2 ? '（承运商）' : ''}`"
            :value="d.id!"
            :disabled="d.status !== 0"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="采购安排" prop="purchaseOrderNo">
        <el-input v-model="formData.purchaseOrderNo" placeholder="选填：关联的采购订单号；不挂也能派车" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>

      <el-divider content-position="left">停靠点（一车可提多家）</el-divider>
      <div class="tip mb-10px">
        每个停靠点各自交接、各自复磅、各自结算。**一次集货不能成为把几个出售者合并结算的依据**。
      </div>
      <div v-for="(stop, index) in formData.stops" :key="index" class="stop-row">
        <div class="stop-row__head">
          <span>停靠点 {{ index + 1 }}</span>
          <el-button link type="danger" @click="removeStop(index)">删除</el-button>
        </div>
        <el-form-item :label="'地址'" :prop="`stops.${index}.address`" :rules="[{ required: true, message: '停靠点地址不能为空' }]">
          <el-input v-model="stop.address" placeholder="必填：某某路 1 号" />
        </el-form-item>
        <el-form-item label="出售者">
          <el-input v-model="stop.payeeName" placeholder="姓名（快照）" class="mr-5px" style="width: 46%" />
          <el-input v-model="stop.payeeMobile" placeholder="手机号" style="width: 46%" />
        </el-form-item>
        <el-form-item label="品类 / 约量">
          <el-input v-model="stop.cargoName" placeholder="品类（计划提示）" class="mr-5px" style="width: 46%" />
          <el-input v-model="stop.estimatedQuantity" placeholder="约量" style="width: 22%" class="mr-5px" />
          <el-input v-model="stop.quantityUnit" placeholder="单位" style="width: 22%" />
        </el-form-item>
      </div>
      <el-button type="primary" plain @click="addStopRow"><Icon icon="ep:plus" class="mr-5px" /> 添加停靠点</el-button>
      <div class="tip">车辆与司机要么都选、要么都不选；只给一个等于半套派车，会被拦下。</div>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 派车（待分配的任务） -->
  <Dialog title="派车" v-model="assignVisible" width="480px">
    <el-form label-width="90px">
      <el-form-item label="车辆">
        <el-select v-model="assignForm.vehicleId" placeholder="请选择" filterable class="!w-100%">
          <el-option
            v-for="v in vehicleOptions"
            :key="v.id"
            :label="`${v.plateNo}${v.vehicleType ? ' · ' + v.vehicleType : ''}`"
            :value="v.id!"
            :disabled="v.status === 2"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="司机">
        <el-select v-model="assignForm.driverId" placeholder="请选择" filterable class="!w-100%">
          <el-option
            v-for="d in driverOptions"
            :key="d.id"
            :label="`${d.name}${d.source === 2 ? '（承运商）' : ''}`"
            :value="d.id!"
            :disabled="d.status !== 0"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="授权放行">
        <el-checkbox v-model="assignForm.override">
          证件已过期，带原因授权放行（只挂管理员）
        </el-checkbox>
      </el-form-item>
      <el-form-item v-if="assignForm.override" label="放行原因">
        <el-input v-model="assignForm.overrideReason" type="textarea" :rows="2" placeholder="必填：为什么要带着过期证件出车" />
        <div class="tip">留痕：原因、授权人、时间都会记到这趟任务上。车辆维修中、司机离职这类**硬门禁不可绕过**。</div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitAssign" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="assignVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 改派 -->
  <Dialog title="改派运输任务" v-model="reassignVisible" width="520px">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="换车换人保留前后承接关系（原车原人 → 新车新人 + 原因）。改派不改任务状态。"
    />
    <el-form label-width="90px">
      <el-form-item label="车辆">
        <el-select v-model="reassignForm.vehicleId" placeholder="请选择新车" filterable class="!w-100%">
          <el-option
            v-for="v in vehicleOptions"
            :key="v.id"
            :label="`${v.plateNo}${v.vehicleType ? ' · ' + v.vehicleType : ''}`"
            :value="v.id!"
            :disabled="v.status === 2"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="司机">
        <el-select v-model="reassignForm.driverId" placeholder="请选择新司机" filterable class="!w-100%">
          <el-option
            v-for="d in driverOptions"
            :key="d.id"
            :label="`${d.name}${d.source === 2 ? '（承运商）' : ''}`"
            :value="d.id!"
            :disabled="d.status !== 0"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="改派原因">
        <el-input v-model="reassignForm.reason" type="textarea" :rows="3" placeholder="必填：为什么要中途换车换人" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitReassign" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="reassignVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 取消 -->
  <Dialog title="取消运输任务" v-model="cancelVisible" width="480px">
    <el-form label-width="90px">
      <el-form-item label="取消原因">
        <el-input v-model="cancelReason" type="textarea" :rows="3" placeholder="必填：为什么这趟活没跑" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitCancel" type="danger" :disabled="formLoading">确认取消</el-button>
      <el-button @click="cancelVisible = false">返 回</el-button>
    </template>
  </Dialog>

  <!-- 追加停靠点 -->
  <Dialog title="追加停靠点" v-model="addStopVisible" width="520px">
    <el-form :model="addStopForm" label-width="90px">
      <el-form-item label="地址">
        <el-input v-model="addStopForm.address" placeholder="必填：某某路 1 号" />
      </el-form-item>
      <el-form-item label="出售者">
        <el-input v-model="addStopForm.payeeName" placeholder="姓名（快照）" class="mr-5px" style="width: 46%" />
        <el-input v-model="addStopForm.payeeMobile" placeholder="手机号" style="width: 46%" />
      </el-form-item>
      <el-form-item label="品类 / 约量">
        <el-input v-model="addStopForm.cargoName" placeholder="品类（计划提示）" class="mr-5px" style="width: 46%" />
        <el-input v-model="addStopForm.estimatedQuantity" placeholder="约量" style="width: 22%" class="mr-5px" />
        <el-input v-model="addStopForm.quantityUnit" placeholder="单位" style="width: 22%" />
      </el-form-item>
      <div class="tip">停靠顺序接在最后。追加的停靠点同样各自交接、各自结算。</div>
    </el-form>
    <template #footer>
      <el-button @click="submitAddStop" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="addStopVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 上报节点 -->
  <Dialog :title="`上报运输节点${nodeForm.stopLabel ? ' · ' + nodeForm.stopLabel : ''}`" v-model="nodeVisible" width="520px">
    <el-form :model="nodeForm" label-width="90px">
      <el-form-item label="节点类型">
        <el-select v-model="nodeForm.nodeType" class="!w-100%">
          <el-option v-for="value in nodeTypeOptions" :key="value" :label="NODE_TYPE_NAME[value]" :value="value" />
        </el-select>
        <div v-if="nodePhotoRequired" class="tip">「交接完成」与「卸货完成」是货物流的关键凭证，必须上传照片。</div>
      </el-form-item>
      <el-form-item label="发生时间">
        <el-date-picker v-model="nodeForm.nodeTime" type="datetime" value-format="x" placeholder="事情实际发生的时刻" class="!w-100%" />
      </el-form-item>
      <el-form-item label="位置">
        <el-input v-model="nodeForm.location" placeholder="选填" />
      </el-form-item>
      <el-form-item label="凭证照片">
        <UploadFile v-model="nodeForm.photos" :limit="6" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="nodeForm.remark" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitNode" type="primary" :disabled="formLoading">上 报</el-button>
      <el-button @click="nodeVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 上报异常 -->
  <Dialog :title="`上报运输异常${abnormalForm.stopLabel ? ' · ' + abnormalForm.stopLabel : ''}`" v-model="abnormalVisible" width="520px">
    <el-alert type="info" :closable="false" class="mb-10px" title="异常是独立标记，不改变任务状态；调度会根据它决定是否改派。" />
    <el-form :model="abnormalForm" label-width="90px">
      <el-form-item label="异常类型">
        <el-select v-model="abnormalForm.abnormalType" class="!w-100%">
          <el-option v-for="(label, value) in ABNORMAL_TYPE_NAME" :key="value" :label="label" :value="Number(value)" />
        </el-select>
      </el-form-item>
      <el-form-item label="发生时间">
        <el-date-picker v-model="abnormalForm.nodeTime" type="datetime" value-format="x" placeholder="事情实际发生的时刻" class="!w-100%" />
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="abnormalForm.abnormalReason" type="textarea" :rows="2" placeholder="必填：发生了什么" />
      </el-form-item>
      <el-form-item label="现场照片">
        <UploadFile v-model="abnormalForm.photos" :limit="6" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitAbnormal" type="warning" :disabled="formLoading">上 报</el-button>
      <el-button @click="abnormalVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 解决异常 -->
  <Dialog title="标记异常已解决" v-model="resolveVisible" width="480px">
    <el-form label-width="90px">
      <el-form-item label="解决说明">
        <el-input v-model="resolveForm.resolveRemark" type="textarea" :rows="3" placeholder="怎么解决的（选填）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitResolve" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="resolveVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 详情与时间线 -->
  <el-drawer v-model="detailVisible" title="运输任务详情" size="720px">
    <div v-if="detail" v-loading="detailLoading" class="detail">
      <el-alert v-if="detail.scopeNote" type="info" :closable="false" class="mb-10px" :title="detail.scopeNote" />

      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务单号">{{ detail.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="STATUS_TAG[detail.status!] || 'info'">{{ detail.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="车牌号">{{ detail.plateNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="司机">
          {{ detail.driverName || '—' }}{{ detail.driverMobile ? ' · ' + detail.driverMobile : '' }}
        </el-descriptions-item>
        <el-descriptions-item label="出发地">{{ detail.departureAddress || '—' }}</el-descriptions-item>
        <el-descriptions-item label="提货点（首站）">{{ detail.pickupAddress || '—' }}</el-descriptions-item>
        <el-descriptions-item label="时间窗" :span="2">
          {{ formatTime(detail.expectedStartTime) }} ~ {{ formatTime(detail.expectedEndTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="采购安排" :span="2">{{ detail.purchaseOrderNo || '未关联（直接上门收购）' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.overrideReason" label="授权放行" :span="2">
          <el-tag type="warning" class="mr-5px">证件过期放行</el-tag>
          {{ detail.overrideReason }}（{{ formatTime(detail.overrideTime) }}）
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.cancelReason" label="取消原因" :span="2">{{ detail.cancelReason }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="section">停靠点（还剩 {{ detail.pendingStopCount || 0 }} 家没提）</h4>
      <el-empty v-if="!detail.stops || !detail.stops.length" description="没有停靠点（单点 / 历史口径）" :image-size="60" />
      <el-card v-for="stop in detail.stops" :key="stop.id" shadow="never" class="stop-card">
        <div class="stop-card__head">
          <div>
            <el-tag size="small" class="mr-5px">第 {{ stop.stopNo }} 站</el-tag>
            <span class="stop-card__title">{{ stop.payeeName || '未署出售者' }}</span>
          </div>
          <el-tag :type="STOP_STATUS_TAG[stop.status!] || 'info'">{{ stop.statusName }}</el-tag>
        </div>
        <div class="node__line">{{ stop.address }}</div>
        <div class="node__line" v-if="stop.cargoName">
          货物：{{ stop.cargoName }}{{ stop.estimatedQuantity ? ` · 约 ${stop.estimatedQuantity}${stop.quantityUnit || ''}` : '' }}
        </div>
        <div class="stop-card__progress">
          <el-tag
            v-for="type in STOP_SCOPED_TYPES"
            :key="type"
            :type="stopNodeReported(stop, type) ? 'success' : 'info'"
            size="small"
            class="mr-5px"
          >{{ NODE_TYPE_NAME[type] }}{{ stopNodeReported(stop, type) ? ' ✓' : '' }}</el-tag>
        </div>
        <div v-if="stop.missingNodeNames && stop.missingNodeNames.length" class="node__line node__line--warn">
          断点：{{ stop.missingNodeNames.join('、') }}
        </div>
        <div v-if="stop.nodes && stop.nodes.length" class="stop-card__nodes">
          <div v-for="node in stop.nodes" :key="node.id" class="node__line">
            {{ formatTime(node.nodeTime) }} · {{ node.nodeTypeName || '异常' }}
            <span v-if="node.abnormalType" class="node__line--warn">（{{ node.abnormalTypeName }}：{{ node.abnormalReason }}）</span>
            · {{ node.operatorName || '—' }}
          </div>
        </div>
        <div v-if="stop.cancelReason" class="node__line node__line--warn">已取消：{{ stop.cancelReason }}</div>
        <div class="mt-10px" v-if="stop.status !== 2 && stop.status !== 3">
          <el-button link type="primary" @click="openNodeDialog(stop.id!, stop.payeeName || null)" v-hasPermi="['logistics:transport-node:report']">
            上报节点
          </el-button>
          <el-button link type="warning" @click="openAbnormalDialog(stop.id!, stop.payeeName || null)" v-hasPermi="['logistics:transport-node:report']">
            上报异常
          </el-button>
          <el-button link type="danger" @click="openStopCancel(stop)" v-hasPermi="['logistics:transport-task:update']">
            取消这个停靠点
          </el-button>
        </div>
      </el-card>
      <el-button
        v-if="detail.status !== 4 && detail.status !== 5"
        type="primary"
        plain
        class="mt-10px"
        @click="openAddStop"
        v-hasPermi="['logistics:transport-task:update']"
      >追加停靠点</el-button>

      <h4 class="section">整趟收尾节点（到达场站 / 卸货完成）</h4>
      <el-button
        type="primary"
        plain
        @click="openNodeDialog(null, null)"
        v-hasPermi="['logistics:transport-node:report']"
      >上报整趟收尾节点</el-button>

      <h4 class="section">运输时间线（全部节点）</h4>
      <el-timeline v-if="detail.nodes && detail.nodes.length">
        <el-timeline-item
          v-for="node in detail.nodes"
          :key="node.id"
          :type="node.abnormalType ? 'warning' : 'primary'"
          :timestamp="`发生 ${formatTime(node.nodeTime)} ／ 上报 ${formatTime(node.reportTime)}`"
          placement="top"
        >
          <el-card shadow="never">
            <div class="node__title">
              {{ node.nodeTypeName || '异常' }}
              <el-tag v-if="node.stopId" size="small" type="info" class="ml-5px">停靠点</el-tag>
              <el-tag v-if="node.abnormalType" type="warning" size="small" class="ml-5px">异常</el-tag>
            </div>
            <div v-if="node.abnormalType" class="node__line">
              异常类型：{{ node.abnormalTypeName }}｜说明：{{ node.abnormalReason || '—' }}
            </div>
            <div class="node__line">位置：{{ node.location || '未记录' }}</div>
            <div class="node__line">上报人：{{ node.operatorName || '—' }}</div>
            <div v-if="node.photos && node.photos.length" class="node__line">凭证：{{ node.photos.length }} 张照片</div>
            <div v-else class="node__line node__line--warn">凭证：无照片</div>
            <div v-if="node.abnormalType" class="node__line">
              <template v-if="node.abnormalResolved">
                已解决：{{ node.abnormalResolvedName || '—' }}（{{ formatTime(node.abnormalResolvedAt) }}）{{ node.abnormalResolvedRemark ? '：' + node.abnormalResolvedRemark : '' }}
              </template>
              <template v-else>
                <el-tag type="danger" size="small">待解决</el-tag>
                <el-button
                  link
                  type="primary"
                  class="ml-5px"
                  @click="openResolve(node)"
                  v-hasPermi="['logistics:transport-node:abnormal:resolve']"
                >标记已解决</el-button>
              </template>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="还没有任何节点上报" />

      <el-alert
        v-if="(detail.missingNodeNames && detail.missingNodeNames.length) || (detail.missingEvidenceNames && detail.missingEvidenceNames.length)"
        type="warning"
        :closable="false"
        class="mt-10px"
        :title="brokenPointText"
      />

      <h4 class="section">改派承接记录</h4>
      <el-empty v-if="!detail.reassigns || !detail.reassigns.length" description="没有改派过" :image-size="60" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="record in detail.reassigns"
          :key="record.id"
          :timestamp="formatTime(record.reassignTime)"
          placement="top"
        >
          <div class="node__line">
            {{ record.prevPlateNo || '—' }}／{{ record.prevDriverName || '—' }}
            → {{ record.plateNo || '—' }}／{{ record.driverName || '—' }}
          </div>
          <div class="node__line">原因：{{ record.reason }}（{{ record.operatorName || '—' }}）</div>
        </el-timeline-item>
      </el-timeline>

      <h4 class="section">运输轨迹（模拟演示）</h4>
      <TransportTrackDemo v-if="detail.id" :task-id="detail.id" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import {
  LogisticsTransportTaskApi,
  LogisticsTransportNodeApi,
  LogisticsTransportStopApi,
  LogisticsTransportNodeVO,
  LogisticsTransportStopVO,
  LogisticsTransportStopSaveVO,
  LogisticsTransportTaskCreateVO,
  LogisticsTransportTaskVO
} from '@/api/logistics/task'
import { LogisticsVehicleApi, LogisticsVehicleVO } from '@/api/logistics/vehicle'
import { LogisticsDriverApi, LogisticsDriverVO } from '@/api/logistics/driver'
import { formatDate } from '@/utils/formatTime'
import TransportTrackDemo from './components/TransportTrackDemo.vue'
import UploadFile from '@/components/UploadFile/src/UploadFile.vue'

defineOptions({ name: 'LogisticsTask' })

const STATUS_NAME: Record<number, string> = {
  0: '待分配',
  1: '已分配',
  2: '已接单',
  3: '执行中',
  4: '已完成',
  5: '已取消'
}
const STATUS_TAG: Record<number, 'success' | 'warning' | 'info' | 'danger'> = {
  0: 'info',
  1: 'warning',
  2: 'warning',
  3: 'warning',
  4: 'success',
  5: 'danger'
}
const STOP_STATUS_TAG: Record<number, 'success' | 'warning' | 'info' | 'danger'> = {
  0: 'info',
  1: 'warning',
  2: 'success',
  3: 'danger'
}
// 五类节点（与后端 LogisticsTransportNodeTypeEnum 对齐）
const NODE_TYPE_NAME: Record<number, string> = {
  1: '到达提货点',
  2: '交接完成',
  3: '起运',
  4: '到达场站',
  5: '卸货完成'
}
// 按停靠点上报的三类（V5 #72）
const STOP_SCOPED_TYPES = [1, 2, 3]
// 整趟收尾的两类
const TASK_SCOPED_TYPES = [4, 5]
// 照片必填的两类：交接完成与卸货完成（货物流关键凭证）
const NODE_PHOTO_REQUIRED_TYPES = [2, 5]
// 八类异常（与后端 LogisticsTransportAbnormalTypeEnum 对齐）
const ABNORMAL_TYPE_NAME: Record<number, string> = {
  1: '车辆故障',
  2: '交通事故',
  3: '天气延误',
  4: '道路封闭',
  5: '货物损坏',
  6: '对方不在',
  7: '地址错误',
  8: '其他'
}

const { t } = useI18n()
const message = useMessage()

const loading = ref(true)
const list = ref<LogisticsTransportTaskVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  taskNo: undefined,
  plateNo: undefined,
  status: undefined,
  pickupAddress: undefined
})
const queryFormRef = ref()

const vehicleOptions = ref<LogisticsVehicleVO[]>([])
const driverOptions = ref<LogisticsDriverVO[]>([])

const getList = async () => {
  loading.value = true
  try {
    const data = await LogisticsTransportTaskApi.getTaskPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const loadOptions = async () => {
  const [vehicles, drivers] = await Promise.all([
    LogisticsVehicleApi.getVehiclePage({ pageNo: 1, pageSize: 100 }),
    LogisticsDriverApi.getDriverPage({ pageNo: 1, pageSize: 100 })
  ])
  vehicleOptions.value = vehicles.list
  driverOptions.value = drivers.list
}
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

// ==================== 新建 / 派车 ====================
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<LogisticsTransportTaskCreateVO>(buildEmpty())
const timeWindow = ref<[number, number] | undefined>()

function buildEmptyStop(): LogisticsTransportStopSaveVO {
  return { stopType: 1, address: undefined, payeeName: undefined, payeeMobile: undefined, cargoName: undefined }
}

function buildEmpty(): LogisticsTransportTaskCreateVO {
  return {
    pickupAddress: undefined,
    departureAddress: undefined,
    vehicleId: undefined,
    driverId: undefined,
    purchaseOrderNo: undefined,
    remark: undefined,
    stops: [buildEmptyStop()]
  }
}

const formRules = reactive({
  pickupAddress: [
    {
      validator: (_rule: any, _value: any, callback: any) => {
        // 停靠点与提货点地址至少有一个
        const hasStops = (formData.value.stops || []).some((s) => s.address)
        callback(!formData.value.pickupAddress && !hasStops ? new Error('至少要有一个停靠点或提货点地址') : undefined)
      },
      trigger: 'blur'
    }
  ]
})

const addStopRow = () => {
  formData.value.stops = [...(formData.value.stops || []), buildEmptyStop()]
}
const removeStop = (index: number) => {
  formData.value.stops = (formData.value.stops || []).filter((_s, i) => i !== index)
}

const openForm = async (type: string, id?: number) => {
  dialogVisible.value = true
  formType.value = type
  dialogTitle.value = t('action.' + type)
  formData.value = buildEmpty()
  timeWindow.value = undefined
  formRef.value?.resetFields()
  await loadOptions()
  if (id) {
    formLoading.value = true
    try {
      const detail = await LogisticsTransportTaskApi.getTask(id)
      formData.value = detail
      if (detail.expectedStartTime && detail.expectedEndTime) {
        timeWindow.value = [detail.expectedStartTime as unknown as number, detail.expectedEndTime as unknown as number]
      }
    } finally {
      formLoading.value = false
    }
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  if (timeWindow.value?.length === 2) {
    formData.value.expectedStartTime = timeWindow.value[0] as unknown as Date
    formData.value.expectedEndTime = timeWindow.value[1] as unknown as Date
  } else {
    formData.value.expectedStartTime = undefined
    formData.value.expectedEndTime = undefined
  }
  // 过滤掉完全空白的停靠点行
  formData.value.stops = (formData.value.stops || []).filter((s) => s.address)
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await LogisticsTransportTaskApi.createTask(formData.value)
      message.success('已派车')
    } else {
      await LogisticsTransportTaskApi.updateTask(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const assignVisible = ref(false)
const assignForm = reactive<{
  id?: number
  vehicleId?: number
  driverId?: number
  override?: boolean
  overrideReason?: string
}>({})
const openAssign = async (row: LogisticsTransportTaskVO) => {
  assignVisible.value = true
  assignForm.id = row.id
  assignForm.vehicleId = undefined
  assignForm.driverId = undefined
  assignForm.override = false
  assignForm.overrideReason = undefined
  await loadOptions()
}
const submitAssign = async () => {
  if (!assignForm.vehicleId || !assignForm.driverId) {
    message.warning('车辆与司机都要选')
    return
  }
  if (assignForm.override && !assignForm.overrideReason) {
    message.warning('授权放行必须填原因')
    return
  }
  formLoading.value = true
  try {
    if (assignForm.override) {
      await LogisticsTransportTaskApi.assignTaskWithOverride({
        id: assignForm.id!,
        vehicleId: assignForm.vehicleId,
        driverId: assignForm.driverId,
        overrideReason: assignForm.overrideReason!
      })
      message.success('已带原因授权放行')
    } else {
      await LogisticsTransportTaskApi.assignTask({
        id: assignForm.id!,
        vehicleId: assignForm.vehicleId,
        driverId: assignForm.driverId
      })
      message.success('已派车')
    }
    assignVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handleAccept = async (id: number) => {
  await LogisticsTransportTaskApi.acceptTask(id)
  message.success('已接单')
  await getList()
}
const handleComplete = async (id: number) => {
  await LogisticsTransportTaskApi.completeTask(id)
  message.success('已完成，车辆已放回车队')
  await getList()
}

// ==================== 改派 ====================
const reassignVisible = ref(false)
const reassignForm = reactive<{ id?: number; vehicleId?: number; driverId?: number; reason?: string }>({})
const openReassign = async (row: LogisticsTransportTaskVO) => {
  reassignVisible.value = true
  reassignForm.id = row.id
  reassignForm.vehicleId = undefined
  reassignForm.driverId = undefined
  reassignForm.reason = undefined
  await loadOptions()
}
const submitReassign = async () => {
  if (!reassignForm.vehicleId || !reassignForm.driverId) {
    message.warning('新车与新司机都要选')
    return
  }
  if (!reassignForm.reason) {
    message.warning('改派原因必填')
    return
  }
  formLoading.value = true
  try {
    await LogisticsTransportTaskApi.reassignTask({
      id: reassignForm.id!,
      vehicleId: reassignForm.vehicleId,
      driverId: reassignForm.driverId,
      reason: reassignForm.reason
    })
    message.success('已改派，承接记录已留痕')
    reassignVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

// ==================== 取消 ====================
const cancelVisible = ref(false)
const cancelReason = ref('')
const cancelId = ref<number>()
const openCancel = (row: LogisticsTransportTaskVO) => {
  cancelVisible.value = true
  cancelId.value = row.id
  cancelReason.value = ''
}
const submitCancel = async () => {
  if (!cancelReason.value) {
    message.warning('取消原因必填')
    return
  }
  formLoading.value = true
  try {
    await LogisticsTransportTaskApi.cancelTask({ id: cancelId.value!, cancelReason: cancelReason.value })
    message.success('已取消，车辆已放回车队')
    cancelVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

// ==================== 详情与时间线 ====================
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<LogisticsTransportTaskVO>()

const brokenPointText = computed(() => {
  const parts: string[] = []
  if (detail.value?.missingNodeNames?.length) {
    parts.push(`尚未上报：${detail.value.missingNodeNames.join('、')}`)
  }
  if (detail.value?.missingEvidenceNames?.length) {
    parts.push(`缺凭证：${detail.value.missingEvidenceNames.join('、')}`)
  }
  return parts.length ? `断点 · ${parts.join('；')}` : ''
})

const openDetail = async (id: number) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await LogisticsTransportTaskApi.getTask(id)
  } finally {
    detailLoading.value = false
  }
}

const stopNodeReported = (stop: LogisticsTransportStopVO, nodeType: number) =>
  (stop.nodes || []).some((node) => node.nodeType === nodeType)

/** UploadFile 的 v-model 是「逗号分隔的 URL 字符串」，转成接口要的数组 */
function toPhotoList(photos: string): string[] {
  return (photos || '')
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
}

// ==================== 上报节点 ====================
const nodeVisible = ref(false)
const nodeForm = reactive<{
  stopId?: number
  stopLabel?: string
  nodeType?: number
  nodeTime?: number
  location?: string
  photos: string
  remark?: string
}>({ photos: '' })

const nodeTypeOptions = computed<number[]>(() => {
  if (nodeForm.stopId) {
    return STOP_SCOPED_TYPES
  }
  // 有停靠点的任务，整趟收尾只能报「到达场站 / 卸货完成」；单点历史口径才放开五类
  return detail.value?.stops?.length ? TASK_SCOPED_TYPES : [1, 2, 3, 4, 5]
})
const nodePhotoRequired = computed(() => NODE_PHOTO_REQUIRED_TYPES.includes(nodeForm.nodeType ?? 0))

const openNodeDialog = (stopId: number | null, stopLabel: string | null) => {
  nodeVisible.value = true
  nodeForm.stopId = stopId ?? undefined
  nodeForm.stopLabel = stopLabel ?? undefined
  nodeForm.nodeType = stopId ? 1 : detail.value?.stops?.length ? 4 : 3
  nodeForm.nodeTime = Date.now()
  nodeForm.location = undefined
  nodeForm.photos = ''
  nodeForm.remark = undefined
}
const submitNode = async () => {
  if (!nodeForm.nodeType) {
    message.warning('请选择节点类型')
    return
  }
  if (!nodeForm.nodeTime) {
    message.warning('发生时间不能为空')
    return
  }
  const photos = toPhotoList(nodeForm.photos)
  if (nodePhotoRequired.value && photos.length === 0) {
    message.warning('「交接完成」与「卸货完成」必须上传照片')
    return
  }
  formLoading.value = true
  try {
    await LogisticsTransportNodeApi.reportNode({
      taskId: detail.value!.id!,
      stopId: nodeForm.stopId,
      nodeType: nodeForm.nodeType,
      nodeTime: nodeForm.nodeTime as unknown as Date,
      location: nodeForm.location,
      photos,
      remark: nodeForm.remark,
      clientRequestId: newClientRequestId()
    })
    message.success('已上报节点')
    nodeVisible.value = false
    await openDetail(detail.value!.id!)
    await getList()
  } finally {
    formLoading.value = false
  }
}

// ==================== 上报异常 ====================
const abnormalVisible = ref(false)
const abnormalForm = reactive<{
  stopId?: number
  stopLabel?: string
  abnormalType?: number
  nodeTime?: number
  abnormalReason?: string
  photos: string
}>({ photos: '' })
const openAbnormalDialog = (stopId: number | null, stopLabel: string | null) => {
  abnormalVisible.value = true
  abnormalForm.stopId = stopId ?? undefined
  abnormalForm.stopLabel = stopLabel ?? undefined
  abnormalForm.abnormalType = 1
  abnormalForm.nodeTime = Date.now()
  abnormalForm.abnormalReason = undefined
  abnormalForm.photos = ''
}
const submitAbnormal = async () => {
  if (!abnormalForm.abnormalType) {
    message.warning('请选择异常类型')
    return
  }
  if (!abnormalForm.abnormalReason) {
    message.warning('异常说明必填')
    return
  }
  if (!abnormalForm.nodeTime) {
    message.warning('发生时间不能为空')
    return
  }
  formLoading.value = true
  try {
    await LogisticsTransportNodeApi.reportAbnormal({
      taskId: detail.value!.id!,
      stopId: abnormalForm.stopId,
      abnormalType: abnormalForm.abnormalType,
      abnormalReason: abnormalForm.abnormalReason,
      nodeTime: abnormalForm.nodeTime as unknown as Date,
      photos: toPhotoList(abnormalForm.photos),
      clientRequestId: newClientRequestId()
    })
    message.success('已上报异常（不影响任务状态）')
    abnormalVisible.value = false
    await openDetail(detail.value!.id!)
  } finally {
    formLoading.value = false
  }
}

// ==================== 异议 / 异常解决 ====================
const resolveVisible = ref(false)
const resolveForm = reactive<{ id?: number; resolveRemark?: string }>({})
const openResolve = (node: LogisticsTransportNodeVO) => {
  resolveVisible.value = true
  resolveForm.id = node.id
  resolveForm.resolveRemark = undefined
}
const submitResolve = async () => {
  formLoading.value = true
  try {
    await LogisticsTransportNodeApi.resolveAbnormal({
      id: resolveForm.id!,
      resolveRemark: resolveForm.resolveRemark
    })
    message.success('已标记解决')
    resolveVisible.value = false
    await openDetail(detail.value!.id!)
  } finally {
    formLoading.value = false
  }
}

// ==================== 停靠点追加 / 取消 ====================
const addStopVisible = ref(false)
const addStopForm = reactive<LogisticsTransportStopSaveVO>({})
const openAddStop = () => {
  addStopVisible.value = true
  Object.assign(addStopForm, buildEmptyStop())
}
const submitAddStop = async () => {
  if (!addStopForm.address) {
    message.warning('停靠点地址必填')
    return
  }
  formLoading.value = true
  try {
    await LogisticsTransportStopApi.createStop({ ...addStopForm, taskId: detail.value!.id! })
    message.success('已追加停靠点')
    addStopVisible.value = false
    await openDetail(detail.value!.id!)
    await getList()
  } finally {
    formLoading.value = false
  }
}

const openStopCancel = (stop: LogisticsTransportStopVO) => {
  ElMessageBox.prompt('取消原因（必填）', `取消停靠点：${stop.payeeName || stop.address}`, {
    inputPlaceholder: '为什么这个点不去了',
    inputValidator: (value: string) => (value ? true : '取消原因必填')
  })
    .then(async ({ value }) => {
      await LogisticsTransportStopApi.cancelStop({ id: stop.id!, cancelReason: value })
      message.success('已取消这个停靠点，其它停靠点不受影响')
      await openDetail(detail.value!.id!)
      await getList()
    })
    .catch(() => {})
}

/**
 * 时间显示：接口给的是毫秒时间戳（本项目 LocalDateTime 的协议格式，前端一律 value-format="x"），
 * 空值给「—」，避免时间线上出现 Invalid Date。
 */
function formatTime(value?: Date | string | null): string {
  return value ? formatDate(new Date(value)) : '—'
}

/**
 * 幂等键：与现场端的做法一致（不引入 uuid 依赖），前缀区分来源端。
 * 服务端按「租户 + 请求号」唯一，重复提交返回既有节点。
 */
function newClientRequestId() {
  return `admin-${Date.now()}-${Math.floor(Math.random() * 1e6)}`
}

/** 初始化 **/
getList()
</script>

<style scoped lang="scss">
.tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}
.section {
  margin: 20px 0 10px;
  font-size: 14px;
  font-weight: 600;
}
.detail {
  padding: 0 4px;
}
.node__title {
  font-weight: 600;
}
.node__line {
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.8;
}
.node__line--warn {
  color: var(--el-color-warning);
}
.stop-card {
  margin-bottom: 10px;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  &__title {
    font-weight: 600;
  }

  &__progress {
    margin: 8px 0;
  }

  &__nodes {
    margin-top: 6px;
    padding-top: 6px;
    border-top: 1px dashed var(--el-border-color-lighter);
  }
}
.stop-row {
  margin-bottom: 10px;
  padding: 10px;
  background-color: var(--el-fill-color-lighter);
  border-radius: 6px;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
    font-weight: 600;
  }
}
</style>
