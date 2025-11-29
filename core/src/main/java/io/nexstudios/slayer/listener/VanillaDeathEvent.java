package io.nexstudios.slayer.listener;

import io.nexstudios.slayer.NexSlayer;
import io.nexstudios.slayer.logic.SlayerService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class VanillaDeathEvent implements IVanillaDeathEvent {

    @Override
    public void execute(EntityDeathEvent event) {

        SlayerService slayerService = NexSlayer.getInstance().getSlayerService();
        if (slayerService == null) {
            return;
        }

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        slayerService.handleVanillaMobDeath(killer, entity.getType(), entity.getLocation());
        slayerService.handleBossDeath(entity);
    }
}