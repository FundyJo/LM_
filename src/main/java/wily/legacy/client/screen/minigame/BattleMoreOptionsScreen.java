package wily.legacy.client.screen.minigame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.*;
import wily.legacy.mixin.base.client.AbstractWidgetAccessor;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * More Options screen for Battle Minigame customization.
 *
 * Features:
 * - Scrollable options panel with game settings
 * - Tooltip box on the right side
 * - Two sections: "Game Options" and "Custom Options"
 */
public class BattleMoreOptionsScreen extends MinigameMoreOptionsScreen {

    // ==================== INSTANCE FIELDS ====================

    // Scrollable renderer for tooltip box
    protected ScrollableRenderer scrollableRenderer = new ScrollableRenderer(new LegacyScrollRenderer());

    // Shared options state
    private final BattleOptions options;

    // ==================== CONSTRUCTOR ====================

    public BattleMoreOptionsScreen(Screen parent) {
        this(parent, new BattleOptions());
    }

    public BattleMoreOptionsScreen(Screen parent, BattleOptions options) {
        super(parent, DEFAULT_PANEL_WIDTH, MAX_PANEL_HEIGHT, Component.translatable("createWorld.tab.more.title"));
        this.options = options != null ? options : new BattleOptions();
        buildOptions();
    }

    // ==================== UI INITIALIZATION ====================

