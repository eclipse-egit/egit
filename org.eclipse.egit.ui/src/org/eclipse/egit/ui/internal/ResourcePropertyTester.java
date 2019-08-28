/*******************************************************************************
 * Copyright (c) 2011, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza <dariusz@luksza.org> - add 'isSafe' implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 493352
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.gerrit.GerritUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.expressions.AbstractPropertyTester;
import org.eclipse.egit.ui.internal.selection.RepositoryStateCache;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Resource-based property tester.
 * <p>
 * Supported properties:
 * <ul>
 * <li>isShared <code>true</code> if the resource is mapped to EGit. EGit may
 * still affect a resource if it belongs to the workspace of some shared
 * project.</li>
 * <li>isContainer <code>true</code> if the resource is a project or a folder</li>
 * <li>is<em>repository state</em>
 * <ul>
 * <li>isSafe - see {@link RepositoryState#SAFE}</li>
 * <li>isReverting - see {@link RepositoryState#REVERTING}</li>
 * <li>isRevertingResolved - see {@link RepositoryState#REVERTING_RESOLVED}</li>
 * <li>isCherryPicking - see {@link RepositoryState#CHERRY_PICKING}</li>
 * <li>isCherryPickingResolved - see
 * {@link RepositoryState#CHERRY_PICKING_RESOLVED}</li>
 * <li>isMerging - see {@link RepositoryState#MERGING}</li>
 * <li>isMergingResolved - see {@link RepositoryState#MERGING_RESOLVED}</li>
 * <li>isRebasing - see {@link RepositoryState#REBASING}</li>
 * <li>isRebasingRebasing - see {@link RepositoryState#REBASING_REBASING}</li>
 * <li>isRebasingMerge - see {@link RepositoryState#REBASING_MERGE}</li>
 * <li>isRebasingInteractive - see {@link RepositoryState#REBASING_INTERACTIVE}</li>
 * <li>isApply - see {@link RepositoryState#APPLY}</li>
 * <li>isBisecting - see {@link RepositoryState#BISECTING}</li>
 * </ul>
 * <li>Capabilities/properties of the current state:<ul>
 * <li>canCheckout  - see {@link RepositoryState#canCheckout()}</li>
 * <li>canAmend  - see {@link RepositoryState#canAmend()}</li>
 * <li>canCommit  - see {@link RepositoryState#canCommit()}</li>
 * <li>canResetHead  - see {@link RepositoryState#canResetHead()}</li>
 * </ul>
 * </ul>
 */
