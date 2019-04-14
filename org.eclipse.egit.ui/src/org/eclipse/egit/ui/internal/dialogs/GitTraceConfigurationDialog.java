/*******************************************************************************
 * Copyright (C) 2010, 2013 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;

/**
 * Preferences page for EGit UI Trace Configuration
 * <p>
 * Traces are enabled in a three-level way: first, there is a global switch,
 * then there is one "main switch" per plug-in with the key "<plug-in id>/debug"
 * which governs the overall tracing for a plug-in, and finally, there are any
 * number of options which are used as "trace locations"; they are always
 * prefixed with the plug-in id.
 */
public class GitTraceConfigurationDialog extends TitleAreaDialog {
	private final static class PluginNode {
		private final String plugin;

		PluginNode(String plugin) {
			this.plugin = plugin;
		}

		public String getPlugin() {
			return plugin;
		}

		@Override
		public String toString() {
			return NLS.bind(
					UIText.GitTraceConfigurationDialog_MainSwitchNodeText,
					plugin);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + plugin.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PluginNode other = (PluginNode) obj;
			if (!plugin.equals(other.plugin))
				return false;
			return true;
		}
	}

	private final static class OptionNode implements Comparable<OptionNode> {
		private final PluginNode plugin;

		private final String option;

		private final String strippedOption;

		OptionNode(PluginNode parent, String option) {
			this.plugin = parent;
			this.option = option;
			strippedOption = option.substring(option.indexOf('/'));
		}

		public PluginNode getPlugin() {
			return plugin;
		}

		public String getOption() {
			return this.option;
		}

		@Override
		public String toString() {
			return this.strippedOption;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + option.hashCode();
			result = prime * result + plugin.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OptionNode other = (OptionNode) obj;
			if (!option.equals(other.option))
				return false;
			if (!plugin.equals(other.plugin))
				return false;
			return true;
		}

		@Override
		public int compareTo(OptionNode o) {
			return option.compareTo(o.option);
		}
	}

