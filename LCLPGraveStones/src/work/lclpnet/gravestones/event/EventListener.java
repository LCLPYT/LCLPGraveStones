package work.lclpnet.gravestones.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import work.lclpnet.gravestones.LCLPGraveStones;
import work.lclpnet.gravestones.handler.DoubleChestHandler;
import work.lclpnet.gravestones.handler.SingleChestHandler;
import work.lclpnet.gravestones.util.GraveStone;
import work.lclpnet.gravestones.util.GraveStones;

public class EventListener implements Listener{

	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		final Player p = e.getEntity();

		Location deathLoc = p.getLocation();

		World w = p.getWorld();
		List<ItemStack> items = gatherItems(p);
		int itemStackCount = getItemStackCount(items);
		if(w.getGameRuleValue(GameRule.KEEP_INVENTORY) || itemStackCount <= 0) {
			p.sendMessage(String.format("%s%sYou died at x=%s%s %sy=%s%s %sz=%s%s.", 
					LCLPGraveStones.pre, 
					ChatColor.GRAY, 
					ChatColor.YELLOW, 
					deathLoc.getBlockX(), 
					ChatColor.GRAY, 
					ChatColor.YELLOW, 
					deathLoc.getBlockY(), 
					ChatColor.GRAY, 
					ChatColor.YELLOW, 
					deathLoc.getBlockZ()
					));
			return;
		}
		
		Consumer<SingleChestHandler> onSuccess = h -> {
			e.getDrops().clear();
			Block block = h.getFirst();
			p.sendMessage(String.format("%s%sYour gravestone was placed at x=%s%s %sy=%s%s %sz=%s%s.", 
					LCLPGraveStones.pre, 
					ChatColor.GRAY, 
					ChatColor.YELLOW, 
					block.getX(), 
					ChatColor.GRAY, 
					ChatColor.YELLOW, 
					block.getY(), 
					ChatColor.GRAY, 
					ChatColor.YELLOW, 
					block.getZ()
					));
		};
		Consumer<SingleChestHandler> onError = h -> {
			p.sendMessage(String.format("%s%sCould not find a position for your gravestone. Your items dropped.", 
					LCLPGraveStones.pre, 
					ChatColor.RED
					));
		};
		
		SingleChestHandler handler;
		if(itemStackCount <= 27) handler = new SingleChestHandler(p, items, onSuccess, onError);
		else handler = new DoubleChestHandler(p, items, onSuccess, onError);
		
		handler.go();
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		Block b = e.getBlock();
		if(b.getType() != Material.CHEST) return;
		
		GraveStone gs = GraveStones.getGraveStone(b);
		if(gs == null) return;
		
		GraveStones.remove(gs);
		GraveStones.asyncSave();

		e.setDropItems(false);
	}
	
	@EventHandler
	public void onBlockExplode(BlockExplodeEvent e) {
		List<Block> remove = new ArrayList<>();
		for(Block b : e.blockList()) {
			if(isGravestone(b)) remove.add(b);
		}
	}
	
	private boolean isGravestone(Block b) {
		return b.getType() == Material.CHEST && GraveStones.isGraveStone(b);
	}

	private int getItemStackCount(List<ItemStack> items) {
		int count = 0;
		for(ItemStack is : items) {
			if(isItemValid(is)) count++;
		}
		return count;
	}

	private List<ItemStack> gatherItems(Player p) {
		List<ItemStack> items = new ArrayList<>();
		
		for(ItemStack is : p.getInventory().getContents()) {
			if(isItemValid(is)) items.add(is);
		}
		
		if(isItemValid(p.getItemOnCursor())) items.add(p.getItemOnCursor());
		return items;
	}

	public static boolean isItemValid(ItemStack is) {
		return is != null && !is.getType().isAir();
	}

}
