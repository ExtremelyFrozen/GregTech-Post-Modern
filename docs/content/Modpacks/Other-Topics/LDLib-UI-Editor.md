---
title: LDLib2 界面编辑器
---


# LDLib2 界面编辑器

GTPM 用 LDLib2 XML 定义可编辑的机器和配方类型布局。在游戏内由管理员玩家运行 `/gtceu ui_editor` 打开编辑器。


## 创建并应用模板

1. 从 **Templates** 菜单选择 **Machine** 或 **Recipe**。
2. 选中要编辑的机器或配方类型。编辑器生成目标的 XML 模板并设置默认保存路径。
3. 在源码区调整 XML，在预览区检查布局。用播放和停止按钮切换模拟状态。
4. 保存项目并重新打开目标界面。编辑器会在 XML 保存到对应工作区路径时使该目标的缓存失效。

工作区为 `<游戏目录>/ldlib2/assets/`。对于 `<namespace>:<path>`，保存路径如下。

| 模板 | 工作区路径 |
| --- | --- |
| Machine | `<namespace>/ui/machine/<path>.xml` |
| Recipe | `<namespace>/ui/recipe_type/<path>.xml` |

例如 `gtpm:assembly_line` 的配方类型使用 `gtpm/ui/recipe_type/assembly_line.xml`。工作区外的项目可以保存到原位置，再将 XML 放进工作区对应目标路径以应用。

要随资源包分发，将 XML 放在资源包的 `assets/<namespace>/ui/machine/<path>.xml` 或 `assets/<namespace>/ui/recipe_type/<path>.xml`。启用资源包后重新加载客户端资源并打开对应界面。


## XML 运行时绑定

使用 UTF-8 无 BOM 保存 XML。布局根元素指定固定的宽度和高度。保留与机器或配方类型关联的绑定 ID。

| 元素或 ID | 绑定用途 |
| --- | --- |
| `id="progress"` | 使用 `gtpm-progress-bar` 或 `gtpm-dual-progress` 绑定配方进度。 |
| `item_in_0`、`fluid_out_0` 等 | 通过 `<能力>_<方向>_<索引>` 对应物品、流体等槽位。索引从 `0` 开始，物品使用 `gtpm-item-slot`，流体使用 `gtpm-fluid-slot`。 |
| `id="battery_slot"` | 可编辑的单方块电压机器模板使用 `gtpm-item-slot` 绑定电池槽。 |
| `id="energy_container"` | 单方块发电机模板使用 `gtpm-progress-bar` 绑定储能。 |

控件名使用 `gtpm-` 前缀，由 LDLib2 元素注册表识别。配方类型模板也会用于对应的 JEI 和 EMI 页面。旧 `.mui` 或 `.rtui` 界面迁移时，可用当前模板重建布局并保留它生成的绑定 ID。自定义多方块的文本和页面可以通过附属模组提供。 [多方块界面开发](Modifying-Multiblock-Controller-UI.md)
