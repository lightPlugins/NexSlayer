package io.nexstudios.slayer.listener;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.nexstudios.nexus.bukkit.hooks.mythicmobs.IMythicDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MythicDeathEvent implements IMythicDeathEvent {


    @Override
    public void execute(MythicMobDeathEvent mythicMobDeathEvent) {

    }

    @Override
    public void execute(EntityDamageByEntityEvent entityDamageByEntityEvent, ActiveMob activeMob) {

    }
}
