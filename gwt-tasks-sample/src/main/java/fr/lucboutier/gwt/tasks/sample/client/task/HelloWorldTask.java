package fr.lucboutier.gwt.tasks.sample.client.task;

import fr.cloudcad.serializer.StringSerializerFactory;
import fr.lucboutier.gwt.tasks.sample.client.serializer.HelloWorldAbstractTaskSerializer;

/**
 * The actual task is just used to register the required generated serializers.
 * 
 * @author luc boutier
 */
public class HelloWorldTask extends HelloWorldAbstractTask {
	static {
		StringSerializerFactory.register(HelloWorldTask.class.getName(), new HelloWorldAbstractTaskSerializer());
	}
}