package com.rs.region;

import com.rs.cache.loaders.ObjectDefinitions;

public class WorldObject {

	private int x, y, plane;
	private int id;
	private int type;
	private int rotation;
	
	public WorldObject(int id, int type, int rotation, int x, int y, int plane) {
		this.x = x;
		this.y = y;
		this.plane = plane;
		this.id = id;
		this.type = type;
		this.rotation = rotation;
	}

	public WorldObject(WorldObject object) {
		this.x = object.getX();
		this.y = object.getY();
		this.plane = object.getPlane();
		this.id = object.id;
		this.type = object.type;
		this.rotation = object.rotation;
	}

	public int getId() {
		return id;
	}

	public int getType() {
		return type;
	}

	public int getRotation() {
		return rotation;
	}

	public void setRotation(int rotation) {
		this.rotation = rotation;
	}
	
	public ObjectDefinitions getDefinitions() {
		return ObjectDefinitions.getObjectDefinitions(id);
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setType(int type) {
		this.type = type;
	}
	
	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getPlane() {
		return plane;
	}

	public void setPlane(int plane) {
		this.plane = plane;
	}

}