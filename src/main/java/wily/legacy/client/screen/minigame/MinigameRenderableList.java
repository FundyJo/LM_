package wily.legacy.client.screen.minigame;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.CreationList;
import wily.legacy.client.screen.RenderableVList;

import java.util.function.Consumer;

public class MinigameRenderableList extends RenderableVList {
    private final boolean isCreateMode;
    private final Minecraft minecraft = Minecraft.getInstance();

    public MinigameRenderableList(UIAccessor accessor, boolean isCreateMode) {
        super(accessor);
        layoutSpacing(l -> 0);
        this.isCreateMode = isCreateMode;
    }

    @Override
    public void init(String id, int x, int y, int width, int height) {
        renderables.clear();

        // Battle Button
        addMinigameButton(
            Legacy4J.createModLocation("minigame/battle"),
            Component.translatable("legacy.menu.minigame.battle"),
            b -> {
                if (isCreateMode) {
                    minecraft.setScreen(new CreateMinigameScreen(getScreen(PlayMinigameScreen.class), CreateMinigameScreen.MinigameType.BATTLE));
                } else {
                    // TODO: Join Battle minigame logic
                }
            });

        // Tumble Button
        addMinigameButton(
            Legacy4J.createModLocation("minigame/tumble"),
            Component.translatable("legacy.menu.minigame.tumble"),
            b -> {
                if (isCreateMode) {
                    minecraft.setScreen(new CreateMinigameScreen(getScreen(PlayMinigameScreen.class), CreateMinigameScreen.MinigameType.TUMBLE));
                } else {
                    // TODO: Join Tumble minigame logic
                }
            });

        // Glide Button
        addMinigameButton(
            Legacy4J.createModLocation("minigame/glide"),
            Component.translatable("legacy.menu.minigame.glide"),
            b -> {
                if (isCreateMode) {
                    minecraft.setScreen(new CreateMinigameScreen(getScreen(PlayMinigameScreen.class), CreateMinigameScreen.MinigameType.GLIDE));
                } else {
                    // TODO: Join Glide minigame logic
                }
            });

        super.init(id, x, y, width, height);
    }

    private void addMinigameButton(net.minecraft.resources.ResourceLocation iconSprite, Component message, Consumer<AbstractButton> onPress) {
        AbstractButton button;
        this.addRenderable(button = new CreationList.ContentButton(this, 0, 0, 270, 30, message) {
            @Override
            public void renderIcon(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
                FactoryGuiGraphics.of(guiGraphics).blitSprite(iconSprite, getX() + x, getY() + y, width, height);
            }

            @Override
            public void renderIconHighlight(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
                // Deaktiviert den Transparenz-Effekt beim Hover
            }

            @Override
            public void onPress(InputWithModifiers input) {
                onPress.accept(this);
            }
        });
    }
}

