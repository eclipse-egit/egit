/*******************************************************************************
 * Copyright (C) 2016 Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator.MergeStrategyDescriptor;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Policy;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.IWorkbenchGraphicConstants;
import org.eclipse.ui.internal.WorkbenchImages;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Helper class to facilitate the display of the preferred merge strategy and
 * the available merge strategies in both the preference page and the preferred
 * merge strategy dialog.
 */
@SuppressWarnings("restriction")
public class MergeStrategyHelper {

	// We need to add a default name for EGit default strategy in order to
	// be able to distinguish a state where users haven't chosen any
	// preference (in this case, values defined in eclipse configuration
	// files may apply) or if they have chosen the default merge strategy.
	private static final MergeStrategyInfo JGIT_DEFAULT = new MergeStrategyInfo(
			GitCorePreferences.core_preferredMergeStrategy_Default,
			UIText.GitPreferenceRoot_defaultMergeStrategyLabel,
			"org.eclipse.jgit", ""); //$NON-NLS-1$ //$NON-NLS-2$

	private ScopedPreferenceStore corePreferenceStore;

	private List<MergeStrategyInfo> availableStrategies;

	private MergeStrategyInfo selectedStrategy;

	private List<Button> strategyButtons;

	private Button cbJgitDefault;

	private final boolean showDefaultCheckbox;

	private Composite strategyListComp;

	/**
	 * @param showDefaultCheckbox
	 *            Whether to show a checkbox that wraps the strategy list.
	 */
	public MergeStrategyHelper(boolean showDefaultCheckbox) {
		this.showDefaultCheckbox = showDefaultCheckbox;
	}

