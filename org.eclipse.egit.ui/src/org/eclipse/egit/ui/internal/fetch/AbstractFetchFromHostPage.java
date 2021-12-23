/*******************************************************************************
 * Copyright (c) 2010, 2021 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Marc Khouzam (Ericsson)  - Add an option not to checkout the new branch
 *    Thomas Wolf <thomas.wolf@paranor.ch>
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.UIUtils.ExplicitContentProposalAdapter;
import org.eclipse.egit.ui.internal.ActionUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.egit.ui.internal.commit.command.CherryPickUI;
import org.eclipse.egit.ui.internal.components.BranchNameNormalizer;
import org.eclipse.egit.ui.internal.dialogs.AbstractBranchSelectionDialog;
import org.eclipse.egit.ui.internal.dialogs.BranchEditDialog;
import org.eclipse.egit.ui.internal.dialogs.CancelableFuture;
import org.eclipse.egit.ui.internal.dialogs.NonBlockingWizardDialog;
import org.eclipse.egit.ui.internal.gerrit.GerritDialogSettings;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Fetch a change from a remote host.
 */
public abstract class AbstractFetchFromHostPage extends WizardPage {

	enum CheckoutMode {
		CREATE_BRANCH, CREATE_TAG, CHECKOUT_FETCH_HEAD, CHERRY_PICK, NOCHECKOUT
	}

	private final @NonNull Repository repository;

	private final IDialogSettings settings;

	private final String lastUriKey;

	private Combo uriCombo;

	private Map<String, CancelableFuture<Collection<Change>>> changeRefs = new HashMap<>();

	private Map<String, AtomicBoolean> gettingRefs = new HashMap<>();

	private Text refText;

	private Button createBranch;

	private Button createTag;

	private Button checkoutFetchHead;

	private Button cherryPickFetchHead;

	private Button updateFetchHead;

	private Label placeholder;

	private Label tagTextlabel;

	private Text tagText;

	private Label branchTextlabel;

	private Text branchText;

	private String initialRefText;

	private Composite warningAdditionalRefNotActive;

	private Button activateAdditionalRefs;

	private IInputValidator branchValidator;

	private IInputValidator tagValidator;

	private Button branchEditButton;

	private Button branchCheckoutButton;

	private ExplicitContentProposalAdapter contentProposer;

	private boolean branchTextEdited;

	private boolean refTextEdited;

	private boolean tagTextEdited;

	private boolean fetching;

	private boolean supportsCherryPick;

	private String changeLabel;

	private String changeNameSingular;

	private String changeNamePlural;

	/**
	 * @param repository
	 * @param initialText
	 *            initial value for the ref field
	 * @param changeLabel
	 *            label of the change (or ref) input field
	 * @param changeNameSingular
	 *            term to use for a change in the UI
	 * @param changeNamePlural
	 *            plural of {@code changeNameSingular}
	 * @param supportsCherryPick
	 *            whether the "cherry-pick" option should be displayed
	 */
	public AbstractFetchFromHostPage(Repository repository, String initialText,
			String changeLabel, String changeNameSingular,
			String changeNamePlural, boolean supportsCherryPick) {
		super(AbstractFetchFromHostPage.class.getName());
		Assert.isNotNull(repository);
		this.repository = repository;
		this.initialRefText = initialText;
		this.supportsCherryPick = supportsCherryPick;
		this.changeLabel = changeLabel;
		this.changeNameSingular = changeNameSingular;
		this.changeNamePlural = changeNamePlural;
		settings = getDialogSettings();
		lastUriKey = repository + GerritDialogSettings.LAST_URI_SUFFIX;

		branchValidator = ValidationUtils.getRefNameInputValidator(repository,
				Constants.R_HEADS, true);
		tagValidator = ValidationUtils.getRefNameInputValidator(repository,
				Constants.R_TAGS, true);
		setTitle(
				MessageFormat.format(UIText.AbstractFetchFromHostPage_PageTitle,
						RepositoryUtil.INSTANCE.getRepositoryName(repository)));
		setMessage(MessageFormat.format(
				UIText.AbstractFetchFromHostPage_PageMessage,
				changeNameSingular));
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		return GerritDialogSettings
				.getSection(GerritDialogSettings.FETCH_FROM_GERRIT_SECTION);
	}

