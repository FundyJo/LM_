package wily.legacy.client.screen.minigame;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.*;
import wily.legacy.client.screen.compat.FriendsServerRenderableList;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.HashSet;
import java.util.List;


public class PlayMinigameScreen extends PanelVListScreen implements ControlTooltip.Event, TabList.Access {
    public final MinigameRenderableList createMinigameList = new MinigameRenderableList(accessor, true);
    public final MinigameRenderableList joinMinigameList = new MinigameRenderableList(accessor, false);
    protected final Panel panelRecess;
    protected final ServerRenderableList friendsRenderableList = PublishScreen.hasWorldHost() ? new FriendsServerRenderableList(accessor) : new ServerRenderableList(accessor);
    protected final TabList tabList = new TabList(accessor)
        .add(LegacyTabButton.Type.LEFT, Component.translatable("legacy.menu.create"), b -> repositionElements())
        .add(LegacyTabButton.Type.MIDDLE, Component.translatable("legacy.menu.join"), b -> repositionElements())
        .add(LegacyTabButton.Type.RIGHT, (t, guiGraphics, i, j, f) -> t.renderString(guiGraphics, font, canNotifyOnlineFriends() ? 0xFFFFFFFF : CommonColor.INVENTORY_GRAY_TEXT.get(), canNotifyOnlineFriends()), Component.translatable("legacy.menu.friends"), b -> repositionElements());
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    public boolean isLoading = false;

    public PlayMinigameScreen(Screen parent, int initialTab) {
        super(s -> Panel.createPanel(s, p -> p.appearance(300, Math.min(256, s.height - 52)), p -> p.pos(p.centeredLeftPos(s), p.centeredTopPos(s) + (UIAccessor.of(s).getBoolean("hasTabList", true) ? 12 : 0))), Component.translatable("legacy.menu.minigames"));
        panelRecess = Panel.createPanel(this, p -> p.appearance(LegacySprites.PANEL_RECESS, panel.width - 18, panel.height - 18), p -> p.pos(panel.x + 9, panel.y + 9));
        this.parent = parent;
        renderableVLists.clear();
        renderableVLists.add(createMinigameList);
        renderableVLists.add(joinMinigameList);
        renderableVLists.add(friendsRenderableList);
        tabList.setSelected(initialTab);
    }

    public PlayMinigameScreen(Screen parent) {
        this(parent, 0);
    }

    protected boolean canNotifyOnlineFriends() {
        return friendsRenderableList.hasOnlineFriends() && Util.getMillis() % 1000 < 500;
    }

    public boolean hasTabList() {
        return accessor.getBoolean("hasTabList", true);
    }

    @Override
    public TabList getTabList() {
        return tabList;
    }

    @Override
    public void added() {
        super.added();
        friendsRenderableList.added();
    }

    @Override
    protected void init() {
        if (hasTabList()) addWidget(tabList);
        panel.init();
        panelRecess.init("panelRecess");
        renderableVListInit();
        if (hasTabList()) tabList.init(panel.x, panel.y - 25, panel.width, 31);
    }

    @Override
    public void renderableVListInit() {
        initRenderableVListHeight(30);
        getRenderableVList().init(panel.x + 15, panel.y + 15, panel.width - 30, panel.height - 30);
        if (!hasTabList())
            friendsRenderableList.init("friendsRenderableVList", panel.x + 15, panel.y + 15, panel.width - 30, panel.height - 30);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, false);
        if (hasTabList()) tabList.render(guiGraphics, i, j, f);
        panel.render(guiGraphics, i, j, f);
        tabList.renderSelected(guiGraphics, i, j, f);
        panelRecess.render(guiGraphics, i, j, f);
        if (isLoading)
            LegacyRenderUtil.drawGenericLoading(guiGraphics, panelRecess.x + (panelRecess.width - 75) / 2, panelRecess.y + (panelRecess.height - 75) / 2);
    }

    @Override
    public RenderableVList getRenderableVList() {
        return getRenderableVLists().get(hasTabList() ? tabList.getIndex() : 0);
    }

    @Override
    public void removed() {
        friendsRenderableList.removed();
        this.pinger.removeAll();
    }

    @Override
    public void tick() {
        super.tick();
        List<LanServer> list = friendsRenderableList.lanServerList.takeDirtyServers();
        if (list != null) {
            if (friendsRenderableList.lanServers == null || !new HashSet<>(friendsRenderableList.lanServers).containsAll(list)) {
                friendsRenderableList.lanServers = list;
                friendsRenderableList.updateServers();
                rebuildWidgets();
            }
        }
        this.pinger.tick();
    }

    public ServerStatusPinger getPinger() {
        return this.pinger;
    }

    public ServerList getServers() {
        return friendsRenderableList.servers;
    }
}
