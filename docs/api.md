# API 文档

本项目包含三个主要接口层：

- 核心 Core（Java）：对外公开的接口位于 `com.swiftest.core.interfaces` 包。
- Android SDK（Java）：对第三方 App 暴露的 API 位于 `com.swiftest.sdk` 包。
- Web 前端（TypeScript）：通过 WebSocket 与服务端交互。

## Core 层接口

- `SpeedTestCallback`
  - `onTestStarted()`
  - `onTestCompleted(downloadMbps, uploadMbps, pingMs, totalTrafficMB)`
  - `onTestFailed(errorCode, errorMessage)`
  - `onTestCancelled()`
  - `onConnectionStateChanged(isConnected)`

- `ProgressListener`
  - `onProgressUpdate(progress0to100, stageText)`
  - `onSpeedUpdate(currentMbps, averageMbps)`
  - `onStageChanged(stage, description)`
  - `onQualityUpdate(quality, score)`

- `SpeedTestConfig`（Builder）
  - `serverHost(String)` / `webSocketPort(int)` / `udpPort(int)`
  - `guid(String)` / `testId(String)`
  - `sendTimeout(int sec)` / `receiveTimeout(int sec)`
  - `appMode(boolean)`

- `SpeedTestProtocol`
  - `startSpeedTest()` / `stopSpeedTest()`

## Android SDK

- 入口类：`com.swiftest.sdk.SwiftestSDK`
  - Builder：`serverHost(String)`, `wsPort(int)`, `guid(String)`, `testId(String)`
  - `start(SpeedTestCallback, ProgressListener)`
  - `stop()`

示例：
```java path=null start=null
SwiftestSDK sdk = new SwiftestSDK.Builder()
    .serverHost("swiftest.thucloud.com")
    .wsPort(8080)
    .build();
sdk.start(callback, progressListener);
```

## Web 前端

- 入口：`web-frontend/src/components/SpeedTest.vue`
- 功能：演示 WebSocket 连通性；实际 UDP 测速由移动端/桌面端承载。
