package com.gregtechceu.gtceu.client.renderer.block;

import com.gregtechceu.gtceu.client.util.ModelEventHelper;
import com.gregtechceu.gtceu.common.item.LampBlockItem;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class LampItemRendererHelper {

    private LampItemRendererHelper() {}

    public static final IClientItemExtensions CLIENT_EXTENSIONS = new IClientItemExtensions() {

        @Override
        public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return LampItemRenderer.getInstance();
        }
    };

    public static void registerModelBakeListener(LampBlockItem item) {
        ModelResourceLocation modelLoc = ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item));
        ModelEventHelper.registerBakeEventListener(false,
                (resourceLocation, bakedModel, rootModel, modelBakery) -> {
                    if (!modelLoc.equals(resourceLocation)) {
                        return bakedModel;
                    }
                    return new BakedModelWrapper<>(bakedModel) {

                        @Override
                        public boolean isCustomRenderer() {
                            return true;
                        }

                        @Override
                        public BakedModel applyTransform(ItemDisplayContext displayContext, PoseStack poseStack,
                                                         boolean leftHanded) {
                            super.applyTransform(displayContext, poseStack, leftHanded);
                            return this;
                        }
                    };
                });
    }
}
