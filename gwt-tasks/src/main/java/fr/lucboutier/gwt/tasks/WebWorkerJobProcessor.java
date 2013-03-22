package fr.lucboutier.gwt.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.shared.GWT;
import com.kfuntak.gwt.json.serialization.client.JsonSerializable;
import com.kfuntak.gwt.json.serialization.client.Serializer;

import fr.lucboutier.gwt.webworker.client.MessageEvent;
import fr.lucboutier.gwt.webworker.client.MessageHandler;
import fr.lucboutier.gwt.webworker.client.Worker;

/**
 * A job processor that process tasks using web workers.
 * 
 * @author luc boutier
 */
public class WebWorkerJobProcessor implements IJobProcessor {
	private static final Logger LOGGER = Logger.getLogger(WebWorkerJobProcessor.class.getName());

	private final Serializer serializer = GWT.create(Serializer.class);

	private static final int MAX_WORKERS = 4;
	private int currentActiveWorkers = 0;
	private int currentHiddleWorkers = 0;
	private List<WebWorkerJob> pendingJobs = new ArrayList<WebWorkerJob>();

	// map of hidle workers
	private Map<String, List<TaskWorker>> hidleWorkers = new HashMap<String, List<TaskWorker>>();

	@Override
	public void processJob(final Job job) {
		final Task<?>[] tasks = job.getTasks();
		if (tasks == null || tasks.length == 0) {
			job.getCallback().onCompleted(tasks);
		} else {
			pendingJobs.add(new WebWorkerJob(job));
			launchTask();
		}
	}

	/**
	 * Start the next task if a worker is available.
	 */
	private void launchTask() {
		if (pendingJobs.size() == 0) {
			return; // if there is not more pending jobs then return.
		}
		if (currentActiveWorkers >= MAX_WORKERS) {
			return; // if we launched already the maximum number of workers then don't launch now.
		}
		final WebWorkerJob webWorkerJob = pendingJobs.get(0);

		final int taskIndex = webWorkerJob.getAndIncrementCurrentTaskIndex();

		if (taskIndex < webWorkerJob.getJob().getTasks().length) {
			final Task<?> currentTask = webWorkerJob.getJob().getTasks()[taskIndex];

			TaskWorker worker = createWorker(currentTask);
			worker.start(webWorkerJob, taskIndex);

			// launch the next task
			launchTask();
		}
	}

	private TaskWorker createWorker(Task<?> task) {
		String taskClassName = task.getClass().getName();
		String workerName = taskClassName.substring(taskClassName.lastIndexOf(".") + 1);

		List<TaskWorker> taskWorkers = this.hidleWorkers.get(workerName);
		if (taskWorkers != null) {
			if (taskWorkers.size() > 0) {
				// return the worker for this class type.
				this.currentHiddleWorkers--;
				return taskWorkers.remove(0);
			} else {
				this.hidleWorkers.remove(workerName);
			}
		}

		String workerPath = "../" + workerName + "/" + workerName + ".nocache.js";
		if (LOGGER.isLoggable(Level.FINER)) {
			LOGGER.finer("Loading worker from " + workerPath);
		}
		Worker worker = Worker.create(workerPath);
		TaskWorker taskWorker = new TaskWorker(worker, workerName);

		if (MAX_WORKERS <= (currentActiveWorkers + currentHiddleWorkers)) {
			removeIdleWorker();
		}

		return taskWorker;
	}

	/** Add a {@link TaskWorker} to the list of hiddle workers. */
	private void addIdleWorkers(final TaskWorker taskWorker) {
		List<TaskWorker> taskWorkers = this.hidleWorkers.get(taskWorker.workerName);
		if (taskWorkers == null) {
			taskWorkers = new ArrayList<WebWorkerJobProcessor.TaskWorker>();
			this.hidleWorkers.put(taskWorker.workerName, taskWorkers);
		}
		taskWorkers.add(taskWorker);
		this.currentHiddleWorkers++;
	}

	/** Remove one of the hiddle workers. */
	private void removeIdleWorker() {
		boolean removed = false;
		Iterator<Entry<String, List<TaskWorker>>> iterator = this.hidleWorkers.entrySet().iterator();
		while (iterator.hasNext() && !removed) {
			Entry<String, List<TaskWorker>> entry = iterator.next();
			List<TaskWorker> taskWorkers = entry.getValue();
			if (taskWorkers.size() > 0) {
				TaskWorker taskWorker = taskWorkers.remove(0);
				taskWorker.worker.terminate();
				this.currentHiddleWorkers--;
				removed = true;
			}
			if (taskWorkers.isEmpty()) {
				this.hidleWorkers.remove(entry.getKey());
			}
		}
	}

