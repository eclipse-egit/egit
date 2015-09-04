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
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Helper class to facilitate the display of the preferred merge strategy and
 * the available merge strategies in both the preference page and the preferred
 * merge strategy dialog.
 */
public class PreferredMergedStrategyHelper {

	private final boolean showJGitStrategies;

	private RadioGroupFieldEditor preferredMergeStrategyEditor;

	private ScopedPreferenceStore corePreferenceStore;

	private MergeStrategy selectedStrategy;

	/**
	 * @param showJGitStrategies
	 */
	public PreferredMergedStrategyHelper(boolean showJGitStrategies) {
		this.showJGitStrategies = showJGitStrategies;
	}

	/**
	 * Create the panel to display the available merge strategies.
	 *
	 * @param parent
	 * @return The created field editor.
	 */
	public RadioGroupFieldEditor createPreferredStrategyPanel(Composite parent) {
		preferredMergeStrategyEditor = new RadioGroupFieldEditor(
				GitCorePreferences.core_preferredMergeStrategy,
				UIText.GitPreferenceRoot_preferreMergeStrategy_group, 1,
				getAvailableMergeStrategies(), parent);
		preferredMergeStrategyEditor.setPreferenceStore(getPreferenceStore());
		preferredMergeStrategyEditor.getLabelControl(parent)
				.setToolTipText(
						UIText.GitPreferenceRoot_preferreMergeStrategy_label);
		preferredMergeStrategyEditor
				.setPropertyChangeListener(new IPropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent event) {
						String strategyName = (String) event.getNewValue();
						selectedStrategy = MergeStrategy.get(strategyName);
					}
				});
		return preferredMergeStrategyEditor;
	}

	/**
	 * @return The selected strategy, <code>null</code> if the default JGit
	 *         implementation must be used.
	 */
	public MergeStrategy getSelectedStrategy() {
		return selectedStrategy;
	}

	/**
	 * @return The available merge strategies that will be displayed.
	 */
	protected String[][] getAvailableMergeStrategies() {
		org.eclipse.egit.core.Activator coreActivator = org.eclipse.egit.core.Activator
				.getDefault();
		List<String[]> strategies = new ArrayList<>();
		strategies.add(new String[] {
				UIText.GitPreferenceRoot_defaultMergeStrategyLabel, "" }); //$NON-NLS-1$
		if (showJGitStrategies) {
			strategies.add(new String[] {
					UIText.PreferredMergeStrategy_Ours_Label,
					MergeStrategy.OURS.getName() });
			strategies.add(new String[] {
					UIText.PreferredMergeStrategy_Theirs_Label,
					MergeStrategy.THEIRS.getName() });
			strategies.add(new String[] {
					UIText.PreferredMergeStrategy_SimpleTwoWayInCore_Label,
					MergeStrategy.SIMPLE_TWO_WAY_IN_CORE.getName() });
			strategies.add(new String[] {
					UIText.PreferredMergeStrategy_Resolve_Label,
					MergeStrategy.RESOLVE.getName() });
			strategies.add(new String[] {
					UIText.PreferredMergeStrategy_Recursive_Label,
					MergeStrategy.RECURSIVE.getName() });
		}
		for (MergeStrategyDescriptor strategy : coreActivator
				.getRegisteredMergeStrategies()) {
			strategies.add(new String[] { strategy.getLabel(),
					strategy.getName() });
		}
		return strategies.toArray(new String[strategies.size()][2]);
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
	 * Store the preferred merge strategy in the dedicated preference store.
	 * Note that this does NOT save the store. Must be called after
	 * createPreferreStrategyPanel().
	 */
	public void store() {
		preferredMergeStrategyEditor.store();
	}

	/**
	 * Load the preferred merge strategy. Must be called after
	 * createPreferreStrategyPanel().
	 */
	public void load() {
		preferredMergeStrategyEditor.load();
	}

	/**
	 * The the preference store used for the preferred merge strategy.
	 */
	public void save() {
		try {
			getPreferenceStore().save();
		} catch (IOException e) {
			String message = JFaceResources.format(
					"PreferenceDialog.saveErrorMessage", new Object[] { //$NON-NLS-1$
					UIText.PreferredStrategyDialog_Title, e.getMessage() });
			Policy.getStatusHandler()
					.show(new Status(IStatus.ERROR, Policy.JFACE, message, e),
							JFaceResources
									.getString("PreferenceDialog.saveErrorTitle")); //$NON-NLS-1$

		}
	}
}
