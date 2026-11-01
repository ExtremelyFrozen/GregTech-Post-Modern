package com.gregtechceu.gtceu.api.multiblock.pattern.layout

import it.unimi.dsi.fastutil.objects.ObjectList

/** Branch IDs are local to this stable declaration path. */
@JvmRecord
data class PatternChoiceDescriptor(val id: String, val path: String, val alternatives: ObjectList<String>, val priority: ObjectList<String>)
