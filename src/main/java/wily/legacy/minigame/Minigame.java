package wily.legacy.minigame;

import java.util.function.Function;

/**
 * Klasse für verfügbare Minigames
 */
public class Minigame<T extends AbstractMinigameController<T>> {
    public static final Minigame<AbstractMinigameController.NoneMinigameController> NONE = new Minigame<>(0, AbstractMinigameController.NoneMinigameController::new);
    public static final Minigame<BattleMinigameController> BATTLE = new Minigame<>(1, BattleMinigameController::new);
    public static final Minigame<TumbleMinigameController> TUMBLE = new Minigame<>(2, TumbleMinigameController::new);
    public static final Minigame<GlideMinigameController> GLIDE = new Minigame<>(3, GlideMinigameController::new);
    public static final Minigame<LobbyMinigameController> LOBBY = new Minigame<>(99, LobbyMinigameController::new);

    private final int id;
    private final Function<MinigamesController, T> controllerSupplier;

    Minigame(int id, Function<MinigamesController, T> controllerSupplier) {
        this.id = id;
        this.controllerSupplier = controllerSupplier;
    }

    public int getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractMinigameController<T>> Minigame<T> fromId(int id) {
        return (Minigame<T>) switch (id) {
            case 1 -> BATTLE;
            case 2 -> TUMBLE;
            case 3 -> GLIDE;
            case 99 -> LOBBY;
            default -> NONE;
        };
    }

    /**
     * Gibt das Minigame basierend auf dem String-Identifier zurück (z.B. "lobby", "battle")
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractMinigameController<T>> Minigame<T> fromId(String id) {
        if (id == null) return (Minigame<T>) NONE;
        return (Minigame<T>) switch (id.toLowerCase()) {
            case "battle" -> BATTLE;
            case "tumble" -> TUMBLE;
            case "glide" -> GLIDE;
            case "lobby" -> LOBBY;
            default -> NONE;
        };
    }

    public T newController(MinigamesController controller) {
        return controllerSupplier.apply(controller);
    }

    public String getName() {
        return switch (id) {
            case 0 -> "None";
            case 1 -> "Battle";
            case 2 -> "Tumble";
            case 3 -> "Glide";
            case 99 -> "Lobby";
            default -> "Unknown";
        };
    }

    @Override
    public String toString() {
        return "Minigame[" + getName() + "]";
    }

    public boolean isActualMinigame() {
        return this.id != 0 && this.id != 99; // Not NONE and not LOBBY
    }

    public boolean hasBareHotbar() {
        return this.id == 99; // LOBBY hat bare hotbar (keine Herzen)
    }

    public String tId() {
        return switch (id) {
            case 0 -> "none";
            case 1 -> "battle";
            case 2 -> "tumble";
            case 3 -> "glide";
            case 99 -> "lobby";
            default -> "unknown";
        };
    }
}
