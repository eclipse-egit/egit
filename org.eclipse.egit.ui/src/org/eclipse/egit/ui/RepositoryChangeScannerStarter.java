package org.eclipse.egit.ui;

import org.eclipse.e4.ui.workbench.UIEvents;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * OSGI service to start the RepositoryChangeScanner only after workbench
 * initialization.
 *
 */
@Component(property = EventConstants.EVENT_TOPIC + '='
		+ UIEvents.UILifeCycle.APP_STARTUP_COMPLETE)
public class RepositoryChangeScannerStarter implements EventHandler {

	@Override
	public void handleEvent(Event event) {
		Activator.getDefault().setupRepoChangeScanner();
	}

}
