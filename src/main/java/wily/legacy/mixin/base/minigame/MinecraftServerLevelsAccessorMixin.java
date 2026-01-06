package wily.legacy.mixin.base.minigame;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.minigame.IMinecraftServerLevels;

import java.util.Map;

/**
 * Accessor-Mixin für Zugriff auf die private levels Map
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerLevelsAccessorMixin implements IMinecraftServerLevels {

    @Shadow
    @Final
    private Map<ResourceKey<Level>, ServerLevel> levels;

    @Override
    public Map<ResourceKey<Level>, ServerLevel> legacy$getLevels() {
        return this.levels;
    }
}

