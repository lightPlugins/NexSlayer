package io.nexstudios.slayer.commands;

import io.nexstudios.nexus.libs.commands.BaseCommand;
import io.nexstudios.nexus.libs.commands.annotation.*;
import io.nexstudios.slayer.NexSlayer;
import org.bukkit.command.CommandSender;


@CommandAlias("nexslayer")
public class ReloadCommand extends BaseCommand {

    @Subcommand("reload")
    @CommandPermission("nexslayer.command.admin.reload")
    @Description("Reloads the plugin configuration and settings.")
    public void onReload(CommandSender sender) {

        NexSlayer.getInstance().onReload();
        NexSlayer.getInstance().messageSender.send(sender, "general.reload");

    }

}
