package org.eclipse.egit.ui.internal.push;

import java.util.Set;

import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.components.RefContentProposal;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * A simple dialog to select from a list of {@link Ref}s
 */
public class BrowseRefDialog extends Dialog {
	private final class LabelProvider extends BaseLabelProvider implements
			ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			Ref actRef = (Ref) element;
			return new RefContentProposal(repo, actRef).getLabel();
		}
	}

	private final Set<Ref> refList;

	private final Repository repo;

	private Ref ref = null;

	TableViewer tv;

	/**
	 * @param parentShell
	 * @param repo
	 * @param refs
	 */
	protected BrowseRefDialog(Shell parentShell, Repository repo, Set<Ref> refs) {
		super(parentShell);
		this.repo = repo;
		this.refList = refs;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		main.setLayout(new GridLayout(1, false));
		tv = new TableViewer(main, SWT.SINGLE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tv.getTable());
		tv.setContentProvider(ArrayContentProvider.getInstance());
		tv.setLabelProvider(new LabelProvider());
		tv.setInput(this.refList);
		tv.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				okPressed();
			}
		});
		return main;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.BrowseRefDialog_SelectARefWindowTitle);
	}

	@Override
	protected void okPressed() {
		ref = (Ref) ((IStructuredSelection) tv.getSelection())
				.getFirstElement();
		super.okPressed();
	}

	/**
	 * @return the selected {@link Ref}
	 */
	public Ref getRef() {
		return ref;
	}
}
