package wily.legacy.minigame;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;

/**
 * Accessor-Interface für Zugriff auf private Felder des MinecraftServer.
 *
 * Wird via Mixin implementiert um Zugriff auf die levels Map zu ermöglichen,
 * was für dynamisches Laden/Entladen von Dimensionen benötigt wird.
 *
 * @see MinigameLevelManager für Dimension-Verwaltung
 */
public interface IMinecraftServerLevels {

    /**
     * Gibt Zugriff auf die private levels Map.
     * Präfix 'legacy$' für Mixin-Kompatibilität.
     */
    Map<ResourceKey<Level>, ServerLevel> legacy$getLevels();
}