	private void processWorkerMessage(final String messageStr, final WebWorkerJob webWorkerJob, final int taskIndex) {
		if (LOGGER.isLoggable(Level.FINER)) {
			LOGGER.finer("Message received from worker <" + messageStr + ">");
		}
		final Task<?> sourceTask = webWorkerJob.job.getTasks()[taskIndex];

		if (messageStr.startsWith(Task.TASK_COMPLETED_FLAG)) {
			currentActiveWorkers--;
			String data = messageStr.substring(Task.TASK_COMPLETED_FLAG.length());

			if (sourceTask instanceof JsonSerializable) {
				webWorkerJob.results[taskIndex] = this.serializer.deSerialize(data);
			}

			// check if the job is completed.
			int completedTasks = webWorkerJob.incrementAndGetCompletedTasks(true);
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.fine("Task successfull  total completed <" + completedTasks + "> on <"
						+ webWorkerJob.getJob().getTasks().length + ">");
			}
			if (webWorkerJob.getJob().getTasks().length == completedTasks) {
				pendingJobs.remove(0);
				webWorkerJob.getJob().getCallback().onCompleted(webWorkerJob.results);
			}
			launchTask();
		} else if (messageStr.startsWith(Task.TASK_ERROR_FLAG)) {
			currentActiveWorkers--;
			String data = messageStr.substring(Task.TASK_ERROR_FLAG.length());
			LOGGER.severe("Task <" + sourceTask.getClass().getName() + "> failed with message <" + data + ">");
			int completedTasks = webWorkerJob.incrementAndGetCompletedTasks(false);
			if (webWorkerJob.getJob().getTasks().length == completedTasks) {
				pendingJobs.remove(0);
				webWorkerJob.getJob().getCallback().onCompleted(webWorkerJob.getJob().getTasks());
			}
		} else if (messageStr.startsWith(Task.TASK_LOG_FLAG)) {
			String data = messageStr.substring(Task.TASK_LOG_FLAG.length());
			LOGGER.info(data);
		} else {
			LOGGER.warning("Received unexpected message from Web worker " + messageStr);
		}
	}

	class TaskWorker implements MessageHandler {
		private final Worker worker;
		private final String workerName;

		private WebWorkerJob currentJob;
		private int currentTaskIndex;

		/**
		 * Create a new task worker.
		 * 
		 * @param worker The web-worker reference.
		 * @param workerName The name of the web-worker.
		 */
		public TaskWorker(final Worker worker, final String workerName) {
			this.worker = worker;
			this.workerName = workerName;
			this.worker.setOnMessage(this);
		}

		/**
		 * Start to process the given task.
		 * 
		 * @param workerJob The job that contains the task.
		 * @param taskIndex The index of the task in the job's task list.
		 */
		public void start(final WebWorkerJob workerJob, final int taskIndex) {
			this.currentJob = workerJob;
			this.currentTaskIndex = taskIndex;

			WebWorkerJobProcessor.this.currentActiveWorkers++;
			Task<?> currentTask = this.currentJob.getJob().getTasks()[this.currentTaskIndex];
			if (currentTask instanceof JsonSerializable) {
				this.worker.postMessage(WebWorkerJobProcessor.this.serializer.serialize(currentTask));
			} else {
				this.worker.postMessage("start!");
			}
			if (LOGGER.isLoggable(Level.FINER)) {
				LOGGER.finer("Task " + (taskIndex + 1) + " / " + this.currentJob.getJob().getTasks().length
						+ " started!");
			}
		}

		@Override
		public void onMessage(MessageEvent event) {
			addIdleWorkers(this);
			// The worker has completed it's task
			processWorkerMessage(event.getDataAsString(), currentJob, currentTaskIndex);
		}

		public WebWorkerJob getCurrentJob() {
			return currentJob;
		}

		public int getCurrentTaskIndex() {
			return currentTaskIndex;
		}
	}

	class WebWorkerJob {
		private final Job job;
		private final Object[] results;
		private int currentTaskIndex = 0;
		private int successTasks = 0;
		private int failedTasks = 0;

		public WebWorkerJob(final Job job) {
			this.job = job;
			this.results = new Object[job.getTasks().length];
		}

		public Job getJob() {
			return job;
		}

		public int getAndIncrementCurrentTaskIndex() {
			return currentTaskIndex++;
		}

		public int incrementAndGetCompletedTasks(boolean success) {
			if (success) {
				successTasks++;
			} else {
				failedTasks++;
			}
			return successTasks + failedTasks;
		}
	}
}