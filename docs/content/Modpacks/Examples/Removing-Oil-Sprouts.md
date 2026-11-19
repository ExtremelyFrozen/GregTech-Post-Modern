---
title: 配置原油苗
---

# 配置原油苗

原油苗由 Minecraft 的世界生成特征 `gtpm:raw_oil_sprout` 生成。可以使用数据包覆盖对应的 JSON 文件。

## 移除原油苗

在数据包中用 `minecraft:no_op` 覆盖原油苗的 configured feature，可以禁用其生成。

```json title="data/gtpm/worldgen/configured_feature/raw_oil_sprout.json"
{
    "type": "minecraft:no_op",
    "config": {}
}
```

## 调整原油苗放置条件

将内置的 placed feature 复制到数据包的 `data/gtpm/worldgen/placed_feature/raw_oil_sprout.json`。修改 `minecraft:rarity_filter` 的 `chance` 即可调整放置频率，默认值为 `128`。[原油苗放置配置](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/generated/resources/data/gtpm/worldgen/placed_feature/raw_oil_sprout.json)
