package wily.legacy.minigame;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * Controller for the Glide minigame.
 */
public class GlideMinigameController extends AbstractMinigameController<GlideMinigameController> {

    public GlideMinigameController(MinigamesController controller) {
        super(controller);
    }

    @Override
    public void writeNbt(CompoundTag tag) {

    }

    @Override
    public void readNbt(CompoundTag tag) {

    }

    @Override
    public Minigame<GlideMinigameController> getMinigame() {
        return Minigame.GLIDE;
    }

    // Glide Spawn Konfiguration
    private static final SpawnPosition.SpawnPositions GLIDE_SPAWNS = SpawnPosition.SpawnPositions.builder()
        .universal(0.5, 64.0, 0.5, 0.0f)
        .build();

    @Override
    public SpawnPosition.SpawnPositions getSpawnPositions() {
        return GLIDE_SPAWNS;
    }

    @Override
    public MinigameGamerules getGamerules() {
        return MinigameGamerules.GLIDE;
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


