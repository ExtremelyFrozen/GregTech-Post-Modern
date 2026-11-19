---
title: 配方逻辑
---


# 配方定义与机器状态

| 类型 | 职责 |
| --- | --- |
| `GTRecipeDefinition` | 配方加载、同步、研究查询与查看器消费的定义。通过 `GTRecipeSerializer` 进行数据与网络序列化。 |
| `GTRecipe` | 搜索、超频、并行和执行使用的运行时配方。记录执行过程需要的派生数据。 |
| `RecipeLogic` | 机器的配方 trait，管理搜索、修改、进度、消耗、产出和失败原因。 |
| `WorkLogic` | 通用工作状态 trait，管理工作开关、状态、等待原因和 Tick 订阅。`RecipeLogic` 扩展该类。 |

`GTRecipeDefinition.toRuntime()` 复制能力内容和可变配方数据。查看器、配方 ID 和研究数据查询使用定义。修改超频、并行或执行字段时操作运行时配方。`GTRecipeBuilder.buildDefinition()` 生成定义，`save(RecipeOutput)` 导出定义；`build()` 提供运行时配方。

输入、输出、每刻输入和每刻输出均使用 `ContentListMap`，按 `RecipeCapability` 键访问。内置能力包括物品、流体、EU 与 CWU。EU 能力保存 `Long` 总量，配方等级用独立的 `tier` 表示。`RecipeHandlerGroup` 和 `RecipeHandlerList` 决定参与匹配和执行的处理器组合。


## 运行流程

1. `RecipeLogic` 重用可运行的上一条配方，或者根据机器提供的输入查询候选。
2. `IRecipeLogicMachine.fullModifyRecipe` 应用机器的运行时修改器，再检查条件与输入输出。通过检查后消耗开始输入，进入工作状态。
3. 每次工作 Tick 检查动态条件和每刻 I/O。成功后增加进度，失败时记录 `ActionResult` 的原因并进入等待。
4. 到达目标进度后发送一次性输出。输出受阻时保留待完成状态。输出成功后，根据当前条件和修改器开始下一轮或回到空闲。

通用机器界面和 Jade 从 `IWorkLogicMachine` 读取状态。需要查看当前配方、匹配失败原因和配方处理器时使用 `IRecipeLogicMachine`。 [配方搜索](Recipe-Searching.md) · [配方执行](Recipe-Execution.md)
