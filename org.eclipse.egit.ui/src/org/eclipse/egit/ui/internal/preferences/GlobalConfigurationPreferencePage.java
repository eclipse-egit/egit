/*******************************************************************************
 * Copyright (c) 2010, 2023 SAP AG and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.SWTUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jgit.events.ConfigChangedEvent;
import org.eclipse.jgit.events.ConfigChangedListener;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.UserConfigFile;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Displays the global Git configuration and allows to edit it.
 * <p>
 * In EGit, this maps to the user configuration.
 */
public class GlobalConfigurationPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {
	/** The ID of this page */
	public static final String ID = "org.eclipse.egit.ui.internal.preferences.GlobalConfigurationPreferencePage"; //$NON-NLS-1$

	private FileBasedConfig userConfig;

	private FileBasedConfig xdgConfig;

	private FileBasedConfig sysConfig;

	private StackLayout repoConfigStackLayout;

	private StackLayout userConfigStackLayout;

	private List<Repository> repositories;

	private Map<Repository, ConfigurationEditorComponent> repoConfigEditors = new HashMap<>();

	private Set<Repository> dirtyRepositories = new HashSet<>();

	private boolean userIsDirty;

	private boolean xdgIsDirty;

	private boolean sysIsDirty;

	private ConfigurationEditorComponent userConfigEditor;

	private ConfigurationEditorComponent xdgConfigEditor;

	private ConfigurationEditorComponent sysConfigEditor;

	private Composite repoConfigComposite;

	private Composite userConfigComposite;

