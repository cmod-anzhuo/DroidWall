package com.menethil.hiapk.netmaster;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.menethil.hiapk.netmaster.bean.BaseApp;
import com.menethil.hiapk.netmaster.util.Contents;
import com.menethil.hiapk.netmaster.util.ScriptRunner;
import com.menethil.hiapk.netmaster.util.Utils;

public class BaseContext {
	private static BaseContext baseContext;
	private Context context;
	// 应用程序缓存
	public BaseApp applications[] = null;
	// 是否有 root 权限
	private boolean rooted = false;
	// 是否 ARMv6 设备 (-1: unknown, 0: no, 1: yes)
	private boolean isARMv6 = false;
	private ScriptRunner scriptRunner;

	private BaseContext() {

	}

	public static synchronized BaseContext getInstance() {
		if (baseContext == null) {
			baseContext = new BaseContext();
		}
		return baseContext;
	}

	public void inistContext(Context context) {
		this.context = context;
		setScriptRunner(new ScriptRunner());
		setRooted(Utils.hasRootAccess());
		isARMv6 = Utils.isARMv6();
	}

	/**
	 * 防火墙是否打开
	 * 
	 * @return boolean
	 */
	public boolean isEnabled() {
		if (getContext() == null)
			return false;
		return getContext().getSharedPreferences(Contents.PREFS_NAME, 0).getBoolean(Contents.PREF_ENABLED, false);
	}

	/**
	 * 设置防火墙状态并发送通知
	 * 
	 * @param enabled
	 *            enabled flag
	 */
	public void setEnabled(boolean enabled) {
		if (getContext() == null)
			return;
		final SharedPreferences prefs = getContext().getSharedPreferences(Contents.PREFS_NAME, 0);
		if (prefs.getBoolean(Contents.PREF_ENABLED, false) == enabled) {
			return;
		}
		final Editor edit = prefs.edit();
		edit.putBoolean(Contents.PREF_ENABLED, enabled);
		if (!edit.commit()) {
			// 错误
			return;
		}
		/* 通知 */
		final Intent message = new Intent(Contents.STATUS_CHANGED_MSG);
		message.putExtra(Contents.STATUS_EXTRA, enabled);
		getContext().sendBroadcast(message);
	}

	/**
	 * 是否 ARMv6 设备
	 * 
	 * @return
	 */
	public boolean isARMv6() {
		return isARMv6;
	}

	public void setRooted(boolean rooted) {
		this.rooted = rooted;
	}

	public boolean isRooted() {
		return rooted;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Context getContext() {
		return context;
	}

	public void setScriptRunner(ScriptRunner scriptRunner) {
		this.scriptRunner = scriptRunner;
	}

	public ScriptRunner getScriptRunner() {
		return scriptRunner;
	}
}
