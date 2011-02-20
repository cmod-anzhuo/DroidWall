/**
 * Contains shared programming interfaces.
 * All iptables "communication" is handled by this class.
 * 
 * Copyright (C) 2009-2010  Rodrigo Zechin Rosauro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Rodrigo Zechin Rosauro
 * @version 1.0
 */

package com.menethil.hiapk.netmaster.util;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import com.menethil.hiapk.netmaster.BaseContext;
import com.menethil.hiapk.netmaster.bean.BaseApp;
import com.menethil.hiapk.netmaster.bean.LogInfo;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Contains shared programming interfaces. All iptables "communication" is
 * handled by this class.
 */
public final class Api {
	/**
	 * 创建 iptables 配置文件头
	 * 
	 * @param ctx
	 *            context
	 * @return script header
	 */
	private static String scriptHeader(Context ctx) {
		final String dir = ctx.getCacheDir().getAbsolutePath();
		final String myiptables = dir + (BaseContext.getInstance().isARMv6() ? "/iptables_g1" : "/iptables_n1");
		return "" + "IPTABLES=iptables\n" + "BUSYBOX=busybox\n" + "GREP=grep\n" + "ECHO=echo\n"
				+ "# Try to find busybox\n" + "if "
				+ dir
				+ "/busybox_g1 --help >/dev/null 2>/dev/null ; then\n"
				+ "	BUSYBOX="
				+ dir
				+ "/busybox_g1\n"
				+ "	GREP=\"$BUSYBOX grep\"\n"
				+ "	ECHO=\"$BUSYBOX echo\"\n"
				+ "elif busybox --help >/dev/null 2>/dev/null ; then\n"
				+ "	BUSYBOX=busybox\n"
				+ "elif /system/xbin/busybox --help >/dev/null 2>/dev/null ; then\n"
				+ "	BUSYBOX=/system/xbin/busybox\n"
				+ "elif /system/bin/busybox --help >/dev/null 2>/dev/null ; then\n"
				+ "	BUSYBOX=/system/bin/busybox\n"
				+ "fi\n"
				+ "# Try to find grep\n"
				+ "if ! $ECHO 1 | $GREP -q 1 >/dev/null 2>/dev/null ; then\n"
				+ "	if $ECHO 1 | $BUSYBOX grep -q 1 >/dev/null 2>/dev/null ; then\n"
				+ "		GREP=\"$BUSYBOX grep\"\n"
				+ "	fi\n"
				+ "	# Grep is absolutely required\n"
				+ "	if ! $ECHO 1 | $GREP -q 1 >/dev/null 2>/dev/null ; then\n"
				+ "		$ECHO The grep command is required. DroidWall will not work.\n"
				+ "		exit 1\n"
				+ "	fi\n"
				+ "fi\n"
				+ "# Try to find iptables\n"
				+ "if "
				+ myiptables
				+ " --version >/dev/null 2>/dev/null ; then\n"
				+ "	IPTABLES=" + myiptables + "\n" + "fi\n" + "";
	}

