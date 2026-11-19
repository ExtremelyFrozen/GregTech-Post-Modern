---
title: "Icon Sets（图标集）"
---


# Icon Sets（图标集）

!!! note "上游示例"
    本页示例面向 GregTech CEu Modern。GTPM 1.21.1 的数据包和附属模组入口见[整合包制作](../index.md#upstream-kubejs)。

Material 系统使用 Icon Sets 来决定生成方块和物品的贴图。


## 可用 Icon Sets

默认可用以下 Icon Sets：

- `GTMaterialIconSet.BRIGHT`
- `GTMaterialIconSet.CERTUS`
- `GTMaterialIconSet.DIAMOND`
- `GTMaterialIconSet.DULL`
- `GTMaterialIconSet.EMERALD`
- `GTMaterialIconSet.FINE`
- `GTMaterialIconSet.FLINT`
- `GTMaterialIconSet.FLUID`
- `GTMaterialIconSet.GAS`
- `GTMaterialIconSet.GEM_HORIZONTAL`
- `GTMaterialIconSet.GEM_VERTICAL`
- `GTMaterialIconSet.GLASS`
- `GTMaterialIconSet.LAPIS`
- `GTMaterialIconSet.LIGNITE`
- `GTMaterialIconSet.MAGNETIC`
- `GTMaterialIconSet.METALLIC`
- `GTMaterialIconSet.NETHERSTAR`
- `GTMaterialIconSet.OPAL`
- `GTMaterialIconSet.QUARTZ`
- `GTMaterialIconSet.ROUGH`
- `GTMaterialIconSet.RUBY`
- `GTMaterialIconSet.SAND`
- `GTMaterialIconSet.SHINY`
- `GTMaterialIconSet.WOOD`


## 自定义 Icon Sets

也可以使用 `gtceu:material_icon_set` 事件指定自定义材质图标集。

```js title="custom_iconsets.js"
GTCEuStartupEvents.registry('gtceu:material_icon_set', event => {
    event.create('starry')
        .parent('shiny')
})
```
