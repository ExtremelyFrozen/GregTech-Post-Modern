---
title: 基岩流体矿脉
---


# 基岩流体矿脉

基岩流体矿脉使用 `gtpm:bedrock_fluid` 动态注册表。将 JSON 放在 `data/<命名空间>/gtpm/bedrock_fluid/<路径>.json`。要覆盖内置原油矿脉，在高优先级的数据包放入 `data/gtpm/gtpm/bedrock_fluid/raw_oil_deposit.json`。

??? example "原油矿脉定义"

    ```json
    {
      "fluid": "gtpm:raw_oil",
      "weight": 20,
      "yield": { "min": 200, "max": 300 },
      "depletion_amount": 1,
      "depletion_chance": 100,
      "depleted_yield": 25,
      "dimension_filter": ["minecraft:overworld"]
    }
    ```

`fluid` 指向已注册流体。`yield.min` 和 `yield.max` 设置初始产量范围，`depleted_yield` 设置耗尽产量。`depletion_amount` 为耗竭扣除量，`depletion_chance` 为触发百分比。基础选择权重为 `weight`，可在 `weight_modifier` 中按生物群系调整。`dimension_filter` 指定允许的维度。

使用流体钻机从区块的基岩流体矿脉提取对应流体。修改定义后重新进入世界。 [内置流体矿脉定义](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/tree/1.21/src/generated/resources/data/gtpm/gtpm/bedrock_fluid)
