package io.nexstudios.slayer.logic;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.hooks.mythicmobs.MythicMobsHook;
import io.nexstudios.slayer.NexSlayer;
import io.nexstudios.slayer.listener.IVanillaDeathEvent;
import io.nexstudios.slayer.listener.MythicDeathEvent;
import io.nexstudios.slayer.listener.VanillaDeathEvent;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SlayerFactory implements Listener {

    public static final List<IVanillaDeathEvent> vanillaDeathEvents = new ArrayList<>();

    /**
     * Zentraler Service, auf den u.a. der SlayerCommand zugreift.
     */
    private final SlayerService slayerService;

    public SlayerFactory(Plugin plugin, SlayerService slayerService) {
        this.slayerService = slayerService;
        registerEvents();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void registerEvents() {
        // MythicMobs-Hook, falls vorhanden
        if (NexusPlugin.getInstance().getMythicMobsHook() != null) {
            MythicMobsHook.registerMythicDeathEvent(new MythicDeathEvent());
            NexSlayer.nexusLogger.info("Successfully registered MythicMobs events.");
        }

        // Vanilla-Death-Events
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

    /**
     * Nur der Spieler, dem der Boss gehört, darf ihn hitten.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        // Ist das überhaupt ein Slayer-Boss?
        SlayerService.ActiveSlayer active = slayerService.getActiveSlayerByBossUuid(living.getUniqueId());
        if (active == null) {
            return; // kein getrackter Boss -> nichts tun
        }

        // Angreifenden Spieler bestimmen (direkt oder Projektil-Schütze)
        Player damagerPlayer = null;

        if (event.getDamager() instanceof Player p) {
            damagerPlayer = p;
        } else if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player p) {
                damagerPlayer = p;
            }
        }

        // prevent damage on boss by other damage sources
        if (damagerPlayer == null) {
            return;
        }

        // Prüfen, ob der Boss diesem Spieler gehört
        if (!active.getPlayerId().equals(damagerPlayer.getUniqueId())) {
            // falscher Spieler -> Schaden canceln
            event.setCancelled(true);
            damagerPlayer.sendMessage("§c[Slayer] §7Du kannst diesen Boss nicht angreifen.");
        }
    }
}