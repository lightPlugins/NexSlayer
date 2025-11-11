package io.nexstudios.slayer;

import io.nexstudios.nexus.bukkit.files.NexusFile;
import io.nexstudios.nexus.bukkit.files.NexusFileReader;
import io.nexstudios.nexus.bukkit.handler.MessageSender;
import io.nexstudios.nexus.bukkit.inv.api.InvService;
import io.nexstudios.nexus.bukkit.inv.renderer.DefaultNexItemRenderer;
import io.nexstudios.nexus.bukkit.language.NexusLanguage;
import io.nexstudios.nexus.bukkit.utils.NexusLogger;
import io.nexstudios.nexus.libs.commands.PaperCommandManager;
import io.nexstudios.slayer.commands.ReloadCommand;
import io.nexstudios.slayer.slayer.SlayerBossReader;
import io.nexstudios.slayer.slayer.SlayerReader;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class NexSlayer extends JavaPlugin {

    @Getter
    private static NexSlayer instance;
    public PaperCommandManager commandManager;
    public static NexusLogger nexusLogger;
    public NexusFile settingsFile;
    public NexusFileReader languageFiles;
    public NexusFileReader slayerFiles;
    public NexusFileReader bossFiles;
    public NexusLanguage nexusLanguage;
    public MessageSender messageSender;
    private InvService invService;
    public SlayerReader slayerReader;
    public SlayerBossReader slayerBossReader;

    @Override
    public void onLoad() {
        instance = this;
        if(!checkPluginRequirements()) return;
        nexusLogger = new NexusLogger("<reset>[<dark_purple>NexSlayer<reset>]", true, 99, "<dark_purple>");
        nexusLogger.info("Loading <dark_purple>NexSlayer <reset>...");
    }


    @Override
    public void onEnable() {
        nexusLogger.info("Starting up ...");
        nexusLogger.info("Register commands ...");
        commandManager = new PaperCommandManager(this);
        nexusLogger.info("Load files and drop tables ...");
        onReload();
        registerCommands();
        registerEvents();
        this.invService = new InvService(this, new DefaultNexItemRenderer(), nexusLanguage);
        nexusLogger.info("Successfully started up.");
    }

    @Override
    public void onDisable() {
        nexusLogger.info("Shutting down NexSlayer ...");
    }

    public void onReload() {
        nexusLogger.info("Reloading NexSlayer ...");
        loadNexusFiles();
        readBosses();
        readSlayers();
        messageSender = new MessageSender(nexusLanguage);
    }

    public void registerCommands() {

        commandManager.registerCommand(new ReloadCommand());

        int size = commandManager.getRegisteredRootCommands().size();
        nexusLogger.info("Successfully registered " + size  + " command(s).");

    }

    private void readSlayers() {
        slayerReader = new SlayerReader(slayerFiles, nexusLogger);
        slayerReader.read();
    }

    private void readBosses() {
        slayerBossReader = new SlayerBossReader(bossFiles, nexusLogger);
        slayerBossReader.read();
    }

    private void registerEvents() {
        PluginManager manager = Bukkit.getPluginManager();
    }

    private void loadNexusFiles() {
        settingsFile = new NexusFile(this, "settings.yml", nexusLogger, true);
        new NexusFile(this, "languages/english.yml", nexusLogger, true);
        nexusLogger.setDebugEnabled(settingsFile.getBoolean("logging.debug.enable", true));
        nexusLogger.setDebugLevel(settingsFile.getInt("logging.debug.level", 3));

        languageFiles = new NexusFileReader("languages", this);
        slayerFiles = new NexusFileReader("slayers", this);
        bossFiles = new NexusFileReader("bosses", this);

        nexusLanguage = new NexusLanguage(languageFiles, nexusLogger);
        nexusLogger.info("All Nexus files have been (re)loaded successfully.");
    }

    private boolean checkPluginRequirements() {
        if(Bukkit.getPluginManager().getPlugin("Nexus") == null) {
            getLogger().severe("NexSlayer requires the Nexus plugin to be installed and enabled.");
            getLogger().severe("Please download the plugin from https://www.spigotmc.org/resources/nexus.10000/");
            getLogger().severe("Disabling NexSlayer ...");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }

}
