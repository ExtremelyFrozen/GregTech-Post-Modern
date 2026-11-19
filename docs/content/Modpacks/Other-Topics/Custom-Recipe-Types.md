---
title: 自定义配方类型
---


# 自定义配方类型

附属模组可通过 `GTRecipeTypes.register(id, group, proxyRecipes...)` 注册配方类型。它以同一个 ID 登记 Minecraft 配方类型和序列化器。`setMaxIOSize` 依次指定物品输入、物品输出、流体输入和流体输出槽位上限。

??? example "注册有一个物品输入和输出的类型"

    ```java
    static final GTRecipeType POLISHING = GTRecipeTypes.register(
                    ResourceLocation.fromNamespaceAndPath("example", "polishing"),
                    GTRecipeTypes.ELECTRIC)
            .setMaxIOSize(1, 1, 0, 0);
    ```

在机器构建器上用 `recipeType(POLISHING)` 绑定类型。数据包用 `"type": "example:polishing"`，Java 配方生成使用 `POLISHING.recipeBuilder(id)`。通过 `proxyRecipes` 参数可把指定的原版或附属模组配方类型纳入代理配方。 [添加机器配方](../Recipes/Adding-and-Removing-Recipes.md) · [配方界面](LDLib-UI-Editor.md)
