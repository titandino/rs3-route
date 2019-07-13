package com.rs.region;

import java.util.ArrayList;
import java.util.List;

import com.rs.cache.Cache;
import com.rs.cache.IndexType;
import com.rs.cache.loaders.ObjectDefinitions;
import com.rs.io.InputStream;

public class Region {

	public static final int[] OBJECT_SLOTS = new int[] { 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3 };
	public static final int WIDTH = 64;
	public static final int HEIGHT = 64;
	
	private byte[][][] clip;
	private int[][][] overlayIds;
	private int[][][] underlayIds;
	private byte[][][] overlayPathShapes;
	private byte[][][] overlayRotations;
	private byte[][][] tileFlags;
	protected WorldObject[][][][] objects;
	private int regionId;
	private boolean hasData;

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
		
		setHasData(true);

		overlayIds = new int[4][64][64];
		underlayIds = new int[4][64][64];
		overlayPathShapes = new byte[4][64][64];
		overlayRotations = new byte[4][64][64];
		tileFlags = new byte[4][64][64];
		clip = new byte[4][64][64];

		InputStream mapStream = new InputStream(mapContainerData);
		for (int plane = 0; plane < 4; plane++) {
			for (int x = 0; x < 64; x++) {
				for (int y = 0; y < 64; y++) {
					int value = mapStream.readUnsignedByte();
					if ((value & 0x1) != 0) {
						int overlayMetaData = mapStream.readUnsignedByte();
						overlayIds[plane][x][y] = mapStream.readUnsignedSmart();
						overlayPathShapes[plane][x][y] = (byte) (overlayMetaData >> 2);
						overlayRotations[plane][x][y] = (byte) (overlayMetaData & 0x3);
					}
					if ((value & 0x2) != 0) {
						tileFlags[plane][x][y] = (byte) mapStream.readByte();
					}
					if ((value & 0x4) != 0) {
						underlayIds[plane][x][y] = mapStream.readUnsignedSmart();
					}
					if ((value & 0x8) != 0) {
						mapStream.readUnsignedByte();
						//tileHeights[plane][x][y] 
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
				clip(objectId, type, rotation, objectPlane, localX, localY);
			}
		}

	}

	private void clip(int id, int type, int rotation, int plane, int x, int y) {
		if (id == -1)
			return;
		if (x < 0 || y < 0 || x >= clip[plane].length || y >= clip[plane][x].length)
			return;
		if (type >= OBJECT_SLOTS.length)
			return;
		if (objects == null)
			objects = new WorldObject[4][64][64][4];
		ObjectDefinitions objectDefinition = ObjectDefinitions.getObjectDefinitions(id);
		
		int slot = OBJECT_SLOTS[type];
		objects[plane][x][y][slot] = new WorldObject(id, type, rotation, x, y, plane);
		
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
	
	public List<WorldObject> getObjects() {
		if (objects == null) {
			return null;
		}
		List<WorldObject> list = new ArrayList<WorldObject>();
		for (int z = 0; z < objects.length; z++) {
			if (objects[z] == null) {
				continue;
			}
			for (int x = 0; x < objects[z].length; x++) {
				if (objects[z][x] == null) {
					continue;
				}
				for (int y = 0; y < objects[z][x].length; y++) {
					if (objects[z][x][y] == null) {
						continue;
					}
					for (WorldObject o : objects[z][x][y]) {
						if (o != null) {
							list.add(o);
						}
					}
				}
			}
		}
		return list;
	}
	
	public final boolean isLinkedBelow(final int z, final int x, final int y) {
		return RenderFlag.flagged(getRenderFlags(z, x, y), RenderFlag.LOWER_OBJECT_CLIP);
	}

	public final boolean isVisibleBelow(final int z, final int x, final int y) {
		return RenderFlag.flagged(getRenderFlags(z, x, y), RenderFlag.FORCE_TO_BOTTOM);
	}
	
	public int getRenderFlags(int z, int x, int y) {
		return tileFlags != null ? tileFlags[z][x][y] : 0;
	}

	public int getUnderlayId(int z, int x, int y) {
		return underlayIds != null ? underlayIds[z][x][y] & 0x7fff : -1;
	}
	
	public int getOverlayId(int z, int x, int y) {
		return overlayIds != null ? overlayIds[z][x][y] & 0x7fff : -1;
	}
	
	public int getOverlayPathShape(int z, int x, int y) {
		return overlayPathShapes != null ? overlayPathShapes[z][x][y] & 0x7fff : -1;
	}
	
	public int getOverlayRotation(int z, int x, int y) {
		return overlayRotations != null ? overlayRotations[z][x][y] : -1;
	}
	
	public int getBaseX() {
		return (regionId >> 8 & 0xFF) << 6;
	}
	
	public int getBaseY() {
		return (regionId & 0xFF) << 6;
	}

	public boolean hasData() {
		return hasData;
	}

	public void setHasData(boolean hasData) {
		this.hasData = hasData;
	}
	
	public int getRegionId() {
		return regionId;
	}
}
