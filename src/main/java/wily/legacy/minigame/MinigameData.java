package wily.legacy.minigame;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Datenklasse für Minigame-Konfiguration
 */
public record MinigameData(
    Minigame<?> minigame,
    Optional<Boolean> glideSolo,
    List<ResourceLocation> selectedMaps,
    Optional<Object> additionalData
) {
}