    @Override
    protected void buildOptions() {
        // ==================== GAME OPTIONS SECTION ====================
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0, 9, r -> ((guiGraphics, i, j, f) ->
            guiGraphics.drawString(font, Component.literal("Game Options"), r.x + 1, r.y + 2, CommonColor.INVENTORY_GRAY_TEXT.get(), false))));

        // Game Type Slider
        renderableVList.addRenderable(new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Game Type: " + b.getObjectValue()),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.game_type.tooltip")),
            options.gameType,
            () -> List.of("Casual", "Competitive", "Custom"),
            b -> {
                options.gameType = b.getObjectValue();
                updateCustomOptionsState();
            },
            () -> options.gameType
        ));

        // Select Maps Button
        renderableVList.addRenderable(Button.builder(Component.literal("Select Maps"),
            button -> onSelectMaps())
            .tooltip(new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.select_maps.tooltip")))
            .build());

        // Round Length Slider
        renderableVList.addRenderable(new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Round Length: " + b.getObjectValue()),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.round_length.tooltip")),
            options.roundLength,
            () -> List.of("Short", "Normal", "Long"),
            b -> options.roundLength = b.getObjectValue(),
            () -> options.roundLength
        ));

        // Game Size Slider
        renderableVList.addRenderable(new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Game Size: " + b.getObjectValue() + " Players"),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.game_size.tooltip")),
            options.gameSize,
            () -> List.of(8, 16),
            b -> options.gameSize = b.getObjectValue(),
            () -> options.gameSize
        ));

        // Central Spawn Checkbox
        renderableVList.addRenderable(new TickBox(0, 0, options.centralSpawn,
            b -> Component.literal("Central Spawn"),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.central_spawn.tooltip")),
            b -> options.centralSpawn = b.selected
        ));

        // Empty line
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0, SECTION_SPACING, r -> ((guiGraphics, i, j, f) -> {})));

        // ==================== CUSTOM OPTIONS SECTION ====================
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0, 9, r -> ((guiGraphics, i, j, f) ->
            guiGraphics.drawString(font, Component.literal("Custom Options"), r.x + 1, r.y + 2, CommonColor.INVENTORY_GRAY_TEXT.get(), false))));

        // Lives per Round Slider
        List<String> livesOptions = new ArrayList<>();
        livesOptions.add("Infinite");
        for (int i = 1; i <= 10; i++) livesOptions.add(String.valueOf(i));
        LegacySliderButton<String> livesSlider = new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Lives per Round: " + b.getObjectValue()),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.lives_per_round.tooltip")),
            options.livesPerRound,
            () -> livesOptions,
            b -> options.livesPerRound = b.getObjectValue(),
            () -> options.livesPerRound
        );
        customOptionsWidgets.add(livesSlider);
        renderableVList.addRenderable(livesSlider);

        // Spectator Mode Slider
        LegacySliderButton<String> spectatorSlider = new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Spectator Mode: " + b.getObjectValue()),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.spectator_mode.tooltip")),
            options.spectatorMode,
            () -> List.of("Invisible", "Bat", "Parrot", "Vex", "Head"),
            b -> options.spectatorMode = b.getObjectValue(),
            () -> options.spectatorMode
        );
        customOptionsWidgets.add(spectatorSlider);
        renderableVList.addRenderable(spectatorSlider);

        // Allow All Skins Checkbox
        TickBox allowAllSkinsBox = new TickBox(0, 0, options.allowAllSkins,
            b -> Component.literal("Allow All Skins"),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.allow_all_skins.tooltip")),
            b -> options.allowAllSkins = b.selected
        );
        customOptionsWidgets.add(allowAllSkinsBox);
        renderableVList.addRenderable(allowAllSkinsBox);

        // Empty line
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0, SECTION_SPACING, r -> ((guiGraphics, i, j, f) -> {})));

        // Item Set Slider
        LegacySliderButton<String> itemSetSlider = new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Item Set: " + b.getObjectValue()),
            // Tooltip supplier now maps the selected value to a specific translation key
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable(getItemSetTooltipKey(b.getObjectValue()))),
            options.itemSet,
            () -> List.of("Normal", "No Armor", "High Power", "Decayed", "Food Central", "Random"),
            b -> options.itemSet = b.getObjectValue(),
            () -> options.itemSet
        );
        customOptionsWidgets.add(itemSetSlider);
        renderableVList.addRenderable(itemSetSlider);

        // Hunger Settings Slider
        LegacySliderButton<String> hungerSlider = new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Hunger Settings: " + b.getObjectValue()),
            // Tooltip supplier now maps the selected value to a specific translation key
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable(getHungerSettingsTooltipKey(b.getObjectValue()))),
            options.hungerSettings,
            () -> List.of("Normal", "Anti Camper", "Fast Healing", "No Starving",
                    "Always Healing", "No Hunger", "Lead Feet", "Always Hungry"),
            b -> options.hungerSettings = b.getObjectValue(),
            () -> options.hungerSettings
        );
        customOptionsWidgets.add(hungerSlider);
        renderableVList.addRenderable(hungerSlider);

        // Round Count Slider
        LegacySliderButton<Integer> roundCountSlider = new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Round Count: " + b.getObjectValue()),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.round_count.tooltip")),
            options.roundCount,
            () -> List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
            b -> options.roundCount = b.getObjectValue(),
            () -> options.roundCount
        );
        customOptionsWidgets.add(roundCountSlider);
        renderableVList.addRenderable(roundCountSlider);

        // Map Size Slider
        LegacySliderButton<String> mapSizeSlider = new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Map Size: " + b.getObjectValue()),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.map_size.tooltip")),
            options.mapSize,
            () -> List.of("Auto", "Small", "Large", "Large+"),
            b -> options.mapSize = b.getObjectValue(),
            () -> options.mapSize
        );
        customOptionsWidgets.add(mapSizeSlider);
        renderableVList.addRenderable(mapSizeSlider);

        // Additional Checkboxes (were missing): Natural Regeneration, Small Inventory, Take Everything, Chest Refill
        TickBox naturalRegenBox = new TickBox(0, 0, options.naturalRegeneration,
            b -> Component.literal("Natural Regeneration"),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.natural_regeneration.tooltip")),
            b -> options.naturalRegeneration = b.selected
        );
        customOptionsWidgets.add(naturalRegenBox);
        renderableVList.addRenderable(naturalRegenBox);

        TickBox smallInventoryBox = new TickBox(0, 0, options.smallInventory,
            b -> Component.literal("Small Inventory"),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.small_inventory.tooltip")),
            b -> options.smallInventory = b.selected
        );
        customOptionsWidgets.add(smallInventoryBox);
        renderableVList.addRenderable(smallInventoryBox);

        TickBox takeEverythingBox = new TickBox(0, 0, options.takeEverything,
            b -> Component.literal("Take Everything"),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.take_everything.tooltip")),
            b -> options.takeEverything = b.selected
        );
        customOptionsWidgets.add(takeEverythingBox);
        renderableVList.addRenderable(takeEverythingBox);

        TickBox chestRefillBox = new TickBox(0, 0, options.chestRefill,
            b -> Component.literal("Chest Refill"),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.battle.more_options.chest_refill.tooltip")),
            b -> options.chestRefill = b.selected
        );
        customOptionsWidgets.add(chestRefillBox);
        renderableVList.addRenderable(chestRefillBox);

        // Initialize custom options state
        updateCustomOptionsState();
    }

    /**
     * Updates the active state of all custom options widgets based on the selected game type.
     * Custom options are only enabled when game type is set to "Custom".
     */
    @Override
    protected void updateCustomOptionsState() {
        boolean isCustomMode = "Custom".equals(options.gameType);
        for (AbstractWidget widget : customOptionsWidgets) {
            widget.active = isCustomMode;
        }
    }

    // ==================== HELPER METHODS ====================

    // Helper methods to map the displayed value to the translation key for tooltips
    private static String getItemSetTooltipKey(String value) {
        if (value == null) return "legacy.menu.battle.more_options.item_set.normal.tooltip";
        return switch (value) {
            case "Normal" -> "legacy.menu.battle.more_options.item_set.normal.tooltip";
            case "No Armor" -> "legacy.menu.battle.more_options.item_set.no_armor.tooltip";
            case "High Power" -> "legacy.menu.battle.more_options.item_set.high_power.tooltip";
            case "Decayed" -> "legacy.menu.battle.more_options.item_set.decayed.tooltip";
            case "Food Central" -> "legacy.menu.battle.more_options.item_set.food_central.tooltip";
            case "Random" -> "legacy.menu.battle.more_options.item_set.random.tooltip";
            default -> "legacy.menu.battle.more_options.item_set.normal.tooltip";
        };
    }

    private static String getHungerSettingsTooltipKey(String value) {
        if (value == null) return "legacy.menu.battle.more_options.hunger_settings.normal.tooltip";
        return switch (value) {
            case "Normal" -> "legacy.menu.battle.more_options.hunger_settings.normal.tooltip";
            case "Anti Camper" -> "legacy.menu.battle.more_options.hunger_settings.anti_camper.tooltip";
            case "Fast Healing" -> "legacy.menu.battle.more_options.hunger_settings.fast_healing.tooltip";
            case "No Starving" -> "legacy.menu.battle.more_options.hunger_settings.no_starving.tooltip";
            case "Always Healing" -> "legacy.menu.battle.more_options.hunger_settings.always_healing.tooltip";
            case "No Hunger" -> "legacy.menu.battle.more_options.hunger_settings.no_hunger.tooltip";
            case "Lead Feet" -> "legacy.menu.battle.more_options.hunger_settings.lead_feet.tooltip";
            case "Always Hungry" -> "legacy.menu.battle.more_options.hunger_settings.always_hungry.tooltip";
            default -> "legacy.menu.battle.more_options.hunger_settings.normal.tooltip";
        };
    }

    // ==================== EVENT HANDLERS ====================

    private void onSelectMaps() {
        // TODO: Open map selection screen
    }

    // ==================== RENDERING OVERRIDE FOR SCROLLABLE TOOLTIPS ====================

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (tooltipBox.isHovered(d, e) && scrollableRenderer.mouseScrolled(g)) return true;
        return super.mouseScrolled(d, e, f, g);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        // Use parent's rendering logic first
        super.renderDefaultBackground(guiGraphics, i, j, f);

        // Override tooltip rendering to add scrolling support
        if (LegacyRenderUtil.hasTooltipBoxes(accessor)) {
            List<FormattedCharSequence> tooltipBoxLabel = getTooltipBoxLabel(i, j);

            if (tooltipBoxLabel == null)
                scrollableRenderer.resetScrolled();
            else
                scrollableRenderer.scrolled.max = Math.max(0, tooltipBoxLabel.size() - (tooltipBox.getHeight() - 44) / 12);

            if (tooltipBoxLabel != null) {
                // Clear parent's rendered tooltip
                guiGraphics.fill(panel.x + panel.width + 3, panel.y + 13,
                               panel.x + panel.width + 3 + tooltipBox.width - 10,
                               panel.y + 13 + (tooltipBox.getHeight() - 44), 0);

                // Render with scrolling
                List<FormattedCharSequence> finalTooltipBoxLabel = tooltipBoxLabel;
                scrollableRenderer.render(guiGraphics, panel.x + panel.width + 3, panel.y + 13,
                    tooltipBox.width - 10, tooltipBox.getHeight() - 44, () ->
                        finalTooltipBoxLabel.forEach(c -> guiGraphics.drawString(font, c,
                            panel.x + panel.width + 3,
                            panel.y + 13 + finalTooltipBoxLabel.indexOf(c) * 12, 0xFFFFFFFF)));
            }
        }
    }

    private List<FormattedCharSequence> getTooltipBoxLabel(int i, int j) {
        List<FormattedCharSequence> tooltipBoxLabel = null;

        if (getFocused() instanceof AbstractWidgetAccessor widget && widget.getTooltip() != null && widget.getTooltip().get() != null)
            tooltipBoxLabel = widget.getTooltip().get().toCharSequence(minecraft);
        else {
            var listener = getChildAt(i, j);
            if (listener.isPresent() && listener.get() instanceof AbstractWidgetAccessor widget &&
                widget.getTooltip() != null && widget.getTooltip().get() != null)
                tooltipBoxLabel = widget.getTooltip().get().toCharSequence(minecraft);
        }

        // Check custom options widgets for disabled widget tooltips
        if (tooltipBoxLabel == null) {
            for (AbstractWidget w : customOptionsWidgets) {
                if (w == null) continue;
                try {
                    if (w instanceof AbstractWidgetAccessor acc && acc.getTooltip() != null && acc.getTooltip().get() != null) {
                        try {
                            if (LegacyRenderUtil.isMouseOver(i, j, w.getX(), w.getY(), w.getWidth(), w.getHeight())) {
                                tooltipBoxLabel = acc.getTooltip().get().toCharSequence(minecraft);
                                break;
                            }
                        } catch (Throwable ignored) {
                            if (w.isMouseOver(i, j)) {
                                tooltipBoxLabel = acc.getTooltip().get().toCharSequence(minecraft);
                                break;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        return tooltipBoxLabel;
    }
}
