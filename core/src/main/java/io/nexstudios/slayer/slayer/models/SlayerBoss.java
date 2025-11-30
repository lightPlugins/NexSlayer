package io.nexstudios.slayer.slayer.models;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

@Getter
@Setter
public class SlayerBoss {

    private String id;
    private String type;
    private String name;
    private Settings settings;
    private Hologram hologram;

    private YamlConfiguration configuration;

    @Getter
    @Setter
    public static class Settings {
        private Double scale;
        private Double health;
        private Double damage;
        private Double speed;
        private Double armor;
        private Double armorToughness;
        private Double knockbackResistance;
        private Integer noDamageTicks;
        private Boolean aggressive;
        private Boolean baby;
        private Boolean disableDrops;
        private Equipment equipment;

        @Getter
        @Setter
        public static class Equipment {
            private String head;
            private String body;
            private String legs;
            private String feet;
            private String hand;
            private String offHand;
        }
    }

    @Getter
    @Setter
    public static class Hologram {
        private Boolean enabled;
        private HologramSettings settings;
        private List<String> lines;

        @Getter
        @Setter
        public static class HologramSettings {
            private String billboard;
            private String backgroundColor;
            private Size size;
            private Size offset;
            private Integer viewRange;
            private Boolean seeThrough;
            private Double lineWidth;
            private int updateInterval;

            @Getter
            @Setter
            public static class Size {
                private Double x;
                private Double y;
                private Double z;
            }
        }
    }
}