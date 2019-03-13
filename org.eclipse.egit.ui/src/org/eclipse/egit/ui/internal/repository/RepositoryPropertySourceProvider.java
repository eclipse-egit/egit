/*******************************************************************************
 * Copyright (c) 2010, 2019 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Alexander Nittka <alex@nittka.de> - Bug 545123
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jgit.events.ConfigChangedEvent;
import org.eclipse.jgit.events.ConfigChangedListener;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Control;
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

	private enum SourceType {
		UNDEFINED, REPOSITORY, REMOTE, BRANCH
	}

	private SourceType lastSourceType = SourceType.UNDEFINED;

	private ListenerHandle listenerHandle;

	private DisposeListener disposeListener;

	/**
	 * @param page
	 *            the page
	 */
	public RepositoryPropertySourceProvider(PropertySheetPage page) {
		myPage = page;
	}

	private void registerDisposal() {
		if (disposeListener != null)
			return;

		final Control control = myPage.getControl();
		if (control == null)
			return;

		disposeListener = new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				removeListener();
			}
		};
		control.addDisposeListener(disposeListener);
	}

	private void removeListener() {
		final ListenerHandle handle = listenerHandle;
		if (handle != null)
			handle.remove();
	}

	@Override
	public IPropertySource getPropertySource(Object object) {

		if (object == lastObject) {
			return lastRepositorySource;
		}

		if (!(object instanceof RepositoryTreeNode)) {
			return null;
		} else if (((RepositoryTreeNode) object).getRepository() == null) {
			return null;
		}

		registerDisposal();
		removeListener();

		RepositoryTreeNode node = (RepositoryTreeNode) object;
		listenerHandle = node.getRepository().getListenerList()
				.addConfigChangedListener(new ConfigChangedListener() {
					@Override
					public void onConfigChanged(ConfigChangedEvent event) {
						// force a refresh of the page
						lastObject = null;
						myPage.getSite().getShell().getDisplay().asyncExec(new Runnable() {

							@Override
							public void run() {
								myPage.setPropertySourceProvider(RepositoryPropertySourceProvider.this);
							}
						});
					}
				});

		if (node.getType() == RepositoryTreeNodeType.REPO) {
			lastObject = object;
			checkChangeType(SourceType.REPOSITORY);
			lastRepositorySource = new RepositoryPropertySource(
					(Repository) node.getObject(), myPage);
			return lastRepositorySource;
		} else if (node.getType() == RepositoryTreeNodeType.REMOTE) {
			lastObject = object;
			checkChangeType(SourceType.REMOTE);
			lastRepositorySource = new RepositoryRemotePropertySource(node
					.getRepository().getConfig(), (String) node.getObject(),
					myPage);
			return lastRepositorySource;
		} else if (node.getType() == RepositoryTreeNodeType.FETCH
				|| node.getType() == RepositoryTreeNodeType.PUSH)
			return getPropertySource(node.getParent());
		else if (node.getType() == RepositoryTreeNodeType.REF) {
			lastObject = object;
			Ref ref = (Ref) node.getObject();
			if (ref.getName().startsWith(Constants.R_HEADS) || ref.getName().startsWith(Constants.R_REMOTES)){
				checkChangeType(SourceType.BRANCH);
				Repository repository = Adapters.adapt(node, Repository.class);
				lastRepositorySource =  new BranchPropertySource(repository, ref.getName(), myPage);
				return lastRepositorySource;
			}
			return null;
		} else
			return null;
	}

	private void checkChangeType(SourceType type) {
		// the different pages contribute different actions, so if we
		// change to a different page type, we need to clear them
		if (lastSourceType != type) {
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
		lastSourceType = type;
	}
}
