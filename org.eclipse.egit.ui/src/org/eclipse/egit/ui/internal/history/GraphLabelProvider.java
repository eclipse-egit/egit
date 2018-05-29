/*******************************************************************************
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2011, IBM Corporation
 * Copyright (C) 2012, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.dialogs.CommitLabelProvider;

/**
 * A Label Provider for Commits
 */
class GraphLabelProvider extends CommitLabelProvider {

	public GraphLabelProvider() {
		super();
	}

	public GraphLabelProvider(boolean canShowEmailAddresses) {
		super(canShowEmailAddresses);
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element == null) {
			return ""; //$NON-NLS-1$
		}
		final SWTCommit c = (SWTCommit) element;
		try {
			c.parseBody();
		} catch (IOException e) {
			Activator.error("Error parsing body", e); //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		}
		return super.getColumnText(c, columnIndex);
	}
}
