/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, Daniel Megert <daniel_megert@ch.ibm.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Commit;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.GitLabelProvider;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.synchronize.GitChangeSetModelProvider;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
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

		if (element instanceof GitChangeSetModelProvider)
			return new StyledString(UIText.GitChangeSetModelProviderLabel);

		return getDelegateLabelProvider().getStyledText(element);
	}

	private String createChangeSetLabel(GitModelCommit commitModel) {
		String format = store.getString(UIPreferences.SYNC_VIEW_CHANGESET_LABEL_FORMAT);

		Commit commit = commitModel.getCachedCommitObj();
		Map<String, String> bindings = new HashMap<String, String>();
		bindings.put(BINDING_CHANGESET_DATE, DATE_FORMAT.format(commit.getCommitDate()));
		bindings.put(BINDING_CHANGESET_AUTHOR, commit.getAuthorName());
		bindings.put(BINDING_CHANGESET_COMMITTER, commit.getCommitterName());
		bindings.put(BINDING_CHANGESET_SHORT_MESSAGE, commit.getShortMessage());

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
		AbbreviatedObjectId shortId = commit.getCachedCommitObj().getId();

		return shortId.name().substring(0, 6);
	}

}
