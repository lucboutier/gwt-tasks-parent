package fr.lucboutier.gwt.tasks;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link IJobProcessor} implementation based on the GWT's RepeatingCommand.
 * 
 * @author luc boutier
 */
public class ThreadJobProcessor implements IJobProcessor {
	private static final Logger LOGGER = Logger.getLogger(ThreadJobProcessor.class.getName());

	private final boolean blockCallingThread;
	private final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
	private final ThreadPoolExecutor executor;

	/**
	 * Create a {@link ThreadJobProcessor} that doesn't block the calling thread on a processJob request and that uses
	 * as many threads as CPUs.
	 */
	public ThreadJobProcessor() {
		this.blockCallingThread = false;
		executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime()
				.availableProcessors(), 60, TimeUnit.SECONDS, workQueue);
	}

	/**
	 * Create a {@link ThreadJobProcessor} that uses as many threads as CPUs.
	 * 
	 * @param blockCallingThread <code>true</code> if the calling thread should be blocked on
	 *            {@link ThreadJobProcessor#processJob(Job)} calls until the job is completed, <code>false</code> if
	 *            not.
	 */
	public ThreadJobProcessor(boolean blockCallingThread) {
		this.blockCallingThread = blockCallingThread;
		executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime()
				.availableProcessors(), 60, TimeUnit.SECONDS, workQueue);
	}

	/**
	 * Create a new {@link ThreadJobProcessor}.
	 * 
	 * @param blockCallingThread <code>true</code> if the calling thread should be blocked on
	 *            {@link ThreadJobProcessor#processJob(Job)} calls until the job is completed, <code>false</code> if
	 *            not.
	 * @param nbThreads The number of threads to be used for the {@link ThreadJobProcessor} (min = max).
	 */
	public ThreadJobProcessor(boolean blockCallingThread, int nbThreads) {
		this.blockCallingThread = blockCallingThread;
		executor = new ThreadPoolExecutor(nbThreads, nbThreads, 60, TimeUnit.SECONDS, workQueue);
	}

	/**
	 * Create a new {@link ThreadJobProcessor}.
	 * 
	 * @param blockCallingThread <code>true</code> if the calling thread should be blocked on
	 *            {@link ThreadJobProcessor#processJob(Job)} calls until the job is completed, <code>false</code> if
	 *            not.
	 * @param minThreads The minimum number of threads to use in the thread pool.
	 * @param maxThreads The maximum number of threads to use in the thread pool.
	 */
	public ThreadJobProcessor(boolean blockCallingThread, int minThreads, int maxThreads) {
		this.blockCallingThread = blockCallingThread;
		executor = new ThreadPoolExecutor(minThreads, maxThreads, 60, TimeUnit.SECONDS, workQueue);
	}

	/**
	 * Shutdown the thread pool (no more tasks will be processed) pending jobs may not be completed.
	 */
	public void shutdown() {
		executor.shutdown();
	}

	@Override
	public void processJob(final Job job) {
		final Task[] tasks = job.getTasks();
		final AtomicInteger completedCount = new AtomicInteger();
		final AtomicInteger successCount = new AtomicInteger();

		if (blockCallingThread) {
			Future<?>[] futures = new Future<?>[tasks.length];
			for (int i = 0; i < tasks.length; i++) {
				final Task task = tasks[i];
				futures[i] = executor.submit(new Runnable() {
					@Override
					public void run() {
						try {
							task.execute();
							onTaskCompleted(job, task, tasks.length, completedCount, successCount, true);
						} catch (Throwable t) {
							LOGGER.log(Level.SEVERE, "Error while processing task", t);
							onTaskCompleted(job, task, tasks.length, completedCount, successCount, false);
						}
					}
				});
			}

			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (InterruptedException e) {
					LOGGER.log(Level.SEVERE, "Unable to process job correctly", e);
				} catch (ExecutionException e) {
					LOGGER.log(Level.SEVERE, "Unable to process job correctly", e);
				}
			}
		} else {
			for (final Task task : tasks) {
				executor.submit(new Runnable() {
					@Override
					public void run() {
						try {
							task.execute();
							onTaskCompleted(job, task, tasks.length, completedCount, successCount, true);
						} catch (Throwable t) {
							LOGGER.log(Level.SEVERE, "Error while processing task", t);
							onTaskCompleted(job, task, tasks.length, completedCount, successCount, false);
						}
					}
				});
			}
		}
	}

	private void onTaskCompleted(final Job job, final Task task, final int totalCount,
			final AtomicInteger completedCount, final AtomicInteger successCount, boolean success) {
		int currentCompleted = completedCount.incrementAndGet();
		if (success) {
			successCount.incrementAndGet();
		}
		int successCompleted = successCount.get();
		LOGGER.info("Completed " + currentCompleted + " tasks on " + totalCount + " success " + successCompleted);
		if (currentCompleted == totalCount) {
			job.getCallback().onCompleted();
		}
	}
}