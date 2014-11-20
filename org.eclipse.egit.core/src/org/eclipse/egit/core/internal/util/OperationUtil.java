/*******************************************************************************
 * Copyright (c) 2014, EclipseSource.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Philip Langer (EclipseSource) - initial implementation 
 *******************************************************************************/
package org.eclipse.egit.core.internal.util;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.internal.merge.StrategyRecursiveModel;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.StrategyRecursive;
import org.eclipse.jgit.merge.ThreeWayMergeStrategy;

/**
 * Utility class for operations.
 */
public class OperationUtil {

	private static final StrategyRecursiveModel LOGICAL_MODEL_RECURSIVE_STRATEGY = new StrategyRecursiveModel();

	private static final ThreeWayMergeStrategy PLAIN_RECURSIVE_STRATEGY = MergeStrategy.RECURSIVE;

	/**
	 * Specifies whether operations should use the logical model according to
	 * the user's preferences.
	 *
	 * @return <code>true</code> if the logical model should be used,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isUseLogicalModel() {
		String corePluginId = org.eclipse.egit.core.Activator.getPluginId();
		IEclipsePreferences d = DefaultScope.INSTANCE.getNode(corePluginId);
		IEclipsePreferences p = InstanceScope.INSTANCE.getNode(corePluginId);
		return p.getBoolean(GitCorePreferences.core_useLogicalModel,
				d.getBoolean(GitCorePreferences.core_useLogicalModel, true));
	}

	/**
	 * Returns the {@link MergeStrategy} to be used in jgit commands that are
	 * created within egit operations.
	 * <p>
	 * The returned strategy currently depends on the user preference
	 * {@link GitCorePreferences#core_useLogicalModel} on whether to use the
	 * logical model or not. The default merge strategy is either
	 * {@link StrategyRecursiveModel} if the logical model support is enabled,
	 * otherwise it is {@link StrategyRecursive}.
	 * </p>
	 *
	 * @return The merge strategy.
	 */
	public static MergeStrategy getMergeStrategy() {
		if (isUseLogicalModel()) {
			return LOGICAL_MODEL_RECURSIVE_STRATEGY;
		} else {
			return PLAIN_RECURSIVE_STRATEGY;
		}

	}

}
