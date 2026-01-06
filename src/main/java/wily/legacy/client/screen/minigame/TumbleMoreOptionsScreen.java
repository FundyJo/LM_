package wily.legacy.client.screen.minigame;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.LegacySliderButton;
import wily.legacy.client.screen.MultilineTooltip;
import wily.legacy.client.screen.TickBox;

import java.util.ArrayList;
import java.util.List;

/**
 * More Options screen for Tumble Minigame customization.
 *
 * Features:
 * - Options panel with game settings
 * - Tooltip box on the right side
 * - Two sections: "Game Options" and "Custom Options"
 */
public class TumbleMoreOptionsScreen extends MinigameMoreOptionsScreen {

    // ==================== INSTANCE FIELDS ====================

    // Shared options state
    private final TumbleOptions options;


    // ==================== CONSTRUCTOR ====================

    public TumbleMoreOptionsScreen(Screen parent) {
        this(parent, new TumbleOptions());
    }

    public TumbleMoreOptionsScreen(Screen parent, TumbleOptions options) {
        super(parent, DEFAULT_PANEL_WIDTH, calculatePanelHeight(2, 6, 1, 1), Component.translatable("createWorld.tab.more.title"));
        this.options = options != null ? options : new TumbleOptions();
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
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.tumble.more_options.game_type.tooltip")),
            options.gameType,
            () -> List.of("Shovels", "Snowballs", "Mixed", "Custom"),
            b -> {
                options.gameType = b.getObjectValue();
                updateCustomOptionsState();
            },
            () -> options.gameType
        ));

        // Empty line
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0, SECTION_SPACING, r -> ((guiGraphics, i, j, f) -> {})));

        // ==================== CUSTOM OPTIONS SECTION ====================
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0, 9, r -> ((guiGraphics, i, j, f) ->
            guiGraphics.drawString(font, Component.literal("Custom Options"), r.x + 1, r.y + 2, CommonColor.INVENTORY_GRAY_TEXT.get(), false))));

        // Lives per Round Slider
        List<Integer> livesOptions = new ArrayList<>();
        for (int i = 1; i <= 10; i++) livesOptions.add(i);
        LegacySliderButton<Integer> livesSlider = new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Lives per Round: " + b.getObjectValue()),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.tumble.more_options.lives_per_round.tooltip")),
            Integer.parseInt(options.livesPerRound),
            () -> livesOptions,
            b -> options.livesPerRound = String.valueOf(b.getObjectValue()),
            () -> Integer.parseInt(options.livesPerRound)
        );
        customOptionsWidgets.add(livesSlider);
        renderableVList.addRenderable(livesSlider);

        // Spectator Mode Slider
        LegacySliderButton<String> spectatorSlider = new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Spectator Mode: " + b.getObjectValue()),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.tumble.more_options.spectator_mode.tooltip")),
            options.spectatorMode,
            () -> List.of("Invisible", "Bat", "Parrot", "Vex", "Head"),
            b -> options.spectatorMode = b.getObjectValue(),
            () -> options.spectatorMode
        );
        customOptionsWidgets.add(spectatorSlider);
        renderableVList.addRenderable(spectatorSlider);

        // Item Set Slider
        LegacySliderButton<String> itemSetSlider = new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Item Set: " + b.getObjectValue()),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable(getItemSetTooltipKey(b.getObjectValue()))),
            options.itemSet,
            () -> List.of("Shovels", "Snowballs", "Fireworks", "Levitate Potions"),
            b -> options.itemSet = b.getObjectValue(),
            () -> options.itemSet
        );
        customOptionsWidgets.add(itemSetSlider);
        renderableVList.addRenderable(itemSetSlider);

        // Layer Scale Slider
        LegacySliderButton<String> layerScaleSlider = new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Layer Scale: " + b.getObjectValue()),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.tumble.more_options.layer_scale.tooltip")),
            options.layerScale,
            () -> List.of("Auto", "x1", "x1.25", "x1.5", "x1.75", "x2"),
            b -> options.layerScale = b.getObjectValue(),
            () -> options.layerScale
        );
        customOptionsWidgets.add(layerScaleSlider);
        renderableVList.addRenderable(layerScaleSlider);

        // Layer Count Slider
        List<String> layerCountOptions = new ArrayList<>();
        layerCountOptions.add("Auto");
        for (int i = 1; i <= 8; i++) layerCountOptions.add(String.valueOf(i));
        LegacySliderButton<String> layerCountSlider = new LegacySliderButton<>(0, 0, 0, SLIDER_HEIGHT,
            b -> Component.literal("Layer Count: " + b.getObjectValue()),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.tumble.more_options.layer_count.tooltip")),
            options.layerCount,
            () -> layerCountOptions,
            b -> options.layerCount = b.getObjectValue(),
            () -> options.layerCount
        );
        customOptionsWidgets.add(layerCountSlider);
        renderableVList.addRenderable(layerCountSlider);

        // Spectator Participation Checkbox
        TickBox spectatorParticipationBox = new TickBox(0, 0, options.spectatorParticipation,
            b -> Component.literal("Spectator Participation"),
            b -> new MultilineTooltip(tooltipBox.width - 10, Component.translatable("legacy.menu.tumble.more_options.spectator_participation.tooltip")),
            b -> options.spectatorParticipation = b.selected
        );
        customOptionsWidgets.add(spectatorParticipationBox);
        renderableVList.addRenderable(spectatorParticipationBox);

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

    /**
     * Maps the Item Set value to the corresponding tooltip translation key.
     */
    private static String getItemSetTooltipKey(String value) {
        if (value == null) return "legacy.menu.tumble.more_options.item_set.shovels.tooltip";
        return switch (value) {
            case "Shovels" -> "legacy.menu.tumble.more_options.item_set.shovels.tooltip";
            case "Snowballs" -> "legacy.menu.tumble.more_options.item_set.snowballs.tooltip";
            case "Fireworks" -> "legacy.menu.tumble.more_options.item_set.fireworks.tooltip";
            case "Levitate Potions" -> "legacy.menu.tumble.more_options.item_set.levitate_potions.tooltip";
            default -> "legacy.menu.tumble.more_options.item_set.shovels.tooltip";
        };
    }
}

