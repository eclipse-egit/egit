/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.compare.ICompareInputLabelProvider;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.storage.CommitFileRevision;
import org.eclipse.egit.core.internal.storage.IndexFileRevision;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.revision.FileRevisionTypedElement;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.history.IFileRevision;

/**
 * A label provider for {@link DiffNode}s as created by EGit.
 */
public class GitCompareLabelProvider extends BaseLabelProvider
		implements ICompareInputLabelProvider {

	private final PreferenceBasedDateFormatter formatter = PreferenceBasedDateFormatter
			.create();

	private boolean isEditable(Object object) {
		if (object instanceof IEditableContent) {
			return ((IEditableContent) object).isEditable();
		}
		return false;
	}

	private String getLabel(FileRevisionTypedElement element) {
		if (element == null) {
			return null;
		}
		IFileRevision fileObject = element.getFileRevision();
		if (fileObject instanceof IndexFileRevision) {
			if (isEditable(element)) {
				return MessageFormat.format(
						UIText.GitCompareFileRevisionEditorInput_IndexEditableLabel,
						element.getName());
			} else {
				return MessageFormat.format(
						UIText.GitCompareFileRevisionEditorInput_IndexLabel,
						element.getName());
			}
		} else {
			String id = CompareUtils
					.truncatedRevision(element.getContentIdentifier());
			if (fileObject instanceof CommitFileRevision commitRevision) {
				id += ' ' + formatter
						.formatDate(new Date(commitRevision.getTimestamp()));
			}
			return MessageFormat.format(
					UIText.GitCompareFileRevisionEditorInput_RevisionLabel,
					element.getName(), id, element.getAuthor());
		}
	}

	private IResource getResource(ITypedElement element) {
		if (element instanceof IResourceProvider) {
			IResourceProvider resourceProvider = (IResourceProvider) element;
			return resourceProvider.getResource();
		}
		return null;
	}

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		return null;
	}

	@Override
	public String getAncestorLabel(Object input) {
		if (input instanceof DiffNode) {
			ITypedElement item = ((DiffNode) input).getAncestor();
			if (item instanceof FileRevisionTypedElement) {
				return getLabel((FileRevisionTypedElement) item);
			}
		}
		return null;
	}

	@Override
	public Image getAncestorImage(Object input) {
		return null;
	}

	@Override
	public String getLeftLabel(Object input) {
		if (input instanceof DiffNode) {
			ITypedElement item = ((DiffNode) input).getLeft();
			if (item instanceof FileRevisionTypedElement) {
				return getLabel((FileRevisionTypedElement) item);
			} else if (getResource(item) != null) {
				return MessageFormat.format(
						UIText.GitCompareFileRevisionEditorInput_LocalLabel,
						item.getName());
			}
		}
		return null;
	}

	@Override
	public Image getLeftImage(Object input) {
		return null;
	}

	@Override
	public String getRightLabel(Object input) {
		if (input instanceof DiffNode) {
			ITypedElement item = ((DiffNode) input).getRight();
			if (item instanceof FileRevisionTypedElement) {
				return getLabel((FileRevisionTypedElement) item);
			} else if (getResource(item) != null) {
				return MessageFormat.format(
						UIText.GitCompareFileRevisionEditorInput_LocalLabel,
						item.getName());
			}
		}
		return null;
	}

	@Override
	public Image getRightImage(Object input) {
		return null;
	}

	/**
	 * Fires a {@link LabelProviderChangedEvent} to tell users that the labels
	 * for the given object changed.
	 *
	 * @param object
	 *            for which labels need updating
	 */
	public void fireNodeLabelChanged(Object object) {
		LabelProviderChangedEvent event = new LabelProviderChangedEvent(this,
				object);
		fireLabelProviderChanged(event);
	}
}
