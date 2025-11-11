package io.nexstudios.slayer.listener;

import org.bukkit.event.entity.EntityDeathEvent;

public interface IVanillaDeathEvent {
    void execute(EntityDeathEvent event);
}
