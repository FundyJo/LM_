package wily.legacy.mixin.base.minigame;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
    @Accessor
    Long2ObjectLinkedOpenHashMap<ChunkHolder> getUpdatingChunkMap();

    @Accessor
    Long2ObjectLinkedOpenHashMap<ChunkHolder> getVisibleChunkMap();

    @Accessor
    void setModified(boolean modified);

    @Invoker("promoteChunkMap")
    boolean invokePromoteChunkMap();
}

