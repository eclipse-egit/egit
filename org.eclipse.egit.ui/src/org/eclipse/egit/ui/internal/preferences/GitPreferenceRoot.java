/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2013, Dariusz Luksza <dariusz.luksza@gmail.com>
 * Copyright (C) 2015, Andre Bossert <anb0s@anbos.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.externaltools.BaseToolManager;
import org.eclipse.egit.ui.internal.externaltools.DiffToolManager;
import org.eclipse.egit.ui.internal.externaltools.ITool;
import org.eclipse.egit.ui.internal.externaltools.MergeToolManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.events.ConfigChangedEvent;
import org.eclipse.jgit.events.ConfigChangedListener;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/** Root preference page for the all of our workspace preferences. */
public class GitPreferenceRoot extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {
	private final static int GROUP_SPAN = 3;

	private final static String[][] MERGE_MODE_NAMES_AND_VALUES = new String[3][2];

	private final static String[][] MERGE_TOOL_NAMES_AND_VALUES = new String[3][2];

	private final static String[][] DIFF_TOOL_NAMES_AND_VALUES = new String[3][2];

	private final static boolean HAS_DEBUG_UI = hasDebugUiBundle();

	static FileBasedConfig userScopedConfig = null;

	static String[][] diffToolsList = null;

	static String[][] mergeToolsList = null;

	static ListenerHandle userScopedConfigChangeListener = null;

	static {
		loadUserScopedConfig();
	}

	static {
		MERGE_MODE_NAMES_AND_VALUES[0][0] = UIText.GitPreferenceRoot_MergeMode_0_Label;
		MERGE_MODE_NAMES_AND_VALUES[0][1] = "0";//$NON-NLS-1$
		MERGE_MODE_NAMES_AND_VALUES[1][0] = UIText.GitPreferenceRoot_MergeMode_1_Label;
		MERGE_MODE_NAMES_AND_VALUES[1][1] = "1";//$NON-NLS-1$
		MERGE_MODE_NAMES_AND_VALUES[2][0] = UIText.GitPreferenceRoot_MergeMode_2_Label;
		MERGE_MODE_NAMES_AND_VALUES[2][1] = "2"; //$NON-NLS-1$
	}

	static {
		MERGE_TOOL_NAMES_AND_VALUES[0][0] = UIText.GitPreferenceRoot_MergeTool_0_Label;
		MERGE_TOOL_NAMES_AND_VALUES[0][1] = "0";//$NON-NLS-1$
		MERGE_TOOL_NAMES_AND_VALUES[1][0] = UIText.GitPreferenceRoot_MergeTool_1_Label;
		MERGE_TOOL_NAMES_AND_VALUES[1][1] = "1";//$NON-NLS-1$
		MERGE_TOOL_NAMES_AND_VALUES[2][0] = UIText.GitPreferenceRoot_MergeTool_2_Label;
		MERGE_TOOL_NAMES_AND_VALUES[2][1] = "2"; //$NON-NLS-1$
	}

	static {
		DIFF_TOOL_NAMES_AND_VALUES[0][0] = UIText.GitPreferenceRoot_DiffTool_0_Label;
		DIFF_TOOL_NAMES_AND_VALUES[0][1] = "0";//$NON-NLS-1$
		DIFF_TOOL_NAMES_AND_VALUES[1][0] = UIText.GitPreferenceRoot_DiffTool_1_Label;
		DIFF_TOOL_NAMES_AND_VALUES[1][1] = "1";//$NON-NLS-1$
		DIFF_TOOL_NAMES_AND_VALUES[2][0] = UIText.GitPreferenceRoot_DiffTool_2_Label;
		DIFF_TOOL_NAMES_AND_VALUES[2][1] = "2"; //$NON-NLS-1$
	}

	/**
	 * The default constructor
	 */
	public GitPreferenceRoot() {
		super(FLAT);
	}

