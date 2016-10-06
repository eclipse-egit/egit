/*******************************************************************************
 * Copyright (C) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.handler;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionBars;

/**
 * Centralized action management for views that have several {@link Viewer}s
 * that should contribute to the global actions. A {@link GlobalActionHandler}
 * automatically registers global action handlers for the actions provided by
 * the {@link IGlobalActionProvider}s and tracks focus changes among these
 * providers to maintain the enablement state of these action handlers.
 */
public class GlobalActionHandler {

	private final Set<IGlobalActionProvider> contributors;

	private IGlobalActionProvider currentFocus;

	private Map<String, DelegatingAction> globalActions = new HashMap<>();

	private final FocusListener focusTracker = new FocusAdapter() {

		@Override
		public void focusLost(FocusEvent e) {
			for (IGlobalActionProvider contributor : contributors) {
				if (contributor.getViewer().getControl() == e.widget
						&& currentFocus == contributor) {
					currentFocus = null;
					handleFocusChange();
					return;
				}
			}
		}

		@Override
		public void focusGained(FocusEvent e) {
			for (IGlobalActionProvider contributor : contributors) {
				if (contributor.getViewer().getControl() == e.widget
						&& currentFocus != contributor) {
					currentFocus = contributor;
					handleFocusChange();
					return;
				}
			}
		}
	};

	/**
	 * Creates a new {@link GlobalActionHandler} that creates global actions for
	 * the {@link IAction}s provided by the {@link IGlobalActionProvider}s and
	 * registers them with the given {@link IActionBars}. The registered actions
	 * delegate to the appropriate provided {@link IAction} depending on which
	 * {@link IGlobalActionProvider}'s viewer has the focus.
	 *
	 * @param bars
	 *            to register actions with
	 * @param actionProviders
	 *            to get actions to delegate to and viewers to focus-track from
	 */
	public GlobalActionHandler(IActionBars bars,
			Collection<IGlobalActionProvider> actionProviders) {
		this.contributors = new HashSet<>(actionProviders);
		Set<String> ids = new HashSet<>();
		for (IGlobalActionProvider contributor : actionProviders) {
			Viewer v = contributor.getViewer();
			if (v.getControl().isFocusControl()) {
				currentFocus = contributor;
			}
			v.getControl().addFocusListener(focusTracker);
			for (IAction action : contributor.getActions()) {
				ids.add(action.getId());
			}
		}
		for (String id : ids) {
			globalActions.put(id, new DelegatingAction(id));
		}
		handleFocusChange();
		for (IAction action : globalActions.values()) {
			bars.setGlobalActionHandler(action.getId(), action);
		}
		bars.updateActionBars();
	}

	/**
	 * Dispose the {@link GlobalActionHandler}.
	 */
	public void dispose() {
		contributors.clear();
		currentFocus = null;
		globalActions.clear();
	}

	private void handleFocusChange() {
		if (currentFocus == null) {
			for (DelegatingAction action : globalActions.values()) {
				action.setDelegate(null);
			}
		} else {
			Set<String> ids = new HashSet<>(globalActions.keySet());
			for (IAction action : currentFocus.getActions()) {
				DelegatingAction global = globalActions.get(action.getId());
				if (global != null) {
					global.setDelegate(action);
				}
				ids.remove(action.getId());
			}
			for (String disappeared : ids) {
				DelegatingAction global = globalActions.get(disappeared);
				if (global != null) {
					global.setDelegate(null);
				}
			}
		}
	}

	private static class DelegatingAction extends Action {

		private IAction delegate;

		private IPropertyChangeListener listener = (event) -> {
			if (IAction.ENABLED.equals(event.getProperty())) {
				internalSetEnabled(isEnabled());
			} else if (IAction.CHECKED.equals(event.getProperty())) {
				internalSetChecked(isChecked());
			}
		};

		public DelegatingAction(String id) {
			super();
			setId(id);
		}

		private void internalSetEnabled(boolean enabled) {
			super.setEnabled(enabled);
		}

		@Override
		public void setEnabled(boolean enabled) {
			if (delegate != null) {
				delegate.setEnabled(enabled);
			} else {
				internalSetEnabled(false);
			}
		}

		@Override
		public boolean isEnabled() {
			return delegate != null && delegate.isEnabled();
		}

		@Override
		public int getStyle() {
			if (delegate != null) {
				return delegate.getStyle();
			} else {
				return IAction.AS_PUSH_BUTTON;
			}
		}

		private void internalSetChecked(boolean checked) {
			super.setChecked(checked);
		}

		@Override
		public void setChecked(boolean checked) {
			int style = getStyle();
			if (style == IAction.AS_CHECK_BOX
					|| style == IAction.AS_RADIO_BUTTON) {
				delegate.setChecked(checked);
			} else {
				internalSetChecked(false);
			}
		}

		@Override
		public boolean isChecked() {
			int style = getStyle();
			if (style == IAction.AS_CHECK_BOX
					|| style == IAction.AS_RADIO_BUTTON) {
				return delegate.isChecked();
			}
			return false;
		}

		@Override
		public void run() {
			if (delegate != null) {
				delegate.run();
			}
		}

		@Override
		public void runWithEvent(Event event) {
			if (delegate != null) {
				delegate.runWithEvent(event);
			}
		}

		public void setDelegate(IAction action) {
			if (delegate != null) {
				delegate.removePropertyChangeListener(listener);
			}
			delegate = action;
			internalSetEnabled(isEnabled());
			int style = getStyle();
			if (style == IAction.AS_CHECK_BOX
					|| style == IAction.AS_RADIO_BUTTON) {
				internalSetChecked(isChecked());
			} else {
				internalSetChecked(false);
			}
			if (delegate != null) {
				delegate.addPropertyChangeListener(listener);
			}
		}
	}
}
