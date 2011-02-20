package com.menethil.hiapk.netmaster.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.menethil.hiapk.netmaster.BaseContext;

import android.content.Context;

public class ScriptRunner {
	private Context context = BaseContext.getInstance().getContext();

	/**
	 * Runs a script, wither as root or as a regular user (multiple commands
	 * separated by "\n").
	 * 
	 * @param ctx
	 *            mandatory context
	 * @param script
	 *            the script to be executed
	 * @param res
	 *            the script output response (stdout + stderr)
	 * @param timeout
	 *            timeout in milliseconds (-1 for none)
	 * @return the script exit code
	 */
	public int runScript(String script, StringBuilder res, long timeout, boolean asroot) {
		final File file = new File(context.getCacheDir(), Contents.SCRIPT_FILE);
		final ScriptRunnerWithThread runner = new ScriptRunnerWithThread(file, script, res, asroot);
		runner.start();
		try {
			if (timeout > 0) {
				runner.join(timeout);
			} else {
				runner.join();
			}
			if (runner.isAlive()) {
				// Timed-out
				runner.interrupt();
				runner.join(150);
				runner.destroy();
				runner.join(50);
			}
		} catch (InterruptedException ex) {
		}
		return runner.exitcode;
	}

	/**
	 * Runs a script as root (multiple commands separated by "\n").
	 * 
	 * @param ctx
	 *            mandatory context
	 * @param script
	 *            the script to be executed
	 * @param res
	 *            the script output response (stdout + stderr)
	 * @param timeout
	 *            timeout in milliseconds (-1 for none)
	 * @return the script exit code
	 */
	public int runScriptAsRoot(String script, StringBuilder res, long timeout) {
		return runScript(script, res, timeout, true);
	}

	/**
	 * Runs a script as root (multiple commands separated by "\n") with a
	 * default timeout of 20 seconds.
	 * 
	 * @param ctx
	 *            mandatory context
	 * @param script
	 *            the script to be executed
	 * @param res
	 *            the script output response (stdout + stderr)
	 * @param timeout
	 *            timeout in milliseconds (-1 for none)
	 * @return the script exit code
	 * @throws IOException
	 *             on any error executing the script, or writing it to disk
	 */
	public int runScriptAsRoot(String script, StringBuilder res) throws IOException {
		return runScriptAsRoot(script, res, 40000);
	}

	/**
	 * Runs a script as a regular user (multiple commands separated by "\n")
	 * with a default timeout of 20 seconds.
	 * 
	 * @param ctx
	 *            mandatory context
	 * @param script
	 *            the script to be executed
	 * @param res
	 *            the script output response (stdout + stderr)
	 * @param timeout
	 *            timeout in milliseconds (-1 for none)
	 * @return the script exit code
	 * @throws IOException
	 *             on any error executing the script, or writing it to disk
	 */
	public int runScript(String script, StringBuilder res) throws IOException {
		return runScript(script, res, 40000, false);
	}

	/**
	 * Internal thread used to execute scripts (as root or not).
	 */
	private final class ScriptRunnerWithThread extends Thread {
		private final File file;
		private final String script;
		private final StringBuilder res;
		private final boolean asroot;
		public int exitcode = -1;
		private Process exec;

		/**
		 * Creates a new script runner.
		 * 
		 * @param file
		 *            temporary script file
		 * @param script
		 *            script to run
		 * @param res
		 *            response output
		 * @param asroot
		 *            if true, executes the script as root
		 */
		public ScriptRunnerWithThread(File file, String script, StringBuilder res, boolean asroot) {
			this.file = file;
			this.script = script;
			this.res = res;
			this.asroot = asroot;
		}

		@Override
		public void run() {
			try {
				file.createNewFile();
				final String abspath = file.getAbsolutePath();
				// make sure we have execution permission on the script file
				Runtime.getRuntime().exec("chmod 777 " + abspath).waitFor();
				// Write the script to be executed
				final OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file));
				if (new File("/system/bin/sh").exists()) {
					out.write("#!/system/bin/sh\n");
				}
				out.write(script);
				if (!script.endsWith("\n"))
					out.write("\n");
				out.write("exit\n");
				out.flush();
				out.close();
				if (this.asroot) {
					// Create the "su" request to run the script
					exec = Runtime.getRuntime().exec("su -c " + abspath);
				} else {
					// Create the "sh" request to run the script
					exec = Runtime.getRuntime().exec("sh " + abspath);
				}
				InputStreamReader r = new InputStreamReader(exec.getInputStream());
				final char buf[] = new char[1024];
				int read = 0;
				// Consume the "stdout"
				while ((read = r.read(buf)) != -1) {
					if (res != null)
						res.append(buf, 0, read);
				}
				// Consume the "stderr"
				r = new InputStreamReader(exec.getErrorStream());
				read = 0;
				while ((read = r.read(buf)) != -1) {
					if (res != null)
						res.append(buf, 0, read);
				}
				// get the process exit code
				if (exec != null)
					this.exitcode = exec.waitFor();
			} catch (InterruptedException ex) {
				if (res != null)
					res.append("\nOperation timed-out");
			} catch (Exception ex) {
				if (res != null)
					res.append("\n" + ex);
			} finally {
				destroy();
			}
		}

		/**
		 * Destroy this script runner
		 */
		public synchronized void destroy() {
			if (exec != null)
				exec.destroy();
			exec = null;
		}
	}
}
