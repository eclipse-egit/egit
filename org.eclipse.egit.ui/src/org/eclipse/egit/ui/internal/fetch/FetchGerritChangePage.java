/*******************************************************************************
 * Copyright (c) 2010, 2018 SAP AG and others.
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
import org.eclipse.egit.core.internal.gerrit.GerritUtil;
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
import org.eclipse.egit.ui.internal.components.AsynchronousListOperation;
import org.eclipse.egit.ui.internal.components.BranchNameNormalizer;
import org.eclipse.egit.ui.internal.dialogs.AbstractBranchSelectionDialog;
import org.eclipse.egit.ui.internal.dialogs.BranchEditDialog;
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
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
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
 * Fetch a change from Gerrit
 */
public class FetchGerritChangePage extends WizardPage {

	private static final String GERRIT_CHANGE_REF_PREFIX = "refs/changes/"; //$NON-NLS-1$

	private static final Pattern GERRIT_FETCH_PATTERN = Pattern.compile(
			"git fetch (\\w+:\\S+) (refs/changes/\\d+/\\d+/\\d+) && git (\\w+) FETCH_HEAD"); //$NON-NLS-1$

	private static final Pattern GERRIT_URL_PATTERN = Pattern.compile(
			"(?:https?://\\S+?/|/)?([1-9][0-9]*)(?:/([1-9][0-9]*)(?:/([1-9][0-9]*)(?:\\.\\.\\d+)?)?)?(?:/\\S*)?"); //$NON-NLS-1$

	private static final Pattern GERRIT_CHANGE_REF_PATTERN = Pattern
			.compile("refs/changes/(\\d\\d)/([1-9][0-9]*)(?:/([1-9][0-9]*)?)?"); //$NON-NLS-1$

	private static final Pattern DIGITS = Pattern
			.compile("\\d+(?:/\\d+)?"); //$NON-NLS-1$

	private static final String WILDCARD = ".*"; //$NON-NLS-1$

	private static final SimpleDateFormat SIMPLE_TIMESTAMP = new SimpleDateFormat(
			"yyyyMMddHHmmss"); //$NON-NLS-1$

	private enum CheckoutMode {
		CREATE_BRANCH, CREATE_TAG, CHECKOUT_FETCH_HEAD, CHERRY_PICK, NOCHECKOUT
	}

	private final @NonNull Repository repository;

	private final IDialogSettings settings;

	private final String lastUriKey;

	private Combo uriCombo;

	private Map<String, ChangeList> changeRefs = new HashMap<>();

	private Text refText;

	private Button createBranch;

	private Button createTag;

	private Button checkoutFetchHead;

	private Button cherryPickFetchHead;

	private Button updateFetchHead;

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

	/**
	 * @param repository
	 * @param initialText
	 *            initial value for the ref field
	 */
	public FetchGerritChangePage(Repository repository, String initialText) {
		super(FetchGerritChangePage.class.getName());
		Assert.isNotNull(repository);
		this.repository = repository;
		this.initialRefText = initialText;
		setTitle(NLS.bind(UIText.FetchGerritChangePage_PageTitle,
				RepositoryUtil.INSTANCE.getRepositoryName(repository)));
		setMessage(UIText.FetchGerritChangePage_PageMessage);
		settings = getDialogSettings();
		lastUriKey = repository + GerritDialogSettings.LAST_URI_SUFFIX;

		branchValidator = ValidationUtils.getRefNameInputValidator(repository,
				Constants.R_HEADS, true);
		tagValidator = ValidationUtils.getRefNameInputValidator(repository,
				Constants.R_TAGS, true);
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		return GerritDialogSettings
				.getSection(GerritDialogSettings.FETCH_FROM_GERRIT_SECTION);
	}

