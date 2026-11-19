---
title: 配方搜索与索引
---


# 配方搜索与索引


## 服务端索引

配方资源加载结束后，GTPM 收集 `RecipeManager` 中的配方定义，处理配方类型的派生输出，并把该类型使用的原版或其他类型的配方转为代理配方。`RecipeAdditionHandler` 按类型开启暂存，调用 `StagingRecipeDB` 对输入建立索引，再更新机器搜索所用的 `RecipeDB`。

暂存器把普通输入和参与索引的每刻输入转换为 `AbstractMapIngredient`。它统计索引成分出现的频率，将优先级较高的成分排在前面，借助共享分支保存输入组合。`GTRecipeDefinition` 分开生成普通和每刻输入的索引成分。索引的叶节点持有机器搜索使用的运行时配方。

??? example "普通输入与启动匹配"

    粉碎机的物品输入提供物品成分索引，`RecipeDB` 由此得到候选。机器继续用 `RecipeHelper` 计算输入数量、输出空间和每刻能耗，并用配方条件决定是否能够启动。


## 从机器输入查询

`IRecipeCapabilityHolder` 暴露按方向和能力组织的 `RecipeHandlerList`。`RecipeDB` 收集这些处理器的输入并调用能力对应的索引转换器。物品与流体可提供具体值、标签或其他索引表示。同一输入的不同表示组成一组候选。

`RecipeDB.RecipeIterator` 遍历满足索引路径的叶节点，向 `RecipeLogic` 提供候选。机器修改器可以改变原配方内容，最终是否可执行取决于修改后的运行配方、机器条件和处理器模拟。匹配失败的候选可随处理器变化再次检查。 [配方执行](Recipe-Execution.md)


## JEI 与 EMI 的展示索引

客户端在 `RecipesUpdatedEvent` 后用已接收的完整配方列表重建各类型的展示数据。空类型也会提交，使删除配方、重载和切换服务器时及时更新。代理配方在客户端根据其来源生成展示定义，使用独立的合成标识。

查看器从展示定义读取物品、流体、能耗和条件。多方块结构是单独的预览类别，所需材料按预览选项求解。 [配方展示刷新](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/integration/xei/RecipeDisplayReload.java)
