package org.eclipse.egit.ui.internal.clean;

import org.eclipse.egit.core.op.CleanOperation;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Displays the result of a clean operation.
 */
public class CleanResultDialog extends TitleAreaDialog {

	private CleanOperation cleanOp;

	private static Image CLEAN_FOLDER_IMAGE = UIIcons.CLEAN.createImage();

	private static Image CLEAN_FILE_IMAGE = UIIcons.ELCL16_CLEAR.createImage();

	/**
	 * @param parentShell parent shell
	 * @param op the clean operation which has been run already
	 */
	public CleanResultDialog(Shell parentShell, CleanOperation op) {
		super(parentShell);
		this.cleanOp = op;
	}

	@Override
	public void create() {
		super.create();

		setTitle(UIText.CleanResultDialog_title);

		if(cleanOp.isDryRun()) {
			setMessage(UIText.CleanResultDialog_messagePretend);
		} else {
			setMessage(UIText.CleanResultDialog_messageResult);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		main.setLayout(new GridLayout(1, false));

		TableViewer tv = new TableViewer(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tv.getControl());

		tv.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				String elem = element.toString();

				if(elem.endsWith("/")) { //$NON-NLS-1$
					return CLEAN_FOLDER_IMAGE;
				} else {
					return CLEAN_FILE_IMAGE;
				}
			}
		});

		tv.setContentProvider(new ArrayContentProvider());
		tv.setInput(cleanOp.getResult());
		return main;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
}
