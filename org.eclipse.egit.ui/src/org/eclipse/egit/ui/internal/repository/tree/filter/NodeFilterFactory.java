/*******************************************************************************
 * Copyright (C) 2019, Alexander Nittka <alex@nittka.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.filter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;

/**
 * Factory creating a filter based on the node type parameter.
 *
 * usage example from plugin.xml <code><pre>
 * &lt;commonFilter ...
 *   class="org.eclipse.egit.ui.internal.repository.tree.filter.NodeFilterFactory:TAGS"&gt;</pre></code>
 */
public class NodeFilterFactory
		implements IExecutableExtensionFactory, IExecutableExtension {

	private RepositoryTreeNodeType typeToHide;

	@Override
	public Object create() throws CoreException {
		return new NodeByTypeFilter(typeToHide);
	}

	@Override
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		typeToHide = RepositoryTreeNodeType.valueOf((String) data);
	}

}