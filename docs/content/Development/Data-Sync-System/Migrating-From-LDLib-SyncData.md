---
title: "从 LDLib SyncData 迁移"
---
# 从 LDLib SyncData 迁移

### 简单示例

这个简单示例覆盖了向标准 machine、machine trait 或 cover 添加同步/保存字段时的大多数用例。

#### 使用 LDLib 时：
```java
class CustomMachine extends SimpleTieredMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(CustomMachine.class,
            SimpleTieredMachine.MANAGED_FIELD_HOLDER);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected int customIntValue;

    @Persisted(key = "customNBTKey")
    protected String customStringValue;

    public void setCustomIntValue(int newValue) {
        this.customIntValue = newValue;
    }
}
```

#### 新系统：
```java
class CustomMachine extends SimpleTieredMachine {
    @Getter
    @SaveField
    @SyncToClient
    protected int customIntValue;

    @SaveField(nbtKey = "customNBTKey")
    protected String customStringValue;

    public void setCustomIntValue(int newValue) {
        this.customIntValue = newValue;
    }
}

```

### 通用迁移准则

- 移除所有 `ManagedFieldHolder` 字段。
- 将 `FieldManagedStorage` 字段替换为 `SyncDataHolder` 字段。
- 将 `IEnhancedManaged` 对象替换为 `ISyncManaged`。
- 将 `IAsyncAutoSyncBlockEntity`、`IAutoPersistBlockEntity`、`IAutoSyncBlockEntity` 和 `IManagedBlockEntity` 替换为继承 `ManagedSyncBlockEntity`。
- 只有真正需要 `SavedData` 后端的全局或跨区块持久数据才迁移到 `ManagedSavedData`。默认 sync save key 是 `${GTCEu.MOD_ID}_sync_data`，当前 key 为 `gtpm_sync_data`；如果某个 `SavedData` 子类需要独立后端，应设置专用 key。

### LDLib2 界面迁移

页签、临时筛选等界面状态可保留在客户端。需要同步的业务状态使用 GTPM 的同步字段。机器、覆板等需要服务端执行的操作可参照现有的 `@RPCMethod` 用法，在服务端验证操作目标和参数后更新状态。

### 注解

!!! note
    同步系统会自动检测字段变更。可变对象的内部状态可以通过受管子对象或上下文 Codec 同步。

- `@DescSynced` -> `@SyncToClient`
- `@RequireRerender` -> `@RerenderOnChanged`
- `@Persisted` -> `@SaveField`
- `@UpdateListener` -> 在 listener method 上使用 `@ClientFieldChangeListener`
- `@DropSaved` - 已移除，改为让 machine 实现 `IDropSaveMachine`
- `@ReadOnlyManaged` 和 `@LazyManaged` - 有关复杂 sync object 的说明，请参阅用法文档

### 其他变更

 - `saveCustomPersistedData` 和 `loadCustomPersistedData` 方法，以及自定义数据类型的序列化 - 请参阅 `ValueTransformer<T>` 和 `ValueTransformers` 类。
