package org.eclipse.egit.internal.mylyn.ui.tasks;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Manuel Doninger <manuel.doninger@googlemail.com>
 *
 */
public class QueryBranchMappingDialog extends TitleAreaDialog {

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public QueryBranchMappingDialog(Shell parent, int style) {
		super(parent);
		setTitle("Map a Branch to a Query"); //$NON-NLS-1$
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout(3, false));

		return composite;
	}
}
