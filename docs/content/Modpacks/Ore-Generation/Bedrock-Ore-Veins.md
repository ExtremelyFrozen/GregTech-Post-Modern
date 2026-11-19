---
title: 基岩矿脉
---


# 基岩矿脉

基岩矿脉使用 `gtpm:bedrock_ore` 动态注册表。将 JSON 放在 `data/<命名空间>/gtpm/bedrock_ore/<路径>.json`，启用数据包后重新进入世界。探矿仪和基岩采矿机按世界中的对应矿脉工作。

??? example "data/example/gtpm/bedrock_ore/tin.json"

    ```json
    {
      "weight": 30,
      "size": 3,
      "yield": 4,
      "depletion_amount": 1,
      "depletion_chance": 10,
      "depleted_yield": 1,
      "materials": [
        { "material": "gtpm:tin", "weight": 3 },
        { "material": "gtpm:cassiterite", "weight": 1 }
      ],
      "dimension_filter": ["minecraft:overworld"]
    }
    ```

| 字段 | 用途 |
| --- | --- |
| `weight`、`weight_modifier` | 矿脉基础选择权重和生物群系修正。 |
| `size` | 矿脉范围，单位为区块。 |
| `yield` | 矿脉未耗尽时的产量，可写为正整数或正整数 IntProvider。 |
| `depletion_amount`、`depletion_chance` | 每次耗竭扣除量与触发百分比。 |
| `depleted_yield` | 矿脉耗尽后的产量。 |
| `materials` | 候选材料及各自相对权重。 |
| `dimension_filter` | 矿脉可出现的维度。 |

[基岩矿脉数据模型](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/api/data/worldgen/bedrockore/BedrockOreDefinition.java)
