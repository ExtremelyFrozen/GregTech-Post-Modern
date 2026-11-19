---
title: 复制机器与覆板设置
---


# 为机器与覆板添加设置复制

机器记忆卡通过 `ICopyable` 读写 `SyncFieldData`。受管对象的默认实现通过字段 Codec 复制 `@SaveField` 数据。机器和覆板可选择其中用于配置的字段，并在粘贴时用同一组键读回。

| 方法 | 用途 |
| --- | --- |
| `copyConfig(HolderLookup.Provider registries)` | 导出配置字段，返回 `SyncFieldData`。 |
| `pasteConfig(ServerPlayer player, HolderLookup.Provider registries, SyncFieldData config)` | 在服务端应用字段数据，并执行需要的设置更新。 |
| `getItemsRequiredToPaste()` | 返回粘贴设置所需的物品，例如目标缺少的覆板。 |

组合配置时可使用 `ConfigCopyHelper` 选取字段和合并覆板数据。记忆卡的配置组件与机器掉落物持久化分别读写，随机器拆装保存的字段使用 `@ItemSave`。 [ICopyable 源码](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/api/blockentity/ICopyable.java) · [字段保存](../../Development/Data-Sync-System/Usage.md)
