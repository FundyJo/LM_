package wily.legacy.mixin.base.minigame.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.minigame.IMinecraftServer;

/**
 * Verhindert Auto-Saving Screens auf Minigame-Servern
 */
@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

    @Shadow
    public abstract void setScreen(Screen screen);

    /**
     * Unterdrückt Auto-Saving Screens für Minigame-Server
     * - "Saving Level..." (beim Verlassen)
     * - "Autosaving in X..." (periodisch während des Spiels)
     */
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        Minecraft minecraft = (Minecraft)(Object)this;

        // Prüfe ob es ein Minigame-Server ist
        if (minecraft.getSingleplayerServer() instanceof IMinecraftServer minigameServer) {
            if (minigameServer.isMinigameServer()) {
                // Unterdrücke GenericMessageScreen (z.B. "Saving Level...")
                // und ProgressScreen (z.B. "Autosaving in X...")
                if (screen instanceof GenericMessageScreen || screen instanceof ProgressScreen) {
                    ci.cancel();
                }
            }
        }
    }
}

