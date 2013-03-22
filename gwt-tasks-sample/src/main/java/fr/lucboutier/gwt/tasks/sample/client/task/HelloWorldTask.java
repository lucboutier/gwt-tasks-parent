package fr.lucboutier.gwt.tasks.sample.client.task;

import com.kfuntak.gwt.json.serialization.client.JsonSerializable;

import fr.lucboutier.gwt.tasks.Task;

/**
 * Simple task that returns "Hello <parameter>" as a result.
 * 
 * @author luc boutier
 */
public class HelloWorldTask extends Task<String> implements JsonSerializable {
	private String parameter;

	@Override
	public String execute() {
		return "Hello " + this.parameter;
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
}