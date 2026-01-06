package wily.legacy.client.screen.minigame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.OptionsScreen;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;
import wily.legacy.mixin.base.client.AbstractWidgetAccessor;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base class for Minigame More Options screens.
 *
 * Features:
 * - Automatic panel height calculation
 * - Tooltip box on the right side
 * - Support for disabled widget tooltips
 * - Custom options state management
 */
public abstract class MinigameMoreOptionsScreen extends PanelVListScreen {

    // ==================== UI CONFIGURATION ====================

    protected static final int DEFAULT_PANEL_WIDTH = 245;
    protected static final int MAX_PANEL_HEIGHT = 256;
    protected static final int TOOLTIP_BOX_WIDTH = 185;
    protected static final int SLIDER_HEIGHT = 19;
    protected static final int SECTION_SPACING = 10;

    // ==================== INSTANCE FIELDS ====================

    protected final Panel tooltipBox;
    protected final List<AbstractWidget> customOptionsWidgets = new ArrayList<>();

    // ==================== CONSTRUCTOR ====================

    public MinigameMoreOptionsScreen(Screen parent, int panelWidth, int panelHeight, Component title) {
        super(parent, panelWidth, panelHeight, title);
        this.tooltipBox = Panel.tooltipBoxOf(panel, TOOLTIP_BOX_WIDTH);
    }

    // ==================== ABSTRACT METHODS ====================

    /**
     * Subclasses should implement this to build their specific options UI.
     */
    protected abstract void buildOptions();

    /**
     * Subclasses should implement this to update custom options state based on game type.
     */
    protected abstract void updateCustomOptionsState();

    // ==================== INITIALIZATION ====================

    @Override
    public void renderableVListInit() {
        if (LegacyRenderUtil.hasTooltipBoxes(accessor)) tooltipBox.init();
        super.renderableVListInit();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Calculate panel height based on number of elements.
     *
     * @param headerCount Number of section headers (9px each)
     * @param sliderCount Number of sliders (19px each)
     * @param checkboxCount Number of checkboxes (12px each)
     * @param emptyLineCount Number of empty lines/spacing
     * @return Calculated height, capped at MAX_PANEL_HEIGHT
     */
    protected static int calculatePanelHeight(int headerCount, int sliderCount, int checkboxCount, int emptyLineCount) {
        int totalHeight = 0;
        int separation = 3; // layoutSeparation in HD mode

        // Headers (text height is ~9px)
        totalHeight += headerCount * (9 + separation);

        // Sliders
        totalHeight += sliderCount * (SLIDER_HEIGHT + separation);

        // Checkboxes (height is ~12px in HD mode)
        totalHeight += checkboxCount * (12 + separation);

        // Empty lines
        totalHeight += emptyLineCount * (SECTION_SPACING + separation);

        // Add padding for top/bottom margins (10px top + 10px bottom from renderableVListInit)
        totalHeight += 20;

        // Cap at maximum height
        return Math.min(totalHeight, MAX_PANEL_HEIGHT);
    }

    // ==================== EVENT HANDLERS ====================

    @Override
    public void onClose() {
        // Notify parent screen to refresh UI
        if (parent instanceof CreateMinigameScreen cms) {
            cms.updateFromSharedOptions();
        }
        super.onClose();
    }

    // ==================== RENDERING ====================

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, false);
        if (LegacyRenderUtil.hasTooltipBoxes(accessor)) {
            Optional<GuiEventListener> listener;
            List<FormattedCharSequence> tooltipBoxLabel = null;

            if (getFocused() instanceof AbstractWidgetAccessor widget && widget.getTooltip() != null && widget.getTooltip().get() != null)
                tooltipBoxLabel = widget.getTooltip().get().toCharSequence(minecraft);
            else if ((listener = getChildAt(i, j)).isPresent() && listener.get() instanceof AbstractWidgetAccessor widget && widget.getTooltip() != null && widget.getTooltip().get() != null)
                tooltipBoxLabel = widget.getTooltip().get().toCharSequence(minecraft);

            // If no tooltip from focused/child (e.g. widget disabled), explicitly check customOptionsWidgets by mouse hover
            if (tooltipBoxLabel == null) {
                for (AbstractWidget w : customOptionsWidgets) {
                    if (w == null) continue;
                    try {
                        if (w instanceof AbstractWidgetAccessor acc && acc.getTooltip() != null && acc.getTooltip().get() != null) {
                            // Use absolute position check instead of widget.isMouseOver so disabled
                            // widgets still show tooltips when hovered.
                            try {
                                if (LegacyRenderUtil.isMouseOver(i, j, w.getX(), w.getY(), w.getWidth(), w.getHeight())) {
                                    tooltipBoxLabel = acc.getTooltip().get().toCharSequence(minecraft);
                                    break;
                                }
                            } catch (Throwable ignored) {
                                // Fallback to widget.isMouseOver if position access fails for any reason
                                if (w.isMouseOver(i, j)) {
                                    tooltipBoxLabel = acc.getTooltip().get().toCharSequence(minecraft);
                                    break;
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }

            tooltipBox.render(guiGraphics, i, j, f);
            if (tooltipBoxLabel != null) {
                // Render tooltip directly without scrolling
                int yOffset = panel.y + 13;
                for (FormattedCharSequence line : tooltipBoxLabel) {
                    guiGraphics.drawString(font, line, panel.x + panel.width + 3, yOffset, 0xFFFFFFFF);
                    yOffset += 12;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        if (LegacyRenderUtil.hasTooltipBoxes(accessor))
            guiGraphics.deferredTooltip = null;
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        OptionsScreen.setupSelectorControlTooltips(renderer, this);
    }
}

