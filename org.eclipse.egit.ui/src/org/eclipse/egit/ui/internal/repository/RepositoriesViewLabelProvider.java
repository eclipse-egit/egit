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

import org.eclipse.egit.ui.internal.GitLabels;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IToolTipProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Label Provider for the Git Repositories View
 */
public class RepositoriesViewLabelProvider extends WorkbenchLabelProvider
		implements IToolTipProvider {

	/**
	 * Returns a repository tree node label provider that is hooked up to the
	 * decorator mechanism.
	 *
	 * @return a new label provider that also decorates.
	 */
	public static ILabelProvider getDecoratingLabelProvider() {
		return new DecoratingLabelProviderWithToolTips(
				new RepositoriesViewLabelProvider());
	}

	/**
	 * Returns a repository tree node label provider that is hooked up to the
	 * decorator mechanism.
	 *
	 * @return a new styled label provider that also decorates.
	 */
	public static IStyledLabelProvider getDecoratingStyledLabelProvider() {
		return new DecoratingLabelProviderWithToolTips(
				new RepositoriesViewLabelProvider());
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

	private static class DecoratingLabelProviderWithToolTips
			extends DecoratingLabelProvider
			implements IStyledLabelProvider, IToolTipProvider {

		private final RepositoriesViewLabelProvider labelProvider;

		DecoratingLabelProviderWithToolTips(
				RepositoriesViewLabelProvider labelProvider) {
			super(labelProvider, PlatformUI.getWorkbench().getDecoratorManager()
					.getLabelDecorator());
			this.labelProvider = labelProvider;
		}

		@Override
		public String getToolTipText(Object element) {
			return labelProvider.getToolTipText(element);
		}

		@Override
		public StyledString getStyledText(Object element) {
			String decorated = getText(element);
			StyledString label = labelProvider.getStyledText(element);
			return StyledCellLabelProvider.styleDecoratedString(decorated,
					StyledString.DECORATIONS_STYLER, label);
		}
	}
}
