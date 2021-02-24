/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf - factored out of Activator
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Determines whether any shell of the application is active and fires an event
 * when the value changes.
 */
@Component(property = {
		EventConstants.EVENT_TOPIC + '='
				+ UIEvents.UILifeCycle.APP_STARTUP_COMPLETE,
		EventConstants.EVENT_TOPIC + '='
				+ UIEvents.UILifeCycle.APP_SHUTDOWN_STARTED })
public class ApplicationActiveListener implements EventHandler {

	/**
	 * Event topic for the events posted by this component.
	 */
	public static final String TOPIC_APPLICATION_ACTIVE = "org/eclipse/egit/ui/APPLICATION_ACTIVE"; //$NON-NLS-1$

	private volatile WindowTracker listener;

	@Override
	public void handleEvent(Event event) {
		String topic = event.getTopic();
		if (topic == null) {
			return;
		}
		switch (topic) {
		case UIEvents.UILifeCycle.APP_STARTUP_COMPLETE:
			if (listener == null) {
				listener = new WindowTracker();
				listener.update();
				PlatformUI.getWorkbench().addWindowListener(listener);
			}
			break;
		case UIEvents.UILifeCycle.APP_SHUTDOWN_STARTED:
			shutDown();
			break;
		default:
			break;
		}
	}

	@Deactivate
	void shutDown() {
		if (listener != null) {
			PlatformUI.getWorkbench().removeWindowListener(listener);
			listener = null;
		}
	}

	private static class WindowTracker implements IWindowListener {

		private boolean isActive;

		void update() {
			if (PlatformUI.isWorkbenchRunning()) {
				Display display = PlatformUI.getWorkbench().getDisplay();
				if (display != null && !display.isDisposed()) {
					try {
						display.asyncExec(() -> {
							boolean wasActive = isActive;
							isActive = !display.isDisposed()
									&& display.getActiveShell() != null;
							if (wasActive != isActive) {
								notify(isActive);
							}
						});
					} catch (SWTException e) {
						// Silently ignore -- display was disposed already
					}
				}
			}
		}

		private void notify(boolean active) {
			if (PlatformUI.isWorkbenchRunning()) {
				IEventBroker broker = PlatformUI.getWorkbench()
						.getService(IEventBroker.class);
				if (broker != null) {
					broker.post(TOPIC_APPLICATION_ACTIVE,
							Boolean.valueOf(active));
				}
			}
		}

		@Override
		public void windowActivated(IWorkbenchWindow window) {
			update();
		}

		@Override
		public void windowDeactivated(IWorkbenchWindow window) {
			update();
		}

		@Override
		public void windowClosed(IWorkbenchWindow window) {
			update();
		}

		@Override
		public void windowOpened(IWorkbenchWindow window) {
			update();
		}
	}
}