	/**
	 * Purge and re-add all rules (internal implementation).
	 * 
	 * @param ctx
	 *            application context (mandatory)
	 * @param uidsWifi
	 *            list of selected UIDs for WIFI to allow or disallow (depending
	 *            on the working mode)
	 * @param uids3g
	 *            list of selected UIDs for 2G/3G to allow or disallow
	 *            (depending on the working mode)
	 * @param showErrors
	 *            indicates if errors should be alerted
	 */
	private static boolean applyIptablesRulesImpl(Context ctx, List<Integer> uidsWifi, List<Integer> uids3g,
			boolean showErrors) {
		if (ctx == null) {
			return false;
		}
		Utils.assertBinaries(ctx, showErrors);
		final String ITFS_WIFI[] = { "tiwlan+", "wlan+", "eth+" };
		final String ITFS_3G[] = { "rmnet+", "pdp+", "ppp+", "uwbr+", "wimax+" };
		final SharedPreferences prefs = ctx.getSharedPreferences(Contents.PREFS_NAME, 0);
		final boolean whitelist = prefs.getString(Contents.PREF_MODE, Contents.MODE_WHITELIST).equals(
				Contents.MODE_WHITELIST);
		final boolean blacklist = !whitelist;
		final boolean logenabled = ctx.getSharedPreferences(Contents.PREFS_NAME, 0).getBoolean(
				Contents.PREF_LOGENABLED, false);

		final StringBuilder script = new StringBuilder();
		try {
			int code;
			script.append(scriptHeader(ctx));
			script.append(""
					+ "$IPTABLES --version || exit 1\n"
					+ "# Create the droidwall chains if necessary\n"
					+ "$IPTABLES -L droidwall >/dev/null 2>/dev/null || $IPTABLES --new droidwall || exit 2\n"
					+ "$IPTABLES -L droidwall-3g >/dev/null 2>/dev/null || $IPTABLES --new droidwall-3g || exit 3\n"
					+ "$IPTABLES -L droidwall-wifi >/dev/null 2>/dev/null || $IPTABLES --new droidwall-wifi || exit 4\n"
					+ "$IPTABLES -L droidwall-reject >/dev/null 2>/dev/null || $IPTABLES --new droidwall-reject || exit 5\n"
					+ "# Add droidwall chain to OUTPUT chain if necessary\n"
					+ "$IPTABLES -L OUTPUT | $GREP -q droidwall || $IPTABLES -A OUTPUT -j droidwall || exit 6\n"
					+ "# Flush existing rules\n" + "$IPTABLES -F droidwall || exit 7\n"
					+ "$IPTABLES -F droidwall-3g || exit 8\n" + "$IPTABLES -F droidwall-wifi || exit 9\n"
					+ "$IPTABLES -F droidwall-reject || exit 10\n" + "");
			// Check if logging is enabled
			if (logenabled) {
				script.append(""
						+ "# Create the log and reject rules (ignore errors on the LOG target just in case it is not available)\n"
						+ "$IPTABLES -A droidwall-reject -j LOG --log-prefix \"[DROIDWALL] \" --log-uid\n"
						+ "$IPTABLES -A droidwall-reject -j REJECT || exit 11\n" + "");
			} else {
				script.append("" + "# Create the reject rule (log disabled)\n"
						+ "$IPTABLES -A droidwall-reject -j REJECT || exit 11\n" + "");
			}
			if (whitelist && logenabled) {
				script.append("# Allow DNS lookups on white-list for a better logging (ignore errors)\n");
				script.append("$IPTABLES -A droidwall -p udp --dport 53 -j RETURN\n");
			}
			script.append("# Main rules (per interface)\n");
			for (final String itf : ITFS_3G) {
				script.append("$IPTABLES -A droidwall -o ").append(itf).append(" -j droidwall-3g || exit\n");
			}
			for (final String itf : ITFS_WIFI) {
				script.append("$IPTABLES -A droidwall -o ").append(itf).append(" -j droidwall-wifi || exit\n");
			}

			script.append("# Filtering rules\n");
			final String targetRule = (whitelist ? "RETURN" : "droidwall-reject");
			final boolean any_3g = uids3g.indexOf(Contents.SPECIAL_UID_ANY) >= 0;
			final boolean any_wifi = uidsWifi.indexOf(Contents.SPECIAL_UID_ANY) >= 0;
			if (whitelist && !any_wifi) {
				// When "white listing" wifi, we need to ensure that the dhcp
				// and wifi users are allowed
				int uid = android.os.Process.getUidForName("dhcp");
				if (uid != -1) {
					script.append("# dhcp user\n");
					script.append("$IPTABLES -A droidwall-wifi -m owner --uid-owner ").append(uid)
							.append(" -j RETURN || exit\n");
				}
				uid = android.os.Process.getUidForName("wifi");
				if (uid != -1) {
					script.append("# wifi user\n");
					script.append("$IPTABLES -A droidwall-wifi -m owner --uid-owner ").append(uid)
							.append(" -j RETURN || exit\n");
				}
			}
			if (any_3g) {
				if (blacklist) {
					/* block any application on this interface */
					script.append("$IPTABLES -A droidwall-3g -j ").append(targetRule).append(" || exit\n");
				}
			} else {
				/* release/block individual applications on this interface */
				for (final Integer uid : uids3g) {
					if (uid >= 0)
						script.append("$IPTABLES -A droidwall-3g -m owner --uid-owner ").append(uid).append(" -j ")
								.append(targetRule).append(" || exit\n");
				}
			}
			if (any_wifi) {
				if (blacklist) {
					/* block any application on this interface */
					script.append("$IPTABLES -A droidwall-wifi -j ").append(targetRule).append(" || exit\n");
				}
			} else {
				/* release/block individual applications on this interface */
				for (final Integer uid : uidsWifi) {
					if (uid >= 0)
						script.append("$IPTABLES -A droidwall-wifi -m owner --uid-owner ").append(uid).append(" -j ")
								.append(targetRule).append(" || exit\n");
				}
			}
			if (whitelist) {
				if (!any_3g) {
					if (uids3g.indexOf(Contents.SPECIAL_UID_KERNEL) >= 0) {
						script.append("# hack to allow kernel packets on white-list\n");
						script.append("$IPTABLES -A droidwall-3g -m owner --uid-owner 0:999999999 -j droidwall-reject || exit\n");
					} else {
						script.append("$IPTABLES -A droidwall-3g -j droidwall-reject || exit\n");
					}
				}
				if (!any_wifi) {
					if (uidsWifi.indexOf(Contents.SPECIAL_UID_KERNEL) >= 0) {
						script.append("# hack to allow kernel packets on white-list\n");
						script.append("$IPTABLES -A droidwall-wifi -m owner --uid-owner 0:999999999 -j droidwall-reject || exit\n");
					} else {
						script.append("$IPTABLES -A droidwall-wifi -j droidwall-reject || exit\n");
					}
				}
			} else {
				if (uids3g.indexOf(Contents.SPECIAL_UID_KERNEL) >= 0) {
					script.append("# hack to BLOCK kernel packets on black-list\n");
					script.append("$IPTABLES -A droidwall-3g -m owner --uid-owner 0:999999999 -j RETURN || exit\n");
					script.append("$IPTABLES -A droidwall-3g -j droidwall-reject || exit\n");
				}
				if (uidsWifi.indexOf(Contents.SPECIAL_UID_KERNEL) >= 0) {
					script.append("# hack to BLOCK kernel packets on black-list\n");
					script.append("$IPTABLES -A droidwall-wifi -m owner --uid-owner 0:999999999 -j RETURN || exit\n");
					script.append("$IPTABLES -A droidwall-wifi -j droidwall-reject || exit\n");
				}
			}
			final StringBuilder res = new StringBuilder();
			code = BaseContext.getInstance().getScriptRunner().runScriptAsRoot(script.toString(), res);
			if (showErrors && code != 0) {
				String msg = res.toString();
				Log.e("DroidWall", msg);
				// Remove unnecessary help message from output
				if (msg.indexOf("\nTry `iptables -h' or 'iptables --help' for more information.") != -1) {
					msg = msg.replace("\nTry `iptables -h' or 'iptables --help' for more information.", "");
				}
				// alert(ctx, "Error applying iptables rules. Exit code: " +
				// code + "\n\n" + msg.trim());
			} else {
				return true;
			}
		} catch (Exception e) {
			// if (showErrors)
			// alert(ctx, "error refreshing iptables: " + e);
		}
		return false;
	}

