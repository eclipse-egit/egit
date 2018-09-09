/*******************************************************************************
 * Copyright (C) 2018, Luís Copetti <lhcopetti@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

/**
 * A helper class to simplify storing the association between project paths and
 * (repository + branch). This API prevents the user
 * {@link BranchProjectTracker} from knowing where these associations are being
 * saved. For this particular case at a {@link IPreferenceStore} using
 * {@link XMLMemento}.
 *
 * <p>
 * The usual location for the file is at:
 * .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.egit.ui.prefs
 * </p>
 *
 * <p>
 * An example entry would be: {@code
 * //BranchProjectTracker__/absolute/path/to/git/repository/.git_BranchName=<?xml
 * version\="1.0"
 * encoding\="UTF-8"?>\n<projects>\n<project>/</project>\n</projects> }
 * </p>
 */
class ProjectTrackerPreferenceHelper {

	private static final String PREFIX = "BranchProjectTracker_"; //$NON-NLS-1$

	private static final String KEY_PROJECTS = "projects"; //$NON-NLS-1$

	private static final String KEY_PROJECT = "project"; //$NON-NLS-1$

	/**
	 * @param repo
	 * @param branch
	 * @param projects
	 */
	static void saveToPreferences(Repository repo, String branch,
			List<String> projects)
	{
		XMLMemento preferencesMemento = createXMLMemento(projects);
		String preferenceKey = getPreferenceKey(repo, branch);
		saveToPreferenceStore(preferenceKey, preferencesMemento);
	}

	private static XMLMemento createXMLMemento(List<String> projectPaths) {

		XMLMemento memento = XMLMemento.createWriteRoot(KEY_PROJECTS);

		projectPaths.forEach(path -> {
			IMemento child = memento.createChild(KEY_PROJECT);
			child.putTextData(path);
		});

		return memento;
	}

	/**
	 * Get preference key for branch. This will be unique to the repository and
	 * branch.
	 *
	 * @param repo
	 * @param branch
	 * @return key
	 */
	static String getPreferenceKey(Repository repo,
			final String branch) {
		return PREFIX + '_' + repo.getDirectory().getAbsolutePath() + '_'
				+ branch;
	}

	private static void saveToPreferenceStore(String key, XMLMemento content)
	{
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		StringWriter writer = new StringWriter();
		try {
			content.save(writer);
			store.setValue(key, writer.toString());
		} catch (IOException e) {
			Activator.logError("Error writing branch-project associations", e); //$NON-NLS-1$
		}
	}

	/**
	 * Load the project paths associated with the currently checked out branch.
	 * These paths will be relative to the supplied repository.
	 *
	 * @param repo
	 * @param branch
	 * @return non-null but possibly empty array of projects
	 */
	static List<String> restoreFromPreferences(Repository repo,
			String branch) {

		String key = getPreferenceKey(repo, branch);
		String value = Activator.getDefault().getPreferenceStore()
				.getString(key);

		if (value.length() == 0)
			return Collections.emptyList();

		XMLMemento memento;
		try {
			memento = XMLMemento.createReadRoot(new StringReader(value));
		} catch (WorkbenchException e) {
			Activator.logError("Error reading branch-project associations", e); //$NON-NLS-1$
			return Collections.emptyList();
		}
		IMemento[] children = memento.getChildren(KEY_PROJECT);
		if (children.length == 0)
			return Collections.emptyList();

		List<String> projectPaths = Stream.of(children) //
				.map(child -> child.getTextData()) //
				.filter(x -> !StringUtils.isEmptyOrNull(x)) //
				.collect(Collectors.toList());
		return projectPaths;
	}
}
