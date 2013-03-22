package fr.lucboutier.gwt.tasks;

import com.google.gwt.core.shared.GWT;
import com.kfuntak.gwt.json.serialization.client.JsonSerializable;
import com.kfuntak.gwt.json.serialization.client.Serializer;

import fr.lucboutier.gwt.webworker.client.DedicatedWorkerEntryPoint;
import fr.lucboutier.gwt.webworker.client.MessageEvent;
import fr.lucboutier.gwt.webworker.client.MessageHandler;

/**
 * All task must extend this class.
 * 
 * @author luc boutier
 */
public abstract class Task<T extends Object> extends DedicatedWorkerEntryPoint implements MessageHandler {
	public static final String TASK_COMPLETED_FLAG = "TC::";
	public static final String TASK_ERROR_FLAG = "TERR::";
	public static final String TASK_LOG_FLAG = "TLOG::";

	private final Serializer serializer = GWT.create(Serializer.class);

	@Override
	public void onWorkerLoad() {
		// register for messages to get parameters.
		setOnMessage(this);
	}

	/**
	 * This method is used for web-worker only.
	 * 
	 * @param event The web worker message event.
	 */
	@SuppressWarnings("rawtypes")
	public void onMessage(MessageEvent event) {
		// parse parameters
		String parameter = event.getDataAsString();
		final Task task;
		if (this instanceof JsonSerializable) {
			task = this.serializer.deSerialize(parameter, this.getClass());
		} else {
			task = this;
		}
		try {
			task.execute();
			if (this instanceof JsonSerializable) {
				postMessage(TASK_COMPLETED_FLAG + this.serializer.serialize(task));
			} else {
				postMessage(TASK_COMPLETED_FLAG);
			}
		} catch (Throwable t) {
			postMessage(TASK_ERROR_FLAG + t.getMessage());
		}
	}

	/**
	 * Execute the task and return a result.
	 * 
	 * @return A result object.
	 */
	public abstract T execute();
}