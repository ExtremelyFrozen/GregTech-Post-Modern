---
title: 生成层与维度
---


# 生成层与维度

`layer` 选择生成层，决定可替换的方块和适用维度。`dimension_filter` 从这些维度中进一步筛选，`biomes` 在维度内选择生物群系。

| 层 | 默认维度 | 目标 |
| --- | --- | --- |
| `stone` | `minecraft:overworld` | 石质矿石替换目标。 |
| `deepslate` | `minecraft:overworld` | 深板岩矿石替换目标。 |
| `netherrack` | `minecraft:the_nether` | 下界开凿可替换方块。 |
| `endstone` | `minecraft:the_end` | 末地矿石替换目标。 |


## 注册新的生成层

附属模组在 `IGTAddon.registerWorldgenLayers()` 中构造 `SimpleWorldGenLayer`，传入层名、提供 `RuleTest` 的工厂，以及接受该层的维度键集合。矿脉 JSON 的 `layer` 使用注册时的层名。

??? example "仅在末地替换末地石的层"

    ```java
    @Override
    public void registerWorldgenLayers() {
        new SimpleWorldGenLayer(
                "example_end_stone",
                () -> new BlockMatchTest(Blocks.END_STONE),
                Set.of(Level.END));
    }
    ```

    对应矿脉使用 `"layer": "example_end_stone"` 和 `"dimension_filter": ["minecraft:the_end"]`。 [矿脉定义](01-Customizing-Veins.md)
