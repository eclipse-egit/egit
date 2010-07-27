/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

/**
 * Properties for repository configuration
 *
 */
public class RepositoryPropertySource implements IPropertySource,
		IPropertySource2 {

	private static final String USER_ID_PREFIX = "user"; //$NON-NLS-1$

	private static final String REPO_ID_PREFIX = "repo"; //$NON-NLS-1$

	private static final String EFFECTIVE_ID_PREFIX = "effe"; //$NON-NLS-1$

	private static final String PREFERENCE_KEYS = "RepositoryPropertySource.ConfiguredKeys"; //$NON-NLS-1$

	private Action configureKeyAction;

	private Action modeToggleAction;

	private Action restoreKeyAction;

	private final PropertySheetPage myPage;

	private final FileBasedConfig userHomeConfig;

	private final StoredConfig repositoryConfig;

	private final StoredConfig effectiveConfig;

	/**
	 * @param rep
	 *            the repository
	 * @param page
	 */
	public RepositoryPropertySource(Repository rep, PropertySheetPage page) {

		myPage = page;

		makeActions();
		addActions();

		effectiveConfig = rep.getConfig();
		userHomeConfig = SystemReader.getInstance().openUserConfig(FS.DETECTED);

		if (effectiveConfig instanceof FileBasedConfig) {
			File configFile = ((FileBasedConfig) effectiveConfig).getFile();
			repositoryConfig = new FileBasedConfig(configFile, rep.getFS());
		} else {
			repositoryConfig = effectiveConfig;
		}

		try {
			effectiveConfig.load();
			userHomeConfig.load();
			repositoryConfig.load();
		} catch (IOException e) {
			showExceptionMessage(e);
		} catch (ConfigInvalidException e) {
			showExceptionMessage(e);
		}
	}

	private void makeActions() {

		configureKeyAction = new Action(
				UIText.RepositoryPropertySource_ConfigureKeysAction) {

			@Override
			public String getId() {
				return "ConfigKeyActionId"; //$NON-NLS-1$
			}

			@Override
			public void run() {
				ConfigureKeysDialog dlg = new ConfigureKeysDialog(myPage
						.getSite().getShell(), getConfiguredKeys());
				if (dlg.open() == Window.OK)
					try {
						setConfiguredKeys(dlg.getActiveKeys());
						myPage.refresh();
					} catch (IOException e) {
						showExceptionMessage(e);
					}

			}

		};

		modeToggleAction = new Action(
				UIText.RepositoryPropertySource_EffectiveConfigurationAction) {
			// TODO icon
			@Override
			public String getId() {
				return "ViewModeToggle"; //$NON-NLS-1$
			}

			@Override
			public void run() {
				myPage.refresh();
			}

			@Override
			public int getStyle() {
				return IAction.AS_CHECK_BOX;
			}

		};

		restoreKeyAction = new Action(
				UIText.RepositoryPropertySource_RestoreStandardAction) {

			@Override
			public String getId() {
				return "RestoreKeys"; //$NON-NLS-1$
			}

			@Override
			public void run() {
				try {
					setConfiguredKeys(new ArrayList<String>());
					myPage.refresh();
				} catch (IOException e) {
					showExceptionMessage(e);
				}
			}

		};
	}

	private void addActions() {

		boolean refreshToolbar = false;
		IActionBars bars = myPage.getSite().getActionBars();

		if (bars.getToolBarManager().find(modeToggleAction.getId()) == null) {
			bars.getToolBarManager().add(modeToggleAction);
			refreshToolbar = true;
		}

		if (bars.getMenuManager().find(configureKeyAction.getId()) == null)
			bars.getMenuManager().add(configureKeyAction);

		if (bars.getMenuManager().find(restoreKeyAction.getId()) == null)
			bars.getMenuManager().add(restoreKeyAction);

		if (refreshToolbar)
			bars.getToolBarManager().update(false);
	}

	private List<String> getConfiguredKeys() {

		List<String> result = new ArrayList<String>();
		ScopedPreferenceStore store = new ScopedPreferenceStore(
				new InstanceScope(), Activator.getPluginId());
		String keys = store.getString(PREFERENCE_KEYS);
		if (keys.length() > 0) {
			StringTokenizer tok = new StringTokenizer(keys, " "); //$NON-NLS-1$
			while (tok.hasMoreTokens()) {
				result.add(tok.nextToken());
			}
		} else {
			result.addAll(ConfigureKeysDialog.standardKeys);
		}
		return result;
	}

	private void setConfiguredKeys(List<String> keys) throws IOException {

		StringBuilder sb = new StringBuilder();
		for (String key : keys) {
			sb.append(key);
			sb.append(" "); //$NON-NLS-1$
		}
		ScopedPreferenceStore store = new ScopedPreferenceStore(
				new InstanceScope(), Activator.getPluginId());
		store.putValue(PREFERENCE_KEYS, sb.toString());
		store.save();

	}

	private Object getValueFromConfig(Config config, String keyString) {

		StringTokenizer tok = new StringTokenizer(keyString, "."); //$NON-NLS-1$

		String section;
		String subsection;
		String name;

		String[] valueList = null;
		if (tok.countTokens() == 2) {
			section = tok.nextToken();
			subsection = null;
			name = tok.nextToken();
		} else if (tok.countTokens() == 3) {
			section = tok.nextToken();
			subsection = tok.nextToken();
			name = tok.nextToken();
		} else {
			// TODO exception?
			return null;
		}

		valueList = config.getStringList(section, subsection, name);

		if (valueList == null || valueList.length == 0)
			return null;

		if (valueList.length == 1) {
			return valueList[0];
		}

		StringBuilder sb = new StringBuilder();
		for (String value: valueList){
			sb.append('[');
			sb.append(value);
			sb.append(']');
		}

		return sb.toString();

	}

	public Object getEditableValue() {
		return null;
	}

	public IPropertyDescriptor[] getPropertyDescriptors() {

		// initFromConfig();

		try {
			userHomeConfig.load();
			repositoryConfig.load();
			effectiveConfig.load();
		} catch (IOException e) {
			showExceptionMessage(e);
		} catch (ConfigInvalidException e) {
			showExceptionMessage(e);
		}

		List<IPropertyDescriptor> resultList = new ArrayList<IPropertyDescriptor>();

		List<String> configuredKeys = getConfiguredKeys();

		boolean effectiveMode = false;

		ActionContributionItem item = (ActionContributionItem) myPage.getSite()
				.getActionBars().getToolBarManager().find(
						modeToggleAction.getId());
		if (item != null) {
			effectiveMode = ((Action) item.getAction()).isChecked();
		}

		if (effectiveMode) {
			for (String key : configuredKeys) {

				for (String sub : getSubSections(effectiveConfig, key)) {
					PropertyDescriptor desc = new PropertyDescriptor(
							EFFECTIVE_ID_PREFIX + sub, sub);

					desc
							.setCategory(UIText.RepositoryPropertySource_EffectiveConfigurationCategory);
					resultList.add(desc);
				}

			}
		} else {

			String categoryString = UIText.RepositoryPropertySource_GlobalConfigurationCategory
					+ userHomeConfig.getFile().getAbsolutePath();
			for (String key : configuredKeys) {

				// no remote configuration globally....
				if (key.startsWith(RepositoriesView.REMOTE + ".")) //$NON-NLS-1$
					continue;

				for (String sub : getSubSections(effectiveConfig, key)) {
					TextPropertyDescriptor desc = new TextPropertyDescriptor(
							USER_ID_PREFIX + sub, sub);
					desc.setCategory(categoryString);
					resultList.add(desc);
				}
			}

			categoryString = UIText.RepositoryPropertySource_RepositoryConfigurationCategory;
			if (repositoryConfig instanceof FileBasedConfig) {
				categoryString += ((FileBasedConfig) repositoryConfig)
						.getFile().getAbsolutePath();
			}

			boolean editable = true;

			for (String key : configuredKeys) {

				// remote stuff is not configurable
				editable = !key.startsWith("remote"); //$NON-NLS-1$

				for (String sub : getSubSections(effectiveConfig, key)) {
					PropertyDescriptor desc;
					if (editable)
						desc = new TextPropertyDescriptor(REPO_ID_PREFIX + sub,
								sub);
					else
						desc = new PropertyDescriptor(REPO_ID_PREFIX + sub, sub);
					desc.setCategory(categoryString);
					resultList.add(desc);
				}
			}
		}

		return resultList.toArray(new IPropertyDescriptor[0]);
	}

	private List<String> getSubSections(Config configuration, String key) {

		List<String> result = new ArrayList<String>();

		if (key.indexOf(".?.") < 0) { //$NON-NLS-1$
			result.add(key);
			return result;
		}

		StringTokenizer stok = new StringTokenizer(key, "."); //$NON-NLS-1$
		if (stok.countTokens() == 3) {
			String section = stok.nextToken();
			String subsection = stok.nextToken();
			String name = stok.nextToken();
			if (subsection.equals("?")) { //$NON-NLS-1$
				Set<String> subs = configuration.getSubsections(section);
				for (String sub : subs)
					result.add(section + "." + sub + "." + name); //$NON-NLS-1$ //$NON-NLS-2$
				return result;
			} else {
				result.add(key);
			}
		}
		return result;
	}

	public Object getPropertyValue(Object id) {
		String actId = ((String) id);
		Object value = null;
		if (actId.startsWith(USER_ID_PREFIX)) {
			value = getValueFromConfig(userHomeConfig, actId.substring(4));
		} else if (actId.startsWith(REPO_ID_PREFIX)) {
			value = getValueFromConfig(repositoryConfig, actId.substring(4));
		} else if (actId.startsWith(EFFECTIVE_ID_PREFIX)) {
			value = getValueFromConfig(effectiveConfig, actId.substring(4));
		}
		if (value == null)
			// the text editor needs this to work
			return ""; //$NON-NLS-1$

		return value;
	}

	public boolean isPropertySet(Object id) {
		String actId = ((String) id);
		Object value = null;
		if (actId.startsWith(USER_ID_PREFIX)) {
			value = getValueFromConfig(userHomeConfig, actId.substring(4));
		} else if (actId.startsWith(REPO_ID_PREFIX)) {
			value = getValueFromConfig(repositoryConfig, actId.substring(4));
		} else if (actId.startsWith(EFFECTIVE_ID_PREFIX)) {
			value = getValueFromConfig(effectiveConfig, actId.substring(4));
		}
		return value != null;
	}

	public void resetPropertyValue(Object id) {
		setPropertyValue(id, null);
	}

	public void setPropertyValue(Object id, Object value) {

		String actId = (String) id;
		try {
			if (actId.startsWith(USER_ID_PREFIX)) {
				setConfigValue(userHomeConfig, actId.substring(4),
						(String) value);
			}
			if (actId.startsWith(REPO_ID_PREFIX)) {
				setConfigValue(repositoryConfig, actId.substring(4),
						(String) value);
			}
		} catch (IOException e) {
			showExceptionMessage(e);
		}

	}

	public boolean isPropertyResettable(Object id) {
		return isPropertySet(id);
	}

	private void setConfigValue(StoredConfig configuration, String key,
			String value) throws IOException {
		// we un-set empty strings, as the config API does not allow to
		// distinguish this case
		// (null is returned, even if the value is set to "", but in the
		// effective configuration
		// this results in shadowing of the base configured values
		StringTokenizer tok = new StringTokenizer(key, "."); //$NON-NLS-1$
		if (tok.countTokens() == 2) {
			if (value == null || value.length() == 0) {
				configuration.unset(tok.nextToken(), null, tok.nextToken());
			} else {
				configuration.setString(tok.nextToken(), null, tok.nextToken(),
						value);
			}
		}
		if (tok.countTokens() == 3) {
			if (value == null || value.length() == 0) {
				configuration.unset(tok.nextToken(), tok.nextToken(), tok
						.nextToken());
			} else {
				configuration.setString(tok.nextToken(), tok.nextToken(), tok
						.nextToken(), value);
			}

		}
		configuration.save();
	}

	private void showExceptionMessage(Exception e) {
		org.eclipse.egit.ui.Activator.handleError(UIText.RepositoryPropertySource_ErrorHeader, e, true);
	}

}
