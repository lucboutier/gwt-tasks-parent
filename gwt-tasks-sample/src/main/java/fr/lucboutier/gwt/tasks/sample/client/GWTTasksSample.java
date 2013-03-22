package fr.lucboutier.gwt.tasks.sample.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import fr.lucboutier.gwt.tasks.IJobCompletedCallback;
import fr.lucboutier.gwt.tasks.IJobProcessor;
import fr.lucboutier.gwt.tasks.Job;
import fr.lucboutier.gwt.tasks.RepeatingCommandJobProcessor;
import fr.lucboutier.gwt.tasks.Task;
import fr.lucboutier.gwt.tasks.WebWorkerJobProcessor;
import fr.lucboutier.gwt.tasks.sample.client.task.HelloWorldTask;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class GWTTasksSample implements EntryPoint {

	@Override
	public void onModuleLoad() {
		final VerticalPanel verticalPanel = new VerticalPanel();
		final Label label = new Label();
		final Button repeatingCommandButton = new Button();
		final Button webWorkerButton = new Button();
		final IJobProcessor jobProcessor = new RepeatingCommandJobProcessor();
		final IJobProcessor webWorkerjobProcessor = new WebWorkerJobProcessor();

		repeatingCommandButton.setText("Repeating Command Job Processor");
		webWorkerButton.setText("Web Worker Job Processor");

		verticalPanel.add(label);
		verticalPanel.add(repeatingCommandButton);
		verticalPanel.add(webWorkerButton);

		final HelloWorldTask task = new HelloWorldTask();

		final Job job = new Job(new Task[] { task }, new IJobCompletedCallback() {
			@Override
			public void onCompleted(Object[] results) {
				label.setText("Parameter <" + task.getParameter() + "> Result <" + results[0] + ">");
			}
		});

		repeatingCommandButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				task.setParameter("no worker");
				jobProcessor.processJob(job);
			}
		});

		webWorkerButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				task.setParameter("worker");
				webWorkerjobProcessor.processJob(job);
			}
		});

		RootLayoutPanel.get().add(verticalPanel);
	}
}