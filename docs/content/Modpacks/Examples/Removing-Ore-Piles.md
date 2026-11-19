---
title: "移除地表矿石指示物"
---


# 移除地表矿石指示物

!!! note "上游示例"
    本页示例面向 GregTech CEu Modern。GTPM 1.21.1 的数据包和附属模组入口见[整合包制作](../index.md#upstream-kubejs)。

## 移除脚本

```js title="remove_piles.js"
GTCEuServerEvents.oreVeins(event => {
    event.modifyAll((veinId, vein) => {
        vein.surfaceIndicatorGenerator(indicator => indicator
            .block(Block.getBlock("minecraft:air")) // (1)
            .placement("above")
            .density(0.4) // Unsure if this matters
            .radius(5) // Unsure if this matters
        )
    })
})
```

1. 将原本会生成矿石堆的位置替换为空气方块，从效果上移除它。