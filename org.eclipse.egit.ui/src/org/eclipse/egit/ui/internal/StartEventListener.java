/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Initializes some EGit components that rely on the workbench having been
 * created.
 */
@Component(property = EventConstants.EVENT_TOPIC + '='
		+ UIEvents.UILifeCycle.APP_STARTUP_COMPLETE)
public class StartEventListener implements EventHandler {

	private final AtomicBoolean started = new AtomicBoolean();

	private void startInternalComponents() {
		if (started.compareAndSet(false, true)) {
			SelectionRepositoryStateCache.INSTANCE.initialize();
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (UIEvents.UILifeCycle.APP_STARTUP_COMPLETE
				.equals(event.getTopic())) {
			// If the workbench wasn't running yet when we were activated, we'll
			// initialize on the APP_STARTUP_COMPLETE event.
			startInternalComponents();
		}
	}

	@Activate
	void startUp() {
		if (PlatformUI.isWorkbenchRunning()) {
			startInternalComponents();
		}
	}

	@Deactivate
	void shutDown() {
		if (started.get()) {
			SelectionRepositoryStateCache.INSTANCE.dispose();
		}
	}
}
