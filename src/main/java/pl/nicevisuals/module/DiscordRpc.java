package pl.nicevisuals.module;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import pl.nicevisuals.discord.DiscordIpc;
import pl.nicevisuals.setting.BoolSetting;

/** Shows the client name, current server and play time on your Discord profile (Rich Presence). */
public class DiscordRpc extends Module {
    private final BoolSetting showServer = add(new BoolSetting("Show server", true));
    private final BoolSetting showTime = add(new BoolSetting("Show time", true));
    private int ticks;

    public DiscordRpc() { super("Discord RPC", Category.CLIENT); }

    @Override public void onEnable() { ticks = 0; DiscordIpc.start(); }
    @Override public void onDisable() { DiscordIpc.stop(); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (ticks++ % 40 != 0) return;               // refresh every 2 seconds
        String details;
        ServerInfo si = mc.getCurrentServerEntry();
        if (mc.world == null) details = "W menu głównym";
        else if (si != null) details = showServer.value ? "Gra na serwerze " + si.address : "Gra multiplayer";
        else if (mc.isInSingleplayer()) details = "Gra solo";
        else details = "Gra multiplayer";
        String name = DiscordIpc.config().clientName;
        if (name == null || name.isBlank()) name = "Fire Client";
        String state = name + " • Minecraft " + SharedConstants.getGameVersion().getName();
        DiscordIpc.update(details, state, showTime.value);
    }
}
