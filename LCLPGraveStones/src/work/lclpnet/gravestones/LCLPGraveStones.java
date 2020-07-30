package work.lclpnet.gravestones;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import work.lclpnet.gravestones.event.EventListener;
import work.lclpnet.gravestones.util.GraveStones;

public class LCLPGraveStones extends JavaPlugin{

	public static final String pre = String.format("%sGraveStones> %s", ChatColor.BLUE, ChatColor.GREEN);
	
	private static LCLPGraveStones plugin;

	public static LCLPGraveStones getPlugin() {
		return plugin;
	}
	
	@Override
	public void onEnable() {
		LCLPGraveStones.plugin = this;
		
		GraveStones.load();
		
		Bukkit.getPluginManager().registerEvents(new EventListener(), this);
	}
	
	@Override
	public void onDisable() {
		GraveStones.save();
	}
	
}
