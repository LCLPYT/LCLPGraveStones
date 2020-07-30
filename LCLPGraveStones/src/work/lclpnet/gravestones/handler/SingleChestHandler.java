package work.lclpnet.gravestones.handler;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.Chest.Type;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import work.lclpnet.gravestones.event.EventListener;
import work.lclpnet.gravestones.util.GraveStone;
import work.lclpnet.gravestones.util.GraveStones;

public class SingleChestHandler {

	protected Location deathLoc;
	protected Player player;
	protected List<ItemStack> items;
	protected World world;
	@Nullable
	protected Consumer<SingleChestHandler> onSuccess, onError;

	protected Block first;

	public SingleChestHandler(Player p, List<ItemStack> items, @Nullable Consumer<SingleChestHandler> onSuccess, @Nullable Consumer<SingleChestHandler> onError) {
		this.player = p;
		this.deathLoc = p.getLocation();
		this.world = p.getWorld();
		this.items = items;
		this.onSuccess = onSuccess;
		this.onError = onError;
	}

	public void go() {
		if(deathLoc.getBlockY() < 0) deathLoc.setY(0);

		while(!isValid(deathLoc.getBlock())) {
			if(deathLoc.getBlockY() > getMaxWorldGravestoneHeight(world)) {
				error();
				return;
			}
			deathLoc.add(new Vector(0, 1, 0));
		}

		first = deathLoc.getBlock();

		boolean wasWater = first.getType() == Material.WATER;
		
		first.setType(Material.CHEST);

		org.bukkit.block.data.type.Chest data1 = (org.bukkit.block.data.type.Chest) first.getBlockData();
		data1.setType(Type.SINGLE);
		data1.setFacing(BlockFace.NORTH);
		data1.setWaterlogged(wasWater);

		first.setBlockData(data1, true);

		if(onSuccess != null) onSuccess.accept(this);
		GraveStones.add(new GraveStone(first));
		GraveStones.asyncSave();

		Chest chest1 = (Chest) first.getState();
		Inventory inv1 = chest1.getInventory();

		for (int i = 0; i < inv1.getSize(); i++) {
			if(items.isEmpty()) break;
			inv1.setItem(i, items.remove(0));
		}

		if(!items.isEmpty()) {
			for(ItemStack is : items) {
				if(EventListener.isItemValid(is))
					player.getWorld().dropItemNaturally(first.getLocation(), is);
			}
		}
	}

	protected void error() {
		if(onError != null) onError.accept(this);
	}

	protected boolean isValid(Block b) {
		return b.getY() >= 0 && b.getY() <= getMaxWorldGravestoneHeight(b.getWorld()) && (b.isEmpty() || b.getType() == Material.WATER);
	}

	protected int getMaxWorldGravestoneHeight(World w) {
		return w.getEnvironment() == Environment.NETHER ? 127 : 255;
	}

	protected void reset() {
		first.setType(Material.AIR);
	}

	public Block getFirst() {
		return first;
	}
	
}
