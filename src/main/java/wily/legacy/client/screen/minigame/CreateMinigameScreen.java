package wily.legacy.client.screen.minigame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.*;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

import java.util.List;

/**
 * Screen for creating minigame sessions (Battle, Tumble, Glide).
 *
 * Features:
 * - Configurable game settings via checkboxes and sliders
 * - Dynamic tooltip system with scrolling for long descriptions
 * - Minigame-specific UI elements based on game type
 *
 * Layout Structure:
 * - Left Panel: Game settings (checkboxes, buttons, sliders)
 * - Right Panel: Tooltip box with scrollable descriptions
 * - Top: Minecraft Java Edition text + minigame icon/title
 */
public class CreateMinigameScreen extends PanelBackgroundScreen {
    // ==================== UI CONFIGURATION ====================

    // Panel & Icon Configuration
    private static final int PANEL_WIDTH = 245;
    private static final int ICON_SIZE = 36;
    private static final int PANEL_PADDING = 12;

    // Button & Slider Configuration
    private static final int BUTTON_HEIGHT = 20;
    private static final int SLIDER_HEIGHT = 19; // 1px smaller than buttons

    // Spacing Configuration
    private static final int ICON_TO_CHECKBOX_MARGIN = 16;
    private static final int CHECKBOX_TO_BUTTON_MARGIN = 3;
    private static final int ELEMENT_SPACING = 3;

    // Tooltip Configuration
    private static final int TOOLTIP_BOX_WIDTH = 160;
    private static final int TOOLTIP_MAX_VISIBLE_LINES = 6;
    private static final float TOOLTIP_TEXT_SCALE = 1.0f;
    private static final int TOOLTIP_LINE_SPACING = 2;

    // ==================== INSTANCE FIELDS ====================


    // Core Components
    private final MinigameType minigameType;
    private final Panel panelRecess;
    protected final Panel tooltipBox;
    protected ScrollableRenderer scrollableRenderer = new ScrollableRenderer(new LegacyScrollRenderer());

    // Shared options for Battle/Tumble etc.
    private final BattleOptions sharedBattleOptions = new BattleOptions();
    private final TumbleOptions sharedTumbleOptions = new TumbleOptions();

    // UI Widget References (for tooltip rendering)
    private TickBox onlineGameCheckbox;
    private TickBox publicGameCheckbox;
    private TickBox friendsOfFriendsCheckbox;
    private TickBox inviteOnlyCheckbox;
    private Button selectMapsButton;
    private LegacySliderButton<String> gameTypeSlider;
    private Button moreOptionsButton;
    private Button createButton;

    // Game Configuration State
    private boolean soloMode = false;
    private boolean onlineGame = false;
    private boolean publicGame = false;
    private boolean allowFriendsOfFriends = false;
    private boolean inviteOnly = false;
    private String gameType;
    private int gameSize = 8;


    // ==================== MINIGAME TYPE ENUM ====================

    public enum MinigameType {
        BATTLE("legacy.menu.minigame.battle", "minigame/battle"),
        TUMBLE("legacy.menu.minigame.tumble", "minigame/tumble"),
        GLIDE("legacy.menu.minigame.glide", "minigame/glide");

        private final String translationKey;
        private final String iconPath;

        MinigameType(String translationKey, String iconPath) {
            this.translationKey = translationKey;
            this.iconPath = iconPath;
        }

        public Component getDisplayName() {
            return Component.translatable(translationKey);
        }

        public String getIconPath() {
            return iconPath;
        }

        public List<String> getGameTypes() {
            return switch (this) {
                case BATTLE -> List.of("Casual", "Competitive", "Custom");
                case TUMBLE -> List.of("Shovels", "Snowballs", "Mixed", "Custom");
                case GLIDE -> List.of("Time Attack", "Score Attack");
            };
        }

        public boolean hasSoloMode() {
            return this == GLIDE;
        }

