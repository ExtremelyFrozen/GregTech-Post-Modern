package com.gregtechceu.gtceu.api.gui.fancy;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolderContext;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.StructureCheckConfigurator;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * LDLib2 Fancy shell that owns local navigation, title bar, side tabs, configurator panel, and tooltip panel.
 */
public class FancyMachineUIWidget extends UIElement {

    private static final int MIN_PAGE_WIDTH = 172;
    private static final int MIN_PAGE_HEIGHT = 86;
    private static final int PLAYER_INVENTORY_WIDTH = 162;
    private static final int PLAYER_INVENTORY_ELEMENT_HEIGHT = 76;
    private static final int PLAYER_INVENTORY_FRAME_HEIGHT = 82;
    private static final int DEFAULT_BORDER = 4;

    private final IFancyUIProvider mainPage;
    /**
     * Player whose inventory was captured when this Fancy screen opened.
     */
    @Getter
    private final Player openingPlayer;
    @Getter
    private final MachineUIHolder holder;
    private final Map<MetaMachine, MachineUIHolder> machineHolders = new Reference2ObjectOpenHashMap<>();
    private final UIElement pageContainer;
    private final TitleBarWidget titleBar;
    @Getter
    private final TabsWidget sideTabsElement;
    @Getter
    private final ConfiguratorPanel configuratorPanel;
    @Getter
    private final TooltipsPanel tooltipsPanel;
    private final PageSwitcher pageSwitcher;
    /**
     * Holds side pages that belong to this opened UI context, such as pages that capture its validated holder.
     */
    private final List<IFancyUIProvider> contextualSubTabs;
    private final Map<IFancyUIProvider, UIElement> pageCache = new IdentityHashMap<>();
    private final Deque<NavigationEntry> previousPages = new ArrayDeque<>();

    @Nullable
    private final UIElement playerInventory;
    private int border = DEFAULT_BORDER;
    private IFancyUIProvider currentPage;
    private IFancyUIProvider currentHomePage;
    private List<IFancyUIProvider> allPages = List.of();

    private record NavigationEntry(IFancyUIProvider page, IFancyUIProvider homePage,
                                   Runnable onNavigation) {}

    public FancyMachineUIWidget(IFancyUIProvider mainPage, Inventory inventory, MachineUIHolder holder,
                                int width, int height) {
        this(mainPage, inventory, holder, width, height, List.of());
    }

    /**
     * Creates a Fancy shell with side pages scoped to this opened holder rather than shared machine state.
     */
    public FancyMachineUIWidget(IFancyUIProvider mainPage, Inventory inventory, MachineUIHolder holder,
                                int width, int height,
                                List<? extends IFancyUIProvider> contextualSubTabs) {
        this.mainPage = mainPage;
        this.openingPlayer = inventory.player;
        this.holder = holder;
        this.contextualSubTabs = List.copyOf(contextualSubTabs);
        UITemplate.setBounds(this, 0, 0, width, height);
        UITemplate.setBackgroundTexture(this, GuiTextures.BACKGROUND);
        style(style -> style.overflowVisible(true));

        this.pageContainer = UITemplate.setBounds(new UIElement(), 0, 0, width, height);
        addChild(pageContainer);

        if (mainPage.hasPlayerInventory()) {
            this.playerInventory = UITemplate.bindPlayerInventory(inventory, GuiTextures.SLOT, 0, 0, true);
            addChild(playerInventory);
        } else {
            this.playerInventory = null;
        }

        this.titleBar = new TitleBarWidget(width, event -> navigateBack(), event -> openPageSwitcher());
        this.sideTabsElement = new TabsWidget(this::navigate, inventory.player, holder, -20, 0, 24, height, true);
        this.tooltipsPanel = new TooltipsPanel(width + 2, 2);
        this.configuratorPanel = new ConfiguratorPanel(inventory.player, holder, -26, height);
        this.pageSwitcher = new PageSwitcher(this::switchPage);
        addChildren(titleBar, sideTabsElement, tooltipsPanel, configuratorPanel);

        initializePages();
    }

    /**
     * Returns the machine holder captured when this LDLib2 Fancy screen was opened.
     */
    public MachineUIHolder getHolder(MetaMachine machine) {
        return machine == holder.getMachine() ? holder :
                machineHolders.computeIfAbsent(machine, key -> {
                    if (openingPlayer.level().isClientSide()) {
                        UIEventListener sync = event -> key.sendServerSyncChanges();
                        addEventListener(UIEvents.TICK, sync);
                        addEventListener(UIEvents.REMOVED, sync);
                    }
                    return new MachineUIHolderContext(key);
                });
    }

    public FancyMachineUIWidget setBorder(int border) {
        this.border = border;
        setupFancyUI(currentPage, currentPage.hasPlayerInventory());
        return this;
    }

    protected void navigate(IFancyUIProvider newPage) {
        navigate(newPage, currentHomePage);
    }

    protected void navigate(IFancyUIProvider nextPage, IFancyUIProvider nextHomePage) {
        if (nextPage != mainPage) {
            if (!previousPages.isEmpty() && previousPages.peek().page == nextPage) {
                previousPages.pop();
            } else {
                previousPages.push(new NavigationEntry(currentPage, currentHomePage, () -> {}));
            }
        } else {
            previousPages.clear();
        }
        performNavigation(nextPage, nextHomePage);
    }

    protected void navigateBack() {
        NavigationEntry navigationEntry = previousPages.pop();
        performNavigation(navigationEntry.page, navigationEntry.homePage);
        navigationEntry.onNavigation.run();
    }

