package org.eclipse.egit.gitflow.ui.internal.actions;

import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

class StartDialog extends InputDialog {

	private Button startButton;

	StartDialog(Shell parentShell, String dialogTitle,
			String dialogMessage, String initialValue,
			IInputValidator validator) {
		super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		startButton = createButton(parent, IDialogConstants.OK_ID,
				UIText.StartDialog_ButtonOK, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);

		getText().setFocus();
		if (getValue() != null) {
			getText().setText(getValue());
			getText().selectAll();
		}
	}

	@Override
	protected Button getOkButton() {
		return startButton;
	}
}
