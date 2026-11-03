package com.gregtechceu.gtceu.integration.jei.multipage;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;
import com.gregtechceu.gtceu.api.multiblock.preview.PatternGenerationGuard;
import com.gregtechceu.gtceu.api.multiblock.preview.PatternGenerationGuard.Resolution;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;
import com.gregtechceu.gtceu.integration.xei.multiblock.MultiblockInfo;

import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIJEIWidget;
import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIRecipeCategory;
import com.lowdragmc.lowdraglib2.integration.xei.jei.handler.JEIRecipeSlotHandler;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiblockInfoCategory extends ModularUIRecipeCategory<MultiblockInfo>
                                    implements ISimpleRecipeManagerPlugin<MultiblockInfo> {

    public final static RecipeType<MultiblockInfo> RECIPE_TYPE = new RecipeType<>(
            GTCEu.id("multiblock_info"),
            MultiblockInfo.class);
    private final int width;
    private final int height;
    private final IDrawable icon;
    private final ObjectList<MultiblockInfo> recipes;
    private final Object2ObjectMap<MaterialKey, ObjectList<MultiblockInfo>> outputs = new Object2ObjectLinkedOpenHashMap<>();
    private @Nullable Resolution<Object2ObjectMap<MaterialKey, ObjectList<MultiblockInfo>>> inputs;

    public MultiblockInfoCategory(IJeiHelpers helpers) {
        super(PatternPreviewWidget::createModularUI);
        this.width = PatternPreviewWidget.WIDTH;
        this.height = PatternPreviewWidget.HEIGHT;
        this.icon = helpers.getGuiHelper().createDrawableItemStack(GTMultiMachines.ELECTRIC_BLAST_FURNACE.asStack());
        recipes = GTRegistries.MACHINES.stream()
                .filter(MultiblockMachineDefinition.class::isInstance)
                .map(MultiblockMachineDefinition.class::cast)
                .filter(MultiblockMachineDefinition::isRenderXEIPreview)
                .map(MultiblockInfo::new)
                .collect(ObjectImmutableList.toList());
        for (var recipe : recipes) {
            for (ItemStack stack : recipe.getOutputs()) {
                outputs.computeIfAbsent(MaterialKey.of(stack), ignored -> new ObjectArrayList<>()).add(recipe);
            }
        }
        outputs.replaceAll((key, values) -> new ObjectImmutableList<>(values));
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(@NotNull MultiblockInfo recipe) {
        return recipe.getDefinition().getId();
    }

    @Override
    @NotNull
    public RecipeType<MultiblockInfo> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, MultiblockInfo recipe, IFocusGroup focuses) {
        super.setRecipe(builder, recipe, focuses);
        builder.addSlot(RecipeIngredientRole.OUTPUT)
                .addItemStacks(recipe.getOutputs())
                .setPosition(PatternPreviewWidget.WIDTH - 22, 4);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MultiblockInfo recipe, IFocusGroup focuses) {
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
        for (var binding : JEIRecipeSlotHandler.collectBindings(ui)) {
            builder.getRecipeSlots().findSlotByName(binding.name()).ifPresent(slot -> {
                var updater = binding.slotUpdater();
                if (updater != null) updater.bind(slot);
                builder.addSlottedWidget(new ISlottedRecipeWidget() {

                    @Override
                    public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
                        var mouse = widget.getWorldMouse((float) mouseX, (float) mouseY);
                        return binding.isInteractive() && binding.element().isMouseOverElement(mouse.x, mouse.y) ?
                                Optional.of(new RecipeSlotUnderMouse(slot, 0, 0)) : Optional.empty();
                    }

                    @Override
                    public ScreenPosition getPosition() {
                        return ModularUIJEIWidget.ZERO;
                    }
                }, ObjectList.of(slot));
            });
        }
    }

    private ObjectList<MultiblockInfo> inputRecipes(ItemStack stack) {
        var indexed = inputs;
        if (indexed == null || indexed.generation() != StructurePatternRegistry.generation()) {
            indexed = PatternGenerationGuard.resolve(StructurePatternRegistry::generation, attempt -> {
                Object2ObjectMap<MaterialKey, ObjectList<MultiblockInfo>> index = new Object2ObjectLinkedOpenHashMap<>();
                for (var recipe : recipes) {
                    for (var key : recipe.getPotentialInputs()) {
                        index.computeIfAbsent(key, ignored -> new ObjectArrayList<>()).add(recipe);
                    }
                }
                index.replaceAll((key, values) -> new ObjectImmutableList<>(values));
                return index;
            });
            inputs = indexed;
        }
        return indexed.value().getOrDefault(MaterialKey.of(stack), ObjectList.of());
    }

    @Override
    public boolean isHandledInput(ITypedIngredient<?> input) {
        return input.getIngredient(VanillaTypes.ITEM_STACK).map(stack -> !inputRecipes(stack).isEmpty()).orElse(false);
    }

    @Override
    public boolean isHandledOutput(ITypedIngredient<?> output) {
        return !getRecipesForOutput(output).isEmpty();
    }

    @Override
    public ObjectList<MultiblockInfo> getRecipesForInput(ITypedIngredient<?> input) {
        return input.getIngredient(VanillaTypes.ITEM_STACK).map(this::recipesUsing).orElse(ObjectList.of());
    }

    private ObjectList<MultiblockInfo> recipesUsing(ItemStack stack) {
        return inputRecipes(stack).stream().filter(recipe -> recipe.getMaterials().stream()
                .anyMatch(material -> material.candidates().stream()
                        .anyMatch(candidate -> ItemStack.isSameItemSameComponents(candidate, stack))))
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public ObjectList<MultiblockInfo> getRecipesForOutput(ITypedIngredient<?> output) {
        return output.getIngredient(VanillaTypes.ITEM_STACK)
                .map(stack -> outputs.getOrDefault(MaterialKey.of(stack), ObjectList.of())).orElse(ObjectList.of());
    }

    @Override
    public ObjectList<MultiblockInfo> getAllRecipes() {
        return recipes;
    }
}
