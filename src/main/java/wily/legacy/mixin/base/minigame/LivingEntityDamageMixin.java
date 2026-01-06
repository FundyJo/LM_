package wily.legacy.mixin.base.minigame;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;
import wily.legacy.minigame.Minigame;
import wily.legacy.minigame.MinigamesController;

/**
 * Verhindert jeglichen Schaden in Minigame-Lobbies.
 * Auch andere Schadensquellen wie Feuer, Fall, Void, etc. werden blockiert.
 */
@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {

    /**
     * Verhindert allen Schaden wenn der Spieler in einer Minigame-Lobby ist.
     */
    @Inject(
        method = "hurtServer",
        at = @At("HEAD"),
        cancellable = true
    )
    private void legacy$onHurtServer(ServerLevel level, DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof ServerPlayer)) {
            return;
        }

        if (legacy$shouldPreventDamage(level)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean legacy$shouldPreventDamage(ServerLevel level) {
        MinigamesController controller = MinigamesController.getMinigameController(level);
        if (!controller.isActive()) {
            return false;
        }

        if (Legacy4JClient.getMinigame() == Minigame.LOBBY) {
            return true;
        }

        var activeController = controller.getActiveController();
        return activeController != null && !activeController.allowDamage();
    }
}
