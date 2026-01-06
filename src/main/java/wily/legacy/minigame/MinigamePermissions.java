package wily.legacy.minigame;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;

/**
 * Definiert Berechtigungen für Item/Block-Nutzung in Minigames.
 * Unterstützt Whitelist- und Blacklist-Modus.
 *
 * <p>Beispiel-Nutzung:</p>
 * <pre>
 * // Whitelist: Nur diese Items erlaubt
 * MinigamePermissions.builder()
 *     .whitelist()
 *     .addItem(Items.BOW)
 *     .addItem(Items.ARROW)
 *     .addItem(Items.FISHING_ROD)
 *     .build();
 *
 * // Blacklist: Alles erlaubt außer diese
 * MinigamePermissions.builder()
 *     .blacklist()
 *     .addItem(Items.TNT)
 *     .addItem(Items.FLINT_AND_STEEL)
 *     .build();
 * </pre>
 */
public class MinigamePermissions {

    /**
     * Erlaubt alles (keine Einschränkungen).
     */
    public static final MinigamePermissions ALLOW_ALL = new MinigamePermissions(Mode.BLACKLIST, Set.of(), Set.of());

    /**
     * Verbietet alles.
     */
    public static final MinigamePermissions DENY_ALL = new MinigamePermissions(Mode.WHITELIST, Set.of(), Set.of());

    public enum Mode {
        /** Nur Items/Blöcke in der Liste sind erlaubt */
        WHITELIST,
        /** Alles ist erlaubt außer Items/Blöcke in der Liste */
        BLACKLIST
    }

    private final Mode mode;
    private final Set<ResourceLocation> allowedItems;
    private final Set<ResourceLocation> allowedBlocks;

    private MinigamePermissions(Mode mode, Set<ResourceLocation> allowedItems, Set<ResourceLocation> allowedBlocks) {
        this.mode = mode;
        this.allowedItems = Set.copyOf(allowedItems);
        this.allowedBlocks = Set.copyOf(allowedBlocks);
    }

    /**
     * Prüft ob ein Item verwendet werden darf.
     */
    public boolean isItemAllowed(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return isAllowed(id, allowedItems);
    }

    /**
     * Prüft ob ein Block interagiert werden darf.
     */
    public boolean isBlockAllowed(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return isAllowed(id, allowedBlocks);
    }

    private boolean isAllowed(ResourceLocation id, Set<ResourceLocation> set) {
        if (mode == Mode.WHITELIST) {
            return set.contains(id);
        } else {
            return !set.contains(id);
        }
    }

    public Mode getMode() {
        return mode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Mode mode = Mode.BLACKLIST;
        private final Set<ResourceLocation> items = new HashSet<>();
        private final Set<ResourceLocation> blocks = new HashSet<>();

        /**
         * Setzt den Modus auf Whitelist (nur gelistete erlaubt).
         */
        public Builder whitelist() {
            this.mode = Mode.WHITELIST;
            return this;
        }

        /**
         * Setzt den Modus auf Blacklist (alles außer gelistete erlaubt).
         */
        public Builder blacklist() {
            this.mode = Mode.BLACKLIST;
            return this;
        }

        /**
         * Fügt ein Item zur Liste hinzu.
         */
        public Builder addItem(Item item) {
            items.add(BuiltInRegistries.ITEM.getKey(item));
            return this;
        }

        /**
         * Fügt ein Item per ResourceLocation hinzu.
         */
        public Builder addItem(ResourceLocation id) {
            items.add(id);
            return this;
        }

        /**
         * Fügt ein Item per String-ID hinzu (z.B. "minecraft:bow").
         */
        public Builder addItem(String id) {
            items.add(ResourceLocation.parse(id));
            return this;
        }

        /**
         * Fügt einen Block zur Liste hinzu.
         */
        public Builder addBlock(Block block) {
            blocks.add(BuiltInRegistries.BLOCK.getKey(block));
            return this;
        }

        /**
         * Fügt einen Block per ResourceLocation hinzu.
         */
        public Builder addBlock(ResourceLocation id) {
            blocks.add(id);
            return this;
        }

        /**
         * Fügt einen Block per String-ID hinzu (z.B. "minecraft:oak_door").
         */
        public Builder addBlock(String id) {
            blocks.add(ResourceLocation.parse(id));
            return this;
        }

        public MinigamePermissions build() {
            return new MinigamePermissions(mode, items, blocks);
        }
    }
}