        public boolean hasMapSelection() {
            return this == BATTLE || this == GLIDE;
        }

        public boolean hasGameSizeOption() {
            return this == GLIDE;
        }

        public boolean hasMoreOptions() {
            return this == BATTLE || this == TUMBLE;
        }
    }

    // ==================== CONSTRUCTOR ====================

    public CreateMinigameScreen(Screen parent, MinigameType minigameType) {
        super(parent, s -> Panel.createPanel(s,
            p -> p.appearance(PANEL_WIDTH, calculatePanelHeight(s, minigameType)),
            p -> p.pos(p.centeredLeftPos(s), p.centeredTopPos(s))),
            Component.translatable("legacy.menu.create_minigame"));
        panelRecess = Panel.createPanel(this,
            p -> p.appearance(LegacySprites.PANEL_RECESS, panel.width - 18, panel.height - 18),
            p -> p.pos(panel.x + 9, panel.y + 9));
        tooltipBox = Panel.tooltipBoxOf(panel, TOOLTIP_BOX_WIDTH);
        this.minigameType = minigameType;

        // Set initial game type based on minigame type
        if (minigameType == MinigameType.TUMBLE) {
            this.gameType = "Mixed";
            this.sharedTumbleOptions.gameType = "Mixed";
        } else {
            this.gameType = minigameType.getGameTypes().getFirst();
            if (minigameType == MinigameType.BATTLE) {
                this.sharedBattleOptions.gameType = this.gameType;
            }
        }
    }


    private static int calculatePanelHeight(Screen screen, MinigameType minigameType) {
        int checkboxHeight = TickBox.getDefaultHeight();

        // Count elements
        int buttonCount = 1; // Create Game Button
        if (minigameType.hasMoreOptions()) buttonCount++;
        if (minigameType.hasGameSizeOption()) buttonCount++;
        buttonCount++; // Game Type Slider
        if (minigameType.hasMapSelection()) buttonCount++;

        int checkboxCount = 4; // Standard checkboxes
        if (minigameType.hasSoloMode()) checkboxCount++;

        // Calculate total content height
        int contentHeight = ICON_SIZE + ICON_TO_CHECKBOX_MARGIN +
                          checkboxCount * checkboxHeight + (checkboxCount - 1) * ELEMENT_SPACING +
                          CHECKBOX_TO_BUTTON_MARGIN +
                          buttonCount * BUTTON_HEIGHT + (buttonCount - 1) * ELEMENT_SPACING;

        return Math.min(contentHeight + 2 * PANEL_PADDING, screen.height - 52);
    }

    // ==================== UI INITIALIZATION ====================

    @Override
    protected void init() {
        super.init();
        panel.init();
        tooltipBox.init();

        // Layout configuration
        int layoutX = panel.x + 13;
        int layoutWidth = 220;
        int currentY = panel.y + PANEL_PADDING + ICON_SIZE + ICON_TO_CHECKBOX_MARGIN;

        // Add all UI elements from top to bottom
        currentY = addCheckboxes(layoutX, layoutWidth, currentY);
        addButtons(layoutX, layoutWidth, currentY);
    }


