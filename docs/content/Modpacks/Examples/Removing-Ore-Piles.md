---
title: 移除地表矿石指示物
---


# 移除地表矿石指示物

地表指示物由每个矿脉的 `indicators` 列表控制。

1. 找到要修改的矿脉 ID，从内置资源复制整份定义。例如 `gtpm:cassiterite` 使用 `data/gtpm/gtpm/ore_vein/cassiterite.json`。
2. 在数据包中以相同的路径保存定义，将 `indicators` 设置为空列表 `[]`。
3. 重新进入世界，让新生成区块使用修改后的矿脉定义。

[矿脉文件路径与覆盖方式](../Ore-Generation/01-Customizing-Veins.md)
