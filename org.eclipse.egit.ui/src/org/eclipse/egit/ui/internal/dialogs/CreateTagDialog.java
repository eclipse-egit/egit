/*******************************************************************************
 * Copyright (C) 2010, Darusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Tag;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Dialog for creating and editing tags.
 *
 */
public class CreateTagDialog extends InputDialog {

	private Composite detailsComposite;

	private String message;

	private ObjectId commit;

	private boolean overwriteTag;

	private boolean showDetails = false;

	private RevWalk revCommits;

	private List<Tag> existingTags;

	private Tag tag;

	private Text messageText;

	private Button overwriteButton;

	private Button detailsButton;

	private TableViewer tagViewer;

	private CommitCombo commitCombo;

	class TagInputList extends LabelProvider implements IWorkbenchAdapter,
			IAdaptable {

		private final List<Tag> tagList;

		public TagInputList(List<Tag> tagList) {
			this.tagList = tagList;
		}

		public Object[] getChildren(Object o) {
			return tagList.toArray(new Object[] {});
		}

		public ImageDescriptor getImageDescriptor(Object object) {
			return null;
		}

		public String getLabel(Object o) {
			if (o instanceof Tag)
				return ((Tag) o).getTag();

			return null;
		}

		public Object getParent(Object o) {
			return null;
		}

		public Object getAdapter(Class adapter) {
			if (adapter == IWorkbenchAdapter.class)
				return this;

			return null;
		}
	}

