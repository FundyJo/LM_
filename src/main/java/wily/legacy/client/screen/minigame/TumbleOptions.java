package wily.legacy.client.screen.minigame;

/**
 * Simple data holder for shared tumble/minigame options between screens.
 */
public class TumbleOptions {
    public String gameType = "Mixed";
    public String livesPerRound = "1";
    public String spectatorMode = "Bat";
    public String itemSet = "Shovels";
    public String layerScale = "Auto";
    public String layerCount = "Auto";
    public boolean spectatorParticipation = false;

    public TumbleOptions() {}

    /**
     * Konvertiert die Tumble-Optionen in eine MinigameServerConfig
     */
    public wily.legacy.minigame.MinigameServerConfig toMinigameConfig(int maxPlayers, int minPlayers) {
        return new wily.legacy.minigame.MinigameServerConfig(
            "TUMBLE",
            maxPlayers,
            minPlayers
        );
    }
}