    protected void performNavigation(IFancyUIProvider nextPage, IFancyUIProvider nextHomePage) {
        if (currentHomePage != nextHomePage) {
            setupSideTabs(nextHomePage);
        }

        currentPage = nextPage;
        currentHomePage = nextHomePage;

        if (currentPage != currentHomePage) {
            setupFancyUI(currentHomePage);
        }
        setupFancyUI(nextPage, nextPage.hasPlayerInventory());
    }

    protected void openPageSwitcher() {
        pageSwitcher.setPageList(allPages, currentHomePage);
        if (currentPage != currentHomePage && !previousPages.isEmpty()) {
            previousPages.pop();
        }

        sideTabsElement.setVisible(false);
        sideTabsElement.setActive(false);

        previousPages.push(new NavigationEntry(currentHomePage, currentHomePage, () -> {
            sideTabsElement.setVisible(true);
            sideTabsElement.setActive(true);
        }));

        currentPage = pageSwitcher;
        currentHomePage = pageSwitcher;
        setupFancyUI(pageSwitcher);
    }

    protected void switchPage(IFancyUIProvider nextHomePage) {
        currentHomePage = mainPage;
        currentPage = mainPage;
        previousPages.clear();
        sideTabsElement.setVisible(true);
        sideTabsElement.setActive(true);
        setupSideTabs(currentHomePage);
        navigate(nextHomePage, nextHomePage);
    }

    protected void setupFancyUI(IFancyUIProvider fancyUI) {
        setupFancyUI(fancyUI, fancyUI.hasPlayerInventory());
    }

    protected void setupFancyUI(IFancyUIProvider fancyUI, boolean showInventory) {
        clearUI();
        sideTabsElement.selectTab(fancyUI);
        titleBar.updateState(fancyUI, !previousPages.isEmpty(),
                allPages.size() > 1 && currentPage != pageSwitcher);

        UIElement page = getOrCreatePage(fancyUI);
        int pageWidth = fancyUI.getPageWidth();
        int pageHeight = fancyUI.getPageHeight();
        int contentWidth = Math.max(MIN_PAGE_WIDTH, pageWidth + border * 2);
        int contentHeight = Math.max(MIN_PAGE_HEIGHT, pageHeight + border * 2);
        int rootHeight = contentHeight + (showInventory && playerInventory != null ? PLAYER_INVENTORY_FRAME_HEIGHT : 0);

        // The machine screen owns root positioning; page rebuilds only update the shell dimensions.
        getLayout().width(contentWidth).height(rootHeight);
        UITemplate.setBounds(pageContainer, 0, 0, contentWidth, contentHeight);
        UITemplate.setBackgroundTexture(pageContainer, GuiTextures.BACKGROUND);
        UITemplate.setBounds(sideTabsElement, -20, 0, 24, contentHeight);
        tooltipsPanel.moveTo(contentWidth + 2, 2);
        titleBar.resize(contentWidth);

        setupInventoryPosition(showInventory, contentWidth, contentHeight);

        for (UIElement cachedPage : pageCache.values()) {
            cachedPage.setVisible(cachedPage == page);
            cachedPage.setActive(cachedPage == page);
        }
        UITemplate.setBounds(page, (contentWidth - pageWidth) / 2, (contentHeight - pageHeight) / 2,
                pageWidth, pageHeight);

        configuratorPanel.setHolder(fancyUI instanceof MetaMachine machine ? getHolder(machine) : holder);
        fancyUI.attachConfigurators(configuratorPanel);
        if (fancyUI instanceof MultiblockControllerMachine controller) {
            configuratorPanel.attachConfigurators(new StructureCheckConfigurator(controller));
        }
        configuratorPanel.moveTo(-26, rootHeight - configuratorPanel.getPanelContentHeight() - border);
        fancyUI.attachTooltips(tooltipsPanel);

        if (openingPlayer.level().isClientSide) {
            ModularUI modularUI = getModularUI();
            if (modularUI != null && modularUI.getScreen() instanceof ModularUIContainerScreen screen) {
                modularUI.init(screen.width, screen.height);
                screen.init();
            }
        }
    }

    protected void clearUI() {
        for (UIElement page : pageCache.values()) {
            page.setVisible(false);
            page.setActive(false);
        }
        configuratorPanel.clear();
        tooltipsPanel.clear();
    }

    protected void initializePages() {
        allPages = Stream.concat(Stream.of(mainPage), mainPage.getSubTabs().stream()).toList();
        for (IFancyUIProvider page : allPages) {
            UIElement createdPage = getOrCreatePage(page);
            createdPage.setVisible(false);
            createdPage.setActive(false);
        }
        performNavigation(mainPage, mainPage);
    }

    protected UIElement getOrCreatePage(IFancyUIProvider fancyUI) {
        return pageCache.computeIfAbsent(fancyUI, key -> {
            UIElement createdPage = key.createUIWidget(this);
            pageContainer.addChild(createdPage);
            return createdPage;
        });
    }

    protected void setupSideTabs(IFancyUIProvider homePage) {
        sideTabsElement.setHolder(homePage instanceof MetaMachine machine ? getHolder(machine) : holder);
        sideTabsElement.setMainTab(homePage);
        sideTabsElement.clearSubTabs();
        homePage.attachSideTabs(sideTabsElement);
        if (homePage == mainPage) {
            contextualSubTabs.forEach(sideTabsElement::attachSubTab);
        }
    }

    private void setupInventoryPosition(boolean showInventory, int contentWidth, int contentHeight) {
        if (playerInventory == null) {
            return;
        }
        UITemplate.setBounds(playerInventory, (contentWidth - PLAYER_INVENTORY_WIDTH) / 2, contentHeight,
                PLAYER_INVENTORY_WIDTH, PLAYER_INVENTORY_ELEMENT_HEIGHT);
        playerInventory.setVisible(showInventory);
        playerInventory.setActive(showInventory);
    }
}
