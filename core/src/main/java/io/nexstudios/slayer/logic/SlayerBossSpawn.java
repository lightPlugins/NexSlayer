package io.nexstudios.slayer.logic;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.hologram.NexHologramService;
import io.nexstudios.nexus.bukkit.hooks.mythicmobs.MythicMobsHook;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import io.nexstudios.slayer.NexSlayer;
import io.nexstudios.slayer.slayer.models.SlayerBoss;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SlayerBossSpawn {

    public SlayerBossSpawn() {}

    @Nullable
    public LivingEntity spawnBoss(Location location, Player player, SlayerBoss boss) {

        if(NexusPlugin.getInstance().getMythicMobsHook() != null) {
            LivingEntity le = MythicMobsHook.spawnMythicMob(boss.getId(), location);
            applyHolo(boss, location, player, le);
            return le;
        }

        EntityType type = EntityType.valueOf(boss.getType());

        if(!type.isSpawnable()) {
            NexSlayer.nexusLogger.warning(List.of(
                    "Something went wrong with the boss spawn from boss id " + boss.getId(),
                    "Boss type " + boss.getType() + " is not spawnable"
            ));
            return null;
        }

        LivingEntity le = NexServices.newMobBuilder()
                .entity(type)
                .health(boss.getSettings().getHealth())
                .noDamageTicks(boss.getSettings().getNoDamageTicks())
                .damage(boss.getSettings().getDamage())
                .aggressive(boss.getSettings().getAggressive())
                .baby(boss.getSettings().getBaby())
                .speed(boss.getSettings().getSpeed())
                .scale(boss.getSettings().getScale())
                .armor(boss.getSettings().getArmor())
                .armorToughness(boss.getSettings().getArmorToughness())
                .helm(StringUtils.parseItem(boss.getSettings().getEquipment().getHead()))
                .chest(StringUtils.parseItem(boss.getSettings().getEquipment().getBody()))
                .legs(StringUtils.parseItem(boss.getSettings().getEquipment().getLegs()))
                .boots(StringUtils.parseItem(boss.getSettings().getEquipment().getFeet()))
                .knockbackResistance(boss.getSettings().getKnockbackResistance())
                .spawn(location, player);

        applyHolo(boss, location, player, le);

        return null;
    }

    private void applyHolo(SlayerBoss boss, Location location, Player player, LivingEntity le) {
        List<Component> holoLines = new ArrayList<>();

        for(String line : boss.getHologram().getLines()) {
            holoLines.add(NexSlayer.getInstance().getMessageSender().stringToComponent(player, line));
        }

        NexHologramService.Handle bossHolo = NexusPlugin.getInstance().nexHoloService.register(
                new NexHologramService.Spec()
                        .base(location.add(
                                boss.getHologram().getSettings().getOffset().getX(),
                                boss.getHologram().getSettings().getOffset().getY(),
                                boss.getHologram().getSettings().getOffset().getZ()))
                        .perPlayer(p -> holoLines)
                        .refreshTicks(10)
                        .attachTo(le)
        );
    }



}
