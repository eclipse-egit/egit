/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.indexdiff;

import org.eclipse.jgit.lib.Repository;

/**
 * This interface is used to notify clients about changes in index diffs. See
 * also: {@link IndexDiffCache}
 */
public interface IndexDiffChangedListener {

	/**
	 * @param repository
	 * @param indexDiffData
	 */
	void indexDiffChanged(Repository repository, IndexDiffData indexDiffData);
}
