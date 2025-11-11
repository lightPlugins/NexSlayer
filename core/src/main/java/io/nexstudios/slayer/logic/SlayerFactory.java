package io.nexstudios.slayer.logic;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.hooks.mythicmobs.MythicMobsHook;
import io.nexstudios.slayer.NexSlayer;
import io.nexstudios.slayer.listener.IVanillaDeathEvent;
import io.nexstudios.slayer.listener.MythicDeathEvent;
import io.nexstudios.slayer.listener.VanillaDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class SlayerFactory implements Listener {

    public static List<IVanillaDeathEvent> vanillaDeathEvents = new ArrayList<>();

    public SlayerFactory(Plugin plugin) {
        registerEvents();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

    }

    private void registerEvents() {
        if(NexusPlugin.getInstance().getMythicMobsHook() != null) {
            MythicMobsHook.registerMythicDeathEvent(new MythicDeathEvent());
            NexSlayer.nexusLogger.info("Successfully registered MythicMobs events.");
        }

        registerVanillaDeathEvent(new VanillaDeathEvent());
    }

    public static void registerVanillaDeathEvent(IVanillaDeathEvent event) {
        NexSlayer.nexusLogger.info("Successfully registered VanillaDeathEvents.");
        vanillaDeathEvents.add(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVanillaDeath(EntityDeathEvent event) {
        vanillaDeathEvents.forEach(single -> single.execute(event));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

    }

}
