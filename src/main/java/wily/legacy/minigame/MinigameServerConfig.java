package wily.legacy.minigame;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

/**
 * Konfiguration für einen Minigame-Server
 * Wird beim Server-Start festgelegt und kann nicht mehr geändert werden
 */
public class MinigameServerConfig {

    public static final Codec<MinigameServerConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("minigameType").forGetter(c -> c.minigameType),
            Codec.INT.fieldOf("maxPlayers").forGetter(c -> c.maxPlayers),
            Codec.INT.fieldOf("minPlayersToStart").forGetter(c -> c.minPlayersToStart)
        ).apply(instance, MinigameServerConfig::new)
    );

    private final String minigameType; // "TUMBLE", "BATTLE", "GLIDE"
    private final int maxPlayers;
    private final int minPlayersToStart;

    public MinigameServerConfig(String minigameType, int maxPlayers, int minPlayersToStart) {
        this.minigameType = minigameType;
        this.maxPlayers = maxPlayers;
        this.minPlayersToStart = minPlayersToStart;
    }

    public String getMinigameType() {
        return minigameType;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getMinPlayersToStart() {
        return minPlayersToStart;
    }

    public Minigame<?> getMinigame() {
        return switch (minigameType.toUpperCase()) {
            case "TUMBLE" -> Minigame.TUMBLE;
            case "BATTLE" -> Minigame.BATTLE;
            case "GLIDE" -> Minigame.GLIDE;
            default -> Minigame.NONE;
        };
    }

    public static MinigameServerConfig readNbt(CompoundTag tag) {
        String minigameType = tag.contains("minigameType") ? tag.getString("minigameType").orElse("TUMBLE") : "TUMBLE";
        int maxPlayers = tag.contains("maxPlayers") ? tag.getInt("maxPlayers").orElse(8) : 8;
        int minPlayersToStart = tag.contains("minPlayersToStart") ? tag.getInt("minPlayersToStart").orElse(2) : 2;

        return new MinigameServerConfig(minigameType, maxPlayers, minPlayersToStart);
    }

    /**
     * Standard-Konfigurationen für verschiedene Minigames
     */
    public static MinigameServerConfig tumble(int maxPlayers) {
        return new MinigameServerConfig("TUMBLE", maxPlayers, 2);
    }

    public static MinigameServerConfig battle(int maxPlayers) {
        return new MinigameServerConfig("BATTLE", maxPlayers, 2);
    }

    public static MinigameServerConfig glide(int maxPlayers) {
        return new MinigameServerConfig("GLIDE", maxPlayers, 2);
    }
}

