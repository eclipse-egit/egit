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

import org.eclipse.egit.ui.internal.repository.RepositoryTreeNode.RepositoryTreeNodeType;
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
			lastRepositorySource = new RepositoryPropertySource(
					(Repository) node.getObject(), myPage);
			return lastRepositorySource;
		} else if (node.getType() == RepositoryTreeNodeType.REMOTE) {
			lastObject = object;
			lastRepositorySource = new RepositoryRemotePropertySource(node
					.getRepository().getConfig(), (String) node.getObject(), myPage);
			return lastRepositorySource;
		} else {
			return null;
		}
	}
}
