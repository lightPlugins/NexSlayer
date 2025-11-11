package io.nexstudios.slayer.slayer.models;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Slayer {

    private String id;
    private String name;

    private Inventory inventory;

    private List<Double> slayerLevels;

    // Map<Level, List<ActionMap>> -> Nexus Action API
    private Map<Integer, List<Map<String, Object>>> levelUpActions;

    private List<SlayerTier> slayerTiers;

    // Optional: die vollständige Yaml-Konfiguration
    private YamlConfiguration configuration;

    @Getter
    @Setter
    public static class Inventory {
        private String item;
        private String displayName;
        private List<String> lore;
    }

    @Getter
    @Setter
    public static class SlayerTier {
        private String name;
        private String item;
        private String displayName;
        private List<String> lore;

        private MobSettings mobSettings;
        private BossSettings bossSettings;
        private SlayerSettings slayerSettings;

        @Getter
        @Setter
        public static class MobSettings {
            private String mob;
            private String kills; // YAML erlaubt "10-12" oder Einzelwerte → als String beibehalten
            private String chance;
            private List<Map<String, Object>> deathActions;
        }

        @Getter
        @Setter
        public static class BossSettings {
            private String bossId;
            private Integer timeToKill;
            private Integer slayerXp;
            private Integer teleportToPlayer;
            private RemoveBoss removeBoss;
            private Boolean removeVanillaLoot;
            private Boolean instantTargetPlayer;
            private List<Map<String, Object>> deathActions; // Aktionen als Liste von Maps -> Nexus Action API

            @Getter
            @Setter
            public static class RemoveBoss {
                private Boolean onTeleport;
                private Boolean onDeath;
                private Boolean onQuit;
            }
        }

        @Getter
        @Setter
        public static class SlayerSettings {
            private Double startCosts;
            private List<Map<String, Object>> requirements; // Aktionen als Liste von Maps -> Nexus Requirements API
        }
    }
}