/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
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
package org.eclipse.egit.ui.internal.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * Read-only view of remote configuration
 */
public class RepositoryRemotePropertySource implements IPropertySource {

	private final StoredConfig myConfig;

	private final String myName;

	/**
	 * @param config
	 * @param remoteName
	 * @param page
	 *
	 */
	public RepositoryRemotePropertySource(StoredConfig config,
			String remoteName, PropertySheetPage page) {
		myConfig = config;
		myName = remoteName;
	}

	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {

		try {
			myConfig.load();
		} catch (IOException e) {
			Activator.handleError(
					UIText.RepositoryRemotePropertySource_ErrorHeader, e, true);
		} catch (ConfigInvalidException e) {
			Activator.handleError(
					UIText.RepositoryRemotePropertySource_ErrorHeader, e, true);
		}
		List<IPropertyDescriptor> resultList = new ArrayList<>();
		PropertyDescriptor desc = new PropertyDescriptor(RepositoriesView.URL,
				UIText.RepositoryRemotePropertySource_RemoteFetchURL_label);
		resultList.add(desc);
		desc = new PropertyDescriptor(RepositoriesView.FETCH,
				UIText.RepositoryRemotePropertySource_FetchLabel);
		resultList.add(desc);
		desc = new PropertyDescriptor(RepositoriesView.PUSHURL,
				UIText.RepositoryRemotePropertySource_RemotePushUrl_label);
		resultList.add(desc);
		desc = new PropertyDescriptor(RepositoriesView.PUSH,
				UIText.RepositoryRemotePropertySource_PushLabel);
		resultList.add(desc);
		return resultList.toArray(new IPropertyDescriptor[0]);
	}

	@Override
	public Object getPropertyValue(Object id) {
		String[] list = myConfig.getStringList(RepositoriesView.REMOTE, myName,
				(String) id);
		if (list != null && list.length > 1) {
			// let's show this as "[some/uri][another/uri]"
			StringBuilder sb = new StringBuilder();
			for (String s : list) {
				sb.append('[');
				sb.append(s);
				sb.append(']');
			}
			return sb.toString();
		}
		return myConfig.getString(RepositoriesView.REMOTE, myName, (String) id);
	}

	@Override
	public boolean isPropertySet(Object id) {
		// no default values
		return false;
	}

	@Override
	public void resetPropertyValue(Object id) {
		// nothing to do
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		// read-only
	}

}
