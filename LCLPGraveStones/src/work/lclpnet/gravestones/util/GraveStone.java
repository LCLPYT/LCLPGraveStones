package work.lclpnet.gravestones.util;

import org.bukkit.block.Block;

public class GraveStone {

	public String world;
	public int x, y, z;
	
	public GraveStone(Block b) {
		this(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
	}
	
	public GraveStone(String world, int x, int y, int z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public String toString() {
		return String.format("{ \"world\": %s, \"x\": %s, \"y\": %s, \"z\": %s }", 
				this.world,
				this.x,
				this.y,
				this.z);
	}

}
