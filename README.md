# Swiftest 网络测速软件

一个现代化的多平台网络测速解决方案，包含Android应用、SDK和Web前端。

## 🏗️ 项目结构

```
swiftest-speedtest/
├── core/                    # 核心测速引擎 (共享代码)
├── android-app/             # Android 手机应用
├── android-sdk/             # Android SDK 库
├── web-frontend/            # Vue.js Web前端
├── server/                  # 服务端组件 (待实现)
├── docs/                    # 项目文档
└── scripts/                 # 构建和部署脚本
```

## 🚀 快速开始

### 构建所有模块
```bash
./scripts/build-all.sh
```

### 分别构建各模块
```bash
# 构建核心库
./gradlew :core:build

# 构建Android应用
./gradlew :android-app:build

# 构建Android SDK
./gradlew :android-sdk:build

# 构建Web前端
cd web-frontend && npm run build
```

## 📚 文档

- [API文档](docs/api.md)
- [架构设计](docs/architecture.md)
- [部署指南](docs/deployment.md)

## 🔧 开发环境要求

- **Android**: Android Studio, JDK 11+, Gradle 7.4+
- **Web**: Node.js 16+, npm 8+
- **服务端**: Java 11+, Docker (可选)

## 📄 许可证

[MIT License](LICENSE)