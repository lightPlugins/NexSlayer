package io.nexstudios.slayer.logic;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.hologram.NexHologram;
import io.nexstudios.nexus.bukkit.hologram.NexHologramService;
import io.nexstudios.nexus.bukkit.hooks.mythicmobs.MythicMobsHook;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import io.nexstudios.slayer.NexSlayer;
import io.nexstudios.slayer.slayer.models.SlayerBoss;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
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

        String[] split = boss.getType().split(":");

        if(split.length != 2) {
            NexSlayer.nexusLogger.warning(List.of(
                    "Something went wrong with the boss spawn from boss id " + boss.getId(),
                    "Boss type " + boss.getType() + " is not in format <namespace>:<id>"
            ));
            return null;
        }

        if(split[0].equalsIgnoreCase("mythicmobs")) {
            if(NexusPlugin.getInstance().getMythicMobsHook() != null) {
                LivingEntity le = MythicMobsHook.spawnMythicMob(boss.getId(), location);
                applyHolo(boss, location, player, le);
                return le;
            }
        }

        if(!split[0].equalsIgnoreCase("minecraft")) {
            NexSlayer.nexusLogger.warning(List.of(
                    "Something went wrong with the boss spawn from boss id " + boss.getId(),
                    "Boss type " + boss.getType() + " is not a valid namespace:key value!"
            ));
            return null;
        }


        EntityType type = EntityType.valueOf(split[1]);

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
                .disableDrops(boss.getSettings().getDisableDrops())
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

        return le;
    }

    private void applyHolo(SlayerBoss boss, Location location, Player player, LivingEntity le) {

        NexHologramService.Handle bossHolo = NexusPlugin.getInstance().nexHoloService.register(
                new NexHologramService.Spec()
                        .base(location.add(
                                boss.getHologram().getSettings().getOffset().getX(),
                                boss.getHologram().getSettings().getOffset().getY(),
                                boss.getHologram().getSettings().getOffset().getZ()
                        ))
                        .perPlayer(p -> {
                            // Zeit über SlayerService (ActiveSlayer.bossDeadline)
                            int remainingTime = NexSlayer.getInstance()
                                    .getSlayerService()
                                    .getRemainingBossTimeSeconds(p.getUniqueId());

                            if (remainingTime < 0) {
                                remainingTime = 0;
                            }

                            int minutes = remainingTime / 60;
                            int seconds = remainingTime % 60;
                            String formattedTime = String.format("%d:%02d", minutes, seconds);

                            double currentHealth = le.getHealth();
                            double maxHealth = boss.getSettings().getHealth();

                            TagResolver resolver = TagResolver.resolver(
                                    Placeholder.parsed("time", formattedTime),
                                    Placeholder.parsed("player", p.getName()),
                                    Placeholder.parsed("current-health", String.format("%.0f", currentHealth)),
                                    Placeholder.parsed("max-health", String.format("%.0f", maxHealth))
                            );

                            List<Component> lines = new ArrayList<>();
                            for (String rawLine : boss.getHologram().getLines()) {
                                Component line = MiniMessage.miniMessage().deserialize(rawLine, resolver);
                                lines.add(line);
                            }
                            return lines;
                        })
                        // Update-Intervall aus der Boss-Konfiguration
                        .refreshTicks(boss.getHologram().getSettings().getUpdateInterval())
                        .attachTo(le)
        );
    }
}