	@Override
	protected Control createContents(Composite parent) {

		Composite composite = SWTUtils.createHVFillComposite(parent,
				SWTUtils.MARGINS_NONE);
		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		tabFolder.setLayoutData(SWTUtils.createHVFillGridData());
		Control userTabControl;
		if (xdgConfig != null) {
			Composite userTab = new Composite(tabFolder, SWT.NONE);
			String variable = SystemReader.getInstance().isWindows()
					? '%' + Constants.XDG_CONFIG_HOME + '%'
					: '$' + Constants.XDG_CONFIG_HOME;
			String[] items = { "~" + File.separatorChar + ".gitconfig", //$NON-NLS-1$ //$NON-NLS-2$
					variable + File.separatorChar + "git" //$NON-NLS-1$
							+ File.separatorChar + Constants.CONFIG };
			Combo userCombo = insertCombo(userTab,
					UIText.GlobalConfigurationPreferencePage_userSettingLabel,
					items);
			userCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					Combo combo = (Combo) e.widget;
					switch (combo.getSelectionIndex()) {
					case 0:
						userConfigStackLayout.topControl = userConfigEditor
								.getContents();
						break;
					case 1:
						userConfigStackLayout.topControl = xdgConfigEditor
								.getContents();
						break;
					default:
						return;
					}
					userConfigComposite.layout();
				}
			});

			userConfigComposite = new Composite(userTab, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true)
					.applyTo(userConfigComposite);
			userConfigStackLayout = new StackLayout();
			userConfigComposite.setLayout(userConfigStackLayout);
			userConfigEditor = new ConfigurationEditorComponent(
					userConfigComposite, userConfig, true, 5) {
				@Override
				protected void setErrorMessage(String message) {
					GlobalConfigurationPreferencePage.this
							.setErrorMessage(message);
				}

				@Override
				protected void setDirty(boolean dirty) {
					userIsDirty = dirty;
					updateApplyButton();
				}
			};
			Control control = userConfigEditor.createContents();
			Dialog.applyDialogFont(control);
			userConfigStackLayout.topControl = control;

			xdgConfigEditor = new ConfigurationEditorComponent(
					userConfigComposite,
					xdgConfig, true, 5) {
				@Override
				protected void setErrorMessage(String message) {
					GlobalConfigurationPreferencePage.this
							.setErrorMessage(message);
				}

				@Override
				protected void setDirty(boolean dirty) {
					xdgIsDirty = dirty;
					updateApplyButton();
				}
			};
			control = xdgConfigEditor.createContents();
			Dialog.applyDialogFont(control);
			userTabControl = userTab;
		} else {
			userConfigEditor = new ConfigurationEditorComponent(tabFolder,
					userConfig, true, 5) {
				@Override
				protected void setErrorMessage(String message) {
					GlobalConfigurationPreferencePage.this
							.setErrorMessage(message);
				}

				@Override
				protected void setDirty(boolean dirty) {
					userIsDirty = dirty;
					updateApplyButton();
				}
			};
			userTabControl = userConfigEditor.createContents();
			Dialog.applyDialogFont(userTabControl);
		}
		sysConfigEditor = new ConfigurationEditorComponent(tabFolder, sysConfig,
				true, 5) {
			@Override
			protected void setErrorMessage(String message) {
				GlobalConfigurationPreferencePage.this.setErrorMessage(message);
			}

			@Override
			protected void setDirty(boolean dirty) {
				sysIsDirty = dirty;
				updateApplyButton();
			}
		};

		Composite repoTab = new Composite(tabFolder, SWT.NONE);
		Combo repoCombo = insertCombo(repoTab,
				UIText.GlobalConfigurationPreferencePage_repositorySettingRepositoryLabel,
				getRepositoryComboItems());
		repoCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Combo combo = (Combo) e.widget;
				showRepositoryConfiguration(combo.getSelectionIndex());
			}
		});

		repoConfigComposite = new Composite(repoTab, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(repoConfigComposite);
		repoConfigStackLayout = new StackLayout();
		repoConfigComposite.setLayout(repoConfigStackLayout);

		TabItem repoTabItem = new TabItem(tabFolder, SWT.FILL);
		repoTabItem.setControl(repoTab);
		repoTabItem.setText(UIText.GlobalConfigurationPreferencePage_repositorySettingTabTitle);

		if (repoCombo.getItemCount() > 0) {
			repoCombo.select(0);
			showRepositoryConfiguration(0);
		} else {
			repoCombo.setItems(new String[] {UIText.GlobalConfigurationPreferencePage_repositorySettingNoRepositories});
			repoCombo.select(0);
			repoCombo.setEnabled(false);
		}

		TabItem userTabItem = new TabItem(tabFolder, SWT.FILL);
		userTabItem.setControl(userTabControl);
		userTabItem.setText(
				UIText.GlobalConfigurationPreferencePage_userSettingTabTitle);

		Control result = sysConfigEditor.createContents();
		Dialog.applyDialogFont(result);
		TabItem sysTabItem = new TabItem(tabFolder, SWT.FILL);
		sysTabItem.setControl(result);
		sysTabItem.setText(
				UIText.GlobalConfigurationPreferencePage_systemSettingTabTitle);

		tabFolder.setSelection(userTabItem);

		return composite;
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible)
			updateApplyButton();
		super.setVisible(visible);
	}

	@Override
	protected void updateApplyButton() {
		if (getApplyButton() != null)
			getApplyButton().setEnabled(userIsDirty || xdgIsDirty || sysIsDirty
					|| !dirtyRepositories.isEmpty());
	}

	@Override
	public boolean performOk() {
		boolean ok = true;
		if (userIsDirty) {
			try {
				userConfigEditor.save();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
				ok = false;
			}
		}
		if (xdgIsDirty) {
			try {
				xdgConfigEditor.save();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
				ok = false;
			}
		}
		if (sysIsDirty) {
			try {
				sysConfigEditor.save();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
				ok = false;
			}
		}
		// Use array since calling save updates the dirty state which updates
		// the set of dirty repositories that is being iterated over
		final Repository[] repos = dirtyRepositories
				.toArray(new Repository[0]);
		for (Repository repository : repos) {
			ConfigurationEditorComponent editor = repoConfigEditors.get(repository);
			try {
				editor.save();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
				ok = false;
			}
		}
		return ok;
	}

	@Override
	protected void performDefaults() {
		try {
			userConfigEditor.restore();
			if (xdgConfigEditor != null) {
				xdgConfigEditor.restore();
			}
			sysConfigEditor.restore();
			for (ConfigurationEditorComponent editor : repoConfigEditors.values()) {
				editor.restore();
			}

		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
		super.performDefaults();
	}

	@Override
	public void init(IWorkbench workbench) {
		if (sysConfig == null)
			sysConfig = SystemReader.getInstance().openSystemConfig(null, FS.DETECTED);
		if (userConfig == null) {
			FileBasedConfig userCfg = SystemReader.getInstance()
					.openUserConfig(null, FS.DETECTED);
			FileBasedConfig xdgCfg = null;

			if (userCfg instanceof UserConfigFile) {
				Config base = userCfg.getBaseConfig();
				if (base instanceof FileBasedConfig) {
					xdgCfg = (FileBasedConfig) base;
					userCfg = new FileBasedConfig(null, userCfg.getFile(),
							FS.DETECTED);
					if (xdgCfg.getFile() == null
							|| !xdgCfg.getFile().isFile()) {
						xdgCfg = null;
					}
				}
			}
			userConfig = userCfg;
			xdgConfig = xdgCfg;
		}
		if (repositories == null) {
			repositories = new ArrayList<>();
			List<String> repoPaths = RepositoryUtil.INSTANCE
					.getConfiguredRepositories();
			for (String repoPath : repoPaths) {
				File gitDir = new File(repoPath);
				if (!gitDir.exists())
					continue;
				try {
					repositories.add(
							RepositoryCache.INSTANCE.lookupRepository(gitDir));
				} catch (IOException e) {
					continue;
				}
			}
			sortRepositoriesByName();
		}
	}

	private String getName(final Repository repo) {
		return RepositoryUtil.INSTANCE.getRepositoryName(repo);
	}

	private void sortRepositoriesByName() {
		Collections.sort(repositories, Comparator.comparing(this::getName,
				String.CASE_INSENSITIVE_ORDER));
	}

	private Combo insertCombo(Composite parent, String label, String[] items) {
		GridLayoutFactory.swtDefaults().margins(0, 0).applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		Composite comboComposite = new Composite(parent, SWT.NONE);
		comboComposite.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(comboComposite);
		Label l = new Label(comboComposite, SWT.NONE);
		l.setText(label);

		Combo combo = new Combo(comboComposite, SWT.READ_ONLY);
		combo.setItems(items);
		combo.select(0);
		return combo;
	}

	private String[] getRepositoryComboItems() {
		List<String> items = new ArrayList<>();
		for (Repository repository : repositories) {
			String repoName = getName(repository);
			if (repoName.length() > 0)
				items.add(repoName);
		}
		return items.toArray(new String[0]);
	}

	private void showRepositoryConfiguration(int index) {
		Repository repository = repositories.get(index);
		ConfigurationEditorComponent editor = repoConfigEditors.get(repository);
		if (editor == null) {
			editor = createConfigurationEditor(repository);
			repoConfigEditors.put(repository, editor);
		}
		repoConfigStackLayout.topControl = editor.getContents();
		repoConfigComposite.layout();
	}

	private ConfigurationEditorComponent createConfigurationEditor(final Repository repository) {
		StoredConfig repositoryConfig;
		if (repository.getConfig() instanceof FileBasedConfig) {
			File configFile = ((FileBasedConfig) repository.getConfig()).getFile();
			repositoryConfig = new FileBasedConfig(configFile, repository
					.getFS());
			repositoryConfig.addChangeListener(new ConfigChangedListener() {

				@Override
				public void onConfigChanged(ConfigChangedEvent event) {
					repository.getListenerList().dispatch(
							new ConfigChangedEvent());
				}
			});
		} else {
			repositoryConfig = repository.getConfig();
		}
		ConfigurationEditorComponent editorComponent = new ConfigurationEditorComponent(
				repoConfigComposite, repositoryConfig, true, 5) {
			@Override
			protected void setErrorMessage(String message) {
				GlobalConfigurationPreferencePage.this.setErrorMessage(message);
			}

			@Override
			protected void setDirty(boolean dirty) {
				if (dirty)
					dirtyRepositories.add(repository);
				else
					dirtyRepositories.remove(repository);
				updateApplyButton();
			}
		};
		editorComponent.createContents();
		return editorComponent;
	}
}
