package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.preferences.PreferredMergedStrategyHelper;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Policy;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Dialog for choosing the preferred merge strategy.
 */
public class PreferredStrategyDialog extends TitleAreaDialog {

	private final PreferredMergedStrategyHelper helper;

	private BooleanFieldEditor dontAskAgainCheckBox;

	/**
	 * @param parentShell
	 */
	public PreferredStrategyDialog(Shell parentShell) {
		super(parentShell);
		helper = new PreferredMergedStrategyHelper(true);
		setBlockOnOpen(true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.PreferredStrategyDialog_ShellTitle);
	}

	@Override
	public void create() {
		super.create();
		setTitle(UIText.PreferredStrategyDialog_Title);
		setMessage(UIText.PreferredStrategyDialog_Message);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertVerticalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		main.setLayout(layout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		Label explanation = new Label(main, SWT.HORIZONTAL);
		explanation.setText(UIText.PreferredStrategyDialog_Explanation);

		Composite comp = new Composite(main, SWT.NONE);
		helper.createPreferredStrategyPanel(comp);
		helper.load();

		Label sep = new Label(main, SWT.HORIZONTAL);
		sep.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		comp = new Composite(main, SWT.NONE);
		dontAskAgainCheckBox = new BooleanFieldEditor(
				UIPreferences.PREFERRED_MERGE_STRATEGY_HIDE_DIALOG,
				UIText.PreferredStrategyDialog_DontAskAgain, comp);
		dontAskAgainCheckBox.load();

		Label separator = new Label(main, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		applyDialogFont(main);
		return main;
	}

	@Override
	protected void okPressed() {
		dontAskAgainCheckBox.store();
		save(getUIPreferenceStore());
		if (dontAskAgainCheckBox.getBooleanValue()) {
			// Only change the preferred strategy preference if the checkbox has
			// been checked
			helper.store();
			helper.save();
		}
		super.okPressed();
	}

	/**
	 * @return The merge strategy to use, <code>null</code> if JGit default must
	 *         be used.
	 */
	public MergeStrategy getSelectedStrategy() {
		return helper.getSelectedStrategy();
	}

	private void save(ScopedPreferenceStore store) {
		if (store.needsSaving()) {
			try {
				store.save();
			} catch (IOException e) {
				String message = JFaceResources.format(
						"PreferenceDialog.saveErrorMessage", new Object[] { //$NON-NLS-1$
						UIText.PreferredStrategyDialog_Title,
										e.getMessage() });
				Policy.getStatusHandler().show(
						new Status(IStatus.ERROR, Policy.JFACE, message, e),
						JFaceResources
								.getString("PreferenceDialog.saveErrorTitle")); //$NON-NLS-1$

			}
		}
	}

	private ScopedPreferenceStore getUIPreferenceStore() {
		return new ScopedPreferenceStore(InstanceScope.INSTANCE,
				Activator.getPluginId());
	}
}
