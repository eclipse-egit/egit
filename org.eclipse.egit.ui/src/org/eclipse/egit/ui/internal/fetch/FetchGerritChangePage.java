/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Marc Khouzam (Ericsson)  - Add an option not to checkout the new branch
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 493935, 495777
 *    Jaxsun McCarthy Huggan <jaxsun.mccarthy@tasktop.com> - Bug 509181
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.gerrit.GerritUtil;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.egit.ui.internal.dialogs.AbstractBranchSelectionDialog;
import org.eclipse.egit.ui.internal.dialogs.BranchEditDialog;
import org.eclipse.egit.ui.internal.gerrit.GerritDialogSettings;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;

/**
 * Fetch a change from Gerrit
 */
public class FetchGerritChangePage extends WizardPage {

	private static final String RUN_IN_BACKGROUND = "runInBackground"; //$NON-NLS-1$

	private final Repository repository;

	private final IDialogSettings settings;

	private final String lastUriKey;

	private Combo uriCombo;

	private List<Change> changeRefs;

	private Text refText;

	private Button changeBranch;

	private Button createBranch;

	private Button createTag;

	private Button checkout;

	private Button dontCheckout;

	private Label tagTextlabel;

	private Text tagText;

	private Label branchTextlabel;

	private Text branchText;

	private String refName;

	private Composite warningAdditionalRefNotActive;

	private Button activateAdditionalRefs;

	private Button runInBackgroud;

	private IInputValidator branchValidator;

	private IInputValidator tagValidator;

	private Button branchEditButton;

	private Button branchCheckoutButton;

	private Composite main;

