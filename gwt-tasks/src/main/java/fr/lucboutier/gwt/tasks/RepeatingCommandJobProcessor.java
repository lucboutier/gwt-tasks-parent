package fr.lucboutier.gwt.tasks;

import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

/**
 * An {@link IJobProcessor} implementation based on the GWT's RepeatingCommand.
 * 
 * @author luc boutier
 */
public class RepeatingCommandJobProcessor implements IJobProcessor {
	private static final Logger LOGGER = Logger.getLogger(RepeatingCommandJobProcessor.class.getName());

	@Override
	public void processJob(final Job job) {
		final Task[] tasks = job.getTasks();
		RepeatingCommand repeatingCommand = new RepeatingCommand() {
			int current = 0;

			public boolean execute() {
				if (current < tasks.length) {
					LOGGER.info("Task " + current + "/" + tasks.length);
					Task task = tasks[current];
					task.execute();
					current++;
					return true;
				}
				job.getCallback().onCompleted();
				return false;
			}
		};
		Scheduler.get().scheduleIncremental(repeatingCommand);
	}
}