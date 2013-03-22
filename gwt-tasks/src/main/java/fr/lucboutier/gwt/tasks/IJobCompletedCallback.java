package fr.lucboutier.gwt.tasks;

/**
 * Callback triggered once a job is completed.
 * 
 * @author luc boutier
 */
public interface IJobCompletedCallback {
	/**
	 * Method triggered once the job is completed.
	 * 
	 * @param results The tasks results (object returned by the execute method). Position of results in the array
	 *            matches the job's tasks array.
	 */
	void onCompleted(Object[] results);
}