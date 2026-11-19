---
title: 方法 RPC
---


# 方法 RPC

同步字段描述界面设置和显示状态。RPC 用于执行单次操作，例如请求多方块重新检测、点击覆板配置槽或确认终端搭建方案。


## 受管对象的方法

在机器、trait、覆板或物品持有者上使用 `@RPCMethod`。方法名是调用名，参数按已注册的字段 Codec 编码。首个参数可以声明为 `RPCSender`，由接收端注入，其 `asPlayer()` 返回发起客户端请求的服务端玩家。接收端在游戏线程调用方法。

客户端使用 `rpcToServer(methodName, args...)`。受管机器和管道可通过 `rpcToPlayer(player, methodName, args...)` 或 `rpcToTracking(methodName, args...)` 向客户端发送方法调用。物品持有者的 RPC 由当前物品菜单承载。

??? example "从界面请求执行机器操作"

    ```java
    @RPCMethod
    public void requestStop(RPCSender sender) {
        var player = sender.asPlayer();
        if (player != null && MachineOwner.canOpenOwnerMachine(player, this)) {
            getRecipeLogic().setWorkingEnabled(false);
        }
    }
    ```

    机器界面的按钮事件调用 `rpcToServer("requestStop")`。操作方法根据发送方与机器的交互要求决定是否应用变更。


## 独立静态方法

用 `@RPCPacket("example:request_action")` 声明静态方法。`RPCPacketDistributor` 在模组发现阶段登记这些方法。客户端用 `RPCPacketDistributor.rpcToServer("example:request_action", args...)` 发送请求。可选集成可以在注解的 `modId` 中指定对应模组。方法参数和执行线程与实例 RPC 采用同一套规则。

需要可缺省的传输参数时，使用 `Optional<T>` 且为 `T` 提供 Codec。机器界面中持续编辑的设置可以直接使用 `@SyncBoth`，由服务端规范化并回传。 [字段注解](Annotations.md)
