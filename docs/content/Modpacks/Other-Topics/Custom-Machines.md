---
title: 自定义机器
---


# 自定义机器

附属模组通过自己的 `GTRegistrate` 注册机器。机器工厂接收 `BlockEntityCreationInfo`，构建器配置配方类型、模型、朝向和显示信息。


## 单方块机器

| 机器类型 | 注册入口 |
| --- | --- |
| 电力机器 | `GTMachineUtils.registerSimpleMachines` 可以用配方类型、流体容量函数和等级列表批量注册 `SimpleTieredMachine`。 |
| 蒸汽机器 | `GTMachineUtils.registerSteamMachines` 为低压与高压版本提供相同的定义入口，工厂接收压力变体。 |
| 发电机 | `GTMachineUtils.registerSimpleGenerator` 为 `SimpleGeneratorMachine` 配置燃料类型、流体容量与电压等级。 |
| 自定义运行逻辑 | 使用 `GTRegistrate.machine(name, factory)` 绑定自己的机器类，在类中接入需要的 trait。 |

??? example "为附属模组注册两种电压的电力机器"

    ```java
    GTMachineUtils.registerSimpleMachines(
            registrate, "polisher", POLISHING,
            tier -> tier * 3200, false, GTValues.LV, GTValues.MV);
    ```

`registrate` 为附属模组的 `GTRegistrate`，`POLISHING` 为已注册的配方类型。 [配方类型注册](Custom-Recipe-Types.md)


## 多方块机器

用 `GTRegistrate.multiblock(name, factory)` 创建主机。通过 `pattern` 为每个名称注册一个结构。主体结构的名称为 `main`。

??? example "为自定义加工机注册外壳、舱室和主机布局"

    ```java
    registrate.multiblock("large_polisher", WorkableElectricMultiblockMachine::new)
            .recipeType(POLISHING)
            .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
            .pattern(MultiblockControllerMachine.DEFAULT_STRUCTURE, definition ->
                    PatternBuilder.start(definition)
                            .aisle("XXX", "X~X", "XXX")
                            .aisle("XXX", "X#X", "XXX")
                            .where('X', PatternPredicate.blocks(GTBlocks.CASING_STEEL_SOLID.get())
                                    .or(PatternPredicate.autoAbilities(definition.getRecipeTypes())))
                            .where('#', PatternPredicate.air())
                            .build())
            .workableCasingModel(
                    GTCEu.id("block/casings/solid/machine_casing_solid_steel"),
                    GTCEu.id("block/multiblock/large_chemical_reactor"))
            .register();
    ```

结构中的 `~` 对应主机，`X` 接受指定机壳或配方类型需要的舱室，`#` 要求空气。通过 `PatternPredicate` 可加入线圈、维护仓、消声仓及各项数量限制。终端和 JEI、EMI 从机器定义及其结构资源计算预览。通过谓词中的候选与数量规则控制默认展示。 [多方块结构文件](Multiblock-Structures.md) · [多方块结构开发](../../Development/General-Topics/Multiblock-Structures.md)

给控制器添加文本、配置页或工作钩子的方式见相关页面。 [控制器界面](Modifying-Multiblock-Controller-UI.md) · [自定义机器行为](Custom-Machine-Behavior.md)