    private int addCheckboxes(int layoutX, int layoutWidth, int startY) {
        int currentY = startY;

        // Online Game checkbox
        onlineGameCheckbox = createCheckbox(layoutX, currentY, layoutWidth, onlineGame,
            "legacy.menu.minigame.online_game",
            selected -> this.onlineGame = selected);
        addRenderableWidget(accessor.putWidget("onlineGameCheckbox", onlineGameCheckbox));
        currentY += (TickBox.getDefaultHeight() + ELEMENT_SPACING);

        // Public Game checkbox
        publicGameCheckbox = createCheckbox(layoutX, currentY, layoutWidth, publicGame,
            "legacy.menu.minigame.public_game",
            selected -> this.publicGame = selected);
        addRenderableWidget(accessor.putWidget("publicGameCheckbox", publicGameCheckbox));
        currentY += (TickBox.getDefaultHeight() + ELEMENT_SPACING);

        // Allow friends of friends checkbox
        friendsOfFriendsCheckbox = createCheckbox(layoutX, currentY, layoutWidth, allowFriendsOfFriends,
            "legacy.menu.minigame.friends_of_friends",
            selected -> this.allowFriendsOfFriends = selected);
        addRenderableWidget(accessor.putWidget("friendsOfFriendsCheckbox", friendsOfFriendsCheckbox));
        currentY += (TickBox.getDefaultHeight() + ELEMENT_SPACING);

        // Invite Only checkbox
        inviteOnlyCheckbox = createCheckbox(layoutX, currentY, layoutWidth, inviteOnly,
            "legacy.menu.minigame.invite_only",
            selected -> this.inviteOnly = selected);
        addRenderableWidget(accessor.putWidget("inviteOnlyCheckbox", inviteOnlyCheckbox));
        currentY += (TickBox.getDefaultHeight() + ELEMENT_SPACING);

        // Solo checkbox (Glide only) - after Invite Only
        if (minigameType.hasSoloMode()) {
            TickBox soloCheckbox = createCheckbox(layoutX, currentY, layoutWidth, soloMode,
                "legacy.menu.minigame.solo",
                selected -> this.soloMode = selected);
            addRenderableWidget(accessor.putWidget("soloCheckbox", soloCheckbox));
            currentY += (TickBox.getDefaultHeight() + CHECKBOX_TO_BUTTON_MARGIN);
        } else {
            currentY += CHECKBOX_TO_BUTTON_MARGIN;
        }

        return currentY;
    }

    private void addButtons(int layoutX, int layoutWidth, int startY) {
        int currentY = startY;

        // Select Maps button (Battle and Glide only)
        if (minigameType.hasMapSelection()) {
            selectMapsButton = Button.builder(Component.translatable("legacy.menu.minigame.select_maps"),
                    button -> onSelectMaps()
                ).bounds(layoutX, currentY, layoutWidth, BUTTON_HEIGHT).build();
            addRenderableWidget(accessor.putWidget("selectMapsButton", selectMapsButton));
            currentY += (BUTTON_HEIGHT + ELEMENT_SPACING);
        }

        // Game Type Slider - now backed by sharedOptions so it's in sync with MoreOptionsScreen
        List<String> gameTypes = minigameType.getGameTypes();

        // Get the appropriate shared options object based on minigame type
        String currentGameType;
        if (minigameType == MinigameType.BATTLE) {
            if (!gameTypes.contains(sharedBattleOptions.gameType)) sharedBattleOptions.gameType = gameTypes.get(0);
            currentGameType = sharedBattleOptions.gameType;
        } else if (minigameType == MinigameType.TUMBLE) {
            if (!gameTypes.contains(sharedTumbleOptions.gameType)) sharedTumbleOptions.gameType = gameTypes.get(0);
            currentGameType = sharedTumbleOptions.gameType;
        } else {
            currentGameType = gameTypes.get(0);
        }

        gameTypeSlider = new LegacySliderButton<>(layoutX, currentY, layoutWidth, SLIDER_HEIGHT,
                b -> Component.translatable("legacy.menu.minigame.game_type").append(": ").append(b.getObjectValue()),
                b -> null,
                currentGameType,
                () -> gameTypes,
                b -> {
                    this.gameType = b.getObjectValue();
                    if (minigameType == MinigameType.BATTLE) {
                        sharedBattleOptions.gameType = b.getObjectValue();
                    } else if (minigameType == MinigameType.TUMBLE) {
                        sharedTumbleOptions.gameType = b.getObjectValue();
                    }
                },
                () -> {
                    if (minigameType == MinigameType.BATTLE) {
                        return sharedBattleOptions.gameType;
                    } else if (minigameType == MinigameType.TUMBLE) {
                        return sharedTumbleOptions.gameType;
                    }
                    return gameType;
                }
            );
        addRenderableWidget(accessor.putWidget("gameTypeSlider", gameTypeSlider));
        currentY += (SLIDER_HEIGHT + ELEMENT_SPACING);

        // Game Size Slider (Glide only)
        if (minigameType.hasGameSizeOption()) {
            LegacySliderButton<Integer> gameSizeSlider = new LegacySliderButton<>(layoutX, currentY, layoutWidth, SLIDER_HEIGHT,
                    b -> Component.translatable("legacy.menu.minigame.game_size")
                        .append(": ")
                        .append(String.valueOf(b.getObjectValue()))
                        .append(" ")
                        .append(Component.translatable("legacy.menu.minigame.players")),
                    b -> null,
                    gameSize,
                    () -> List.of(8, 16),
                    b -> this.gameSize = b.getObjectValue(),
                    () -> this.gameSize
                );
            addRenderableWidget(accessor.putWidget("gameSizeSlider", gameSizeSlider));
            currentY += (SLIDER_HEIGHT + ELEMENT_SPACING);
        }

        // More Options Button (Battle and Tumble only)
        if (minigameType.hasMoreOptions()) {
            moreOptionsButton = Button.builder(Component.translatable("createWorld.tab.more.title"),
                    button -> onMoreOptions()
                ).bounds(layoutX, currentY, layoutWidth, BUTTON_HEIGHT).build();
            addRenderableWidget(accessor.putWidget("moreOptionsButton", moreOptionsButton));
            currentY += (BUTTON_HEIGHT + ELEMENT_SPACING);
        }

        // Create Game Button
        createButton = Button.builder(Component.translatable("legacy.menu.minigame.create_game"),
                button -> onCreate()
            ).bounds(layoutX, currentY, layoutWidth, BUTTON_HEIGHT).build();
        addRenderableWidget(accessor.putWidget("createButton", createButton));
    }

