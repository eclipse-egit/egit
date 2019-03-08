/*******************************************************************************
 *  Copyright (c) 2011, 2016 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - preference-based date formatting
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.ClipboardUtils;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.GitLabelProvider;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.egit.ui.internal.history.CommitFileDiffViewer;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.util.GitDateFormatter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.AbstractHyperlink;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;

/**
 * Commit editor page class displaying author, committer, parent commits,
 * message, and file information in form sections.
 */
public class CommitEditorPage extends FormPage
		implements ISchedulingRule, IShowInSource {

	/**
	 * Max number of branches, after that we compute "commit on branch" info
	 * asynchronously
	 */
	private static final int BRANCH_LIMIT_FOR_SYNC_LOAD = 42;

	private static final String SIGNED_OFF_BY = "Signed-off-by: {0} <{1}>"; //$NON-NLS-1$

	private LocalResourceManager resources = new LocalResourceManager(
			JFaceResources.getResources());

	private Composite tagLabelArea;

	private Section branchSection;

	private TableViewer branchViewer;

	private Section diffSection;

	private CommitFileDiffViewer diffViewer;

	private FocusTracker focusTracker = new FocusTracker();

	/**
	 * Create commit editor page
	 *
	 * @param editor
	 */
	public CommitEditorPage(FormEditor editor) {
		this(editor, "commitPage", UIText.CommitEditorPage_Title); //$NON-NLS-1$
	}

	/**
	 * Create commit editor page
	 *
	 * @param editor
	 * @param id
	 * @param title
	 */
	public CommitEditorPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	/**
	 * Add the given {@link Control} to this form's focus tracking.
	 *
	 * @param control
	 *            to add to focus tracking
	 */
	protected void addToFocusTracking(@NonNull Control control) {
		focusTracker.addToFocusTracking(control);
	}

	private void addSectionTextToFocusTracking(@NonNull Section composite) {
		for (Control control : composite.getChildren()) {
			if (control instanceof AbstractHyperlink) {
				addToFocusTracking(control);
			}
		}
	}

	private void hookExpansionGrabbing(final Section section) {
		section.addExpansionListener(new ExpansionAdapter() {

			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				((GridData) section.getLayoutData()).grabExcessVerticalSpace = e
						.getState();
				getManagedForm().getForm().getBody().layout(true, true);
			}
		});
	}

	private Image getImage(ImageDescriptor descriptor) {
		return (Image) this.resources.get(descriptor);
	}

	Section createSection(Composite parent, FormToolkit toolkit, String title,
			int span, int extraStyles) {
		Section section = toolkit.createSection(parent, extraStyles);
		GridDataFactory.fillDefaults().span(span, 1).grab(true, true)
				.applyTo(section);
		section.setText(title);
		addSectionTextToFocusTracking(section);
		return section;
	}

	Composite createSectionClient(Section parent, FormToolkit toolkit) {
		Composite client = toolkit.createComposite(parent);
		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2)
				.applyTo(client);
		return client;
	}

	private boolean isSignedOffBy(PersonIdent person) {
		RevCommit commit = getCommit().getRevCommit();
		return commit.getFullMessage().indexOf(getSignedOffByLine(person)) != -1;
	}

	private String getSignedOffByLine(PersonIdent person) {
		return MessageFormat.format(SIGNED_OFF_BY, person.getName(),
				person.getEmailAddress());
	}

	private void setPerson(Text text, PersonIdent person, boolean isAuthor) {
		PreferenceBasedDateFormatter formatter = PreferenceBasedDateFormatter
				.create();
		boolean isRelative = formatter
				.getFormat() == GitDateFormatter.Format.RELATIVE;
		String textTemplate = null;
		if (isAuthor) {
			textTemplate = isRelative
					? UIText.CommitEditorPage_LabelAuthorRelative
					: UIText.CommitEditorPage_LabelAuthor;
		} else {
			textTemplate = isRelative
					? UIText.CommitEditorPage_LabelCommitterRelative
					: UIText.CommitEditorPage_LabelCommitter;
		}
		text.setText(MessageFormat.format(textTemplate, person.getName(),
				person.getEmailAddress(), formatter.formatDate(person)));
	}

	private Composite createUserArea(Composite parent, FormToolkit toolkit,
			PersonIdent person, boolean author) {
		Composite userArea = toolkit.createComposite(parent);
		GridLayoutFactory.fillDefaults().spacing(2, 2).numColumns(3)
				.applyTo(userArea);

		Label userLabel = toolkit.createLabel(userArea, null);
		userLabel.setImage(getImage(author ? UIIcons.ELCL16_AUTHOR
				: UIIcons.ELCL16_COMMITTER));
		if (author)
			userLabel.setToolTipText(UIText.CommitEditorPage_TooltipAuthor);
		else
			userLabel.setToolTipText(UIText.CommitEditorPage_TooltipCommitter);

		boolean signedOff = isSignedOffBy(person);

		final Text userText = new Text(userArea, SWT.FLAT | SWT.READ_ONLY);
		addToFocusTracking(userText);
		setPerson(userText, person, author);
		toolkit.adapt(userText, false, false);
		userText.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
		IPropertyChangeListener uiPrefsListener = (event) -> {
			String property = event.getProperty();
			if (UIPreferences.DATE_FORMAT.equals(property)
					|| UIPreferences.DATE_FORMAT_CHOICE.equals(property)) {
				setPerson(userText, person, author);
				userArea.layout();
			}
		};
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(uiPrefsListener);
		userText.addDisposeListener((e) -> {
			Activator.getDefault().getPreferenceStore()
					.removePropertyChangeListener(uiPrefsListener);
		});
		GridDataFactory.fillDefaults().span(signedOff ? 1 : 2, 1)
				.applyTo(userText);
		if (signedOff) {
			Label signedOffLabel = toolkit.createLabel(userArea, null);
			signedOffLabel.setImage(getImage(UIIcons.SIGNED_OFF));
			if (author)
				signedOffLabel
						.setToolTipText(UIText.CommitEditorPage_TooltipSignedOffByAuthor);
			else
				signedOffLabel
						.setToolTipText(UIText.CommitEditorPage_TooltipSignedOffByCommitter);
		}

		return userArea;
	}

	void updateSectionClient(Section section, Composite client,
			FormToolkit toolkit) {
		hookExpansionGrabbing(section);
		toolkit.paintBordersFor(client);
		section.setClient(client);
	}

	private void createHeaderArea(Composite parent, FormToolkit toolkit,
			int span) {
		RevCommit commit = getCommit().getRevCommit();
		Composite top = toolkit.createComposite(parent);
		GridDataFactory.fillDefaults().grab(true, false).span(span, 1)
				.applyTo(top);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(top);

		Composite userArea = toolkit.createComposite(top);
		GridLayoutFactory.fillDefaults().spacing(2, 2).numColumns(1)
				.applyTo(userArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(userArea);

		PersonIdent author = commit.getAuthorIdent();
		if (author != null)
			createUserArea(userArea, toolkit, author, true);

		PersonIdent committer = commit.getCommitterIdent();
		if (committer != null && !committer.equals(author))
			createUserArea(userArea, toolkit, committer, false);

		int count = commit.getParentCount();
		if (count > 0)
			createParentsArea(top, toolkit, commit);

		createTagsArea(userArea, toolkit, 2);
	}

	private void createParentsArea(Composite parent, FormToolkit toolkit,
			RevCommit commit) {
		Composite parents = toolkit.createComposite(parent);
		GridLayoutFactory.fillDefaults().spacing(2, 2).numColumns(2)
				.applyTo(parents);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(parents);

		for (int i = 0; i < commit.getParentCount(); i++) {
			final RevCommit parentCommit = commit.getParent(i);
			toolkit.createLabel(parents, getParentCommitLabel(i))
					.setForeground(
							toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
			final Hyperlink link = toolkit
					.createHyperlink(parents,
							Utils.getShortObjectId(parentCommit),
							SWT.NONE);
			link.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					try {
						CommitEditor.open(new RepositoryCommit(getCommit()
								.getRepository(), parentCommit));
						if ((e.getStateMask() & SWT.MOD1) != 0)
							getEditor().close(false);
					} catch (PartInitException e1) {
						Activator.logError(
								"Error opening commit editor", e1);//$NON-NLS-1$
					}
				}
			});
			String sha1 = parentCommit.getName();
			link.setToolTipText(sha1);
			createParentContextMenu(link, sha1);
			addToFocusTracking(link);
		}
	}

	private void createParentContextMenu(Hyperlink link, String sha1) {
		Menu contextMenu = new Menu(link);

		final MenuItem copySHA1MenuItem = new MenuItem(contextMenu, SWT.PUSH);
		copySHA1MenuItem.setText(UIText.Header_contextMenu_copy_SHA1);
		copySHA1MenuItem.setImage(getImage(UIIcons.ELCL16_ID));
		final Shell shell = link.getShell();
		copySHA1MenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				ClipboardUtils.copySha1ToClipboard(sha1, shell);
			}
		});
		link.setMenu(contextMenu);
	}

	@SuppressWarnings("unused")
	String getParentCommitLabel(int i) {
		return UIText.CommitEditorPage_LabelParent;
	}

	private List<Ref> getTags() throws IOException {
		Repository repository = getCommit().getRepository();
		List<Ref> tags = new ArrayList<>(
				repository.getRefDatabase().getRefsByPrefix(Constants.R_TAGS));
		Collections.sort(tags, new Comparator<Ref>() {

			@Override
			public int compare(Ref r1, Ref r2) {
				return CommonUtils.STRING_ASCENDING_COMPARATOR.compare(
						Repository.shortenRefName(r1.getName()),
						Repository.shortenRefName(r2.getName()));
			}
		});
		return tags;
	}

	void createTagsArea(Composite parent, FormToolkit toolkit,
			int span) {
		Composite tagArea = toolkit.createComposite(parent);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false)
				.applyTo(tagArea);
		GridDataFactory.fillDefaults().span(span, 1).grab(true, false)
				.applyTo(tagArea);
		toolkit.createLabel(tagArea, UIText.CommitEditorPage_LabelTags)
				.setForeground(
						toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
		tagLabelArea = toolkit.createComposite(tagArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tagLabelArea);
		GridLayoutFactory.fillDefaults().spacing(1, 1).applyTo(tagLabelArea);
	}

	void fillDiffs(FileDiff[] diffs) {
		diffViewer.newInput(diffs);
		diffSection.setText(getDiffSectionTitle(Integer.valueOf(diffs.length)));
		setSectionExpanded(diffSection, diffs.length != 0);
	}

	static void setSectionExpanded(Section section, boolean expanded) {
		section.setExpanded(expanded);
		((GridData) section.getLayoutData()).grabExcessVerticalSpace = expanded;
	}

	String getDiffSectionTitle(Integer numChanges) {
		return MessageFormat.format(UIText.CommitEditorPage_SectionFiles,
				numChanges);
	}

	void fillTags(FormToolkit toolkit, List<Ref> tags) {
		for (Control child : tagLabelArea.getChildren())
			child.dispose();

		// Hide "Tags" area if no tags to show
		((GridData) tagLabelArea.getParent().getLayoutData()).exclude = tags
				.isEmpty();

		GridLayoutFactory.fillDefaults().spacing(1, 1).numColumns(tags.size())
				.applyTo(tagLabelArea);

		for (Ref tag : tags) {
			ObjectId id = tag.getPeeledObjectId();
			boolean annotated = id != null;
			if (id == null)
				id = tag.getObjectId();
			CLabel tagLabel = new CLabel(tagLabelArea, SWT.NONE);
			toolkit.adapt(tagLabel, false, false);
			if (annotated)
				tagLabel.setImage(getImage(UIIcons.TAG_ANNOTATED));
			else
				tagLabel.setImage(getImage(UIIcons.TAG));
			tagLabel.setText(Repository.shortenRefName(tag.getName()));
		}
	}

	private void createMessageArea(Composite parent, FormToolkit toolkit,
			int span) {
		Section messageSection = createSection(parent, toolkit,
				UIText.CommitEditorPage_SectionMessage, span,
				ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE
						| ExpandableComposite.EXPANDED);
		Composite messageArea = createSectionClient(messageSection, toolkit);

		RevCommit commit = getCommit().getRevCommit();
		String message = commit.getFullMessage();

		SpellcheckableMessageArea textContent = new SpellcheckableMessageArea(
				messageArea, message, true, toolkit.getBorderStyle()) {

			@Override
			protected IAdaptable getDefaultTarget() {
				return new PlatformObject() {
					@Override
					public <T> T getAdapter(Class<T> adapter) {
						return Platform.getAdapterManager().getAdapter(
								getEditorInput(), adapter);
					}
				};
			}

			@Override
			protected void createMarginPainter() {
				// Disabled intentionally
			}

		};

		if ((toolkit.getBorderStyle() & SWT.BORDER) == 0)
			textContent.setData(FormToolkit.KEY_DRAW_BORDER,
					FormToolkit.TEXT_BORDER);

		StyledText textWidget = textContent.getTextWidget();
		Point size = textWidget.computeSize(SWT.DEFAULT,
				SWT.DEFAULT);
		int yHint = size.y > 80 ? 80 : SWT.DEFAULT;
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, yHint).minSize(1, 20)
				.grab(true, true).applyTo(textContent);

		addToFocusTracking(textWidget);
		updateSectionClient(messageSection, messageArea, toolkit);
	}

	private void createBranchesArea(Composite parent, FormToolkit toolkit,
			int span) {
		branchSection = createSection(parent, toolkit,
				UIText.CommitEditorPage_SectionBranchesEmpty, span,
				ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE);
		((GridData) branchSection
				.getLayoutData()).grabExcessVerticalSpace = false;
		Composite branchesArea = createSectionClient(branchSection, toolkit);

		branchViewer = new TableViewer(toolkit.createTable(branchesArea,
				SWT.V_SCROLL | SWT.H_SCROLL));
		Control control = branchViewer.getControl();
		control.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 50)
				.applyTo(control);
		addToFocusTracking(control);
		branchViewer.setComparator(new ViewerComparator() {
			@Override
			protected Comparator<? super String> getComparator() {
				return CommonUtils.STRING_ASCENDING_COMPARATOR;
			}
		});
		branchViewer.setLabelProvider(new GitLabelProvider() {

			@Override
			public String getText(Object element) {
				return Repository.shortenRefName(super.getText(element));
			}

		});
		branchViewer.setContentProvider(ArrayContentProvider.getInstance());
		updateSectionClient(branchSection, branchesArea, toolkit);
	}

	private void fillBranches(List<Ref> result) {
		if (!result.isEmpty()) {
			branchViewer.setInput(result);
			branchSection.setText(MessageFormat.format(
					UIText.CommitEditorPage_SectionBranches,
					Integer.valueOf(result.size())));
		}
	}

	void createDiffArea(Composite parent, FormToolkit toolkit, int span) {
		diffSection = createSection(parent, toolkit,
				UIText.CommitEditorPage_SectionFilesEmpty, span,
				ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE
						| ExpandableComposite.EXPANDED);
		Composite filesArea = createSectionClient(diffSection, toolkit);

		diffViewer = new CommitFileDiffViewer(filesArea, getSite(), SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL | SWT.FULL_SELECTION
				| toolkit.getBorderStyle());
		Control control = diffViewer.getControl();
		control.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 50).minSize(1, 50)
				.grab(true, true).applyTo(control);
		addToFocusTracking(control);
		updateSectionClient(diffSection, filesArea, toolkit);
	}

	RepositoryCommit getCommit() {
		return AdapterUtils.adapt(getEditor(), RepositoryCommit.class);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		managedForm.addPart(new FocusManagerFormPart(focusTracker) {

			@Override
			public void setDefaultFocus() {
				getManagedForm().getForm().setFocus();
			}
		});
		Composite body = managedForm.getForm().getBody();
		body.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				resources.dispose();
			}
		});
		FillLayout bodyLayout = new FillLayout();
		bodyLayout.marginHeight = 5;
		bodyLayout.marginWidth = 5;
		body.setLayout(bodyLayout);

		FormToolkit toolkit = managedForm.getToolkit();

		Composite displayArea = new Composite(body, toolkit.getOrientation()) {

			@Override
			public boolean setFocus() {
				Control control = focusTracker.getLastFocusControl();
				if (control != null && control.forceFocus()) {
					return true;
				}
				return super.setFocus();
			}
		};
		toolkit.adapt(displayArea);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(displayArea);

		createHeaderArea(displayArea, toolkit, 2);
		createMessageArea(displayArea, toolkit, 2);
		createChangesArea(displayArea, toolkit);

		loadSections();
	}

	void createChangesArea(Composite displayArea, FormToolkit toolkit) {
		createBranchesArea(displayArea, toolkit, 2);
		createDiffArea(displayArea, toolkit, 2);
	}

	private List<Ref> loadTags() {
		RepositoryCommit repoCommit = getCommit();
		RevCommit commit = repoCommit.getRevCommit();
		Repository repository = repoCommit.getRepository();
		List<Ref> tags = new ArrayList<>();
		try {
			for (Ref tag : getTags()) {
				tag = repository.getRefDatabase().peel(tag);
				ObjectId id = tag.getPeeledObjectId();
				if (id == null) {
					id = tag.getObjectId();
				}
				if (commit.equals(id)) {
					tags.add(tag);
				}
			}
		} catch (IOException e) {
			Activator.logError(MessageFormat.format(
					UIText.CommitEditor_couldNotGetTags,
					commit.getName(),
					repository.getDirectory().getAbsolutePath()), e);
		}
		return tags;
	}

	private List<Ref> loadBranches() {
		RepositoryCommit commit = getCommit();
		Repository repository = commit.getRepository();
		List<Ref> refs = getAllBranchRefs(repository);
		if (refs.isEmpty()) {
			return Collections.emptyList();
		}
		if (refs.size() < BRANCH_LIMIT_FOR_SYNC_LOAD) {
			return findBranchesReachableFromCommit(commit, refs);
		} else {
			Job branchRefreshJob = new CommitEditorPageJob(commit) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					List<Ref> branches = findBranchesReachableFromCommit(commit,
							refs);
					updateUI(monitor, () -> fillBranches(branches));
					return Status.OK_STATUS;
				}
			};
			branchRefreshJob.schedule();
			return Collections.emptyList();
		}
	}

	private List<Ref> findBranchesReachableFromCommit(RepositoryCommit commit,
			List<Ref> refs) {
		try (RevWalk revWalk = new RevWalk(commit.getRepository())) {
			return RevWalkUtils.findBranchesReachableFrom(commit.getRevCommit(),
					revWalk, refs);
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, false);
			return Collections.emptyList();
		}
	}

	private List<Ref> getAllBranchRefs(Repository repository) {
		List<Ref> refs = new ArrayList<>();
		try {
			refs.addAll(repository.getRefDatabase().getRefsByPrefix(
					Constants.R_HEADS));
			refs.addAll(repository.getRefDatabase().getRefsByPrefix(
					Constants.R_REMOTES));
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, false);
			return Collections.emptyList();
		}
		return refs;
	}

	void loadSections() {
		Job refreshJob = new CommitEditorPageJob(getCommit()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final List<Ref> tags = loadTags();
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				final List<Ref> branches = loadBranches();
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				final FileDiff[] diffs = getCommit().getDiffs();
				updateUI(monitor, () -> {
					fillTags(getManagedForm().getToolkit(), tags);
					fillDiffs(diffs);
					fillBranches(branches);
				});
				return Status.OK_STATUS;
			}
		};
		refreshJob.schedule();
	}

	private abstract class CommitEditorPageJob extends Job {

		protected final RepositoryCommit commit;

		public CommitEditorPageJob(RepositoryCommit commit) {
			super(MessageFormat.format(UIText.CommitEditorPage_JobName,
					commit.getRevCommit().name()));
			this.commit = commit;
			setRule(CommitEditorPage.this);
		}

		@Override
		public boolean belongsTo(Object family) {
			return CommitEditorPage.this == family;
		}

		protected final void updateUI(IProgressMonitor monitor, Runnable task) {
			ScrolledForm form = getManagedForm().getForm();
			PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
				if (!shouldContinue(monitor, form)) {
					return;
				}
				task.run();
				form.reflow(true);
				form.layout(true, true);
			});
		}

		protected boolean shouldContinue(IProgressMonitor monitor,
				ScrolledForm form) {
			return UIUtils.isUsable(form) && !monitor.isCanceled();
		}
	}

	/**
	 * Refresh the editor page
	 */
	public void refresh() {
		loadSections();
	}

	@Override
	public void dispose() {
		focusTracker.dispose();
		Job.getJobManager().cancel(this);
		super.dispose();
	}

	@Override
	public boolean contains(ISchedulingRule rule) {
		return rule == this;
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		return rule == this;
	}

	@Override
	public ShowInContext getShowInContext() {
		if (diffViewer != null && diffViewer.getControl().isFocusControl()) {
			return diffViewer.getShowInContext();
		}
		return null;
	}

	@Override
	public void setFocus() {
		if (diffViewer != null) {
			diffViewer.getControl().setFocus();
		}
	}
}
