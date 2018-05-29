/*******************************************************************************
 * Copyright (C) 2012, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Callback interface for incrementally loading table items
 */
public interface TableLoader {
	/**
	 * @param index hint for index of table item to be loaded
	 */
	void loadItem(int index);

	/**
	 * @param c commit to be loaded
	 */
	void loadCommit(RevCommit c);
}
