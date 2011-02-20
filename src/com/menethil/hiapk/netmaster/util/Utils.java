package com.menethil.hiapk.netmaster.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import com.menethil.hiapk.netmaster.BaseContext;
import com.menethil.hiapk.netmaster.R;

import android.content.Context;
import android.widget.Toast;

public class Utils {
	private static int isARMv6 = -1;

	/**
	 * 检查是否有 root 权限
	 * 
	 * @return boolean true if we have root
	 */
	public static boolean hasRootAccess() {
		final StringBuilder res = new StringBuilder();
		try {
			// Run an empty script just to check root access
			if (BaseContext.getInstance().getScriptRunner().runScriptAsRoot("exit 0", res) == 0) {
				return true;
			}
		} catch (Exception e) {
		}

		return false;
	}

	/**
	 * 复制指定资源ID到给定的路径，并设置权限
	 * 
	 * @param ctx
	 *            context
	 * @param resid
	 *            资源ID
	 * @param file
	 *            指定路径
	 * @param mode
	 *            文件权限 (E.g.: "755")
	 * @throws IOException
	 *             on error
	 * @throws InterruptedException
	 *             when interrupted
	 */
	public static void copyRawFile(Context ctx, int resid, File file, String mode) throws IOException,
			InterruptedException {
		final String abspath = file.getAbsolutePath();
		// Write the iptables binary
		final FileOutputStream out = new FileOutputStream(file);
		final InputStream is = ctx.getResources().openRawResource(resid);
		byte buf[] = new byte[1024];
		int len;
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		is.close();
		// Change the permissions
		Runtime.getRuntime().exec("chmod " + mode + " " + abspath).waitFor();
	}

	/**
	 * 检查是否 ARMv6 设备
	 * 
	 * @return true if this is ARMv6
	 */
	public static boolean isARMv6() {
		if (isARMv6 == -1) {
			BufferedReader r = null;
			try {
				isARMv6 = 0;
				r = new BufferedReader(new FileReader("/proc/cpuinfo"));
				for (String line = r.readLine(); line != null; line = r.readLine()) {
					if (line.startsWith("Processor") && line.contains("ARMv6")) {
						isARMv6 = 1;
						break;
					} else if (line.startsWith("CPU architecture") && (line.contains("6TE") || line.contains("5TE"))) {
						isARMv6 = 1;
						break;
					}
				}
			} catch (Exception ex) {
			} finally {
				if (r != null)
					try {
						r.close();
					} catch (Exception ex) {
					}
			}
		}
		return (isARMv6 == 1);
	}

	/**
	 * 检查文件是否存在.
	 * 
	 * @param ctx
	 *            context
	 * @param showErrors
	 *            indicates if errors should be alerted
	 * @return false if the binary files could not be installed
	 */
	public static boolean assertBinaries(Context ctx, boolean showErrors) {
		boolean changed = false;
		try {
			// Check iptables_g1
			File file = new File(ctx.getCacheDir(), "iptables_g1");
			if ((!file.exists()) && isARMv6()) {
				copyRawFile(ctx, R.raw.iptables_g1, file, "755");
				changed = true;
			}
			// Check iptables_n1
			file = new File(ctx.getCacheDir(), "iptables_n1");
			if ((!file.exists()) && (!isARMv6())) {
				copyRawFile(ctx, R.raw.iptables_n1, file, "755");
				changed = true;
			}
			// Check busybox
			file = new File(ctx.getCacheDir(), "busybox_g1");
			if (!file.exists()) {
				copyRawFile(ctx, R.raw.busybox_g1, file, "755");
				changed = true;
			}
			if (changed) {
				Toast.makeText(ctx, R.string.toast_bin_installed, Toast.LENGTH_LONG).show();
			}
		} catch (Exception e) {
			if (showErrors)
				// alert(ctx, "Error installing binary files: " + e);
				return false;
		}
		return true;
	}
}
