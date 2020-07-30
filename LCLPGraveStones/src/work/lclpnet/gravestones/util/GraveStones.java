package work.lclpnet.gravestones.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.block.Block;

import work.lclpnet.gravestones.LCLPGraveStones;

public class GraveStones {

	private static final byte FILE_VERSION = 0;
	private static List<GraveStone> gravestones = new ArrayList<>();
	private static ReentrantLock ioLock = new ReentrantLock();

	public static void add(GraveStone gs) {
		if(gs != null) gravestones.add(gs);
	}
	
	public static void remove(GraveStone gs) {
		if(gs != null) gravestones.remove(gs);
	}

	public static List<GraveStone> getGravestones() {
		return gravestones;
	}

	public static void load() {
		gravestones.clear();
		try {
			gravestones = loadFromDisk();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void save() {
		try {
			saveToDisk();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void asyncSave() {
		new Thread(GraveStones::save, "GraveStones Saver").start();
	}

	private static void saveToDisk() throws IOException {
		HashMap<String, List<GraveStone>> byWorld = sortByWorld();

		ioLock.lock();

		File tmpFile = getTmpFile();
		tmpFile.getParentFile().mkdirs();
		try (DataOutputStream out = new DataOutputStream(new FileOutputStream(tmpFile))) {
			out.writeByte(FILE_VERSION);
			out.writeInt(byWorld.keySet().size());

			for(String s : byWorld.keySet()) {
				out.writeUTF(s);

				List<GraveStone> list = byWorld.get(s);
				out.writeInt(list.size());

				for(GraveStone gs : list) {
					out.writeInt(gs.x);
					out.writeInt(gs.y);
					out.writeInt(gs.z);
				}
			}
		}

		try (InputStream in = new FileInputStream(tmpFile);
				OutputStream out = new FileOutputStream(getFile())) {
			IOHelper.transfer(in, out);
		}

		tmpFile.delete();

		ioLock.unlock();
	}

	private static List<GraveStone> loadFromDisk() throws IOException {
		ioLock.lock();

		List<GraveStone> gravestones = new ArrayList<>();

		File file = getFile();
		if(!file.exists()) {
			ioLock.unlock();
			return new ArrayList<>();
		}

		try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
			byte version = in.readByte();
			if(version != FILE_VERSION) throw new IOException("Version mismatch: " + version + " (currently version " + FILE_VERSION + ")");

			int worlds = in.readInt();
			for (int wi = 0; wi < worlds; wi++) {
				String world = in.readUTF();

				int gravestoneCount = in.readInt();
				for (int i = 0; i < gravestoneCount; i++) {
					int x = in.readInt(),
							y = in.readInt(),
							z = in.readInt();

					gravestones.add(new GraveStone(world, x, y, z));
				}
			}
		} finally {
			ioLock.unlock();
		}

		return gravestones;
	}

	private static HashMap<String, List<GraveStone>> sortByWorld() {
		HashMap<String, List<GraveStone>> byWorld = new HashMap<>();

		for(GraveStone gs : gravestones) {
			if(byWorld.containsKey(gs.world)) {
				List<GraveStone> list = byWorld.get(gs.world);
				list.add(gs);
			} else {
				List<GraveStone> list = new ArrayList<>();
				list.add(gs);
				byWorld.put(gs.world, list);
			}
		}

		return byWorld;
	}

	public static File getFile() {
		return new File("plugins", LCLPGraveStones.getPlugin().getName() + File.separatorChar + "gravestones.bin");
	}

	private static File getTmpFile() {
		return new File("plugins", LCLPGraveStones.getPlugin().getName() + File.separatorChar + "gravestones.bin.tmp");
	}
	
	public static GraveStone getGraveStone(Block b) {
		String name = b.getWorld().getName();
		int x = b.getX(), y = b.getY(), z = b.getZ();

		for(GraveStone gs : gravestones) 
			if(gs.world.equals(name) && gs.x == x && gs.y == y && gs.z == z)
				return gs;

		return null;
	}

	public static boolean isGraveStone(Block b) {
		return getGraveStone(b) != null;
	}

}