	/**
	 * @param repository
	 * @param refName initial value for the ref field
	 */
	public FetchGerritChangePage(Repository repository, String refName) {
		super(FetchGerritChangePage.class.getName());
		this.repository = repository;
		this.refName = refName;
		setTitle(NLS
				.bind(UIText.FetchGerritChangePage_PageTitle,
						Activator.getDefault().getRepositoryUtil()
								.getRepositoryName(repository)));
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
		Clipboard clipboard = new Clipboard(parent.getDisplay());
		String clipText = (String) clipboard.getContents(TextTransfer
				.getInstance());
		clipboard.dispose();
		String defaultUri = null;
		String defaultCommand = null;
		String defaultChange = null;
		String candidateChange = null;
		if (clipText != null) {
			String pattern = "git fetch (\\w+:\\S+) (refs/changes/\\d+/\\d+/\\d+) && git (\\w+) FETCH_HEAD"; //$NON-NLS-1$
			Matcher matcher = Pattern.compile(pattern).matcher(clipText);
			if (matcher.matches()) {
				defaultUri = matcher.group(1);
				defaultChange = matcher.group(2);
				defaultCommand = matcher.group(3);
			} else {
				candidateChange = determineChangeFromString(clipText.trim());
			}
		}
		main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		new Label(main, SWT.NONE)
				.setText(UIText.FetchGerritChangePage_UriLabel);
		uriCombo = new Combo(main, SWT.DROP_DOWN);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(uriCombo);
		uriCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changeRefs = null;
			}
		});
		new Label(main, SWT.NONE)
				.setText(UIText.FetchGerritChangePage_ChangeLabel);
		refText = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(refText);
		final ExplicitContentProposalAdapter contentProposer = addRefContentProposalToText(
				refText);
		refText.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent event) {
				event.text = event.text.trim();
			}
		});

		final Group checkoutGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		checkoutGroup.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().span(3, 1).grab(true, false)
				.applyTo(checkoutGroup);
		checkoutGroup.setText(UIText.FetchGerritChangePage_AfterFetchGroup);

		// radio: checkout local branch
		changeBranch = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(changeBranch);
		changeBranch
				.setText(UIText.FetchGerritChangePage_ChangeToLocalBranchRadio);
		changeBranch.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}
		});

		// radio: create local branch
		createBranch = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(createBranch);
		createBranch.setText(UIText.FetchGerritChangePage_CreateLocalBranchRadio);
		createBranch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}
		});

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
		GridDataFactory.fillDefaults().grab(true, false).applyTo(branchText);
		branchText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		branchEditButton = new Button(checkoutGroup, SWT.PUSH);
		branchEditButton.setFont(JFaceResources.getDialogFont());
		branchEditButton.setText(UIText.FetchGerritChangePage_BranchEditButton);
		branchEditButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent selectionEvent) {
				String txt = branchText.getText();
				String refToMark = "".equals(txt) ? null : Constants.R_HEADS + txt; //$NON-NLS-1$
				AbstractBranchSelectionDialog dlg = new BranchEditDialog(
						checkoutGroup.getShell(), repository, refToMark);
				if (dlg.open() == Window.OK) {
					branchText.setText(Repository.shortenRefName(dlg
							.getRefName()));
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
		createTag.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}
		});

		tagTextlabel = new Label(checkoutGroup, SWT.NONE);
		GridDataFactory.defaultsFor(tagTextlabel).exclude(true)
				.applyTo(tagTextlabel);
		tagTextlabel.setText(UIText.FetchGerritChangePage_TagNameText);
		tagText = new Text(checkoutGroup, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().exclude(true).grab(true, false)
				.applyTo(tagText);
		tagText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		// radio: checkout FETCH_HEAD
		checkout = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(checkout);
		checkout.setText(UIText.FetchGerritChangePage_CheckoutRadio);
		checkout.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}
		});

		// radio: don't checkout
		dontCheckout = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(checkout);
		dontCheckout.setText(UIText.FetchGerritChangePage_UpdateRadio);
		dontCheckout.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}
		});

		if ("checkout".equals(defaultCommand)) { //$NON-NLS-1$
			checkout.setSelection(true);
		} else if (getLocalRef(refName) != null) {
			changeBranch.setSelection(true);
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
				.setToolTipText(UIText.FetchGerritChangePage_ActivateAdditionalRefsTooltip);

		refText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Change change = Change.fromRef(refText.getText());
				if (change != null) {
					branchText.setText(change.suggestBranchName());
					tagText.setText(branchText.getText());
				} else {
					branchText.setText(""); //$NON-NLS-1$
					tagText.setText(""); //$NON-NLS-1$
				}
				if (getLocalRef(refText.getText()) == null
						&& changeBranch.getSelection()) {
					createBranch.setSelection(true);
				} else {
					changeBranch.setSelection(true);
				}
				checkPage();
			}
		});
		if (defaultChange != null) {
			refText.setText(defaultChange);
		} else if (candidateChange != null) {
			refText.setText(candidateChange);
		}
		runInBackgroud = new Button(main, SWT.CHECK);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.BEGINNING, SWT.END)
				.grab(true, true)
				.applyTo(runInBackgroud);
		runInBackgroud.setText(UIText.FetchGerritChangePage_RunInBackground);

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
		}
		if (defaultUri != null) {
			uriCombo.setText(defaultUri);
		} else {
			selectLastUsedUri();
		}
		restoreRunInBackgroundSelection();
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

	/**
	 * Tries to determine a Gerrit change number from an input string.
	 *
	 * @param input
	 *            string to derive a change number from
	 * @return the change number as a string, or {@code null} if none could be
	 *         determined.
	 */
	protected static String determineChangeFromString(String input) {
		if (input == null) {
			return null;
		}
		Pattern pattern = Pattern.compile(
				"(?:https?://\\S+?/|/)?([1-9][0-9]*)(?:/([1-9][0-9]*)(?:/([1-9][0-9]*)(?:..\\d+)?)?)?(?:/\\S*)?"); //$NON-NLS-1$
		Matcher matcher = pattern.matcher(input);
		if (matcher.matches()) {
			String first = matcher.group(1);
			String second = matcher.group(2);
			String third = matcher.group(3);
			if (second != null && !second.isEmpty()) {
				if (third != null && !third.isEmpty()) {
					return second;
				} else if (input.startsWith("http")) { //$NON-NLS-1$
					// A URL ending with two digits: take the first.
					return first;
				} else {
					// Take the numerically larger. Might be a fragment like
					// /10/65510 as in refs/changes/10/65510/6, or /65510/6 as
					// in https://git.eclipse.org/r/#/c/65510/6. This is a
					// heuristic, it might go wrong on a Gerrit where there are
					// not many changes (yet), and one of them has many patch
					// sets.
					try {
						if (Integer.parseInt(first) > Integer
								.parseInt(second)) {
							return first;
						} else {
							return second;
						}
					} catch (NumberFormatException e) {
						// Numerical overflow?
						return null;
					}
				}
			} else {
				return first;
			}
		}
		return null;
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

	private void storeRunInBackgroundSelection() {
		settings.put(RUN_IN_BACKGROUND, runInBackgroud.getSelection());
	}

	private void restoreRunInBackgroundSelection() {
		runInBackgroud.setSelection(settings.getBoolean(RUN_IN_BACKGROUND));
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && refName != null)
			refText.setText(refName);
	}

	private void checkPage() {
		boolean hasLocalBranch = getLocalRef(refText.getText()) != null;
		changeBranch.setEnabled(hasLocalBranch);
		changeBranch.setVisible(hasLocalBranch);
		GridData gd = (GridData) changeBranch.getLayoutData();
		gd.exclude = !hasLocalBranch;

		boolean createBranchSelected = createBranch.getSelection();
		branchText.setEnabled(createBranchSelected);
		branchText.setVisible(createBranchSelected);
		branchTextlabel.setVisible(createBranchSelected);
		branchEditButton.setVisible(createBranchSelected);
		branchCheckoutButton.setVisible(createBranchSelected);
		gd = (GridData) branchText.getLayoutData();
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

		boolean showActivateAdditionalRefs = false;
		showActivateAdditionalRefs = (checkout.getSelection() || dontCheckout
				.getSelection())
				&& !Activator
						.getDefault()
						.getPreferenceStore()
						.getBoolean(
								UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS);

		gd = (GridData) warningAdditionalRefNotActive.getLayoutData();
		gd.exclude = !showActivateAdditionalRefs;
		warningAdditionalRefNotActive.setVisible(showActivateAdditionalRefs);

		main.getParent().layout(true);

		setErrorMessage(null);
		try {
			if (refText.getText().length() > 0) {
				Change change = Change.fromRef(refText.getText());
				if (change == null) {
					setErrorMessage(UIText.FetchGerritChangePage_MissingChangeMessage);
					return;
				}
			} else {
				setErrorMessage(UIText.FetchGerritChangePage_MissingChangeMessage);
				return;
			}

			if (createBranchSelected)
				setErrorMessage(branchValidator.isValid(branchText.getText()));
			else if (createTagSelected)
				setErrorMessage(tagValidator.isValid(tagText.getText()));
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

	private Ref getLocalRef(String ref) {
		Change change = Change.fromRef(ref);
		if (change != null) {
			try {
				return repository.findRef(change.computeFullRefName());
			} catch (IOException e) {
				// ignore
			}
		}
		return null;
	}

	private List<Change> getRefsForContentAssist()
			throws InvocationTargetException, InterruptedException {
		if (changeRefs == null) {
			final String uriText = uriCombo.getText();
			getContainer().run(true, true,
					new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							ListRemoteOperation listOp;
							try {
								listOp = new ListRemoteOperation(
										repository,
										new URIish(uriText),
										Activator
												.getDefault()
												.getPreferenceStore()
												.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT));
							} catch (URISyntaxException e) {
								throw new InvocationTargetException(e);
							}

							listOp.run(monitor);
							changeRefs = new ArrayList<>();
							for (Ref ref : listOp.getRemoteRefs()) {
								Change change = Change.fromRef(ref.getName());
								if (change != null)
									changeRefs.add(change);
							}
							Collections.sort(changeRefs,
									Collections.reverseOrder());
						}
					});
		}
		return changeRefs;
	}

	boolean doFetch() {
		final RefSpec spec = new RefSpec().setSource(refText.getText())
				.setDestination(Constants.FETCH_HEAD);
		final String uri = uriCombo.getText();
		final boolean doCheckout = checkout.getSelection();
		final boolean doCreateTag = createTag.getSelection();
		final boolean doChangeBranch = changeBranch.getSelection();
		final boolean doCreateBranch = createBranch.getSelection();
		final boolean doCheckoutNewBranch = branchCheckoutButton.getSelection();
		final boolean doActivateAdditionalRefs = (checkout.getSelection() || dontCheckout
				.getSelection()) && activateAdditionalRefs.getSelection();
		final String textForTag = tagText.getText();
		final String textForBranch = branchText.getText();

		storeRunInBackgroundSelection();

		if (runInBackgroud.getSelection()) {
			Job job = new WorkspaceJob(
					UIText.FetchGerritChangePage_GetChangeTaskName) {

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) {
					try {
						internalDoFetch(spec, uri, doCheckout, doCreateTag,
								doChangeBranch, doCreateBranch,
								doCheckoutNewBranch, doActivateAdditionalRefs,
								textForTag, textForBranch, monitor);
					} catch (CoreException ce) {
						return ce.getStatus();
					} catch (Exception e) {
						return Activator.createErrorStatus(e.getLocalizedMessage(), e);
					}
					return org.eclipse.core.runtime.Status.OK_STATUS;
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
		} else {
			try {
			getWizard().getContainer().run(true, true,
					new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							try {
								internalDoFetch(spec, uri, doCheckout,
											doCreateTag, doChangeBranch,
											doCreateBranch,
										doCheckoutNewBranch, doActivateAdditionalRefs,
										textForTag, textForBranch, monitor);
							} catch (RuntimeException e) {
								throw e;
							} catch (Exception e) {
								throw new InvocationTargetException(e);
							} finally {
								monitor.done();
							}
						}
					});
			} catch (InvocationTargetException e) {
				Activator.handleError(e.getCause().getMessage(), e.getCause(),
						true);
				return false;
			} catch (InterruptedException e) {
				// just return
			}
			return true;
		}
	}

	private void internalDoFetch(RefSpec spec, String uri, boolean doCheckout,
			boolean doCreateTag, boolean doChangeBranch,
			boolean doCreateBranch, boolean doCheckoutNewBranch,
			boolean doActivateAdditionalRefs, String textForTag, String textForBranch, IProgressMonitor monitor)
			throws IOException, CoreException, URISyntaxException {

		int totalWork = 1;
		if (doCheckout)
			totalWork++;
		if (doCreateTag || doCreateBranch)
			totalWork++;
		monitor.beginTask(
				UIText.FetchGerritChangePage_GetChangeTaskName,
				totalWork);

		if (doChangeBranch) {
			Ref localRef = getLocalRef(spec.getSource());
			checkout(localRef.getName(), monitor);
		} else {
			try {
				RevCommit commit = fetchChange(uri, spec, monitor);

				if (doCreateTag)
					createTag(spec, textForTag, commit, monitor);

				if (doCreateBranch)
					createBranch(textForBranch, doCheckoutNewBranch, commit,
							monitor);

				if (doCheckout || doCreateTag)
					checkout(commit.name(), monitor);

				if (doActivateAdditionalRefs)
					activateAdditionalRefs();

				storeLastUsedUri(uri);

			} finally {
				monitor.done();
			}
		}
	}

	private RevCommit fetchChange(String uri, RefSpec spec,
			IProgressMonitor monitor) throws CoreException, URISyntaxException,
			IOException {
		int timeout = Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);

		List<RefSpec> specs = new ArrayList<>(1);
		specs.add(spec);

		String taskName = NLS
				.bind(UIText.FetchGerritChangePage_FetchingTaskName,
						spec.getSource());
		monitor.setTaskName(taskName);
		FetchResult fetchRes = new FetchOperationUI(repository,
				new URIish(uri), specs, timeout, false).execute(monitor);

		monitor.worked(1);
		try (RevWalk rw = new RevWalk(repository)) {
			return rw.parseCommit(
					fetchRes.getAdvertisedRef(spec.getSource()).getObjectId());
		}
	}

	private void createTag(final RefSpec spec, final String textForTag,
			RevCommit commit, IProgressMonitor monitor) throws CoreException {
		monitor.setTaskName(UIText.FetchGerritChangePage_CreatingTagTaskName);
		final TagBuilder tag = new TagBuilder();
		PersonIdent personIdent = new PersonIdent(repository);

		tag.setTag(textForTag);
		tag.setTagger(personIdent);
		tag.setMessage(NLS.bind(
				UIText.FetchGerritChangePage_GeneratedTagMessage,
				spec.getSource()));
		tag.setObjectId(commit);
		new TagOperation(repository, tag, false).execute(monitor);
		monitor.worked(1);
	}

	private void createBranch(final String textForBranch, boolean doCheckout,
			RevCommit commit, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor,
				UIText.FetchGerritChangePage_CreatingBranchTaskName,
				doCheckout ? 10 : 2);
		CreateLocalBranchOperation bop = new CreateLocalBranchOperation(
				repository, textForBranch, commit);
		bop.execute(progress.newChild(2));
		if (doCheckout) {
			checkout(textForBranch, progress.newChild(8));
		}
	}

	private void checkout(String targetName, IProgressMonitor monitor)
			throws CoreException {
		monitor.setTaskName(UIText.FetchGerritChangePage_CheckingOutTaskName);
		BranchOperationUI.checkout(repository, targetName).run(monitor);
		monitor.worked(1);
	}

	private void activateAdditionalRefs() {
		// do this in the UI thread as it results in a
		// refresh() on the history page
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				Activator
						.getDefault()
						.getPreferenceStore()
						.setValue(
								UIPreferences.RESOURCEHISTORY_SHOW_ADDITIONAL_REFS,
								true);
			}
		});
	}

	private ExplicitContentProposalAdapter addRefContentProposalToText(
			final Text textField) {
		KeyStroke stroke = UIUtils
				.getKeystrokeOfBestActiveBindingFor(IWorkbenchCommandConstants.EDIT_CONTENT_ASSIST);
		if (stroke != null) {
			UIUtils.addBulbDecorator(textField, NLS.bind(
					UIText.FetchGerritChangePage_ContentAssistTooltip,
					stroke.format()));
		}
		IContentProposalProvider cp = new IContentProposalProvider() {
			@Override
			public IContentProposal[] getProposals(String contents, int position) {
				List<IContentProposal> resultList = new ArrayList<>();

				Pattern pattern = UIUtils.createProposalPattern(contents);

				List<Change> proposals;
				try {
					proposals = getRefsForContentAssist();
				} catch (InvocationTargetException e) {
					Activator.handleError(e.getMessage(), e, true);
					return null;
				} catch (InterruptedException e) {
					return null;
				}

				if (proposals != null) {
					for (final Change ref : proposals) {
						if (pattern != null
								&& !pattern.matcher(
										ref.getChangeNumber().toString())
										.matches()) {
							continue;
						}
						resultList.add(new ChangeContentProposal(ref));
					}
				}
				return resultList.toArray(new IContentProposal[resultList
						.size()]);
			}
		};

		ExplicitContentProposalAdapter adapter = new ExplicitContentProposalAdapter(
				textField, cp, stroke);
		// set the acceptance style to always replace the complete content
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		return adapter;
	}

	private static class ExplicitContentProposalAdapter
			extends ContentProposalAdapter {

		public ExplicitContentProposalAdapter(Control control,
				IContentProposalProvider proposalProvider,
				KeyStroke keyStroke) {
			super(control, new TextContentAdapter(), proposalProvider,
					keyStroke, null);
		}

		@Override
		public void openProposalPopup() {
			// Make this method accessible
			super.openProposalPopup();
		}
	}

	private final static class Change implements Comparable<Change> {
		private final String refName;

		private final Integer changeNumber;

		private final Integer patchSetNumber;

		static @Nullable Change fromRef(@Nullable String refName) {
			try {
				if (refName == null || !refName.startsWith("refs/changes/")) //$NON-NLS-1$
					return null;
				String[] tokens = refName.substring(13).split("/"); //$NON-NLS-1$
				if (tokens.length != 3)
					return null;
				Integer changeNumber = Integer.valueOf(tokens[1]);
				Integer patchSetNumber = Integer.valueOf(tokens[2]);
				return new Change(refName, changeNumber, patchSetNumber);
			} catch (NumberFormatException e) {
				// if we can't parse this, just return null
				return null;
			} catch (IndexOutOfBoundsException e) {
				// if we can't parse this, just return null
				return null;
			}
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

		public String suggestBranchName() {
			return NLS.bind(UIText.Change_SuggestedBranchNamePattern,
					changeNumber, patchSetNumber);
		}

		public String computeFullRefName() {
			return NLS.bind(UIText.Change_FullRefNamePattern, changeNumber,
					patchSetNumber);
		}

		@Override
		public String toString() {
			return refName;
		}

		@Override
		public int compareTo(Change o) {
			int changeDiff = this.changeNumber.compareTo(o.changeNumber);
			if (changeDiff == 0) {
				changeDiff = this.getPatchSetNumber()
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
}
