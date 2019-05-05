/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - improve UI responsiveness
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.egit.ui.internal.components.BranchNameNormalizer;
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
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jgit.errors.RevisionSyntaxException;
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
import org.eclipse.swt.graphics.Point;
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

/**
 * Dialog for creating and editing tags.
 */
public class CreateTagDialog extends TitleAreaDialog {

	private static final int MAX_COMMIT_COUNT = 1000;

	/**
	 * Button id for a "Clear" button.
	 */
	private static final int CLEAR_ID = 22;

	/**
	 * Button id for "Create Tag and Start Push..." button
	 */
	private static final int CREATE_AND_START_PUSH_ID = 23;

	/**
	 * We trim leading whitespace from the tag commit message.
	 */
	private static final Pattern LEADING_WHITESPACE = Pattern
			.compile("^[\\h\\v]+"); //$NON-NLS-1$

	private String tagName;

	private String tagMessage;

	private ObjectId tagCommit;

	private boolean shouldStartPushWizard = false;

	private boolean overwriteTag;

	private boolean annotated;

	/** Tag object in case an existing annotated tag was entered */
	private TagWrapper existingTag;

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

	private static class TagWrapper {
		RevTag annotatedTag;

		Ref lightweightTag;

		TagWrapper(RevTag t) {
			annotatedTag = t;
			lightweightTag = null;
		}

		TagWrapper(Ref l) {
			annotatedTag = null;
			lightweightTag = l;
		}

		public String getName() {
			if (annotatedTag != null)
				return annotatedTag.getTagName();
			return lightweightTag.getName().replaceFirst("^" + Constants.R_TAGS, //$NON-NLS-1$
					""); //$NON-NLS-1$
		}

		public ObjectId getId() {
			if (annotatedTag != null)
				return annotatedTag.getObject();
			return lightweightTag.getObjectId();
		}

		public String getMessage() {
			if (annotatedTag != null)
				return annotatedTag.getFullMessage();
			return null;
		}
	}

	private static class TagLabelProvider extends LabelProvider {
		private final Image IMG_TAG;

		private final Image IMG_LIGHTTAG;

		private TagLabelProvider() {
			IMG_TAG = UIIcons.TAG_ANNOTATED.createImage();
			IMG_LIGHTTAG = UIIcons.TAG.createImage();
		}

		@Override
		public Image getImage(Object element) {
			// initially, we just display a single String ("Loading...")
			if (element instanceof String)
				return null;
			else if (element instanceof Ref)
				return IMG_LIGHTTAG;
			else
				return IMG_TAG;
		}

		@Override
		public String getText(Object element) {
			// initially, we just display a single String ("Loading...")
			if (element instanceof String)
				return (String) element;
			else if (element instanceof Ref)
				return ((Ref) element).getName().substring(10);
			else
				return ((RevTag) element).getTagName();
		}

		@Override
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
	 * Indicates if the tag is annotated
	 *
	 * @return <code>true</code> if annotated, <code>false</code> otherwise
	 */
	public boolean isAnnotated() {
		return annotated;
	}

	/**
	 * @return true if the user wants to start the push wizard after creating
	 *         the tag, false otherwise
	 */
	public boolean shouldStartPushWizard() {
		return shouldStartPushWizard;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.CreateTagDialog_NewTag);
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
		parent.setLayoutData(
				GridDataFactory.fillDefaults().grab(true, false).create());

		Button clearButton = createButton(parent, CLEAR_ID,
				UIText.CreateTagDialog_clearButton, false);
		clearButton.setToolTipText(UIText.CreateTagDialog_clearButtonTooltip);
		setButtonLayoutData(clearButton);

		Composite margin = new Composite(parent, SWT.NONE);
		margin.setLayoutData(
				GridDataFactory.fillDefaults().grab(true, false).create());

		Button createTagAndStartPushButton = createButton(parent,
				CREATE_AND_START_PUSH_ID,
				UIText.CreateTagDialog_CreateTagAndStartPushButton, false);
		createTagAndStartPushButton.setToolTipText(
				UIText.CreateTagDialog_CreateTagAndStartPushToolTip);
		setButtonLayoutData(createTagAndStartPushButton);

		super.createButtonsForButtonBar(parent);

		getButton(OK).setText(UIText.CreateTagDialog_CreateTagButton);
		getButton(OK).setToolTipText(UIText.CreateTagDialog_tagMessageToolTip);

		validateInput();
	}

