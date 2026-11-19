package com.gregtechceu.gtceu.common.item.tool.behavior

import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.UITemplate.setBounds
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement
import com.gregtechceu.gtceu.api.gui.factory.HeldItemUIHolder
import com.gregtechceu.gtceu.api.item.datacomponents.AoESymmetrical
import com.gregtechceu.gtceu.api.item.tool.ToolHelper.getAoEDefinition
import com.gregtechceu.gtceu.api.item.tool.behavior.IToolUIBehavior
import com.gregtechceu.gtceu.api.item.tool.behavior.ToolBehaviorType
import com.gregtechceu.gtceu.api.sync_system.FieldCodecs
import com.gregtechceu.gtceu.common.data.GTDataComponents
import com.gregtechceu.gtceu.common.data.GTToolBehaviors

import com.lowdragmc.lowdraglib2.gui.sync.rpc.rpcEvent
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf

open class AOEConfigUIBehavior : IToolUIBehavior<AOEConfigUIBehavior> {

	override fun openUI(player: Player, hand: InteractionHand): Boolean = player.isShiftKeyDown && !player.getItemInHand(hand).getOrDefault(GTDataComponents.AOE, AoESymmetrical.ZERO).isZero()

	override fun canCreateUI(player: Player, holder: HeldItemUIHolder): Boolean = ItemStack.isSameItem(holder.held, holder.openedStack) && !getAoEDefinition(holder.held).isZero()

	override fun isUIStillValid(player: Player, holder: HeldItemUIHolder): Boolean = canCreateUI(player, holder)

	override fun createUI(player: Player, holder: HeldItemUIHolder): UI {
		val definition = getAoEDefinition(holder.held).toMutable()
		val root = UIElement()
		val configureAOE = root.rpcEvent<Int, Int, Int> { column, row, layer ->
			val held = holder.held
			val current = getAoEDefinition(held)
			held.set(
				GTDataComponents.AOE,
				AoESymmetrical(
					current.maxColumn(),
					current.maxRow(),
					current.maxLayer(),
					column,
					row,
					layer,
				),
			)
		}
		setBounds(root, 0, 0, 120, 80)
		root.style { it.backgroundTexture(GuiTextures.BACKGROUND) }

		root.addChild(createLabel("item.gtpm.tool.aoe.columns", 6, 10, 38, 10, true))
		root.addChild(createLabel("item.gtpm.tool.aoe.rows", 49, 10, 28, 10, true))
		root.addChild(createLabel("item.gtpm.tool.aoe.layers", 79, 10, 34, 10, true))

		val current = definition.toImmutable()
		val columnValue = createLabel(columnText(current), 23, 65, 18, 10, false)
		val rowValue = createLabel(rowText(current), 58, 65, 18, 10, false)
		val layerValue = createLabel(layerText(current), 93, 65, 18, 10, false)

		fun updateDefinition(next: AoESymmetrical) {
			if (holder.player.level().isClientSide()) {
				configureAOE.send(next.column(), next.row(), next.layer())
			}
			columnValue.setText(columnText(next), false)
			rowValue.setText(rowText(next), false)
			layerValue.setText(layerText(next), false)
		}

		root.addChild(createAOEButton("+", 15, 24) { updateDefinition(definition.increaseColumn().toImmutable()) })
		root.addChild(createAOEButton("-", 15, 44) { updateDefinition(definition.decreaseColumn().toImmutable()) })
		root.addChild(createAOEButton("+", 50, 24) { updateDefinition(definition.increaseRow().toImmutable()) })
		root.addChild(createAOEButton("-", 50, 44) { updateDefinition(definition.decreaseRow().toImmutable()) })
		root.addChild(createAOEButton("+", 85, 24) { updateDefinition(definition.increaseLayer().toImmutable()) })
		root.addChild(createAOEButton("-", 85, 44) { updateDefinition(definition.decreaseLayer().toImmutable()) })

		root.addChildren(columnValue, rowValue, layerValue)
		return UI.of(root)
	}

	override fun getType(): ToolBehaviorType<AOEConfigUIBehavior> = GTToolBehaviors.AOE_CONFIG_UI

	private fun createLabel(text: String, x: Int, y: Int, width: Int, height: Int, translate: Boolean): GTLabelElement {
		val label = GTLabelElement(x, y, width, height, text, translate)
		label.textStyle {
			it.textColor(0x404040).textShadow(false)
				.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER)
		}
		return label
	}

	private fun createAOEButton(text: String, x: Int, y: Int, action: () -> Unit): GTButtonElement {
		val button = GTButtonElement(x, y, 20, 20, GuiTextures.BUTTON) { action() }
		button.setText(text, false)
		button.textStyle {
			it.textColor(0x404040).textShadow(false)
				.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER)
		}
		return button
	}

	private fun columnText(definition: AoESymmetrical): String = (1 + 2 * definition.column()).toString()

	private fun rowText(definition: AoESymmetrical): String = (1 + 2 * definition.row()).toString()

	private fun layerText(definition: AoESymmetrical): String = (1 + definition.layer()).toString()

	companion object {
		@JvmField
		val INSTANCE = AOEConfigUIBehavior()

		@JvmField
		val CODEC: Codec<AOEConfigUIBehavior> = Codec.unit(INSTANCE)

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, AOEConfigUIBehavior> = StreamCodec.unit(INSTANCE)

		init {
			FieldCodecs.register(AOEConfigUIBehavior::class.java, CODEC)
			FieldCodecs.registerStream(AOEConfigUIBehavior::class.java, STREAM_CODEC.cast<RegistryFriendlyByteBuf>())
		}
	}
}
