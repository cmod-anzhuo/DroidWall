package com.menethil.hiapk.netmaster.bean;

/**
 * 程序信息
 */
public final class BaseApp {
	/** linux 用户 id */
	public int uid;
	/** id 对应的用户名[应用程序名] */
	public String names[];
	/** 是否选中 wifi */
	public boolean selected_wifi;
	/** 是否选中 3g */
	public boolean selected_3g;
	String tostr;

	public BaseApp() {
	}

	public BaseApp(int uid, String name, boolean selected_wifi, boolean selected_3g) {
		this.uid = uid;
		this.names = new String[] { name };
		this.selected_wifi = selected_wifi;
		this.selected_3g = selected_3g;
	}

	@Override
	public String toString() {
		if (tostr == null) {
			final StringBuilder s = new StringBuilder();
			if (uid > 0)
				s.append(uid + ": ");
			for (int i = 0; i < names.length; i++) {
				if (i != 0)
					s.append(", ");
				s.append(names[i]);
			}
			s.append("\n");
			tostr = s.toString();
		}
		return tostr;
	}
}