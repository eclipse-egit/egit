/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.egit.ui.internal.ResourcePropertyTester;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Property Tester used for enabling/disabling of context menus in the Git
 * Repositories View.
 */
public class RepositoriesViewPropertyTester extends PropertyTester {

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

		if (!(receiver instanceof RepositoryTreeNode))
			return false;
		RepositoryTreeNode node = (RepositoryTreeNode) receiver;

		if (property.equals("isBare")) //$NON-NLS-1$
			return node.getRepository().isBare();

		if (property.equals("containsHead")) //$NON-NLS-1$
			return containsHead(node);

		if (ResourcePropertyTester.testRepositoryState(node.getRepository(), property))
			return true;

		if (property.equals("isRefCheckedOut")) { //$NON-NLS-1$
			if (!(node.getObject() instanceof Ref))
				return false;
			Ref ref = (Ref) node.getObject();
			try {
				if (ref.getName().startsWith(Constants.R_REFS)) {
					return ref.getName().equals(
							node.getRepository().getFullBranch());
				} else if (ref.getName().equals(Constants.HEAD))
					return true;
				else {
					String leafname = ref.getLeaf().getName();
					if (leafname.startsWith(Constants.R_REFS)
							&& leafname.equals(node.getRepository()
									.getFullBranch()))
						return true;
					else
						ref.getLeaf().getObjectId().equals(
								node.getRepository().resolve(Constants.HEAD));
				}
			} catch (IOException e) {
				return false;
			}
		}
		if (property.equals("isLocalBranch")) { //$NON-NLS-1$
			if (!(node.getObject() instanceof Ref))
				return false;
			Ref ref = (Ref) node.getObject();
			return ref.getName().startsWith(Constants.R_HEADS);
		}
		if (property.equals("fetchExists")) { //$NON-NLS-1$
			if (node instanceof RemoteNode) {
				String configName = ((RemoteNode) node).getObject();

				RemoteConfig rconfig;
				try {
					rconfig = new RemoteConfig(
							node.getRepository().getConfig(), configName);
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
							node.getRepository().getConfig(), configName);
				} catch (URISyntaxException e2) {
					return false;
				}
				// we need to have at least a push ref spec and any URI
				return !rconfig.getPushRefSpecs().isEmpty()
						&& (!rconfig.getPushURIs().isEmpty() || !rconfig
								.getURIs().isEmpty());
			}
		}
		if (property.equals("canMerge")) { //$NON-NLS-1$
			Repository rep = node.getRepository();
			if (rep.getRepositoryState() != RepositoryState.SAFE)
				return false;
			try {
				String branch = rep.getFullBranch();
				if (branch == null)
					return false; // fail gracefully...
				return branch.startsWith(Constants.R_HEADS);
			} catch (IOException e) {
				return false;
			}
		}

		if (property.equals("canAbortRebase")) //$NON-NLS-1$
			switch (node.getRepository().getRepositoryState()) {
			case REBASING_INTERACTIVE:
				return true;
			case REBASING_REBASING:
				return true;
			default:
				return false;
			}

		if (property.equals("canContinueRebase")) //$NON-NLS-1$
			switch (node.getRepository().getRepositoryState()) {
			case REBASING_INTERACTIVE:
				return true;
			default:
				return false;
			}

		if ("isSubmodule".equals(property)) { //$NON-NLS-1$
			RepositoryTreeNode<?> parent = node.getParent();
			return parent != null
					&& parent.getType() == RepositoryTreeNodeType.SUBMODULES;
		}
		return false;
	}

	private boolean containsHead(RepositoryTreeNode node) {
		try {
			return node.getRepository().resolve(Constants.HEAD) != null;
		} catch (IOException e) {
			return false;
		}
	}

}
