package org.eclipse.egit.ui.internal.clean;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;

/**
 * A Wizard that guides the user through cleaning the repository.
 */
public class CleanWizardDialog extends WizardDialog {

	/**
	 * Creates a new Clean Wizard instance
	 *
	 * @param parentShell
	 *            parent shell
	 * @param repository
	 */
	public CleanWizardDialog(Shell parentShell, Repository repository) {
		super(parentShell, new CleanWizard(repository));
	}

}
