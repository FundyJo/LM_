package wily.legacy.mixin.base.minigame;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.minigame.AbstractMinigameController;
import wily.legacy.minigame.Minigame;
import wily.legacy.minigame.MinigamesController;

/**
 * Verhindert Hunger-Verlust in Minigame-Lobbies.
 */
@Mixin(FoodData.class)
public class FoodDataMixin {

    /**
     * Verhindert Hunger-Verlust durch Tick.
     */
    @Inject(
        method = "tick",
        at = @At("HEAD"),
        cancellable = true
    )
    private void legacy$onTick(ServerPlayer player, CallbackInfo ci) {
        if (legacy$shouldPreventHunger(player)) {
            // Setze Hunger und Sättigung auf Maximum
            FoodData self = (FoodData) (Object) this;
            self.setFoodLevel(20);
            self.setSaturation(20.0f);
            ci.cancel();
        }
    }

    @Unique
    private boolean legacy$shouldPreventHunger(ServerPlayer player) {
        if (player.level() == null) return false;

        MinigamesController controller = MinigamesController.getMinigameController(player.level());
        if (!controller.isActive()) {
            return false;
        }

        AbstractMinigameController<?> activeController = controller.getActiveController();
        if (activeController == null) {
            return false;
        }

        // In der Lobby ist immer kein Hunger-Verlust erlaubt
        if (activeController.getMinigame() == Minigame.LOBBY) {
            return true;
        }

        // Prüfe ob der Controller Hunger erlaubt
        return !activeController.allowHunger();
    }
}

