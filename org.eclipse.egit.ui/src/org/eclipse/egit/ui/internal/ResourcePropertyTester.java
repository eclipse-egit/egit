/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza <dariusz@luksza.org> - add 'isSafe' implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.gerrit.GerritUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.transport.RefSpec;
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
public class ResourcePropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		boolean value = internalTest(receiver, property);
		boolean trace = GitTraceLocation.PROPERTIESTESTER.isActive();
		if (trace)
			GitTraceLocation
					.getTrace()
					.trace(GitTraceLocation.PROPERTIESTESTER.getLocation(),
							"prop "	+ property + " of " + receiver + " = " + value + ", expected = " + expectedValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		return value;
	}

	private boolean internalTest(Object receiver, String property) {
		if (!(receiver instanceof IResource))
			return false;
		IResource res = (IResource) receiver;
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
			if ("hasGerritConfiguration".equals(property)) //$NON-NLS-1$
				return hasGerritConfiguration(repository);

			RepositoryState state = repository.getRepositoryState();

			if ("canAbortRebase".equals(property)) //$NON-NLS-1$
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

			if ("canContinueRebase".equals(property)) //$NON-NLS-1$
				switch (state) {
				case REBASING_INTERACTIVE:
					return true;
				case REBASING_MERGE:
					return true;
				default:
					return false;
				}

			// isSTATE checks repository state where STATE is the CamelCase version
			// of the RepositoryState enum values.
			if (property.length() > 3 && property.startsWith("is")) { //$NON-NLS-1$
				// e.g. isCherryPickingResolved => CHERRY_PICKING_RESOLVED
				String lookFor = property.substring(2,3) + property.substring(3).replaceAll("([A-Z])","_$1").toUpperCase();  //$NON-NLS-1$//$NON-NLS-2$
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
	public static boolean hasGerritConfiguration(Repository repository) {
		Config config = repository.getConfig();
		if (GerritUtil.getCreateChangeId(config))
			return true;
		try {
			List<RemoteConfig> remoteConfigs = RemoteConfig.getAllRemoteConfigs(config);
			for (RemoteConfig remoteConfig : remoteConfigs) {
				for (RefSpec pushSpec : remoteConfig.getPushRefSpecs()) {
					String destination = pushSpec.getDestination();
					if (destination == null)
						continue;
					if (destination.startsWith(GerritUtil.REFS_FOR))
						return true;
				}
				for (RefSpec fetchSpec : remoteConfig.getFetchRefSpecs()) {
					String source = fetchSpec.getSource();
					String destination = fetchSpec.getDestination();
					if (source == null || destination == null)
						continue;
					if (source.startsWith(Constants.R_NOTES)
							&& destination.startsWith(Constants.R_NOTES))
						return true;
				}
			}
		} catch (URISyntaxException e) {
			// Assume it doesn't contain Gerrit configuration
			return false;
		}
		return false;
	}

}
