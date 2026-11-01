package com.gregtechceu.gtceu.api.multiblock.pattern.layout

import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists

/** A resolved, immutable layout shared by matching, preview and construction. */
@JvmRecord
data class PatternLayout @JvmOverloads constructor(val cells: ObjectList<PatternLayoutCell>, val repeats: ObjectList<PatternRepeatInstance> = ObjectLists.emptyList(), val choices: ObjectList<PatternChoiceInstance> = ObjectLists.emptyList())
