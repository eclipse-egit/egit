/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.egit.core.op.TrackOperation;

/**
 * An action to add resources to the Git repository.
 *
 * @see TrackOperation
 */
public class Track extends RepositoryAction {
	/**
	 *
	 */
	public Track() {
		super(ActionCommands.TRACK_ACTION);
	}
}
