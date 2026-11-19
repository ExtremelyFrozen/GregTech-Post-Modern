---
title: 超频与并行
---


# 超频与并行

机器在 `IRecipeLogicMachine.fullModifyRecipe` 中为运行时配方应用修改器。`GTRecipe` 的 `tier` 表示配方等级，`getInputEUt()` 和 `getOutputEUt()` 返回每刻总用电量和发电量。


## 电力超频

机器定义可以使用 `GTRecipeModifiers.OC_NON_PERFECT`、`OC_PERFECT` 或相应的子刻版本。修改器根据 `IOverclockMachine.getOverclockVoltage()` 与配方用量计算超频。机器等级电压由 `ITieredMachine.getTierVoltage()` 提供。

发电配方以 `getOutputEUt()` 读取每刻产出。大型燃烧引擎的燃料显示使用当前运行配方中的实际输入。尚未运行但已经找到候选时，才按机器可用发电电压预估并行消耗。


## 并行与批处理

机器可通过 `GTRecipeModifiers.PARALLEL_HATCH` 采用可用并行仓的设置，并通过 `BATCH_MODE` 使用批处理。`ParallelLogic` 根据可用输入、输出容量及机器条件限制实际并行数量。自定义修改器要同时记录并行数量和修改对应能力的内容。`parallels`、`batchParallels` 和 `subtickParallels` 用于保存并行信息，具体用量由内容修改器处理。

??? example "给机器组合并行、超频与批处理"

    ```java
    builder.recipeModifiers(
            GTRecipeModifiers.PARALLEL_HATCH,
            GTRecipeModifiers.OC_NON_PERFECT_SUBTICK,
            GTRecipeModifiers.BATCH_MODE);
    ```

[配方执行](../Recipe-Logic/Recipe-Execution.md) · [自定义修改器](../../Modpacks/Recipes/Custom-Recipe-Modifiers.md)
