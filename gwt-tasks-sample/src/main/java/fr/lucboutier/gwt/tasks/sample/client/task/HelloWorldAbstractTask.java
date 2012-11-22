package fr.lucboutier.gwt.tasks.sample.client.task;

import fr.cloudcad.serializer.IStringSerializable;
import fr.lucboutier.gwt.tasks.Task;

/**
 * Simple task that returns "Hello World" as a result.
 * static {
		StringSerializerFactory.register(HelloWorldTask.class.getName(), );
	}
 * @author luc boutier
 */
public abstract class HelloWorldAbstractTask extends Task implements IStringSerializable {
	private String parameter;
	private String result;

	@Override
	public void execute() {
		this.result = "Hello " + this.parameter;
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
}