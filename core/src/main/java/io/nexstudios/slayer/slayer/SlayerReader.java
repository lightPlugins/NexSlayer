package io.nexstudios.slayer.slayer;

import io.nexstudios.nexus.bukkit.files.NexusFileReader;
import io.nexstudios.nexus.bukkit.utils.NexusLogger;
import io.nexstudios.slayer.slayer.models.Slayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class SlayerReader {

    private NexusFileReader reader;
    private final HashMap<String, Slayer> slayers = new HashMap<>();
    private final NexusLogger logger;

    public SlayerReader(NexusFileReader reader, NexusLogger logger) {
        this.reader = reader;
        this.logger = logger;
    }

    public void read(NexusFileReader slayerFiles) {
        this.reader = slayerFiles;
        slayers.clear();
        for (File file : reader.getFiles()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = file.getName().replace(".yml", "");
            Slayer slayer = readSingle(config, id);
            slayers.put(id, slayer);
            logger.info("Loaded slayer <dark_purple>" + id + "<reset> successfully.");
        }
        logger.info("Successfully loaded <dark_purple>" + slayers.size() + "<reset> slayer(s).");
    }

    public Map<String, Slayer> getSlayers() {
        return Collections.unmodifiableMap(slayers);
    }

    public Slayer getSlayerById(String id) {
        return slayers.get(id);
    }

    private Slayer readSingle(YamlConfiguration config, String id) {
        Slayer slayer = new Slayer();
        slayer.setId(id);
        slayer.setConfiguration(config);

        slayer.setName(config.getString("name", id));

        // Inventory
        Slayer.Inventory inventory = new Slayer.Inventory();
        inventory.setItem(config.getString("inventory.item", "minecraft:stone"));
        inventory.setDisplayName(config.getString("inventory.display-name", "displayname not found"));
        inventory.setLore(config.getStringList("inventory.lore"));
        slayer.setInventory(inventory);

        // slayer-levels section (nested)
        ConfigurationSection slayerLevelsSection = config.getConfigurationSection("slayer-levels");
        if (slayerLevelsSection != null) {
            // read "levels" list (XP required per level)
            slayer.setSlayerLevels(slayerLevelsSection.getDoubleList("levels"));

            // read "level-up-actions" list
            slayer.setLevelUpActions(readLevelUpActions(slayerLevelsSection, id));
        } else {
            logger.warning("No 'slayer-levels' section found in slayer file <dark_purple>" + id + "<reset>.yml");
        }

        slayer.setSlayerTiers(readSlayerTiers(config, id));

        return slayer;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, List<Map<String, Object>>> readLevelUpActions(ConfigurationSection section, String id) {
        // section is expected to be "slayer-levels"
        List<Map<?, ?>> entries = section.getMapList("level-up-actions");
        if (entries.isEmpty()) {
            logger.warning("No level-up-actions found in slayer file <dark_purple>" + id + "<reset>.yml");
            return Collections.emptyMap();
        }
        Map<Integer, List<Map<String, Object>>> out = new LinkedHashMap<>();

        for (Map<?, ?> rawEntry : entries) {
            if (rawEntry == null) continue;
            Object levelObj = rawEntry.get("level");
            Integer level = asInteger(levelObj);
            if (level == null) continue;

            Object actionsObj = rawEntry.get("actions");
            List<Map<String, Object>> actions = new ArrayList<>();
            if (actionsObj instanceof List<?> list) {
                for (Object elem : list) {
                    if (elem instanceof Map<?, ?> m) {
                        actions.add((Map<String, Object>) m);
                    }
                }
            }
            out.put(level, actions);
        }
        return out;
    }

    private List<Slayer.SlayerTier> readSlayerTiers(YamlConfiguration config, String id) {
        List<Map<?, ?>> tiersConfig = config.getMapList("slayer-tiers");
        if (tiersConfig.isEmpty()) {
            logger.error("No slayer-tiers found in slayer file <dark_purple>" + id + "<reset>.yml");
            return Collections.emptyList();
        }

        List<Slayer.SlayerTier> tiers = new ArrayList<>();
        for (Map<?, ?> tierMap : tiersConfig) {
            if (tierMap == null) continue;

            YamlConfiguration tierSection = mapToYamlConfiguration(tierMap);

            Slayer.SlayerTier tier = new Slayer.SlayerTier();
            tier.setName(tierSection.getString("name", "Name not found"));
            tier.setItem(tierSection.getString("item", "minecraft:stone"));
            tier.setDisplayName(tierSection.getString("display-name", ""));
            tier.setLore(tierSection.getStringList("lore"));

            // mob-settings
            ConfigurationSection mobSec = tierSection.getConfigurationSection("mob-settings");
            if (mobSec != null) {
                Slayer.SlayerTier.MobSettings mob = new Slayer.SlayerTier.MobSettings();
                mob.setMob(mobSec.getString("mob", "minecraft:zombie"));

                mob.setKills(mobSec.getString("kills", "1"));
                if (mobSec.contains("chance")) {
                    mob.setChance(mobSec.getString("chance", "100"));
                }

                mob.setDeathActions(getActionList(mobSec, "death-actions"));
                tier.setMobSettings(mob);
            } else {
                logger.error("No mob-settings found in slayer file <dark_purple>" + id + "<reset>.yml");
            }

            // boss-settings
            ConfigurationSection bossSec = tierSection.getConfigurationSection("boss-settings");
            if (bossSec != null) {
                Slayer.SlayerTier.BossSettings boss = new Slayer.SlayerTier.BossSettings();
                boss.setBossId(bossSec.getString("boss-id", ""));
                if (bossSec.contains("time-to-kill")) boss.setTimeToKill(bossSec.getInt("time-to-kill"));
                if (bossSec.contains("slayer-xp")) boss.setSlayerXp(bossSec.getInt("slayer-xp"));
                if (bossSec.contains("teleport-to-player")) boss.setTeleportToPlayer(bossSec.getInt("teleport-to-player"));

                // remove-boss
                ConfigurationSection removeSec = bossSec.getConfigurationSection("remove-boss");
                if (removeSec != null) {
                    Slayer.SlayerTier.BossSettings.RemoveBoss remove = new Slayer.SlayerTier.BossSettings.RemoveBoss();
                    if (removeSec.contains("on-teleport")) remove.setOnTeleport(removeSec.getBoolean("on-teleport", false));
                    if (removeSec.contains("on-death")) remove.setOnDeath(removeSec.getBoolean("on-death", false));
                    if (removeSec.contains("on-quit")) remove.setOnQuit(removeSec.getBoolean("on-quit", false));
                    boss.setRemoveBoss(remove);
                } else {
                    logger.error("No remove-boss setting found in slayer file <dark_purple>" + id + "<reset>.yml");
                }

                if (bossSec.contains("remove-vanilla-loot")) {
                    boss.setRemoveVanillaLoot(bossSec.getBoolean("remove-vanilla-loot", false));
                }
                if (bossSec.contains("instant-target-player")) {
                    boss.setInstantTargetPlayer(bossSec.getBoolean("instant-target-player", false));
                }

                // death-actions as List<Map<String, Object>>
                boss.setDeathActions(getActionList(bossSec, "death-actions"));

                tier.setBossSettings(boss);
            } else {
                logger.error("No boss-settings found in slayer file <dark_purple>" + id + "<reset>.yml");
            }

            // slayer-settings
            ConfigurationSection settingsSec = tierSection.getConfigurationSection("slayer-settings");
            if (settingsSec != null) {
                Slayer.SlayerTier.SlayerSettings settings = new Slayer.SlayerTier.SlayerSettings();
                if (settingsSec.contains("start-costs")) {
                    settings.setStartCosts(settingsSec.getDouble("start-costs", 0D));
                }
                // requirements as List<Map<String, Object>>
                settings.setRequirements(getActionList(settingsSec, "requirements"));
                tier.setSlayerSettings(settings);
            } else {
                logger.error("No slayer-settings found in slayer file <dark_purple>" + id + "<reset>.yml");
            }

            tiers.add(tier);
        }

        return tiers;
    }

    private static YamlConfiguration mapToYamlConfiguration(Map<?, ?> map) {
        YamlConfiguration yaml = new YamlConfiguration();
        setRecursively(yaml, null, map);
        return yaml;
    }


    private static void setRecursively(YamlConfiguration yaml, String basePath, Object value) {
        if (value instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                String key = String.valueOf(e.getKey());
                String path = (basePath == null || basePath.isEmpty()) ? key : basePath + "." + key;
                setRecursively(yaml, path, e.getValue());
            }
        } else if (value instanceof List<?> list) {
            yaml.set(basePath, list);
        } else {
            yaml.set(basePath, value);
        }
    }


    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getActionList(ConfigurationSection section, String path) {
        List<Map<?, ?>> raw = section.getMapList(path);
        if (raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> out = new ArrayList<>(raw.size());
        for (Map<?, ?> m : raw) {
            if (m == null) continue;
            out.add((Map<String, Object>) m);
        }
        return out;
    }

    private Integer asInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}