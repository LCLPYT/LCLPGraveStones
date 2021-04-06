package work.lclpnet.gravestones.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

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
		for(Block b : e.blockList()) if(isGravestone(b)) remove.add(b);
		e.blockList().removeAll(remove);
	}
	
	@EventHandler
	public void onEntityExplode(EntityExplodeEvent e) {
		List<Block> remove = new ArrayList<>();
		for(Block b : e.blockList()) if(isGravestone(b)) remove.add(b);
		e.blockList().removeAll(remove);
	}
	
	@EventHandler
	public void onChestEmpty(InventoryClickEvent e) {
		if(!topInventoryEmptied(e)) return;
		
		InventoryHolder holder = e.getView().getTopInventory().getHolder();
		if(holder == null || !(holder instanceof Chest)) return;
		
		Chest chest = (Chest) holder;
		Block b = chest.getBlock();
		GraveStone gs = GraveStones.getGraveStone(b);
		if(gs == null) return;
		
		HumanEntity whoClicked = e.getWhoClicked();
		
		new BukkitRunnable() {
			
			@Override
			public void run() {
				whoClicked.closeInventory();
				disappearGravestone(b, gs, whoClicked);
			}

		}.runTaskLater(LCLPGraveStones.getPlugin(), 5L);
	}
	
	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		if(e.getCursor() != null && !e.getCursor().getType().isAir()) return;
		
		Inventory topInv = e.getView().getTopInventory();
		if(!topInv.isEmpty()) return;
		
		InventoryHolder holder = topInv.getHolder();
		if(holder == null || !(holder instanceof Chest)) return;
		
		Chest chest = (Chest) holder;
		Block b = chest.getBlock();
		GraveStone gs = GraveStones.getGraveStone(b);
		if(gs == null) return;
		
		HumanEntity whoClicked = e.getWhoClicked();
		
		whoClicked.closeInventory();
		disappearGravestone(b, gs, whoClicked);
	}
	
	@EventHandler
	public void onInteractGravestone(PlayerInteractEvent e) {
		Block b = e.getClickedBlock();
		if(b == null || b.getType() != Material.CHEST) return;
		
		BlockState state = b.getState();
		if(!(state instanceof Chest)) return;
		
		Chest c = (Chest) state;
		Inventory inv = c.getBlockInventory();
		if(!inv.isEmpty()) return;

		GraveStone gs = GraveStones.getGraveStone(b);
		if(gs == null) return;
		
		e.setCancelled(true);
		
		disappearGravestone(b, gs, e.getPlayer());
	}
	
	private void disappearGravestone(Block b, GraveStone gs, HumanEntity whoClicked) {
		whoClicked.sendMessage(String.format("%s%sThe gravestone has disappeared.", 
			LCLPGraveStones.pre, 
			ChatColor.GRAY 
			));
		
		GraveStones.remove(gs);
		GraveStones.asyncSave();
		b.setType(Material.AIR, true);
		
		World w = b.getWorld();
		w.spawnParticle(Particle.PORTAL, b.getLocation().clone().add(0D, 0.5D, 0D), 100, 0D, 0D, 0D, 1D);
		w.playSound(b.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1F, 1.3F);
		w.playSound(b.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1F, 1F);
	}
	
	private boolean topInventoryEmptied(InventoryClickEvent e) {
		Inventory topInv = e.getView().getTopInventory();

		// shift-emptied
		if(e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && topInv.equals(e.getClickedInventory())) {
			int occupiedSlots = 0;
			for(ItemStack is : e.getInventory()) 
				if(is != null && !is.getType().isAir()) 
					occupiedSlots++;
			
			if(occupiedSlots <= 1) return true;
		}
		
		// drag-emptied
		if(e.getClickedInventory() instanceof PlayerInventory 
				&& !e.getCursor().getType().isAir() 
				&& topInv.isEmpty()) return true;
		
		// hotbar swap-emptied
		if(e.getAction() == InventoryAction.HOTBAR_SWAP && topInv.equals(e.getClickedInventory())) {
			int occupiedSlots = 0;
			for(ItemStack is : e.getInventory()) 
				if(is != null && !is.getType().isAir()) 
					occupiedSlots++;
			
			if(occupiedSlots == 1) return true;
		}
		
		return false;
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
