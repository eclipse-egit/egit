/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelRepository;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.ui.mapping.SynchronizationLabelProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Label provider for Git ChangeSet model.
 */
public class GitChangeSetLabelProvider extends SynchronizationLabelProvider implements IStyledLabelProvider {

	private static final ILabelProvider workbenchLabelProvider = WorkbenchLabelProvider
			.getDecoratingWorkbenchLabelProvider();

	private LabelProvider delegateLabelProvider;

	@Override
	protected ILabelProvider getDelegateLabelProvider() {
		if (delegateLabelProvider == null)
			delegateLabelProvider = new DelegateLabelProvider();

		return delegateLabelProvider;
	}

	private static class DelegateLabelProvider extends LabelProvider {

		private final ResourceManager fImageCache = new LocalResourceManager(
				JFaceResources.getResources());

		public String getText(Object element) {
			if (element instanceof GitModelObject)
				return ((GitModelObject) element).getName();

			return null;
		}

		public Image getImage(Object element) {
			if (element instanceof GitModelBlob) {
				Object adapter = ((GitModelBlob) element)
						.getAdapter(IResource.class);
				return workbenchLabelProvider.getImage(adapter);
			}

			if (element instanceof GitModelTree) {
				Object adapter = ((GitModelTree) element)
						.getAdapter(IResource.class);
				return workbenchLabelProvider.getImage(adapter);
			}

			if (element instanceof GitModelCommit
					|| element instanceof GitModelCache)
				return fImageCache.createImage(UIIcons.CHANGESET);

			if (element instanceof GitModelRepository)
				return fImageCache.createImage(UIIcons.REPOSITORY);

			return super.getImage(element);
		}

		@Override
		public void dispose() {
			fImageCache.dispose();
			super.dispose();
		}

	}

	public StyledString getStyledText(Object element) {
		String rawText = getText(element);
		// need to compare classes as everything is 'instanceof GitModelCommit'
		if (element.getClass().equals(GitModelCommit.class)) {
			StyledString string = new StyledString(rawText);
			GitModelCommit commit = (GitModelCommit) element;
			String format = " [" + getAbbreviatedId(commit) + "]"; //$NON-NLS-1$//$NON-NLS-2$
			string.append(format, StyledString.DECORATIONS_STYLER);
			return string;
		}

		return new StyledString(rawText);
	}

	private String getAbbreviatedId(GitModelCommit commit) {
		RevCommit remoteCommit = commit.getRemoteCommit();
		ObjectReader reader = commit.getRepository().newObjectReader();
		ObjectId commitId = remoteCommit.getId();
		AbbreviatedObjectId shortId;
		try {
			shortId = reader.abbreviate(commitId, 6);
		} catch (IOException e) {
			shortId = AbbreviatedObjectId.fromObjectId(ObjectId.zeroId());
			Activator.logError(e.getMessage(), e);
		} finally {
			reader.release();
		}
		return shortId.name();
	}

}