public class ResourcePropertyTester extends AbstractPropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if (!(receiver instanceof IResource)) {
			return false;
		}
		boolean value = internalTest((IResource) receiver, property);
		boolean trace = GitTraceLocation.PROPERTIESTESTER.isActive();
		if (trace)
			GitTraceLocation
					.getTrace()
					.trace(GitTraceLocation.PROPERTIESTESTER.getLocation(),
							"prop "	+ property + " of " + receiver + " = " + value + ", expected = " + expectedValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		return computeResult(expectedValue, value);
	}

	private boolean internalTest(@NonNull IResource res, String property) {
		if ("isContainer".equals(property)) { //$NON-NLS-1$
			int type = res.getType();
			return type == IResource.FOLDER || type == IResource.PROJECT;
		}

		RepositoryMapping mapping = RepositoryMapping.getMapping(res);
		if (mapping != null) {
			Repository repository = mapping.getRepository();
			return testRepositoryState(repository, property);
		}
		return false;
	}

	/**
	 * @param repository
	 * @param property
	 * @return true if the repository is in an appropriate state. See
	 *         {@link ResourcePropertyTester}
	 */
	public static boolean testRepositoryState(Repository repository, String property) {
		if ("isShared".equals(property)) //$NON-NLS-1$
			return repository != null;
		if (repository != null) {
			if ("hasGerritConfiguration".equals(property)) { //$NON-NLS-1$
				return hasGerritConfiguration(repository);
			}
			if ("canFetchFromGerrit".equals(property)) { //$NON-NLS-1$
				return canFetchFromGerrit(repository);
			}
			if ("canPushToGerrit".equals(property)) { //$NON-NLS-1$
				return canPushToGerrit(repository);
			}
			RepositoryState state = RepositoryStateCache.INSTANCE
					.getRepositoryState(repository);

			if ("canAbortRebase".equals(property)) { //$NON-NLS-1$
				return canAbortRebase(state);
			}
			if ("canContinueRebase".equals(property)) { //$NON-NLS-1$
				return canContinueRebase(state);
			}
			// isSTATE checks repository state where STATE is the CamelCase version
			// of the RepositoryState enum values.
			if (property.length() > 3 && property.startsWith("is")) { //$NON-NLS-1$
				// e.g. isCherryPickingResolved => CHERRY_PICKING_RESOLVED
				String lookFor = property.substring(2, 3)
						+ property.substring(3).replaceAll("([A-Z])", "_$1") //$NON-NLS-1$//$NON-NLS-2$
								.toUpperCase(Locale.ROOT);
				if (state.toString().equals(lookFor))
					return true;
			}
			// invokes test methods of RepositoryState, canCommit etc
			try {
				Method method = RepositoryState.class.getMethod(property);
				if (method.getReturnType() == boolean.class) {
					Boolean ret = (Boolean) method.invoke(state);
					return ret.booleanValue();
				}
			} catch (Exception e) {
				// ignore
			}
		}
		return false;
	}

	/**
	 * @param repository
	 * @return {@code true} if repository has been configured for Gerrit
	 */
	public static boolean hasGerritConfiguration(
			@NonNull Repository repository) {
		Config config = RepositoryStateCache.INSTANCE.getConfig(repository);
		if (GerritUtil.getCreateChangeId(config)) {
			return true;
		}
		try {
			List<RemoteConfig> remoteConfigs = RemoteConfig.getAllRemoteConfigs(config);
			for (RemoteConfig remoteConfig : remoteConfigs) {
				if (GerritUtil.isGerritPush(remoteConfig)
						|| GerritUtil.isGerritFetch(remoteConfig)) {
					return true;
				}
			}
		} catch (URISyntaxException e) {
			// Assume it doesn't contain Gerrit configuration
			return false;
		}
		return false;
	}

	/**
	 * @param repository
	 * @return {@code true} if repository has been configured to fetch from
	 *         Gerrit
	 */
	public static boolean canFetchFromGerrit(@NonNull Repository repository) {
		Config config = RepositoryStateCache.INSTANCE.getConfig(repository);
		try {
			List<RemoteConfig> remoteConfigs = RemoteConfig
					.getAllRemoteConfigs(config);
			for (RemoteConfig remoteConfig : remoteConfigs) {
				if (GerritUtil.isGerritFetch(remoteConfig)) {
					return true;
				}
			}
		} catch (URISyntaxException e) {
			return false;
		}
		return false;
	}

	/**
	 * @param repository
	 * @return {@code true} if repository has been configured for pushing to
	 *         Gerrit
	 */
	public static boolean canPushToGerrit(@NonNull Repository repository) {
		Config config = RepositoryStateCache.INSTANCE.getConfig(repository);
		try {
			List<RemoteConfig> remoteConfigs = RemoteConfig
					.getAllRemoteConfigs(config);
			for (RemoteConfig remoteConfig : remoteConfigs) {
				if (GerritUtil.isGerritPush(remoteConfig)) {
					return true;
				}
			}
		} catch (URISyntaxException e) {
			return false;
		}
		return false;
	}

	/**
	 * @param state
	 * @return {@code true} if the repository state permits a rebase to be
	 *         aborted
	 */
	public static boolean canAbortRebase(@NonNull RepositoryState state) {
		switch (state) {
		case REBASING_INTERACTIVE:
			return true;
		case REBASING_REBASING:
			return true;
		case REBASING_MERGE:
			return true;
		default:
			return false;
		}
	}

	/**
	 * @param state
	 * @return {@code true} if the repository state permits a rebase to be
	 *         continued
	 */
	public static boolean canContinueRebase(@NonNull RepositoryState state) {
		switch (state) {
		case REBASING_INTERACTIVE:
			return true;
		case REBASING_MERGE:
			return true;
		default:
			return false;
		}
	}
}
