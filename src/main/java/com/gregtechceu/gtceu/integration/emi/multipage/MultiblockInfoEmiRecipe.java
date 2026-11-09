package com.gregtechceu.gtceu.integration.emi.multipage;

import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.integration.xei.multiblock.MultiblockInfo;

import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.integration.xei.emi.EMIUIEvents;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIWidget;
import com.lowdragmc.lowdraglib2.integration.xei.emi.handler.EMIRecipeWidgetHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.runtime.EmiReloadManager;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

public class MultiblockInfoEmiRecipe extends ModularUIEMIRecipe {

    private final MultiblockInfo info;

    public MultiblockInfoEmiRecipe(MultiblockMachineDefinition definition) {
        super(recipe -> PatternPreviewWidget.createModularUI(((MultiblockInfoEmiRecipe) recipe).info));
        info = new MultiblockInfo(definition);
    }

    @Override
    public void addWidgets(@NotNull WidgetHolder widgets) {
        var ui = getModularUI();
        ui.init(getDisplayWidth(), getDisplayHeight());
        var widget = new ModularUIEMIWidget(ui) {

            private final Matrix4f localToScreen = new Matrix4f();

            @Override
            public Matrix4f getLocalToWorld() {
                return localToScreen.invert(new Matrix4f());
            }

            @Override
            public Vector2f getWorldMouse(float mouseX, float mouseY) {
                var point = localToScreen.transformPosition(new Vector3f(mouseX, mouseY, 0));
                return new Vector2f(point.x, point.y);
            }

            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                graphics.flush();
                localToScreen.set(graphics.pose().last().pose());
                // EMI supplies recipe-local coordinates; LDLib hit testing expects screen coordinates.
                var mouse = getWorldMouse(mouseX, mouseY);
                ui.getWidget().render(graphics, Math.round(mouse.x), Math.round(mouse.y), partialTick);
                ui.getWidget().mouseMoved(mouse.x, mouse.y);
            }

            @Override
            public boolean mouseClicked(int mouseX, int mouseY, int button) {
                var mouse = getWorldMouse(mouseX, mouseY);
                return ui.getWidget().mouseClicked(mouse.x, mouse.y, button);
            }
        };
        var slots = new EMIRecipeWidgetHandler(widget::getLocalToWorld);
        var event = UIEvent.create(EMIUIEvents.RECIPE_WIDGET);
        event.target = ui.ui.rootElement;
        event.customData = slots;
        UIEventDispatcher.dispatchAllChildren(event);
        for (var slot : slots.slots) {
            if (slot instanceof SlotWidget output) output.recipeContext(this);
            widgets.add(slot);
        }
        widgets.add(widget);
        widgets.add(new SlotWidget(EmiIngredient.of(getOutputs()),
                getDisplayWidth() - 22, 4)
                .recipeContext(this)
                .drawBack(false));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return MultiblockInfoEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return info.getDefinition().getId().withPrefix("/");
    }

    @Override
    public @NotNull ObjectList<EmiStack> getOutputs() {
        return info.getOutputs().stream().map(EmiStack::of)
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public @NotNull List<EmiIngredient> getInputs() {
        // Both native indexing passes need the candidate superset. EMI's pruneUses then queries the live inputs.
        if (!EmiReloadManager.isLoaded() || EmiRecipes.activeWorker != null) {
            return info.getPotentialInputs().stream().<EmiIngredient>map(key -> EmiStack.of(key.prototype()))
                    .collect(ObjectImmutableList.toList());
        }
        return info.getMaterials().stream().map(material -> EmiIngredient.of(material.candidates().stream()
                .map(stack -> EmiStack.of(stack).setAmount(material.required())).toList()))
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public @NotNull List<EmiIngredient> getCatalysts() {
        return ObjectList.of();
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public int getDisplayWidth() {
        return Math.min(PatternPreviewWidget.WIDTH, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 64);
    }

    @Override
    public int getDisplayHeight() {
        return Math.min(PatternPreviewWidget.HEIGHT, Minecraft.getInstance().getWindow().getGuiScaledHeight() - 96);
    }
}
