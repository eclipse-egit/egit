/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - improve UI responsiveness
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.SWTUtils;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTag;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Dialog for creating and editing tags.
 */
public class CreateTagDialog extends TitleAreaDialog {

	/**
	 * Button id for a "Clear" button (value 22).
	 */
	public static final int CLEAR_ID = 22;

	private String tagName;

	private String tagMessage;

	private ObjectId tagCommit;

	private boolean overwriteTag;

	private RevTag tag;

	private Repository repo;

	private Text tagNameText;

	private SpellcheckableMessageArea tagMessageText;

	private Button overwriteButton;

	private TableViewer tagViewer;

	private CommitCombo commitCombo;

	private Pattern tagNamePattern;

	private final String branchName;

	private final ObjectId commitId;

	private final IInputValidator tagNameValidator;

	private final RevWalk rw;

	private static class TagLabelProvider extends WorkbenchLabelProvider implements
			ITableLabelProvider {
		private final Image IMG_TAG;

		private final Image IMG_LIGHTTAG;

		private TagLabelProvider() {
			IMG_TAG = UIIcons.TAG.createImage();
			IMG_LIGHTTAG = SWTUtils.getDecoratedImage(IMG_TAG,
					UIIcons.OVR_LIGHTTAG);
		}

		public Image getColumnImage(Object element, int columnIndex) {
			// initially, we just display a single String ("Loading...")
			if (element instanceof String)
				return null;
			else if (element instanceof Ref)
				return IMG_LIGHTTAG;
			else
				return IMG_TAG;
		}

		public String getColumnText(Object element, int columnIndex) {
			// initially, we just display a single String ("Loading...")
			if (element instanceof String)
				return (String) element;
			else if (element instanceof Ref)
				return ((Ref) element).getName().substring(10);
			else
				return ((RevTag) element).getTagName();
		}

		public void dispose() {
			IMG_TAG.dispose();
			IMG_LIGHTTAG.dispose();
			super.dispose();
		}
	}

	/**
	 * Construct dialog to creating or editing tag.
	 *
	 * @param parent
	 * @param branchName
	 * @param repo
	 */
	public CreateTagDialog(Shell parent, String branchName, Repository repo) {
		super(parent);
		this.tagNameValidator = ValidationUtils.getRefNameInputValidator(repo,
				Constants.R_TAGS, false);
		this.branchName = branchName;
		this.commitId = null;
		this.repo = repo;
		this.rw = new RevWalk(repo);
		setHelpAvailable(false);
	}

	/**
	 * Construct dialog to creating or editing tag.
	 *
	 * @param parent
	 * @param commitId
	 * @param repo
	 */
	public CreateTagDialog(Shell parent, ObjectId commitId, Repository repo) {
		super(parent);
		this.tagNameValidator = ValidationUtils.getRefNameInputValidator(repo,
				Constants.R_TAGS, false);
		this.branchName = null;
		this.commitId = commitId;
		this.repo = repo;
		this.rw = new RevWalk(repo);
		setHelpAvailable(false);
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
	 * Data from <code>tag</code> argument will be set in this dialog box.
	 *
	 * @param tag
	 */
	public void setTag(RevTag tag) {
		this.tag = tag;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.CreateTagDialog_NewTag);
		newShell.setMinimumSize(600, 400);
	}

