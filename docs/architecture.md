# 架构设计

## 总览

- core：核心测速引擎（WebSocket/UDP/协议/模型/回调）
- android-app：Android 应用（UI + 业务服务层），依赖 core
- android-sdk：面向第三方的 SDK 封装，公开简化 API，依赖 core
- web-frontend：Vite + Vue3 + TS 前端，演示 WebSocket 连通
- server（预留）：WebSocket/UDP 服务端

```
UI(App)  ─┐                  ┌─> Web 前端
           │                  │
Business  ─┼─> SpeedTestProtocol ─┼─> WebSocketClient
           │                  │
SDK(App) ─┘                  └─> UdpTester
```

## 模块职责

- WebSocketClient：握手、超时控制、消息编解码
- UdpTester：触发、采样、速度计算、饱和判断
- SpeedTestProtocol：编排 WebSocket 与 UDP 的状态机
- SpeedTestService（App 层）：桥接 UI 与 core，聚合结果
- SwiftestSDK：对外暴露易用接口（Builder + start/stop）

## 数据流

1. WebSocket 连接 -> 获取 UDP 端口 -> 启动 UdpTester
2. 采样统计 -> 计算 P95 速度 -> 饱和/继续/重复
3. 结束时回传 download + traffic -> UI/SDK 回调

## 关键设计点

- Builder 配置 + 回调接口，解耦 UI 与核心逻辑
- 饱和判断与“继续/重复”控制闭环
- 线程安全、定时器/Socket 资源释放
- App/SDK 双协议模式（GUID+testId vs hello）
