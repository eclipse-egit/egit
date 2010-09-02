/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
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
public class GitTracePreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {
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
			return NLS.bind(UIText.GitTracePreferencePage_MainSwitchMessage,
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

		public int compareTo(OptionNode o) {
			return option.compareTo(o.option);
		}
	}

	private final static class TraceTableContentProvider extends
			ArrayContentProvider implements ITreeContentProvider {
		private final Map<PluginNode, Properties> myOptionsMap;

		public TraceTableContentProvider(Map<PluginNode, Properties> optionsMap) {
			this.myOptionsMap = optionsMap;
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof PluginNode) {
				PluginNode node = (PluginNode) parentElement;
				List<OptionNode> result = new ArrayList<OptionNode>();
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

		public Object getParent(Object element) {
			if (element instanceof OptionNode)
				return ((OptionNode) element).getPlugin();
			return null;
		}

		public boolean hasChildren(Object element) {
			return element instanceof PluginNode;
		}
	}

	private static final String MAINSWITCH = "/debug"; //$NON-NLS-1$

	private static final PluginNode[] PLUGIN_LIST = new PluginNode[] {
			new PluginNode(Activator.getPluginId()),
			new PluginNode(org.eclipse.egit.core.Activator.getPluginId()) };

	private final Map<PluginNode, Properties> optionsMap = new HashMap<PluginNode, Properties>();

	private boolean isDirty;

	private Button platformSwitch;

	private Text traceFileLocation;

	private CheckboxTreeViewer tv;

	/** */
	public GitTracePreferencePage() {
		// nothing to do
	}

	public void init(IWorkbench workbench) {
		// nothing
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));

		platformSwitch = new Button(main, SWT.CHECK);
		platformSwitch
				.setText(UIText.GitTracePreferencePage_PlatformTraceCheckbox);
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
		c1.setText(UIText.GitTracePreferencePage_LocationHeader);
		tree.setHeaderVisible(true);

		Label fileLabel = new Label(main, SWT.NONE);
		fileLabel.setText(UIText.GitTracePreferencePage_FileLocationLabel);
		traceFileLocation = new Text(main, SWT.BORDER);
		traceFileLocation.setEditable(false);
		GridDataFactory.defaultsFor(traceFileLocation).grab(true, false)
				.applyTo(traceFileLocation);

		Button openButton = new Button(main, SWT.PUSH);
		openButton.setText(UIText.GitTracePreferencePage_OpenButton);
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
			public void checkStateChanged(CheckStateChangedEvent event) {
				setDirty(true);
			}
		});

		Dialog.applyDialogFont(main);
		return main;
	}

	private void initValues() {
		DebugOptions options = getOptions();
		fillOptionsMapFromCurrent(options);
		tv.setCheckStateProvider(new ICheckStateProvider() {
			public boolean isGrayed(Object element) {
				return false;
			}

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

	@Override
	public void setVisible(boolean visible) {
		if (visible)
			updateApplyButton();
		super.setVisible(visible);
	}

	@Override
	protected void updateApplyButton() {
		if (getApplyButton() != null)
			getApplyButton().setEnabled(isDirty);
		if (getDefaultsButton() != null)
			getDefaultsButton().setEnabled(platformSwitch.getSelection());
	}

	private void updateEnablement() {
		setMessage(null);
		if (!platformSwitch.getSelection())
			setMessage(UIText.GitTracePreferencePage_TraceDisabledMessage,
					INFORMATION);
		tv.getTree().setEnabled(platformSwitch.getSelection());
		updateApplyButton();
	}

	@Override
	public boolean performOk() {
		DebugOptions options = getOptions();
		if (isDirty) {
			options.setDebugEnabled(platformSwitch.getSelection());
			if (platformSwitch.getSelection()) {
				// if this is off, we won't be able to save anything
				List<String> checkedKeys = new ArrayList<String>();
				for (Object checked : Arrays.asList(tv.getCheckedElements())) {
					if (checked instanceof PluginNode)
						checkedKeys.add(((PluginNode) checked).getPlugin()
								+ MAINSWITCH);
					else if (checked instanceof OptionNode)
						checkedKeys.add(((OptionNode) checked).getOption());
				}

				for (PluginNode plugin : optionsMap.keySet()) {
					Properties props = optionsMap.get(plugin);
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
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		// restore the currently active values
		fillOptionsMapFromOptions();
		tv.refresh();
		super.performDefaults();
	}

	private void fillOptionsMapFromOptions() {
		Map<String, String> oldValues = new HashMap<String, String>();
		for (Properties props : optionsMap.values())
			for (Object keyObject : props.keySet()) {
				String key = (String) keyObject;
				oldValues.put(key, props.getProperty(key));
			}

		optionsMap.clear();
		for (PluginNode plugin : PLUGIN_LIST) {
			Properties props = new Properties();
			try {
				InputStream is = Platform.getBundle(plugin.getPlugin())
						.getResource(".options").openStream(); //$NON-NLS-1$
				props.load(is);
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
			optionsMap.put(plugin, props);
		}

		Map<String, String> newValues = new HashMap<String, String>();
		for (Properties props : optionsMap.values())
			for (Object keyObject : props.keySet()) {
				String key = (String) keyObject;
				oldValues.put(key, props.getProperty(key));
			}

		boolean dirty = false;
		if (oldValues.keySet().containsAll(newValues.keySet())
				&& newValues.keySet().containsAll(oldValues.keySet())) {
			for (String key : oldValues.keySet()) {
				if (!oldValues.get(key).equals(newValues.get(key))) {
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
				InputStream is = Platform.getBundle(plugin.getPlugin())
						.getResource(".options").openStream(); //$NON-NLS-1$
				props.load(is);
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
