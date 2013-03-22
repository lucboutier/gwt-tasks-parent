package fr.lucboutier.gwt.tasks;

import java.util.logging.Level;
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
		final Task<?>[] tasks = job.getTasks();
		final Object[] results = new Object[tasks.length];
		RepeatingCommand repeatingCommand = new RepeatingCommand() {
			int current = 0;

			public boolean execute() {
				if (current < tasks.length) {
					if (LOGGER.isLoggable(Level.FINER)) {
						LOGGER.finer("Task " + (current + 1) + "/" + tasks.length);
					}
					results[current] = tasks[current].execute();
					current++;
					return true;
				}
				job.getCallback().onCompleted(results);
				return false;
			}
		};
		Scheduler.get().scheduleIncremental(repeatingCommand);
	}
}