	private String getTitle() {
		String title = ""; //$NON-NLS-1$
		if (branchName != null) {
			title = NLS.bind(UIText.CreateTagDialog_questionNewTagTitle,
					branchName);
		} else if (commitId != null) {
			title = NLS.bind(UIText.CreateTagDialog_CreateTagOnCommitTitle,
					CompareUtils.truncatedRevision(commitId.getName()));
		}
		return title;
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
	public void create() {
		super.create();
		// start a job that fills the tag list lazily
		Job job = new Job(UIText.CreateTagDialog_GetTagJobName) {
			@Override
			public boolean belongsTo(Object family) {
				if (family.equals(JobFamilies.FILL_TAG_LIST))
					return true;
				return super.belongsTo(family);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					final List<Object> tags = getRevTags();
					PlatformUI.getWorkbench().getDisplay()
							.asyncExec(new Runnable() {
								public void run() {
									if (!tagViewer.getTable().isDisposed()) {
										tagViewer.setInput(tags);
										tagViewer.getTable().setEnabled(true);
									}
								}
							});
				} catch (IOException e) {
					setErrorMessage(UIText.CreateTagDialog_ExceptionRetrievingTagsMessage);
					return Activator.createErrorStatus(e.getMessage(), e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	@Override
	public boolean close() {
		rw.dispose();
		return super.close();
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		initializeDialogUnits(parent);

		setTitle(getTitle());
		setMessage(UIText.CreateTagDialog_Message);

		Composite composite = (Composite) super.createDialogArea(parent);

		final SashForm mainForm = new SashForm(composite, SWT.HORIZONTAL
				| SWT.FILL);
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
			if (commitCombo != null) {
				commitCombo.clearSelection();
				commitCombo.setEnabled(true);
			}
			tagNameText.setEnabled(true);
			tagMessageText.setEnabled(true);
			overwriteButton.setEnabled(false);
			overwriteButton.setSelection(false);
			break;
		case IDialogConstants.OK_ID:
			// read and store data from widgets
			tagName = tagNameText.getText();
			if (commitCombo != null)
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
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH / 2);
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

		new Label(left, SWT.WRAP).setText(UIText.CreateTagDialog_tagMessage);

		tagMessageText = new SpellcheckableMessageArea(left, tagMessage);
		tagMessageText.setLayoutData(GridDataFactory.fillDefaults()
				.minSize(50, 50).grab(true, true).create());

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

		tagMessageText.getTextWidget().addModifyListener(new ModifyListener() {
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
				if (commitCombo != null)
					commitCombo.setEnabled(state);
				tagMessageText.setEnabled(state);
				validateInput();
			}
		});

		createAdvancedSection(left);
	}

	private void createAdvancedSection(final Composite composite) {
		if (commitId != null)
			return;
		ExpandableComposite advanced = new ExpandableComposite(composite,
				ExpandableComposite.TREE_NODE
						| ExpandableComposite.CLIENT_INDENT);

		advanced.setText(UIText.CreateTagDialog_advanced);
		advanced.setToolTipText(UIText.CreateTagDialog_advancedToolTip);
		advanced.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.create());

		Composite advancedComposite = new Composite(advanced, SWT.WRAP);
		advancedComposite.setLayout(GridLayoutFactory.swtDefaults().create());
		advancedComposite.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, true).create());

		Label advancedLabel = new Label(advancedComposite, SWT.WRAP);
		advancedLabel.setText(UIText.CreateTagDialog_advancedMessage);
		advancedLabel.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());

