/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * PropertySource provider for Resource properties
 *
 */
public class RepositoryPropertySourceProvider implements
		IPropertySourceProvider {

	private final PropertySheetPage myPage;

	private Object lastObject;

	private IPropertySource lastRepositorySource;

	// 0: Repository, 1: Remote, 2: Branch
	private int lastSourceType = -1;

	/**
	 * @param page
	 *            the page
	 */
	public RepositoryPropertySourceProvider(PropertySheetPage page) {
		myPage = page;
	}

	public IPropertySource getPropertySource(Object object) {

		if (object == lastObject)
			return lastRepositorySource;

		if (!(object instanceof RepositoryTreeNode))
			return null;

		RepositoryTreeNode node = (RepositoryTreeNode) object;

		if (node.getType() == RepositoryTreeNodeType.REPO) {
			lastObject = object;
			checkChangeType(0);
			lastRepositorySource = new RepositoryPropertySource(
					(Repository) node.getObject(), myPage);
			return lastRepositorySource;
		} else if (node.getType() == RepositoryTreeNodeType.REMOTE) {
			lastObject = object;
			checkChangeType(1);
			lastRepositorySource = new RepositoryRemotePropertySource(node
					.getRepository().getConfig(), (String) node.getObject(),
					myPage);
			return lastRepositorySource;
		} else if (node.getType() == RepositoryTreeNodeType.FETCH
				|| node.getType() == RepositoryTreeNodeType.PUSH) {
			return getPropertySource(node.getParent());
		} else if (node.getType() == RepositoryTreeNodeType.REF) {
			lastObject = object;
			Ref ref = (Ref) node.getObject();
			if (ref.getName().startsWith(Constants.R_HEADS) || ref.getName().startsWith(Constants.R_REMOTES)){
				checkChangeType(2);
				Repository repository = (Repository) node.getAdapter(Repository.class);
				lastRepositorySource =  new BranchPropertySource(repository, ref.getName(), myPage);
				return lastRepositorySource;
			}
			return null;
		} else {
			return null;
		}
	}

	private void checkChangeType(int i) {
		// the different pages contribute different actions, so if we
		// change to a different page type, we need to clear them
		if (lastSourceType != i) {
			IToolBarManager mgr = myPage.getSite().getActionBars()
					.getToolBarManager();
			boolean update = false;
			update = update
					| mgr.remove(RepositoryPropertySource.CHANGEMODEACTIONID) != null;
			update = update
					| mgr.remove(RepositoryPropertySource.SINGLEVALUEACTIONID) != null;
			update = update
					| mgr.remove(RepositoryPropertySource.EDITACTIONID) != null;
			update = update
					| mgr.remove(BranchPropertySource.EDITACTIONID) != null;
			if (update)
				mgr.update(false);
		}
		lastSourceType = i;
	}
}
