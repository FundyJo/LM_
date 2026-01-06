package wily.legacy.client.screen.minigame;

/**
 * Simple data holder for shared battle/minigame options between screens.
 */
public class BattleOptions {
    public String gameType = "Casual";
    public String roundLength = "Normal";
    public int gameSize = 16;
    public boolean centralSpawn = true;
    public String livesPerRound = "1";
    public String spectatorMode = "Bat";
    public boolean allowAllSkins = false;
    public String itemSet = "Normal";
    public String hungerSettings = "Normal";
    public int roundCount = 1;
    public String mapSize = "Auto";
    public boolean naturalRegeneration = true;
    public boolean smallInventory = true;
    public boolean takeEverything = true;
    public boolean chestRefill = true;

    public BattleOptions() {}

    /**
     * Konvertiert die Battle-Optionen in eine MinigameServerConfig
     */
    public wily.legacy.minigame.MinigameServerConfig toMinigameConfig(int minPlayers) {
        return new wily.legacy.minigame.MinigameServerConfig(
            "BATTLE",
            gameSize,
            minPlayers
        );
    }
}

