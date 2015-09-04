/*******************************************************************************
 * Copyright (C) 2016 Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import org.eclipse.jgit.merge.MergeStrategy;

/**
 * Operation that involves a merge, and that can consequently receive a specific
 * merge strategy to override the preferred merge strategy.
 *
 * @since 4.1
 */
public interface MergingOperation extends IEGitOperation {

	/**
	 * @param strategy
	 */
	void setMergeStrategy(MergeStrategy strategy);
}
