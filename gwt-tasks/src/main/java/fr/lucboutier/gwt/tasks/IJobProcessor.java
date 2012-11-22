package fr.lucboutier.gwt.tasks;

/**
 * Interface for the component responsible to process a job.
 * 
 * @author luc boutier
 */
public interface IJobProcessor {
	/**
	 * Process all the tasks of the job and call the onComplete method once done.
	 * 
	 * @param job The job to process.
	 */
	void processJob(final Job job);
}