/*******************************************************************************
 * Copyright (C) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BooleanSupplier;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ActiveShellExpression;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action-related utilities.
 */
public final class ActionUtils {

	private ActionUtils() {
		// Utility class shall not be instantiated.
	}

	/**
	 * Create an {@link IAction} taking the text, id, and action definition id
	 * from the given {@link ActionFactory}.
	 *
	 * @param factory
	 *            from which the new {@link IAction} shall be derived
	 * @param action
	 *            to execute
	 * @return the new {@link IAction}
	 */
	public static IAction createGlobalAction(ActionFactory factory,
			final Runnable action) {
		IWorkbenchAction template = factory
				.create(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		IAction result = new Action(template.getText()) {

			@Override
			public void run() {
				action.run();
			}
		};
		result.setActionDefinitionId(template.getActionDefinitionId());
		result.setId(template.getId());
		result.setImageDescriptor(template.getImageDescriptor());
		result.setDisabledImageDescriptor(
				template.getDisabledImageDescriptor());
		template.dispose();
		return result;
	}

	/**
	 * Create an {@link UpdateableAction} taking the text, id, and action
	 * definition id from the given {@link ActionFactory}. The using code of
	 * such an action is responsible for calling {@link IUpdate#update()
	 * update()} on the action when its enablement should be updated.
	 *
	 * @param factory
	 *            from which the new {@link IAction} shall be derived
	 * @param action
	 *            to execute
	 * @param enabled
	 *            to obtain the action's enablement
	 * @return the new {@link UpdateableAction}
	 */
	public static UpdateableAction createGlobalAction(ActionFactory factory,
			final Runnable action, final BooleanSupplier enabled) {
		IWorkbenchAction template = factory
				.create(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		UpdateableAction result = new UpdateableAction(template.getText()) {

			@Override
			public void run() {
				action.run();
			}

			@Override
			public void update() {
				setEnabled(enabled.getAsBoolean());
			}
		};
		result.setActionDefinitionId(template.getActionDefinitionId());
		result.setId(template.getId());
		result.setImageDescriptor(template.getImageDescriptor());
		result.setDisabledImageDescriptor(
				template.getDisabledImageDescriptor());
		result.update();
		template.dispose();
		return result;
	}

	/**
	 * Hooks up the {@link Control} such that the given {@link IAction}s are
	 * registered with the given {@link IHandlerService} while the control has
	 * the focus. Ensures that actions are properly de-registered when the
	 * control is disposed.
	 *
	 * @param control
	 *            to hook up
	 * @param actions
	 *            to be registered while the control has the focus
	 * @param service
	 *            to register the actions with
	 */
	public static void setGlobalActions(Control control,
			Collection<? extends IAction> actions, IHandlerService service) {
		Collection<IHandlerActivation> handlerActivations = new ArrayList<>();
		control.addDisposeListener(event -> {
			if (!handlerActivations.isEmpty()) {
				service.deactivateHandlers(handlerActivations);
				handlerActivations.clear();
			}
		});
		final ActiveShellExpression expression = new ActiveShellExpression(
				control.getShell());
		control.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				if (!handlerActivations.isEmpty()) {
					service.deactivateHandlers(handlerActivations);
					handlerActivations.clear();
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
				if (!handlerActivations.isEmpty()) {
					// Looks like sometimes we get two focusGained events.
					return;
				}
				for (final IAction action : actions) {
					handlerActivations.add(service.activateHandler(
							action.getActionDefinitionId(),
							new ActionHandler(action), expression, false));
					if (action instanceof IUpdate) {
						((IUpdate) action).update();
					}
				}
			}
		});
	}

	/**
	 * Hooks up the {@link Control} such that the given {@link IAction}s are
	 * registered with the given {@link IHandlerService} while the control has
	 * the focus. Ensures that actions are properly de-registered when the
	 * control is disposed.
	 *
	 * @param control
	 *            to hook up
	 * @param service
	 *            to register the actions with
	 * @param actions
	 *            to be registered while the control has the focus
	 */
	public static void setGlobalActions(Control control,
			IHandlerService service, IAction... actions) {
		setGlobalActions(control, Arrays.asList(actions), service);
	}

	/**
	 * Hooks up the {@link Control} such that the given {@link IAction}s are
	 * registered with the workbench-global {@link IHandlerService} while the
	 * control has the focus. Ensures that actions are properly de-registered
	 * when the control is disposed.
	 *
	 * @param control
	 *            to hook up
	 * @param actions
	 *            to be registered while the control has the focus
	 */
	public static void setGlobalActions(Control control,
			Collection<? extends IAction> actions) {
		setGlobalActions(control, actions, CommonUtils
				.getService(PlatformUI.getWorkbench(), IHandlerService.class));
	}

	/**
	 * Hooks up the {@link Control} such that the given {@link IAction}s are
	 * registered with the workbench-global {@link IHandlerService} while the
	 * control has the focus. Ensures that actions are properly de-registered
	 * when the control is disposed.
	 *
	 * @param control
	 *            to hook up
	 * @param actions
	 *            to be registered while the control has the focus
	 */
	public static void setGlobalActions(Control control, IAction... actions) {
		setGlobalActions(control, Arrays.asList(actions));
	}

	/**
	 * An {@link Action} that is updateable via {@link IUpdate}.
	 */
	public static abstract class UpdateableAction extends Action
			implements IUpdate {

		/**
		 * Creates a new {@link UpdateableAction} with the given text.
		 *
		 * @param text
		 */
		public UpdateableAction(String text) {
			super(text);
		}
	}
}
