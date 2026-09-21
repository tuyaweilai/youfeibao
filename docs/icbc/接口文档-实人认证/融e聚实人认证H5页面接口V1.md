融e聚实人认证H5页面接口V1

1 功能说明

本接口为页面接口。合作方后台根据【请求参数】章节说明组装上送报文，通过工行开放平台 SDK 生成 自动提交表单将用户跳转到电子钱包账户开立页面，进行后续电子钱包账户开立操作流程。

请求方式： POST

2 请求路径

3 通用请求参数

4 请求参数

8 请求示例

9 响应示例

10 异常示例

11 返回码解释

| 环境 | 地址 |
| --- | --- |
| 正式环境 | https://gw.open.icbc.com.cn/ui/jft/ui/user/faceH5/submit/V1 |

| 参数名 | 类  型 | 是否 必输 | 最大 长度 | 描述 | 示例值 |
| --- | --- | --- | --- | --- | --- |
| app_id | str | true | 20 | APP的编号,应用在API开放平 台注册时生成 | 10000000000000261019 |
| msg_id | str | true | 40 | 消息通讯唯一编号 ，每次调用 独立生成 ，APP级唯一 | urcnl24ciutr9 |
| format | str | false | 5 | 请求参数格式 ，仅支持json | json |
| charset | str | false | 10 | 字符集,缺省为UTF-8 | UTF-8 |
| encrypt_type | str | false | 5 | 现在仅支持AES ，部分接口支  持加密 ，如接口无需加密 ，参 数中此字段无需上送 | AES |
| sign_type | str | false | 5 | 签名类型 ，CA-工行颁发的证   书认证 ，RSA-RSAWithSha1， RSA2-RSAWithSha256 ，缺省 为RSA | RSA2 |
| sign | str | true | 300 | 报文签名 ，api平台自动生成 | ERITJKEIJKJHKKKHJEREEEEEE |
| timestamp | str | true | 19 | 交易发生时间戳 ，yyyy-MM- ddHH:mm:ss格式 | 2016-10-29 20:44:38 |
| ca | str | false | 2048 | 采用ca认证方式时 ，需上送证 书 | FSGFRHRGHTHTFDFER |
| biz_content | str | true | - | 请求参数的集合 | - |

| 参数名 | 类  型 | 是否 必输 | 最大 长度 | 描述 | 示例值 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| appId | str | true | 20 | 合作 方编 号 | 10000000000000261019 | 应用在API开 放平台注册  时生成；
同通用请求 参数中的
appid |
| authScene | str | false | 2 | 认证 场景 | 01 | 01-反向开票 |
| outUserId | str | true | 20 | 外部 用户 编号 | 123456789 | - |
| transNo | str | true | 36 | 合作 方交 易单 号 | SRRZ202501200001 | - |
| callbackUrl | str | true | 500 | 合作 方接 收结 果通 知
URL | https://www.icbc.com.cn/ notify | 该通知地址 用于实人认 证结果通知 字段长度不 大于 500； 不允许携带 参数 |
| jumpUrl | str | true | 1024 | 完成 后跳 转合 作方 URL |  | 用户实人认 证通过后，
结果页“确
认”按钮的跳 转 URL；
字段长度不
大于
1024，且为 合法 URL 格 式；允许携  带参数 |

| 参数名 | 类  型 | 是否 必输 | 最大 长度 | 描述 | 示例值 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| failJumpUrl | str | true | 1024 | 失败 后跳 转合 作方 URL | https://www.icbc.com.cn/j umpback?userId=xxx | 用户实人认 证未通过，
结果页“确
认”按钮的跳 转 URL，用  于合作方重  新发起实人  认证流程；
字段长度不 大于
1024，且为 合法 URL 格 式；允许携  带参数 |
| custName | str | true | 30 | 待认 证人 姓名 | 张三 | - |
| certNo | str | true | 18 | 证件 号码 | 123456789012345678 | 目前仅支持 身份证 |
| mobile | str | true | 11 | 用户 手机 号 | 13845676579 | 需符合手机 号规则，用 于认证流程 中发送验证 短信 |
| 5 通用响应参数
页面接口无响应参数
6 响应参数
页面接口无响应参数
7 使用示例 | 5 通用响应参数
页面接口无响应参数
6 响应参数
页面接口无响应参数
7 使用示例 | 5 通用响应参数
页面接口无响应参数
6 响应参数
页面接口无响应参数
7 使用示例 | 5 通用响应参数
页面接口无响应参数
6 响应参数
页面接口无响应参数
7 使用示例 | 5 通用响应参数
页面接口无响应参数
6 响应参数
页面接口无响应参数
7 使用示例 | 5 通用响应参数
页面接口无响应参数
6 响应参数
页面接口无响应参数
7 使用示例 | 5 通用响应参数
页面接口无响应参数
6 响应参数
页面接口无响应参数
7 使用示例 |
| public class JftUiUserFaceH5SubmitTestServletV1 extends HttpServlet { @Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
doPost(req, resp); }
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
try {
// FIXME: 此处替换合作方  APPID
final String APP_ID = "[需替换]APP的编号";
// FIXME：此处替换合作方  APPID 对应私钥 | public class JftUiUserFaceH5SubmitTestServletV1 extends HttpServlet { @Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
doPost(req, resp); }
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
try {
// FIXME: 此处替换合作方  APPID
final String APP_ID = "[需替换]APP的编号";
// FIXME：此处替换合作方  APPID 对应私钥 | public class JftUiUserFaceH5SubmitTestServletV1 extends HttpServlet { @Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
doPost(req, resp); }
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
try {
// FIXME: 此处替换合作方  APPID
final String APP_ID = "[需替换]APP的编号";
// FIXME：此处替换合作方  APPID 对应私钥 | public class JftUiUserFaceH5SubmitTestServletV1 extends HttpServlet { @Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
doPost(req, resp); }
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
try {
// FIXME: 此处替换合作方  APPID
final String APP_ID = "[需替换]APP的编号";
// FIXME：此处替换合作方  APPID 对应私钥 | public class JftUiUserFaceH5SubmitTestServletV1 extends HttpServlet { @Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
doPost(req, resp); }
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
try {
// FIXME: 此处替换合作方  APPID
final String APP_ID = "[需替换]APP的编号";
// FIXME：此处替换合作方  APPID 对应私钥 | public class JftUiUserFaceH5SubmitTestServletV1 extends HttpServlet { @Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
doPost(req, resp); }
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
try {
// FIXME: 此处替换合作方  APPID
final String APP_ID = "[需替换]APP的编号";
// FIXME：此处替换合作方  APPID 对应私钥 | public class JftUiUserFaceH5SubmitTestServletV1 extends HttpServlet { @Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
doPost(req, resp); }
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
try {
// FIXME: 此处替换合作方  APPID
final String APP_ID = "[需替换]APP的编号";
// FIXME：此处替换合作方  APPID 对应私钥 |

| 页面服务无响应示例 |
| --- |

| 页面服务无响应示例 |
| --- |

| 页面服务无返回码 |
| --- |

