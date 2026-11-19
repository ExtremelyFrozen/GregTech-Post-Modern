package com.gregtechceu.gtceu.integration.emi.multipage;

import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.preview.PatternGenerationGuard;
import com.gregtechceu.gtceu.api.multiblock.preview.PatternGenerationGuard.Resolution;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;
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
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
public class MultiblockInfoEmiRecipe extends ModularUIEMIRecipe {

    private final MultiblockInfo info;
    private volatile @Nullable Resolution<IndexedIngredients> indexedIngredients;

    public MultiblockInfoEmiRecipe(MultiblockMachineDefinition definition) {
        super(recipe -> PatternPreviewWidget.createModularUI(((MultiblockInfoEmiRecipe) recipe).info));
        info = new MultiblockInfo(definition);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        var ui = getModularUI();
        ui.init(getDisplayWidth(), getDisplayHeight());
        ((PatternPreviewWidget) ui.ui.rootElement).reset();
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
                getDisplayWidth() - 26, 4)
                .recipeContext(this)
                .drawBack(false));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return MultiblockInfoEmiCategory.CATEGORY;
    }

    @Override
    public ResourceLocation getId() {
        return info.getDefinition().getId().withPrefix("/");
    }

    @Override
    public ObjectList<EmiStack> getOutputs() {
        return getIndexedIngredients().outputs();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        // EMI reads this list again when filtering uses after the bake.
        return getIndexedIngredients().inputs();
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return ObjectList.of();
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public int getDisplayWidth() {
        return Math.min(PatternPreviewWidget.WIDTH, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 24);
    }

    @Override
    public int getDisplayHeight() {
        return Math.min(PatternPreviewWidget.HEIGHT, Minecraft.getInstance().getWindow().getGuiScaledHeight() - 96);
    }

    private IndexedIngredients getIndexedIngredients() {
        var cached = indexedIngredients;
        if (cached != null && cached.generation() == StructurePatternRegistry.generation()) return cached.value();

        var resolved = PatternGenerationGuard.resolve(StructurePatternRegistry::generation, attempt -> {
            ObjectList<EmiIngredient> inputs = info.getPotentialInputs().stream()
                    .<EmiIngredient>map(key -> EmiStack.of(key.prototype()))
                    .collect(ObjectImmutableList.toList());
            var outputs = info.getOutputs().stream().map(EmiStack::of).collect(ObjectImmutableList.toList());
            return new IndexedIngredients(inputs, outputs);
        });
        indexedIngredients = resolved;
        return resolved.value();
    }

    private record IndexedIngredients(ObjectList<EmiIngredient> inputs, ObjectList<EmiStack> outputs) {}
}
