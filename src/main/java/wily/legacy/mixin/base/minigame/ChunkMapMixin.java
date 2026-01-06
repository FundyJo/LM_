package wily.legacy.mixin.base.minigame;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.minigame.IMinecraftServer;

/**
 * Verhindert das Speichern von Chunks auf Minigame-Servern
 */
@Mixin(ChunkMap.class)
public class ChunkMapMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("ChunkMapMixin");

    @Shadow
    @Final
    ServerLevel level;

    /**
     * Verhindert das Speichern einzelner Chunks
     */
    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void onSaveChunk(ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir) {
        if (level.getServer() instanceof IMinecraftServer minigameServer && minigameServer.isMinigameServer()) {
            // Verhindere das Speichern
            cir.setReturnValue(false);
        }
    }
}

