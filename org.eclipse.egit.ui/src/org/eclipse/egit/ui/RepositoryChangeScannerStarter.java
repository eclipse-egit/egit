/*******************************************************************************
 * Copyright (C) 2020 Michael Keppler <michael.keppler@gmx.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui;

import org.eclipse.e4.ui.workbench.UIEvents;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * OSGI service to start the RepositoryChangeScanner only after workbench
 * initialization.
 */
@Component(property = EventConstants.EVENT_TOPIC + '='
		+ UIEvents.UILifeCycle.APP_STARTUP_COMPLETE)
public class RepositoryChangeScannerStarter implements EventHandler {

	@Override
	public void handleEvent(Event event) {
		Activator.getDefault().setupRepoChangeScanner();
	}

}
