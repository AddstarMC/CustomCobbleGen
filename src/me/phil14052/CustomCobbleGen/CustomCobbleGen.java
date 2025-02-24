/**
 * CustomCobbleGen By @author Philip Flyvholm
 * CustomCobbleGen.java
 */
package me.phil14052.CustomCobbleGen;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.phil14052.CustomCobbleGen.Commands.MainCommand;
import me.phil14052.CustomCobbleGen.Events.BlockEvents;
import me.phil14052.CustomCobbleGen.Events.PlayerEvents;
import me.phil14052.CustomCobbleGen.Files.ConfigUpdater;
import me.phil14052.CustomCobbleGen.Files.Files;
import me.phil14052.CustomCobbleGen.Files.Lang;
import me.phil14052.CustomCobbleGen.Files.LangFileUpdater;
import me.phil14052.CustomCobbleGen.Files.PlayerFileUpdater;
import me.phil14052.CustomCobbleGen.GUI.InventoryEvents;
import me.phil14052.CustomCobbleGen.Managers.BlockManager;
import me.phil14052.CustomCobbleGen.Managers.EconomyManager;
import me.phil14052.CustomCobbleGen.Managers.TierManager;
import me.phil14052.CustomCobbleGen.Utils.GlowEnchant;
import me.phil14052.CustomCobbleGen.Utils.TierPlaceholderExpansion;
import me.phil14052.CustomCobbleGen.Utils.Metrics.Metrics;

public class CustomCobbleGen extends JavaPlugin {
	private static CustomCobbleGen plugin;
	public Files lang;
	private FileConfiguration playerConfig;
	private File playerConfigFile;
	private TierManager tierManager;
	private EconomyManager econManager;
	public boolean isUsingPlaceholderAPI = false;
	
	@Override
	public void onEnable(){
		double time = System.currentTimeMillis();
		plugin = this;
		tierManager = TierManager.getInstance();
		plugin.log("Enabling CustomCobbleGen plugin");
		new ConfigUpdater();
		saveConfig();
		this.debug("The config is now setup");
		lang = new Files(this, "lang.yml");
		new LangFileUpdater(plugin);
		Lang.setFile(lang);
		this.debug("Lang is now setup");
		playerConfig = null;
		playerConfigFile = null;
		new PlayerFileUpdater(plugin);
		tierManager.load();
		this.debug("Players is now setup");
		econManager = EconomyManager.getInstance();
		if(econManager.setupEconomy()) {
			this.debug("Economy is now setup");	
		}else {
			this.debug("Economy is not setup");
		}
		this.isUsingPlaceholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
		if(this.isUsingPlaceholderAPI) {
			new TierPlaceholderExpansion(this).register();
			plugin.debug("Found PlaceholderAPI and registed placeholders&2 \u2713");
		}
		registerEvents();
		plugin.debug("Events loaded&2 \u2713");
		plugin.getCommand("cobblegen").setExecutor(new MainCommand());
		plugin.debug("Commands loaded&2 \u2713");
		registerGlow();
        Metrics metrics = new Metrics(this);
        Metrics.SingleLineChart chart = new Metrics.SingleLineChart("generators", new Callable<Integer>() {
        	
			@Override
			public Integer call() throws Exception {
				return BlockManager.getInstance().getKnownGenLocations().size();
			}
        	
        });
        metrics.addCustomChart(chart);
		plugin.debug("CustomCobbleGen is now enabled&2 \u2713");
		double time2 = System.currentTimeMillis();
		double time3 = (time2-time)/1000;
		plugin.debug("Took " + String.valueOf(time3) + " seconds to setup CustomCobbleGen");
	}
	@Override
	public void onDisable(){
		tierManager.unload();
		this.savePlayerConfig();
		tierManager = null;
		plugin = null;
	}
	
	public void reloadPlugin() {
		this.reloadConfig();
		this.lang.reload();
		this.reloadPlayerConfig();
		tierManager.reload();
		tierManager = TierManager.getInstance();
	}
	
	public void reloadPlayerConfig(){
		if(this.playerConfigFile == null){
			this.playerConfigFile = new File(new File(plugin.getDataFolder(), "Data"),"players.yml");
			this.playerConfig = YamlConfiguration.loadConfiguration(this.playerConfigFile);
			
		}
	}
	 //Return the arena config
    public FileConfiguration getPlayerConfig() {
 
        if(this.playerConfigFile == null) this.reloadPlayerConfig();
 
        return this.playerConfig;
 
    }
 
    //Save the arena config
    public void savePlayerConfig() {
 
        if(this.playerConfig == null || this.playerConfigFile == null) return;
 
        try {
            this.getPlayerConfig().save(this.playerConfigFile);
        } catch (IOException ex) {
            plugin.getServer().getLogger().log(Level.SEVERE, "Could not save Player config to " + this.playerConfigFile +"!", ex);
        }
 
    }
	
    public void registerGlow() {
    	if(Enchantment.getByKey(new NamespacedKey(this, "GlowEnchant")) != null) return;
        try {
            Field f = Enchantment.class.getDeclaredField("acceptingNew");
            f.setAccessible(true);
            f.set(null, true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            GlowEnchant glow = new GlowEnchant(new NamespacedKey(this, "GlowEnchant"));
            Enchantment.registerEnchantment(glow);
        }
        catch (IllegalArgumentException e){
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    
	private void registerEvents(){
	    PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new BlockEvents(), this);
		pm.registerEvents(new InventoryEvents(), this);
		pm.registerEvents(new PlayerEvents(), this);
	}
	
	public void debug(boolean overrideConfigOption, Object... objects) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(Object s : objects) {
			if(!first) {
				sb.append(", ");
			}else first = false;
			sb.append("[" + s.getClass().getTypeName() + ": " + s.toString() + "]");
		}
		this.debug(sb.toString());
	}
	
	public void debug(Object... objects) {
		this.debug(false, objects);
	}
	
	public void debug(String message){
		this.debug(message, false);
	}
	public void debug(String message, boolean overrideConfigOption){
		if(overrideConfigOption == false && plugin.getConfig().getBoolean("debug") == false) return;
		Bukkit.getConsoleSender().sendMessage(("&8[&3&lCustomCobbleGen&8]: &c&lDebug &8-&7 " + message).replaceAll("&", "\u00A7"));
	}
	public void log(String message){
		Bukkit.getConsoleSender().sendMessage(("&8[&3&lCustomCobbleGen&8]: &c&lLog &8-&7 " + message).replaceAll("&", "\u00A7"));
	}
	
	public static CustomCobbleGen getInstance(){
		return plugin;
	}
}
