/*******************************************************************************
 * Copyright (c) 2015 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Laurent Goubet <laurent.goubet@obeo.fr> - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.StrategyResolve;
import org.eclipse.jgit.merge.ThreeWayMerger;

/**
 * A three-way merge strategy leaving the merging to the
 * {@link org.eclipse.core.resources.mapping.ModelProvider models} if
 * applicable, and delegating to the
 * {@link org.eclipse.jgit.merge.StrategyRecursive} otherwise.
 */
public class StrategyRecursiveModel extends StrategyResolve {
	@Override
	public ThreeWayMerger newMerger(Repository db) {
		return new RecursiveModelMerger(db, false);
	}

	@Override
	public ThreeWayMerger newMerger(Repository db, boolean inCore) {
		return new RecursiveModelMerger(db, inCore);
	}

	@Override
	public String getName() {
		return "model recursive"; //$NON-NLS-1$
	}
}