    private TickBox createCheckbox(int x, int y, int width, boolean selected, String translationKey, java.util.function.Consumer<Boolean> onSelect) {
        return new TickBox(x + 1, y, width, selected,
            b -> Component.translatable(translationKey),
            b -> null,
            button -> onSelect.accept(button.selected)
        );
    }

    // ==================== EVENT HANDLERS ====================

    private void onSelectMaps() {
        // TODO: Open map selection screen
    }

    private void onMoreOptions() {
        if (minecraft != null) {
            // Open the appropriate more options screen based on minigame type
            switch (minigameType) {
                case BATTLE:
                    minecraft.setScreen(new BattleMoreOptionsScreen(this, sharedBattleOptions));
                    break;
                case TUMBLE:
                    minecraft.setScreen(new TumbleMoreOptionsScreen(this, sharedTumbleOptions));
                    break;
                default:
                    // No more options screen for other minigame types
                    break;
            }
        }
    }

    private void onCreate() {
        LegacySoundUtil.playSimpleUISound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);

        if (minecraft == null) {
            return;
        }

        // Erstelle MinigameServerConfig basierend auf den Einstellungen
        wily.legacy.minigame.MinigameServerConfig config = createMinigameConfig();

        // Starte Minigame via MinigameWorldLoader
        boolean success = wily.legacy.minigame.MinigameWorldLoader.loadMinigameLobby(config, minecraft);

        if (!success) {
            // Zeige Fehler-Dialog
            minecraft.setScreen(new net.minecraft.client.gui.screens.AlertScreen(
                () -> minecraft.setScreen(this),
                Component.literal("Error"),
                Component.literal("Failed to load minigame lobby!\nCheck logs for details.")
            ));
        }

