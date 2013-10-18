package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Creates a {@link WizardDialog} for the project import wizard.
 *
 *
 * @author Johan Wannheden
 */
public class ImportProjectsWizardDialog extends WizardDialog {

	/**
	 * Creates a new instance of the import projects dialog.
	 *
	 * @param parentShell
	 * @param newWizard
	 */
	public ImportProjectsWizardDialog(Shell parentShell, IWizard newWizard) {
		super(parentShell, newWizard);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return Activator.getDefault().getDialogSettings();
	}

}
