---
title: 配方条件
---


# 配方条件

`recipeConditions` 中的条件由机器检查。每个条目用 `type` 选择条件类型。带有 `reverse` 的条件可用 `true` 反转通过条件，例如限制配方在某个维度以外运行。

??? example "限制运行维度和高度"

    将以下字段写入机器配方 JSON。

    ```json
    {
      "recipeConditions": [
        { "type": "gtpm:dimension", "dimension": "minecraft:overworld" },
        { "type": "gtpm:pos_y", "min": 0, "max": 128 }
      ]
    }
    ```


## 内置条件

| 类型 | 配置项 |
| --- | --- |
| `gtpm:dimension` | `dimension` 为维度 ID。 |
| `gtpm:biome`、`gtpm:biome_tag` | `biome` 为生物群系 ID，`biome_tag` 为生物群系标签 ID。 |
| `gtpm:pos_y` | `min` 和 `max` 为主机高度范围。 |
| `gtpm:adjacent_block`、`gtpm:adjacent_fluid` | `blocks` 或 `fluids` 列出相邻位置可用的方块或流体。 |
| `gtpm:rain`、`gtpm:thunder` | `level` 为所需的降雨或雷暴强度。 |
| `gtpm:daytime`、`gtpm:steam_vent` | 检查白天或蒸汽机器排气状态。 |
| `gtpm:cleanroom` | `cleanroom` 指定洁净室类型。 |
| `gtpm:eu_to_start` | `eu_to_start` 指定启动要求的 EU。 |
| `gtpm:research` | `research` 保存研究条目。 |
| `gtpm:environmental_hazard` | `condition` 指定医学状态。 |
| `gtpm:ftb_quest`、`gtpm:heracles_quest` | 在对应任务模组加载时注册，`questId` 为任务标识。 |

配方启动检查在消耗一次性输入前执行。白天、降雨、雷暴、排气与洁净室等返回 `perTick() == true` 的条件还会在运行中检查。运行条件失败会使配方进入等待，并在机器界面显示原因。 [配方执行](../../Development/Recipe-Logic/Recipe-Execution.md)

附属模组可在 `GTRegistries.RECIPE_CONDITIONS` 中注册自己的 `RecipeConditionType`，并提供该条件的 `MapCodec`。 [内置条件登记](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/common/data/GTRecipeConditions.java)
