/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.egit.ui.internal.ResourcePropertyTester;
import org.eclipse.egit.ui.internal.expressions.AbstractPropertyTester;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.util.LfsFactory;

/**
 * Property Tester used for enabling/disabling of context menus in the Git
 * Repositories View.
 */
public class RepositoriesViewPropertyTester extends AbstractPropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if (!(receiver instanceof RepositoryTreeNode)) {
			return false;
		}
		RepositoryTreeNode node = (RepositoryTreeNode) receiver;

		Collection<? extends Repository> repositories = null;
		if (node instanceof RepositoryGroupNode) {
			repositories = ((RepositoryGroupNode) node).getRepositories();
		} else {
			Repository repository = node.getRepository();
			if (repository != null) {
				repositories = Collections.singleton(repository);
			}
		}
		if (repositories == null || repositories.isEmpty()) {
			return false;
		}
		boolean value = true;
		for (Repository repository : repositories) {
			value &= internalTest(node, repository, property);
		}
		boolean trace = GitTraceLocation.PROPERTIESTESTER.isActive();
		if (trace) {
			GitTraceLocation
					.getTrace()
					.trace(GitTraceLocation.PROPERTIESTESTER.getLocation(),
							"prop "	+ property + " of " + receiver + " = " + value + ", expected = " + expectedValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		return computeResult(expectedValue, value);
	}

	private boolean internalTest(RepositoryTreeNode node, Repository repository,
			String property) {
		if (property.equals("isBare")) { //$NON-NLS-1$
			return repository.isBare();
		}

		if (property.equals("containsHead")) {//$NON-NLS-1$
			return containsHead(repository);
		}

		if (ResourcePropertyTester.testRepositoryState(repository, property)) {
			return true;
		}

		if (property.equals("isRefCheckedOut")) { //$NON-NLS-1$
			if (node instanceof BranchHierarchyNode) {
				try {
					for (Ref ref : ((BranchHierarchyNode) node)
							.getChildRefsRecursive()) {
						if (isRefCheckedOut(repository, ref)) {
							return true;
						}
					}
				} catch (IOException e) {
					return false;
				}
			}
			if (!(node.getObject() instanceof Ref)) {
				return false;
			}
			Ref ref = (Ref) node.getObject();
			return isRefCheckedOut(repository, ref);
		}
		if (property.equals("isLocalBranch")) { //$NON-NLS-1$
			if (!(node.getObject() instanceof Ref)) {
				return false;
			}
			Ref ref = (Ref) node.getObject();
			return ref.getName().startsWith(Constants.R_HEADS);
		}
		if (property.equals("fetchExists")) { //$NON-NLS-1$
			if (node instanceof RemoteNode) {
				String configName = ((RemoteNode) node).getObject();

				RemoteConfig rconfig;
				try {
					rconfig = new RemoteConfig(
							SelectionRepositoryStateCache.INSTANCE.getConfig(repository),
							configName);
				} catch (URISyntaxException e2) {
					return false;
				}
				// we need to have a fetch ref spec and a fetch URI
				return !rconfig.getFetchRefSpecs().isEmpty()
						&& !rconfig.getURIs().isEmpty();
			}
		}
		if (property.equals("pushExists")) { //$NON-NLS-1$
			if (node instanceof RemoteNode) {
				String configName = ((RemoteNode) node).getObject();

				RemoteConfig rconfig;
				try {
					rconfig = new RemoteConfig(
							SelectionRepositoryStateCache.INSTANCE.getConfig(repository),
							configName);
				} catch (URISyntaxException e2) {
					return false;
				}
				// we need to have at least a push ref spec and any URI
				return !rconfig.getPushRefSpecs().isEmpty()
						&& (!rconfig.getPushURIs().isEmpty() || !rconfig
								.getURIs().isEmpty());
			}
		}
		if (property.equals("canStash")) { //$NON-NLS-1$
			RepositoryState state = SelectionRepositoryStateCache.INSTANCE
					.getRepositoryState(repository);
			return state.canCommit();
		}
		if (property.equals("canMerge")) { //$NON-NLS-1$
			RepositoryState state = SelectionRepositoryStateCache.INSTANCE
					.getRepositoryState(repository);
			if (state != RepositoryState.SAFE) {
				return false;
			}
			String branch = SelectionRepositoryStateCache.INSTANCE
					.getFullBranchName(repository);
			if (branch == null) {
				return false; // fail gracefully...
			}
			return branch.startsWith(Constants.R_HEADS);
		}

		if ("isSubmodule".equals(property)) { //$NON-NLS-1$
			RepositoryTreeNode<?> parent = node.getParent();
			return parent != null
					&& parent.getType() == RepositoryTreeNodeType.SUBMODULES;
		}

		if ("canEnableLfs".equals(property)) { //$NON-NLS-1$
			if (LfsFactory.getInstance().isAvailable()) {
				return !LfsFactory.getInstance().isEnabled(repository);
			}
		}

		return false;
	}

	private boolean isRefCheckedOut(Repository repository, Ref ref) {
		if (ref.getName().startsWith(Constants.R_REFS)) {
			return ref.getName().equals(
					SelectionRepositoryStateCache.INSTANCE.getFullBranchName(repository));
		} else if (ref.getName().equals(Constants.HEAD)) {
			return true;
		} else {
			String leafname = ref.getLeaf().getName();
			if (leafname.startsWith(Constants.R_REFS) && leafname.equals(
					SelectionRepositoryStateCache.INSTANCE.getFullBranchName(repository))) {
				return true;
			} else {
				ObjectId objectId = ref.getLeaf().getObjectId();
				return objectId != null && objectId
						.equals(SelectionRepositoryStateCache.INSTANCE.getHead(repository));
			}
		}
	}

	private boolean containsHead(Repository repository) {
		return SelectionRepositoryStateCache.INSTANCE.getHead(repository) != null;
	}

}
