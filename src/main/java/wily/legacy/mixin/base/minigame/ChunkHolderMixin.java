package wily.legacy.mixin.base.minigame;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.minigame.AbstractMinigameController;
import wily.legacy.minigame.IMinecraftServer;
import wily.legacy.minigame.MinigamesController;

import java.util.List;

/**
 * Beschränkt aktive Chunks auf definierte Bereiche in Minigame-Welten.
 * Nur Chunks innerhalb der vom Controller definierten Bereiche werden geladen.
 */
@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin extends GenerationChunkHolder {

    @Shadow
    @Final
    private LevelHeightAccessor levelHeightAccessor;

    public ChunkHolderMixin(ChunkPos chunkPos) {
        super(chunkPos);
    }

    @WrapOperation(
        method = "updateFutures",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkLevel;fullStatus(I)Lnet/minecraft/server/level/FullChunkStatus;"
        )
    )
    private FullChunkStatus legacy$limitActiveChunks(int level, Operation<FullChunkStatus> original) {
        // Prüfe ob es ein Minigame-Level ist
        if (!(this.levelHeightAccessor instanceof Level gameLevel)) {
            return original.call(level);
        }

        // Prüfe ob es ein Minigame-Server ist
        if (gameLevel.getServer() != null &&
            gameLevel.getServer() instanceof IMinecraftServer minigameServer &&
            minigameServer.isMinigameServer()) {

            // Hole den aktiven Controller
            MinigamesController controller = MinigamesController.getMinigameController(gameLevel);
            if (controller != null && controller.isActive()) {
                AbstractMinigameController<?> activeController = controller.getActiveController();
                if (activeController != null) {
                    List<AABB> activeAreas = activeController.getActiveChunkAreas();

                    // Wenn keine Bereiche definiert, alle Chunks erlauben
                    if (activeAreas.isEmpty()) {
                        return original.call(level);
                    }

                    // Prüfe ob dieser Chunk in einem aktiven Bereich liegt
                    int chunkX = this.pos.x;
                    int chunkZ = this.pos.z;

                    for (AABB area : activeAreas) {
                        // +2 Chunk Buffer für Rendering/Simulation
                        if (chunkX >= area.minX - 2 && chunkX <= area.maxX + 2 &&
                            chunkZ >= area.minZ - 2 && chunkZ <= area.maxZ + 2) {
                            return original.call(level);
                        }
                    }

                    // Chunk liegt außerhalb aller aktiven Bereiche
                    return FullChunkStatus.INACCESSIBLE;
                }
            }
        }

        return original.call(level);
    }
}

