package com.rs;

import java.io.IOException;

import com.rs.cache.Cache;
import com.rs.region.Region;

public class Main {
	
	public static void main(String[] args) throws IOException {
		Cache.init("D:/RSPS/EclipseWorkspace/Axios RS3/cache/");
//		for (int i = 0;i < Short.MAX_VALUE;i++) {
//			new Region(i);
//		}
		Region test = new Region(10287);
		for (int x = 0;x < 64;x++) {
			for (int y = 0;y < 64;y++) {
				System.out.print(" "+getFlagChar(test.getFlag(0, x, y)));
			}
			System.out.print("\r\n");
		}
	}
	
	public static char getFlagChar(byte val) {
		if (val != 0)
			return 'x';
		return ' ';
	}

}
