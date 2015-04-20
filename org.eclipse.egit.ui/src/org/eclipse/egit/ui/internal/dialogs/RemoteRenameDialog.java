package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.RenameRemoteOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Allows to rename a single remote
 */
public class RemoteRenameDialog extends TitleAreaDialog {
	private final Repository repository;

	private final RemoteNode remoteToRename;

	private Text name;

	/**
	 * @param parentShell
	 * @param repository
	 * @param remoteToRename
	 */
	public RemoteRenameDialog(Shell parentShell, Repository repository,
			RemoteNode remoteToRename) {
		super(parentShell);
		this.repository = repository;
		this.remoteToRename = remoteToRename;
		setHelpAvailable(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5)
				.applyTo(main);
		new Label(main, SWT.NONE)
				.setText(UIText.RemoteRenameDialog_NewNameLabel);
		name = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(name);
		return main;
	}

	@Override
	public void create() {
		super.create();
		setTitle(UIText.RemoteRenameDialog_Title);
		String oldName = remoteToRename.getObject();

		setMessage(NLS.bind(UIText.RemoteRenameDialog_Message, oldName));

		name.setText(oldName);
		name.setSelection(0, oldName.length());

		final IInputValidator inputValidator = ValidationUtils
				.getRemoteNameInputValidator(repository.getConfig(), true);
		name.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String error = inputValidator.isValid(name.getText());
				setErrorMessage(error);
				getButton(OK).setEnabled(error == null);
			}
		});

		getButton(OK).setEnabled(false);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.RemoteRenameDialog_WindowTitle);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK)
			try {
				String newName = name.getText();
				new RenameRemoteOperation(repository,
						remoteToRename.getObject(), newName).execute(null);
			} catch (CoreException e) {
				Activator.handleError(
						UIText.RemoteRenameDialog_RenameExceptionMessage, e,
						true);
				return;
			}
		super.buttonPressed(buttonId);
	}
}