	/**
	 * Purge and re-add all saved rules (not in-memory ones). This is much
	 * faster than just calling "applyIptablesRules", since it don't need to
	 * read installed applications.
	 * 
	 * @param ctx
	 *            application context (mandatory)
	 * @param showErrors
	 *            indicates if errors should be alerted
	 */
	public static boolean applySavedIptablesRules(Context ctx, boolean showErrors) {
		if (ctx == null) {
			return false;
		}
		final SharedPreferences prefs = ctx.getSharedPreferences(Contents.PREFS_NAME, 0);
		final String savedUids_wifi = prefs.getString(Contents.PREF_WIFI_UIDS, "");
		final String savedUids_3g = prefs.getString(Contents.PREF_3G_UIDS, "");
		final List<Integer> uids_wifi = new LinkedList<Integer>();
		if (savedUids_wifi.length() > 0) {
			// Check which applications are allowed on wifi
			final StringTokenizer tok = new StringTokenizer(savedUids_wifi, "|");
			while (tok.hasMoreTokens()) {
				final String uid = tok.nextToken();
				if (!uid.equals("")) {
					try {
						uids_wifi.add(Integer.parseInt(uid));
					} catch (Exception ex) {
					}
				}
			}
		}
		final List<Integer> uids_3g = new LinkedList<Integer>();
		if (savedUids_3g.length() > 0) {
			// Check which applications are allowed on 2G/3G
			final StringTokenizer tok = new StringTokenizer(savedUids_3g, "|");
			while (tok.hasMoreTokens()) {
				final String uid = tok.nextToken();
				if (!uid.equals("")) {
					try {
						uids_3g.add(Integer.parseInt(uid));
					} catch (Exception ex) {
					}
				}
			}
		}
		return applyIptablesRulesImpl(ctx, uids_wifi, uids_3g, showErrors);
	}

