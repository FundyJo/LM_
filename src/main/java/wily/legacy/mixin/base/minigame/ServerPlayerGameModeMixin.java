package wily.legacy.mixin.base.minigame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.minigame.AbstractMinigameController;
import wily.legacy.minigame.MinigamePermissions;
import wily.legacy.minigame.MinigamesController;

/**
 * Verhindert Block-Zerstörung, -Platzierung und kontrolliert Item/Block-Nutzung in Minigame-Welten.
 * Funktioniert unabhängig vom GameMode des Spielers (auch im Creative Mode).
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow
    protected ServerLevel level;

    @Shadow
    @Final
    protected ServerPlayer player;

    /**
     * Verhindert das Starten des Block-Abbaus - prüft DestroyPermissions.
     */
    @Inject(
        method = "handleBlockBreakAction",
        at = @At("HEAD"),
        cancellable = true
    )
    private void legacy$onBlockBreakAction(BlockPos blockPos, ServerboundPlayerActionPacket.Action action, Direction direction, int i, int j, CallbackInfo ci) {
        if (legacy$shouldPreventBlockBreaking(blockPos)) {
            // Sende Block-Update bei START damit Client keine Animation zeigt
            if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                BlockState state = level.getBlockState(blockPos);
                player.connection.send(new ClientboundBlockUpdatePacket(blockPos, state));
            }
            ci.cancel();
        }
    }

    /**
     * Backup: Verhindert auch destroyBlock falls handleBlockBreakAction umgangen wird.
     */
    @Inject(
        method = "destroyBlock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void legacy$onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (legacy$shouldPreventBlockBreaking(pos)) {
            BlockState state = level.getBlockState(pos);
            player.connection.send(new ClientboundBlockUpdatePacket(pos, state));
            cir.setReturnValue(false);
        }
    }

    /**
     * Verhindert Block-Platzierung und Block-Interaktion - prüft PlacePermissions und BlockUsePermissions.
     */
    @Inject(
        method = "useItemOn",
        at = @At("HEAD"),
        cancellable = true
    )
    private void legacy$onUseItemOn(ServerPlayer serverPlayer, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (level == null) return;

        MinigamesController controller = MinigamesController.getMinigameController(level);
        if (!controller.isActive()) return;

        AbstractMinigameController<?> activeController = controller.getActiveController();
        if (activeController == null) return;

        // Prüfe Block-Platzierung
        if (stack.getItem() instanceof BlockItem blockItem) {
            MinigamePermissions placePerms = activeController.getPlacePermissions();
            if (!placePerms.isBlockAllowed(blockItem.getBlock())) {
                serverPlayer.inventoryMenu.sendAllDataToRemote();
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }
        }

        // Prüfe Block-Interaktion (Türen, Truhen, Amboss, etc.)
        BlockPos clickedPos = hitResult.getBlockPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        Block clickedBlock = clickedState.getBlock();

        MinigamePermissions blockUsePerms = activeController.getBlockUsePermissions();
        if (!blockUsePerms.isBlockAllowed(clickedBlock)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Unique
    private boolean legacy$shouldPreventBlockBreaking(BlockPos pos) {
        if (level == null) return false;

        MinigamesController controller = MinigamesController.getMinigameController(level);
        if (!controller.isActive()) {
            return false;
        }

        AbstractMinigameController<?> activeController = controller.getActiveController();
        if (activeController == null) {
            return false;
        }

        // Prüfe DestroyPermissions
        BlockState state = level.getBlockState(pos);
        MinigamePermissions destroyPerms = activeController.getDestroyPermissions();
        return !destroyPerms.isBlockAllowed(state.getBlock());
    }
}

