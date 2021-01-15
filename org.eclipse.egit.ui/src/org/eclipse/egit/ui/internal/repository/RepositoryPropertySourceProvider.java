/*******************************************************************************
 * Copyright (c) 2010, 2021 SAP AG and others.
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
import org.eclipse.egit.ui.internal.properties.TagPropertySource;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * A {@link IPropertySourceProvider} for git repository properties.
 */
public class RepositoryPropertySourceProvider implements
		IPropertySourceProvider {

	private final PropertySheetPage myPage;

	private Object lastObject;

	private IPropertySource lastRepositorySource;

	private enum SourceType {
		UNDEFINED, REPOSITORY, REMOTE, BRANCH, TAG
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

	private void refreshPage() {
		lastObject = null;
		myPage.getSite().getShell().getDisplay()
				.asyncExec(() -> myPage.setPropertySourceProvider(this));
	}

	@Override
	public IPropertySource getPropertySource(Object object) {

		if (object instanceof IPropertySource) {
			// Enable nested properties
			return (IPropertySource) object;
		}

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
				.addConfigChangedListener(event -> refreshPage());

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
		} else if (node.getType() == RepositoryTreeNodeType.TAG) {
			lastObject = object;
			checkChangeType(SourceType.TAG);
			lastRepositorySource = new TagPropertySource(node.getRepository(),
					(Ref) node.getObject(), myPage);
			return lastRepositorySource;
		}
		return null;
	}

	private void checkChangeType(SourceType type) {
		// the different pages contribute different actions, so if we
		// change to a different page type, we need to clear them
		if (lastSourceType != type) {
			IActionBars bars = myPage.getSite().getActionBars();
			IToolBarManager mgr = bars.getToolBarManager();
			boolean update = false;
			update |= mgr.remove(
					RepositoryPropertySource.CHANGEMODEACTIONID) != null;
			update |= mgr.remove(RepositoryPropertySource.SINGLEVALUEACTIONID) != null;
			update |= mgr.remove(RepositoryPropertySource.EDITACTIONID) != null;
			update |= mgr.remove(BranchPropertySource.EDITACTIONID) != null;
			if (update) {
				// Need to update the full IActionBars, not just the toolbar
				// manager, to get proper layout when items are added or
				// removed.
				bars.updateActionBars();
			}
		}
		lastSourceType = type;
	}
}