	class TagLabelProvider extends WorkbenchLabelProvider implements
			ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			return ((Tag) element).getTag();
		}

	}

	/**
	 * Construct dialog to creating or editing tag.
	 *
	 * @param parent
	 * @param validator
	 */
	public CreateTagDialog(Shell parent, IInputValidator validator) {
		super(parent, UIText.CreateTagDialog_questionNewTagTitle,
				UIText.CreateTagDialog_questionNewTagMessage, null, validator);
		setBlockOnOpen(true);
	}

	/**
	 * Returns {@link ObjectId} of commit with new or edited tag should be
	 * associated with
	 *
	 * @return {@link ObjectId} of commit with new or edited tag should be
	 *         associated with
	 */
	public ObjectId getTagCommit() {
		return commit;
	}

	/**
	 * Returns message for created or edited tag.
	 *
	 * @return message for created or edited tag.
	 */
	public String getTagMessage() {
		return message;
	}

	/**
	 * Returns name of new tag.
	 *
	 * @return name of new tag
	 */
	public String getTagName() {
		return getValue();
	}

	/**
	 * Indicates does tag should be forced to update (overwritten) or created.
	 *
	 * @return <code>true</code> if tag should be forced to update,
	 *         <code>false</code> if tag should be created
	 */
	public boolean shouldOverWriteTag() {
		return overwriteTag;
	}

	/**
	 * Sets list of already existing tags. This list will be loaded in
	 * <code>Details</code> section of this dialog.
	 *
	 * @param existingTags
	 */
	public void setExistingTags(List<Tag> existingTags) {
		this.existingTags = existingTags;
	}

	/**
	 * Sets list of existing commits. This list will be loaded in
	 * {@link CommitCombo} widget in <code>Advanced</code> section of this
	 * dialog.
	 *
	 * @param revCommits
	 */
	public void setRevCommitList(RevWalk revCommits) {
		this.revCommits = revCommits;
	}

	/**
	 * Data from <code>tag</code> argument will be set in this dialog box.
	 *
	 * @param tag
	 */
	public void setTag(Tag tag) {
		this.tag = tag;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		initializeDialogUnits(parent);

		final Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(GridLayoutFactory.swtDefaults().create());

		new Label(composite, SWT.NONE)
				.setText(UIText.CreateTagDialog_tagMessage);

		messageText = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		messageText.setLayoutData(GridDataFactory.fillDefaults().span(2, 1)
				.grab(true, true).hint(400, 100).create());

		// key listener taken from CommitDialog.createDialogArea() allow to
		// commit with ctrl-enter
		messageText.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent arg0) {
				if (arg0.keyCode == SWT.CR
						&& (arg0.stateMask & SWT.CONTROL) > 0) {
					Control button = getButton(IDialogConstants.OK_ID);
					// fire OK action only when button is enabled
					if (null != button && button.isEnabled())
						buttonPressed(IDialogConstants.OK_ID);
				} else if (arg0.keyCode == SWT.TAB
						&& (arg0.stateMask & SWT.SHIFT) == 0) {
					arg0.doit = false;
					messageText.traverse(SWT.TRAVERSE_TAB_NEXT);
				}
				validateInput();
			}
		});

		overwriteButton = new Button(composite, SWT.CHECK);
		overwriteButton.setText(UIText.CreateTagDialog_overwriteTag);
		overwriteButton
				.setToolTipText(UIText.CreateTagDialog_overwriteTagToolTip);
		overwriteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (overwriteButton.getSelection()) {
					Control button = getButton(IDialogConstants.OK_ID);
					button.setEnabled(true);
				} else {
					validateInput();
				}
			}
		});

		createAdvancedSection(composite);

		if (null != tag) {
			setTagImpl();
		}

		validateInput();
		applyDialogFont(parent);
		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		detailsButton = createButton(parent, IDialogConstants.DETAILS_ID,
				IDialogConstants.SHOW_DETAILS_LABEL, false);
		detailsButton.setToolTipText(UIText.CreateTagDialog_detailsButtonToolTip);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);

		// do this here because setting the text will set enablement on the OK
		// button
		Text text = getText();
		text.setFocus();
		if (getValue() != null) {
			text.setText(getValue());
			text.selectAll();
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case IDialogConstants.DETAILS_ID:
			if (showDetails) {
				detailsComposite.dispose();
				detailsButton.setText(IDialogConstants.SHOW_DETAILS_LABEL);
			} else {
				detailsComposite = createDetailsComposite();
				detailsButton.setText(IDialogConstants.HIDE_DETAILS_LABEL);
			}

			getContents().getShell().pack(); // update dialog size
			showDetails = !showDetails;
			break;
		case IDialogConstants.OK_ID:
			// read and store data from widgets
			commit = commitCombo.getValue();
			message = messageText.getText();
			overwriteTag = overwriteButton.getSelection();
			//$FALL-THROUGH$ continue propagating OK button action
		default:
			super.buttonPressed(buttonId);
		}
	}

	@Override
	protected void validateInput() {
		// don't validate if dialog is disposed
		if (null == getShell()) {
			return;
		}

		// validate tag message
		Control button = getButton(IDialogConstants.OK_ID);
		if (null != button) {
			boolean enabled = null != messageText
					&& 0 != messageText.getText().trim().length();
			button.setEnabled(enabled);
		}

		// validate tag name
		super.validateInput();
	}

	private void createAdvancedSection(final Composite composite) {
		ExpandableComposite advanced = new ExpandableComposite(composite,
				ExpandableComposite.TREE_NODE | ExpandableComposite.CLIENT_INDENT);

		advanced.setText(UIText.CreateTagDialog_advanced);
		advanced.setToolTipText(UIText.CreateTagDialog_advancedToolTip);
		advanced.setLayoutData(GridDataFactory.swtDefaults().create());

		Composite advancedComposite = new Composite(advanced, SWT.WRAP);
		advancedComposite.setLayout(GridLayoutFactory.swtDefaults().create());

		Label advancedLabel = new Label(advancedComposite, SWT.WRAP);
		advancedLabel.setText(UIText.CreateTagDialog_advancedMessage);
		advancedLabel.setLayoutData(GridDataFactory.fillDefaults().span(2, 1)
				.grab(true, true).hint(450, SWT.DEFAULT).create());

		commitCombo = new CommitCombo(advancedComposite, SWT.NORMAL);

		for (RevCommit revCommit : revCommits)
			commitCombo.add(revCommit);

		advanced.setClient(advancedComposite);
		advanced.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				composite.getShell().pack(); // update dialog size
			}
		});
	}

	private Composite createDetailsComposite() {
		Composite parent = (Composite) getContents();

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(GridLayoutFactory.swtDefaults().create());
		composite.setLayoutData(GridDataFactory.fillDefaults().create());

		Table table = new Table(composite, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.FILL);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(100));
		table.setLayout(layout);
		table.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).grab(
				true, true).hint(400, 100).create());

		new TableColumn(table, SWT.BORDER | SWT.V_SCROLL);

		tagViewer = new TableViewer(table);
		tagViewer.setLabelProvider(new TagLabelProvider());
		tagViewer.setContentProvider(new WorkbenchContentProvider());
		tagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				fillTagDialog();
			}
		});

		tagViewer.getControl().addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				fillTagDialog();
				// hide details section
				buttonPressed(IDialogConstants.DETAILS_ID);
			}
		});

		tagViewer.setInput(new TagInputList(existingTags));

		applyDialogFont(parent);
		return composite;
	}

	private void fillTagDialog() {
		IStructuredSelection selection = (IStructuredSelection) tagViewer
				.getSelection();
		Object firstSelected = selection.getFirstElement();

		if (firstSelected instanceof Tag) {
			tag = (Tag) firstSelected;
			setTagImpl();
		}
	}

	private void setTagImpl() {
		getText().setText(tag.getTag());
		messageText.setText(tag.getMessage());
		commitCombo.setSelectedElement(tag.getObjId());
	}

}
