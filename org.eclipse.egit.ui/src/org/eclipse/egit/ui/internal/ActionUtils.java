/*******************************************************************************
 * Copyright (C) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.util.function.BooleanSupplier;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

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
}
