package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.multiblock.preview.PreviewVisibility;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Side-safe multiblock preview container shared by XEI and a controller-bound terminal menu.
 *
 * <p>
 * The server constructs this same semantic container but never loads a client world or scene. On the logical
 * client it attaches one window-owned scene element; profile controls and execute actions remain outside this
 * rendering-only container.
 * </p>
 */
@NullMarked
public final class MultiblockPreviewPanel extends UIElement {

    @Nullable
    private static Function<ClientContext, UIElement> clientFactory;
    @Getter
    private final PreviewVisibility state;
    private Consumer<StructurePreview> clientUpdater = ignored -> {};
    private BiConsumer<Integer, Integer> clientResize = (width, height) -> {};

    /**
     * Creates a preview container that is safe to construct on both menu sides.
     *
     * @param width            full panel width
     * @param height           full panel height
     * @param preview          resolved view
     * @param showAvailability whether the panel includes terminal-only persistent material allocation details
     */
    public MultiblockPreviewPanel(int width, int height, StructurePreview preview,
                                  boolean showAvailability) {
        state = new PreviewVisibility(preview);
        layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.width(width);
            layout.height(height);
        });
        getStyle().backgroundTexture(GuiTextures.BACKGROUND_INVERSE);
        setOverflowVisible(false);
        if (LDLib2.isRemote()) {
            @Nullable
            Function<ClientContext, UIElement> factory = clientFactory;
            if (factory == null) {
                throw new IllegalStateException("The multiblock preview client factory was not registered");
            }
            addChild(factory.apply(new ClientContext(width, height, state, showAvailability,
                    updater -> clientUpdater = updater, resize -> clientResize = resize)));
        }
    }

    public void resize(int width, int height) {
        getLayout().width(width).height(height);
        clientResize.accept(width, height);
    }

    /**
     * Replaces terminal-authoritative or locally re-resolved content without recreating the panel.
     */
    public void setPreview(StructurePreview preview) {
        if (!state.getPreview().definitionId().equals(preview.definitionId())) {
            throw new IllegalArgumentException("A preview panel cannot switch machine definitions");
        }
        state.setPreview(preview);
        clientUpdater.accept(preview);
    }

    /**
     * Registers the physical-client scene factory without putting client classes in common bytecode.
     */
    public static void registerClientFactory(Function<ClientContext, UIElement> factory) {
        if (clientFactory != null) {
            throw new IllegalStateException("The multiblock preview client factory is already registered");
        }
        clientFactory = factory;
    }

    /**
     * Client construction values passed through a common JDK function instead of a custom single-use interface.
     */
    public record ClientContext(int width, int height, PreviewVisibility state, boolean showAvailability,
                                Consumer<Consumer<StructurePreview>> installUpdater,
                                Consumer<BiConsumer<Integer, Integer>> installResize) {}
}
