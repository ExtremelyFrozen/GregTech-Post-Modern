---
title: 自定义配方修改器
---


# 自定义配方修改器

`RecipeModifier` 根据机器实例和运行时 `GTRecipe` 返回 `ModifierFunction`。把修改器注册在机器构建器的 `recipeModifiers(...)` 上，机器匹配候选后执行这些修改器，并重新检查实际使用的输入与输出。

??? example "将机器处理时间缩短到原来的 75%"

    ```java
    static final RecipeModifier FASTER_PROCESSING = (machine, recipe) ->
            ModifierFunction.builder()
                    .durationMultiplier(0.75)
                    .build();
    ```

    在机器构建器上调用 `recipeModifiers(FASTER_PROCESSING)`。修改器以运行时配方为输入，原始配方定义继续供查看器和索引读取。

| 接口 | 作用 |
| --- | --- |
| `durationMultiplier`、`eutMultiplier` | 缩放运行时间或每刻 EU。 |
| `inputModifier`、`outputModifier` | 调整开始输入和完成输出。 |
| `tickInputModifier`、`tickOutputModifier` | 调整一般的每刻能力内容，EU 由 `eutModifier` 处理。 |
| `conditions` | 为修改后的运行时配方添加条件。 |
| `ModifierFunction.IDENTITY` | 采用原配方。 |
| `ModifierFunction.cancel(reason)` | 停止当前候选并提供失败原因。 |

需要随等级、并行仓或批处理设置变化时，可以组合 GTPM 提供的修改器。 [超频与并行](../../Development/General-Topics/Overclocking-And-Parallel-Logic.md)
