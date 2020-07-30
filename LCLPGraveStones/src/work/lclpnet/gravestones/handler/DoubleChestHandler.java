package work.lclpnet.gravestones.handler;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.Chest.Type;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import work.lclpnet.gravestones.util.GraveStone;
import work.lclpnet.gravestones.util.GraveStones;

public class DoubleChestHandler extends SingleChestHandler{

	protected Block second;
	
	public DoubleChestHandler(Player p, List<ItemStack> items, Consumer<SingleChestHandler> onSuccess, Consumer<SingleChestHandler> onError) {
		super(p, items, onSuccess, onError);
	}

	@Override
	public void go() {
		if(deathLoc.getBlockY() < 0) deathLoc.setY(0);

		second = null;
		while(!isValid(deathLoc.getBlock()) || (second = getPossibleNeighbour(deathLoc)) == null) {
			if(deathLoc.getBlockY() > getMaxWorldGravestoneHeight(world)) {
				error();
				return;
			}
			deathLoc.add(new Vector(0, 1, 0));
		}

		first = deathLoc.getBlock();

		boolean wasWater1 = first.getType() == Material.WATER,
				wasWater2 = second.getType() == Material.WATER;
		
		first.setType(Material.CHEST);
		second.setType(Material.CHEST);

		BlockFace face = getFace(first, second);
		if(face == null) {
			error();
			reset();
			return;
		}

		Type type1 = getType(first, second, face);
		if(type1 == null) {
			error();
			reset();
			return;
		}

		Type type2 = type1 == Type.SINGLE ? Type.SINGLE : (type1 == Type.LEFT ? Type.RIGHT : Type.LEFT);

		org.bukkit.block.data.type.Chest data1 = (org.bukkit.block.data.type.Chest) first.getBlockData(),
				data2 = (org.bukkit.block.data.type.Chest) second.getBlockData();
		
		data1.setType(type1);
		data1.setFacing(face);
		data1.setWaterlogged(wasWater1);
		data2.setType(type2);
		data2.setFacing(face);
		data2.setWaterlogged(wasWater2);

		first.setBlockData(data1, true);
		second.setBlockData(data2, true);
		
		if(onSuccess != null) onSuccess.accept(this);
		GraveStones.add(new GraveStone(first));
		GraveStones.add(new GraveStone(second));
		GraveStones.asyncSave();
		
		{
			Chest chest1 = (Chest) first.getState();
			Inventory inv1 = chest1.getInventory();

			for (int i = 0; i < inv1.getSize(); i++) {
				if(items.isEmpty()) break;
				inv1.setItem(i, items.remove(0));
			}
		}

		if(!items.isEmpty()) {
			if(type2 == Type.SINGLE) {
				Chest chest2 = (Chest) second.getState();
				Inventory inv2 = chest2.getInventory();

				for (int i = 0; i < inv2.getSize(); i++) {
					if(items.isEmpty()) break;
					inv2.setItem(i, items.remove(0));
				}
			}

			if(!items.isEmpty()) {
				for(ItemStack is : items) 
					player.getWorld().dropItemNaturally(first.getLocation(), is);
			}
		}
	}
	
	@Override
	protected void reset() {
		super.reset();
		second.setType(Material.AIR);
	}
	
	protected Block getPossibleNeighbour(Location loc) {
		Location tmp = loc.clone();
		BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST, BlockFace.UP, BlockFace.DOWN};
		for(BlockFace bf : faces) {
			Block rel = tmp.getBlock().getRelative(bf);
			if(isValid(rel)) return rel;
		}
		return null;
	}
	
	protected Type getType(Block first, Block second, BlockFace face) {
		if(face == BlockFace.EAST) return first.getZ() > second.getZ() ? Type.RIGHT : Type.LEFT;
		else if(face == BlockFace.SOUTH) return first.getX() < second.getX() ? Type.RIGHT : Type.LEFT;
		else if(face == BlockFace.NORTH) return Type.SINGLE;
		return null;
	}

	protected BlockFace getFace(Block first, Block second) {
		if(first.getY() < second.getY()) return BlockFace.NORTH;
		else if(first.getX() == second.getX()) return BlockFace.EAST;
		else if(first.getZ() == second.getZ()) return BlockFace.SOUTH;
		return null;
	}
	
	public Block getSecond() {
		return second;
	}
	
}
