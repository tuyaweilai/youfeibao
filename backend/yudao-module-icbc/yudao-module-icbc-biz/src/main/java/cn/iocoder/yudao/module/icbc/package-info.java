/**
 * 工商银行反向开票模块，主要实现企业代个人开票的完整业务流程
 * 
 * 1. Controller 提供 RESTful API 接口
 * 2. Service 实现业务逻辑
 * 3. DAO 提供数据库操作
 * 4. 工行SDK 封装工行接口调用
 * 5. 回调处理 处理工行异步通知
 */
package cn.iocoder.yudao.module.icbc; 