package io.nexstudios.slayer.listener;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.nexstudios.nexus.bukkit.hooks.mythicmobs.IMythicDeathEvent;
import io.nexstudios.slayer.NexSlayer;
import io.nexstudios.slayer.logic.SlayerService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MythicDeathEvent implements IMythicDeathEvent {

    @Override
    public void execute(MythicMobDeathEvent event) {

        SlayerService slayerService = NexSlayer.getInstance().getSlayerService();
        if (slayerService == null) {
            return;
        }

        if (!(event.getKiller() instanceof Player killer)) {
            return;
        }

        MythicMob mythicMob = event.getMobType();
        ActiveMob activeMob = event.getMob();
        if (mythicMob == null || activeMob == null || activeMob.getEntity() == null) {
            return;
        }

        // Bukkit-Entity aus ActiveMob holen
        if (!(activeMob.getEntity().getBukkitEntity() instanceof LivingEntity living)) {
            return;
        }

        // 1) MythicMob als normalen Slayer-Kill zählen (für mob: "mythicmobs:<id>")
        slayerService.handleMythicMobDeath(killer, mythicMob.getInternalName(), living.getLocation());

        // 2) Gleichzeitig prüfen, ob es ein Slayer-Boss ist
        slayerService.handleBossDeath(living);
    }

    @Override
    public void execute(EntityDamageByEntityEvent event, ActiveMob activeMob) {
        // Platz für spätere Features (InstantTarget, Teleport, etc.).
    }
}