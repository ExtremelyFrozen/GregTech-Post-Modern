---
title: 矿脉生成器
---


# 矿脉与指示物生成器

矿脉的 `generator.type` 指定几何生成方式。配置中接受材料的位置可以填写材料 ID，由生成器选择适合目标岩石的矿石变体。某些生成器也接受原版替换规则和方块状态。


## 矿脉生成器 { #vein-generators }

| 类型 | 几何与主要字段 |
| --- | --- |
| `gtpm:layer` | 分层矿脉。`layer_patterns` 定义分层序列，每层可配置不同材料、替换目标、厚度和权重。 |
| `gtpm:veined` | 噪声驱动的矿脉。`ore_blocks`、`rare_blocks` 和 `rare_block_chance` 定义矿物分布，`veininess_threshold`、`min_richness`、`max_richness` 等控制形状与充实程度。 |
| `gtpm:dike` | 垂直岩墙。`min_y`、`max_y` 定义整个生成器的高度范围，`blocks` 定义在相应高度可选的材料或替换目标及权重。 |
| `gtpm:standard` | 紧凑的标准矿脉。`targets` 对应普通石头、深板岩和下界的替换结果。 |
| `gtpm:geode` | 晶洞。`blocks` 定义各层方块，`layers` 和 `crack` 定义厚度和裂隙。 |
| `gtpm:classic`、`gtpm:cuboid` | 传统分层矿脉。`classic` 提供 `primary`、`secondary`、`between`、`sporadic`，`cuboid` 提供 `top`、`middle`、`bottom`、`spread`。 |

### 岩墙矿脉横截面

Dike 从 `cluster_size` 采样并向上取整得到半径 `r = ceil(cluster_size / 2)`。候选点满足 `dx² + dz² ≤ r²`，再按噪声、密度和目标方块条件筛选。调大 `cluster_size` 会增大岩墙的圆形横截面。`blocks` 中每项的 `min_y` 和 `max_y` 决定该项参与选择的高度。 [完整岩墙定义](01-Customizing-Veins.md)

### 脉状矿脉的高度与边缘

`min_y` 和 `max_y` 控制生成器的垂直范围。`edge_roundoff_begin` 与 `max_edge_roundoff` 控制靠近端部的收缩。`veininess_threshold` 设置噪声通过阈值，`max_richness_threshold` 与丰度范围决定内部矿石分布。 [锡石矿脉示例](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/generated/resources/data/gtpm/gtpm/ore_vein/cassiterite.json)


## 地表指示物 { #indicator-generators }

向 `indicators` 添加 `gtpm:surface` 条目可在矿脉附近放置地表线索。`block` 可指定材料 ID 或方块状态，`radius` 和 `density` 控制分布。使用空列表可移除该矿脉的全部指示物。

??? example "在锡矿脉附近生成地表指示物"

    ```json
    {
      "type": "gtpm:surface",
      "block": "gtpm:tin",
      "density": 0.2,
      "placement": "surface",
      "radius": 5
    }
    ```

附属模组可通过 `IGTAddon.registerVeinGenerators()` 与 `registerIndicatorGenerators()` 注册新类型。 [生成器注册表](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/tree/1.21/src/main/java/com/gregtechceu/gtceu/api/data/worldgen/generator)
