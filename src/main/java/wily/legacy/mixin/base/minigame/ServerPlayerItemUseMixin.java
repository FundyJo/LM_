package wily.legacy.mixin.base.minigame;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.minigame.AbstractMinigameController;
import wily.legacy.minigame.MinigamePermissions;
import wily.legacy.minigame.MinigamesController;

/**
 * Kontrolliert Item-Nutzung (UsePermissions) in Minigame-Welten.
 * Prüft ob Items benutzt werden dürfen (Bogen, Angel, Schneebälle, etc.).
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerItemUseMixin {

    @Shadow
    protected ServerLevel level;

    @Shadow
    @Final
    protected ServerPlayer player;

    /**
     * Verhindert Item-Nutzung wenn nicht in UsePermissions erlaubt.
     */
    @Inject(
        method = "useItem",
        at = @At("HEAD"),
        cancellable = true
    )
    private void legacy$onUseItem(ServerPlayer serverPlayer, Level level, ItemStack itemStack, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        if (level == null) return;

        MinigamesController controller = MinigamesController.getMinigameController(level);
        if (!controller.isActive()) return;

        AbstractMinigameController<?> activeController = controller.getActiveController();
        if (activeController == null) return;

        ItemStack stack = serverPlayer.getItemInHand(interactionHand);
        Item item = stack.getItem();
        MinigamePermissions usePerms = activeController.getUsePermissions();

        if (!usePerms.isItemAllowed(item)) {
            // Item-Nutzung nicht erlaubt
            serverPlayer.inventoryMenu.sendAllDataToRemote();
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}

