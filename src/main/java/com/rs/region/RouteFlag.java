package com.rs.region;

import java.util.ArrayList;

public enum RouteFlag {
	
	NW_BLOCK(0x1),
	N_BLOCK(0x2),
	NE_BLOCK(0x4),
	E_BLOCK(0x8),
	SE_BLOCK(0x10),
	S_BLOCK(0x20),
	SW_BLOCK(0x40),
	W_BLOCK(0x80),
	BLOCKED(0x100);
	
	public byte flag;

	private RouteFlag(int flag) {
		this.flag = (byte) flag;
	}

	public static ArrayList<RouteFlag> getFlags(int value) {
		ArrayList<RouteFlag> flags = new ArrayList<>();
		for (RouteFlag f : RouteFlag.values()) {
			if ((value & f.flag) != 0)
				flags.add(f);
		}
		return flags;
	}

	public static boolean flagged(int value, RouteFlag... flags) {
		int flag = 0;
		for (RouteFlag f : flags)
			flag |= f.flag;
		return (value & flag) != 0;
	}
}
