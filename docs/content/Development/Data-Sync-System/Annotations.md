---
title: 字段注解
---


# 字段注解

| 注解 | 作用 |
| --- | --- |
| `@SaveField` | 将字段写入持有者的持久数据。`nbtKey` 默认为字段名。使用独立的键可以在重命名字段后继续读取对应数据。 |
| `@ItemSave` | 将机器字段保存到机器掉落物，重新放置时读回。键规则与 `@SaveField` 相同。 |
| `@SyncToClient` | 将服务端值发送给客户端。初始同步提供完整值，之后发送变化。 |
| `@SyncToServer` | 允许客户端当前菜单提交字段变化。 |
| `@SyncBoth` | 允许客户端提交字段变化，并用服务端值更新客户端。 |
| `@RerenderOnChanged` | 客户端收到字段变化时刷新方块渲染。用于向客户端同步的字段。 |


## 同步回调

`@ClientFieldChangeListener(fieldName = {...})` 标记客户端无参数方法。任一指定字段在客户端变化后调用一次，多个字段同批变化时合并调用。

`@ServerFieldChangeListener(fieldName = {...})` 用于 `@SyncToServer` 或 `@SyncBoth`。服务端成功应用整批更新后执行。监听方法返回 `void`，可无参数，或接收两个与字段类型完全一致的参数，依次为旧值和新值。带参数的方法可以监听同类型字段。

`@ServerFieldNormalizer(fieldName = {...})` 用于 `@SyncBoth`。方法接收解码后的客户端候选值，返回服务端采用的同类型值。规范化在应用更新前运行，保持纯函数。抛出异常会拒绝整批更新，规范化后的值会随双向同步回到客户端。

??? example "接收界面提交的批处理上限"

    ```java
    @SaveField(nbtKey = "batch_limit")
    @SyncBoth
    private int batchLimit = 1;

    @ServerFieldNormalizer(fieldName = "batchLimit")
    private int normalizeBatchLimit(int candidate) {
        return Math.clamp(candidate, 1, 64);
    }

    @ServerFieldChangeListener(fieldName = "batchLimit")
    private void onBatchLimitChanged(int previous, int current) {
        getRecipeLogic().markLastRecipeDirty();
    }
    ```

机器与持有物品菜单会扫描编码后的值。客户端界面给受管字段赋值或更新受支持的可变集合即可。双向同步以服务端提交的值为准，合并集合时保留既有容器引用。
