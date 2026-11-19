---
title: 自定义机器行为
---


# 自定义机器行为

附属模组可以继承与机器类型对应的类，并将构造器传给 `GTRegistrate.machine` 或 `multiblock`。与配方执行相关的行为通过 `IRecipeLogicMachine` 的工作钩子实现。


## 配方运行中的行为

`beforeWorking` 在消耗开始输入前执行，可以返回组件说明无法启动的原因。`onWorking` 在每刻条件与 I/O 成功后执行，返回 `false` 会中止当前配方。`afterWorking` 在配方到达完成阶段时执行。

??? example "机器工作时将上方泥土变成草方块"

    ```java
    public class Greenhouse extends WorkableElectricMultiblockMachine {
        public Greenhouse(BlockEntityCreationInfo info) {
            super(info);
        }

        @Override
        public boolean onWorking() {
            if (!super.onWorking()) {
                return false;
            }
            if (getLevel() instanceof ServerLevel level) {
                BlockPos above = getBlockPos().above();
                if (level.getBlockState(above).is(Blocks.DIRT)) {
                    level.setBlockAndUpdate(above, Blocks.GRASS_BLOCK.defaultBlockState());
                }
            }
            return true;
        }
    }
    ```

在机器注册入口使用 `Greenhouse::new`。涉及具体配方条件和超频、并行的行为可以复用各自的配方接口。 [配方执行](../../Development/Recipe-Logic/Recipe-Execution.md) · [自定义修改器](../Recipes/Custom-Recipe-Modifiers.md)


## 独立于配方的 Tick

机器可通过 `subscribeServerTick` 调度自身操作。多方块的启动和停止点分别是 `formStructure(String name)` 和 `invalidateStructure(String name)`。按对应的结构名管理订阅，结构失效或机器卸载时取消订阅。需要根据某个状态订阅和取消时，可以用 `ConditionalSubscriptionHandler` 管理生命周期。 [Tick 更新](../../Development/General-Topics/Tick-Updates.md)
