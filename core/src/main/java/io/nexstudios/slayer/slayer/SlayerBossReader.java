package io.nexstudios.slayer.slayer;

import io.nexstudios.nexus.bukkit.files.NexusFileReader;
import io.nexstudios.nexus.bukkit.utils.NexusLogger;
import io.nexstudios.slayer.slayer.models.SlayerBoss;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class SlayerBossReader {

    private NexusFileReader reader;
    private final NexusLogger logger;
    private final HashMap<String, SlayerBoss> bosses = new HashMap<>();

    public SlayerBossReader(NexusFileReader reader, NexusLogger logger) {
        this.reader = reader;
        this.logger = logger;
    }

    public void read(NexusFileReader bossFiles) {
        this.reader = bossFiles;
        bosses.clear();
        for (File file : reader.getFiles()) {
            String id = file.getName().replace(".yml", "");
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                SlayerBoss boss = readSingle(config, id);
                bosses.put(id, boss);
                logger.info("Loaded boss <dark_purple>" + id + "<reset> successfully.");
            } catch (Exception e) {
                logger.error("Failed to load boss <dark_purple>" + id + "<reset>: " + e.getMessage());
            }
        }
        logger.info("Successfully loaded <dark_purple>" + bosses.size() + "<reset> boss(es).");
    }

    public Map<String, SlayerBoss> getBosses() {
        return Collections.unmodifiableMap(bosses);
    }

    public SlayerBoss getBossById(String id) {
        return bosses.get(id);
    }

    private SlayerBoss readSingle(YamlConfiguration config, String id) {
        SlayerBoss boss = new SlayerBoss();
        boss.setId(id);
        boss.setConfiguration(config);

        boss.setType(config.getString("type", "minecraft:zombie"));
        boss.setName(config.getString("name", id));

        // settings
        ConfigurationSection settingsSec = config.getConfigurationSection("settings");
        if (settingsSec != null) {
            boss.setSettings(readSettings(settingsSec));
        } else {
            boss.setSettings(null);
            logger.info("No 'settings' section found for boss <dark_purple>" + id + "<reset> (this section is optional).");
        }

        // hologram
        ConfigurationSection holoSec = config.getConfigurationSection("hologram");
        if (holoSec != null) {
            boss.setHologram(readHologram(holoSec));
        } else {
            boss.setHologram(null);
            logger.warning("No 'hologram' section found for boss <dark_purple>" + id + "<reset>.");
        }

        return boss;
    }

    private SlayerBoss.Settings readSettings(ConfigurationSection sec) {
        SlayerBoss.Settings s = new SlayerBoss.Settings();
        if (sec.contains("scale")) s.setScale(sec.getDouble("scale"));
        if (sec.contains("health")) s.setHealth(sec.getDouble("health"));
        if (sec.contains("damage")) s.setDamage(sec.getDouble("damage"));
        if (sec.contains("speed")) s.setSpeed(sec.getDouble("speed"));
        if (sec.contains("armor")) s.setArmor(sec.getDouble("armor"));
        if (sec.contains("armor-toughness")) s.setArmorToughness(sec.getDouble("armor-toughness"));
        if (sec.contains("knockback-resistance")) s.setKnockbackResistance(sec.getDouble("knockback-resistance"));
        if (sec.contains("no-damage-ticks")) s.setNoDamageTicks(sec.getInt("no-damage-ticks"));
        if (sec.contains("aggressive")) s.setAggressive(sec.getBoolean("aggressive"));
        if (sec.contains("baby")) s.setBaby(sec.getBoolean("baby"));
        if (sec.contains("disable-drops")) s.setDisableDrops(sec.getBoolean("disable-drops", false));

        ConfigurationSection equipSec = sec.getConfigurationSection("equipment");
        if (equipSec != null) {
            SlayerBoss.Settings.Equipment eq = new SlayerBoss.Settings.Equipment();
            eq.setHead(equipSec.getString("head", null));
            eq.setBody(equipSec.getString("body", null));
            eq.setLegs(equipSec.getString("legs", null));
            eq.setFeet(equipSec.getString("feet", null));
            eq.setHand(equipSec.getString("hand", null));
            eq.setOffHand(equipSec.getString("offHand", null));
            s.setEquipment(eq);
        }

        return s;
    }

    private SlayerBoss.Hologram readHologram(ConfigurationSection sec) {
        SlayerBoss.Hologram h = new SlayerBoss.Hologram();
        if (sec.contains("enabled")) h.setEnabled(sec.getBoolean("enabled"));

        ConfigurationSection setSec = sec.getConfigurationSection("settings");
        if (setSec != null) {
            SlayerBoss.Hologram.HologramSettings hs = new SlayerBoss.Hologram.HologramSettings();
            hs.setBillboard(setSec.getString("billboard", null));
            hs.setBackgroundColor(setSec.getString("background-color", null));

            ConfigurationSection sizeSec = setSec.getConfigurationSection("size");
            if (sizeSec != null) {
                SlayerBoss.Hologram.HologramSettings.Size size = new SlayerBoss.Hologram.HologramSettings.Size();
                if (sizeSec.contains("x")) size.setX(sizeSec.getDouble("x", 0));
                if (sizeSec.contains("y")) size.setY(sizeSec.getDouble("y", 0));
                if (sizeSec.contains("z")) size.setZ(sizeSec.getDouble("z", 0));
                hs.setSize(size);
            }

            ConfigurationSection offsetSec = setSec.getConfigurationSection("offset");
            if (offsetSec != null) {
                SlayerBoss.Hologram.HologramSettings.Size offset = new SlayerBoss.Hologram.HologramSettings.Size();
                if (offsetSec.contains("x")) offset.setX(offsetSec.getDouble("x", 0));
                if (offsetSec.contains("y")) offset.setY(offsetSec.getDouble("y", 0));
                if (offsetSec.contains("z")) offset.setZ(offsetSec.getDouble("z", 0));
                hs.setOffset(offset);
            }

            if (setSec.contains("view-range")) hs.setViewRange(setSec.getInt("view-range", 75));
            if (setSec.contains("see-through")) hs.setSeeThrough(setSec.getBoolean("see-through", false));
            if (setSec.contains("line-width")) hs.setLineWidth(setSec.getDouble("line-width", 150));
            if (setSec.contains("update-interval")) hs.setUpdateInterval(setSec.getInt("update-interval", 20));
            h.setSettings(hs);
        }

        h.setLines(sec.getStringList("lines"));
        return h;
    }
}