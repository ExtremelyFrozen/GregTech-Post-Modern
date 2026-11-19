---
title: 整合包制作
---


# 整合包制作

GregTech Post Modern 1.21.1 使用 `gtpm` 命名空间。添加或覆盖实例中的配方和世界生成定义时使用数据包，发布界面与纹理时使用资源包，注册新方块、材料和配方类型时使用附属模组。

- [添加、替换与移除配方](Recipes/Adding-and-Removing-Recipes.md)
- [配置矿脉生成](Ore-Generation/index.md)
- [注册机器](Other-Topics/Custom-Machines.md)与[定义多方块结构](Other-Topics/Multiblock-Structures.md)
- [配置 LDLib2 界面](Other-Topics/LDLib-UI-Editor.md)
- [当前源码入口](Beyond-The-Docs.md)


## 迁移旧内容 { #upstream-kubejs }

GTPM 1.21.1 已移除内置 KubeJS 集成。`GTCEuStartupEvents`、`GTCEuServerEvents` 和 `event.recipes.gtceu` 是 GregTech CEu Modern 的上游入口。迁移时按内容改用数据包、资源包或附属模组。 [1.21.1 迁移指南](Changes/1.21-Migration.md)
