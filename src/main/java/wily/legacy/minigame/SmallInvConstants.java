package wily.legacy.minigame;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public class SmallInvConstants {
	public static final int WO = 58;
	public static final int WOL = 70;

	public static boolean supportsSmallInventory(Object v) {
		return v instanceof InventoryScreen || v instanceof ContainerScreen;
	}
}