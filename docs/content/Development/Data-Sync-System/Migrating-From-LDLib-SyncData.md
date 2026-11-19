---
title: 从 LDLib SyncData 迁移
---


# 从 LDLib SyncData 迁移

GTPM 的保存与同步由 `SyncDataHolder` 和字段 Codec 驱动。LDLib2 提供界面、控件和菜单框架。


## 字段迁移

| 旧入口 | 当前入口 |
| --- | --- |
| `ManagedFieldHolder`、`FieldManagedStorage` | `ISyncManaged` 和 `SyncDataHolder`。现有机器、trait 与覆板通过自身持有者接入。 |
| `@Persisted(key = "...")` | `@SaveField(nbtKey = "...")`。使用原字段键时仍需匹配当前 Codec 的数据结构。 |
| `@DescSynced` | `@SyncToClient`。客户端可编辑且服务端需要回传的设置使用 `@SyncBoth`。 |
| `@RequireRerender` | `@RerenderOnChanged`。 |
| `@UpdateListener` | `@ClientFieldChangeListener`。服务端提交回调使用 `@ServerFieldChangeListener`。 |
| `IAutoPersistBlockEntity`、`IAutoSyncBlockEntity` | `ManagedSyncBlockEntity`，由 ticker 调用 `serverTick()`。 |
| `ValueTransformer` 和字段的旧 NBT 编解码入口 | 在 `FieldCodecs` 注册 `Codec`、`StreamCodec` 或 `ContextualFieldCodec`。 |
| 手写配置动作包 | 双向同步字段或受管对象的方法 RPC。 |

??? example "迁移机器的两个字段"

    ```java
    @SaveField(nbtKey = "custom_value")
    @SyncToClient
    @RerenderOnChanged
    private int displayValue;

    @SaveField(nbtKey = "custom_name")
    private String customName = "";
    ```

    `displayValue` 在服务端更新后同步到客户端并刷新渲染。`customName` 随世界存档保存。


## 存档与物品

自定义存档工具和 GameTest 模板应使用 `gtpm_sync_data` 及相应的 `SyncFieldData` 表示。迁移已有方块实体数据时，可在 `loadAdditional(tag, registries)` 中转换旧字段，然后调用父类以读取转换后的数据。字段值结构由现行 Codec 决定。

机器拆装时随掉落物保留的属性使用 `@ItemSave`。可手持的物品通过 `ItemSyncHolder` 使用 `gtpm:item_data`。机器记忆卡使用独立的配置数据组件，并通过 `ICopyable` 的 Codec 字段读写。 [持有者与 Codec](Usage.md) · [复制机器与覆板设置](../../Modpacks/Other-Topics/Cover-Machine-Copy-Paste-Support.md)


## 界面迁移

旧 LDLib 界面布局可以从 LDLib2 模板重建。界面上的普通设置可直接绑定到双向同步字段，执行操作用方法 RPC。 [LDLib2 界面编辑器](../../Modpacks/Other-Topics/LDLib-UI-Editor.md) · [方法 RPC](RPC.md)
