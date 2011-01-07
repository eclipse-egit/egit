/**
 *
 */
package org.eclipse.egit.ui.internal.history.command;

import java.util.List;

import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

abstract class AmbiguosBranchDialog extends MessageDialog {
	static final class BranchLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			RefNode refNode = (RefNode) element;
			return refNode.getObject().getName();
		}

		@Override
		public Image getImage(Object element) {
			return RepositoryTreeNodeType.REF.getIcon();
		}
	}

	private final List<RefNode> nodes;

	TableViewer branchesList;

	RefNode selected;

	AmbiguosBranchDialog(Shell parentShell, List<RefNode> nodes, String title, String message) {
		super(parentShell, title, null,
				message,
				MessageDialog.QUESTION, new String[] {
						IDialogConstants.OK_LABEL,
						IDialogConstants.CANCEL_LABEL }, 0);
		this.nodes = nodes;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayoutData(new GridData(GridData.FILL_BOTH));
		area.setLayout(new FillLayout());

		branchesList = new TableViewer(area, SWT.SINGLE | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER);
		branchesList.setContentProvider(ArrayContentProvider.getInstance());
		branchesList.setLabelProvider(new BranchLabelProvider());
		branchesList.setInput(nodes);
		branchesList
				.addSelectionChangedListener(new ISelectionChangedListener() {

					public void selectionChanged(SelectionChangedEvent event) {
						getButton(OK).setEnabled(
								!event.getSelection().isEmpty());
					}
				});
		return area;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK)
			selected = (RefNode) ((IStructuredSelection) branchesList
					.getSelection()).getFirstElement();
		super.buttonPressed(buttonId);
	}

	@Override
	public void create() {
		super.create();
		getButton(OK).setEnabled(false);
	}

	public RefNode getSelectedNode() {
		return selected;
	}
}