	private final static class TraceTableContentProvider implements
			ITreeContentProvider {
		private final Map<PluginNode, Properties> myOptionsMap;

		public TraceTableContentProvider(Map<PluginNode, Properties> optionsMap) {
			this.myOptionsMap = optionsMap;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof Object[])
				return (Object[]) inputElement;
			return new Object[0];
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof PluginNode) {
				PluginNode node = (PluginNode) parentElement;
				List<OptionNode> result = new ArrayList<>();
				for (Object key : myOptionsMap.get(node).keySet()) {
					// hide the main switch
					if (key.equals(node.getPlugin() + MAINSWITCH))
						continue;
					result.add(new OptionNode(node, (String) key));
				}
				Collections.sort(result);
				return result.toArray();
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof OptionNode)
				return ((OptionNode) element).getPlugin();
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof PluginNode;
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Do nothing
		}

		@Override
		public void dispose() {
			// Do nothing
		}
	}

	private static final String MAINSWITCH = "/debug"; //$NON-NLS-1$

	private static final PluginNode[] PLUGIN_LIST = new PluginNode[] {
			new PluginNode(Activator.getPluginId()),
			new PluginNode(org.eclipse.egit.core.Activator.getPluginId()) };

	private static final int APPLY_ID = 77;

	private static final int DEFAULT_ID = 88;

	private final Map<PluginNode, Properties> optionsMap = new HashMap<>();

	private boolean isDirty;

	private Button platformSwitch;

	private Text traceFileLocation;

	private CheckboxTreeViewer tv;

	/**
	 * @param shell
	 */
	public GitTraceConfigurationDialog(Shell shell) {
		super(shell);
		setHelpAvailable(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));

		platformSwitch = new Button(main, SWT.CHECK);
		platformSwitch
				.setText(UIText.GitTraceConfigurationDialog_PlatformSwitchCheckbox);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(platformSwitch);
		platformSwitch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateEnablement();
			}
		});

		tv = new CheckboxTreeViewer(main, SWT.BORDER);
		tv.setContentProvider(new TraceTableContentProvider(optionsMap));

		Tree tree = tv.getTree();
		GridDataFactory.fillDefaults().span(3, 1).grab(true, true)
				.applyTo(tree);

		// enable testing with SWTBot
		tree.setData("org.eclipse.swtbot.widget.key", "LocationTree"); //$NON-NLS-1$ //$NON-NLS-2$

		TreeColumn c1 = new TreeColumn(tree, SWT.NONE);
		c1.setWidth(400);
		c1.setText(UIText.GitTraceConfigurationDialog_LocationHeader);
		tree.setHeaderVisible(true);

		Label fileLabel = new Label(main, SWT.NONE);
		fileLabel
				.setText(UIText.GitTraceConfigurationDialog_TraceFileLocationLabel);
		traceFileLocation = new Text(main, SWT.BORDER);
		traceFileLocation.setEditable(false);
		GridDataFactory.defaultsFor(traceFileLocation).grab(true, false)
				.applyTo(traceFileLocation);

		Button openButton = new Button(main, SWT.PUSH);
		openButton
				.setText(UIText.GitTraceConfigurationDialog_OpenInEditorButton);
		openButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IFileStore store = EFS.getLocalFileSystem().getStore(
						new Path(traceFileLocation.getText()));
				try {
					IDE.openEditor(PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage(),
							new FileStoreEditorInput(store),
							EditorsUI.DEFAULT_TEXT_EDITOR_ID);

				} catch (PartInitException ex) {
					Activator.handleError(ex.getMessage(), ex, true);
				}
			}
		});

		initValues();

		platformSwitch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
			}
		});

		tv.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				setDirty(true);
			}
		});

		Dialog.applyDialogFont(main);
		return main;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		createButton(parent, APPLY_ID,
				UIText.GitTraceConfigurationDialog_ApplyButton, false);
		createButton(parent, DEFAULT_ID,
				UIText.GitTraceConfigurationDialog_DefaultButton, false);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.GitTraceConfigurationDialog_ShellTitle);
	}

	@Override
	public void create() {
		super.create();
		updateApplyButton();
		setTitle(UIText.GitTraceConfigurationDialog_DialogTitle);
	}

	private void initValues() {
		DebugOptions options = getOptions();
		fillOptionsMapFromCurrent(options);
		tv.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isGrayed(Object element) {
				return false;
			}

			@Override
			public boolean isChecked(Object element) {
				Object data = element;
				Properties props;
				String key;
				if (data instanceof PluginNode) {
					PluginNode node = (PluginNode) data;
					props = optionsMap.get(node);
					key = node.getPlugin() + MAINSWITCH;
				} else if (data instanceof OptionNode) {
					OptionNode node = (OptionNode) data;
					props = optionsMap.get(node.getPlugin());
					key = node.getOption();
				} else {
					return false;
				}
				boolean active = Boolean.valueOf(props.getProperty(key))
						.booleanValue();
				return active;
			}
		});

		tv.setInput(PLUGIN_LIST);
		tv.expandAll();

		if (platformSwitch.getSelection() != options.isDebugEnabled()) {
			platformSwitch.setSelection(options.isDebugEnabled());
		}

		traceFileLocation.setText(getOptions().getFile().getPath());
		updateEnablement();
	}

	private void updateApplyButton() {
		if (getApplyButton() != null)
			getApplyButton().setEnabled(isDirty);
		if (getDefaultsButton() != null)
			getDefaultsButton().setEnabled(platformSwitch.getSelection());
	}

	private Button getApplyButton() {
		return getButton(APPLY_ID);
	}

	private Button getDefaultsButton() {
		return getButton(DEFAULT_ID);
	}

	private void updateEnablement() {
		setMessage(null);
		if (!platformSwitch.getSelection())
			setMessage(
					UIText.GitTraceConfigurationDialog_PlatformTraceDisabledMessage,
					IMessageProvider.INFORMATION);
		tv.getTree().setEnabled(platformSwitch.getSelection());
		updateApplyButton();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case IDialogConstants.OK_ID:
			performOk();
			break;
		case DEFAULT_ID:
			fillOptionsMapFromOptions();
			tv.refresh();
			break;
		case APPLY_ID:
			performOk();
			break;
		default:
			break;
		}
		super.buttonPressed(buttonId);
	}

	private void performOk() {
		DebugOptions options = getOptions();
		if (isDirty) {
			options.setDebugEnabled(platformSwitch.getSelection());
			if (platformSwitch.getSelection()) {
				// if this is off, we won't be able to save anything
				List<String> checkedKeys = new ArrayList<>();
				for (Object checked : Arrays.asList(tv.getCheckedElements())) {
					if (checked instanceof PluginNode)
						checkedKeys.add(((PluginNode) checked).getPlugin()
								+ MAINSWITCH);
					else if (checked instanceof OptionNode)
						checkedKeys.add(((OptionNode) checked).getOption());
				}

				for (Properties props : optionsMap.values()) {
					for (Object keyObject : props.keySet()) {
						String key = (String) keyObject;
						boolean isOn = options.getBooleanOption(key, false);
						boolean shouldBeOn = checkedKeys.contains(key);
						if (isOn != shouldBeOn) {
							options
									.setOption(key, Boolean
											.toString(shouldBeOn));
						}
					}
				}
			}
			fillOptionsMapFromCurrent(options);
			tv.refresh();
			setDirty(false);
		}
	}

	private void fillOptionsMapFromOptions() {
		Map<String, String> oldValues = new HashMap<>();
		for (Properties props : optionsMap.values())
			for (Object keyObject : props.keySet()) {
				String key = (String) keyObject;
				oldValues.put(key, props.getProperty(key));
			}

		optionsMap.clear();
		for (PluginNode plugin : PLUGIN_LIST) {
			Properties props = new Properties();
			try {
				URL resource = Platform.getBundle(plugin.getPlugin())
						.getResource(".options"); //$NON-NLS-1$
				if (resource != null) {
					try (InputStream is = resource.openStream()) {
						props.load(is);
					}
				}
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
			optionsMap.put(plugin, props);
		}

		Map<String, String> newValues = new HashMap<>();
		for (Properties props : optionsMap.values())
			for (Object keyObject : props.keySet()) {
				String key = (String) keyObject;
				oldValues.put(key, props.getProperty(key));
			}

		boolean dirty = false;
		if (oldValues.keySet().containsAll(newValues.keySet())
				&& newValues.keySet().containsAll(oldValues.keySet())) {
			for (Entry<String, String> oldValueEntry : oldValues.entrySet()) {
				String key = oldValueEntry.getKey();
				// don't in-line key here, as it would disquiet findbugs
				String newValue = newValues.get(key);
				if (!oldValueEntry.getValue().equals(newValue)) {
					dirty = true;
					break;
				}
			}
		} else {
			dirty = true;
		}
		if (dirty)
			setDirty(true);
	}

	private void fillOptionsMapFromCurrent(DebugOptions options) {
		optionsMap.clear();
		for (PluginNode plugin : PLUGIN_LIST) {
			Properties props = new Properties();
			try {
				URL resource = Platform.getBundle(plugin.getPlugin())
						.getResource(".options"); //$NON-NLS-1$
				if (resource != null) {
					try (InputStream is = resource.openStream()) {
						props.load(is);
					}
				}
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
			for (Object keyObject : props.keySet()) {
				String key = (String) keyObject;
				boolean isActive = options.getBooleanOption(key, false);
				props.setProperty(key, Boolean.toString(isActive));
			}
			optionsMap.put(plugin, props);
		}
	}

	private void setDirty(boolean dirty) {
		isDirty = dirty;
		updateApplyButton();
	}

	private DebugOptions getOptions() {
		return Activator.getDefault().getDebugOptions();
	}
}