		commitCombo = new CommitCombo(advancedComposite, SWT.NORMAL);
		commitCombo.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).hint(300, SWT.DEFAULT).create());

		advanced.setClient(advancedComposite);
		advanced.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				// fill the Combo lazily to improve UI responsiveness
				if (((Boolean) e.data).booleanValue()
						&& commitCombo.getItemCount() == 0) {
					final Collection<RevCommit> commits = new ArrayList<RevCommit>();
					try {
						PlatformUI.getWorkbench().getProgressService()
								.busyCursorWhile(new IRunnableWithProgress() {

									public void run(IProgressMonitor monitor)
											throws InvocationTargetException,
											InterruptedException {
										getRevCommits(commits);
									}
								});
					} catch (InvocationTargetException e1) {
						Activator.logError(e1.getMessage(), e1);
					} catch (InterruptedException e1) {
						// ignore here
					}
					for (RevCommit revCommit : commits)
						commitCombo.add(revCommit);

					// Set combo selection if a tag is selected
					if (tag != null)
						commitCombo.setSelectedElement(tag.getObject());
				}
				composite.layout(true);
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
		tagViewer.setContentProvider(ArrayContentProvider.getInstance());
		tagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				fillTagDialog(event.getSelection());
			}
		});
		tagViewer.addFilter(new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				if (tagNamePattern == null)
					return true;
				String name;
				if (element instanceof String)
					return true;
				else if (element instanceof Ref) {
					Ref t = (Ref) element;
					name = t.getName().substring(10);
				} else if (element instanceof RevTag) {
					RevTag t = (RevTag) element;
					name = t.getTagName();
				} else
					return true;
				return tagNamePattern.matcher(name).find();
			}

		});
		// let's set the table inactive initially and display a "Loading..."
		// message and fill the list asynchronously during create() in order to
		// improve UI responsiveness
		tagViewer
				.setInput(new String[] { UIText.CreateTagDialog_LoadingMessageText });
		tagViewer.getTable().setEnabled(false);
		applyDialogFont(parent);
	}

	private void validateInput() {
		// don't validate if dialog is disposed
		if (getShell() == null) {
			return;
		}

		// validate tag name
		String tagNameMessage = tagNameValidator.isValid(tagNameText.getText());
		setErrorMessage(tagNameMessage);

		String tagMessageVal = tagMessageText.getText().trim();

		Control button = getButton(IDialogConstants.OK_ID);
		if (button != null) {
			boolean containsTagNameAndMessage = (tagNameMessage == null || tagMessageVal
					.length() == 0) && tagMessageVal.length() != 0;
			boolean shouldOverwriteTag = (overwriteButton.getSelection() && Repository
					.isValidRefName(Constants.R_TAGS + tagNameText.getText()));

			button.setEnabled(containsTagNameAndMessage || shouldOverwriteTag);
		}
	}

	private void fillTagDialog(ISelection actSelection) {
		IStructuredSelection selection = (IStructuredSelection) actSelection;
		Object firstSelected = selection.getFirstElement();

		if (firstSelected instanceof RevTag)
			tag = (RevTag) firstSelected;
		else {
			setErrorMessage(UIText.CreateTagDialog_LightweightTagMessage);
			return;
		}

		if (!overwriteButton.isEnabled()) {
			String tagMessageValue = tag.getFullMessage();
			// don't enable OK button if we are dealing with un-annotated
			// tag because JGit doesn't support them
			if (tagMessageValue != null && tagMessageValue.trim().length() != 0)
				overwriteButton.setEnabled(true);

			tagNameText.setEnabled(false);
			if (commitCombo != null)
				commitCombo.setEnabled(false);
			tagMessageText.setEnabled(false);
		}

		setTagImpl();
	}

	private void setTagImpl() {
		tagNameText.setText(tag.getTagName());
		if (commitCombo != null)
			commitCombo.setSelectedElement(tag.getObject());

		// handle un-annotated tags
		String message = tag.getFullMessage();
		tagMessageText.setText(null != message ? message : ""); //$NON-NLS-1$
	}

	private void getRevCommits(Collection<RevCommit> commits) {
		RevWalk revWalk = new RevWalk(repo);
		revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
		revWalk.sort(RevSort.BOUNDARY, true);

		try {
			AnyObjectId headId = repo.resolve(Constants.HEAD);
			if (headId != null)
				revWalk.markStart(revWalk.parseCommit(headId));
		} catch (IOException e) {
			Activator.logError(UIText.TagAction_errorWhileGettingRevCommits, e);
			setErrorMessage(UIText.TagAction_errorWhileGettingRevCommits);
		}
		// do the walk to get the commits
		for (RevCommit commit : revWalk)
			commits.add(commit);
	}

	/**
	 * @return the annotated tags
	 * @throws IOException
	 */
	private List<Object> getRevTags() throws IOException {
		List<Object> result = new ArrayList<Object>();
		Collection<Ref> refs = repo.getRefDatabase().getRefs(Constants.R_TAGS)
				.values();
		for (Ref ref : refs) {
			RevObject any;
			try {
				any = rw.parseAny(repo.resolve(ref.getName()));
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
				break;
			}
			if (any instanceof RevTag)
				result.add(any);
			else
				result.add(ref);
		}
		return result;
	}
}