	/**
	 * 保存并重载规则
	 * 
	 * @param ctx
	 *            application context (mandatory)
	 * @param showErrors
	 *            indicates if errors should be alerted
	 */
	public static boolean applyIptablesRules(Context ctx, boolean showErrors) {
		if (ctx == null) {
			return false;
		}
		saveRules(ctx);
		return applySavedIptablesRules(ctx, showErrors);
	}

	/**
	 * 保存规则设置
	 * 
	 * @param ctx
	 *            application context (mandatory)
	 */
	public static void saveRules(Context ctx) {
		final SharedPreferences prefs = ctx.getSharedPreferences(Contents.PREFS_NAME, 0);
		final BaseApp[] apps = getApps(ctx);
		// Builds a pipe-separated list of names
		final StringBuilder newuids_wifi = new StringBuilder();
		final StringBuilder newuids_3g = new StringBuilder();
		for (int i = 0; i < apps.length; i++) {
			if (apps[i].selected_wifi) {
				if (newuids_wifi.length() != 0)
					newuids_wifi.append('|');
				newuids_wifi.append(apps[i].uid);
			}
			if (apps[i].selected_3g) {
				if (newuids_3g.length() != 0)
					newuids_3g.append('|');
				newuids_3g.append(apps[i].uid);
			}
		}
		// save the new list of UIDs
		final Editor edit = prefs.edit();
		edit.putString(Contents.PREF_WIFI_UIDS, newuids_wifi.toString());
		edit.putString(Contents.PREF_3G_UIDS, newuids_3g.toString());
		edit.commit();
	}

	/**
	 * Purge all iptables rules.
	 * 
	 * @param ctx
	 *            mandatory context
	 * @param showErrors
	 *            indicates if errors should be alerted
	 * @return true if the rules were purged
	 */
	public static boolean purgeIptables(Context ctx, boolean showErrors) {
		StringBuilder res = new StringBuilder();
		try {
			Utils.assertBinaries(ctx, showErrors);
			int code = BaseContext
					.getInstance()
					.getScriptRunner()
					.runScriptAsRoot(
							scriptHeader(ctx) + "$IPTABLES -F droidwall\n" + "$IPTABLES -F droidwall-reject\n"
									+ "$IPTABLES -F droidwall-3g\n" + "$IPTABLES -F droidwall-wifi\n", res);
			if (code == -1) {
				if (showErrors)
					// alert(ctx, "error purging iptables. exit code: " + code +
					// "\n" + res);
					return false;
			}
			return true;
		} catch (Exception e) {
			// if (showErrors)
			// alert(ctx, "error purging iptables: " + e);
			return false;
		}
	}

