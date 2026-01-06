package wily.legacy.mixin.base.minigame;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.minigame.AbstractMinigameController;
import wily.legacy.minigame.BattleMinigameController;
import wily.legacy.minigame.MinigamesController;

/**
 * Mixin um Spieler-Tod im Minigame zu handhaben
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void legacy$onPlayerDie(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;

        MinigamesController controller = MinigamesController.getMinigameController(level);
        AbstractMinigameController<?> activeController = controller.getActiveController();

        if (activeController instanceof BattleMinigameController battleController) {
            // Prüfe ob der Killer ein Spieler ist
            Entity attacker = damageSource.getEntity();
            if (attacker instanceof ServerPlayer killer && !killer.getUUID().equals(self.getUUID())) {
                // Spieler wurde von einem anderen Spieler getötet
                battleController.playerKill(killer, self);
            } else {
                // Spieler starb durch Umwelt/Selbst
                battleController.playerDied(self);
            }
        }
    }
}

