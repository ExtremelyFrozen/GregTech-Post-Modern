---
title: 多方块结构开发
---


# 多方块结构开发

`MultiblockMachineBuilder.pattern(name, factory)` 注册命名结构。`MultiblockControllerMachine.DEFAULT_STRUCTURE` 对应 `main`。`requiresStructure(name, dependencies...)` 声明终端搭建该结构时需要一并搭建的其他结构。 [结构文件格式](../../Modpacks/Other-Topics/Multiblock-Structures.md)


## 注册结构

`PatternBuilder` 位于 `api.multiblock.pattern.dsl`。Java 和 Kotlin 都可通过 `start(definition, name)` 构造结构。构建器为 `~` 注册主机谓词，为空格注册任意方块谓词，其他字符通过 `where` 绑定 `PatternPredicate`。

??? example "为现有多方块构建器绑定主体和扩展结构"

    ```java
    builder.pattern(MultiblockControllerMachine.DEFAULT_STRUCTURE, definition ->
                    PatternBuilder.start(definition)
                            .aisle("XXX", "X~X", "XXX")
                            .where('X', PatternPredicate.blocks(Blocks.BRICKS))
                            .build())
            .pattern("extension", definition ->
                    PatternBuilder.start(definition, "extension")
                            .aisle(" ~ ")
                            .aisle("XXX")
                            .where('X', PatternPredicate.blocks(Blocks.STONE_BRICKS))
                            .build())
            .requiresStructure("extension", MultiblockControllerMachine.DEFAULT_STRUCTURE);
    ```

两套结构均以同一主机为锚点。设计可同时搭建的命名结构时，重叠位置应满足各自的谓词。使用 `appendDefinition()` 可以把当前结构键的资源定义引入构建器。需要把代码定义作为资源缺失时的回退时，在工厂中写入可独立成型的布局。

结构模型使用 `PatternDefinition`，包含坐标轴、谓词、节点、约束、参数与测量。Kotlin 代码可通过 `PatternBuilder` 的 `fragment`、`node`、`measurement` 和 `sizeConstraint` 添加布局。`PatternJsonCodec` 读取 JSON。`PatternBinaryCodec` 读取和写入同一模型的压缩二进制表示，机器与命名结构 ID 来自资源键。


## 注册可序列化的结构谓词

在 `IGTAddon.registerStructurePredicateTypes()` 中调用 `GTRegistrate.structurePredicate(name, codec)`。它注册实现 `StructurePredicate` 的类型与其 `MapCodec`，JSON 中的 `type` 使用附属模组的命名空间。内置类型和附属类型会在机器定义使用结构 Codec 前完成注册。

`observePosition` 通过世界状态生成匹配观察结果。`candidateStates` 与 `previewChoices` 提供供预览、终端和等级选择使用的候选。`collectDependencies` 声明匹配读取的位置，成型检测按这些位置跟踪方块变化。`diagnose` 为匹配失败提供 Jade 所用的原因。 [StructurePredicate 源码](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/api/multiblock/structurepredicate/StructurePredicate.kt)


## 注册材料来源

`GTRegistrate.materialSource(name, priority, provider).register()` 注册 `MaterialRequestProvider`。提供者接收玩家和本次材料请求，通过 `MaterialRequest.Result` 提供材料来源与绑定。批次规划向各来源查询同一批需求，按优先级从小到大分配，拆除返还按相反顺序处理。内置玩家物品栏优先级为 `0`，AE2 无线来源优先级为 `100`。


## 成型检测与预览

`MultiBlockPattern` 根据统一的结构模型解析布局并匹配世界。服务端收集候选读取依赖，按区块和位置索引结构变化，以共享预算分批处理检测。已有匹配快照可用于增量检查，候选依赖发生变化时会取消过时的检测。匹配成功后在服务端提交成型状态和部件归属。

终端与 JEI、EMI 都通过 `BatchPlanner` 展开结构、选择候选和计算材料。查看器使用无世界绑定的预览，终端使用当前主机的位置、朝向和世界状态生成实际计划。结构重载使已缓存的预览计划失效。 [玩家操作](../../Gameplay/Multiblocks.md)
