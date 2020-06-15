package unprotesting.com.github;

import java.util.Map;
import java.util.logging.Logger;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.io.FileOutputStream;



import java.io.BufferedReader;
import java.io.File;

import java.io.InputStreamReader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.URL;
import org.apache.commons.io.IOUtils;

import com.google.common.net.HttpHeaders;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Main extends JavaPlugin implements Listener {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static Economy econ;
    private static JavaPlugin plugin;
    File resources = new File("plugins/Auto-Tune/", "resources.yml");
    FileConfiguration resourcesconfig = YamlConfiguration.loadConfiguration(resources);
    File cfile;
    FileConfiguration config = this.getConfig();
    private static final String BASEDIR = "plugins/Auto-Tune/Auto-TuneOfficalWebsite";
    

    @Override
    public void onDisable() {
        cancelAllTasks(this);
        log.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
    }

    private void cancelAllTasks(Main main) {
    }

    @Override
    public void onEnable() {
        config.addDefault("Port", 8321);
        config.options().copyDefaults(true);
        saveConfig();
        try {
            resourcesconfig.save(resources);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int PORT = getConfig().getInt("Port");
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/Home.html", new StaticFileHandler(BASEDIR));
            server.start();
            log.info("[Auto Tune] Web server has started on port " + PORT);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        this.getCommand("autotune").setExecutor(new AutoTuneCommand());
        if (!setupEconomy()) {
            log.severe(
                    String.format("Disabled Auto-Tune due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }


    public boolean onCommand(CommandSender sender, Command testcmd, String trade, String[] help) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String hostIP = "";
            try
        { 
            URL url_name = new URL("http://bot.whatismyipaddress.com"); 
  
            BufferedReader sc = 
            new BufferedReader(new InputStreamReader(url_name.openStream())); 
  
            // reads system IPAddress 
            hostIP = sc.readLine().trim(); 

            int PORT = getConfig().getInt("Port");
            InetAddress address = InetAddress.getLocalHost();
            String hostName = address.getHostName();
            TextComponent message = new TextComponent(ChatColor.YELLOW + "" + ChatColor.BOLD + player.getDisplayName() + ", go to http://" + hostIP + ":" + PORT + "/trade");
            message.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to begin trading online").create() ) );
            message.setClickEvent( new ClickEvent( ClickEvent.Action.OPEN_URL, "http://" + hostIP + ":" + PORT + "/trade") );
            player.spigot().sendMessage(message);
            player.sendMessage(ChatColor.ITALIC + "Hostname : "+ hostName);
        } 
        catch (Exception e) 
        { 
            hostIP = "Cannot Execute Properly"; 
        } 
            return true;
        }
        return false;
    }
}

