package io.nexstudios.slayer.commands;

import io.nexstudios.nexus.bukkit.levels.*;
import io.nexstudios.nexus.libs.commands.BaseCommand;
import io.nexstudios.nexus.libs.commands.annotation.CommandAlias;
import io.nexstudios.nexus.libs.commands.annotation.CommandPermission;
import io.nexstudios.nexus.libs.commands.annotation.CommandCompletion;
import io.nexstudios.nexus.libs.commands.annotation.Description;
import io.nexstudios.nexus.libs.commands.annotation.Subcommand;
import io.nexstudios.slayer.NexSlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

@CommandAlias("slayer")
public class SlayerXpCommand extends BaseCommand {

    private static final String NAMESPACE = "nexslayer";

    private LevelService getLevelService() {
        LevelService service = NexSlayer.getInstance().getLevelService();
        if (service == null) {
            throw new IllegalStateException("LevelService ist null. Ist das Nexus-Plugin geladen?");
        }
        return service;
    }

    private String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    // /slayerxp set <slayerId> <player> <totalXp>
    @Subcommand("set")
    @CommandPermission("nexslayer.command.admin.xp.set")
    @Description("Setzt die gesamte Slayer-XP eines Spielers (inkl. Rückrechnung von Leveln).")
    @CommandCompletion("@slayer_ids @players")
    public void onSet(CommandSender sender, String slayerId, String playerName, double totalXp) {

        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage("§cSpieler '" + playerName + "' wurde nicht gefunden.");
            return;
        }

        if (totalXp < 0) totalXp = 0;

        UUID pid = target.getUniqueId();
        LevelService levels = getLevelService();

        // aktuellen Fortschritt laden
        LevelProgress current = levels.getProgress(pid, NAMESPACE, slayerId);
        double currentTotal = current.getTotalXp();

        double delta = totalXp - currentTotal;
        if (Math.abs(delta) < 1e-6) {
            sender.sendMessage("§7Slayer-XP von §e" + target.getName() + "§7 bei §e" + slayerId +
                    "§7 ist bereits §b" + fmt(totalXp) + "§7.");
            return;
        }

        // XP-Differenz anwenden (kann positiv oder negativ sein)
        LevelProgress after = levels.addXp(pid, NAMESPACE, slayerId, delta);
        levels.safeToDatabase(pid);

        sender.sendMessage("§aSlayer-XP für §e" + target.getName() + "§a bei §e" + slayerId +
                "§a auf insgesamt §b" + fmt(after.getTotalXp()) + "§a gesetzt.");
        sender.sendMessage("§7Neues Level: §a" + after.getLevel() +
                "§7 | XP im Level: §b" + fmt(after.getXp()));
    }

    // /slayerxp add <slayerId> <player> <xp>
    @Subcommand("add")
    @CommandPermission("nexslayer.command.admin.xp.add")
    @Description("Fügt einem Spieler Slayer-XP hinzu.")
    @CommandCompletion("@slayer_ids @players")
    public void onAdd(CommandSender sender, String slayerId, String playerName, double xp) {

        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage("§cSpieler '" + playerName + "' wurde nicht gefunden.");
            return;
        }

        if (xp <= 0) {
            sender.sendMessage("§cXP muss größer als 0 sein.");
            return;
        }

        UUID pid = target.getUniqueId();
        LevelService levels = getLevelService();

        LevelProgress after = levels.addXp(pid, NAMESPACE, slayerId, xp);
        levels.safeToDatabase(pid);

        sender.sendMessage("§aSlayer-XP von §e" + target.getName() + "§a bei §e" + slayerId +
                "§a um §b" + fmt(xp) + "§a erhöht.");
        sender.sendMessage("§7Neues Level: §a" + after.getLevel() +
                "§7 | XP im Level: §b" + fmt(after.getXp()) +
                "§7 | Gesamt-XP: §b" + fmt(after.getTotalXp()));
    }

    // /slayerxp remove <slayerId> <player> <xp>
    @Subcommand("remove")
    @CommandPermission("nexslayer.command.admin.xp.remove")
    @Description("Entfernt Slayer-XP von einem Spieler.")
    @CommandCompletion("@slayer_ids @players")
    public void onRemove(CommandSender sender, String slayerId, String playerName, double xp) {

        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage("§cSpieler '" + playerName + "' wurde nicht gefunden.");
            return;
        }

        if (xp <= 0) {
            sender.sendMessage("§cXP muss größer als 0 sein.");
            return;
        }

        UUID pid = target.getUniqueId();
        LevelService levels = getLevelService();

        // aktueller Fortschritt zur Begrenzung
        LevelProgress current = levels.getProgress(pid, NAMESPACE, slayerId);
        double currentTotal = current.getTotalXp();

        if (currentTotal <= 0) {
            sender.sendMessage("§7Der Spieler §e" + target.getName() + "§7 hat bei §e" + slayerId +
                    "§7 bereits §b0§7 Slayer-XP.");
            return;
        }

        double effectiveRemove = Math.min(currentTotal, xp);

        LevelProgress after = levels.addXp(pid, NAMESPACE, slayerId, -effectiveRemove);
        levels.safeToDatabase(pid);

        sender.sendMessage("§aSlayer-XP von §e" + target.getName() + "§a bei §e" + slayerId +
                "§a um §b" + fmt(effectiveRemove) + "§a reduziert.");
        sender.sendMessage("§7Neues Level: §a" + after.getLevel() +
                "§7 | XP im Level: §b" + fmt(after.getXp()) +
                "§7 | Gesamt-XP: §b" + fmt(after.getTotalXp()));
    }

    // /slayerxp info <slayerId> [player]
    @Subcommand("info")
    @CommandPermission("nexslayer.command.admin.xp.info")
    @Description("Zeigt Slayer-Level und XP eines Spielers für einen Slayer.")
    @CommandCompletion("@slayer_ids @players")
    public void onInfo(CommandSender sender, String slayerId, String... optionalPlayer) {

        Player target;

        if (optionalPlayer.length > 0) {
            target = Bukkit.getPlayerExact(optionalPlayer[0]);
            if (target == null) {
                sender.sendMessage("§cSpieler '" + optionalPlayer[0] + "' wurde nicht gefunden.");
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cBitte gib einen Spieler an.");
                return;
            }
            target = player;
        }

        UUID pid = target.getUniqueId();
        LevelService levels = getLevelService();

        LevelProgress progress = levels.getProgress(pid, NAMESPACE, slayerId);
        int level = progress.getLevel();
        double xpInLevel = progress.getXp();
        double totalXp = progress.getTotalXp();

        // Nutzt die API-Logik:
        // - bei normalen Leveln: Schwelle für das nächste Level
        // - bei Max-Level: Schwelle des Max-Levels
        double requiredXp = levels.getRequiredXpForCurrentLevel(pid, NAMESPACE, slayerId);

        sender.sendMessage("§8========================");
        sender.sendMessage("§7Slayer: §e" + slayerId);
        sender.sendMessage("§7Spieler: §e" + target.getName());
        sender.sendMessage("§7Level: §a" + level);
        sender.sendMessage("§7XP im Level: §b" + fmt(xpInLevel) + "§7 / §b" + fmt(requiredXp));
        sender.sendMessage("§7Gesamt-XP: §b" + fmt(totalXp));
        sender.sendMessage("§8========================");
    }
}