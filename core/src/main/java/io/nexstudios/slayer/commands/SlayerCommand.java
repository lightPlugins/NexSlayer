package io.nexstudios.slayer.commands;

import io.nexstudios.nexus.libs.commands.BaseCommand;
import io.nexstudios.nexus.libs.commands.annotation.CommandAlias;
import io.nexstudios.nexus.libs.commands.annotation.CommandPermission;
import io.nexstudios.nexus.libs.commands.annotation.CommandCompletion;
import io.nexstudios.nexus.libs.commands.annotation.Description;
import io.nexstudios.nexus.libs.commands.annotation.Subcommand;
import io.nexstudios.slayer.NexSlayer;
import io.nexstudios.slayer.logic.SlayerService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("slayer")
public class SlayerCommand extends BaseCommand {

    private final SlayerService slayerService;

    public SlayerCommand(SlayerService slayerService) {
        this.slayerService = slayerService;
    }

    @Subcommand("start")
    @CommandPermission("nexslayer.command.admin.start")
    @Description("Start a slayer via command")
    @CommandCompletion("@slayer_ids @slayer_tiers @players")
    public void onStart(CommandSender sender, String slayer, int tier, String... optionalPlayer) {

        Player target;

        if (optionalPlayer.length > 0) {
            target = Bukkit.getPlayerExact(optionalPlayer[0]);
            if (target == null) {
                NexSlayer.getInstance().getMessageSender().send(sender, "general.player-not-found");
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                NexSlayer.getInstance().getMessageSender().send(sender, "general.player-only");
                return;
            }
            target = player;
        }

        if (slayerService.hasActiveSlayer(target)) {
            NexSlayer.getInstance().getMessageSender().send(sender, "general.slayer-already-started");
            return;
        }

        boolean started = slayerService.startSlayer(target, slayer, tier);

        TagResolver resolver = TagResolver.resolver(
                Placeholder.unparsed("slayer", slayer),
                Placeholder.unparsed("player", target.getName()),
                Placeholder.unparsed("tier", String.valueOf(tier))
        );

        if (!started) {
            NexSlayer.getInstance().getMessageSender().send(sender, "slayer.slayer-not-found", resolver);
            return;
        }

        NexSlayer.getInstance().getMessageSender().send(sender, "slayer.slayer-started", resolver);
        if (!target.equals(sender)) {
            NexSlayer.getInstance().getMessageSender().send(target, "slayer.slayer-started-target", resolver);
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
                NexSlayer.getInstance().getMessageSender().send(sender, "general.player-not-found");
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                NexSlayer.getInstance().getMessageSender().send(sender, "general.player-only");
                return;
            }
            target = player;
        }

        if (!slayerService.hasActiveSlayer(target)) {
            NexSlayer.getInstance().getMessageSender().send(sender, "general.slayer-not-started");
            return;
        }

        slayerService.stopSlayer(target);

        TagResolver resolver = TagResolver.resolver(
                Placeholder.unparsed("player", target.getName())
        );

        NexSlayer.getInstance().getMessageSender().send(target, "slayer.slayer-stopped-target", resolver);
        if (!target.equals(sender)) {
            NexSlayer.getInstance().getMessageSender().send(target, "slayer.slayer-stopped", resolver);
        }
    }
}