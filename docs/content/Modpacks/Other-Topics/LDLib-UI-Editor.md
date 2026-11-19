---
title: LDLib2 界面编辑器
---

# LDLib2 界面编辑器

GregTech Post Modern 使用 LDLib2 XML 定义可编辑的机器界面和配方类型界面。在游戏内使用 `/gtceu ui_editor` 打开编辑器，执行命令需要管理员权限。

## 从模板创建界面

打开 **Templates** 菜单，选择 **Machine** 或 **Recipe**，再选择实际要编辑的机器或配方类型。编辑器会用相应的模板创建 XML 项目，并根据所选目标设置默认保存路径。

在源码区直接编辑 XML，在预览区查看界面。用播放和停止按钮切换模拟状态。项目保存的是 XML 源码。

## 工作区和保存路径

编辑器的工作区是 `<游戏目录>/ldlib2/assets/`。目标 ID 为 `<namespace>:<path>` 时，使用以下路径。

- 机器界面使用 `<namespace>/ui/machine/<path>.xml`。
- 配方类型界面使用 `<namespace>/ui/recipe_type/<path>.xml`。

例如，装配线的配方类型 `gtpm:assembly_line` 对应 `gtpm/ui/recipe_type/assembly_line.xml`。将 XML 保存到工作区中的对应路径会使该目标的缓存失效。重新打开界面即可看到变更。工作区外的 XML 可以保存回原位置，将它移到对应目标路径后才会成为运行时覆盖文件。

## XML 绑定

- 用 UTF-8 无 BOM 保存格式正确的 LDLib2 XML。自定义文件解析或绑定失败时，对应界面会在日志中报告错误。
- 机器和配方模板的根元素需要固定像素宽度和高度。
- 配方进度至少需要一个 `id="progress"` 的 `gtpm-progress-bar` 或 `gtpm-dual-progress`。
- 物品、流体等输入和输出控件采用 `<capability>_<io>_<index>` 格式，例如 `item_in_0`、`fluid_out_0`。索引从 0 开始，元素类型要与相应能力匹配。
- 机器模板沿用配方绑定。单方块有电压等级的机器需要一个 `id="battery_slot"` 的 `gtpm-item-slot`，单方块发电机需要一个 `id="energy_container"` 的 `gtpm-progress-bar`。

## 迁移旧界面

从 LDLib1 的 `.mui` 或 `.rtui` 迁移时，先在 LDLib2 中为目标生成 XML 模板，再在模板上重建布局。保留模板的运行时绑定 ID。
