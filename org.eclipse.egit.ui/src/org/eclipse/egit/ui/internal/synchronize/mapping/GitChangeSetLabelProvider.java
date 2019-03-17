/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Commit;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.GitLabelProvider;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.synchronize.GitChangeSetModelProvider;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.team.ui.mapping.SynchronizationLabelProvider;

/**
 * Label provider for Git ChangeSet model.
 */
public class GitChangeSetLabelProvider extends SynchronizationLabelProvider implements IStyledLabelProvider {

	/** */
	public static final String BINDING_CHANGESET_SHORT_MESSAGE = '{'
			+ GitLightweightDecorator.DecorationHelper.BINDING_SHORT_MESSAGE
			+ '}';

	/** */
	public static final String BINDING_CHANGESET_COMMITTER = "{committer}"; //$NON-NLS-1$

	/** */
	public static final String BINDING_CHANGESET_AUTHOR = "{author}"; //$NON-NLS-1$

	/** */
	public static final String BINDING_CHANGESET_DATE = "{date}"; //$NON-NLS-1$

	private GitLabelProvider delegateLabelProvider;

	private final ListenerList<ILabelProviderListener> listeners = new ListenerList<>(
			ListenerList.IDENTITY);

	private final IPreferenceStore store = Activator.getDefault()
			.getPreferenceStore();

	private final IPropertyChangeListener uiPrefsListener;

	private PreferenceBasedDateFormatter dateFormatter;

	/**
	 * Creates a new {@link GitChangeSetLabelProvider}.
	 */
	public GitChangeSetLabelProvider() {
		dateFormatter = PreferenceBasedDateFormatter.create();
		uiPrefsListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				if (UIPreferences.DATE_FORMAT.equals(property)
						|| UIPreferences.DATE_FORMAT_CHOICE.equals(property)) {
					dateFormatter = PreferenceBasedDateFormatter.create();
					fireLabelProviderChanged(new LabelProviderChangedEvent(
							GitChangeSetLabelProvider.this));
				}

			}
		};
		store.addPropertyChangeListener(uiPrefsListener);
	}

	@Override
	protected GitLabelProvider getDelegateLabelProvider() {
		if (delegateLabelProvider == null)
			delegateLabelProvider = new GitLabelProvider();

		return delegateLabelProvider;
	}

	@Override
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
		Map<String, String> bindings = new HashMap<>();
		bindings.put(BINDING_CHANGESET_DATE,
				dateFormatter.formatDate(commit.getCommitDate()));
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

	// The super class adds the listeners to the delegate label provider, where
	// we don't have access. Therefore we keep our own listener list, and do the
	// notification ourselves when the date format preferences change.

	@Override
	public void addListener(ILabelProviderListener listener) {
		super.addListener(listener);
		listeners.add(listener);
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		listeners.remove(listener);
		super.removeListener(listener);
	}

	private void fireLabelProviderChanged(
			final LabelProviderChangedEvent event) {
		for (Object o : listeners.getListeners()) {
			final ILabelProviderListener l = (ILabelProviderListener) o;
			SafeRunnable.run(new SafeRunnable() {
				@Override
				public void run() {
					l.labelProviderChanged(event);
				}
			});
		}
	}

	@Override
	public void dispose() {
		store.removePropertyChangeListener(uiPrefsListener);
		listeners.clear();
		super.dispose();
	}
}
