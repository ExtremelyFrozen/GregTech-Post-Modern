---
title: 持有者与 Codec
---


# 持有者与 Codec


## 持有者

| 类型 | 用途 |
| --- | --- |
| `ManagedSyncBlockEntity` | 普通方块实体。世界保存通过 `gtpm_sync_data` 下的 `SyncFieldData` 写入。服务端 `serverTick()` 扫描字段，初始状态与增量通过同步包发送给相应客户端。 |
| `ISyncManaged` | 需要主动跟踪字段、同步或执行实例 RPC 的对象。实现 `getSyncDataHolder()`，并通过 `getParentSyncObject()` 接入拥有同步生命周期的宿主。机器、机器 trait 和覆板已有该接入点。 |
| `ISyncAnnotated` | 用字段描述的值对象。通过所在受管字段或自身 Codec 读写。构造器或工厂提供存档缺省值。 |
| `ManagedSavedData` | 跨区块或全局的原版 `SavedData`。默认键为 `gtpm_sync_data`，子类也可指定保存键。 |
| `ItemSyncHolder` 与 `SyncItem` | 每个物品栈的字段状态。`ItemSyncHolder.create(stack, registries)` 通过物品的工厂创建实例并读取 `gtpm:item_data` 组件。`saveToStack()` 写回数据，`SyncMenu` 在界面生命周期中驱动双向同步。 |

自定义方块实体在服务端 ticker 调用 `serverTick()`。覆盖该方法时调用 `super.serverTick()`，让持有者发送字段变更。已有机器、trait 或覆板子类直接为字段添加所需注解。 [字段注解](Annotations.md)

机器掉落物字段由 `@ItemSave` 指定。手持物品的 `ItemSyncHolder` 直接使用 `@SaveField`，例如终端的主机绑定和结构选项。


## 类型与 Codec 注册

`FieldCodecs` 按字段声明类型查找编码器。内置注册覆盖基本类型、枚举、数组、常用 Minecraft 值和 GTPM 注册表引用。受支持的 Java、Kotlin 和 fastutil 集合按声明的类型参数解析元素 Codec，例如 `List<ItemStack>` 和 `Map<ResourceLocation, Integer>`。

| 接口 | 用途 |
| --- | --- |
| `FieldCodecs.register(type, codec)` | 注册可独立编码的值，包括记录、注册表引用和带有原生 Codec 的领域对象。 |
| `FieldCodecs.registerStream(type, streamCodec)` | 注册网络编码，支持基于 `ByteBuf` 或 `RegistryFriendlyByteBuf` 的 `StreamCodec`。 |
| `FieldCodecs.registerContextual(type, codec)` | 根据当前值、注册表、字段信息或同步上下文提供 `Codec<T>`。例如解码时更新既有容器。 |
| `FieldCodecs.registerFactory(type, factory)` | 为需在解码时创建的受管类型提供实例工厂。 |
| `FieldCodecs.createCodec(factory)` | 从工厂创建的对象及其 `@SaveField` 生成 Codec。`createStreamCodec(factory)` 提供使用注册表的网络编码。 |

在领域类型或实际使用点注册其 Codec，并在字段元数据首次解析前完成注册。已注册的独立 Codec 可同时供保存与同步路径使用。对只需同步的类型，可以注册网络 Codec。对同时保存与同步的类型，提供持久化 Codec，并按需要注册独立的网络 Codec。

??? example "注册可保存的坐标值"

    ```java
    public record ScanTarget(BlockPos position) {
        public static final Codec<ScanTarget> CODEC =
                BlockPos.CODEC.xmap(ScanTarget::new, ScanTarget::position);

        public static void registerFieldCodec() {
            FieldCodecs.register(ScanTarget.class, CODEC);
        }
    }
    ```

    在附属模组初始化时调用 `ScanTarget.registerFieldCodec()`。受管对象可将 `ScanTarget` 用作保存或同步字段。

持久化以 `SyncFieldData.CODEC` 和相应的注册表上下文写入。网络字段按自身 Codec 直接编码。上下文 Codec 提供值的编码方式，调用者选择保存或网络场景。 [FieldCodecs 源码](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/api/sync_system/FieldCodecs.kt)
