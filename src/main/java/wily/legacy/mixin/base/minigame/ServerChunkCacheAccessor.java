package wily.legacy.mixin.base.minigame;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor für ServerChunkCache um auf ChunkMap zuzugreifen
 */
@Mixin(ServerChunkCache.class)
public interface ServerChunkCacheAccessor {

    @Accessor("chunkMap")
    ChunkMap getChunkMap();
}

