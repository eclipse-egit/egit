/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tag;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
public class CreateTagDialog extends Dialog {

	/**
	 * Button id for a "Clear" button (value 22).
	 */
	public static final int CLEAR_ID = 22;

	private String tagName;

	private String tagMessage;

	private ObjectId tagCommit;

	private boolean overwriteTag;

	private RevWalk revCommits;

	private List<Tag> existingTags;

	private Tag tag;

	private Text tagNameText;

	private Text tagMessageText;

	private Text tagNameErrorText;

	private Button overwriteButton;

	private TableViewer tagViewer;

	private CommitCombo commitCombo;

	private Pattern tagNamePattern;

	private final String branchName;

	private final IInputValidator tagNameValidator;

	static class TagInputList extends LabelProvider implements IWorkbenchAdapter {

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

	static class TagLabelProvider extends WorkbenchLabelProvider implements
			ITableLabelProvider {

		private final ResourceManager fImageCache = new LocalResourceManager(JFaceResources
				.getResources());

		public Image getColumnImage(Object element, int columnIndex) {
			return fImageCache.createImage(UIIcons.TAG);
		}

		public String getColumnText(Object element, int columnIndex) {
			return ((Tag) element).getTag();
		}

		public void dispose() {
			fImageCache.dispose();
		}

	}

	/**
	 * Construct dialog to creating or editing tag.
	 *
	 * @param parent
	 * @param tagNameValidator
	 * @param branchName
	 */
	public CreateTagDialog(Shell parent, IInputValidator tagNameValidator,
			String branchName) {
		super(parent);
		this.tagNameValidator = tagNameValidator;
		this.branchName = branchName;
	}

	/**
	 * @return {@link ObjectId} of commit with new or edited tag should be
	 *         associated with
	 */
	public ObjectId getTagCommit() {
		return tagCommit;
	}

	/**
	 * @return message for created or edited tag.
	 */
	public String getTagMessage() {
		return tagMessage;
	}

	/**
	 * @return name of new tag
	 */
	public String getTagName() {
		return tagName;
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
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);

		if (branchName != null) {
			newShell.setText(NLS.bind(
					UIText.CreateTagDialog_questionNewTagTitle, branchName));
		}

		newShell.setMinimumSize(600, 400);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		parent.setLayout(GridLayoutFactory.swtDefaults().create());
		parent.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.create());

		Button clearButton = createButton(parent, CLEAR_ID,
				UIText.CreateTagDialog_clearButton, false);
		clearButton.setToolTipText(UIText.CreateTagDialog_clearButtonTooltip);
		setButtonLayoutData(clearButton);

