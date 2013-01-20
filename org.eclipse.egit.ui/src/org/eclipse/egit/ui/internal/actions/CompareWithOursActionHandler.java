/*******************************************************************************
 * Copyright (C) 2013 Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import org.eclipse.jgit.dircache.DirCacheEntry;

/**
 * Action handler for "Compare with ours".
 */
public class CompareWithOursActionHandler extends
		CompareWithIndexStageActionHandler {

	@Override
	protected int getStage() {
		return DirCacheEntry.STAGE_2;
	}

}
