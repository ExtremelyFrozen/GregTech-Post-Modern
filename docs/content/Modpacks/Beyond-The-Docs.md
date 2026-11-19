---
title: 文档之外
---


# 文档之外

## GregTech Post Modern 1.21.1

- 在 Java 附属模组中，使用 `Material.Builder` 定义材料，使用 `GTRegistrate` 注册，使用 `GTRecipeBuilder` 生成配方。[Material](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/api/data/chemical/material/Material.java) · [GTRegistrate](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/api/registry/registrate/GTRegistrate.java) · [GTRecipeBuilder](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/data/recipe/builder/GTRecipeBuilder.java)
- 附属模组通过机器注册器加入命名结构，并从 `multiblock/<命名空间>/` 加载结构资源。 [多方块结构文件](Other-Topics/Multiblock-Structures.md) · [原始高炉结构 JSON](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/resources/multiblock/gtpm/json/primitive_blast_furnace.json)
- 数据包可以使用 `gtpm` 配方序列化器、世界生成特征和标签。参考内置的世界生成 JSON 或配方数据模型。[世界生成资源](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/tree/1.21/src/generated/resources/data/gtpm/worldgen) · [GTRecipeDefinition](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/api/recipe/GTRecipeDefinition.java)

## 上游 KubeJS 源码

历史脚本中的事件绑定、构建器和类型转换可在上游的 `integration/kjs` 包中查询。[GregTech CEu Modern 1.20.1 的 KubeJS 源码](https://github.com/GregTechCEu/GregTech-Modern/tree/1.20.1/src/main/java/com/gregtechceu/gtceu/integration/kjs)
