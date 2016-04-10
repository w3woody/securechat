/*
 * Copyright (c) 2016. William Edward Woody
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>
 *
 */

package com.chaosinmotion.securechat.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Uses the ThreadPoolExecutor to manage a global set of threadpools. This
 * is a thin wrapper which allows me to toss up a runnable. This also works
 * with the Android API to allow execution in the main thread, which is
 * often used by our code to make sure results are executed in the UI.
 *
 * In a way this models how we use GCD on iOS.
 *
 * Created by woody on 4/10/16.
 */
public class ThreadPool
{
	private static ThreadPool shared;
	private Handler handler;
	private ExecutorService threadPool;

	/**
	 * Get the shared object
	 * @return The shared thread pool object
	 */
	public static synchronized ThreadPool get()
	{
		if (shared == null) shared = new ThreadPool();
		return shared;
	}

	private ThreadPool()
	{
		// Start executor with custom factory
		ThreadFactory factory = new ThreadFactory() {
			private int index;
			@Override
			public Thread newThread(Runnable r)
			{
				Thread thread = new Thread(r,"Pool " + (++index));
				thread.setDaemon(true);
				return thread;
			}
		};
		threadPool = Executors.newFixedThreadPool(10,factory);

		// Create our handler
		handler = new Handler(Looper.getMainLooper());
	}

	/**
	 * Enqueue a runnable task in the background and return the allocated
	 * future object. This is provided solely to allow tasks to be
	 * canceled if they are in progress--a feature used by our networking
	 * code.
	 * @param r The task to execute
	 * @return The future object that can be used to cancel the operation
	 * if needed.
	 */
	public Future<?> enqueueAsync(Runnable r)
	{
		return threadPool.submit(r);
	}

	/**
	 * Enqueue a runnable task on the main thread. This allows our background
	 * tasks to surface results in the main UI thread so the UI can be
	 * updated appropriately
	 * @param r The task to execute on the main thread.
	 */
	public void enqueueMain(Runnable r)
	{
		handler.post(r);
	}
}
