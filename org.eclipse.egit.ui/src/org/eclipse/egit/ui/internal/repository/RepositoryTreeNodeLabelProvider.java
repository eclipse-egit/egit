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
 *    Chris Aniszczyk <caniszczyk@gmail.com> - added styled label support
 *    Thomas Wolf <thomas.wolf@paranor.ch> - bug 536814: completely refactored
 *    Alexander Nittka <alex@nittka.de> - bug 545123: repository groups
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.util.Arrays;
import java.util.function.Consumer;

import org.eclipse.egit.ui.internal.GitLabels;
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.custom.StyleRange;
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
	 * Creates a new {@link RepositoryTreeNodeLabelProvider} that neither
	 * decorates nor shows paths.
	 */
	public RepositoryTreeNodeLabelProvider() {
		this(new WorkbenchLabelProvider(), null, null, false);
	}

	/**
	 * Creates a new {@link RepositoryTreeNodeLabelProvider} that decorates and
	 * optionally shows paths.
	 *
	 * @param showPaths
	 *            whether to show paths
	 */
	public RepositoryTreeNodeLabelProvider(boolean showPaths) {
		this(new WorkbenchLabelProvider(), PlatformUI.getWorkbench()
				.getDecoratorManager().getLabelDecorator(), null, showPaths);
	}

	private RepositoryTreeNodeLabelProvider(
			WorkbenchLabelProvider labelProvider, ILabelDecorator decorator,
			IDecorationContext decorationContext, boolean showPaths) {
		super(labelProvider, decorator, decorationContext);
		this.labelProvider = labelProvider;
		this.showPaths = showPaths;
	}

	@Override
	public StyledString getStyledText(Object element) {
		return super.getStyledText(element);
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
	public void update(ViewerCell cell) {
		if (showPaths) {
			update(cell, super::update);
		} else {
			super.update(cell);
		}
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
		if (element instanceof RepositoryGroupNode) {
			RepositoryGroup group = ((RepositoryGroupNode) element).getObject();
			result.append(" (").append(group.getRepositoryDirectories().size()) //$NON-NLS-1$
					.append(')');
		} else if (element instanceof RepositoryTreeNode) {
			if (((RepositoryTreeNode) element)
					.getType() != RepositoryTreeNodeType.REPO) {
				Repository repo = ((RepositoryTreeNode) element)
						.getRepository();
				result.append(" [").append(GitLabels.getPlainShortLabel(repo)) //$NON-NLS-1$
						.append(']');
			}
		}
		return result.toString();
	}

	@Override
	public void init(ICommonContentExtensionSite config) {
		// empty
	}

	static void update(ViewerCell cell, Consumer<ViewerCell> updater) {
		Object element = cell.getElement();
		if (element instanceof RepositoryNode
				|| element instanceof WorkingDirNode) {
			String textBefore = cell.getText();
			StyleRange[] rangesBefore = cell.getStyleRanges();
			updater.accept(cell);
			String textAfter = cell.getText();
			StyleRange[] rangesAfter = cell.getStyleRanges();
			if (textBefore.equals(textAfter)
					&& Arrays.equals(rangesBefore, rangesAfter)) {
				// Decorating delegate decided to wait.
				return;
			}
			Repository repository = ((RepositoryTreeNode<?>) element)
					.getRepository();
			if (repository == null) {
				return;
			}
			String newText = " - "; //$NON-NLS-1$
			if (element instanceof RepositoryNode) {
				newText += repository.getDirectory().getAbsolutePath();
			} else if (element instanceof WorkingDirNode) {
				newText += repository.getWorkTree().getAbsolutePath();
			}
			StyleRange style = new StyleRange();
			style.start = textAfter.length();
			style.length = newText.length();
			StyledString.QUALIFIER_STYLER.applyStyles(style);
			StyleRange[] newRanges = new StyleRange[rangesAfter.length + 1];
			System.arraycopy(rangesAfter, 0, newRanges, 0, rangesAfter.length);
			newRanges[newRanges.length - 1] = style;
			cell.setText(textAfter + newText);
			cell.setStyleRanges(newRanges);
		} else {
			updater.accept(cell);
		}
	}
}
