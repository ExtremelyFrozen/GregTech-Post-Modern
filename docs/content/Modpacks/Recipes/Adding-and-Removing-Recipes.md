---
title: 添加、替换与移除配方
---


# 添加、替换与移除机器配方

GTPM 1.21.1 通过 Minecraft 数据包加载机器配方。配方文件位于 `data/<命名空间>/recipe/<路径>.json`，文件路径决定配方 ID，顶层 `type` 指定机器配方类型。GTPM 内置机器类型使用 `gtpm` 命名空间。


## 添加配方 { #add-recipes }

把配方放入已启用的数据包的 `data` 目录。下例添加 `example:macerator/cobblestone_to_sand`，粉碎机每刻消耗 8 EU，运行 100 刻后将一个圆石变成一个沙子。

??? example "data/example/recipe/macerator/cobblestone_to_sand.json"

    ```json
    {
      "type": "gtpm:macerator",
      "duration": 100,
      "tier": 1,
      "inputs": {
        "item": [
          { "content": { "ingredient": { "item": "minecraft:cobblestone" }, "count": 1 } }
        ]
      },
      "outputs": {
        "item": [
          { "content": { "ingredient": { "item": "minecraft:sand" }, "count": 1 } }
        ]
      },
      "tickInputs": {
        "eu": [
          { "content": 8 }
        ]
      }
    }
    ```

运行 `/reload` 重新加载服务端配方。客户端收到完整一轮配方同步后，JEI 和 EMI 会更新配方展示。


## 输入、输出与配方条件

| 字段 | 用途 |
| --- | --- |
| `inputs`、`outputs` | 开始时消耗、完成时输出的内容。 |
| `tickInputs`、`tickOutputs` | 每刻消耗、每刻输出的内容。发电配方将产出的 EU 写入 `tickOutputs.eu`。 |
| `duration` | 普通配方的处理刻数。研究站的算力配方在这里保存需要累计的 CWU。 |
| `tier` | 配方电压等级编号，例如 `1` 为 LV。 |
| `recipeConditions` | 运行时条件列表，例如维度或洁净室。 |
| `category`、`groupColor`、`data` | 配方分类、处理器组颜色和随配方保存的类型化数据组件。 |

内容按能力分组。`item` 使用带数量的物品成分，`fluid` 使用带用量的流体成分，`eu` 使用整数，`cwu` 使用 CWU 数值。每个条目在 `content` 中写入实际内容，概率条目可加 `chance`、`maxChance` 和 `tierChanceBoost`。每组可通过对应的 `inputChanceLogics`、`outputChanceLogics`、`tickInputChanceLogics` 或 `tickOutputChanceLogics` 指定概率逻辑。

GTPM 配方条件由机器运行时判断。数据包加载条件由 NeoForge 在加载配方时判断。 [配方条件](Recipe-Conditions.md)


## 替换与移除配方 { #replace-or-remove-recipes }

1. 确认原配方 ID，包括冒号前的命名空间和完整路径。
2. 在优先级更高的数据包中创建同一 ID 的 JSON。`gtpm:<路径>` 对应 `data/gtpm/recipe/<路径>.json`。
3. 替换时写入完整的新配方。移除时给覆盖文件添加恒为假的 NeoForge 加载条件。
4. 执行 `/reload`。查看器里的代理配方来自原始配方类型，例如机器显示的原版熔炼配方。修改或移除这类配方时使用其原始配方 ID。

??? example "让同名覆盖文件跳过加载"

    ```json
    {
      "type": "gtpm:macerator",
      "neoforge:conditions": [
        { "type": "neoforge:false" }
      ]
    }
    ```

配方类型也可以根据已有配方生成派生配方。控制这类配方时，编辑驱动生成的原始配方。附属模组可通过 `IGTAddon.removeRecipes(Consumer<ResourceLocation>)` 在配方解析前移除相应的资源 ID。


## 在附属模组中生成配方

在 `IGTAddon.addRecipes(RecipeOutput)` 中，通过配方类型的 `recipeBuilder` 构造定义并调用 `save`。生成器会在 ID 路径前加上配方类型路径。用此入口编写的配方会在数据重载时随模组内置配方生成。

??? example "用 Java 生成同一条粉碎机配方"

    ```java
    GTRecipeTypes.MACERATOR_RECIPES
            .recipeBuilder(ResourceLocation.fromNamespaceAndPath("example", "cobblestone_to_sand"))
            .inputItems(Items.COBBLESTONE)
            .outputItems(Items.SAND)
            .duration(100)
            .EUt(8)
            .tier(GTValues.LV)
            .save(output);
    ```

`EUt(long)` 写入每刻总 EU。双参数版本使用 `voltage * amperage` 计算总量，等级由电压推导。`inputEU` 和 `outputEU` 写入一次性总量。需要指定不同等级时，在设置 EU 后调用 `tier(int)`。

装配线研究可通过 `scannerResearch` 生成扫描仪配方，也可以用 `stationResearch` 对应研究站配方。研究站的构建器可通过 `CWUt(minimum, total)` 设定每刻最低算力和总算力。使用已有研究数据时，`researchWithoutRecipe` 将配方与指定研究 ID 关联。 [算力与研究](../../Gameplay/Electricity/Computation.md) · [配方定义和运行逻辑](../../Development/Recipe-Logic/Recipe-Logic.md)
