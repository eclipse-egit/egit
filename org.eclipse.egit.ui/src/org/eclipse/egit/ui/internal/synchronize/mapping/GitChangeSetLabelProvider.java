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
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelRepository;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelWorkingTree;
import org.eclipse.jface.preference.IPreferenceStore;
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
public class GitChangeSetLabelProvider extends SynchronizationLabelProvider
		implements IStyledLabelProvider {

	/** */
	public static final String BINDING_CHANGESET_SHORT_MESSAGE = "{short_message}"; //$NON-NLS-1$

	/** */
	public static final String BINDING_CHANGESET_COMMITTER = "{committer}"; //$NON-NLS-1$

	/** */
	public static final String BINDING_CHANGESET_AUTHOR = "{author}"; //$NON-NLS-1$

	/** */
	public static final String BINDING_CHANGESET_DATE = "{date}"; //$NON-NLS-1$

	/** */
	public static final String DEFAULT_CHANGESET_FORMAT = String.format("[%s] (%s) %s", //$NON-NLS-1$
			BINDING_CHANGESET_AUTHOR,
			BINDING_CHANGESET_DATE,
			BINDING_CHANGESET_SHORT_MESSAGE);

	/** */
	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";  //$NON-NLS-1$

	private IPreferenceStore store = org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore();

	private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			store.getString(UIPreferences.DATE_FORMAT));

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
					|| element instanceof GitModelCache
					|| element instanceof GitModelWorkingTree)
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
			String formattedName = createChangeSetLabel((GitModelCommit) element);
			StyledString string = new StyledString(formattedName);
			GitModelCommit commit = (GitModelCommit) element;
			String format = " [" + getAbbreviatedId(commit) + "]"; //$NON-NLS-1$//$NON-NLS-2$
			string.append(format, StyledString.DECORATIONS_STYLER);
			return string;
		}

		return new StyledString(rawText);
	}

	private String createChangeSetLabel(GitModelCommit commit) {
		String format = store.getString(UIPreferences.SYNC_VIEW_CHANGESET_LABEL_FORMAT);

		RevCommit baseCommit = commit.getBaseCommit();
		Map<String, String> bindings = new HashMap<String, String>();
		bindings.put(BINDING_CHANGESET_DATE, DATE_FORMAT.format(baseCommit.getAuthorIdent().getWhen()));
		bindings.put(BINDING_CHANGESET_AUTHOR, baseCommit.getAuthorIdent().getName());
		bindings.put(BINDING_CHANGESET_COMMITTER, baseCommit.getCommitterIdent().getName());
		bindings.put(BINDING_CHANGESET_SHORT_MESSAGE, baseCommit.getShortMessage());

		return formatName(format, bindings);
	}

	/**
	 * @param format
	 * @param bindings
	 * @return formatted commit name
	 */
	public static String formatName(final String format, Map<String, String> bindings) {
		String result = format;
		for (Entry<String, String> e : bindings.entrySet()) {
			result = result.replace(e.getKey(), e.getValue());
		}
		return result;
	}

	private String getAbbreviatedId(GitModelCommit commit) {
		RevCommit remoteCommit = commit.getBaseCommit();
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
