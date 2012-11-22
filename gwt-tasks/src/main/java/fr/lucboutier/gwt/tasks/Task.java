package fr.lucboutier.gwt.tasks;

import fr.cloudcad.serializer.IStringSerializable;
import fr.cloudcad.serializer.StringSerializerFactory;
import fr.lucboutier.gwt.webworker.client.DedicatedWorkerEntryPoint;
import fr.lucboutier.gwt.webworker.client.MessageEvent;
import fr.lucboutier.gwt.webworker.client.MessageHandler;

/**
 * All task must extend this class.
 * 
 * @author luc boutier
 */
public abstract class Task extends DedicatedWorkerEntryPoint implements MessageHandler {
	public static final String TASK_COMPLETED_FLAG = "TC::";
	public static final String TASK_ERROR_FLAG = "TERR::";
	public static final String TASK_LOG_FLAG = "TLOG::";

	@Override
	public void onWorkerLoad() {
		// register for messages to get parameters.
		setOnMessage(this);
	}

	public void onMessage(MessageEvent event) {
		// parse parameters
		String parameter = event.getDataAsString();
		if (this instanceof IStringSerializable) {
			StringSerializerFactory.getSerializer(this.getClass().getName()).deSerialize(parameter, this);
		}
		try {
			this.execute();
			if (this instanceof IStringSerializable) {
				postMessage(TASK_COMPLETED_FLAG
						+ StringSerializerFactory.getSerializer(this.getClass().getName()).serialize(this));
			} else {
				postMessage(TASK_COMPLETED_FLAG);
			}
		} catch (Throwable t) {
			postMessage(TASK_ERROR_FLAG + t.getMessage());
		}
	}

	public abstract void execute();
}