package net.techcable.minecraft.minimap_world_id;

import com.google.common.base.Verify;
import com.google.common.io.ByteStreams;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MinimapWorldId extends JavaPlugin implements Listener {
    private static final List<String> XAERO_CHANNELS = List.of(
            "xaeroworldmap:main",
            "xaerominimap:main"
    );

    @Override
    public void onEnable() {
        // all of these are unregistered automatically
        getServer().getPluginManager().registerEvents(this, this);
        for (String channel : XAERO_CHANNELS) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, channel);
        }
        for (World world : getServer().getWorlds()) {
            this.loadXaeroWorldId(world);
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        this.loadXaeroWorldId(event.getWorld());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        this.unloadXaeroWorldId(event.getWorld());
    }

    /// Wait until the client registers the channel to send them the world id.
    ///
    /// If we did this on the `PlayerJoinEvent`,
    /// we might send before the plugin registers the channel.
    /// This decision is based on the one in mc-xaero-map-spigot:
    /// <https://github.com/ChristopherHaws/mc-xaero-map-spigot/blob/554da7c/src/main/java/dev/chaws/xaero/map/spigot/XaeroMapPlugin.java#L46-L48
    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        if (XAERO_CHANNELS.contains(event.getChannel())) {
            this.sendXaeroWorldId(event.getPlayer(), event.getChannel());
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        for (String channel : XAERO_CHANNELS) {
            this.sendXaeroWorldId(event.getPlayer(), channel);
        }
    }

    /// A map from [World#getUID()] to xaero world ids.
    ///
    /// Necessary because xaero uses `int` for ids rather than [UUID].
    private final Map<UUID, Integer> xaeroWorldIds = new HashMap<>();

    private void unloadXaeroWorldId(World world) {
        Integer oldId = xaeroWorldIds.get(world.getUID());
        this.getLogger().fine(() -> "Unloading xaero world id for `" + world.getName() + "` (" + oldId + ")");
    }

    private int loadXaeroWorldId(World world) {
        return this.xaeroWorldIds.computeIfAbsent(world.getUID(), (_uid) -> {
            try {
                return this.readXaeroWorldId(world);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read xaero world id", e);
            }
        });
    }

    private static final Pattern XAERO_ID_FILE_PATTERN = Pattern.compile("id:(?<id>-?\\d+)");

    /// Read the world id from the file.
    ///
    /// Called by [#loadXaeroWorldId(World)] when the id is not cached.
    private int readXaeroWorldId(World world) throws IOException {
        // matches where the server-side mod stores the filere
        File idFile = new File(world.getWorldFolder(), "xaeromap.txt");
        this.getLogger().finer(
                () -> "Reading xaero world id for `" + world.getName() + "` from `" + idFile + "`"
        );
        final String existingText;
        try {
            existingText = Files.readString(idFile.toPath());
        } catch (FileNotFoundException | NoSuchFileException e) {
            int generatedId = new Random().nextInt();
            getLogger().info(() -> "Generated xaero world id for `" + world.getName() + "`: " + generatedId);
            String generatedText = "id:" + generatedId;
            Verify.verify(
                    XAERO_ID_FILE_PATTERN.matcher(generatedText).matches(),
                    "Generated text %s doesn't match pattern",
                    generatedText
            );
            Files.writeString(idFile.toPath(), "id:" + +generatedId);
            getLogger().fine(() -> "Saved xaero world id for `" + world.getName() + "` (" + generatedText + ")");
            return generatedId;
        }
        Matcher matcher = XAERO_ID_FILE_PATTERN.matcher(existingText.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Xaero minimap file `" + idFile + "` is badly formatted");
        }
        int existingId = Integer.parseInt(matcher.group("id"));
        this.getLogger().fine(() -> "Read xaero world id for `" + world.getName() + "`: " + existingId);
        return existingId;
    }

    public void sendXaeroWorldId(Player player, String channel) {
        int worldId = this.loadXaeroWorldId(player.getWorld());
        // https://github.com/ChristopherHaws/mc-xaero-map-spigot/blob/554da7c/src/main/java/dev/chaws/xaero/map/spigot/XaeroMapPlugin.java#L67-L73
        var bytes = ByteStreams.newDataOutput();
        bytes.writeByte(0);
        bytes.writeInt(worldId);
        player.sendPluginMessage(this, channel, bytes.toByteArray());
    }
}
