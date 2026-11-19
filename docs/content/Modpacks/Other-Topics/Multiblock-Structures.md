---
title: 多方块结构文件
---


# 多方块结构文件

多方块以机器 ID 和结构名标识。默认结构名为 `main`，同一台机器也可以注册多个命名结构用于成型、预览和搭建。附属模组通过机器注册器挂接结构，在模组 JAR 内提供 JSON 或 Zstandard 压缩的二进制结构。


## 文件位置与加载

以下路径均相对于模组资源根目录。

| 机器与结构 | JSON | 二进制 |
| --- | --- | --- |
| `example:heater` 的 `main` | `multiblock/example/json/heater.json` | `multiblock/example/binary/heater.bin.zst` |
| `example:heater` 的 `extension` | `multiblock/example/json/heater/extension.json` | `multiblock/example/binary/heater/extension.bin.zst` |

最后一个路径段用作命名结构的名称。机器 ID 的路径自身含有斜杠时，默认结构使用 `<机器路径>/main.json` 或 `<机器路径>/main.bin.zst`。JSON 的 `machine` 字段填写完整机器 ID，包含命名空间。

加载器扫描已加载模组的 `multiblock/`，把结构文件同步到 `<游戏目录>/gtpm/multiblock-cache/`。修改模组资源中的源文件后再重新加载。每个结构键对应一份资源定义，重复的键会在加载时报告。注册的 Java 或 Kotlin 定义在该键没有资源时提供结构。结构数据可以替换几何和匹配规则，注册代码中的成型条件继续参与检查。

在开发环境的集成服务端，管理员可以运行 `/gtceu structure_cache reload`。追加 `json` 或 `binary` 可以重载相应格式，继续追加机器 ID 可以重载该机器的对应格式和所有命名结构，例如 `/gtceu structure_cache reload json gtpm:distillation_tower`。


## JSON 结构

`body` 描述结构的几何。`axes` 将局部的深度、高度、宽度分别映射到一组相互垂直的朝向。固定布局的 `layers` 依次列出深度切片、切片内各行、行内各字符。`~` 表示主机位置，空格表示该位置接受任意方块，其他字符在 `predicates` 中声明。`origin.token` 默认使用 `controller`，也可通过 `origin.offset` 按深度、高度、宽度提供锚点偏移。

??? example "两层固定结构"

    以下内容对应 `example:heater` 已注册的 `main` 结构。`X` 使用耐火砖，`#` 要求空气。

    ```json
    {
      "machine": "example:heater",
      "axes": { "depth": "back", "height": "up", "width": "right" },
      "orientation": { "allowMirror": false, "allowExtendedFacing": false },
      "origin": { "token": "controller" },
      "predicates": {
        "X": { "type": "gtpm:block", "ids": "gtpm:firebricks" },
        "#": { "type": "gtpm:air" }
      },
      "body": {
        "type": "fixed",
        "layers": [
          ["XXX", "X~X", "XXX"],
          ["XXX", "X#X", "XXX"]
        ]
      }
    }
    ```

| 节点 | 字段与用途 |
| --- | --- |
| `fixed` | `layers` 保存固定字符布局。 |
| `sequence` | `axis` 和 `children` 将子节点沿同一轴依次排列。 |
| `repeat` | `id`、`axis`、`direction`、`min`、`max` 和 `body` 定义可重复片段。可用 `count.parameter` 或 `countExpression` 从参数计算次数。 |
| `choice` | `id` 和 `alternatives` 定义一组可选布局。每个选项包含 `id` 和 `node`，预览里的样式选项来自这里。 |
| `fragment` | 通过 `id` 和 `bindings` 引用已命名的片段，并为其传入参数。 |
| `placed` | 通过 `offset` 将 `body` 移到指定坐标。 |
| `assembly` | 用 `root` 指定根片段。`slots` 用 `hostPort` 对接模块的 `in` 端口，支持 `one`、`optional`、`chain` 和 `orderedChoice` 槽位。链式模块使用 `out` 连接下一个模块。 |

`fragments` 可声明局部谓词、参数、锚点和端口。整数范围参数也可由 `measurements` 从世界实测，例如超净间沿轴查找边界以得到实际尺寸。`sizeConstraints` 用于比较坐标范围与尺寸表达式，例如限制一个轴的长度相对于另一轴的比例。

蒸馏塔展示垂直轴映射与可重复层，超净间和木炭堆展示多轴结构。 [蒸馏塔](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/resources/multiblock/gtpm/json/distillation_tower.json) · [超净间](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/resources/multiblock/gtpm/json/cleanroom.json) · [木炭堆](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/resources/multiblock/gtpm/json/charcoal_pile_igniter.json)


## 谓词与数量

| JSON 类型 | 常用字段 |
| --- | --- |
| `gtpm:block` | `ids` 指定一个方块 ID 或一组方块 ID。 |
| `gtpm:air`、`gtpm:any` | 分别要求空气或接受任意方块。 |
| `gtpm:part` | `abilities` 指定舱室能力，例如 `input_energy` 或 `import_fluids`。 |
| `gtpm:concatenated` | `predicates` 按定义组合可用候选，例如在同一位置接受外壳或指定舱室。 |
| `gtpm:restricted` | `predicate` 指定被限制的谓词。`minCount`、`maxCount` 和按层的 `minCountByLayer`、`maxCountByLayer` 设置数量，`previewCount` 设置预览优先展示的数量。 |

用 `facts` 标记谓词产生的事实，再用顶层 `constraints` 的 `count` 约束统计这些事实。数量条件填写 `fact`、`min`、`max` 和 `scope`。作用域可覆盖全结构（`all`）、片段（`fragment:<id>`）、节点（`node:<id>`）或重复段（`repeat:<id>`）。终端与 JEI、EMI 的预览规划会使用同一套候选和数量规则。

附属模组可以声明新的谓词类型和材料来源。 [多方块开发接口](../../Development/General-Topics/Multiblock-Structures.md)
