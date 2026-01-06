package wily.legacy.mixin.base.minigame;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.minigame.AbstractMinigameController;
import wily.legacy.minigame.MinigamesController;

/**
 * Verhindert Entity-Zerstörung (Item Frames, Paintings, Armor Stands, etc.) in Minigame-Welten.
 * Funktioniert unabhängig vom GameMode des Spielers.
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerEntityInteractionMixin {

    /**
     * Verhindert das Angreifen/Zerstören von bestimmten Entities in Minigames.
     */
    @Inject(
        method = "attack",
        at = @At("HEAD"),
        cancellable = true
    )
    private void legacy$onAttack(Entity target, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;

        if (legacy$shouldPreventEntityDestruction(self, target)) {
            ci.cancel();
        }
    }


    @Unique
    private boolean legacy$shouldPreventEntityDestruction(ServerPlayer player, Entity entity) {
        if (player.level() == null) return false;

        MinigamesController controller = MinigamesController.getMinigameController(player.level());
        if (!controller.isActive()) {
            return false;
        }

        AbstractMinigameController<?> activeController = controller.getActiveController();
        if (activeController == null) {
            return false;
        }

        // Verhindere Zerstörung von Dekorations-Entities wenn Block-Interaktion verboten ist
        if (!activeController.allowBlockBreaking()) {
            // Item Frames, Paintings, Armor Stands, etc.
            if (entity instanceof HangingEntity || entity instanceof ArmorStand) {
                return true;
            }
        }

        // Verhindere PvP wenn deaktiviert (für Spieler-Entities)
        if (!activeController.pvpEnabled() && entity instanceof ServerPlayer) {
            return true;
        }

        return false;
    }
}

