package io.nexstudios.slayer.logic;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.UUID;

public class SlayerService {

    public SlayerService() {}

    public HashMap<UUID, SlayerData> slayerData = new HashMap<>();

    public SlayerData getSlayerData(UUID uuid) {
        return slayerData.get(uuid);
    }

    public void addKill(UUID uuid) {
        SlayerData data = slayerData.get(uuid);
        if (data != null) {
            data.setCurrentKills(data.getCurrentKills() + 1);
        }
    }

    public void resetProgress(UUID uuid) {
        SlayerData data = slayerData.get(uuid);
        if (data != null) {
            data.setCurrentKills(0);
            data.setSlayerActive(false);
            data.setCurrentSlayerId(null);
            data.setCurrentSlayerTier(0);
        }
    }

    @Getter
    @Setter
    public static class test {



    }
}
