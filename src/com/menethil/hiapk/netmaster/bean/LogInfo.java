package com.menethil.hiapk.netmaster.bean;

import java.util.HashMap;

/**
 * 日志
 */
public class LogInfo {
	private int totalBlocked; // 阻止包总数
	private HashMap<String, Integer> dstBlocked; // 每个IP地址阻止的包数

	public LogInfo() {
		this.setDstBlocked(new HashMap<String, Integer>());
	}

	public void setTotalBlocked(int totalBlocked) {
		this.totalBlocked = totalBlocked;
	}

	public int getTotalBlocked() {
		return totalBlocked;
	}

	public void setDstBlocked(HashMap<String, Integer> dstBlocked) {
		this.dstBlocked = dstBlocked;
	}

	public HashMap<String, Integer> getDstBlocked() {
		return dstBlocked;
	}
}
