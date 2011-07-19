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

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.GitLabelProvider;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.ui.mapping.SynchronizationLabelProvider;

/**
 * Label provider for Git ChangeSet model.
 */
public class GitChangeSetLabelProvider extends SynchronizationLabelProvider implements IStyledLabelProvider {

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

	private GitLabelProvider delegateLabelProvider;

	@Override
	protected GitLabelProvider getDelegateLabelProvider() {
		if (delegateLabelProvider == null)
			delegateLabelProvider = new GitLabelProvider();

		return delegateLabelProvider;
	}

	public StyledString getStyledText(Object element) {
		// need to compare classes as everything is 'instanceof GitModelCommit'
		if (element.getClass().equals(GitModelCommit.class)) {
			String formattedName = createChangeSetLabel((GitModelCommit) element);
			StyledString string = new StyledString(formattedName);
			GitModelCommit commit = (GitModelCommit) element;
			String format = " [" + getAbbreviatedId(commit) + "]"; //$NON-NLS-1$//$NON-NLS-2$
			string.append(format, StyledString.DECORATIONS_STYLER);
			return string;
		}

		return getDelegateLabelProvider().getStyledText(element);
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
