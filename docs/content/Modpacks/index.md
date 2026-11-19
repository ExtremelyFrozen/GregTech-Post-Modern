---
title: 整合包制作
---


# 整合包制作

GregTech Post Modern 1.21.1 使用 `gtpm` 命名空间。通过数据包覆盖配方或世界生成时，使用 `data/gtpm/` 下的对应路径。通过资源包覆盖内置纹理或界面时，使用 `assets/gtpm/`。注册新机器、材料或配方类型时使用 Java 附属模组的注册 API。源码入口见[文档之外](./Beyond-The-Docs.md)。

## 上游 KubeJS 示例 { #upstream-kubejs }

GTPM 1.21.1 已移除内置 KubeJS 集成。本节保留上游 GregTech CEu Modern 的脚本供迁移时参考，其中的 `GTCEuStartupEvents`、`GTCEuServerEvents`、`event.recipes.gtceu` 和 `gtceu:*` 使用上游的 API 与注册 ID。


## 通用说明

有时，在添加或修改内容时必须调用某个特定方法。
这些方法会在文档中用 `// [*]` 标记，例如：

```js
ServerEvents.exampleEvent(event => {
    event.create('example', builder => {
        builder.requiredMethod(42) // [*] (1)
        builder.otherRequiredMethod(42) // [*]

        builder.optionalMethod() // (2)
    })
})
```

1. 这些方法是必需的
2. 这个方法是可选的，并不需要在所有情况下调用


## 文档之外

虽然我们会尽量保持文档最新且完整，但它不一定总是包含所有最新信息。

也请查看 [文档之外](./Beyond-The-Docs.md) 页面获取更多参考。
