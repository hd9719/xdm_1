

package org.sdg.xdman.proxy;

class UAList {
	static String ualist[] = { "AOL", "Avant Browser",
			"Arora",
			"Fireweb Navigator",
			"Flock",
			"Navigator",// Netscape
			"Navscape",// Netscape
			"PaleMoon", "Firebird", "Firefox", "Konqueror", "Maxthon", "Opera",
			"rekonq", "RockMelt", "SeaMonkey", "ChromePlus", "Chrome",
			"Galeon", "Safari", "MSIE", "Konqueror" };

	public static String getBrowser(String ua) {
		ua = ua.toLowerCase();
		for (int i = 0; i < ualist.length; i++)
			if (ua.indexOf(ualist[i].toLowerCase()) >= 0)
				return ualist[i];
		return "Unknown Browser";
	}

	public static void main(String a[]) {
		System.out.println(getBrowser(a[0]));
	}
}
