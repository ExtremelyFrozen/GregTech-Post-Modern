---
title: 多方块主机界面
---


# 修改多方块主机界面

多方块主页由机器的 LDLib2 页面提供。整合包可在机器定义中添加状态文本，附属模组也可以自定义整个页面。


## 添加主机状态文本

`MultiblockMachineBuilder.additionalDisplay` 接收 `BiConsumer<MultiblockControllerMachine, List<Component>>`。在回调中向组件列表加入文本，默认多方块显示页会把它与机器状态一并绘制。

??? example "成型时增加一行本地化提示"

    ```java
    builder.additionalDisplay((machine, text) -> {
        if (machine.isFormed()) {
            text.add(Component.translatable("machine.example.heater.ready"));
        }
    });
    ```

    在附属模组的语言资源中定义该翻译键。


## 定制 LDLib2 页面

在控制器类中覆盖 `createUIWidget(FancyMachineUIWidget shell)`，返回 LDLib2 的 `UIElement`。`GTComponentPanelElement` 可从 `addDisplayText` 读取动态文本。`attachSideTabs` 和 `attachConfigurators` 分别提供侧边页和设置控件。拥有交互槽位的页面还可以由可用部件提供。 [IFancyUIProvider 源码](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/api/gui/fancy/IFancyUIProvider.java)

机器、部件与覆板使用自身的界面入口。客户端状态显示使用 `@SyncToClient`，双向设置使用 `@SyncBoth`，需要执行一次的交互使用 `@RPCMethod`。 [字段同步](../../Development/Data-Sync-System/index.md) · [XML 模板](LDLib-UI-Editor.md)