	/**
	 * Create the panel to display the available merge strategies.
	 *
	 * @param parent
	 * @return The composite that contains the table of values
	 */
	public Control createContents(Composite parent) {
		if (showDefaultCheckbox) {
			cbJgitDefault = new Button(parent, SWT.CHECK);
			cbJgitDefault.setAlignment(SWT.LEFT);
			cbJgitDefault
					.setText(UIText.MergeStrategyHelper_UseSpecificStrategy_Text);
			cbJgitDefault
					.setToolTipText(UIText.MergeStrategyHelper_UseSpecificStrategy_Tooltip);
			cbJgitDefault.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					if (((Button) event.widget).getSelection()) {
						selectedStrategy = getAvailableMergeStrategies().get(0);
						updateStrategy(selectedStrategy.name);
					} else {
						selectedStrategy = JGIT_DEFAULT;
						updateStrategy(GitCorePreferences.core_preferredMergeStrategy_Default);
					}
				}
			});
		}

		strategyListComp = new Composite(parent, SWT.NONE);

		strategyListComp.setLayout(new GridLayout(4, false));

		Label lblShortName = new Label(strategyListComp, SWT.NONE);
		lblShortName.setAlignment(SWT.CENTER);
		lblShortName.setFont(JFaceResources.getBannerFont());
		lblShortName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false,
				false, 1, 1));
		lblShortName
				.setText(UIText.MergeStrategyHelper_StrategyShortNameHeader);

		Label lblDescription = new Label(strategyListComp, SWT.NONE);
		lblDescription.setAlignment(SWT.CENTER);
		lblDescription.setFont(JFaceResources.getBannerFont());
		lblDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false,
				false, 1, 1));
		lblDescription
				.setText(UIText.MergeStrategyHelper_StrategyDescriptionHeader);

		Label lblProvider = new Label(strategyListComp, SWT.NONE);
		lblProvider.setAlignment(SWT.CENTER);
		lblProvider.setFont(JFaceResources.getBannerFont());
		lblProvider.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false,
				false, 1, 1));
		lblProvider.setText(UIText.MergeStrategyHelper_StrategyProviderHeader);

		Label lblHelp = new Label(strategyListComp, SWT.NONE);
		lblHelp.setAlignment(SWT.CENTER);
		lblHelp.setFont(JFaceResources.getBannerFont());
		lblHelp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1,
				1));
		lblHelp.setText(UIText.MergeStrategyHelper_StrategyHelpHeader);

		strategyButtons = new ArrayList<>();
		for (MergeStrategyInfo info : getAvailableMergeStrategies()) {
			addMergeStrategyRow(info);
		}
		return strategyListComp;
	}

	private void addMergeStrategyRow(MergeStrategyInfo info) {
		Button btnStrategy = new Button(strategyListComp, SWT.RADIO);
		GridData gd_btnStrategy = new GridData(SWT.LEFT, SWT.CENTER, false,
				false, 1, 1);
		gd_btnStrategy.widthHint = 150;
		btnStrategy.setLayoutData(gd_btnStrategy);
		btnStrategy.setText(info.name);
		btnStrategy.setData(info);
		btnStrategy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				selectedStrategy = (MergeStrategyInfo) event.widget
						.getData();
			}
		});
		strategyButtons.add(btnStrategy);
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(btnStrategy, info.helpContextId);

		Label lblDesc = new Label(strategyListComp, SWT.WRAP);
		GridData gd_lblDesc = new GridData(SWT.LEFT, SWT.CENTER, false,
				false, 1, 1);
		gd_lblDesc.widthHint = 200;
		lblDesc.setLayoutData(gd_lblDesc);
		lblDesc.setText(info.description);

		Label lblProvidedBy = new Label(strategyListComp, SWT.WRAP);
		GridData gd_lblProvidedBy = new GridData(SWT.LEFT, SWT.CENTER,
				false, false, 1, 1);
		gd_lblProvidedBy.widthHint = 200;
		lblDesc.setLayoutData(gd_lblProvidedBy);
		lblProvidedBy.setText(info.providedBy);

		Label helpBtn = new Label(strategyListComp, SWT.NONE);
		GridData gd_helpBtn = new GridData(SWT.CENTER, SWT.CENTER, false,
				false, 1, 1);
		gd_helpBtn.heightHint = 16;
		gd_helpBtn.widthHint = 16;
		helpBtn.setLayoutData(gd_helpBtn);
		helpBtn.setImage(WorkbenchImages
				.getImage(IWorkbenchGraphicConstants.IMG_ETOOL_HELP_CONTENTS));
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(helpBtn, info.helpContextId);
	}

	/**
	 * @return The selected strategy, <code>null</code> if the default JGit
	 *         implementation must be used.
	 */
	public MergeStrategy getSelectedStrategy() {
		if (selectedStrategy == JGIT_DEFAULT || selectedStrategy == null) {
			return null;
		}
		return MergeStrategy.get(selectedStrategy.name);
	}

	/**
	 * @return The available merge strategies that will be displayed.
	 */
	protected List<MergeStrategyInfo> getAvailableMergeStrategies() {
		if (availableStrategies == null) {
			availableStrategies = loadAvailableStrategies();
		}
		return availableStrategies;
	}

	/**
	 * Load the available merge strategies from the extension registry.
	 *
	 * @return The list of registered merge strategies.
	 */
	protected List<MergeStrategyInfo> loadAvailableStrategies() {
		org.eclipse.egit.core.Activator coreActivator = org.eclipse.egit.core.Activator
				.getDefault();
		List<MergeStrategyInfo> strategies = new ArrayList<>();
		// Start with recursive since it's JGit default
		strategies.add(new MergeStrategyInfo(MergeStrategy.RECURSIVE.getName(),
						UIText.MergeStrategy_Recursive_Label,
						"org.eclipse.jgit", "org.eclipse.egit.ui.mergeStrategy_recursive")); //$NON-NLS-1$ //$NON-NLS-2$
		strategies.add(new MergeStrategyInfo(MergeStrategy.OURS.getName(),
				UIText.MergeStrategy_Ours_Label,
				"org.eclipse.jgit", "org.eclipse.egit.ui.mergeStrategy_ours")); //$NON-NLS-1$ //$NON-NLS-2$
		strategies.add(new MergeStrategyInfo(MergeStrategy.THEIRS.getName(),
						UIText.MergeStrategy_Theirs_Label,
						"org.eclipse.jgit", "org.eclipse.egit.ui.mergeStrategy_theirs")); //$NON-NLS-1$ //$NON-NLS-2$
		strategies.add(new MergeStrategyInfo(
				MergeStrategy.SIMPLE_TWO_WAY_IN_CORE.getName(),
				UIText.MergeStrategy_SimpleTwoWayInCore_Label,
						"org.eclipse.jgit", "org.eclipse.egit.ui.mergeStrategy_simple2wayInCore")); //$NON-NLS-1$ //$NON-NLS-2$
		strategies.add(new MergeStrategyInfo(MergeStrategy.RESOLVE.getName(),
						UIText.MergeStrategy_Resolve_Label,
						"org.eclipse.jgit", "org.eclipse.egit.ui.mergeStrategy_resolve")); //$NON-NLS-1$ //$NON-NLS-2$
		for (MergeStrategyDescriptor strategy : coreActivator
				.getRegisteredMergeStrategies()) {
			strategies.add(new MergeStrategyInfo(strategy.getName(), strategy
					.getLabel(), strategy.getContributorId(), strategy
					.getHelpContextId()));
		}
		return strategies;
	}

	/**
	 * @return The core preference store.
	 */
	protected ScopedPreferenceStore getPreferenceStore() {
		if (corePreferenceStore == null) {
			corePreferenceStore = new ScopedPreferenceStore(
					InstanceScope.INSTANCE,
					org.eclipse.egit.core.Activator.getPluginId());
		}
		return corePreferenceStore;
	}

	/**
	 * Updates the UI (check the proper radio button of checkbox) for the given
	 * strategy.
	 *
	 * @param strategy
	 */
	private void updateStrategy(String strategy) {
		if (GitCorePreferences.core_preferredMergeStrategy_Default
				.equals(strategy)) {
			setStrategyListEnabled(false);
			for (Button btn : strategyButtons) {
				btn.setSelection(false);
			}
		} else {
			setStrategyListEnabled(true);
			if (strategy != null) {
				for (Button btn : strategyButtons) {
					if (strategy.equals(btn.getText())) {
						btn.setSelection(true);
						break;
					}
				}
			}
		}
	}

	private void setStrategyListEnabled(boolean value) {
		if (showDefaultCheckbox) {
			cbJgitDefault.setSelection(value);
			strategyListComp.setEnabled(value);
			for (Control ctl : strategyListComp.getChildren()) {
				ctl.setEnabled(value);
			}
		}
	}

	/**
	 * Store the preferred merge strategy in the dedicated preference store.
	 * Note that this does NOT save the store. Must be called after
	 * createPreferreStrategyPanel().
	 */
	public void store() {
		getPreferenceStore().setValue(
				GitCorePreferences.core_preferredMergeStrategy,
				selectedStrategy.name);
	}

	/**
	 * Load the preferred merge strategy. Must be called after createContents().
	 */
	public void load() {
		String strategy = getPreferenceStore().getString(
				GitCorePreferences.core_preferredMergeStrategy);
		MergeStrategyInfo selectedInfo = null;
		if (GitCorePreferences.core_preferredMergeStrategy_Default
				.equals(strategy)) {
			selectedInfo = JGIT_DEFAULT;
		} else {
			for (MergeStrategyInfo strategyInfo : getAvailableMergeStrategies()) {
				if (strategy.equals(strategyInfo.name)) {
					selectedInfo = strategyInfo;
					break;
				}
			}
			if (selectedInfo == null
					&& !getAvailableMergeStrategies().isEmpty()) {
				// The strategy wasn't found, we select the 1st one
				selectedInfo = getAvailableMergeStrategies().get(0);
			}
		}
		selectedStrategy = selectedInfo;
		updateStrategy(selectedInfo == null ? null : selectedInfo.name);
	}

	/**
	 * The preference store used for the preferred merge strategy.
	 */
	public void save() {
		try {
			getPreferenceStore().save();
		} catch (IOException e) {
			String message = JFaceResources.format(
					"PreferenceDialog.saveErrorMessage", new Object[] { //$NON-NLS-1$
					UIText.MergeStrategyDialog_Title, e.getMessage() });
			Policy.getStatusHandler()
					.show(new Status(IStatus.ERROR, Policy.JFACE, message, e),
							JFaceResources
									.getString("PreferenceDialog.saveErrorTitle")); //$NON-NLS-1$

		}
	}

	private static final class MergeStrategyInfo {

		private final String name;

		private final String description;

		private final String providedBy;

		private final String helpContextId;

		public MergeStrategyInfo(String name, String description,
				String providedBy, String helpContextId) {
			this.name = name;
			this.description = description;
			this.providedBy = providedBy;
			this.helpContextId = helpContextId;
		}
	}
}
