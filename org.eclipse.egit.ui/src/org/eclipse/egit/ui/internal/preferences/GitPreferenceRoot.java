/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2013, Dariusz Luksza <dariusz.luksza@gmail.com>
 * Copyright (C) 2016, 2022 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.transport.sshd.agent.ConnectorFactory;
import org.eclipse.jgit.transport.sshd.agent.ConnectorFactory.ConnectorDescriptor;
import org.eclipse.jgit.util.LfsFactory;
import org.eclipse.jgit.util.LfsFactory.LfsInstallCommand;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/** Root preference page for the all of our workspace preferences. */
public class GitPreferenceRoot extends DoublePreferencesPreferencePage
		implements IWorkbenchPreferencePage {
	private final static int GROUP_SPAN = 3;

	private final static String[][] HTTP_CLIENT_NAMES_AND_VALUES = new String[2][2];

	private final static boolean HAS_DEBUG_UI = hasDebugUiBundle();

	static {
		HTTP_CLIENT_NAMES_AND_VALUES[0][0] = UIText.GitPreferenceRoot_HttpClient_Jdk_Label;
		HTTP_CLIENT_NAMES_AND_VALUES[0][1] = "jdk"; //$NON-NLS-1$
		HTTP_CLIENT_NAMES_AND_VALUES[1][0] = UIText.GitPreferenceRoot_HttpClient_Apache_Label;
		HTTP_CLIENT_NAMES_AND_VALUES[1][1] = "apache"; //$NON-NLS-1$
	}

	private Group remoteConnectionsGroup;

	private BooleanFieldEditor useSshAgent;

	private ComboFieldEditor defaultSshAgent;

	/**
	 * The default constructor
	 */
	public GitPreferenceRoot() {
		super(FLAT);
	}

	/**
	 * @return true if add to index automatically is enabled
	 */
	public static boolean autoAddToIndex() {
		return Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.MERGE_TOOL_AUTO_ADD_TO_INDEX);
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	protected IPreferenceStore doGetSecondaryPreferenceStore() {
		return new ScopedPreferenceStore(InstanceScope.INSTANCE,
				org.eclipse.egit.core.Activator.PLUGIN_ID);
	}

	@Override
	public void init(final IWorkbench workbench) {
		// Nothing to do
	}

	@Override
	protected void createFieldEditors() {
		Composite main = getFieldEditorParent();
		GridLayoutFactory.swtDefaults().margins(0, 0).applyTo(main);

		Group cloningGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		cloningGroup.setText(UIText.GitPreferenceRoot_CloningRepoGroupHeader);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(cloningGroup);
		DirectoryFieldEditor editor = new DirectoryFieldEditor(
				GitCorePreferences.core_defaultRepositoryDir,
				UIText.GitPreferenceRoot_DefaultRepoFolderLabel, cloningGroup) {

			/** The own control is the variableButton */
			private static final int NUMBER_OF_OWN_CONTROLS = 1;

			@Override
			public void setPreferenceStore(IPreferenceStore store) {
				super.setPreferenceStore(
						store == null ? null : getSecondaryPreferenceStore());
			}

			@Override
			protected boolean doCheckState() {
				String fileName = getTextControl().getText();
				fileName = fileName.trim();
				if (fileName.length() == 0 && isEmptyStringAllowed())
					return true;

				IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
				String substitutedFileName;
				try {
					substitutedFileName = manager.performStringSubstitution(fileName);
				} catch (CoreException e) {
					// It's apparently invalid
					return false;
				}

				File file = new File(substitutedFileName);
				// other than the super implementation, we don't
				// require the file to exist
				return !file.exists() || file.isDirectory();
			}

			@Override
			public int getNumberOfControls() {
				return super.getNumberOfControls() + NUMBER_OF_OWN_CONTROLS;
			}

			@Override
			protected void doFillIntoGrid(Composite parent, int numColumns) {
				super.doFillIntoGrid(parent, numColumns - NUMBER_OF_OWN_CONTROLS);
			}

			@Override
			protected void adjustForNumColumns(int numColumns) {
				super.adjustForNumColumns(numColumns - NUMBER_OF_OWN_CONTROLS);
			}

			@Override
			protected void createControl(Composite parent) {
				// setting validate strategy using the setter method is too late
				super.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);

				super.createControl(parent);

				if (HAS_DEBUG_UI)
					addVariablesButton(parent);
			}

			private void addVariablesButton(Composite parent) {
				Button variableButton = new Button(parent, SWT.PUSH);
				variableButton.setText(UIText.GitPreferenceRoot_DefaultRepoFolderVariableButton);

				variableButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						org.eclipse.debug.ui.StringVariableSelectionDialog dialog = new org.eclipse.debug.ui.StringVariableSelectionDialog(
								getShell());
						int returnCode = dialog.open();
						if (returnCode == Window.OK)
							setStringValue(dialog.getVariableExpression());
					}
				});
			}
		};
		updateMargins(cloningGroup);
		editor.setEmptyStringAllowed(false);
		editor.getLabelControl(cloningGroup).setToolTipText(
				UIText.GitPreferenceRoot_DefaultRepoFolderTooltip);
		addField(editor);

		remoteConnectionsGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(remoteConnectionsGroup);
		remoteConnectionsGroup
				.setText(UIText.GitPreferenceRoot_RemoteConnectionsGroupHeader);

		IntegerFieldEditor pullEditor = new IntegerFieldEditor(
				GitCorePreferences.core_maxPullThreadsCount,
				UIText.GitPreferenceRoot_MaxPullThreadsCount,
				remoteConnectionsGroup) {

			@Override
			public void setPreferenceStore(IPreferenceStore store) {
				super.setPreferenceStore(
						store == null ? null : getSecondaryPreferenceStore());
			}
		};
		pullEditor.getLabelControl(remoteConnectionsGroup).setToolTipText(
				UIText.GitPreferenceRoot_MaxPullThreadsCountTooltip);
		addField(pullEditor);

		IntegerFieldEditor timeoutEditor = new IntegerFieldEditor(
				GitCorePreferences.core_remoteConnectionTimeout,
				UIText.RemoteConnectionPreferencePage_TimeoutLabel,
				remoteConnectionsGroup) {

			@Override
			public void setPreferenceStore(IPreferenceStore store) {
				super.setPreferenceStore(
						store == null ? null : getSecondaryPreferenceStore());
			}
		};
		timeoutEditor.getLabelControl(remoteConnectionsGroup).setToolTipText(
				UIText.RemoteConnectionPreferencePage_ZeroValueTooltip);
		addField(timeoutEditor);
		ComboFieldEditor httpClient = new ComboFieldEditor(
				GitCorePreferences.core_httpClient,
				UIText.RemoteConnectionPreferencePage_HttpClientLabel,
				HTTP_CLIENT_NAMES_AND_VALUES, remoteConnectionsGroup) {

			@Override
			public void setPreferenceStore(IPreferenceStore store) {
				super.setPreferenceStore(
						store == null ? null : getSecondaryPreferenceStore());
			}
		};
		addField(httpClient);
		ConnectorFactory factory = ConnectorFactory.getDefault();
		if (factory != null) {
			boolean isWindows = SystemReader.getInstance().isWindows();
			useSshAgent = new BooleanFieldEditor(
					GitCorePreferences.core_sshAgent,
					UIText.GitPreferenceRoot_SshAgent_Label,
					remoteConnectionsGroup) {

				@Override
				public int getNumberOfControls() {
					return 2;
				}

				@Override
				public void setPreferenceStore(IPreferenceStore store) {
					super.setPreferenceStore(store == null ? null
							: getSecondaryPreferenceStore());
				}
			};
			if (!isWindows) {
				String productName = getProductName();
				useSshAgent.getDescriptionControl(remoteConnectionsGroup)
						.setToolTipText(MessageFormat.format(
								UIText.GitPreferenceRoot_SshAgent_Tooltip,
								productName));
			}
			addField(useSshAgent);
			Collection<ConnectorDescriptor> available = factory
					.getSupportedConnectors();
			if (available.size() > 1) {
				String[][] items = new String[available.size()][2];
				int i = 0;
				for (ConnectorDescriptor desc : available) {
					items[i][0] = desc.getDisplayName();
					items[i][1] = desc.getIdentityAgent();
					i++;
				}
				defaultSshAgent = new ComboFieldEditor(
						GitCorePreferences.core_sshDefaultAgent,
						UIText.GitPreferenceRoot_SshDefaultAgent_Label, items,
						remoteConnectionsGroup) {

					@Override
					public void setPreferenceStore(IPreferenceStore store) {
						super.setPreferenceStore(store == null ? null
								: getSecondaryPreferenceStore());
					}
				};
				defaultSshAgent.getLabelControl(remoteConnectionsGroup)
						.setToolTipText(
								UIText.GitPreferenceRoot_SshDefaultAgent_Tooltip);
				GridDataFactory.fillDefaults()
						.indent(UIUtils.getControlIndent(), 0)
						.applyTo(defaultSshAgent
								.getLabelControl(remoteConnectionsGroup));

				addField(defaultSshAgent);
			}
		}
		updateMargins(remoteConnectionsGroup);

		Group repoChangeScannerGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(repoChangeScannerGroup);
		repoChangeScannerGroup
				.setText(UIText.GitPreferenceRoot_RepoChangeScannerGroupHeader);

		IntegerFieldEditor intervalField = new IntegerFieldEditor(
				UIPreferences.REFRESH_INDEX_INTERVAL,
				UIText.RefreshPreferencesPage_RefreshIndexInterval,
				repoChangeScannerGroup);
		intervalField.getLabelControl(repoChangeScannerGroup).setToolTipText(
				UIText.RefreshPreferencesPage_RefreshIndexIntervalTooltip);
		addField(intervalField);
		addField(new BooleanFieldEditor(UIPreferences.REFRESH_ONLY_WHEN_ACTIVE,
				UIText.RefreshPreferencesPage_RefreshOnlyWhenActive,
				repoChangeScannerGroup) {
			@Override
			public int getNumberOfControls() {
				return 2;
			}
		});
		updateMargins(repoChangeScannerGroup);

		Group blameGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(blameGroup);
		blameGroup.setText(UIText.GitPreferenceRoot_BlameGroupHeader);
		addField(new BooleanFieldEditor(UIPreferences.BLAME_IGNORE_WHITESPACE,
				UIText.GitPreferenceRoot_BlameIgnoreWhitespaceLabel, blameGroup));
		updateMargins(blameGroup);

		Group secureGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(secureGroup);
		secureGroup.setText(UIText.GitPreferenceRoot_SecureStoreGroupLabel);
		addField(new BooleanFieldEditor(UIPreferences.CLONE_WIZARD_STORE_SECURESTORE,
				UIText.GitPreferenceRoot_SecureStoreUseByDefault, secureGroup));
		addField(new BooleanFieldEditor(
				GitCorePreferences.core_saveCredentialsInSecureStore,
				UIText.GitPreferenceRoot_SecureStoreUseForSshKeys,
				secureGroup) {

			@Override
			public void setPreferenceStore(IPreferenceStore store) {
				super.setPreferenceStore(
						store == null ? null : getSecondaryPreferenceStore());
			}
		});
		updateMargins(secureGroup);

		boolean lfsAvailable = LfsFactory.getInstance().isAvailable()
				&& LfsFactory.getInstance().getInstallCommand() != null;
		Group lfsGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridLayoutFactory.fillDefaults().applyTo(lfsGroup);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(lfsGroup);
		lfsGroup.setText(
				lfsAvailable ? UIText.GitPreferenceRoot_lfsSupportCaption : UIText.GitPreferenceRoot_lfsSupportCaptionNotAvailable);
		Button lfsEnable = new Button(lfsGroup, SWT.PUSH);
		lfsEnable.setEnabled(lfsAvailable);
		lfsEnable.setText(UIText.GitPreferenceRoot_lfsSupportInstall);
		lfsEnable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// enable LFS support for user.
				LfsInstallCommand cmd = LfsFactory.getInstance()
						.getInstallCommand();
				try {
					if (cmd != null) {
						cmd.call();

						MessageDialog.openInformation(getShell(),
								UIText.GitPreferenceRoot_lfsSupportSuccessTitle,
								UIText.GitPreferenceRoot_lfsSupportSuccessMessage);
					}
				} catch (Exception ex) {
					Activator.handleError(
							UIText.ConfigurationChecker_installLfsCannotInstall,
							ex, true);
				}
			}
		});
		updateMargins(lfsGroup);
	}

	@Override
	protected void initialize() {
		super.initialize();
		if (defaultSshAgent != null) {
			useSshAgent.setPropertyChangeListener(event -> {
				if (FieldEditor.VALUE.equals(event.getProperty())) {
					defaultSshAgent.setEnabled(
							((Boolean) event.getNewValue()).booleanValue(),
							remoteConnectionsGroup);
				}
			});
		}
	}

	private String getProductName() {
		IProduct product = Platform.getProduct();
		String name = product == null ? null : product.getName();
		return name == null ? UIText.GitPreferenceRoot_DefaultProductName
				: name;
	}

	private void updateMargins(Group group) {
		// make sure there is some room between the group border
		// and the controls in the group
		GridLayout layout = (GridLayout) group.getLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
	}

	private static final boolean hasDebugUiBundle() {
		try {
			return Class
					.forName("org.eclipse.debug.ui.StringVariableSelectionDialog") != null; //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
