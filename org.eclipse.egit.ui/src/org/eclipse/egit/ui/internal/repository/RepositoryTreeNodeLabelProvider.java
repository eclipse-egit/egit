/*******************************************************************************
 * Copyright (c) 2010, 2018 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Chris Aniszczyk <caniszczyk@gmail.com> - added styled label support
 *    Thomas Wolf <thomas.wolf@paranor.ch> - bug 536814: completely refactored
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.util.WeakHashMap;

import org.eclipse.egit.ui.internal.GitLabels;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

/**
 * A decorating label provider for repository tree nodes.
 */
public class RepositoryTreeNodeLabelProvider
		extends DecoratingStyledCellLabelProvider
		implements ICommonLabelProvider, IStyledLabelProvider {

	private final WorkbenchLabelProvider labelProvider;

	private final boolean showPaths;

	/**
	 * Keeps the last label. If the label we originally get is undecorated, we
	 * return this last decorated label instead to prevent flickering. When the
	 * asynchronous lightweight decorator then has computed the decoration, the
	 * label will be updated. Note that this works only because our
	 * RepositoryTreeNodeDecorator always decorates! (If there's no decoration,
	 * it appends a single blank to ensure the decorated label is different from
	 * the undecorated one.)
	 * <p>
	 * For images, there is no such work-around, and thus we need to do the
	 * image decorations in the label provider (in the
	 * RepositoryTreeNodeWorkbenchAdapter in our case) in the UI thread.
	 */
	private final WeakHashMap<Object, StyledString> previousDecoratedLabels = new WeakHashMap<>();

	/**
	 * Creates a new {@link RepositoryTreeNodeLabelProvider} that shows the
	 * paths for repositories and working tree nodes.
	 */
	public RepositoryTreeNodeLabelProvider() {
		this(true);
	}

	/**
	 * Creates a new {@link RepositoryTreeNodeLabelProvider}.
	 *
	 * @param showPaths
	 *            whether to show the file system paths for repositories and
	 *            working tree nodes
	 */
	public RepositoryTreeNodeLabelProvider(boolean showPaths) {
		this(new WorkbenchLabelProvider(), showPaths);
	}

	private RepositoryTreeNodeLabelProvider(
			WorkbenchLabelProvider labelProvider, boolean showPaths) {
		super(labelProvider, PlatformUI.getWorkbench()
				.getDecoratorManager().getLabelDecorator(), null);
		this.labelProvider = labelProvider;
		this.showPaths = showPaths;
	}

	@Override
	public void dispose() {
		super.dispose();
		previousDecoratedLabels.clear();
	}

	@Override
	public StyledString getStyledText(Object element) {
		StyledString decoratedLabel = super.getStyledText(element);
		String decoratedValue = decoratedLabel.getString();
		String simpleValue = labelProvider.getText(element);
		if (decoratedValue.equals(simpleValue)) {
			// Decoration not available yet... but may be shortly. Try to
			// prevent flickering by returning the previous decorated label, if
			// any.
			StyledString previousLabel = previousDecoratedLabels.get(element);
			if (previousLabel != null) {
				return previousLabel;
			}
		} else if (decoratedValue.trim().equals(simpleValue)) {
			// No decoration...
			decoratedLabel = labelProvider.getStyledText(element);
		}
		if (showPaths) {
			if (element instanceof RepositoryNode) {
				Repository repository = ((RepositoryNode) element)
						.getRepository();
				if (repository != null) {
					decoratedLabel.append(" - ", StyledString.QUALIFIER_STYLER) //$NON-NLS-1$
							.append(repository.getDirectory().getAbsolutePath(),
									StyledString.QUALIFIER_STYLER);
				}
			} else if (element instanceof WorkingDirNode) {
				Repository repository = ((WorkingDirNode) element)
						.getRepository();
				if (repository != null) {
					decoratedLabel.append(" - ", StyledString.QUALIFIER_STYLER) //$NON-NLS-1$
							.append(repository.getWorkTree().getAbsolutePath(),
									StyledString.QUALIFIER_STYLER);
				}
			}
		}
		previousDecoratedLabels.put(element, decoratedLabel);
		return decoratedLabel;
	}

	@Override
	public String getText(Object element) {
		return labelProvider.getText(element);
	}

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof AdditionalRefNode) {
			AdditionalRefNode additionalRefNode = (AdditionalRefNode) element;
			Ref ref = additionalRefNode.getObject();
			return GitLabels.getRefDescription(ref);
		}
		return null;
	}

	@Override
	public void restoreState(IMemento memento) {
		// empty
	}

	@Override
	public void saveState(IMemento memento) {
		// empty
	}

	@Override
	public String getDescription(Object element) {
		StringBuilder result = new StringBuilder(getText(element));
		// for branches use the complete name, even with hierarchical layout
		if (element instanceof RefNode) {
			Ref ref = ((RefNode) element).getObject();
			String branchName = Repository.shortenRefName(ref.getName());
			result = new StringBuilder(branchName);
		}
		if (element instanceof RepositoryTreeNode) {
			if (((RepositoryTreeNode) element)
					.getType() != RepositoryTreeNodeType.REPO) {
				Repository repo = ((RepositoryTreeNode) element)
						.getRepository();
				result.append(" - ").append(GitLabels.getPlainShortLabel(repo)); //$NON-NLS-1$
			}
		}
		return result.toString();
	}

	@Override
	public void init(ICommonContentExtensionSite config) {
		// empty
	}
}
