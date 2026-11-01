package com.gregtechceu.gtceu.integration.jei.multipage;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;

import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.integration.xei.jei.JEIUIEvents;
import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIJEIWidget;
import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIRecipeCategory;
import com.lowdragmc.lowdraglib2.integration.xei.jei.handler.JEIRecipeWidgetHandler;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import it.unimi.dsi.fastutil.objects.ObjectList;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiblockInfoCategory extends ModularUIRecipeCategory<MultiblockInfoWrapper> {

    public final static RecipeType<MultiblockInfoWrapper> RECIPE_TYPE = new RecipeType<>(
            GTCEu.id("multiblock_info"),
            MultiblockInfoWrapper.class);
    private final int width;
    private final int height;
    private final IDrawable icon;

    public MultiblockInfoCategory(IJeiHelpers helpers) {
        super(MultiblockInfoWrapper::createModularUI);
        this.width = PatternPreviewWidget.WIDTH;
        this.height = PatternPreviewWidget.HEIGHT;
        this.icon = helpers.getGuiHelper().createDrawableItemStack(GTMultiMachines.ELECTRIC_BLAST_FURNACE.asStack());
    }

    public static void registerRecipes(IRecipeRegistration registry) {
        registry.addRecipes(RECIPE_TYPE, GTRegistries.MACHINES.stream()
                .filter(MultiblockMachineDefinition.class::isInstance)
                .map(MultiblockMachineDefinition.class::cast)
                .filter(MultiblockMachineDefinition::isRenderXEIPreview)
                .map(MultiblockInfoWrapper::new)
                .toList());
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(@NotNull MultiblockInfoWrapper recipe) {
        return recipe.definition.getId();
    }

    @Override
    @NotNull
    public RecipeType<MultiblockInfoWrapper> getRecipeType() {
        return RECIPE_TYPE;
    }

    @NotNull
    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtpm.jei.multiblock_info");
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MultiblockInfoWrapper recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT)
                .addItemStacks(PatternPreviewWidget.getOutputs(recipe.definition))
                .setPosition(PatternPreviewWidget.WIDTH - 22, 4);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MultiblockInfoWrapper recipe, IFocusGroup focuses) {
        var ui = getUIForRecipe(recipe);
        var widget = new ModularUIJEIWidget(ui) {

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
            public Vector2f getWorldMouseNormal(float mouseX, float mouseY) {
                var direction = localToScreen.transformDirection(new Vector3f(mouseX, mouseY, 0));
                return new Vector2f(direction.x, direction.y);
            }

            @Override
            public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
                graphics.flush();
                localToScreen.set(graphics.pose().last().pose());
                var mouse = getWorldMouse((float) mouseX, (float) mouseY);
                ui.getWidget().render(graphics, Math.round(mouse.x), Math.round(mouse.y),
                        Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
                ui.getWidget().mouseMoved(mouse.x, mouse.y);
            }
        };
        builder.addWidget(widget);
        builder.addGuiEventListener(widget);
        var slots = new JEIRecipeWidgetHandler(widget::getLocalToWorld);
        var event = UIEvent.create(JEIUIEvents.RECIPE_WIDGET);
        event.target = ui.ui.rootElement;
        event.customData = slots;
        UIEventDispatcher.dispatchAllChildren(event);
        for (var slot : slots.slots) {
            builder.addSlottedWidget(new ISlottedRecipeWidget() {

                @Override
                public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
                    return Optional.ofNullable(slot.getRecipeSlots(mouseX, mouseY));
                }

                @Override
                public ScreenPosition getPosition() {
                    return ModularUIJEIWidget.ZERO;
                }
            }, ObjectList.of());
        }
    }
}
