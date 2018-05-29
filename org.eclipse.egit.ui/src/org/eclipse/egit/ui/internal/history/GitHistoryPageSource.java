/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.team.ui.history.HistoryPageSource;
import org.eclipse.ui.part.Page;

/**
 * A helper class for constructing the {@link GitHistoryPage}.
 */
public class GitHistoryPageSource extends HistoryPageSource {

	/**
	 * The instance to use if needed.
	 */
	public static final GitHistoryPageSource INSTANCE = new GitHistoryPageSource();

	private GitHistoryPageSource() {
		super();
	}

	@Override
	public boolean canShowHistoryFor(final Object object) {
		return GitHistoryPage.canShowHistoryFor(object);
	}

	@Override
	public Page createPage(final Object object) {
		// don't set the input, the framework does this for us
		return new GitHistoryPage();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof GitHistoryPageSource;
	}

	@Override
	public int hashCode() {
		return 42;
	}
}
