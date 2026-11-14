package com.gregtechceu.gtceu.api.multiblock.error

import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternDiagnostic

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.level.block.Block

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists

/** A rule-owned explanation captured on the world thread. Extensions may supply their own display lines. */
class StructureIssue(val lines: ObjectList<Component>) : PatternError() {
	override fun getErrorInfo(): Component = lines[0]

	enum class Count(val key: String, val located: Boolean) {
		MINIMUM("too_few", false),
		MAXIMUM("too_many", true),
		LAYER_MINIMUM("too_few_layer", false),
		LAYER_MAXIMUM("too_many_layer", true),
	}

	companion object {
		fun mismatch(failure: PatternDiagnostic, expected: Component?): StructureIssue = located(
			text("mismatch"),
			failure,
			expected?.let { text("expected", it.copy().withStyle(ChatFormatting.GREEN)) },
		)
		fun forbidden(failure: PatternDiagnostic): StructureIssue = located(text("forbidden"), failure)
		fun unloaded(failure: PatternDiagnostic): StructureIssue = located(text("unloaded"), failure, showFound = false)
		fun shared(failure: PatternDiagnostic): StructureIssue = located(text("shared"), failure, showFound = false)
		fun condition(failure: PatternDiagnostic, message: Component): StructureIssue = located(message, failure)
		fun mixed(failure: PatternDiagnostic, material: Component): StructureIssue = located(text("mixed", material.copy().withStyle(ChatFormatting.YELLOW)), failure)

		fun count(failure: PatternDiagnostic?, kind: Count, subject: Component?, limit: Int, actual: Int): StructureIssue = located(
			text(kind.key, subject?.copy()?.withStyle(ChatFormatting.YELLOW) ?: Component.empty()),
			if (kind.located) failure else null,
			if (kind == Count.MINIMUM || kind == Count.LAYER_MINIMUM) text("at_least", number(limit, ChatFormatting.GREEN), number(actual, ChatFormatting.RED)) else text("at_most", number(limit, ChatFormatting.GREEN)),
		)

		fun size(): StructureIssue = located(text("size"), null, text("size.combination"))

		fun size(parameter: String, minimum: Int, maximum: Int, actual: Int): StructureIssue {
			val subject = Component.literal(parameter).withStyle(ChatFormatting.YELLOW)
			val detail = if (actual < 0) text("size.unmeasured", subject, number(maximum, ChatFormatting.GREEN)) else text("size.range", subject, number(minimum, ChatFormatting.GREEN), number(maximum, ChatFormatting.GREEN), number(actual, ChatFormatting.RED))
			return located(text("size"), null, detail)
		}

		fun blocks(blocks: Iterable<Block>, exclude: Block? = null): Component? {
			val unique = ObjectLinkedOpenHashSet<Block>()
			for (block in blocks) if (block != exclude) unique.add(block)
			return when (unique.size) {
				0 -> null
				1 -> unique.first().name
				else -> text("variants", unique.first().name, unique.size)
			}
		}

		fun alternatives(labels: Iterable<Component?>): Component? {
			val unique = ObjectLinkedOpenHashSet<Component>()
			for (label in labels) if (label != null) unique.add(label)
			if (unique.isEmpty()) return null
			val result = Component.empty()
			var shown = 0
			for (label in unique) {
				if (shown == 3) break
				if (shown++ > 0) result.append(" | ")
				result.append(label)
			}
			return if (unique.size > 3) text("more", result, unique.size) else result
		}

		fun text(key: String, vararg args: Any): MutableComponent = Component.translatable("gtpm.structure.issue.$key", *args)

		private fun number(value: Int, color: ChatFormatting): Component = Component.literal(value.toString()).withStyle(color)

		private fun located(title: Component, failure: PatternDiagnostic?, detail: Component? = null, showFound: Boolean = true): StructureIssue {
			val lines = ObjectArrayList<Component>(4)
			lines.add(title.copy().withStyle(ChatFormatting.RED))
			if (failure != null) lines.add(text("position", number(failure.world.x, ChatFormatting.AQUA), number(failure.world.y, ChatFormatting.AQUA), number(failure.world.z, ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY))
			if (detail != null) lines.add(detail.copy().withStyle(ChatFormatting.GRAY))
			if (showFound) failure?.actual?.let { lines.add(text("found", it.block.name.copy().withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.GRAY)) }
			return StructureIssue(ObjectLists.unmodifiable(lines))
		}
	}
}
