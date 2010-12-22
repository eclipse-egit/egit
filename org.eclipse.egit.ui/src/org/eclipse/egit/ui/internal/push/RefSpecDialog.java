package org.eclipse.egit.ui.internal.push;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Add or edit a RefSpec
 */
public class RefSpecDialog extends TitleAreaDialog {

	private RefSpec spec;

	private Text sourceText;

	private Text destinationText;

	private Button forceButton;

	private Text specString;

	/**
	 * @param parentShell
	 */
	public RefSpecDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * @param parentShell
	 * @param spec
	 *            the {@link RefSpec} to edit
	 */
	public RefSpecDialog(Shell parentShell, RefSpec spec) {
		super(parentShell);
		this.spec = spec;
	}

	@Override
	protected void configureShell(Shell newShell) {
		// TODO Auto-generated method stub
		super.configureShell(newShell);
		newShell.setText("Create or Edit a RefSpec"); //$NON-NLS-1$ TODO
	}

	@Override
	public void create() {
		super.create();
		setTitle("Create or Edit a RefSpec"); //$NON-NLS-1$ TODO
		setMessage("A RefSpec maps Refs of one Repository to Refs of another Repository"); //$NON-NLS-1$ TODO
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		main.setLayout(new GridLayout(2, false));
		Label sourceLabel = new Label(main, SWT.NONE);
		sourceLabel.setText("Source:"); //$NON-NLS-1$ TODO
		sourceText = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sourceText);
		if (spec != null)
			sourceText.setText(spec.getSource());
		Label destinationLabel = new Label(main, SWT.NONE);
		destinationLabel.setText("Destination:"); //$NON-NLS-1$ TODO
		destinationText = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(
				destinationText);
		if (spec != null)
			destinationText.setText(spec.getDestination());

		forceButton = new Button(main, SWT.CHECK);
		forceButton.setText("Force update"); //$NON-NLS-1$ TODO
		GridDataFactory.fillDefaults().span(2, 1).applyTo(forceButton);
		if (spec != null)
			forceButton.setSelection(spec.isForceUpdate());

		Label stringLabel = new Label(main, SWT.NONE);
		stringLabel.setText("Specification:"); //$NON-NLS-1$ TODO
		specString = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(specString);
		if (spec != null)
			specString.setText(spec.toString());

		sourceText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateSource();
			}
		});
		destinationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateDestination();
			}
		});
		forceButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (getSpec().isForceUpdate() == forceButton.getSelection())
					return;
				setSpec(getSpec().setForceUpdate(forceButton.getSelection()));
			}

		});
		specString.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateFromString();
			}
		});
		return main;
	}

	private void updateSource() {
		setErrorMessage(null);
		try {
			RefSpec current = getSpec();
			if (sourceText.getText().length() > 0) {
				if (sourceText.getText().equals(current.getSource()))
					return;
				setSpec(current.setSource(sourceText.getText()));
			} else {
				if (current.getSource() != null
						&& current.getSource().length() > 0)
					setSpec(current.setSource("")); //$NON-NLS-1$
			}
		} catch (IllegalStateException ex) {
			setErrorMessage(ex.getMessage());
		}
	}

	private void updateDestination() {
		setErrorMessage(null);
		try {
			RefSpec current = getSpec();
			if (destinationText.getText().length() > 0) {
				if (destinationText.getText().equals(current.getDestination()))
					return;
				setSpec(current.setDestination(destinationText.getText()));
			} else {
				if (current.getDestination() != null
						&& current.getDestination().length() > 0)
					setSpec(current.setDestination("")); //$NON-NLS-1$
			}
		} catch (IllegalStateException ex) {
			setErrorMessage(ex.getMessage());
		}
	}

	private void updateFromString() {
		try {
			if (specString.getText().equals(getSpec().toString()))
				return;
			setSpec(new RefSpec(specString.getText()));
			setErrorMessage(null);
		} catch (IllegalArgumentException ex) {
			setErrorMessage(ex.getMessage());
		}
	}

	/**
	 * @return the spec
	 */
	public RefSpec getSpec() {
		if (this.spec == null)
			this.spec = new RefSpec();
		return this.spec;

	}

	private void setSpec(RefSpec spec) {
		this.spec = spec;
		String newSourceText = spec.getSource() != null ? spec.getSource() : ""; //$NON-NLS-1$
		String newDestinationText = spec.getDestination() != null ? spec
				.getDestination() : ""; //$NON-NLS-1$
		String newStringText = spec.toString();
		if (!sourceText.getText().equals(newSourceText)) {
			sourceText.setText(newSourceText);
		}
		if (!destinationText.getText().equals(newDestinationText)) {
			destinationText.setText(newDestinationText);
		}
		if (!specString.getText().equals(newStringText)) {
			specString.setText(newStringText);
		}
		forceButton.setSelection(spec.isForceUpdate());
	}
}
