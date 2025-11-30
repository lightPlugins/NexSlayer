package io.nexstudios.slayer.logic;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.NexParams;
import io.nexstudios.nexus.bukkit.effects.stats.NexusStat;
import io.nexstudios.nexus.bukkit.levels.LevelProgress;
import io.nexstudios.nexus.bukkit.levels.LevelService;
import io.nexstudios.slayer.NexSlayer;
import io.nexstudios.slayer.slayer.SlayerReader;
import io.nexstudios.slayer.slayer.models.Slayer;
import io.nexstudios.slayer.slayer.models.SlayerBoss;
import io.nexstudios.slayer.slayer.SlayerBossReader;
import lombok.Setter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class SlayerService {

    public enum Stage {
        KILLING,
        BOSS_ACTIVE,
        COMPLETED,
        FAILED
    }

    public static class ActiveSlayer {

        private final UUID playerId;
        private final Slayer slayer;
        private final Slayer.SlayerTier tier;

        @Setter
        private Stage stage;
        private int requiredKills;
        private int currentKills;

        @Setter
        private UUID bossUuid;
        @Setter
        private long bossSpawnTime;
        @Setter
        private long bossDeadline; // 0 = no timeout

        public ActiveSlayer(UUID playerId,
                            Slayer slayer,
                            Slayer.SlayerTier tier,
                            int requiredKills) {
            this.playerId = playerId;
            this.slayer = slayer;
            this.tier = tier;
            this.requiredKills = requiredKills;
            this.stage = Stage.KILLING;
            this.currentKills = 0;
        }

        public UUID getPlayerId() {
            return playerId;
        }
        public Slayer getSlayer() {
            return slayer;
        }
        public Slayer.SlayerTier getTier() {
            return tier;
        }
        public Stage getStage() {
            return stage;
        }
        public int getRequiredKills() {
            return requiredKills;
        }
        public int getCurrentKills() {
            return currentKills;
        }
        public void incrementKills() {
            this.currentKills++;
        }
        public UUID getBossUuid() {
            return bossUuid;
        }
        public long getBossSpawnTime() {
            return bossSpawnTime;
        }
        public long getBossDeadline() {
            return bossDeadline;
        }

    }

    private final SlayerReader slayerReader;
    private final SlayerBossReader bossReader;
    private final SlayerBossSpawn bossSpawn;

    private final Map<UUID, ActiveSlayer> activeSlayers = new HashMap<>();

    public SlayerService(SlayerReader slayerReader, SlayerBossReader bossReader) {
        this.slayerReader = slayerReader;
        this.bossReader = bossReader;
        this.bossSpawn = new SlayerBossSpawn();
    }

    public ActiveSlayer getActiveSlayerByBossUuid(UUID bossUuid) {
        if (bossUuid == null) return null;
        for (ActiveSlayer active : activeSlayers.values()) {
            if (bossUuid.equals(active.getBossUuid())) {
                return active;
            }
        }
        return null;
    }

    public java.util.List<String> getAllSlayerIds() {
        return slayerReader.getSlayers()
                .keySet()
                .stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public List<String> getTierIndicesFor(String slayerId) {
        Slayer slayer = slayerReader.getSlayerById(slayerId);
        if (slayer == null || slayer.getSlayerTiers() == null) {
            return Collections.emptyList();
        }
        List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < slayer.getSlayerTiers().size(); i++) {
            result.add(String.valueOf(i + 1));
        }
        return result;
    }

    public int getMaxTierCount() {
        int max = 0;
        for (Slayer slayer : slayerReader.getSlayers().values()) {
            if (slayer.getSlayerTiers() != null) {
                max = Math.max(max, slayer.getSlayerTiers().size());
            }
        }
        return max;
    }

    public boolean isBossOwnedBy(UUID bossUuid, UUID playerId) {
        ActiveSlayer active = getActiveSlayerByBossUuid(bossUuid);
        if (active == null) return false;
        return active.getPlayerId().equals(playerId);
    }

    public boolean hasActiveSlayer(Player player) {
        return activeSlayers.containsKey(player.getUniqueId());
    }

    public ActiveSlayer getActiveSlayer(Player player) {
        return activeSlayers.get(player.getUniqueId());
    }

    public boolean startSlayer(Player player, String slayerId, int tierIndex) {

        Slayer slayer = slayerReader.getSlayerById(slayerId);
        if (slayer == null) {
            NexSlayer.nexusLogger.warning("Unbekannter Slayer: " + slayerId);
            return false;
        }

        List<Slayer.SlayerTier> tiers = slayer.getSlayerTiers();
        int idx = tierIndex - 1;
        if (tiers == null || idx < 0 || idx >= tiers.size()) {
            NexSlayer.nexusLogger.warning(List.of(
                    "Invalid Slayer Tier " + tierIndex + " for slayer " + slayerId,
                    "Valid values are 1-" + tiers.size()
            ));
            return false;
        }

        Slayer.SlayerTier tier = tiers.get(idx);

        if (tier.getMobSettings() == null || tier.getMobSettings().getKills() == null) {
            NexSlayer.nexusLogger.warning("MobSettings or Kills are missing for Slayer " + slayerId + " Tier " + tierIndex);
            return false;
        }

        int requiredKills;
        try {
            requiredKills = Integer.parseInt(tier.getMobSettings().getKills());
        } catch (NumberFormatException e) {
            NexSlayer.nexusLogger.warning(List.of(
                    "'Kills' is not a number for slayer " + slayerId + " in tier " + tierIndex,
                    "Current value: " + tier.getMobSettings().getKills()
            ));
            return false;
        }

        ActiveSlayer active = new ActiveSlayer(player.getUniqueId(), slayer, tier, requiredKills);
        activeSlayers.put(player.getUniqueId(), active);

        return true;
    }

    public void stopSlayer(Player player) {
        ActiveSlayer active = activeSlayers.remove(player.getUniqueId());
        if (active == null) {
            return;
        }

        if (active.getBossUuid() != null) {
            Entity boss = Bukkit.getEntity(active.getBossUuid());
            if (boss != null && !boss.isDead()) {
                boss.remove();
            }
        }
    }

    public void handleVanillaMobDeath(Player killer, EntityType type, Location spawnLocation) {

        ActiveSlayer active = activeSlayers.get(killer.getUniqueId());
        if (active == null) {
            return;
        }

        if (active.getStage() != Stage.KILLING) {
            return;
        }

        Slayer.SlayerTier tier = active.getTier();
        if (tier.getMobSettings() == null || tier.getMobSettings().getMob() == null) {
            return;
        }

        String rawMob = tier.getMobSettings().getMob().toLowerCase();
        String namespace = "minecraft";
        String key = rawMob;

        String[] split = rawMob.split(":", 2);
        if (split.length == 2) {
            namespace = split[0];
            key = split[1];
        } else {
            NexSlayer.nexusLogger.warning(List.of(
                    "Invalid mob format for Slayer tier: " + tier.getMobSettings().getMob(),
                    "Expected format: 'namespace:key'"
            ));
            return;
        }

        if (!namespace.equals("minecraft")) {
            return;
        }

        String bukkitName = type.name().toLowerCase();
        if (!bukkitName.equals(key)) {
            return;
        }

        active.incrementKills();

        if (active.getCurrentKills() >= active.getRequiredKills()) {
            spawnBoss(killer, active, spawnLocation);
        }
    }

    public void handleMythicMobDeath(Player killer, String mythicInternalName, Location spawnLocation) {

        ActiveSlayer active = activeSlayers.get(killer.getUniqueId());
        if (active == null) {
            return;
        }

        if (active.getStage() != Stage.KILLING) {
            return;
        }

        Slayer.SlayerTier tier = active.getTier();
        if (tier.getMobSettings() == null || tier.getMobSettings().getMob() == null) {
            return;
        }

        String rawMob = tier.getMobSettings().getMob().toLowerCase();
        String namespace = "minecraft";
        String key = rawMob;

        String[] split = rawMob.split(":", 2);
        if (split.length == 2) {
            namespace = split[0];
            key = split[1];
        } else {
            NexSlayer.nexusLogger.warning(List.of(
                    "Invalid mob format for Slayer tier: " + tier.getMobSettings().getMob(),
                    "Expected format: 'namespace:key'"
            ));
            return;
        }

        if (!namespace.equals("mythicmobs")) {
            return;
        }

        String internalName = mythicInternalName.toLowerCase();
        if (!internalName.equals(key)) {
            return;
        }

        active.incrementKills();

        killer.sendMessage("§a[Slayer] §7Kills: §e" + active.getCurrentKills() + "§7/§e" + active.getRequiredKills());

        if (active.getCurrentKills() >= active.getRequiredKills()) {
            spawnBoss(killer, active, spawnLocation);
        }
    }

    private void spawnBoss(Player player, ActiveSlayer active, Location spawnLocation) {

        Slayer.SlayerTier.BossSettings bossSettings = active.getTier().getBossSettings();
        if (bossSettings == null) {
            NexSlayer.nexusLogger.warning("Could not spawn Boss for Slayer " + active.getSlayer().getId() + " Tier " + active.getTier().getName() + ": BossSettings is missing!");
            completeSlayer(player, active);
            return;
        }

        if (bossSettings.getBossId() == null) {
            NexSlayer.nexusLogger.warning("Could not spawn Boss for Slayer " + active.getSlayer().getId() + " Tier " + active.getTier().getName() + ": BossId is missing!");
            completeSlayer(player, active);
            return;
        }

        SlayerBoss boss = bossReader.getBossById(bossSettings.getBossId());
        if (boss == null) {
            NexSlayer.nexusLogger.warning("Could not spawn Boss for Slayer " + active.getSlayer().getId() + " Tier " + active.getTier().getName() + ": SlayerBoss with ID " + bossSettings.getBossId() + " not found.");
            completeSlayer(player, active);
            return;
        }

        LivingEntity le = bossSpawn.spawnBoss(spawnLocation, player, boss);
        if (le == null) {
            NexSlayer.nexusLogger.warning("Could not spawn Boss for Slayer " + active.getSlayer().getId() + " Tier " + active.getTier().getName() + ": BossSpawn failed.");
            completeSlayer(player, active);
            return;
        }

        active.setBossUuid(le.getUniqueId());
        active.setStage(Stage.BOSS_ACTIVE);
        active.setBossSpawnTime(System.currentTimeMillis());

        if (bossSettings.getTimeToKill() != null && bossSettings.getTimeToKill() > 0) {
            long deadline = System.currentTimeMillis() + bossSettings.getTimeToKill() * 1000L;
            active.setBossDeadline(deadline);
            scheduleTimeoutCheck(player.getUniqueId());
        }
        TagResolver resolver = TagResolver.resolver(
                Placeholder.parsed("boss", boss.getName())
        );
        NexSlayer.getInstance().getMessageSender().send(player, "slayer.slayer-boss-spawned", resolver);
    }

    private void scheduleTimeoutCheck(UUID playerId) {
        Bukkit.getScheduler().runTaskLater(NexSlayer.getInstance(), () -> {
            ActiveSlayer active = activeSlayers.get(playerId);
            if (active == null) {
                return;
            }
            if (active.getStage() != Stage.BOSS_ACTIVE) {
                return;
            }
            if (active.getBossDeadline() == 0L) {
                return;
            }

            long now = System.currentTimeMillis();
            long deadline = active.getBossDeadline();

            if (now < deadline) {
                scheduleTimeoutCheck(playerId);
                return;
            }

            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                NexSlayer.getInstance().getMessageSender().send(player, "slayer.slayer-timeout");
            }

            failSlayer(playerId);

        }, 20L);
    }

    public int getRemainingBossTimeSeconds(UUID playerId) {
        ActiveSlayer active = activeSlayers.get(playerId);
        if (active == null) {
            return 0;
        }
        long deadline = active.getBossDeadline();
        if (deadline <= 0L) {
            return 0;
        }
        long diffMillis = deadline - System.currentTimeMillis();
        if (diffMillis <= 0L) {
            return 0;
        }
        return (int) (diffMillis / 1000L);
    }

    private void failSlayer(UUID playerId) {
        ActiveSlayer active = activeSlayers.get(playerId);
        if (active == null) {
            return;
        }
        active.setStage(Stage.FAILED);

        if (active.getBossUuid() != null) {
            Entity boss = Bukkit.getEntity(active.getBossUuid());
            if (boss != null && !boss.isDead()) {
                boss.remove();
            }
        }

        TagResolver resolver = TagResolver.resolver(
                Placeholder.parsed("boss", active.getSlayer().getName()),
                Placeholder.parsed("tier", active.getTier().getName())
        );

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            NexSlayer.getInstance().getMessageSender().send(player, "slayer.slayer-failed", resolver);
        }

        activeSlayers.remove(playerId);
    }

    public void handleBossDeath(LivingEntity boss) {
        UUID bossId = boss.getUniqueId();

        for (ActiveSlayer active : activeSlayers.values()) {
            if (bossId.equals(active.getBossUuid())) {
                Player player = Bukkit.getPlayer(active.getPlayerId());
                completeSlayer(player, active);
                break;
            }
        }
    }

    private void completeSlayer(Player player, ActiveSlayer active) {

        active.setStage(Stage.COMPLETED);

        TagResolver resolver = TagResolver.resolver(
                Placeholder.parsed("boss", active.getSlayer().getName()),
                Placeholder.parsed("tier", active.getTier().getName())
        );

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("boss", active.getSlayer().getName());
        paramMap.put("tier", active.getTier().getName());

        NexParams params = NexParams.of(paramMap, resolver);

        if (player != null && player.isOnline()) {
            NexSlayer.getInstance().getMessageSender().send(player, "slayer.slayer-completed", resolver);

            NexusPlugin.getInstance()
                    .getActionFactory()
                    .newExecution()
                    .actor(player)
                    .targetLocation(player.getLocation())
                    .actions(active.getTier().getBossSettings().getDeathActions())
                    .params(params)
                    .execute();

            // --- Slayer-XP auf das passende Slayer-Levelsystem buchen ---
            Slayer.SlayerTier.BossSettings bossSettings = active.getTier().getBossSettings();
            if (bossSettings != null && bossSettings.getSlayerXp() != null) {
                LevelService levelService = NexSlayer.getInstance().getLevelService();
                if (levelService != null) {
                    String ns = "nexslayer";
                    String key = active.getSlayer().getId(); // z.B. "zombie-slayer"
                    double xp = bossSettings.getSlayerXp();

                    LevelProgress progress = levelService.addXp(player.getUniqueId(), ns, key, xp);

                    // Optional: Feedback an den Spieler
                    player.sendMessage("§a[Slayer] §7Du erhältst §e" + xp + "§7 XP für §e" + key
                            + "§7 (Level §e" + progress.getLevel() + "§7).");

                }
            }
            activeSlayers.remove(player.getUniqueId());
        }
    }
}