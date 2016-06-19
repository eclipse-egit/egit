/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.jgit.revwalk.RevObject;

/**
 * Listener interface to listen for changes in a {@link FindResults} object.
 */
public interface IFindListener {

	/**
	 * Called when an item has been added.
	 *
	 * @param index
	 *            table index of the new item
	 * @param rev
	 *            of the new item
	 */
	void itemAdded(int index, RevObject rev);

	/**
	 * Called when an item has been removed.
	 */
	void cleared();
}
