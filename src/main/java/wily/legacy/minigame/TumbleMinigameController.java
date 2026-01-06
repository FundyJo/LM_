package wily.legacy.minigame;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * Controller for the Glide minigame.
 */
public class TumbleMinigameController extends AbstractMinigameController<TumbleMinigameController> {

    public TumbleMinigameController(MinigamesController controller) {
        super(controller);
    }

    @Override
    public void writeNbt(CompoundTag tag) {
        // TumbleMinigameController hat aktuell keinen Zustand zum Speichern
        // Diese Methode kann in Zukunft erweitert werden, wenn State hinzugefügt wird
    }

    @Override
    public void readNbt(CompoundTag tag) {
        // TumbleMinigameController hat aktuell keinen Zustand zum Laden
        // Diese Methode kann in Zukunft erweitert werden, wenn State hinzugefügt wird
    }

    @Override
    public Minigame<TumbleMinigameController> getMinigame() {
        return Minigame.TUMBLE;
    }

    // Tumble Spawn Konfiguration
    private static final SpawnPosition.SpawnPositions TUMBLE_SPAWNS = SpawnPosition.SpawnPositions.builder()
        .universal(0.5, 64.0, 0.5, 0.0f)
        .build();

    @Override
    public SpawnPosition.SpawnPositions getSpawnPositions() {
        return TUMBLE_SPAWNS;
    }

    @Override
    public MinigameGamerules getGamerules() {
        return MinigameGamerules.TUMBLE;
    }

    @Override
    public void sendToMap(RegistryAccess access, ServerPlayer player, boolean late) {
        player.setGameMode(GameType.ADVENTURE);
        player.getInventory().clearContent();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
    }

    @Override
    public boolean pvpEnabled() {
        return false;
    }

    @Override
    public boolean isSmallInventory() {
        return true;
    }
}


