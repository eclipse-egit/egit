/*******************************************************************************
 * Copyright (C) 2026 Lars Vogel <Lars.Vogel@vogella.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.expressions.Expression;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the handler (de-)registration done by
 * {@code ActionUtils.setGlobalActions}. Two controls of the same part must
 * never have their handlers registered at the same time; the handler service
 * reports conflicting handlers if they are.
 */
public class ActionUtilsTest {

	private Display display;

	private Shell shell;

	private Composite container;

	private Text first;

	private Text second;

	/** Handler activations of {@link #first}. */
	private final Set<IHandlerActivation> firstActive = ConcurrentHashMap
			.newKeySet();

	/** Handler activations of {@link #second}. */
	private final Set<IHandlerActivation> secondActive = ConcurrentHashMap
			.newKeySet();

	@Before
	public void setUp() {
		display = PlatformUI.getWorkbench().getDisplay();
		IHandlerService firstService = mockHandlerService(firstActive);
		IHandlerService secondService = mockHandlerService(secondActive);
		UIThreadRunnable.syncExec(display, () -> {
			shell = new Shell(display);
			shell.setLayout(new FillLayout());
			container = new Composite(shell, SWT.NONE);
			container.setLayout(new FillLayout());
			first = new Text(container, SWT.SINGLE);
			second = new Text(container, SWT.SINGLE);
			// Hook up before opening: the initial focus event must not arrive
			// before the listeners are in place.
			ActionUtils.setGlobalActions(first, List.of(copyAction()),
					firstService, false);
			ActionUtils.setGlobalActions(second, List.of(copyAction()),
					secondService, false);
			shell.setSize(300, 100);
			shell.open();
			shell.forceActive();
		});
	}

	@After
	public void tearDown() {
		UIThreadRunnable.syncExec(display, () -> {
			if (shell != null && !shell.isDisposed()) {
				shell.dispose();
			}
		});
		firstActive.clear();
		secondActive.clear();
	}

	@Test
	public void handlersOfDeactivatedControlDontConflictWithOtherControl() {
		focus(first);
		assertEquals("Expected a handler for the focused control", 1,
				firstActive.size());

		// The control keeps the focus: e4 parking a hidden part's controls.
		notifyWhileFocused(first, SWT.Deactivate);
		assertEquals("Deactivation must deregister the handler", 0,
				firstActive.size());

		focus(second);
		assertEquals("Stale handler of the unfocused control", 0,
				firstActive.size());
		assertEquals(1, secondActive.size());
	}

	@Test
	public void handlersRestoredOnActivateWhenStillFocused() {
		notifyWhileFocused(first, SWT.Deactivate);
		assertEquals(0, firstActive.size());

		notify(first, SWT.Activate);
		assertEquals("Handler must be restored for the focused control", 1,
				firstActive.size());
	}

	@Test
	public void handlersRestoredOnShowWhenStillFocused() {
		notifyWhileFocused(first, SWT.Deactivate);
		assertEquals(0, firstActive.size());

		notify(first, SWT.Show);
		assertEquals("Handler must be restored for the focused control", 1,
				firstActive.size());
	}

	@Test
	public void handlersRestoredOnAncestorShow() {
		notifyWhileFocused(first, SWT.Deactivate);
		assertEquals(0, firstActive.size());

		// e4 shows the part again by making an ancestor composite visible; the
		// control itself gets no event.
		notify(container, SWT.Show);
		assertEquals("Handler must be restored when an ancestor is shown", 1,
				firstActive.size());
	}

	@Test
	public void handlersNotRestoredForUnfocusedControl() {
		focus(first);
		focus(second);
		assertEquals(0, firstActive.size());

		notify(first, SWT.Activate);
		notify(first, SWT.Show);
		assertEquals("Only the focused control may have a handler", 0,
				firstActive.size());
		assertEquals(1, secondActive.size());
	}

	@Test
	public void handlersDeregisteredOnDispose() {
		focus(first);
		assertEquals(1, firstActive.size());

		UIThreadRunnable.syncExec(display, () -> first.dispose());
		assertEquals(0, firstActive.size());

		// A disposed control must not react to its ancestors any more.
		notify(container, SWT.Show);
		assertEquals(0, firstActive.size());
	}

	private static IAction copyAction() {
		IAction action = new Action("Copy") {
			// Nothing to do; never run in this test.
		};
		action.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
		return action;
	}

	private static IHandlerService mockHandlerService(
			Set<IHandlerActivation> active) {
		IHandlerService service = mock(IHandlerService.class);
		when(service.activateHandler(anyString(), any(IHandler.class),
				any(Expression.class), anyBoolean())).thenAnswer(invocation -> {
					IHandlerActivation activation = mock(
							IHandlerActivation.class);
					active.add(activation);
					return activation;
				});
		doAnswer(invocation -> {
			active.removeAll((Collection<?>) invocation.getArguments()[0]);
			return null;
		}).when(service).deactivateHandlers(any());
		return service;
	}

	private void focus(Control control) {
		boolean focused = UIThreadRunnable
				.syncExec(display, () -> Boolean.valueOf(control.setFocus()))
				.booleanValue();
		assertTrue("Could not focus the control", focused);
	}

	// Fires the event while the control is guaranteed to be the focus control:
	// the production code only reacts to a deactivation of a focused control,
	// and focus may move again as soon as the UI thread is released.
	private void notifyWhileFocused(Control control, int eventType) {
		boolean focused = UIThreadRunnable.syncExec(display, () -> {
			control.setFocus();
			boolean isFocused = control.isFocusControl();
			if (isFocused) {
				control.notifyListeners(eventType, new Event());
			}
			return Boolean.valueOf(isFocused);
		}).booleanValue();
		assertTrue("Control did not have the focus", focused);
	}

	private void notify(Control control, int eventType) {
		UIThreadRunnable.syncExec(display,
				() -> control.notifyListeners(eventType, new Event()));
	}
}
