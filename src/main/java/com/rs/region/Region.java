package com.rs.region;

import com.rs.cache.Cache;
import com.rs.cache.IndexType;
import com.rs.cache.loaders.ObjectDefinitions;
import com.rs.io.InputStream;

public class Region {

	private byte[][][] clip;
	private int regionId;

	public Region(int regionId) {
		this.regionId = regionId;
		loadRegion();
	}

	private void loadRegion() {
		int regionX = (regionId >> 8);
		int regionY = (regionId & 0xff);
		int archiveId = regionX | regionY << 7;
		
		
		byte[] landContainerData = Cache.STORE.getIndex(IndexType.MAPS).getFile(archiveId, 0);
		byte[] mapContainerData = Cache.STORE.getIndex(IndexType.MAPS).getFile(archiveId, 3);

		if (landContainerData == null || mapContainerData == null) {
			System.out.println("Skipping region: " + regionId + "(" + regionX + ", " + regionY + ")");
			return;
		}

		byte[][][] tileFlags = new byte[4][64][64];
		clip = new byte[4][64][64];

		InputStream mapStream = new InputStream(mapContainerData);
		for (int plane = 0; plane < 4; plane++) {
			for (int x = 0; x < 64; x++) {
				for (int y = 0; y < 64; y++) {
					int value = mapStream.readUnsignedByte();
					if ((value & 0x1) != 0) {
						mapStream.readUnsignedByte();
						mapStream.readUnsignedSmart();
					}
					if ((value & 0x2) != 0) {
						tileFlags[plane][x][y] = (byte) mapStream.readByte();
					}
					if ((value & 0x4) != 0) {
						mapStream.readUnsignedSmart();
					}
					if ((value & 0x8) != 0) {
						mapStream.readUnsignedByte();
					}
				}
			}
		}
		
		for (int plane = 0; plane < 4; plane++) {
			for (int x = 0; x < 64; x++) {
				for (int y = 0; y < 64; y++) {
					if (RenderFlag.flagged(tileFlags[plane][x][y], RenderFlag.CLIPPED)) {
						int finalPlane = plane;
						if (RenderFlag.flagged(tileFlags[1][x][y], RenderFlag.LOWER_OBJECT_CLIP))
							finalPlane--;
						if (finalPlane >= 0) {
							addFlag(finalPlane, x, y, RouteFlag.BLOCKED);
						}
					}
				}
			}
		}
		
		InputStream landStream = new InputStream(landContainerData);
		int objectId = -1;
		int incr;
		while ((incr = landStream.readSmart2()) != 0) {
			objectId += incr;
			int location = 0;
			int incr2;
			while ((incr2 = landStream.readUnsignedSmart()) != 0) {
				location += incr2 - 1;
				int localX = (location >> 6 & 0x3f);
				int localY = (location & 0x3f);
				int plane = location >> 12;
				int objectData = landStream.readUnsignedByte();
				int type = objectData >> 2;
				int rotation = objectData & 0x3;
				if (localX < 0 || localX >= 64 || localY < 0 || localY >= 64)
					continue;
				int objectPlane = plane;
				if (tileFlags != null && (tileFlags[1][localX][localY] & 0x2) != 0)
					objectPlane--;
				if (objectPlane < 0 || objectPlane >= 4 || plane < 0 || plane >= 4)
					continue;
				if (type > 22)
					System.out.println(ObjectDefinitions.getObjectDefinitions(objectId).getName() + "("+objectId+") type: " + type);
				clip(objectId, type, rotation, objectPlane, localX, localY);
			}
		}

	}

	private void clip(int id, int type, int rotation, int plane, int x, int y) {
		if (id == -1)
			return;
		if (x < 0 || y < 0 || x >= clip[plane].length || y >= clip[plane][x].length)
			return;
		ObjectDefinitions objectDefinition = ObjectDefinitions.getObjectDefinitions(id);

		if (type == 22 ? objectDefinition.getClipType() != 1 : objectDefinition.getClipType() == 0)
			return;
		if (type >= 0 && type <= 3) {
			if (objectDefinition.blocks)
				addWall(plane, x, y, type, rotation);
		} else if (type >= 9 && type <= 21) {
			int sizeX;
			int sizeY;
			if (rotation != 1 && rotation != 3) {
				sizeX = objectDefinition.getSizeX();
				sizeY = objectDefinition.getSizeY();
			} else {
				sizeX = objectDefinition.getSizeY();
				sizeY = objectDefinition.getSizeX();
			}
			if (objectDefinition.blocks)
				addObject(plane, x, y, sizeX, sizeY);
		} else if (type == 22)
			addFlag(plane, x, y, RouteFlag.BLOCKED);
	}

