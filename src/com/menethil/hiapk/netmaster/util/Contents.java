package com.menethil.hiapk.netmaster.util;

public class Contents {
	/** 版本 */
	public static final String VERSION = "1.4.8";
	/** 指定 UID 给 "任何程序" */
	public static final int SPECIAL_UID_ANY = -10;
	/** 指定 UID 给 Linux Kernel */
	public static final int SPECIAL_UID_KERNEL = -11;
	/** 脚本文件名 */
	public static final String SCRIPT_FILE = "droidwall.sh";

	// 设置
	public static final String PREFS_NAME = "DroidWallPrefs";
	public static final String PREF_3G_UIDS = "AllowedUids3G";
	public static final String PREF_WIFI_UIDS = "AllowedUidsWifi";
	public static final String PREF_PASSWORD = "Password";
	public static final String PREF_MODE = "BlockMode";
	public static final String PREF_ENABLED = "Enabled";
	public static final String PREF_LOGENABLED = "LogEnabled";
	// 模式
	public static final String MODE_WHITELIST = "whitelist";
	public static final String MODE_BLACKLIST = "blacklist";
	// 消息
	public static final String STATUS_CHANGED_MSG = "com.googlecode.droidwall.intent.action.STATUS_CHANGED";
	public static final String TOGGLE_REQUEST_MSG = "com.googlecode.droidwall.intent.action.TOGGLE_REQUEST";
	public static final String STATUS_EXTRA = "com.googlecode.droidwall.intent.extra.STATUS";
}
