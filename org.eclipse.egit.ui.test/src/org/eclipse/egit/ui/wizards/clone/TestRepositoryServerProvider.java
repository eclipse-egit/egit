/*******************************************************************************
 * Copyright (c) 2012 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.clone;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.provisional.wizards.IRepositoryServerProvider;
import org.eclipse.egit.ui.internal.provisional.wizards.RepositoryServerInfo;

public class TestRepositoryServerProvider implements IRepositoryServerProvider {

	@Override
	public Collection<RepositoryServerInfo> getRepositoryServerInfos() {
		List<RepositoryServerInfo> info = new ArrayList<RepositoryServerInfo>();
		try {
			info.add(new RepositoryServerInfo("EGit Gerrit", new URI(
					"http://egit.eclipse.org/r")));
			info.add(new RepositoryServerInfo("Local Gerrit", new URI(
					"http://localhost:8080")));
		} catch (URISyntaxException e) {
			Activator.error(e.getLocalizedMessage(), e);
		}

		return info;
	}

}