	public void addWall(int plane, int x, int y, int type, int rotation) {
		if (type == 0) {
			if (rotation == 0) {
				addFlag(plane, x, y, RouteFlag.W_BLOCK);
				addFlag(plane, x - 1, y, RouteFlag.E_BLOCK);
			}
			if (rotation == 1) {
				addFlag(plane, x, y, RouteFlag.N_BLOCK);
				addFlag(plane, x, y + 1, RouteFlag.S_BLOCK);
			}
			if (rotation == 2) {
				addFlag(plane, x, y, RouteFlag.E_BLOCK);
				addFlag(plane, x + 1, y, RouteFlag.W_BLOCK);
			}
			if (rotation == 3) {
				addFlag(plane, x, y, RouteFlag.S_BLOCK);
				addFlag(plane, x, y - 1, RouteFlag.N_BLOCK);
			}
		}
		if (type == 1 || type == 3) {
			if (rotation == 0) {
				addFlag(plane, x, y, RouteFlag.NW_BLOCK);
				addFlag(plane, x - 1, y + 1, RouteFlag.SE_BLOCK);
			}
			if (rotation == 1) {
				addFlag(plane, x, y, RouteFlag.NE_BLOCK);
				addFlag(plane, x + 1, y + 1, RouteFlag.SW_BLOCK);
			}
			if (rotation == 2) {
				addFlag(plane, x, y, RouteFlag.SE_BLOCK);
				addFlag(plane, x + 1, y - 1, RouteFlag.NW_BLOCK);
			}
			if (rotation == 3) {
				addFlag(plane, x, y, RouteFlag.SW_BLOCK);
				addFlag(plane, x - 1, y - 1, RouteFlag.NE_BLOCK);
			}
		}
		if (type == 2) {
			if (rotation == 0) {
				addFlag(plane, x, y, RouteFlag.N_BLOCK, RouteFlag.W_BLOCK);
				addFlag(plane, x - 1, y, RouteFlag.E_BLOCK);
				addFlag(plane, x, y + 1, RouteFlag.S_BLOCK);
			}
			if (rotation == 1) {
				addFlag(plane, x, y, RouteFlag.N_BLOCK, RouteFlag.E_BLOCK);
				addFlag(plane, x, y + 1, RouteFlag.S_BLOCK);
				addFlag(plane, x + 1, y, RouteFlag.W_BLOCK);
			}
			if (rotation == 2) {
				addFlag(plane, x, y, RouteFlag.E_BLOCK, RouteFlag.S_BLOCK);
				addFlag(plane, x + 1, y, RouteFlag.W_BLOCK);
				addFlag(plane, x, y - 1, RouteFlag.N_BLOCK);
			}
			if (rotation == 3) {
				addFlag(plane, x, y, RouteFlag.S_BLOCK, RouteFlag.W_BLOCK);
				addFlag(plane, x, y - 1, RouteFlag.N_BLOCK);
				addFlag(plane, x - 1, y, RouteFlag.E_BLOCK);
			}
		}
	}

	public void addObject(int plane, int x, int y, int sizeX, int sizeY) {
		for (int tileX = x; tileX < x + sizeX; tileX++) {
			for (int tileY = y; tileY < y + sizeY; tileY++) {
				addFlag(plane, tileX, tileY, RouteFlag.BLOCKED);
			}
		}
	}

	private void addFlag(int plane, int x, int y, RouteFlag... flags) {
		if (x < 0 || x >= 64 || y < 0 || y >= 64)
			return;
		int flag = 0;
		for (RouteFlag f : flags)
			flag |= f.flag;
		clip[plane][x][y] |= flag;
	}
	
	public byte getFlag(int plane, int x, int y) {
		if (clip == null)
			return RouteFlag.BLOCKED.flag;
		return clip[plane][x][y];
	}

}
