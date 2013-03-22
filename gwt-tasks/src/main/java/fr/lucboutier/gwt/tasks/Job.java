package fr.lucboutier.gwt.tasks;

/**
 * A job composed of multiple tasks.
 * 
 * @author luc boutier
 */
public class Job {
	private final IJobCompletedCallback callback;
	private final Task<?>[] tasks;

	/**
	 * Create a new job.
	 * 
	 * @param tasks The tasks part of the job.
	 * @param callback The callback to trigger once the job is completed.
	 */
	public Job(Task<?>[] tasks, IJobCompletedCallback callback) {
		this.tasks = tasks;
		this.callback = callback;
	}

	public Task<?>[] getTasks() {
		return tasks;
	}

	public IJobCompletedCallback getCallback() {
		return callback;
	}
}