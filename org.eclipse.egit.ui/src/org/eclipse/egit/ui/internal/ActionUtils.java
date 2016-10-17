/*******************************************************************************
 * Copyright (C) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.BooleanSupplier;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ActiveShellExpression;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;

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
		final String text = factory
				.create(PlatformUI.getWorkbench().getActiveWorkbenchWindow())
				.getText();
		Action result = new Action() {

			@Override
			public String getText() {
				return text;
			}

			@Override
			public void run() {
				action.run();
			}
		};
		result.setActionDefinitionId(factory.getCommandId());
		result.setId(factory.getId());
		return result;
	}

	/**
	 * Create an {@link IAction} taking the text, id, and action definition id
	 * from the given {@link ActionFactory}.
	 *
	 * @param factory
	 *            from which the new {@link IAction} shall be derived
	 * @param action
	 *            to execute
	 * @param enabled
	 *            to obtain the action's enablement
	 * @return the new {@link IAction}
	 */
	public static IAction createGlobalAction(ActionFactory factory,
			final Runnable action, final BooleanSupplier enabled) {
		final String text = factory
				.create(PlatformUI.getWorkbench().getActiveWorkbenchWindow())
				.getText();
		Action result = new Action() {

			@Override
			public String getText() {
				return text;
			}

			@Override
			public void run() {
				action.run();
			}

			@Override
			public boolean isEnabled() {
				return enabled.getAsBoolean();
			}
		};
		result.setActionDefinitionId(factory.getCommandId());
		result.setId(factory.getId());
		return result;
	}

	/**
	 * An {@link Expression} that evaluates to {@link EvaluationResult#TRUE
	 * TRUE} when the given control's shell is active and the control has the
	 * focus.
	 */
	private static class FocusExpression extends Expression {

		private final ActiveShellExpression shellExpression;

		private final Control control;

		private final int controlHash;

		public FocusExpression(Control control) {
			this.shellExpression = new ActiveShellExpression(
					control.getShell());
			this.control = control;
			this.controlHash = control.hashCode();
		}

		@Override
		public void collectExpressionInfo(final ExpressionInfo info) {
			this.shellExpression.collectExpressionInfo(info);
		}

		@Override
		public int hashCode() {
			return this.shellExpression.hashCode() ^ controlHash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof FocusExpression) {
				FocusExpression other = (FocusExpression) obj;
				return this.shellExpression.equals(other.shellExpression)
						&& this.control == other.control;
			}
			return false;
		}

		@Override
		public final EvaluationResult evaluate(
				final IEvaluationContext context) {
			EvaluationResult result = shellExpression.evaluate(context);
			if (EvaluationResult.TRUE.equals(result)
					&& !control.isDisposed() && control.isFocusControl()) {
				return result;
			}
			return EvaluationResult.FALSE;
		}
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
			Collection<IAction> actions, IHandlerService service) {
		Collection<IHandlerActivation> handlerActivations = new HashSet<>();
		control.addDisposeListener((event) -> {
			service.deactivateHandlers(handlerActivations);
			handlerActivations.clear();
		});
		final FocusExpression expression = new FocusExpression(control);
		control.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				service.deactivateHandlers(handlerActivations);
				handlerActivations.clear();
			}

			@Override
			public void focusGained(FocusEvent e) {
				for (final IAction action : actions) {
					handlerActivations.add(service.activateHandler(
							action.getActionDefinitionId(),
							new ActionHandler(action), expression, true));
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
			Collection<IAction> actions) {
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
}
