---
title: 开发运行环境
---


# 开发运行环境

`1.21` 分支使用 Java 21、Gradle Wrapper 9.9.1 和 NeoForge ModDevGradle。Java 与 Kotlin 使用同一 JVM 工具链。NeoForge、Kotlin 及 LDLib2 版本由 Gradle 版本目录提供。 [构建版本目录](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/gradle/libs.versions.toml) · [模组依赖版本](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/gradle/forge.versions.toml)


## 启动游戏

| Gradle 任务 | 入口 |
| --- | --- |
| `runClient`、`runServer` | 使用额外开发依赖的客户端和服务端。 |
| `runCleanClient`、`runCleanServer` | 使用常规开发依赖的客户端和服务端。 |
| `runGameTestServer` | 加载 `test` 源集，在独立的 `run/gametest/server` 目录运行 GameTest。 |
| `runGameTestClient` | UI 或交互验证用的测试客户端，游戏目录为 `run/gametest/client`。 |
| `runData` | 生成资源到 `src/generated/resources/`。 |

普通启动使用 GTPM 的常规源集。GameTest 入口加载包含测试配方、结构夹具与 NeoForge Test Framework 的测试源集。

`recipeViewer` 控制开发运行时的查看器。默认 `emi`，可设置为 `jei` 或 `all`。其他值会以没有这些查看器依赖的环境启动，可用于验证 UI 的可选集成。以下示例启动仅使用 JEI 的客户端。

??? example "使用 Gradle Wrapper 指定 JEI"

    ```powershell
    .\gradlew.bat runCleanClient -PrecipeViewer=jei
    ```