	/**
	 * 显示规则配置列表
	 * 
	 * @param ctx
	 *            application context
	 */
	public static void showIptablesRules(Context ctx) {
		try {
			final StringBuilder res = new StringBuilder();
			BaseContext.getInstance().getScriptRunner()
					.runScriptAsRoot(scriptHeader(ctx) + "$ECHO $IPTABLES\n" + "$IPTABLES -L -v\n", res);
			// alert(ctx, res);
		} catch (Exception e) {
			// alert(ctx, "error: " + e);
		}
	}

	/**
	 * 删除日志
	 * 
	 * @param ctx
	 *            application context
	 * @return true if the clogs were cleared
	 */
	public static boolean clearLog(Context ctx) throws Exception {
		try {
			final StringBuilder res = new StringBuilder();
			int code = BaseContext.getInstance().getScriptRunner()
					.runScriptAsRoot("dmesg -c >/dev/null || exit\n", res);
			if (code != 0) {
				return false;
			}
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * 显示日志
	 * 
	 * @param ctx
	 *            application context
	 */
	public static void showLog(Context ctx) {
		try {
			StringBuilder res = new StringBuilder();
			int code = BaseContext.getInstance().getScriptRunner()
					.runScriptAsRoot(scriptHeader(ctx) + "dmesg | $GREP DROIDWALL\n", res);
			if (code != 0) {
				if (res.length() == 0) {
					res.append("Log is empty");
				}
				// alert(ctx, res);
				return;
			}
			final BufferedReader r = new BufferedReader(new StringReader(res.toString()));
			final Integer unknownUID = -99;
			res = new StringBuilder();
			String line;
			int start, end;
			Integer appid;
			final HashMap<Integer, LogInfo> map = new HashMap<Integer, LogInfo>();
			LogInfo loginfo = null;
			while ((line = r.readLine()) != null) {
				if (line.indexOf("[DROIDWALL]") == -1)
					continue;
				appid = unknownUID;
				if (((start = line.indexOf("UID=")) != -1) && ((end = line.indexOf(" ", start)) != -1)) {
					appid = Integer.parseInt(line.substring(start + 4, end));
				}
				loginfo = map.get(appid);
				if (loginfo == null) {
					loginfo = new LogInfo();
					map.put(appid, loginfo);
				}

				loginfo.setTotalBlocked(loginfo.getTotalBlocked() + 1);
				if (((start = line.indexOf("DST=")) != -1) && ((end = line.indexOf(" ", start)) != -1)) {
					String dst = line.substring(start + 4, end);
					if (loginfo.getDstBlocked().containsKey(dst)) {
						loginfo.getDstBlocked().put(dst, loginfo.getDstBlocked().get(dst) + 1);
					} else {
						loginfo.getDstBlocked().put(dst, 1);
					}
				}
			}
			final BaseApp[] apps = getApps(ctx);
			for (Integer id : map.keySet()) {
				res.append("App ID ");
				if (id != unknownUID) {
					res.append(id);
					for (BaseApp app : apps) {
						if (app.uid == id) {
							res.append(" (").append(app.names[0]);
							if (app.names.length > 1) {
								res.append(", ...)");
							} else {
								res.append(")");
							}
							break;
						}
					}
				} else {
					res.append("(kernel)");
				}
				loginfo = map.get(id);
				res.append(" - Blocked ").append(loginfo.getTotalBlocked()).append(" packets");
				if (loginfo.getDstBlocked().size() > 0) {
					res.append(" (");
					boolean first = true;
					for (String dst : loginfo.getDstBlocked().keySet()) {
						if (!first) {
							res.append(", ");
						}
						res.append(loginfo.getDstBlocked().get(dst)).append(" packets for ").append(dst);
						first = false;
					}
					res.append(")");
				}
				res.append("\n\n");
			}
			if (res.length() == 0) {
				res.append("Log is empty");
			}
			// alert(ctx, res);
		} catch (Exception e) {
			// alert(ctx, "error: " + e);
		}
	}

	/**
	 * 获取应用程序列表
	 * 
	 * @param ctx
	 *            application context (mandatory)
	 * @return a list of applications
	 */
	public static BaseApp[] getApps(Context ctx) {
		if (BaseContext.getInstance().applications != null) {
			// return cached instance
			return BaseContext.getInstance().applications;
		}
		final SharedPreferences prefs = ctx.getSharedPreferences(Contents.PREFS_NAME, 0);
		// allowed application names separated by pipe '|' (persisted)
		final String savedUids_wifi = prefs.getString(Contents.PREF_WIFI_UIDS, "");
		final String savedUids_3g = prefs.getString(Contents.PREF_3G_UIDS, "");
		int selected_wifi[] = new int[0];
		int selected_3g[] = new int[0];
		if (savedUids_wifi.length() > 0) {
			// Check which applications are allowed
			final StringTokenizer tok = new StringTokenizer(savedUids_wifi, "|");
			selected_wifi = new int[tok.countTokens()];
			for (int i = 0; i < selected_wifi.length; i++) {
				final String uid = tok.nextToken();
				if (!uid.equals("")) {
					try {
						selected_wifi[i] = Integer.parseInt(uid);
					} catch (Exception ex) {
						selected_wifi[i] = -1;
					}
				}
			}
			// Sort the array to allow using "Arrays.binarySearch" later
			Arrays.sort(selected_wifi);
		}
		if (savedUids_3g.length() > 0) {
			// Check which applications are allowed
			final StringTokenizer tok = new StringTokenizer(savedUids_3g, "|");
			selected_3g = new int[tok.countTokens()];
			for (int i = 0; i < selected_3g.length; i++) {
				final String uid = tok.nextToken();
				if (!uid.equals("")) {
					try {
						selected_3g[i] = Integer.parseInt(uid);
					} catch (Exception ex) {
						selected_3g[i] = -1;
					}
				}
			}
			// Sort the array to allow using "Arrays.binarySearch" later
			Arrays.sort(selected_3g);
		}
		try {
			final PackageManager pkgmanager = ctx.getPackageManager();
			final List<ApplicationInfo> installed = pkgmanager.getInstalledApplications(0);
			final HashMap<Integer, BaseApp> map = new HashMap<Integer, BaseApp>();
			final Editor edit = prefs.edit();
			boolean changed = false;
			String name = null;
			String cachekey = null;
			BaseApp app = null;
			for (final ApplicationInfo apinfo : installed) {
				app = map.get(apinfo.uid);
				// filter applications which are not allowed to access the
				// Internet
				if (app == null
						&& PackageManager.PERMISSION_GRANTED != pkgmanager.checkPermission(
								Manifest.permission.INTERNET, apinfo.packageName)) {
					continue;
				}
				// try to get the application label from our cache -
				// getApplicationLabel() is horribly slow!!!!
				cachekey = "cache.label." + apinfo.packageName;
				name = prefs.getString(cachekey, "");
				if (name.length() == 0) {
					// get label and put on cache
					name = pkgmanager.getApplicationLabel(apinfo).toString();
					edit.putString(cachekey, name);
					changed = true;
				}
				if (app == null) {
					app = new BaseApp();
					app.uid = apinfo.uid;
					app.names = new String[] { name };
					map.put(apinfo.uid, app);
				} else {
					final String newnames[] = new String[app.names.length + 1];
					System.arraycopy(app.names, 0, newnames, 0, app.names.length);
					newnames[app.names.length] = name;
					app.names = newnames;
				}
				// check if this application is selected
				if (!app.selected_wifi && Arrays.binarySearch(selected_wifi, app.uid) >= 0) {
					app.selected_wifi = true;
				}
				if (!app.selected_3g && Arrays.binarySearch(selected_3g, app.uid) >= 0) {
					app.selected_3g = true;
				}
			}
			if (changed) {
				edit.commit();
			}
			/* add special applications to the list */
			final BaseApp special[] = {
					new BaseApp(Contents.SPECIAL_UID_ANY, "(Any application) - Same as selecting all applications",
							false, false),
					new BaseApp(Contents.SPECIAL_UID_KERNEL, "(Kernel) - Linux kernel", false, false),
					new BaseApp(android.os.Process.getUidForName("root"), "(root) - Applications running as root",
							false, false),
					new BaseApp(android.os.Process.getUidForName("media"), "Media server", false, false),
					new BaseApp(android.os.Process.getUidForName("vpn"), "VPN networking", false, false),
					new BaseApp(android.os.Process.getUidForName("shell"), "Linux shell", false, false), };
			for (int i = 0; i < special.length; i++) {
				app = special[i];
				if (app.uid != -1 && !map.containsKey(app.uid)) {
					// check if this application is allowed
					if (Arrays.binarySearch(selected_wifi, app.uid) >= 0) {
						app.selected_wifi = true;
					}
					if (Arrays.binarySearch(selected_3g, app.uid) >= 0) {
						app.selected_3g = true;
					}
					map.put(app.uid, app);
				}
			}
			BaseContext.getInstance().applications = new BaseApp[map.size()];
			int index = 0;
			for (BaseApp application : map.values())
				BaseContext.getInstance().applications[index++] = application;
			return BaseContext.getInstance().applications;
		} catch (Exception e) {
			// alert(ctx, "error: " + e);
		}
		return null;
	}

	/**
	 * 程序被删除时，移除相关的设置
	 * 
	 * @param ctx
	 *            mandatory app context
	 * @param uid
	 *            应用程序 UID
	 */
	public static void applicationRemoved(Context ctx, int uid) {
		final SharedPreferences prefs = ctx.getSharedPreferences(Contents.PREFS_NAME, 0);
		final Editor editor = prefs.edit();
		// allowed application names separated by pipe '|' (persisted)
		final String savedUids_wifi = prefs.getString(Contents.PREF_WIFI_UIDS, "");
		final String savedUids_3g = prefs.getString(Contents.PREF_3G_UIDS, "");
		final String uid_str = uid + "";
		boolean changed = false;
		// look for the removed application in the "wi-fi" list
		if (savedUids_wifi.length() > 0) {
			final StringBuilder newuids = new StringBuilder();
			final StringTokenizer tok = new StringTokenizer(savedUids_wifi, "|");
			while (tok.hasMoreTokens()) {
				final String token = tok.nextToken();
				if (uid_str.equals(token)) {
					Log.d("DroidWall", "Removing UID " + token + " from the wi-fi list (package removed)!");
					changed = true;
				} else {
					if (newuids.length() > 0)
						newuids.append('|');
					newuids.append(token);
				}
			}
			if (changed) {
				editor.putString(Contents.PREF_WIFI_UIDS, newuids.toString());
			}
		}
		// look for the removed application in the "3g" list
		if (savedUids_3g.length() > 0) {
			final StringBuilder newuids = new StringBuilder();
			final StringTokenizer tok = new StringTokenizer(savedUids_3g, "|");
			while (tok.hasMoreTokens()) {
				final String token = tok.nextToken();
				if (uid_str.equals(token)) {
					Log.d("DroidWall", "Removing UID " + token + " from the 3G list (package removed)!");
					changed = true;
				} else {
					if (newuids.length() > 0)
						newuids.append('|');
					newuids.append(token);
				}
			}
			if (changed) {
				editor.putString(Contents.PREF_3G_UIDS, newuids.toString());
			}
		}
		// if anything has changed, save the new prefs...
		if (changed) {
			editor.commit();
			if (BaseContext.getInstance().isEnabled()) {
				// .. and also re-apply the rules if the firewall is enabled
				applySavedIptablesRules(ctx, false);
			}
		}
	}
}