	/**
	 * @return true if external diff tool and false if internal compare should
	 *         be used
	 */
	public static boolean useExternalDiffTool() {
		int diffTool = Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.DIFF_TOOL);
		if (diffTool != 0) {
			String diffToolCustom = Activator.getDefault().getPreferenceStore()
					.getString(UIPreferences.DIFF_TOOL_CUSTOM);
			if (!diffToolCustom.equals("none")) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

	/**
	 * @return true if external merge tool and false if internal compare should
	 *         be used
	 */
	public static boolean useExternalMergeTool() {
		int diffTool = Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.MERGE_TOOL);
		if (diffTool != 0) {
			String diffToolCustom = Activator.getDefault().getPreferenceStore()
					.getString(UIPreferences.MERGE_TOOL_CUSTOM);
			if (!diffToolCustom.equals("none")) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

	/**
	 * @return external diff tool command
	 */
	public static String getExternalDiffToolCommand() {
		String diffCmd = null;
		ITool tool = getExternalDiffTool();
		if (tool != null) {
			diffCmd = tool.getCommand();
		}
		return diffCmd;
	}

	/**
	 * @return external merge tool command
	 */
	public static String getExternalMergeToolCommand() {
		String mergeCmd = null;
		ITool tool = getExternalMergeTool();
		if (tool != null) {
			mergeCmd = tool.getCommand();
		}
		return mergeCmd;
	}

	/**
	 * @return the tool
	 */
	public static ITool getExternalDiffTool() {
		return getExternalTool(UIPreferences.DIFF_TOOL,
				UIPreferences.DIFF_TOOL_CUSTOM, DiffToolManager.getInstance());
	}

	/**
	 * @return the tool
	 */
	public static ITool getExternalMergeTool() {
		return getExternalTool(UIPreferences.MERGE_TOOL,
				UIPreferences.MERGE_TOOL_CUSTOM,
				MergeToolManager.getInstance());
	}

	/**
	 * @param toolName
	 * @param attrName
	 * @return the attribute value
	 */
	public static String getExternalDiffToolAttributeValue(String toolName,
			String attrName) {
		return DiffToolManager.getInstance().getAttributeValue(toolName,
				attrName, true);
	}

	/**
	 * @param toolName
	 * @param attrName
	 * @return the attribute value
	 */
	public static boolean getExternalDiffToolAttributeValueBoolean(
			String toolName,
			String attrName) {
		return DiffToolManager.getInstance().getAttributeValueBoolean(toolName,
				attrName, true);
	}

	/**
	 * @param toolName
	 * @param attrName
	 * @return the attribute value
	 */
	public static String getExternalMergeToolAttributeValue(String toolName,
			String attrName) {
		return MergeToolManager.getInstance().getAttributeValue(toolName,
				attrName, true);
	}

	/**
	 * @param toolName
	 * @param attrName
	 * @return the attribute value
	 */
	public static boolean getExternalMergeToolAttributeValueBoolean(
			String toolName,
			String attrName) {
		return MergeToolManager.getInstance().getAttributeValueBoolean(toolName,
				attrName, true);
	}

	/**
	 * @return the evaluated bash path
	 */
	public static String getBashPath() {
		String bashPath = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.BASH_PATH);
		if (bashPath != null && !bashPath.equals("")) { //$NON-NLS-1$
			IStringVariableManager manager = VariablesPlugin.getDefault()
					.getStringVariableManager();
			String substitutedFileName;
			try {
				substitutedFileName = manager
						.performStringSubstitution(bashPath);
			} catch (CoreException e) {
				// It's apparently invalid
				return null;
			}
			File file = new File(substitutedFileName);
			// other than the super implementation, we don't
			// require the file to exist
			if (file.exists() || !file.isDirectory()) {
				return file.getAbsolutePath();
			}
		}
		return null;
	}

	/**
	 * @return true if add to index automatically is enabled
	 */
	public static boolean autoAddToIndex() {
		return Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.MERGE_TOOL_AUTO_ADD_TO_INDEX);
	}

