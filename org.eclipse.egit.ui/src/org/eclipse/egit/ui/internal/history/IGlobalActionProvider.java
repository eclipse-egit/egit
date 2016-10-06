/*******************************************************************************
 * Copyright (C) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.util.Collection;

import org.eclipse.jface.action.IAction;

/**
 * An abstraction for something that can provide a collection of
 * {@link IAction}s.
 *
 * @see GlobalActionHandler
 */
public interface IGlobalActionProvider extends IViewerProvider {

	/**
	 * Obtains a (possibly empty) collection of {@link IAction}s.
	 * 
	 * @return the actions
	 */
	Collection<IAction> getActions();
}