	@Override
	public void createControl(Composite parent) {
		parent.addDisposeListener(event -> {
			for (ChangeList l : changeRefs.values()) {
				l.cancel(ChangeList.CancelMode.INTERRUPT);
			}
			changeRefs.clear();
		});
		String defaultUri = null;
		String defaultCommand = null;
		String defaultChange = null;
		Change candidateChange = null;
		if (initialRefText != null) {
			Matcher matcher = GERRIT_FETCH_PATTERN.matcher(initialRefText);
			if (matcher.matches()) {
				defaultUri = matcher.group(1);
				defaultChange = matcher.group(2);
				defaultCommand = matcher.group(3);
			} else {
				candidateChange = determineChangeFromString(
						initialRefText.trim());
			}
		}
		SelectionAdapter validatePage = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}
		};
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		new Label(main, SWT.NONE)
				.setText(UIText.FetchGerritChangePage_UriLabel);
		uriCombo = new Combo(main, SWT.DROP_DOWN);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(uriCombo);
		uriCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String uriText = uriCombo.getText();
				ChangeList list = changeRefs.get(uriText);
				if (list != null) {
					list.cancel(ChangeList.CancelMode.INTERRUPT);
				}
				list = new ChangeList(repository, uriText);
				changeRefs.put(uriText, list);
				preFetch(list);
			}
		});
		new Label(main, SWT.NONE)
				.setText(UIText.FetchGerritChangePage_ChangeLabel);
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
		checkoutGroup.setText(UIText.FetchGerritChangePage_AfterFetchGroup);

		// radio: create local branch
		createBranch = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(createBranch);
		createBranch.setText(UIText.FetchGerritChangePage_LocalBranchRadio);
		createBranch.addSelectionListener(validatePage);

		branchCheckoutButton = new Button(checkoutGroup, SWT.CHECK);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.END, SWT.CENTER)
				.applyTo(branchCheckoutButton);
		branchCheckoutButton.setFont(JFaceResources.getDialogFont());
		branchCheckoutButton
				.setText(UIText.FetchGerritChangePage_LocalBranchCheckout);
		branchCheckoutButton.setSelection(true);

		branchTextlabel = new Label(checkoutGroup, SWT.NONE);
		GridDataFactory.defaultsFor(branchTextlabel).exclude(false)
				.applyTo(branchTextlabel);
		branchTextlabel.setText(UIText.FetchGerritChangePage_BranchNameText);
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
		branchEditButton.setText(UIText.FetchGerritChangePage_BranchEditButton);
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
		createTag.setText(UIText.FetchGerritChangePage_TagRadio);
		createTag.addSelectionListener(validatePage);

		tagTextlabel = new Label(checkoutGroup, SWT.NONE);
		GridDataFactory.defaultsFor(tagTextlabel).exclude(true)
				.applyTo(tagTextlabel);
		tagTextlabel.setText(UIText.FetchGerritChangePage_TagNameText);
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
		checkoutFetchHead.setText(UIText.FetchGerritChangePage_CheckoutRadio);
		checkoutFetchHead.addSelectionListener(validatePage);

		// radio: cherry-pick onto current HEAD
		try {
			String headName = repository.getBranch();
			if (headName != null) {
				cherryPickFetchHead = new Button(checkoutGroup, SWT.RADIO);
				GridDataFactory.fillDefaults().span(3, 1)
						.applyTo(cherryPickFetchHead);
				cherryPickFetchHead.setText(MessageFormat.format(
						UIText.FetchGerritChangePage_CherryPickRadio,
						headName));
				cherryPickFetchHead.addSelectionListener(validatePage);
			}
		} catch (IOException e) {
			Activator.logError(e.getLocalizedMessage(), e);
		}

		// radio: don't checkout
		updateFetchHead = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(updateFetchHead);
		updateFetchHead.setText(UIText.FetchGerritChangePage_UpdateRadio);
		updateFetchHead.addSelectionListener(validatePage);

		if ("checkout".equals(defaultCommand)) { //$NON-NLS-1$
			checkoutFetchHead.setSelection(true);
		} else {
			createBranch.setSelection(true);
		}

		warningAdditionalRefNotActive = new Composite(main, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false)
				.exclude(true).applyTo(warningAdditionalRefNotActive);
		warningAdditionalRefNotActive.setLayout(new GridLayout(2, false));
		warningAdditionalRefNotActive.setVisible(false);

		activateAdditionalRefs = new Button(warningAdditionalRefNotActive,
				SWT.CHECK);
		activateAdditionalRefs
				.setText(UIText.FetchGerritChangePage_ActivateAdditionalRefsButton);
		activateAdditionalRefs
				.setToolTipText(
						UIText.FetchGerritChangePage_ActivateAdditionalRefsTooltip);

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
		refText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				refTextEdited = true;
				Change change = determineChangeFromString(refText.getText());
				String suggestion = ""; //$NON-NLS-1$
				if (change != null) {
					Object ps = change.getPatchSetNumber();
					if (ps == null) {
						ps = SIMPLE_TIMESTAMP.format(new Date());
					}
					suggestion = NLS.bind(
							UIText.FetchGerritChangePage_SuggestedRefNamePattern,
							change.getChangeNumber(),
							ps);
				}
				if (!branchTextEdited) {
					branchText.setText(suggestion);
				}
				if (!tagTextEdited) {
					tagText.setText(suggestion);
				}
				checkPage();
			}
		});
		if (defaultChange != null) {
			refText.setText(defaultChange);
		} else if (candidateChange != null) {
			String ref = candidateChange.getRefName();
			if (ref != null) {
				refText.setText(ref);
			} else {
				refText.setText(candidateChange.getChangeNumber().toString());
			}
		}
		refTextEdited = false;

		// get all available Gerrit URIs from the repository
		SortedSet<String> uris = new TreeSet<>();
		try {
			for (RemoteConfig rc : RemoteConfig.getAllRemoteConfigs(repository
					.getConfig())) {
				if (GerritUtil.isGerritFetch(rc)) {
					if (rc.getURIs().size() > 0) {
						uris.add(rc.getURIs().get(0).toPrivateString());
					}
					for (URIish u : rc.getPushURIs()) {
						uris.add(u.toPrivateString());
					}
				}

			}
		} catch (URISyntaxException e) {
			Activator.handleError(e.getMessage(), e, false);
			setErrorMessage(e.getMessage());
		}
		for (String aUri : uris) {
			uriCombo.add(aUri);
			changeRefs.put(aUri, new ChangeList(repository, aUri));
		}
		if (defaultUri != null) {
			uriCombo.setText(defaultUri);
		} else {
			selectLastUsedUri();
		}
		String currentUri = uriCombo.getText();
		ChangeList list = changeRefs.get(currentUri);
		if (list == null) {
			list = new ChangeList(repository, currentUri);
			changeRefs.put(currentUri, list);
		}
		preFetch(list);
		refText.setFocus();
		Dialog.applyDialogFont(main);
		setControl(main);
		if (candidateChange != null) {
			// Launch content assist when the page is displayed
			final IWizardContainer container = getContainer();
			if (container instanceof IPageChangeProvider) {
				((IPageChangeProvider) container)
						.addPageChangedListener(new IPageChangedListener() {
							@Override
							public void pageChanged(PageChangedEvent event) {
								if (event
										.getSelectedPage() == FetchGerritChangePage.this) {
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

	private void preFetch(ChangeList list) {
		try {
			list.start();
		} catch (InvocationTargetException e) {
			Activator.handleError(e.getLocalizedMessage(), e.getCause(), true);
		}
	}

	/**
	 * Tries to determine a Gerrit change number from an input string.
	 *
	 * @param input
	 *            string to derive a change number from
	 * @return the change number and possibly also the patch set number, or
	 *         {@code null} if none could be determined.
	 */
	protected static Change determineChangeFromString(String input) {
		if (input == null) {
			return null;
		}
		try {
			Matcher matcher = GERRIT_URL_PATTERN.matcher(input);
			if (matcher.matches()) {
				String first = matcher.group(1);
				String second = matcher.group(2);
				String third = matcher.group(3);
				if (second != null && !second.isEmpty()) {
					if (third != null && !third.isEmpty()) {
						return Change.create(Integer.parseInt(second),
								Integer.parseInt(third));
					} else if (input.startsWith("http")) { //$NON-NLS-1$
						// A URL ending with two digits: take the first as
						// change number
						return Change.create(Integer.parseInt(first),
								Integer.parseInt(second));
					} else {
						// Take the numerically larger. Might be a fragment like
						// /10/65510 as in refs/changes/10/65510/6, or /65510/6
						// as in https://git.eclipse.org/r/#/c/65510/6. This is
						// a heuristic, it might go wrong on a Gerrit where
						// there are not many changes (yet), and one of them has
						// many patch sets.
						int firstNum = Integer.parseInt(first);
						int secondNum = Integer.parseInt(second);
						if (firstNum > secondNum) {
							return Change.create(firstNum, secondNum);
						} else {
							return Change.create(secondNum);
						}
					}
				} else {
					return Change.create(Integer.parseInt(first));
				}
			}
			matcher = GERRIT_CHANGE_REF_PATTERN.matcher(input);
			if (matcher.matches()) {
				int firstNum = Integer.parseInt(matcher.group(2));
				String second = matcher.group(3);
				if (second != null) {
					return Change.create(firstNum, Integer.parseInt(second));
				} else {
					return Change.create(firstNum);
				}
			}
		} catch (NumberFormatException e) {
			// Numerical overflow?
		}
		return null;
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
				Change input = determineChangeFromString(
						clipText.trim());
				if (input != null) {
					String toInsert = input.getChangeNumber().toString();
					boolean replacesEverything = text.getText().trim().isEmpty()
							|| text.getSelectionText().equals(text.getText());
					boolean openContentAssist = false;
					if (input.getPatchSetNumber() != null) {
						if (replacesEverything) {
							toInsert = input.getRefName();
						} else {
							toInsert = toInsert + '/'
									+ input.getPatchSetNumber();
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

	private void storeLastUsedUri(String uri) {
		settings.put(lastUriKey, uri.trim());
	}

	private void selectLastUsedUri() {
		String lastUri = settings.get(lastUriKey);
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
				Change change = Change.fromRef(refText.getText());
				if (change == null) {
					change = determineChangeFromString(refText.getText());
					if (change == null) {
						setErrorMessage(
								UIText.FetchGerritChangePage_MissingChangeMessage);
						return;
					}
				}
				ChangeList list = changeRefs.get(uriCombo.getText());
				if (list != null && list.isDone()) {
					try {
						Collection<Change> changes = list.get();
						if (change.getPatchSetNumber() != null) {
							if (changes == null || !changes.contains(change)) {
								setErrorMessage(
										UIText.FetchGerritChangePage_UnknownChangeRefMessage);
								return;
							}
						} else {
							Change fromGerrit = findHighestPatchSet(changes,
									change.getChangeNumber().intValue());
							if (fromGerrit == null) {
								setErrorMessage(NLS.bind(
										UIText.FetchGerritChangePage_NoSuchChangeMessage,
										change.getChangeNumber()));
								return;
							}
						}
					} catch (InterruptedException
							| InvocationTargetException e) {
						// Ignore: since we're done, this should never occur
					}
				}
			} else {
				setErrorMessage(UIText.FetchGerritChangePage_MissingChangeMessage);
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
			changeRefs.put(uriText, new ChangeList(repository, uriText));
		}
		ChangeList list = changeRefs.get(uriText);
		if (!list.isFinished()) {
			if (!list.mark()) {
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
								list.cancel(ChangeList.CancelMode.ABANDON);
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
		Change change = determineChangeFromString(refText.getText());
		if (change != null && change.getPatchSetNumber() == null) {
			Change fromGerrit = findHighestPatchSet(changes,
					change.getChangeNumber().intValue());
			if (fromGerrit != null) {
				String fullRef = fromGerrit.getRefName();
				refText.setText(fullRef);
				refTextEdited = false;
				refText.setSelection(fullRef.length());
			}
		}
	}

	private Change findHighestPatchSet(Collection<Change> changes,
			int changeNumber) {
		if (changes == null) {
			return null;
		}
		// We know that the result is sorted by change and
		// patch set number descending
		for (Change fromGerrit : changes) {
			int num = fromGerrit.getChangeNumber().intValue();
			if (num < changeNumber) {
				return null; // Doesn't exist
			} else if (changeNumber == num) {
				// Must be the one with the highest patch
				// set number.
				return fromGerrit;
			}
		}
		return null;
	}

	boolean doFetch() {
		fetching = true;
		final Change change = determineChangeFromString(refText.getText());
		final String uri = uriCombo.getText();
		// If we have an incomplete change (missing patch set number), remove
		// the change list future from the global map so that it won't be
		// interrupted when the dialog closes.
		final ChangeList changeList = change.getPatchSetNumber() == null
				? changeRefs.remove(uri) : null;
		if (changeList != null) {
			// Make sure a pending get() from the content assist gets aborted
			changeList.cancel(ChangeList.CancelMode.ABANDON);
		}
		final CheckoutMode mode = getCheckoutMode();
		final boolean doCheckoutNewBranch = (mode == CheckoutMode.CREATE_BRANCH)
				&& branchCheckoutButton.getSelection();
		final boolean doActivateAdditionalRefs = showAdditionalRefs();
		final String textForTag = tagText.getText();
		final String textForBranch = branchText.getText();

		Job job = new Job(
				UIText.FetchGerritChangePage_GetChangeTaskName) {

			@Override
			public IStatus run(IProgressMonitor monitor) {
				try {
					int steps = getTotalWork(mode);
					SubMonitor progress = SubMonitor.convert(monitor,
							UIText.FetchGerritChangePage_GetChangeTaskName,
							steps + 1);
					Change finalChange = completeChange(change,
							progress.newChild(1));
					if (finalChange == null) {
						// Returning an error status would log the message
						Activator.showError(NLS.bind(
								UIText.FetchGerritChangePage_NoSuchChangeMessage,
								change.getChangeNumber()), null);
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
					changeList.cancel(ChangeList.CancelMode.INTERRUPT);
				}
			}

			private Change completeChange(Change originalChange,
					IProgressMonitor monitor)
					throws OperationCanceledException {
				if (changeList != null) {
					monitor.subTask(NLS.bind(
							UIText.AsynchronousRefProposalProvider_FetchingRemoteRefsMessage,
							uri));
					Collection<Change> changes;
					try {
						changes = changeList.get();
					} catch (InvocationTargetException
							| InterruptedException e) {
						throw new OperationCanceledException();
					}
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					Change highest = findHighestPatchSet(changes,
							originalChange.getChangeNumber().intValue());
					if (highest != null) {
						return highest;
					}
				}
				return originalChange;
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
				|| cherryPickFetchHead.getSelection()
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
		} else if (cherryPickFetchHead.getSelection()) {
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

		String taskName = NLS
				.bind(UIText.FetchGerritChangePage_FetchingTaskName,
						spec.getSource());
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
		monitor.subTask(UIText.FetchGerritChangePage_CreatingTagTaskName);
		PersonIdent personIdent = new PersonIdent(repository);

		TagOperation operation = new TagOperation(repository)
				.setAnnotated(true)
				.setName(textForTag)
				.setTagger(personIdent)
				.setMessage(NLS.bind(
						UIText.FetchGerritChangePage_GeneratedTagMessage,
						spec.getSource()))
				.setTarget(commit)
				.setSign(Boolean.FALSE);
		operation.execute(monitor);
		monitor.worked(1);
	}

	private void createBranch(final String textForBranch, boolean doCheckout,
			RevCommit commit, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, doCheckout ? 10 : 2);
		progress.subTask(UIText.FetchGerritChangePage_CreatingBranchTaskName);
		CreateLocalBranchOperation bop = new CreateLocalBranchOperation(
				repository, textForBranch, commit);
		bop.execute(progress.newChild(2));
		if (doCheckout) {
			checkout(textForBranch, progress.newChild(8));
		}
	}

	private void checkout(String targetName, IProgressMonitor monitor)
			throws CoreException {
		monitor.subTask(UIText.FetchGerritChangePage_CheckingOutTaskName);
		BranchOperationUI.checkout(repository, targetName).run(monitor);
		monitor.worked(1);
	}

	private void cherryPick(@NonNull RevCommit commit,
			IProgressMonitor monitor) {
		monitor.subTask(UIText.FetchGerritChangePage_CherryPickTaskName);
		WorkbenchJob job = new WorkbenchJob(
				PlatformUI.getWorkbench().getDisplay(),
				UIText.FetchGerritChangePage_CherryPickTaskName) {

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
				return new ChangeContentProposal(change);
			}
			return null;
		}, input -> {
			Change change = determineChangeFromString(input);
			int changeNumber = -1;
			try {
				if (change == null) {
					Matcher matcher = DIGITS.matcher(input);
					if (matcher.find()) {
						return Pattern.compile(GERRIT_CHANGE_REF_PREFIX
								+ "(../)?" + matcher.group() + WILDCARD); //$NON-NLS-1$
					} else if (input.startsWith(GERRIT_CHANGE_REF_PREFIX)
							|| GERRIT_CHANGE_REF_PREFIX.startsWith(input)) {
						return null; // Match all
					}
				} else {
					changeNumber = change.getChangeNumber().intValue();
				}
				if (changeNumber > 0) {
					return Pattern.compile(GERRIT_CHANGE_REF_PREFIX + "../" //$NON-NLS-1$
							+ changeNumber + WILDCARD);
				}
			} catch (PatternSyntaxException e) {
				// Ignore and return default pattern below.
			}
			return UIUtils.createProposalPattern(input);
		}, null, UIText.FetchGerritChangePage_ContentAssistTooltip);
	}

	final static class Change implements Comparable<Change> {
		private final String refName;

		private final Integer changeNumber;

		private final Integer patchSetNumber;

		static Change fromRef(String refName) {
			try {
				if (refName == null) {
					return null;
				}
				Matcher m = GERRIT_CHANGE_REF_PATTERN.matcher(refName);
				if (!m.matches() || m.group(3) == null) {
					return null;
				}
				Integer subdir = Integer.valueOf(m.group(1));
				Integer changeNumber = Integer.valueOf(m.group(2));
				if (subdir.intValue() != changeNumber.intValue() % 100) {
					return null;
				}
				Integer patchSetNumber = Integer.valueOf(m.group(3));
				return new Change(refName, changeNumber, patchSetNumber);
			} catch (NumberFormatException | IndexOutOfBoundsException e) {
				// if we can't parse this, just return null
				return null;
			}
		}

		static Change create(int changeNumber) {
			return new Change(null, Integer.valueOf(changeNumber), null);
		}

		static Change create(int changeNumber, int patchSetNumber) {
			int subDir = changeNumber % 100;
			return new Change(
					GERRIT_CHANGE_REF_PREFIX
							+ String.format("%02d", Integer.valueOf(subDir)) //$NON-NLS-1$
							+ '/' + changeNumber + '/' + patchSetNumber,
					Integer.valueOf(changeNumber),
					Integer.valueOf(patchSetNumber));
		}

		private Change(String refName, Integer changeNumber,
				Integer patchSetNumber) {
			this.refName = refName;
			this.changeNumber = changeNumber;
			this.patchSetNumber = patchSetNumber;
		}

		public String getRefName() {
			return refName;
		}

		public Integer getChangeNumber() {
			return changeNumber;
		}

		public Integer getPatchSetNumber() {
			return patchSetNumber;
		}

		@Override
		public String toString() {
			return refName;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Change)) {
				return false;
			}
			return compareTo((Change) obj) == 0;
		}

		@Override
		public int hashCode() {
			return Objects.hash(changeNumber, patchSetNumber);
		}

		@Override
		public int compareTo(Change o) {
			int changeDiff = this.changeNumber.compareTo(o.getChangeNumber());
			if (changeDiff == 0) {
				if (patchSetNumber == null) {
					return o.getPatchSetNumber() != null ? -1 : 0;
				} else if (o.getPatchSetNumber() == null) {
					return 1;
				}
				changeDiff = this.patchSetNumber
						.compareTo(o.getPatchSetNumber());
			}
			return changeDiff;
		}
	}

	private final static class ChangeContentProposal implements
			IContentProposal {
		private final Change myChange;

		ChangeContentProposal(Change change) {
			myChange = change;
		}

		@Override
		public String getContent() {
			return myChange.getRefName();
		}

		@Override
		public int getCursorPosition() {
			return 0;
		}

		@Override
		public String getDescription() {
			return NLS.bind(
					UIText.FetchGerritChangePage_ContentAssistDescription,
					myChange.getPatchSetNumber(), myChange.getChangeNumber());
		}

		@Override
		public String getLabel() {
			return NLS
					.bind("{0} - {1}", myChange.getChangeNumber(), myChange.getPatchSetNumber()); //$NON-NLS-1$
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return getContent();
		}
	}

	/**
	 * A {@code ChangeList} loads the list of change refs asynchronously from
	 * the remote repository.
	 */
	private static class ChangeList extends AsynchronousListOperation<Change> {

		private AtomicBoolean getting = new AtomicBoolean();

		public ChangeList(Repository repository, String uriText) {
			super(repository, uriText);
		}

		@Override
		protected Collection<Change> convert(Collection<Ref> refs) {
			List<Change> changes = new ArrayList<>();
			for (Ref ref : refs) {
				Change change = Change.fromRef(ref.getName());
				if (change != null) {
					changes.add(change);
				}
			}
			Collections.sort(changes, Collections.reverseOrder());
			return new LinkedHashSet<>(changes);
		}

		public boolean mark() {
			return getting.compareAndSet(false, true);
		}
	}
}