	private static ITool getExternalTool(String prefNameTool,
			String prefNameToolCustom, BaseToolManager manager) {
		ITool tool = null;
		int toolNr = Activator.getDefault().getPreferenceStore()
				.getInt(prefNameTool);
		if (toolNr != 0) {
			String toolName = null;
			loadUserScopedConfig();
			// default
			if (toolNr == 1) {
				toolName = manager.getDefaultToolName();
			} else { // custom
				toolName = Activator.getDefault().getPreferenceStore()
						.getString(prefNameToolCustom);
			}
			if (toolName != null && !toolName.equals("none")) { //$NON-NLS-1$
				tool = manager.getTool(toolName);
			}
		}
		return tool;
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	public void init(final IWorkbench workbench) {
		// Do nothing.
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
				if (store == null) {
					// allow reset store on dispose
					super.setPreferenceStore(store);
				} else if (getPreferenceStore() == null) {
					// only allow set store once, to the egit core version
					super.setPreferenceStore(new ScopedPreferenceStore(
							InstanceScope.INSTANCE,
							org.eclipse.egit.core.Activator.getPluginId()));
				}
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

		Group remoteConnectionsGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(remoteConnectionsGroup);
		remoteConnectionsGroup
				.setText(UIText.GitPreferenceRoot_RemoteConnectionsGroupHeader);

		IntegerFieldEditor timeoutEditor = new IntegerFieldEditor(
				UIPreferences.REMOTE_CONNECTION_TIMEOUT,
				UIText.RemoteConnectionPreferencePage_TimeoutLabel,
				remoteConnectionsGroup);
		timeoutEditor.getLabelControl(remoteConnectionsGroup).setToolTipText(
				UIText.RemoteConnectionPreferencePage_ZeroValueTooltip);
		addField(timeoutEditor);
		updateMargins(remoteConnectionsGroup);

		Group repoChangeScannerGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(repoChangeScannerGroup);
		repoChangeScannerGroup
				.setText(UIText.GitPreferenceRoot_RepoChangeScannerGroupHeader);
		addField(new BooleanFieldEditor(UIPreferences.REFESH_ON_INDEX_CHANGE,
				UIText.RefreshPreferencesPage_RefreshWhenIndexChange,
				repoChangeScannerGroup));
		addField(new BooleanFieldEditor(UIPreferences.REFESH_ONLY_WHEN_ACTIVE,
				UIText.RefreshPreferencesPage_RefreshOnlyWhenActive,
				repoChangeScannerGroup));
		updateMargins(repoChangeScannerGroup);

		loadUserScopedConfig();

		Group mergeGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(mergeGroup);
		mergeGroup.setText(UIText.GitPreferenceRoot_MergeGroupHeader);
		ComboFieldEditor mergeMode = new ComboFieldEditor(
				UIPreferences.MERGE_MODE,
				UIText.GitPreferenceRoot_MergeModeLabel,
				MERGE_MODE_NAMES_AND_VALUES, mergeGroup);
		mergeMode.getLabelControl(mergeGroup).setToolTipText(
				UIText.GitPreferenceRoot_MergeModeTooltip);
		addField(mergeMode);
		ComboFieldEditor mergeTool = new ComboFieldEditor(
				UIPreferences.MERGE_TOOL,
				UIText.GitPreferenceRoot_MergeToolLabel,
				MERGE_TOOL_NAMES_AND_VALUES, mergeGroup);
		mergeTool.getLabelControl(mergeGroup)
				.setToolTipText(UIText.GitPreferenceRoot_MergeToolTooltip);
		addField(mergeTool);
		ComboFieldEditor mergeToolCustom = new ComboFieldEditor(
				UIPreferences.MERGE_TOOL_CUSTOM,
				UIText.GitPreferenceRoot_MergeToolCustomLabel, mergeToolsList,
				mergeGroup);
		mergeToolCustom.getLabelControl(mergeGroup).setToolTipText(
				UIText.GitPreferenceRoot_MergeToolCustomTooltip);
		addField(mergeToolCustom);
		BooleanFieldEditor autoAddToIndex = new BooleanFieldEditor(
				UIPreferences.MERGE_TOOL_AUTO_ADD_TO_INDEX,
				UIText.GitPreferenceRoot_MergeToolAutoAddLabel, mergeGroup);
		addField(autoAddToIndex);
		updateMargins(mergeGroup);

		Group diffGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(diffGroup);
		diffGroup.setText(UIText.GitPreferenceRoot_DiffGroupHeader);
		ComboFieldEditor diffTool = new ComboFieldEditor(
				UIPreferences.DIFF_TOOL, UIText.GitPreferenceRoot_DiffToolLabel,
				DIFF_TOOL_NAMES_AND_VALUES, diffGroup);
		diffTool.getLabelControl(diffGroup)
				.setToolTipText(UIText.GitPreferenceRoot_DiffToolTooltip);
		addField(diffTool);
		ComboFieldEditor diffToolCustom = new ComboFieldEditor(
				UIPreferences.DIFF_TOOL_CUSTOM,
				UIText.GitPreferenceRoot_DiffToolCustomLabel, diffToolsList,
				diffGroup);
		diffToolCustom.getLabelControl(diffGroup)
				.setToolTipText(UIText.GitPreferenceRoot_DiffToolCustomTooltip);
		addField(diffToolCustom);
		updateMargins(diffGroup);

		Group bashGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		bashGroup.setText(UIText.GitPreferenceRoot_BashGroupHeader);
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
				.applyTo(bashGroup);
		FileFieldEditor bashPathEditor = new FileFieldEditor(
				UIPreferences.BASH_PATH, UIText.GitPreferenceRoot_BashPathLabel,
				bashGroup) {

			/** The own control is the variableButton */
			private static final int NUMBER_OF_OWN_CONTROLS = 1;

			@Override
			protected boolean doCheckState() {
				String fileName = getTextControl().getText();
				fileName = fileName.trim();
				if (fileName.length() == 0 && isEmptyStringAllowed())
					return true;
				IStringVariableManager manager = VariablesPlugin.getDefault()
						.getStringVariableManager();
				String substitutedFileName;
				try {
					substitutedFileName = manager
							.performStringSubstitution(fileName);
				} catch (CoreException e) {
					// It's apparently invalid
					return false;
				}
				File file = new File(substitutedFileName);
				// other than the super implementation, we don't
				// require the file to exist
				return file.exists() || !file.isDirectory();
			}

			@Override
			public int getNumberOfControls() {
				return super.getNumberOfControls() + NUMBER_OF_OWN_CONTROLS;
			}

			@Override
			protected void doFillIntoGrid(Composite parent, int numColumns) {
				super.doFillIntoGrid(parent,
						numColumns - NUMBER_OF_OWN_CONTROLS);
			}

			@Override
			protected void adjustForNumColumns(int numColumns) {
				super.adjustForNumColumns(numColumns - NUMBER_OF_OWN_CONTROLS);
			}

			@Override
			protected void createControl(Composite parent) {
				// setting validate strategy using the setter method is too late
				super.setValidateStrategy(
						StringFieldEditor.VALIDATE_ON_KEY_STROKE);
				super.createControl(parent);
				if (HAS_DEBUG_UI)
					addVariablesButton(parent);
			}

			private void addVariablesButton(Composite parent) {
				Button variableButton = new Button(parent, SWT.PUSH);
				variableButton.setText(
						UIText.GitPreferenceRoot_BashPathVariableButton);
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
		updateMargins(bashGroup);
		bashPathEditor.setEmptyStringAllowed(false);
		bashPathEditor.getLabelControl(bashGroup)
				.setToolTipText(UIText.GitPreferenceRoot_BashPathTooltip);
		addField(bashPathEditor);

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
		updateMargins(secureGroup);
	}

	private static void loadUserScopedConfig() {
		if (userScopedConfig == null || userScopedConfig.isOutdated()) {
			userScopedConfig = SystemReader.getInstance()
					.openUserConfig(null, FS.DETECTED);
			try {
				userScopedConfig.load();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			} catch (ConfigInvalidException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
			diffToolsList = getCustomDiffTools();
			mergeToolsList = getCustomMergeTools();
			if (userScopedConfigChangeListener != null) {
				userScopedConfigChangeListener = userScopedConfig
						.addChangeListener(new ConfigChangedListener() {
							@Override
							public void onConfigChanged(
									ConfigChangedEvent event) {
								diffToolsList = getCustomDiffTools();
								mergeToolsList = getCustomMergeTools();
							}
						});
			}
		}
	}

	private static String[][] getCustomDiffOrMergeTools(
			String sectionName4BaseTool, String[][] baseToolAttributes,
			String sectionName4AllTools, String[][] allToolAttributes,
			BaseToolManager manager) {
		List<String> toolsList = loadToolManager(sectionName4BaseTool,
				baseToolAttributes, sectionName4AllTools, allToolAttributes,
				manager);
		// convert to right type
		String[][] toolsArray = new String[toolsList.size()][2];
		for (int index = 0; index < toolsList.size(); index++) {
			toolsArray[index][0] = toolsList.get(index);
			toolsArray[index][1] = toolsList.get(index);
		}
		return toolsArray;
	}

	private static List<String> loadToolManager(String sectionName4BaseTool,
			String[][] baseToolAttributes, String sectionName4AllTools,
			String[][] allToolAttributes, BaseToolManager manager) {
		List<String> toolsList = new ArrayList<String>();
		manager.removeAllUserDefinitions();
		if (userScopedConfig != null) {
			// load base tool attributes (e.g. "tool")
			loadExternalToolAttributes(userScopedConfig, sectionName4BaseTool,
					null, manager, baseToolAttributes, true);
			// load all <merge|diff>tool attributes (e.g. "prompt")
			loadExternalToolAttributes(userScopedConfig, sectionName4AllTools,
					null, manager, allToolAttributes, true);
			String defaultDiffMergeTool = manager.getDefaultToolName();
			// get all diff / merge tools
			Set<String> diffMergeTools = userScopedConfig
					.getSubsections(sectionName4AllTools);
			// if no diff / merge tools found check the default one or none
			if (diffMergeTools.size() == 0) {
				if (defaultDiffMergeTool != null) {
					toolsList.add(defaultDiffMergeTool);
				} else {
					toolsList.add("none"); //$NON-NLS-1$
				}
			} else {
				// add default diff / merge tool if not in the list already
				if (defaultDiffMergeTool != null
						&& !diffMergeTools.contains(defaultDiffMergeTool)) {
					toolsList.add(defaultDiffMergeTool);
				}
				for (String diffMergeToolName : diffMergeTools) {
					toolsList.add(diffMergeToolName);
					// add tool
					addExternalTool(userScopedConfig,
							sectionName4AllTools, diffMergeToolName, manager);
					// load all <merge|diff>tool "<toolname>" attributes (e.g.
					// "trustExitCode")
					loadExternalToolAttributes(userScopedConfig,
							sectionName4AllTools, diffMergeToolName, manager,
							allToolAttributes, false);
				}
			}
		}
		return toolsList;
	}

	private static String[][] getCustomMergeTools() {
		BaseToolManager manager = MergeToolManager.getInstance();
		String[][] baseToolAttributes = { { "tool", null } //$NON-NLS-1$
		};
		String[][] allToolAttributes = { { "prompt", "true" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "trustExitCode", "false" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "keepBackup", "true" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "keepTemporaries", "false" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "writeToTemp", "false" } //$NON-NLS-1$ //$NON-NLS-2$
		};
		System.out.println("----- getCustomMergeTools -----"); //$NON-NLS-1$
		return getCustomDiffOrMergeTools(ConfigConstants.CONFIG_KEY_MERGE,
				baseToolAttributes, "mergetool", //$NON-NLS-1$
				allToolAttributes, manager);
	}

	private static String[][] getCustomDiffTools() {
		BaseToolManager manager = DiffToolManager.getInstance();
		String[][] baseToolAttributes = { { "tool", null }, //$NON-NLS-1$
				{ "guitool", null } //$NON-NLS-1$
		};
		String[][] allToolAttributes = { { "prompt", "true" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "trustExitCode", "false" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "keepTemporaries", "false" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "writeToTemp", "false" } //$NON-NLS-1$ //$NON-NLS-2$
		};
		System.out.println("----- getCustomDiffTools -----"); //$NON-NLS-1$
		return getCustomDiffOrMergeTools(ConfigConstants.CONFIG_DIFF_SECTION,
				baseToolAttributes, "difftool", //$NON-NLS-1$
				allToolAttributes, manager);
	}

	private static void loadExternalToolAttributes(FileBasedConfig config,
			String sectionName,
			String subSectionName, BaseToolManager manager,
			String[][] attributes, boolean useDefault) {
		// get other known parameters
		for (String[] attr : attributes) {
			String attrName = attr[0];
			String attrDefValue = attr[1];
			String attrValue = config.getString(sectionName, subSectionName, attrName);
			if (attrValue != null) {
				manager.addAttribute(subSectionName, attrName, attrValue);
				System.out
						.println("addAttribute: FOUND: " + subSectionName + ", " //$NON-NLS-1$ //$NON-NLS-2$
						+ attrName + ", " + attrValue); //$NON-NLS-1$
			} else if (useDefault && attrDefValue != null) {
				manager.addAttribute(subSectionName, attrName, attrDefValue);
				System.out.println(
						"addAttribute: DEFAULT: " + subSectionName + ", " //$NON-NLS-1$ //$NON-NLS-2$
								+ attrName + ", " + attrDefValue); //$NON-NLS-1$
			}
		}
	}

	private static void addExternalTool(FileBasedConfig config,
			String sectionName,
			String toolName, BaseToolManager manager) {
		if (userScopedConfig != null) {
			String toolPath = config.getString(sectionName, toolName,
					"path"); //$NON-NLS-1$
			if (toolPath != null && !toolPath.equals("")) { //$NON-NLS-1$
				manager.addUserOverloadedTool(toolName, toolPath);
			} else {
				String toolCmd = config.getString(sectionName,
						toolName, "cmd"); //$NON-NLS-1$
				if (toolCmd != null && !toolCmd.equals("")) { //$NON-NLS-1$
					manager.addUserDefinedTool(toolName, toolCmd);
				}
			}
		}
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