	@Override
	public void createControl(Composite parent) {
		parent.addDisposeListener(event -> {
			changeRefs.values()
					.forEach(l -> l
							.cancel(CancelableFuture.CancelMode.INTERRUPT));
			changeRefs.clear();
		});
		Defaults defaults = null;
		if (initialRefText != null) {
			defaults = getDefaults(initialRefText);
		}
		SelectionAdapter validatePage = new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (placeholder != null) {
					placeholder.setVisible(false);
					((GridData) placeholder.getLayoutData()).exclude = true;
				}
				checkPage();
			}
		};
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		new Label(main, SWT.NONE)
				.setText(UIText.AbstractFetchFromHostPage_UriLabel);
		uriCombo = new Combo(main, SWT.DROP_DOWN);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(uriCombo);
		uriCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String uriText = uriCombo.getText();
				CancelableFuture<Collection<Change>> list = changeRefs
						.get(uriText);
				if (list != null) {
					list.cancel(CancelableFuture.CancelMode.INTERRUPT);
				}
				list = createChangeList(repository, uriText);
				changeRefs.put(uriText, list);
				gettingRefs.put(uriText, new AtomicBoolean());
				preFetch(list);
			}
		});
		new Label(main, SWT.NONE).setText(changeLabel);
		refText = new Text(main, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(refText);
		contentProposer = addRefContentProposalToText(refText);
		// C.f. https://bugs.eclipse.org/bugs/show_bug.cgi?id=273470
		refText.addVerifyListener(
				event -> event.text = Utils.toSingleLine(event.text).trim());

		final Group checkoutGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		checkoutGroup.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().span(3, 1).grab(true, false)
				.applyTo(checkoutGroup);
		checkoutGroup.setText(UIText.AbstractFetchFromHostPage_AfterFetchGroup);

		// radio: create local branch
		createBranch = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(createBranch);
		createBranch.setText(UIText.AbstractFetchFromHostPage_LocalBranchRadio);
		createBranch.addSelectionListener(validatePage);

		branchCheckoutButton = new Button(checkoutGroup, SWT.CHECK);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.END, SWT.CENTER)
				.applyTo(branchCheckoutButton);
		branchCheckoutButton.setFont(JFaceResources.getDialogFont());
		branchCheckoutButton
				.setText(UIText.AbstractFetchFromHostPage_LocalBranchCheckout);
		branchCheckoutButton.setSelection(true);

		branchTextlabel = new Label(checkoutGroup, SWT.NONE);
		GridDataFactory.defaultsFor(branchTextlabel).exclude(false)
				.applyTo(branchTextlabel);
		branchTextlabel
				.setText(UIText.AbstractFetchFromHostPage_BranchNameText);
		branchText = new Text(checkoutGroup, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false)
				.align(SWT.FILL, SWT.CENTER).applyTo(branchText);
		branchText.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				branchTextEdited = true;
			}
		});
		branchText.addVerifyListener(event -> {
			if (event.text.isEmpty()) {
				branchTextEdited = false;
			}
		});
		branchText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});
		BranchNameNormalizer normalizer = new BranchNameNormalizer(branchText);
		normalizer.setVisible(false);
		branchEditButton = new Button(checkoutGroup, SWT.PUSH);
		branchEditButton.setFont(JFaceResources.getDialogFont());
		branchEditButton
				.setText(UIText.AbstractFetchFromHostPage_BranchEditButton);
		branchEditButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent selectionEvent) {
				String txt = branchText.getText();
				String refToMark = txt.isEmpty() ? null : Constants.R_HEADS + txt;
				AbstractBranchSelectionDialog dlg = new BranchEditDialog(
						checkoutGroup.getShell(), repository, refToMark);
				if (dlg.open() == Window.OK) {
					branchText.setText(Repository.shortenRefName(dlg
							.getRefName()));
					branchTextEdited = true;
				} else {
					// force calling branchText's modify listeners
					branchText.setText(branchText.getText());
				}
			}
		});
		GridDataFactory.defaultsFor(branchEditButton).exclude(false)
				.applyTo(branchEditButton);

		// radio: create tag
		createTag = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(createTag);
		createTag.setText(UIText.AbstractFetchFromHostPage_TagRadio);
		createTag.addSelectionListener(validatePage);

		tagTextlabel = new Label(checkoutGroup, SWT.NONE);
		GridDataFactory.defaultsFor(tagTextlabel).exclude(true)
				.applyTo(tagTextlabel);
		tagTextlabel.setText(UIText.AbstractFetchFromHostPage_TagNameText);
		tagText = new Text(checkoutGroup, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().exclude(true).grab(true, false)
				.applyTo(tagText);
		tagText.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				tagTextEdited = true;
			}
		});
		tagText.addVerifyListener(event -> {
			if (event.text.isEmpty()) {
				tagTextEdited = false;
			}
		});
		tagText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});
		BranchNameNormalizer tagNormalizer = new BranchNameNormalizer(tagText,
				UIText.BranchNameNormalizer_TooltipForTag);
		tagNormalizer.setVisible(false);

		// radio: checkout FETCH_HEAD
		checkoutFetchHead = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(checkoutFetchHead);
		checkoutFetchHead
				.setText(UIText.AbstractFetchFromHostPage_CheckoutRadio);
		checkoutFetchHead.addSelectionListener(validatePage);

		// radio: cherry-pick onto current HEAD
		if (supportsCherryPick) {
			try {
				String headName = repository.getBranch();
				if (headName != null) {
					cherryPickFetchHead = new Button(checkoutGroup, SWT.RADIO);
					GridDataFactory.fillDefaults().span(3, 1)
							.applyTo(cherryPickFetchHead);
					cherryPickFetchHead.setText(MessageFormat.format(
							UIText.AbstractFetchFromHostPage_CherryPickRadio,
							headName));
					cherryPickFetchHead.addSelectionListener(validatePage);
				}
			} catch (IOException e) {
				Activator.logError(e.getLocalizedMessage(), e);
			}
		}

		// radio: don't checkout
		updateFetchHead = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(updateFetchHead);
		updateFetchHead.setText(UIText.AbstractFetchFromHostPage_UpdateRadio);
		updateFetchHead.addSelectionListener(validatePage);

		CheckoutMode mode = defaults == null ? null : defaults.getCommand();
		// If not createBranch or createTag, add a placeHolder so that the
		// initial dialog is large enough
		if (mode == null) {
			createBranch.setSelection(true);
		} else {
			switch (mode) {
			case CHECKOUT_FETCH_HEAD:
				checkoutFetchHead.setSelection(true);
				placeholder = new Label(main, SWT.NONE);
				GridDataFactory.fillDefaults().span(2, 1).applyTo(placeholder);
				break;
			case CHERRY_PICK:
				if (cherryPickFetchHead != null) {
					cherryPickFetchHead.setSelection(true);
					placeholder = new Label(main, SWT.NONE);
					GridDataFactory.fillDefaults().span(2, 1)
							.applyTo(placeholder);
				} else {
					createBranch.setSelection(true);
				}
				break;
			default:
				createBranch.setSelection(true);
				break;
			}
		}

		warningAdditionalRefNotActive = new Composite(main, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false)
				.exclude(true).applyTo(warningAdditionalRefNotActive);
		warningAdditionalRefNotActive.setLayout(new GridLayout(2, false));
		warningAdditionalRefNotActive.setVisible(false);

		activateAdditionalRefs = new Button(warningAdditionalRefNotActive,
				SWT.CHECK);
		activateAdditionalRefs
				.setText(
						UIText.AbstractFetchFromHostPage_ActivateAdditionalRefsButton);
		activateAdditionalRefs
				.setToolTipText(
						UIText.AbstractFetchFromHostPage_ActivateAdditionalRefsTooltip);

		ActionUtils.setGlobalActions(refText, ActionUtils.createGlobalAction(
				ActionFactory.PASTE, () -> {
					if (doPaste(refText) && contentProposer != null) {
						refText.getDisplay().asyncExec(() -> {
							if (!refText.isDisposed()) {
								contentProposer.openProposalPopup();
							}
						});
					}
				}));
		refText.addModifyListener(event -> {
			refTextEdited = true;
			Change change = changeFromString(refText.getText());
			String suggestion = ""; //$NON-NLS-1$
			if (change != null) {
				suggestion = change.getBranchSuggestion();
			}
			if (!branchTextEdited) {
				branchText.setText(suggestion);
			}
			if (!tagTextEdited) {
				tagText.setText(suggestion);
			}
			checkPage();
		});
		Change defaultChange = defaults == null ? null : defaults.getChange();
		if (defaultChange != null) {
			String ref = defaultChange.getRefName();
			if (ref != null) {
				refText.setText(ref);
			} else {
				refText.setText(
						Long.toString(defaultChange.getChangeNumber()));
			}
		}
		refTextEdited = false;

		String defaultUri = defaults == null ? null : defaults.getUri();
		Set<String> upstream = determineUris(repository, defaultUri);
		upstream.stream().sorted().forEach(u -> {
			uriCombo.add(u);
			changeRefs.put(u, createChangeList(repository, u));
			gettingRefs.put(u, new AtomicBoolean());
		});
		if (defaultUri != null) {
			uriCombo.setText(defaultUri);
		} else {
			selectLastUsedUri();
		}
		String currentUri = uriCombo.getText();
		CancelableFuture<Collection<Change>> list = changeRefs.get(currentUri);
		if (list == null) {
			list = createChangeList(repository, currentUri);
			changeRefs.put(currentUri, list);
			gettingRefs.put(currentUri, new AtomicBoolean());
		}
		preFetch(list);
		refText.setFocus();
		Dialog.applyDialogFont(main);
		setControl(main);
		if (defaultChange != null && !defaultChange.isComplete()) {
			// Launch content assist when the page is displayed
			final IWizardContainer container = getContainer();
			if (container instanceof IPageChangeProvider) {
				((IPageChangeProvider) container)
						.addPageChangedListener(new IPageChangedListener() {
							@Override
							public void pageChanged(PageChangedEvent event) {
								if (event
										.getSelectedPage() == AbstractFetchFromHostPage.this) {
									// Only the first time: remove myself
									event.getPageChangeProvider()
											.removePageChangedListener(this);
									getControl().getDisplay()
											.asyncExec(new Runnable() {
										@Override
										public void run() {
											Control control = getControl();
											if (control != null
													&& !control.isDisposed()) {
												contentProposer
														.openProposalPopup();
											}
										}
									});
								}
							}
						});
			}
		}
		checkPage();
	}

	/**
	 * Determines the eligible upstream URIs.
	 *
	 * @param repo
	 *            to get the URIs for
	 * @param defaultUri
	 *            optional URI determined from the initial input, may be
	 *            {@code null} if none
	 * @return a set of URIs
	 */
	abstract Set<String> determineUris(Repository repo,
			String defaultUri);

	/**
	 * Creates a {@link CancelableFuture} for a repository and an upstream URI.
	 *
	 * @param repo
	 *            local clone
	 * @param uri
	 *            of an upstream
	 * @return a {@link CancelableFuture}
	 */
	abstract CancelableFuture<Collection<Change>> createChangeList(
			Repository repo, String uri);

	/**
	 * Creates a {@link Change} from a git ref.
	 *
	 * @param refName
	 *            to create the change from
	 * @return the {@link Change}, or {@code null} if no change can be
	 *         determined
	 */
	abstract Change changeFromRef(String refName);

	/**
	 * Creates a {@link Change} from a general string.
	 *
	 * @param input
	 *            to create the change from
	 * @return the {@link Change}, or {@code null} if no change can be
	 *         determined
	 */
	abstract Change changeFromString(String input);

	/**
	 * A data transfer object collecting some default values.
	 */
	static class Defaults {

		private final String uri;

		private final CheckoutMode command;

		private final Change change;

		/**
		 * Creates a new {@link Defaults} object from the arguments.
		 *
		 * @param uri
		 *            to set
		 * @param command
		 *            to set
		 * @param change
		 *            to set
		 */
		public Defaults(String uri, CheckoutMode command, Change change) {
			this.uri = uri;
			this.command = command;
			this.change = change;
		}

		/**
		 * Retrieves the default remote URI.
		 *
		 * @return the URI or {@code null}
		 */
		public String getUri() {
			return uri;
		}

		/**
		 * Retrieves the default command.
		 *
		 * @return the command or {@code null}
		 */
		public CheckoutMode getCommand() {
			return command;
		}

		/**
		 * Retrieves the default change.
		 *
		 * @return the {@link Change} or {@code null}
		 */
		public Change getChange() {
			return change;
		}
	}

	/**
	 * Determines some defaults from the initial input.
	 *
	 * @param initialText
	 *            initial input
	 * @return the {@link Defaults}, or null if none can be determined
	 */
	abstract Defaults getDefaults(String initialText);

	/**
	 * Creates a {@link Pattern} to select content assist proposals. The pattern
	 * will be matched against the ref name of a {@link Change}.
	 *
	 * @param input
	 *            text to create the pattern from
	 * @return the pattern, or {@code null} to match any ref
	 */
	abstract Pattern getProposalPattern(String input);

	private void preFetch(CancelableFuture<?> list) {
		try {
			list.start();
		} catch (InvocationTargetException e) {
			Activator.handleError(e.getLocalizedMessage(), e.getCause(), true);
		}
	}

	/**
	 * Paste text from the clipboard into the {@code text} field. If possible,
	 * determine the patch set number automatically. If only a change number can
	 * be determined, return {@code true} to indicate to the caller that a
	 * content assist might be helpful.
	 *
	 * @param text
	 *            {@link Text} field to paste into
	 * @return whether invoking a content assist might be helpful to the user
	 */
	private boolean doPaste(Text text) {
		Clipboard clipboard = new Clipboard(text.getDisplay());
		try {
			String clipText = (String) clipboard
					.getContents(TextTransfer.getInstance());
			if (clipText != null) {
				Change input = changeFromString(clipText.trim());
				if (input != null) {
					String toInsert = Long.toString(input.getChangeNumber());
					boolean replacesEverything = text.getText().trim().isEmpty()
							|| text.getSelectionText().equals(text.getText());
					boolean openContentAssist = false;
					if (input.isComplete()) {
						if (replacesEverything) {
							toInsert = input.getRefName();
						} else {
							toInsert = input.completeId();
						}
					} else {
						openContentAssist = replacesEverything;
					}
					clipboard.setContents(new Object[] { toInsert },
							new Transfer[] { TextTransfer.getInstance() });
					try {
						text.paste();
						refTextEdited = false;
					} finally {
						clipboard.setContents(new Object[] { clipText },
								new Transfer[] { TextTransfer.getInstance() });
					}
					return openContentAssist;
				} else {
					text.paste();
				}
			}
		} finally {
			clipboard.dispose();
		}
		return false;
	}

	/**
	 * Retrieves a suffix for the settings key used to store the last visited
	 * URL. Different wizards might want to return different keys, for instance
	 * depending on the type of server being fetched from, to avoid that
	 * settings from one fetch wizard affect other fetch wizards. (For instance,
	 * if a repository has both Gerrit and Github remotes, the selection in the
	 * "Fetch from Gerrit" wizard should probably not affect the "Fetch Github
	 * Pull Request" wizard, and vice versa.)
	 * <p>
	 * Note: for historical reasons, "Fetch from Gerrit" uses an empty string.
	 * </p>
	 *
	 * @return the key, or an empty string if none.
	 */
	protected String getSettingsKey() {
		return ""; //$NON-NLS-1$
	}

	private void storeLastUsedUri(String uri) {
		settings.put(lastUriKey + getSettingsKey(), uri.trim());
	}

	private void selectLastUsedUri() {
		String lastUri = settings.get(lastUriKey + getSettingsKey());
		if (lastUri != null) {
			int i = uriCombo.indexOf(lastUri);
			if (i != -1) {
				uriCombo.select(i);
				return;
			}
		}
		uriCombo.select(0);
	}

	private void checkPage() {
		boolean createBranchSelected = createBranch.getSelection();
		branchText.setEnabled(createBranchSelected);
		branchText.setVisible(createBranchSelected);
		branchTextlabel.setVisible(createBranchSelected);
		branchEditButton.setVisible(createBranchSelected);
		branchCheckoutButton.setVisible(createBranchSelected);
		GridData gd = (GridData) branchText.getLayoutData();
		gd.exclude = !createBranchSelected;
		gd = (GridData) branchTextlabel.getLayoutData();
		gd.exclude = !createBranchSelected;
		gd = (GridData) branchEditButton.getLayoutData();
		gd.exclude = !createBranchSelected;
		gd = (GridData) branchCheckoutButton.getLayoutData();
		gd.exclude = !createBranchSelected;

		boolean createTagSelected = createTag.getSelection();
		tagText.setEnabled(createTagSelected);
		tagText.setVisible(createTagSelected);
		tagTextlabel.setVisible(createTagSelected);
		gd = (GridData) tagText.getLayoutData();
		gd.exclude = !createTagSelected;
		gd = (GridData) tagTextlabel.getLayoutData();
		gd.exclude = !createTagSelected;
		branchText.getParent().layout(true);

		boolean showActivateAdditionalRefs = false;
		showActivateAdditionalRefs = (checkoutFetchHead.getSelection() || updateFetchHead
				.getSelection())
				&& !Activator
						.getDefault()
						.getPreferenceStore()
						.getBoolean(
								UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS);

		gd = (GridData) warningAdditionalRefNotActive.getLayoutData();
		gd.exclude = !showActivateAdditionalRefs;
		warningAdditionalRefNotActive.setVisible(showActivateAdditionalRefs);
		warningAdditionalRefNotActive.getParent().layout(true);

		setErrorMessage(null);
		try {
			if (refText.getText().length() > 0) {
				Change change = changeFromRef(refText.getText());
				if (change == null) {
					change = changeFromString(refText.getText());
					if (change == null) {
						setErrorMessage(MessageFormat.format(
								UIText.AbstractFetchFromHostPage_MissingChangeMessage,
								changeNameSingular));
						return;
					}
				}
				CancelableFuture<Collection<Change>> list = changeRefs
						.get(uriCombo.getText());
				if (list != null && list.isDone()) {
					try {
						Collection<Change> changes = list.get();
						if (change.isComplete()) {
							if (changes == null || !changes.contains(change)) {
								setErrorMessage(MessageFormat.format(
										UIText.AbstractFetchFromHostPage_UnknownChangeRefMessage,
										changeNameSingular));
								return;
							}
						} else {
							Change fromGerrit = change.complete(changes);
							if (fromGerrit == null) {
								setErrorMessage(MessageFormat.format(
										UIText.AbstractFetchFromHostPage_NoSuchChangeMessage,
										changeNameSingular, Long.toString(
												change.getChangeNumber())));
								return;
							}
						}
					} catch (InterruptedException
							| InvocationTargetException e) {
						// Ignore: since we're done, this should never occur
					}
				}
			} else {
				setErrorMessage(MessageFormat.format(
						UIText.AbstractFetchFromHostPage_MissingChangeMessage,
						changeNameSingular));
				return;
			}

			if (createBranchSelected) {
				setErrorMessage(branchValidator.isValid(branchText.getText()));
			} else if (createTagSelected) {
				setErrorMessage(tagValidator.isValid(tagText.getText()));
			}
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

	private Collection<Change> getRefsForContentAssist()
			throws InvocationTargetException, InterruptedException {
		String uriText = uriCombo.getText();
		if (!changeRefs.containsKey(uriText)) {
			// TODO: changeRefs must be read-only? Otherwise, we'd need a modify
			// listener, and check that it's a valid URI...
			// If read-only, the default URI must be added explicitly up front
			// to the combo.
			changeRefs.put(uriText, createChangeList(repository, uriText));
			gettingRefs.put(uriText, new AtomicBoolean());
		}
		CancelableFuture<Collection<Change>> list = changeRefs.get(uriText);
		if (!list.isFinished()) {
			if (!gettingRefs.get(uriText).compareAndSet(false, true)) {
				// We're already getting it
				return null;
			}
			IWizardContainer container = getContainer();
			IRunnableWithProgress operation = monitor -> {
				monitor.beginTask(MessageFormat.format(
						UIText.AsynchronousRefProposalProvider_FetchingRemoteRefsMessage,
						uriText), IProgressMonitor.UNKNOWN);
				Collection<Change> result = list.get();
				if (monitor.isCanceled()) {
					return;
				}
				// If we get here, the ChangeList future is done.
				if (result == null || result.isEmpty() || fetching) {
					// Don't bother if we didn't get any results
					return;
				}
				// If we do have results now, open the proposals.
				Job showProposals = new WorkbenchJob(
						UIText.AsynchronousRefProposalProvider_ShowingProposalsJobName) {

					@Override
					public IStatus runInUIThread(IProgressMonitor uiMonitor) {
						// But only if we're not disposed, the focus is still
						// (or again) in the Change field, and the uri is still
						// the same
						try {
							if (container instanceof NonBlockingWizardDialog) {
								// Otherwise the dialog was blocked anyway, and
								// focus will be restored
								if (fetching) {
									return Status.CANCEL_STATUS;
								}
								String uriNow = uriCombo.getText();
								if (!uriNow.equals(uriText)) {
									return Status.CANCEL_STATUS;
								}
								if (refText != refText.getDisplay()
										.getFocusControl()) {
									refTextEdited = false;
									fillInPatchSet(result);
									return Status.CANCEL_STATUS;
								}
							}
							fillInPatchSet(result);
							contentProposer.openProposalPopup();
						} catch (SWTException e) {
							// Disposed already
							return Status.CANCEL_STATUS;
						} finally {
							uiMonitor.done();
						}
						return Status.OK_STATUS;
					}

				};
				showProposals.schedule();
			};
			if (container instanceof NonBlockingWizardDialog) {
				NonBlockingWizardDialog dialog = (NonBlockingWizardDialog) container;
				dialog.run(operation,
						() -> {
							if (!fetching) {
								list.cancel(
										CancelableFuture.CancelMode.ABANDON);
							}
						});
			} else {
				container.run(true, true, operation);
			}
			return null;
		}
		// ChangeList is already here, so get() won't block
		Collection<Change> changes = list.get();
		fillInPatchSet(changes);
		return changes;
	}

	private void fillInPatchSet(Collection<Change> changes) {
		if (refTextEdited || contentProposer.isProposalPopupOpen()) {
			// User has modified the text: don't interfere
			return;
		}
		Change change = changeFromString(refText.getText());
		if (change != null && !change.isComplete()) {
			Change fromGerrit = change.complete(changes);
			if (fromGerrit != null) {
				String fullRef = fromGerrit.getRefName();
				refText.setText(fullRef);
				refTextEdited = false;
				refText.setSelection(fullRef.length());
			}
		}
	}

	boolean doFetch() {
		fetching = true;
		final Change change = changeFromString(refText.getText());
		final String uri = uriCombo.getText();
		// If we have an incomplete change (missing patch set number), remove
		// the change list future from the global map so that it won't be
		// interrupted when the dialog closes.
		final CancelableFuture<Collection<Change>> changeList = change
				.isComplete() ? null
				: changeRefs.remove(uri);
		if (changeList != null) {
			// Make sure a pending get() from the content assist gets aborted
			changeList.cancel(CancelableFuture.CancelMode.ABANDON);
		}
		final CheckoutMode mode = getCheckoutMode();
		final boolean doCheckoutNewBranch = (mode == CheckoutMode.CREATE_BRANCH)
				&& branchCheckoutButton.getSelection();
		final boolean doActivateAdditionalRefs = showAdditionalRefs();
		final String textForTag = tagText.getText();
		final String textForBranch = branchText.getText();

		String taskName = MessageFormat.format(
				UIText.AbstractFetchFromHostPage_GetChangeTaskName,
				changeNamePlural);
		Job job = new Job(taskName) {

			@Override
			public IStatus run(IProgressMonitor monitor) {
				try {
					int steps = getTotalWork(mode);
					SubMonitor progress = SubMonitor.convert(monitor, taskName,
							steps + 1);
					Change finalChange = change;
					if (changeList != null) {
						finalChange = change.complete(changeList, uri,
								progress.newChild(1));
					}
					if (finalChange == null) {
						// Returning an error status would log the message
						Activator.showError(MessageFormat.format(
								UIText.AbstractFetchFromHostPage_NoSuchChangeMessage,
								changeNameSingular,
								Long.toString(change.getChangeNumber())),
								null);
						return Status.CANCEL_STATUS;
					}
					final RefSpec spec = new RefSpec()
							.setSource(finalChange.getRefName())
							.setDestination(Constants.FETCH_HEAD);
					if (progress.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					RevCommit commit = fetchChange(uri, spec,
							progress.newChild(1));
					if (mode != CheckoutMode.NOCHECKOUT && commit != null) {
						IWorkspace workspace = ResourcesPlugin.getWorkspace();
						IWorkspaceRunnable operation = new IWorkspaceRunnable() {

							@Override
							public void run(IProgressMonitor innerMonitor)
									throws CoreException {
								SubMonitor innerProgress = SubMonitor
										.convert(innerMonitor, steps);
								switch (mode) {
								case CHECKOUT_FETCH_HEAD:
									checkout(commit.name(),
											innerProgress.newChild(1));
									break;
								case CREATE_TAG:
									assert spec != null;
									assert textForTag != null;
									createTag(spec, textForTag, commit,
											innerProgress.newChild(1));
									checkout(commit.name(),
											innerProgress.newChild(1));
									break;
								case CREATE_BRANCH:
									createBranch(textForBranch,
											doCheckoutNewBranch, commit,
											innerProgress.newChild(1));
									break;
								case CHERRY_PICK:
									cherryPick(commit,
											innerProgress.newChild(1));
									break;
								default:
									break;
								}
							}
						};
						workspace.run(operation, null, IWorkspace.AVOID_UPDATE,
								progress.newChild(steps));
					}
					if (doActivateAdditionalRefs) {
						activateAdditionalRefs();
					}
					if (mode == CheckoutMode.NOCHECKOUT) {
						// Tell the world that FETCH_HEAD only changed. In other
						// cases, JGit will have sent a RefsChangeEvent
						// already.
						repository.fireEvent(new FetchHeadChangedEvent());
					}
					storeLastUsedUri(uri);
				} catch (OperationCanceledException oe) {
					return Status.CANCEL_STATUS;
				} catch (CoreException ce) {
					return ce.getStatus();
				} catch (Exception e) {
					return Activator.createErrorStatus(e.getLocalizedMessage(),
							e);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}

			@Override
			protected void canceling() {
				super.canceling();
				if (changeList != null) {
					changeList.cancel(CancelableFuture.CancelMode.INTERRUPT);
				}
			}

			private int getTotalWork(final CheckoutMode m) {
				switch (m) {
				case CHECKOUT_FETCH_HEAD:
				case CREATE_BRANCH:
				case CHERRY_PICK:
					return 2;
				case CREATE_TAG:
					return 3;
				default:
					return 1;
				}
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.FETCH.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.schedule();
		return true;
	}

	private boolean showAdditionalRefs() {
		return (checkoutFetchHead.getSelection()
				|| cherryPickFetchHead != null
						&& cherryPickFetchHead.getSelection()
				|| updateFetchHead.getSelection())
				&& activateAdditionalRefs.getSelection();
	}

	private CheckoutMode getCheckoutMode() {
		if (createBranch.getSelection()) {
			return CheckoutMode.CREATE_BRANCH;
		} else if (createTag.getSelection()) {
			return CheckoutMode.CREATE_TAG;
		} else if (checkoutFetchHead.getSelection()) {
			return CheckoutMode.CHECKOUT_FETCH_HEAD;
		} else if (cherryPickFetchHead != null
				&& cherryPickFetchHead.getSelection()) {
			return CheckoutMode.CHERRY_PICK;
		} else {
			return CheckoutMode.NOCHECKOUT;
		}
	}

	private RevCommit fetchChange(String uri, RefSpec spec,
			IProgressMonitor monitor) throws CoreException, URISyntaxException,
			IOException {
		List<RefSpec> specs = new ArrayList<>(1);
		specs.add(spec);

		String taskName = MessageFormat.format(
				UIText.AbstractFetchFromHostPage_FetchingTaskName,
				changeNameSingular, spec.getSource());
		monitor.subTask(taskName);
		FetchResult fetchRes = new FetchOperationUI(repository, new URIish(uri),
				specs, false).execute(monitor);

		monitor.worked(1);
		try (RevWalk rw = new RevWalk(repository)) {
			return rw.parseCommit(
					fetchRes.getAdvertisedRef(spec.getSource()).getObjectId());
		}
	}

	private void createTag(@NonNull RefSpec spec, @NonNull String textForTag,
			@NonNull RevCommit commit, IProgressMonitor monitor)
			throws CoreException {
		monitor.subTask(UIText.AbstractFetchFromHostPage_CreatingTagTaskName);
		PersonIdent personIdent = new PersonIdent(repository);

		TagOperation operation = new TagOperation(repository)
				.setAnnotated(true)
				.setName(textForTag)
				.setTagger(personIdent)
				.setMessage(MessageFormat.format(
						UIText.AbstractFetchFromHostPage_GeneratedTagMessage,
						changeNameSingular, spec.getSource()))
				.setTarget(commit)
				.setSign(Boolean.FALSE);
		operation.execute(monitor);
		monitor.worked(1);
	}

	private void createBranch(final String textForBranch, boolean doCheckout,
			RevCommit commit, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, doCheckout ? 10 : 2);
		progress.subTask(
				UIText.AbstractFetchFromHostPage_CreatingBranchTaskName);
		CreateLocalBranchOperation bop = new CreateLocalBranchOperation(
				repository, textForBranch, commit);
		bop.execute(progress.newChild(2));
		if (doCheckout) {
			checkout(textForBranch, progress.newChild(8));
		}
	}

	private void checkout(String targetName, IProgressMonitor monitor)
			throws CoreException {
		monitor.subTask(MessageFormat.format(
				UIText.AbstractFetchFromHostPage_CheckingOutTaskName,
				changeNameSingular));
		BranchOperationUI.checkout(repository, targetName).run(monitor);
		monitor.worked(1);
	}

	private void cherryPick(@NonNull RevCommit commit,
			IProgressMonitor monitor) {
		String taskName = MessageFormat.format(
				UIText.AbstractFetchFromHostPage_CherryPickTaskName,
				changeNameSingular);
		monitor.subTask(taskName);
		WorkbenchJob job = new WorkbenchJob(
				PlatformUI.getWorkbench().getDisplay(),
				taskName) {

			@Override
			public IStatus runInUIThread(IProgressMonitor progress) {
				try {
					CherryPickUI ui = new CherryPickUI();
					ui.run(repository, commit, false);
				} catch (CoreException e) {
					return Activator.createErrorStatus(e.getLocalizedMessage(),
							e);
				} finally {
					progress.done();
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		monitor.worked(1);
	}

	private void activateAdditionalRefs() {
		Activator.getDefault().getPreferenceStore().setValue(
				UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS, true);
	}

	private ExplicitContentProposalAdapter addRefContentProposalToText(
			final Text textField) {
		return UIUtils.addContentProposalToText(textField, () -> {
			try {
				return getRefsForContentAssist();
			} catch (InvocationTargetException e) {
				Activator.handleError(e.getMessage(), e, true);
				return null;
			} catch (InterruptedException e) {
				return null;
			}
		}, (pattern, change) -> {
			if (pattern == null
					|| pattern.matcher(change.getRefName()).matches()) {
				return change.getProposal();
			}
			return null;
		}, this::getProposalPattern, null,
				MessageFormat.format(
						UIText.AbstractFetchFromHostPage_ContentAssistTooltip,
						"{0}", changeNamePlural)); //$NON-NLS-1$
	}

	/**
	 * An abstraction for a "change" such as a Gerrit patch set or a pull
	 * request.
	 */
	protected interface Change extends Comparable<Change> {

		/**
		 * Retrieves a Ref name for the change.
		 *
		 * @return the Ref name
		 */
		String getRefName();

		/**
		 * Retrieves the change number.
		 *
		 * @return the change number
		 */
		long getChangeNumber();

		/**
		 * Produces an {@link IContentProposal} for the {@link Change}.
		 *
		 * @return the {@link IContentProposal}
		 */
		IContentProposal getProposal();

		/**
		 * Tells whether the change is complete. If not, it may need to be
		 * completed via one of the two {@code complete()} methods.
		 *
		 * @return whether the change is completely defined
		 */
		boolean isComplete();

		/**
		 * Completes an incomplete change with the result from the given
		 * {@code list}.
		 *
		 * @param list
		 *            to use to complete the change
		 * @param uri
		 *            of the remote
		 * @param monitor
		 *            for progress reporting and cancellation
		 * @return a completed change
		 */
		Change complete(CancelableFuture<Collection<Change>> list, String uri,
				IProgressMonitor monitor);

		/**
		 * If the change is not {@link #isComplete() complete}, completes it
		 * with information from the given {code changes}, which is supposed to
		 * be the result obtained from a remote host.
		 *
		 * @param changes
		 *            to use to complete this change
		 * @return a new completed change
		 */
		Change complete(Collection<Change> changes);

		/**
		 * Retrieves a suggested branch name for the change.
		 *
		 * @return the branch name
		 */
		String getBranchSuggestion();

		/**
		 * Retrieves the complete ID of the change. This may be more than just
		 * the {@link #getChangeNumber() change number}, for instance, it may
		 * include the patch set number for a Gerrit change. This is used only
		 * when pasting text into the ref field.
		 *
		 * @return the complete ID
		 */
		String completeId();
	}
}
