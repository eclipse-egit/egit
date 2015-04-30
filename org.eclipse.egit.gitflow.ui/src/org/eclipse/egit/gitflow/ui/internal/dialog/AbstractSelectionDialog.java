package org.eclipse.egit.gitflow.ui.internal.dialog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.GitLabelProvider;
import static org.eclipse.jface.dialogs.IDialogConstants.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Select Git Flow branches.
 *
 * @param <T>
 */
@SuppressWarnings("restriction")
public abstract class AbstractSelectionDialog<T> extends MessageDialog {

	private final List<T> nodes;

	private TableViewer branchesList;

	private List<T> selected = new ArrayList<T>();

	/**
	 * @param parentShell
	 * @param nodes
	 * @param title
	 * @param message
	 */
	public AbstractSelectionDialog(Shell parentShell, List<T> nodes,
			String title, String message) {
		super(parentShell, title, null, message, MessageDialog.QUESTION,
				new String[] { OK_LABEL, CANCEL_LABEL }, 0);
		this.nodes = nodes;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1)
				.applyTo(area);
		area.setLayout(new GridLayout(1, false));
		branchesList = new TableViewer(area, SWT.SINGLE | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(branchesList.getControl());
		branchesList.setContentProvider(ArrayContentProvider.getInstance());
		branchesList.setLabelProvider(createLabelProvider());
		branchesList.setComparator(new ViewerComparator(
				CommonUtils.STRING_ASCENDING_COMPARATOR));
		branchesList.setInput(nodes);
		branchesList
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						checkPage();
					}
				});
		branchesList.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				buttonPressed(OK);
			}
		});

		branchesList.addFilter(createFilter());
		return area;
	}

	private ViewerFilter createFilter() {
		return new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				return true;
			}
		};
	}

	private IBaseLabelProvider createLabelProvider() {
		final String prefix = getPrefix();
		return new GitLabelProvider() {
			@Override
			public String getText(Object element) {
				return super.getText(element).substring(prefix.length());
			}
		};
	}

	/**
	 * @return Git Flow prefix for this dialog. E.g. feature, release, hotfix
	 */
	abstract protected String getPrefix();

	private void checkPage() {
		getButton(OK).setEnabled(!branchesList.getSelection().isEmpty());

	}

	@SuppressWarnings("unchecked")
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			selected = ((IStructuredSelection) branchesList.getSelection())
					.toList();
		}
		super.buttonPressed(buttonId);
	}

	@Override
	public void create() {
		super.create();
		getButton(OK).setEnabled(false);
	}

	/**
	 * @return the selected entry (single mode)
	 */
	public T getSelectedNode() {
		if (selected.isEmpty())
			return null;
		return selected.get(0);
	}
}
