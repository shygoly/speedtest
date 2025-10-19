# 部署指南

## Android 端

- App：`./gradlew :android-app:assembleDebug`
- SDK：`./gradlew :android-sdk:sdk:publish`（或 `assembleRelease` 产物 AAR）

## Web 前端

- 本地开发：
  ```bash
  cd web-frontend
  npm install
  npm run dev
  ```
- 生产构建：
  ```bash
  npm run build
  ```
  输出目录：`web-frontend/dist`

## 服务端（占位）

- WebSocket 服务：监听 `:8080`，与客户端交换消息（hello/GUID+testId、start/repeat/continue/finish）。
- UDP 服务：按 WebSocket 分配的端口接收/发送数据，用于测速。
- 可部署到 Fly.io（支持 UDP）或自托管；Cloudflare 需使用 Spectrum 才能转发 UDP。

## CI/CD

- GitHub Actions：
  - Android 构建：`.github/workflows/android.yml`（需要 Gradle Wrapper）
  - Web 构建：`.github/workflows/web.yml`

## 配置项

- 服务器域名/端口在 `SpeedTestConfig`/SDK Builder 中配置。
