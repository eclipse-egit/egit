/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.compare.structuremergeviewer.Differencer.LEFT;
import static org.eclipse.compare.structuremergeviewer.Differencer.RIGHT;


/**
 * This helper class contains methods that determinate that operation can be
 * performed on given object
 */
public final class SupportedContextActionsHelper {

	private SupportedContextActionsHelper() {
		// non-instanceable helper class
	}

	/**
	 * @param object
	 * @return {@code true} if commit operation can be performed on
	 *         {@code object}
	 */
	public static boolean canCommit(GitModelObject object) {
		return object instanceof GitModelWorkingFile
				|| object instanceof GitModelCacheFile
				|| object instanceof GitModelCacheTree
				|| object instanceof GitModelCache;
	}

	/**
	 * @param object
	 * @return {@code true} if stage/add operation can be performed on
	 *         {@code object}
	 */
	public static boolean canStage(GitModelObject object) {
		// stage action is not available on root node (object instanceof
		// GitModelWorkingTree) because in some cases we cannot determinate with
		// resource should be staged
		return object instanceof GitModelWorkingFile
				|| getRootObject(object) instanceof GitModelWorkingTree;
	}

	/**
	 * @param object
	 * @return {@code true} if 'assume unchanged' operation can be performed on
	 *         {@code object}
	 */
	public static boolean canAssumeUnchanged(GitModelObject object) {
		return canStage(object) || object instanceof GitModelCacheFile
				|| object instanceof GitModelCacheTree;
	}

	/**
	 * @param object
	 * @return {@code true} if reset operation can be performed on
	 *         {@code object}
	 */
	public static boolean canReset(GitModelObject object) {
		return isOutGoingCommit(object) || canCommit(object);
	}

	/**
	 * @param object
	 * @return {@code true} when push operation can be performed on
	 *         {@code object}
	 */
	public static boolean canPush(GitModelObject object) {
		return isOutGoingCommit(object);
	}

	/**
	 * @param object
	 * @return {@code true} when merge operation can be performed on
	 *         {@code object}
	 */
	public static boolean canMerge(GitModelObject object) {
		return getRootObject(object) instanceof GitModelCommit;
	}

	private static GitModelObject getRootObject(GitModelObject object) {
		GitModelObject root = object.getParent();

		while (root != null && root instanceof GitModelTree)
			root = root.getParent();

		return root;
	}

	private static boolean isOutGoingCommit(GitModelObject object) {
		int direction = LEFT;
		if (getRootObject(object) instanceof GitModelCommit)
			direction = ((GitModelCommit) object).getKind() & RIGHT;

		return direction == RIGHT;
	}

}
