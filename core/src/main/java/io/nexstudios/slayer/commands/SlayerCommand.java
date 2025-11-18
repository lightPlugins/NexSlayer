package io.nexstudios.slayer.commands;

import io.nexstudios.nexus.libs.commands.BaseCommand;
import io.nexstudios.nexus.libs.commands.annotation.CommandAlias;
import io.nexstudios.nexus.libs.commands.annotation.CommandPermission;
import io.nexstudios.nexus.libs.commands.annotation.CommandCompletion;
import io.nexstudios.nexus.libs.commands.annotation.Description;
import io.nexstudios.nexus.libs.commands.annotation.Subcommand;
import io.nexstudios.slayer.logic.SlayerService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("nexslayer")
public class SlayerCommand extends BaseCommand {

    private final SlayerService slayerService;

    public SlayerCommand(SlayerService slayerService) {
        this.slayerService = slayerService;
    }

    @Subcommand("start")
    @CommandPermission("nexslayer.command.admin.start")
    @Description("Startet einen Slayer für einen Spieler.")
    @CommandCompletion("@slayer_ids @slayer_tiers @players")
    public void onStart(CommandSender sender, String slayer, int tier, String... optionalPlayer) {

        Player target;

        if (optionalPlayer.length > 0) {
            target = Bukkit.getPlayerExact(optionalPlayer[0]);
            if (target == null) {
                sender.sendMessage("§cSpieler '" + optionalPlayer[0] + "' wurde nicht gefunden.");
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cDu musst einen Spieler angeben, wenn du kein Spieler bist.");
                return;
            }
            target = player;
        }

        if (slayerService.hasActiveSlayer(target)) {
            sender.sendMessage("§cDieser Spieler hat bereits einen aktiven Slayer.");
            return;
        }

        boolean started = slayerService.startSlayer(target, slayer, tier);
        if (!started) {
            sender.sendMessage("§cSlayer '" + slayer + "' oder Tier '" + tier + "' ist ungültig.");
            return;
        }

        sender.sendMessage("§aSlayer §e" + slayer + " §a(Tier §e" + tier + "§a) für §e" + target.getName() + " §agestartet.");
        if (!target.equals(sender)) {
            target.sendMessage("§aDein Slayer §e" + slayer + " §a(Tier §e" + tier + "§a) wurde gestartet.");
        }
    }

    @Subcommand("stop")
    @CommandPermission("nexslayer.command.admin.stop")
    @Description("Stoppt den aktiven Slayer eines Spielers.")
    public void onStop(CommandSender sender, String... optionalPlayer) {

        Player target;

        if (optionalPlayer.length > 0) {
            target = Bukkit.getPlayerExact(optionalPlayer[0]);
            if (target == null) {
                sender.sendMessage("§cSpieler '" + optionalPlayer[0] + "' wurde nicht gefunden.");
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cDu musst einen Spieler angeben, wenn du kein Spieler bist.");
                return;
            }
            target = player;
        }

        if (!slayerService.hasActiveSlayer(target)) {
            sender.sendMessage("§cDieser Spieler hat keinen aktiven Slayer.");
            return;
        }

        slayerService.stopSlayer(target);

        sender.sendMessage("§aDer aktive Slayer von §e" + target.getName() + " §awurde gestoppt.");
        if (!target.equals(sender)) {
            target.sendMessage("§cDein aktueller Slayer wurde gestoppt.");
        }
    }
}