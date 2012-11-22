package fr.lucboutier.gwt.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.cloudcad.serializer.IStringSerializable;
import fr.cloudcad.serializer.StringSerializerFactory;
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

	private static final int MAX_WORKERS = 4;
	private int currentActiveWorkers = 0;
	private List<WebWorkerJob> pendingJobs = new ArrayList<WebWorkerJob>();

	@Override
	public void processJob(final Job job) {
		final Task[] tasks = job.getTasks();
		if (tasks == null || tasks.length == 0) {
			job.getCallback().onCompleted();
		} else {
			pendingJobs.add(new WebWorkerJob(job));
			launchTask();
		}
	}

	private void launchTask() {
		if (pendingJobs.size() == 0) {
			return; // if there is not more pending jobs then return.
		}
		if (currentActiveWorkers >= MAX_WORKERS) {
			return; // if we launched already the maximum number of workers then don't launch now.
		}
		final WebWorkerJob webWorkerJob = pendingJobs.get(0);
		if (webWorkerJob.getCurrentTaskIndex() < webWorkerJob.getJob().getTasks().length) {
			final Task currentTask = webWorkerJob.getJob().getTasks()[webWorkerJob.getCurrentTaskIndex()];
			String taskClassName = currentTask.getClass().getName();
			String workerName = taskClassName.substring(taskClassName.lastIndexOf(".") + 1);
			String workerPath = "../" + workerName + "/" + workerName + ".nocache.js";
			if (LOGGER.isLoggable(Level.FINER)) {
				LOGGER.finer("Loading worker from " + workerPath);
			}
			Worker worker = Worker.create(workerPath);
			worker.setOnMessage(new MessageHandler() {
				@Override
				public void onMessage(MessageEvent event) {
					String messageStr = event.getDataAsString();
					processWorkerMessage(messageStr, webWorkerJob, currentTask);
				}
			});
			currentActiveWorkers++;
			if (currentTask instanceof IStringSerializable) {
				worker.postMessage(StringSerializerFactory.getSerializer(currentTask.getClass().getName()).serialize(
						currentTask));
			} else {
				worker.postMessage("start!");
			}
			if (LOGGER.isLoggable(Level.FINER)) {
				LOGGER.finer("Worker <" + workerName + "> started!");
			}
		}
	}

	private void processWorkerMessage(final String messageStr, final WebWorkerJob webWorkerJob, final Task currentTask) {
		if (messageStr.startsWith(Task.TASK_COMPLETED_FLAG)) {
			currentActiveWorkers--;
			String data = messageStr.substring(Task.TASK_COMPLETED_FLAG.length());
			if (currentTask instanceof IStringSerializable) {
				StringSerializerFactory.getSerializer(currentTask.getClass().getName()).deSerialize(data, currentTask);
			}
			// check if the job is completed.
			int completedTasks = webWorkerJob.incrementAndGetCompletedTasks(true);
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.fine("Task successfull  total completed <" + completedTasks + "> on <"
						+ webWorkerJob.getJob().getTasks().length + ">");
			}
			if (webWorkerJob.getJob().getTasks().length == completedTasks) {
				pendingJobs.remove(0);
				webWorkerJob.getJob().getCallback().onCompleted();
			}
			launchTask();
		} else if (messageStr.startsWith(Task.TASK_ERROR_FLAG)) {
			currentActiveWorkers--;
			String data = messageStr.substring(Task.TASK_ERROR_FLAG.length());
			LOGGER.severe("Task <" + currentTask.getClass().getName() + "> failed with message <" + data + ">");
			int completedTasks = webWorkerJob.incrementAndGetCompletedTasks(false);
			if (webWorkerJob.getJob().getTasks().length == completedTasks) {
				pendingJobs.remove(0);
				webWorkerJob.getJob().getCallback().onCompleted();
			}
		} else if (messageStr.startsWith(Task.TASK_LOG_FLAG)) {
			String data = messageStr.substring(Task.TASK_LOG_FLAG.length());
			LOGGER.info(data);
		} else {
			LOGGER.warning("Received unexpected message from Web worker " + messageStr);
		}
	}

	class WebWorkerJob {
		private final Job job;
		private int currentTaskIndex = 0;
		private int successTasks = 0;
		private int failedTasks = 0;

		public WebWorkerJob(final Job job) {
			this.job = job;
		}

		public Job getJob() {
			return job;
		}

		public int getCurrentTaskIndex() {
			return currentTaskIndex;
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