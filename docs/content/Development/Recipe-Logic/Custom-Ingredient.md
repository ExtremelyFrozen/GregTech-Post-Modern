---
title: 自定义配方成分与能力
---


# 自定义配方成分与能力

物品能力使用 NeoForge 的 `SizedIngredient`，流体能力使用 `SizedFluidIngredient`。需要匹配标签、物品组件、流体组件或电路档位时，可以使用各自的 Ingredient 类型。需要让机器处理新的资源种类时，注册一项 Recipe Capability。


## 为物品或流体添加匹配方式

实现 NeoForge 的 `ICustomIngredient` 或对应的流体成分接口。为类型注册 `MapCodec`，提供实际匹配规则及供 JEI、EMI 展示的代表值。将成分放入有数量的配方内容，匹配器处理单个候选，能力处理器检查总数量。

??? example "编程电路的配方内容"

    ```json
    {
      "content": {
        "ingredient": { "type": "gtpm:circuit", "configuration": 3 },
        "count": 1
      },
      "chance": 0
    }
    ```

    该条目放在 `inputs.item` 中。档位为 `3` 的电路参与配方匹配，`chance: 0` 让它作为保留的催化物。Java 构建器也可用 `circuitMeta(3)`。

原生物品组件可以使用 `gtpm:components`，填写 `items`、`components` 与可选的 `strict`。对于可经索引搜索的自定义类型，能力的映射成分应同时覆盖配方成分和机器内容的表示。 [电路成分源码](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/api/recipe/ingredient/IntCircuitIngredient.java) · [配方搜索](Recipe-Searching.md)


## 注册配方能力

1. 定义该资源在配方中的值类型，为 `IContentSerializer<T>` 提供 `codec()`、转换、默认值和类型。配方内容会通过该 Codec 用到数据加载与传输中。
2. 实现 `RecipeCapability<T>`，登记到 `GTRegistries.RECIPE_CAPABILITIES`。能力负责复制、缩放与处理对应内容，并提供配方查找所需的表示。
3. 通过 `IRecipeHandler<T>` 把机器中的容器暴露给能力。实现 `handleRecipeInner` 的模拟和执行行为，返回还需处理的内容。全部处理完返回空列表。
4. 将处理器用支持的 `IO` 加入机器的 `RecipeHandlerList`。多方块还要让对应部件通过结构谓词参加成型和配方处理。
5. 在 `GTRecipeBuilder` 上用 `input(capability, content)` 或 `output(capability, content)` 生成内容。构建器设置 `perTick(true)` 时写入每刻输入或输出。

物品与流体已有对应的查看器槽位。自定义能力需要展示时，在能力上提供它的界面元素与配方查看器数据转换。 [RecipeCapability 源码](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/api/capability/recipe/RecipeCapability.java) · [处理器契约](Recipe-Execution.md)
