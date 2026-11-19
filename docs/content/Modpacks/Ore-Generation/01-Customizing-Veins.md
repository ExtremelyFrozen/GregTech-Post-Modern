---
title: 自定义矿脉
---


# 添加、覆盖与停用普通矿脉

普通矿脉的动态注册表为 `gtpm:ore_vein`。矿脉文件的位置是 `data/<命名空间>/gtpm/ore_vein/<路径>.json`。例如 `gtpm:cassiterite` 对应 `data/gtpm/gtpm/ore_vein/cassiterite.json`。 [内置矿脉定义](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/tree/1.21/src/generated/resources/data/gtpm/gtpm/ore_vein)


## 创建矿脉

| 字段 | 用途 |
| --- | --- |
| `layer` | 决定生成层及其可替换的岩石。 |
| `dimension_filter`、`biomes` | 限定维度和生物群系。生物群系可以是 ID、列表或标签。 |
| `weight`、`weight_modifier` | 候选矿脉的基础相对权重和按生物群系计算的修正。 |
| `cluster_size`、`height_range` | 矿脉大小采样与放置高度。大小可以用固定整数或 IntProvider 表示。 |
| `density`、`discard_chance_on_air_exposure` | 生成密度与暴露在空气时的丢弃概率，均在 0 到 1 之间。 |
| `generator`、`indicators` | 矿脉生成器和地表指示物列表。 |

??? example "data/example/gtpm/ore_vein/silver_dike.json"

    ```json
    {
      "layer": "stone",
      "dimension_filter": ["minecraft:overworld"],
      "biomes": "#minecraft:is_overworld",
      "weight": 20,
      "cluster_size": 16,
      "density": 0.4,
      "height_range": {
        "height": {
          "type": "minecraft:uniform",
          "min_inclusive": { "absolute": 20 },
          "max_inclusive": { "absolute": 60 }
        }
      },
      "discard_chance_on_air_exposure": 0.0,
      "generator": {
        "type": "gtpm:dike",
        "min_y": 20,
        "max_y": 60,
        "blocks": [
          { "block": "gtpm:silver", "weight": 3, "min_y": 20, "max_y": 60 }
        ]
      },
      "indicators": []
    }
    ```

启用数据包后重新进入世界。在有管理员权限的测试世界，可以用 `/gtceu place_vein example:silver_dike` 生成该矿脉并查看形状。命令接受可选的坐标参数。Dike 使用生成器配置的 `min_y` 和 `max_y` 作为垂直范围。


## 覆盖与停用矿脉

在高优先级的数据包中提供同命名空间和路径的完整定义即可覆盖矿脉。要停用一个普通矿脉，保留其余字段，把 `weight` 改为 `0` 并移除 `weight_modifier`。要保留矿脉并移除地表指示物，将 `indicators` 设为 `[]`。

[生成器字段](02-Generators.md) · [层与维度](04-Layers-and-Dimensions.md)
