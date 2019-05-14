package com.rs.region;

import java.util.ArrayList;

public enum RenderFlag {
	CLIPPED(0x1),
	LOWER_OBJECT_CLIP(0x2),
	UNDER_ROOF(0x4),
	FORCE_TO_BOTTOM(0x8),
	ROOF(0x10);
	
	private int flag;
	
	private RenderFlag(int flag) {
		this.flag = flag;
	}
	
	public static ArrayList<RenderFlag> getFlags(int value) {
		ArrayList<RenderFlag> flags = new ArrayList<>();
		for (RenderFlag f : RenderFlag.values()) {
			if ((value & f.flag) != 0)
				flags.add(f);
		}
		return flags;
	}
	
	public static boolean flagged(int value, RenderFlag... flags) {
    	int flag = 0;
    	for (RenderFlag f : flags)
    		flag |= f.flag;
    	return (value & flag) != 0;
    }
}