		Composite margin = new Composite(parent, SWT.NONE);
		margin.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.create());

		super.createButtonsForButtonBar(parent);

		validateInput();
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = (Composite) super.createDialogArea(parent);

		final SashForm mainForm = new SashForm(composite, SWT.HORIZONTAL | SWT.FILL);
		mainForm.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.create());

		createLeftSection(mainForm);
		createExistingTagsSection(mainForm);

		mainForm.setWeights(new int[] { 70, 30 });
		if (tag != null) {
			setTagImpl();
		}

		applyDialogFont(parent);
		return composite;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case CLEAR_ID:
			tagNameText.setText(""); //$NON-NLS-1$
			tagMessageText.setText(""); //$NON-NLS-1$
			commitCombo.clearSelection();

			commitCombo.setEnabled(true);
			tagNameText.setEnabled(true);
			tagMessageText.setEnabled(true);
			overwriteButton.setEnabled(false);
			overwriteButton.setSelection(false);
			break;
		case IDialogConstants.OK_ID:
			// read and store data from widgets
			tagName = tagNameText.getText();
			tagCommit = commitCombo.getValue();
			tagMessage = tagMessageText.getText();
			overwriteTag = overwriteButton.getSelection();
			//$FALL-THROUGH$ continue propagating OK button action
		default:
			super.buttonPressed(buttonId);
		}
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	private void createLeftSection(SashForm mainForm) {
		Composite left = new Composite(mainForm, SWT.RESIZE);
		left.setLayout(GridLayoutFactory.swtDefaults().margins(10, 5).create());
		left.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.create());

		Label label = new Label(left, SWT.WRAP);
		label.setText(UIText.CreateTagDialog_tagName);
		GridData data = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_CENTER);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH/2);
		label.setLayoutData(data);
		label.setFont(left.getFont());

		tagNameText = new Text(left, SWT.SINGLE | SWT.BORDER);
		tagNameText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		tagNameText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				String textValue = Pattern.quote(tagNameText.getText());
				tagNamePattern = Pattern.compile(textValue,
						Pattern.CASE_INSENSITIVE);
				tagViewer.refresh();
				validateInput();
			}
		});

		UIUtils.addBulbDecorator(tagNameText,
				UIText.CreateTagDialog_tagNameToolTip);

		tagNameErrorText = new Text(left, SWT.READ_ONLY | SWT.WRAP);
		tagNameErrorText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		tagNameErrorText.setBackground(tagNameErrorText.getDisplay()
				.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		new Label(left, SWT.WRAP).setText(UIText.CreateTagDialog_tagMessage);

		tagMessageText = new Text(left, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		tagMessageText.setLayoutData(GridDataFactory.fillDefaults().minSize(50,
				50).grab(true, true).create());

		// key listener taken from CommitDialog.createDialogArea() allow to
		// commit with ctrl-enter
		tagMessageText.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent arg0) {
				if (arg0.keyCode == SWT.CR
						&& (arg0.stateMask & SWT.CONTROL) > 0) {
					Control button = getButton(IDialogConstants.OK_ID);
					// fire OK action only when button is enabled
					if (button != null && button.isEnabled())
						buttonPressed(IDialogConstants.OK_ID);
				} else if (arg0.keyCode == SWT.TAB
						&& (arg0.stateMask & SWT.SHIFT) == 0) {
					arg0.doit = false;
					tagMessageText.traverse(SWT.TRAVERSE_TAB_NEXT);
				}
			}
		});

		tagMessageText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateInput();
			}
		});

		overwriteButton = new Button(left, SWT.CHECK);
		overwriteButton.setEnabled(false);
		overwriteButton.setText(UIText.CreateTagDialog_overwriteTag);
		overwriteButton
				.setToolTipText(UIText.CreateTagDialog_overwriteTagToolTip);
		overwriteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean state = overwriteButton.getSelection();
				tagNameText.setEnabled(state);
				commitCombo.setEnabled(state);
				tagMessageText.setEnabled(state);
				validateInput();
			}
		});

		createAdvancedSection(left);
	}

	private void createAdvancedSection(final Composite composite) {
		ExpandableComposite advanced = new ExpandableComposite(composite,
				ExpandableComposite.TREE_NODE
						| ExpandableComposite.CLIENT_INDENT);

		advanced.setText(UIText.CreateTagDialog_advanced);
		advanced.setToolTipText(UIText.CreateTagDialog_advancedToolTip);
		advanced.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.create());

		Composite advancedComposite = new Composite(advanced, SWT.WRAP);
		advancedComposite.setLayout(GridLayoutFactory.swtDefaults().create());
		advancedComposite.setLayoutData(GridDataFactory.fillDefaults().grab(
				true, true).create());

		Label advancedLabel = new Label(advancedComposite, SWT.WRAP);
		advancedLabel.setText(UIText.CreateTagDialog_advancedMessage);
		advancedLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true,
				false).create());

		commitCombo = new CommitCombo(advancedComposite, SWT.NORMAL);
		commitCombo.setLayoutData(GridDataFactory.fillDefaults().grab(true,
				false).hint(300, SWT.DEFAULT).create());

		for (RevCommit revCommit : revCommits)
			commitCombo.add(revCommit);

		advanced.setClient(advancedComposite);
		advanced.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				composite.layout();
			}
		});
	}

	private void createExistingTagsSection(Composite parent) {
		Composite right = new Composite(parent, SWT.NORMAL);
		right.setLayout(GridLayoutFactory.swtDefaults().create());
		right.setLayoutData(GridLayoutFactory.fillDefaults().create());

		new Label(right, SWT.WRAP).setText(UIText.CreateTagDialog_existingTags);

		Table table = new Table(right, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER
				| SWT.SINGLE);
		table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.hint(80, 100).create());

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(100, 20));
		table.setLayout(layout);

		tagViewer = new TableViewer(table);
		tagViewer.setLabelProvider(new TagLabelProvider());
		tagViewer.setContentProvider(new WorkbenchContentProvider());
		tagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				fillTagDialog();
			}
		});

		tagViewer.setInput(new TagInputList(existingTags));
		tagViewer.addFilter(new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				Tag actTag = (Tag) element;

				if (tagNamePattern != null)
					return tagNamePattern.matcher(actTag.getTag()).find();
				else
					return true;
			}
		});

		applyDialogFont(parent);
	}

	private void validateInput() {
		// don't validate if dialog is disposed
		if (getShell() == null) {
			return;
		}

		// validate tag name
		String tagNameMessage = tagNameValidator.isValid(tagNameText.getText());
		setTagNameError(tagNameMessage);

		String tagMessageVal = tagMessageText.getText().trim();

		Control button = getButton(IDialogConstants.OK_ID);
		if (button != null) {
			boolean containsTagNameAndMessage = (tagNameMessage == null || tagMessageVal
					.length() == 0)
					&& tagMessageVal.length() != 0;
			boolean shouldOverwriteTag = (overwriteButton.getSelection() && Repository
					.isValidRefName(Constants.R_TAGS + tagNameText.getText()));

			button.setEnabled(containsTagNameAndMessage || shouldOverwriteTag);
		}
	}

	private void fillTagDialog() {
		IStructuredSelection selection = (IStructuredSelection) tagViewer
				.getSelection();
		Object firstSelected = selection.getFirstElement();

		if (firstSelected instanceof Tag) {
			tag = (Tag) firstSelected;

			if (!overwriteButton.isEnabled()) {
				String tagMessageValue = tag.getMessage();
				// don't enable OK button if we are dealing with un-annotated
				// tag because JGit doesn't support them
				if (tagMessageValue != null
						&& tagMessageValue.trim().length() != 0)
					overwriteButton.setEnabled(true);

				tagNameText.setEnabled(false);
				commitCombo.setEnabled(false);
				tagMessageText.setEnabled(false);
			}

			setTagImpl();
		}
	}

	private void setTagImpl() {
		tagNameText.setText(tag.getTag());
		commitCombo.setSelectedElement(tag.getObjId());

		// handle un-annotated tags
		String message = tag.getMessage();
		tagMessageText.setText(null != message ? message : ""); //$NON-NLS-1$
	}

	private void setTagNameError(String tagNameMessage) {
		// copied form
		// org.eclipse.jface.dialogs.InputDialog.setErrorMessage(String)
		if (tagNameErrorText != null && !tagNameErrorText.isDisposed()) {
			tagNameErrorText
					.setText(tagNameMessage == null ? " \n " : tagNameMessage); //$NON-NLS-1$
			// Disable the error message text control if there is no error, or
			// no error text (empty or whitespace only). Hide it also to avoid
			// color change.
			// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=130281
			boolean hasError = tagNameMessage != null
					&& (StringConverter.removeWhiteSpaces(tagNameMessage))
							.length() > 0;
			tagNameErrorText.setEnabled(hasError);
			tagNameErrorText.setVisible(hasError);
			tagNameErrorText.getParent().update();
		}
	}

}