	@Override
	public void create() {
		super.create();
		// start a job that fills the tag list lazily
		Job job = new Job(UIText.CreateTagDialog_GetTagJobName) {
			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.FILL_TAG_LIST.equals(family))
					return true;
				return super.belongsTo(family);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					final List<Object> tags = getRevTags();
					PlatformUI.getWorkbench().getDisplay()
							.asyncExec(new Runnable() {
								@Override
								public void run() {
									if (!tagViewer.getTable().isDisposed()) {
										tagViewer.setInput(tags);
										tagViewer.getTable().setEnabled(true);
									}
								}
							});
				} catch (IOException e) {
					setErrorMessage(
							UIText.CreateTagDialog_ExceptionRetrievingTagsMessage);
					return Activator.createErrorStatus(e.getMessage(), e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setUser(false);
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

		final SashForm mainForm = new SashForm(composite,
				SWT.HORIZONTAL | SWT.FILL);
		mainForm.setLayoutData(
				GridDataFactory.fillDefaults().grab(true, true).create());

		createLeftSection(mainForm);
		createExistingTagsSection(mainForm);

		mainForm.setWeights(new int[] { 70, 30 });

		applyDialogFont(parent);
		return composite;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == CLEAR_ID) {
			tagNameText.setText(""); //$NON-NLS-1$
			tagMessageText.setText(""); //$NON-NLS-1$
			if (commitCombo != null) {
				commitCombo.clearSelection();
			}
			tagMessageText.getTextWidget().setEditable(true);
			overwriteButton.setEnabled(false);
			overwriteButton.setSelection(false);
		} else if (buttonId == IDialogConstants.OK_ID
				|| buttonId == CREATE_AND_START_PUSH_ID) {
			shouldStartPushWizard = (buttonId == CREATE_AND_START_PUSH_ID);
			// read and store data from widgets
			tagName = tagNameText.getText();
			if (commitCombo != null)
				tagCommit = commitCombo.getValue();
			tagMessage = tagMessageText.getCommitMessage();
			if (tagMessage != null) {
				tagMessage = LEADING_WHITESPACE.matcher(tagMessage)
						.replaceFirst(""); //$NON-NLS-1$
			}
			overwriteTag = overwriteButton.getSelection();
			annotated = !tagMessageText.getCommitMessage().isEmpty();
			okPressed();
		} else {
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
		left.setLayoutData(
				GridDataFactory.fillDefaults().grab(true, true).create());

		Label label = new Label(left, SWT.WRAP);
		label.setText(UIText.CreateTagDialog_tagName);
		GridData data = new GridData(
				GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL
						| GridData.VERTICAL_ALIGN_CENTER);
		data.widthHint = convertHorizontalDLUsToPixels(
				IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH / 2);
		label.setLayoutData(data);
		label.setFont(left.getFont());

		tagNameText = new Text(left,
				SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		tagNameText.setLayoutData(new GridData(
				GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		tagNameText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				String tagNameValue = tagNameText.getText();
				tagNamePattern = Pattern.compile(Pattern.quote(tagNameValue),
						Pattern.CASE_INSENSITIVE);
				tagViewer.refresh();
				// Only parse/set tag once (otherwise it would be set twice when
				// selecting from the existing tags)
				if (existingTag == null
						|| !tagNameValue.equals(existingTag.getName()))
					setExistingTagFromText(tagNameValue);
				validateInput();
			}
		});
		BranchNameNormalizer normalizer = new BranchNameNormalizer(tagNameText);
		normalizer.setVisible(false);

		new Label(left, SWT.WRAP).setText(UIText.CreateTagDialog_tagMessage);

		tagMessageText = new SpellcheckableMessageArea(left, tagMessage);
		Point size = tagMessageText.getTextWidget().getSize();
		tagMessageText.setLayoutData(GridDataFactory.fillDefaults().hint(size)
				.grab(true, true).create());

		// allow to tag with ctrl-enter
		tagMessageText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (UIUtils.isSubmitKeyEvent(e)) {
					Control button = getButton(IDialogConstants.OK_ID);
					// fire OK action only when button is enabled
					if (button != null && button.isEnabled())
						buttonPressed(IDialogConstants.OK_ID);
				}
			}
		});

		tagMessageText.getTextWidget().addModifyListener(new ModifyListener() {
			@Override
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
		advanced.setLayoutData(
				GridDataFactory.fillDefaults().grab(true, false).create());

		Composite advancedComposite = new Composite(advanced, SWT.WRAP);
		advancedComposite.setLayout(GridLayoutFactory.swtDefaults().create());
		advancedComposite.setLayoutData(
				GridDataFactory.fillDefaults().grab(true, true).create());

		Label advancedLabel = new Label(advancedComposite, SWT.WRAP);
		advancedLabel.setText(UIText.CreateTagDialog_advancedMessage);
		advancedLabel.setLayoutData(
				GridDataFactory.fillDefaults().grab(true, false).create());

		commitCombo = new CommitCombo(advancedComposite, SWT.NORMAL);
		commitCombo.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).hint(300, SWT.DEFAULT).create());

		advanced.setClient(advancedComposite);
		advanced.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				// fill the Combo lazily to improve UI responsiveness
				if (((Boolean) e.data).booleanValue()
						&& commitCombo.getItemCount() == 0) {
					final Collection<RevCommit> commits = new ArrayList<>();
					try {
						PlatformUI.getWorkbench().getProgressService()
								.busyCursorWhile(new IRunnableWithProgress() {

									@Override
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
					if (existingTag != null)
						commitCombo.setSelectedElement(existingTag.getId());
				}
				composite.layout(true);
				composite.getShell().pack();
			}
		});
	}

	private void createExistingTagsSection(Composite parent) {
		Composite right = new Composite(parent, SWT.NORMAL);
		right.setLayout(GridLayoutFactory.swtDefaults().create());
		right.setLayoutData(GridDataFactory.fillDefaults().create());

		new Label(right, SWT.WRAP).setText(UIText.CreateTagDialog_existingTags);

		Table table = new Table(right,
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE);
		table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.hint(80, 100).create());

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(100, 20));
		table.setLayout(layout);

		tagViewer = new TableViewer(table);
		tagViewer.setLabelProvider(new TagLabelProvider());
		tagViewer.setContentProvider(ArrayContentProvider.getInstance());
		tagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fillTagDialog(event.getSelection());
			}
		});
		tagViewer.setComparator(new ViewerComparator() {
			@Override
			protected Comparator<? super String> getComparator() {
				return CommonUtils.STRING_ASCENDING_COMPARATOR;
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
		tagViewer.setInput(
				new String[] { UIText.CreateTagDialog_LoadingMessageText });
		tagViewer.getTable().setEnabled(false);
		applyDialogFont(parent);
	}

	private void validateInput() {
		// don't do anything if dialog is disposed
		if (getShell() == null) {
			return;
		}

		// validate tag name
		String tagNameMessage = tagNameValidator.isValid(tagNameText.getText());
		setErrorMessage(tagNameMessage);

		String tagMessageVal = tagMessageText.getText().trim();

		boolean isLightWeight = tagMessageVal.isEmpty();
		Control button = getButton(IDialogConstants.OK_ID);
		if (button != null) {
			boolean containsTagNameAndMessage = (tagNameMessage == null
					|| tagMessageVal.length() == 0)
					&& tagMessageVal.length() != 0;
			boolean shouldOverwriteTag = (overwriteButton.getSelection()
					&& Repository.isValidRefName(
							Constants.R_TAGS + tagNameText.getText()));

			boolean enabled = containsTagNameAndMessage || shouldOverwriteTag
					|| (isLightWeight && tagNameMessage == null
							&& !tagNameText.getText().isEmpty());
			button.setEnabled(enabled);

			Button createTagAndStartPush = getButton(CREATE_AND_START_PUSH_ID);
			if (createTagAndStartPush != null)
				createTagAndStartPush.setEnabled(enabled);
		}

		boolean existingTagSelected = existingTag != null;
		boolean readOnly = (existingTagSelected
				&& !overwriteButton.getSelection());
		tagMessageText.getTextWidget().setEditable(!readOnly);

		overwriteButton.setEnabled(existingTagSelected);
		if (!existingTagSelected)
			overwriteButton.setSelection(false);
	}

	private void fillTagDialog(ISelection actSelection) {
		IStructuredSelection selection = (IStructuredSelection) actSelection;
		Object firstSelected = selection.getFirstElement();
		setExistingTag(firstSelected);
	}

	private void setExistingTagFromText(String tagName) {
		try {
			ObjectId tagObjectId = repo.resolve(Constants.R_TAGS + tagName);
			if (tagObjectId != null) {
				try (RevWalk revWalk = new RevWalk(repo)) {
					RevObject tagObject = revWalk.parseAny(tagObjectId);
					setExistingTag(tagObject);
				}
				return;
			}
		} catch (IOException e) {
			// ignore
		} catch (RevisionSyntaxException e) {
			// ignore
		}
		setNoExistingTag();
	}

	private void setNoExistingTag() {
		existingTag = null;
	}

	private void setExistingTag(Object tagObject) {
		if (tagObject instanceof RevTag)
			existingTag = new TagWrapper((RevTag) tagObject);
		else if (tagObject instanceof Ref) {
			existingTag = new TagWrapper((Ref) tagObject);
		} else {
			setNoExistingTag();
			return;
		}

		if (!tagNameText.getText().equals(existingTag.getName()))
			tagNameText.setText(existingTag.getName());
		if (commitCombo != null)
			commitCombo.setSelectedElement(existingTag.getId());

		// handle un-annotated tags
		String message = existingTag.getMessage();
		tagMessageText.setText(message != null ? message : ""); //$NON-NLS-1$
	}

	private void getRevCommits(Collection<RevCommit> commits) {
		try (final RevWalk revWalk = new RevWalk(repo)) {
			revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
			revWalk.sort(RevSort.BOUNDARY, true);
			AnyObjectId headId = repo.resolve(Constants.HEAD);
			if (headId != null)
				revWalk.markStart(revWalk.parseCommit(headId));
			// do the walk to get the commits
			long count = 0;
			RevCommit commit;
			while ((commit = revWalk.next()) != null
					&& count < MAX_COMMIT_COUNT) {
				commits.add(commit);
				count++;
			}
		} catch (IOException e) {
			Activator.logError(UIText.TagAction_errorWhileGettingRevCommits, e);
			setErrorMessage(UIText.TagAction_errorWhileGettingRevCommits);
		}
	}

	/**
	 * @return the annotated tags
	 * @throws IOException
	 */
	private List<Object> getRevTags() throws IOException {
		List<Object> result = new ArrayList<>();
		Collection<Ref> refs = repo.getRefDatabase()
				.getRefsByPrefix(Constants.R_TAGS);
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
