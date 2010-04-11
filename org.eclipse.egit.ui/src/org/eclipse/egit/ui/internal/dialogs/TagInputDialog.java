/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * The new tag name dialog
 *
 */
public class TagInputDialog extends InputDialog {

	private Text message;

	private String messageValue;

	/**
	 * Construct a dialog to insert a new tag name and message.
	 *
	 * @param repo
	 * @param shell
	 * @param message
	 */
	public TagInputDialog(final Repository repo, final Shell shell,
			final String message) {
		super(shell, UIText.TagInputDialog_QuestionNewTagTitle, message, null,
				new IInputValidator() { // input validator taken from
					// BranchSelectionDialog.getRefNameInputDialog()
					public String isValid(final String newText) {
						final String testFor = Constants.R_HEADS + newText;
						try {
							if (repo.resolve(testFor) != null)
								return UIText.TagInputDialog_ErrorAlreadyExists;
						} catch (final IOException e1) {
							Activator.logError(NLS.bind(
									UIText.TagInputDialog_ErrorCouldNotResolve,
									testFor), e1);
							return e1.getMessage();
						}
						if (!Repository.isValidRefName(testFor))
							return UIText.TagInputDialog_ErrorInvalidRefName;
						return null;
					}
				});
	}

	/**
	 * Returns the string typed into message field of this dialog.
	 *
	 * @return message
	 */
	public String getMessage() {
		return messageValue;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);

		new Label(composite, SWT.NONE)
				.setText(UIText.TagInputDialog_TagMessage);

		message = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		message.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).grab(
				true, true).hint(400, 200).create());

		// key listener taken from CommitDialog.createDialogArea()
		// allow to commit with ctrl-enter
		message.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent arg0) {
				if (arg0.keyCode == SWT.CR
						&& (arg0.stateMask & SWT.CONTROL) > 0) {
					okPressed();
				} else if (arg0.keyCode == SWT.TAB
						&& (arg0.stateMask & SWT.SHIFT) == 0) {
					arg0.doit = false;
					message.traverse(SWT.TRAVERSE_TAB_NEXT);
				}
			}
		});

		applyDialogFont(composite);
		return composite;
	}

	@Override
	protected void okPressed() {
		messageValue = message.getText();

		if (messageValue.trim().length() == 0) {
			MessageDialog.openWarning(getShell(),
					UIText.TagInputDialog_ErrorNoMessage,
					UIText.TagInputDialog_ErrorMustEnterCommitMessage);
			return;
		}

		super.okPressed();
	}

}
