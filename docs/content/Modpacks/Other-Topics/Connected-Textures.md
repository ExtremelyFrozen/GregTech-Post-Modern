---
title: 创建连接纹理
---

**Connected textures** 顾名思义，是会与相邻 Block 连接的纹理。

CTM renderer 会通过从 5 张可用的 Block 纹理中组装 4 个象限来绘制 Block 面。
普通的 `texture.png` 是 Block 的“未连接”纹理，会在 CTM 被禁用或该 Block
没有可连接对象时使用。
`texture.png` 包含外角象限，`texture_ctm.png` 包含连接部分。
```
┌─────────────────┐ ┌────────────────────────────────┐
│ texture.png     │ │ texture_ctm.png                │
│ ╔══════╤══════╗ │ │  ──────┼────── ║ ─────┼───── ║ │
│ ║      │      ║ │ │ │      │      │║      │      ║ │
│ ║ 4/4  │ 4/5  ║ │ │ │ 0/0  │ 0/1  │║ 0/2  │ 0/3  ║ │
│ ╟──────┼──────╢ │ │ ┼──────┼──────┼╟──────┼──────╢ │
│ ║      │      ║ │ │ │      │      │║      │      ║ │
│ ║ 5/4  │ 5/5  ║ │ │ │ 1/0  │ 1/1  │║ 1/2  │ 1/3  ║ │
│ ╚══════╧══════╝ │ │  ──────┼────── ║ ─────┼───── ║ │
└─────────────────┘ │ ═══════╤═══════╝ ─────┼───── ╚ │
                    │ │      │      ││      │      │ │
                    │ │ 2/0  │ 2/1  ││ 2/2  │ 2/3  │ │
                    │ ┼──────┼──────┼┼──────┼──────┼ │
                    │ │      │      ││      │      │ │
                    │ │ 3/0  │ 3/1  ││ 3/2  │ 3/3  │ │
                    │ ═══════╧═══════╗ ─────┼───── ╔ │
                    └────────────────────────────────┘
```

例如，组合 4/4、2/1、5/4 和 3/1 这些区域，就能生成一个向右连接的纹理！
```
╔══════╤═══════
║      │      │
║ 4/4  │ 2/1  │
╟──────┼──────┼
║      │      │
║ 5/4  │ 3/1  │
╚══════╧═══════
```
组合 0/2、2/3、5/4 和 3/1 这些区域，就能生成一个 L 形纹理（向右和向上连接）：
```
║ ─────┼───── ╚
║      │      │
║ 0/2  │ 2/3  │
╟──────┼──────┼
║      │      │
║ 5/4  │ 3/1  │
╚══════╧═══════
```


??? example "MCMeta 文件示例"
    `assets/mypack/textures/block/texture.png.mcmeta` 为同目录的 `texture.png` 指定连接纹理。
    ```json title="assets/mypack/textures/block/texture.png.mcmeta"
    {
        "gtpm": {
            "connection_texture": "mypack:block/texture_ctm"
        }
    }
    ```
    塑料混凝土的资源展示了内置纹理的目录结构和 `gtpm` 元数据。[基础纹理](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/resources/assets/gtpm/textures/block/casings/cleanroom/plascrete.png) · [连接纹理](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/resources/assets/gtpm/textures/block/casings/cleanroom/plascrete_ctm.png) · [元数据](https://github.com/ExtremelyFrozen/GregTech-Post-Modern/blob/1.21/src/main/resources/assets/gtpm/textures/block/casings/cleanroom/plascrete.png.mcmeta)
