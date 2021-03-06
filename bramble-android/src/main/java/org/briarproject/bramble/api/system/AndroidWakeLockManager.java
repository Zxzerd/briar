package org.briarproject.bramble.api.system;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;

import java.util.concurrent.Executor;

@NotNullByDefault
public interface AndroidWakeLockManager {

	/**
	 * Creates a wake lock with the given tag. The tag is only used for
	 * logging; the underlying OS wake lock will use its own tag.
	 */
	AndroidWakeLock createWakeLock(String tag);

	/**
	 * Runs the given task while holding a wake lock.
	 */
	void runWakefully(Runnable r, String tag);

	/**
	 * Submits the given task to the given executor while holding a wake lock.
	 * The lock is released when the task completes, or if an exception is
	 * thrown while submitting or running the task.
	 */
	void executeWakefully(Runnable r, Executor executor, String tag);
}