        // doWorldLoad() wird automatisch das GUI schließen wenn erfolgreich
    }

    /**
     * Erstellt MinigameServerConfig aus den aktuellen UI-Einstellungen
     */
    private wily.legacy.minigame.MinigameServerConfig createMinigameConfig() {
        // Bestimme Minigame-Typ
        String minigameTypeName = switch (minigameType) {
            case BATTLE -> "BATTLE";
            case TUMBLE -> "TUMBLE";
            case GLIDE -> "GLIDE";
        };

        // Bestimme max Players
        int maxPlayers = switch (minigameType) {
            case BATTLE -> sharedBattleOptions.gameSize;
            case GLIDE -> gameSize;
            case TUMBLE -> 16; // Standard für Tumble
        };

        // Bestimme min Players
        int minPlayers;
        if (soloMode) {
            minPlayers = 1; // Solo Mode
        } else {
            // Standard min Players basierend auf Typ
            minPlayers = switch (minigameType) {
                case BATTLE -> 2;
                case TUMBLE -> 2;
                case GLIDE -> 2;
            };
        }

        // Erstelle Config
        return new wily.legacy.minigame.MinigameServerConfig(
            minigameTypeName,
            maxPlayers,
            minPlayers
        );
    }

    // ==================== RENDERING ====================

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, false);

        tooltipBox.render(guiGraphics, i, j, f);
        panel.render(guiGraphics, i, j, f);
        panelRecess.render(guiGraphics, i, j, f);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderIcon(guiGraphics);
        renderTooltipInBox(guiGraphics, mouseX, mouseY);
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Check if mouse is over tooltip box
        if (LegacyRenderUtil.isMouseOver((int)mouseX, (int)mouseY, tooltipBox.x, tooltipBox.y, tooltipBox.width, tooltipBox.height)) {
            return scrollableRenderer.mouseScrolled(scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ==================== TOOLTIP SYSTEM ====================

    private void renderTooltipInBox(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component tooltipText = getTooltipText(mouseX, mouseY);

        if (tooltipText != null) {
            renderTooltip(guiGraphics, tooltipText);
        } else {
            scrollableRenderer.resetScrolled();
        }
    }

    private Component getTooltipText(int mouseX, int mouseY) {
        // Check checkboxes
        if (isHoveredOrFocused(onlineGameCheckbox, mouseX, mouseY)) {
            return getTooltipComponent("online_game");
        }
        if (isHoveredOrFocused(publicGameCheckbox, mouseX, mouseY)) {
            return getTooltipComponent("public_game");
        }
        if (isHoveredOrFocused(friendsOfFriendsCheckbox, mouseX, mouseY)) {
            return getTooltipComponent("friends_of_friends");
        }
        if (isHoveredOrFocused(inviteOnlyCheckbox, mouseX, mouseY)) {
            return getTooltipComponent("invite_only");
        }

        // Check buttons and sliders
        if (isHoveredOrFocused(selectMapsButton, mouseX, mouseY)) {
            return getTooltipComponent("select_maps");
        }
        if (isHoveredOrFocused(gameTypeSlider, mouseX, mouseY)) {
            return getGameTypeTooltip();
        }
        // More Options and Create Game buttons have no tooltips

        return null;
    }

    /**
     * Generates tooltip key for checkboxes and simple buttons
     * @param key The tooltip key suffix (e.g., "online_game", "select_maps")
     * @return Component with translated tooltip text
     */
    private Component getTooltipComponent(String key) {
        return Component.translatable("legacy.menu.minigame." + key + ".tooltip");
    }

    /**
     * Generates tooltip for game type slider based on selected game type
     * @return Component with translated tooltip text for the selected game type
     */
    private Component getGameTypeTooltip() {
        String gameTypeKey = normalizeGameTypeKey(gameType);

        // Custom game type uses generic tooltip
        if ("custom".equals(gameTypeKey)) {
            return Component.translatable("legacy.menu.create_minigame.custom.tooltip");
        }

        // Other game types use minigame-specific tooltips
        String minigameKey = minigameType.name().toLowerCase();
        return Component.translatable("legacy.menu.create_minigame." + minigameKey + "." + gameTypeKey + ".tooltip");
    }

    /**
     * Normalizes game type string for use in translation keys
     * Converts to lowercase and replaces spaces with underscores
     * Examples: "Time Attack" -> "time_attack", "Casual" -> "casual"
     */
    private String normalizeGameTypeKey(String gameType) {
        return gameType.toLowerCase().replace(" ", "_");
    }

    private boolean isHoveredOrFocused(net.minecraft.client.gui.components.AbstractWidget widget, int mouseX, int mouseY) {
        if (widget == null) return false;
        return LegacyRenderUtil.isMouseOver(mouseX, mouseY, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight())
            || widget.isFocused();
    }

    private void renderTooltip(GuiGraphics guiGraphics, Component tooltipText) {
        int adjustedWidth = (int)((tooltipBox.width - 16) / TOOLTIP_TEXT_SCALE);

        MultilineTooltip tooltip = new MultilineTooltip(tooltipText, adjustedWidth);
        List<net.minecraft.util.FormattedCharSequence> lines = tooltip.toCharSequence(minecraft);

        // Configure scrollable renderer
        int lineHeight = font.lineHeight + TOOLTIP_LINE_SPACING;
        int scrollAreaHeight = (int)((lineHeight * TOOLTIP_MAX_VISIBLE_LINES) * TOOLTIP_TEXT_SCALE);

        scrollableRenderer.lineHeight = lineHeight;
        scrollableRenderer.scrolled.max = Math.max(0, lines.size() - TOOLTIP_MAX_VISIBLE_LINES);

        // Render with scaling
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(tooltipBox.x + 8, tooltipBox.y + 8);
        guiGraphics.pose().scale(TOOLTIP_TEXT_SCALE, TOOLTIP_TEXT_SCALE);

        scrollableRenderer.render(guiGraphics, 0, 0, adjustedWidth, (int)(scrollAreaHeight / TOOLTIP_TEXT_SCALE), () -> {
            int yPos = 0;
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                guiGraphics.drawString(font, line, 0, yPos, 0xFFFFFFFF, false);
                yPos += lineHeight;
            }
        });

        guiGraphics.pose().popMatrix();
    }

    private void renderIcon(GuiGraphics guiGraphics) {
        int iconX = panel.x + 14;
        int iconY = panel.y + 12;

        // Render icon holder background
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ICON_HOLDER, iconX, iconY, ICON_SIZE, ICON_SIZE);

        // Render minigame icon (centered in holder)
        int iconSpriteSize = 32;
        int iconOffset = (ICON_SIZE - iconSpriteSize) / 2;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(
            Legacy4J.createModLocation(minigameType.getIconPath()),
            iconX + iconOffset, iconY + iconOffset, iconSpriteSize, iconSpriteSize
        );

        // Render minigame title next to icon
        renderMinigameTitle(guiGraphics, iconX + ICON_SIZE + 4, iconY + 4);
    }

    private void renderMinigameTitle(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y);
        if (LegacyOptions.getUIMode().isSD()) {
            guiGraphics.pose().scale(0.5f, 0.5f);
        }
        guiGraphics.drawString(font, minigameType.getDisplayName(), 0, 0, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        guiGraphics.pose().popMatrix();
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        OptionsScreen.setupSelectorControlTooltips(renderer, this);
    }

    public void updateFromSharedOptions() {
        // Refresh local state from sharedOptions so UI reflects changes made in MoreOptionsScreen
        if (gameTypeSlider != null) {
            gameTypeSlider.updateValue();
            if (minigameType == MinigameType.BATTLE) {
                gameType = sharedBattleOptions.gameType;
            } else if (minigameType == MinigameType.TUMBLE) {
                gameType = sharedTumbleOptions.gameType;
            }
        }
        // If there are other widgets that depend on sharedOptions, update them here in future
    }
}
