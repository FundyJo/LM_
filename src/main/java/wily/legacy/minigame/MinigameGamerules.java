package wily.legacy.minigame;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Definiert Gamerules für Minigames.
 * Ermöglicht das automatische Setzen von Gamerules beim Start eines Minigames.
 *
 * <p>Beispiel-Nutzung:</p>
 * <pre>
 * MinigameGamerules.builder()
 *     .doMobSpawning(false)
 *     .doDaylightCycle(false)
 *     .doWeatherCycle(false)
 *     .keepInventory(true)
 *     .announceAdvancements(false)
 *     .build();
 * </pre>
 */
public class MinigameGamerules {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinigameGamerules.class);

    /**
     * Standard Lobby-Gamerules: Keine Mobs, kein Tag/Nacht-Zyklus, kein Wetter
     */
    public static final MinigameGamerules LOBBY = MinigameGamerules.builder()
        .doMobSpawning(false)
        .doDaylightCycle(false)
        .doWeatherCycle(false)
        .doFireTick(false)
        .mobGriefing(false)
        .keepInventory(true)
        .announceAdvancements(false)
        .doInsomnia(false)
        .doPatrolSpawning(false)
        .doTraderSpawning(false)
        .doWardenSpawning(false)
        .disableRaids(true)
        .showDeathMessages(false)
        .naturalRegeneration(true)
        .fallDamage(false)
        .fireDamage(false)
        .freezeDamage(false)
        .drowningDamage(false)
        .build();

    /**
     * Standard Battle-Gamerules: Keine Mobs, kein Tag/Nacht-Zyklus, PvP aktiv
     */
    public static final MinigameGamerules BATTLE = MinigameGamerules.builder()
        .doMobSpawning(false)
        .doDaylightCycle(false)
        .doWeatherCycle(false)
        .doFireTick(false)
        .mobGriefing(false)
        .keepInventory(false)
        .announceAdvancements(false)
        .doInsomnia(false)
        .doPatrolSpawning(false)
        .doTraderSpawning(false)
        .doWardenSpawning(false)
        .disableRaids(true)
        .showDeathMessages(true)
        .naturalRegeneration(false)
        .fallDamage(true)
        .fireDamage(true)
        .freezeDamage(true)
        .drowningDamage(true)
        .build();

    /**
     * Standard Tumble-Gamerules: Ähnlich wie Battle
     */
    public static final MinigameGamerules TUMBLE = BATTLE;

    /**
     * Standard Glide-Gamerules: Kein Schaden außer Void
     */
    public static final MinigameGamerules GLIDE = MinigameGamerules.builder()
        .doMobSpawning(false)
        .doDaylightCycle(false)
        .doWeatherCycle(false)
        .doFireTick(false)
        .mobGriefing(false)
        .keepInventory(true)
        .announceAdvancements(false)
        .doInsomnia(false)
        .doPatrolSpawning(false)
        .doTraderSpawning(false)
        .doWardenSpawning(false)
        .disableRaids(true)
        .showDeathMessages(true)
        .naturalRegeneration(true)
        .fallDamage(false)
        .fireDamage(false)
        .freezeDamage(false)
        .drowningDamage(false)
        .build();

    // Boolean Gamerules
    private final Boolean doMobSpawning;
    private final Boolean doDaylightCycle;
    private final Boolean doWeatherCycle;
    private final Boolean doFireTick;
    private final Boolean mobGriefing;
    private final Boolean keepInventory;
    private final Boolean announceAdvancements;
    private final Boolean doInsomnia;
    private final Boolean doPatrolSpawning;
    private final Boolean doTraderSpawning;
    private final Boolean doWardenSpawning;
    private final Boolean disableRaids;
    private final Boolean showDeathMessages;
    private final Boolean naturalRegeneration;
    private final Boolean fallDamage;
    private final Boolean fireDamage;
    private final Boolean freezeDamage;
    private final Boolean drowningDamage;
    private final Boolean doImmediateRespawn;
    private final Boolean forgiveDeadPlayers;
    private final Boolean universalAnger;
    private final Boolean blockExplosionDropDecay;
    private final Boolean mobExplosionDropDecay;
    private final Boolean tntExplosionDropDecay;
    private final Boolean doTileDrops;
    private final Boolean doEntityDrops;
    private final Boolean doLimitedCrafting;
    private final Boolean reducedDebugInfo;
    private final Boolean sendCommandFeedback;
    private final Boolean logAdminCommands;
    private final Boolean commandBlockOutput;
    private final Boolean spectatorsGenerateChunks;

    // Integer Gamerules
    private final Integer randomTickSpeed;
    private final Integer spawnRadius;
    private final Integer maxEntityCramming;
    private final Integer maxCommandChainLength;
    private final Integer playersSleepingPercentage;

    private MinigameGamerules(Builder builder) {
        this.doMobSpawning = builder.doMobSpawning;
        this.doDaylightCycle = builder.doDaylightCycle;
        this.doWeatherCycle = builder.doWeatherCycle;
        this.doFireTick = builder.doFireTick;
        this.mobGriefing = builder.mobGriefing;
        this.keepInventory = builder.keepInventory;
        this.announceAdvancements = builder.announceAdvancements;
        this.doInsomnia = builder.doInsomnia;
        this.doPatrolSpawning = builder.doPatrolSpawning;
        this.doTraderSpawning = builder.doTraderSpawning;
        this.doWardenSpawning = builder.doWardenSpawning;
        this.disableRaids = builder.disableRaids;
        this.showDeathMessages = builder.showDeathMessages;
        this.naturalRegeneration = builder.naturalRegeneration;
        this.fallDamage = builder.fallDamage;
        this.fireDamage = builder.fireDamage;
        this.freezeDamage = builder.freezeDamage;
        this.drowningDamage = builder.drowningDamage;
        this.doImmediateRespawn = builder.doImmediateRespawn;
        this.forgiveDeadPlayers = builder.forgiveDeadPlayers;
        this.universalAnger = builder.universalAnger;
        this.blockExplosionDropDecay = builder.blockExplosionDropDecay;
        this.mobExplosionDropDecay = builder.mobExplosionDropDecay;
        this.tntExplosionDropDecay = builder.tntExplosionDropDecay;
        this.doTileDrops = builder.doTileDrops;
        this.doEntityDrops = builder.doEntityDrops;
        this.doLimitedCrafting = builder.doLimitedCrafting;
        this.reducedDebugInfo = builder.reducedDebugInfo;
        this.sendCommandFeedback = builder.sendCommandFeedback;
        this.logAdminCommands = builder.logAdminCommands;
        this.commandBlockOutput = builder.commandBlockOutput;
        this.spectatorsGenerateChunks = builder.spectatorsGenerateChunks;
        this.randomTickSpeed = builder.randomTickSpeed;
        this.spawnRadius = builder.spawnRadius;
        this.maxEntityCramming = builder.maxEntityCramming;
        this.maxCommandChainLength = builder.maxCommandChainLength;
        this.playersSleepingPercentage = builder.playersSleepingPercentage;
    }

    /**
     * Wendet alle gesetzten Gamerules auf das Level an.
     */
    public void apply(ServerLevel level) {
        GameRules gameRules = level.getGameRules();

        // Boolean Rules
        applyBooleanRule(gameRules, GameRules.RULE_DOMOBSPAWNING, doMobSpawning);
        applyBooleanRule(gameRules, GameRules.RULE_DAYLIGHT, doDaylightCycle);
        applyBooleanRule(gameRules, GameRules.RULE_WEATHER_CYCLE, doWeatherCycle);
        applyBooleanRule(gameRules, GameRules.RULE_DOFIRETICK, doFireTick);
        applyBooleanRule(gameRules, GameRules.RULE_MOBGRIEFING, mobGriefing);
        applyBooleanRule(gameRules, GameRules.RULE_KEEPINVENTORY, keepInventory);
        applyBooleanRule(gameRules, GameRules.RULE_ANNOUNCE_ADVANCEMENTS, announceAdvancements);
        applyBooleanRule(gameRules, GameRules.RULE_DOINSOMNIA, doInsomnia);
        applyBooleanRule(gameRules, GameRules.RULE_DO_PATROL_SPAWNING, doPatrolSpawning);
        applyBooleanRule(gameRules, GameRules.RULE_DO_TRADER_SPAWNING, doTraderSpawning);
        applyBooleanRule(gameRules, GameRules.RULE_DO_WARDEN_SPAWNING, doWardenSpawning);
        applyBooleanRule(gameRules, GameRules.RULE_DISABLE_RAIDS, disableRaids);
        applyBooleanRule(gameRules, GameRules.RULE_SHOWDEATHMESSAGES, showDeathMessages);
        applyBooleanRule(gameRules, GameRules.RULE_NATURAL_REGENERATION, naturalRegeneration);
        applyBooleanRule(gameRules, GameRules.RULE_FALL_DAMAGE, fallDamage);
        applyBooleanRule(gameRules, GameRules.RULE_FIRE_DAMAGE, fireDamage);
        applyBooleanRule(gameRules, GameRules.RULE_FREEZE_DAMAGE, freezeDamage);
        applyBooleanRule(gameRules, GameRules.RULE_DROWNING_DAMAGE, drowningDamage);
        applyBooleanRule(gameRules, GameRules.RULE_DO_IMMEDIATE_RESPAWN, doImmediateRespawn);
        applyBooleanRule(gameRules, GameRules.RULE_FORGIVE_DEAD_PLAYERS, forgiveDeadPlayers);
        applyBooleanRule(gameRules, GameRules.RULE_UNIVERSAL_ANGER, universalAnger);
        applyBooleanRule(gameRules, GameRules.RULE_BLOCK_EXPLOSION_DROP_DECAY, blockExplosionDropDecay);
        applyBooleanRule(gameRules, GameRules.RULE_MOB_EXPLOSION_DROP_DECAY, mobExplosionDropDecay);
        applyBooleanRule(gameRules, GameRules.RULE_TNT_EXPLOSION_DROP_DECAY, tntExplosionDropDecay);
        applyBooleanRule(gameRules, GameRules.RULE_DOBLOCKDROPS, doTileDrops);
        applyBooleanRule(gameRules, GameRules.RULE_DOMOBLOOT, doEntityDrops);
        applyBooleanRule(gameRules, GameRules.RULE_LIMITED_CRAFTING, doLimitedCrafting);
        applyBooleanRule(gameRules, GameRules.RULE_REDUCEDDEBUGINFO, reducedDebugInfo);
        applyBooleanRule(gameRules, GameRules.RULE_SENDCOMMANDFEEDBACK, sendCommandFeedback);
        applyBooleanRule(gameRules, GameRules.RULE_LOGADMINCOMMANDS, logAdminCommands);
        applyBooleanRule(gameRules, GameRules.RULE_COMMANDBLOCKOUTPUT, commandBlockOutput);
        applyBooleanRule(gameRules, GameRules.RULE_SPECTATORSGENERATECHUNKS, spectatorsGenerateChunks);

        // Integer Rules
        applyIntRule(gameRules, GameRules.RULE_RANDOMTICKING, randomTickSpeed);
        applyIntRule(gameRules, GameRules.RULE_SPAWN_RADIUS, spawnRadius);
        applyIntRule(gameRules, GameRules.RULE_MAX_ENTITY_CRAMMING, maxEntityCramming);
        applyIntRule(gameRules, GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT, maxCommandChainLength);
        applyIntRule(gameRules, GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE, playersSleepingPercentage);

        LOGGER.info("✅ Applied minigame gamerules to level: {}", level.dimension().location());
    }

    private void applyBooleanRule(GameRules gameRules, GameRules.Key<GameRules.BooleanValue> key, Boolean value) {
        if (value != null) {
            gameRules.getRule(key).set(value, null);
        }
    }

    private void applyIntRule(GameRules gameRules, GameRules.Key<GameRules.IntegerValue> key, Integer value) {
        if (value != null) {
            gameRules.getRule(key).set(value, null);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Boolean doMobSpawning;
        private Boolean doDaylightCycle;
        private Boolean doWeatherCycle;
        private Boolean doFireTick;
        private Boolean mobGriefing;
        private Boolean keepInventory;
        private Boolean announceAdvancements;
        private Boolean doInsomnia;
        private Boolean doPatrolSpawning;
        private Boolean doTraderSpawning;
        private Boolean doWardenSpawning;
        private Boolean disableRaids;
        private Boolean showDeathMessages;
        private Boolean naturalRegeneration;
        private Boolean fallDamage;
        private Boolean fireDamage;
        private Boolean freezeDamage;
        private Boolean drowningDamage;
        private Boolean doImmediateRespawn;
        private Boolean forgiveDeadPlayers;
        private Boolean universalAnger;
        private Boolean blockExplosionDropDecay;
        private Boolean mobExplosionDropDecay;
        private Boolean tntExplosionDropDecay;
        private Boolean doTileDrops;
        private Boolean doEntityDrops;
        private Boolean doLimitedCrafting;
        private Boolean reducedDebugInfo;
        private Boolean sendCommandFeedback;
        private Boolean logAdminCommands;
        private Boolean commandBlockOutput;
        private Boolean spectatorsGenerateChunks;
        private Integer randomTickSpeed;
        private Integer spawnRadius;
        private Integer maxEntityCramming;
        private Integer maxCommandChainLength;
        private Integer playersSleepingPercentage;

        public Builder doMobSpawning(boolean value) {
            this.doMobSpawning = value;
            return this;
        }

        public Builder doDaylightCycle(boolean value) {
            this.doDaylightCycle = value;
            return this;
        }

        public Builder doWeatherCycle(boolean value) {
            this.doWeatherCycle = value;
            return this;
        }

        public Builder doFireTick(boolean value) {
            this.doFireTick = value;
            return this;
        }

        public Builder mobGriefing(boolean value) {
            this.mobGriefing = value;
            return this;
        }

        public Builder keepInventory(boolean value) {
            this.keepInventory = value;
            return this;
        }

        public Builder announceAdvancements(boolean value) {
            this.announceAdvancements = value;
            return this;
        }

        public Builder doInsomnia(boolean value) {
            this.doInsomnia = value;
            return this;
        }

        public Builder doPatrolSpawning(boolean value) {
            this.doPatrolSpawning = value;
            return this;
        }

        public Builder doTraderSpawning(boolean value) {
            this.doTraderSpawning = value;
            return this;
        }

        public Builder doWardenSpawning(boolean value) {
            this.doWardenSpawning = value;
            return this;
        }

        public Builder disableRaids(boolean value) {
            this.disableRaids = value;
            return this;
        }

        public Builder showDeathMessages(boolean value) {
            this.showDeathMessages = value;
            return this;
        }

        public Builder naturalRegeneration(boolean value) {
            this.naturalRegeneration = value;
            return this;
        }

        public Builder fallDamage(boolean value) {
            this.fallDamage = value;
            return this;
        }

        public Builder fireDamage(boolean value) {
            this.fireDamage = value;
            return this;
        }

        public Builder freezeDamage(boolean value) {
            this.freezeDamage = value;
            return this;
        }

        public Builder drowningDamage(boolean value) {
            this.drowningDamage = value;
            return this;
        }

        public Builder doImmediateRespawn(boolean value) {
            this.doImmediateRespawn = value;
            return this;
        }

        public Builder forgiveDeadPlayers(boolean value) {
            this.forgiveDeadPlayers = value;
            return this;
        }

        public Builder universalAnger(boolean value) {
            this.universalAnger = value;
            return this;
        }

        public Builder blockExplosionDropDecay(boolean value) {
            this.blockExplosionDropDecay = value;
            return this;
        }

        public Builder mobExplosionDropDecay(boolean value) {
            this.mobExplosionDropDecay = value;
            return this;
        }

        public Builder tntExplosionDropDecay(boolean value) {
            this.tntExplosionDropDecay = value;
            return this;
        }

        public Builder doTileDrops(boolean value) {
            this.doTileDrops = value;
            return this;
        }

        public Builder doEntityDrops(boolean value) {
            this.doEntityDrops = value;
            return this;
        }

        public Builder doLimitedCrafting(boolean value) {
            this.doLimitedCrafting = value;
            return this;
        }

        public Builder reducedDebugInfo(boolean value) {
            this.reducedDebugInfo = value;
            return this;
        }

        public Builder sendCommandFeedback(boolean value) {
            this.sendCommandFeedback = value;
            return this;
        }

        public Builder logAdminCommands(boolean value) {
            this.logAdminCommands = value;
            return this;
        }

        public Builder commandBlockOutput(boolean value) {
            this.commandBlockOutput = value;
            return this;
        }

        public Builder spectatorsGenerateChunks(boolean value) {
            this.spectatorsGenerateChunks = value;
            return this;
        }

        public Builder randomTickSpeed(int value) {
            this.randomTickSpeed = value;
            return this;
        }

        public Builder spawnRadius(int value) {
            this.spawnRadius = value;
            return this;
        }

        public Builder maxEntityCramming(int value) {
            this.maxEntityCramming = value;
            return this;
        }

        public Builder maxCommandChainLength(int value) {
            this.maxCommandChainLength = value;
            return this;
        }

        public Builder playersSleepingPercentage(int value) {
            this.playersSleepingPercentage = value;
            return this;
        }

        public MinigameGamerules build() {
            return new MinigameGamerules(this);
        }
    }
}

