/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.api.Git;
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Fetch a change from Gerrit
 */
public class FetchGerritChangePage extends WizardPage {
	private static final String FETCH_GERRIT_CHANGE_PAGE_SECTION = "FetchGerritChangePage"; //$NON-NLS-1$

	private static final String LAST_URI_POSTFIX = ".lastUri"; //$NON-NLS-1$

	private final Repository repository;

	private final IDialogSettings settings;

	private final String lastUriKey;

	private Combo uriCombo;

	private List<Change> changeRefs;

	private Text refText;

	private Button createBranch;

	private Button createTag;

	private Button checkout;

	private Button dontCheckout;

	private Label tagTextlabel;

	private Text tagText;

	private Label branchTextlabel;

	private Text branchText;

	private String refName;

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
		lastUriKey = repository + LAST_URI_POSTFIX;
	}

	protected IDialogSettings getDialogSettings() {
		IDialogSettings s = Activator.getDefault().getDialogSettings();
		IDialogSettings section = s
				.getSection(FETCH_GERRIT_CHANGE_PAGE_SECTION);
		if (section == null)
			section = s.addNewSection(FETCH_GERRIT_CHANGE_PAGE_SECTION);
		return section;
	}

	public void createControl(Composite parent) {
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
				changeRefs = null;
			}
		});
		new Label(main, SWT.NONE)
				.setText(UIText.FetchGerritChangePage_ChangeLabel);
		refText = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(refText);
		addRefContentProposalToText(refText);

		Group checkoutGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		checkoutGroup.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().span(2, 1).grab(true, true)
				.applyTo(checkoutGroup);
		checkoutGroup.setText(UIText.FetchGerritChangePage_AfterFetchGroup);

		// radio: create local branch
		createBranch = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(createBranch);
		createBranch.setText(UIText.FetchGerritChangePage_LocalBranchRadio);
		createBranch.setSelection(true);
		createBranch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}
		});

		branchTextlabel = new Label(checkoutGroup, SWT.NONE);
		GridDataFactory.defaultsFor(branchTextlabel).exclude(false)
				.applyTo(branchTextlabel);
		branchTextlabel.setText(UIText.FetchGerritChangePage_BranchNameText);
		branchText = new Text(checkoutGroup, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(branchText);
		branchText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		// radio: create tag
		createTag = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(createTag);
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
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		// radio: checkout FETCH_HEAD
		checkout = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(checkout);
		checkout.setText(UIText.FetchGerritChangePage_CheckoutRadio);
		checkout.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}
		});

		// radio: don't checkout
		dontCheckout = new Button(checkoutGroup, SWT.RADIO);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(checkout);
		dontCheckout.setText(UIText.FetchGerritChangePage_UpdateRadio);
		dontCheckout.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}
		});

		refText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				Change change = Change.fromRef(refText.getText());
				if (change != null) {
					branchText.setText(NLS
							.bind(UIText.FetchGerritChangePage_SuggestedRefNamePattern,
									change.getChangeNumber(),
									change.getPatchSetNumber()));
					tagText.setText(branchText.getText());
				} else {
					branchText.setText(""); //$NON-NLS-1$
					tagText.setText(""); //$NON-NLS-1$
				}
				checkPage();
			}
		});

		// get all available URIs from the repository
		SortedSet<String> uris = new TreeSet<String>();
		try {
			for (RemoteConfig rc : RemoteConfig.getAllRemoteConfigs(repository
					.getConfig())) {
				if (rc.getURIs().size() > 0)
					uris.add(rc.getURIs().get(0).toPrivateString());
				for (URIish u : rc.getPushURIs())
					uris.add(u.toPrivateString());

			}
		} catch (URISyntaxException e) {
			Activator.handleError(e.getMessage(), e, false);
			setErrorMessage(e.getMessage());
		}
		for (String aUri : uris)
			uriCombo.add(aUri);
		selectLastUsedUri();
		refText.setFocus();
		Dialog.applyDialogFont(main);
		setControl(main);
		setPageComplete(false);
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

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && refName != null)
			refText.setText(refName);
	}

	private void checkPage() {
		boolean createBranchSelected = createBranch.getSelection();
		branchText.setEnabled(createBranchSelected);
		branchText.setVisible(createBranchSelected);
		branchTextlabel.setVisible(createBranchSelected);
		GridData gd = (GridData) branchText.getLayoutData();
		gd.exclude = !createBranchSelected;
		gd = (GridData) branchTextlabel.getLayoutData();
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

			boolean emptyRefName = (createBranchSelected && branchText
					.getText().length() == 0)
					|| (createTagSelected && tagText.getText().length() == 0);
			if (emptyRefName) {
				setErrorMessage(UIText.FetchGerritChangePage_ProvideRefNameMessage);
				return;
			}

			boolean existingRefName = (createBranchSelected && repository
					.getRef(branchText.getText()) != null)
					|| (createTagSelected && repository.getRef(tagText
							.getText()) != null);
			if (existingRefName) {
				setErrorMessage(NLS.bind(
						UIText.FetchGerritChangePage_ExistingRefMessage,
						branchText.getText()));
				return;
			}
		} catch (IOException e1) {
			// ignore here
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

	private List<Change> getRefsForContentAssist()
			throws InvocationTargetException, InterruptedException {
		if (changeRefs == null) {
			final String uriText = uriCombo.getText();
			getWizard().getContainer().run(true, true,
					new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							changeRefs = new ArrayList<Change>();
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
							for (Ref ref : listOp.getRemoteRefs()) {
								Change change = Change.fromRef(ref.getName());
								if (change != null)
									changeRefs.add(change);
							}
							Collections.sort(changeRefs,
									new Comparator<Change>() {
										public int compare(Change o1, Change o2) {
											// change number descending
											int changeDiff = o2.changeNumber
													.compareTo(o1.changeNumber);
											if (changeDiff == 0)
												// patch set number descending
												changeDiff = o2
														.getPatchSetNumber()
														.compareTo(
																o1.getPatchSetNumber());
											return changeDiff;
										}
									});
						}
					});

		}
		return changeRefs;
	}

	boolean doFetch() {
		try {
			final RefSpec spec = new RefSpec().setSource(refText.getText())
					.setDestination(Constants.FETCH_HEAD);
			final String uri = uriCombo.getText();
			final boolean doCheckout = checkout.getSelection();
			final boolean doCreateTag = createTag.getSelection();
			final boolean doCreateBranch = createBranch.getSelection();
			final String textForTag = tagText.getText();
			final String textForBranch = branchText.getText();
			getWizard().getContainer().run(true, true,
					new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							int totalWork = 1;
							if (doCheckout)
								totalWork++;
							if (doCreateTag || doCreateBranch)
								totalWork++;
							monitor.beginTask(
									UIText.FetchGerritChangePage_GetChangeTaskName,
									totalWork);
							List<RefSpec> specs = new ArrayList<RefSpec>(1);
							specs.add(spec);
							int timeout = Activator
									.getDefault()
									.getPreferenceStore()
									.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
							FetchResult fetchRes;
							try {
								String taskName = NLS
										.bind(UIText.FetchGerritChangePage_FetchingTaskName,
												spec.getSource());
								monitor.setTaskName(taskName);
								fetchRes = new FetchOperationUI(repository,
										new URIish(uri), specs, timeout, false)
										.execute(monitor);

								monitor.worked(1);
								RevCommit commit = new RevWalk(repository)
										.parseCommit(fetchRes.getAdvertisedRef(
												spec.getSource()).getObjectId());

								if (doCreateTag) {
									monitor.setTaskName(UIText.FetchGerritChangePage_CreatingTagTaskName);
									final TagBuilder tag = new TagBuilder();
									PersonIdent personIdent = new PersonIdent(
											repository);

									tag.setTag(textForTag);
									tag.setTagger(personIdent);
									tag.setMessage(NLS
											.bind(UIText.FetchGerritChangePage_GeneratedTagMessage,
													spec.getSource()));
									tag.setObjectId(commit);
									new TagOperation(repository, tag, false)
											.execute(monitor);
									monitor.worked(1);
								}
								if (doCreateBranch) {
									monitor.setTaskName(UIText.FetchGerritChangePage_CreatingBranchTaskName);
									CreateLocalBranchOperation bop = new CreateLocalBranchOperation(
											repository, textForBranch, commit);
									bop.execute(monitor);
									new Git(repository).checkout()
											.setName(textForBranch).call();
									monitor.worked(1);
								}
								if (doCheckout || doCreateTag) {
									monitor.setTaskName(UIText.FetchGerritChangePage_CheckingOutTaskName);
									BranchOperationUI.checkout(repository, commit.name())
											.run(monitor);

									monitor.worked(1);
								}
								storeLastUsedUri(uri);
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
			Activator
					.handleError(e.getCause().getMessage(), e.getCause(), true);
			return false;
		} catch (InterruptedException e) {
			// just return
		}
		return true;
	}

	private void addRefContentProposalToText(final Text textField) {
		KeyStroke stroke;
		try {
			stroke = KeyStroke.getInstance("CTRL+SPACE"); //$NON-NLS-1$
			UIUtils.addBulbDecorator(textField, NLS.bind(
					UIText.FetchGerritChangePage_ContentAssistTooltip,
					stroke.format()));
		} catch (ParseException e1) {
			Activator.handleError(e1.getMessage(), e1, false);
			stroke = null;
		}

		IContentProposalProvider cp = new IContentProposalProvider() {
			public IContentProposal[] getProposals(String contents, int position) {
				List<IContentProposal> resultList = new ArrayList<IContentProposal>();

				// make the simplest possible pattern check: allow "*"
				// for multiple characters
				String patternString = contents;
				// ignore spaces in the beginning
				while (patternString.length() > 0
						&& patternString.charAt(0) == ' ') {
					patternString = patternString.substring(1);
				}

				// we quote the string as it may contain spaces
				// and other stuff colliding with the Pattern
				patternString = Pattern.quote(patternString);

				patternString = patternString.replaceAll("\\x2A", ".*"); //$NON-NLS-1$ //$NON-NLS-2$

				// make sure we add a (logical) * at the end
				if (!patternString.endsWith(".*")) { //$NON-NLS-1$
					patternString = patternString + ".*"; //$NON-NLS-1$
				}

				// let's compile a case-insensitive pattern (assumes ASCII only)
				Pattern pattern;
				try {
					pattern = Pattern.compile(patternString,
							Pattern.CASE_INSENSITIVE);
				} catch (PatternSyntaxException e) {
					pattern = null;
				}

				List<Change> proposals;
				try {
					proposals = getRefsForContentAssist();
				} catch (InvocationTargetException e) {
					Activator.handleError(e.getMessage(), e, false);
					return null;
				} catch (InterruptedException e) {
					return null;
				}

				if (proposals != null)
					for (final Change ref : proposals) {
						if (pattern != null
								&& !pattern.matcher(
										ref.getChangeNumber().toString())
										.matches())
							continue;
						IContentProposal propsal = new ChangeContentProposal(
								ref);
						resultList.add(propsal);
					}

				return resultList.toArray(new IContentProposal[resultList
						.size()]);
			}
		};

		ContentProposalAdapter adapter = new ContentProposalAdapter(textField,
				new TextContentAdapter(), cp, stroke, null);
		// set the acceptance style to always replace the complete content
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	}

	private final static class Change {
		private final String refName;

		private final Integer changeNumber;

		private final Integer patchSetNumber;

		static Change fromRef(String refName) {
			try {
				if (!refName.startsWith("refs/changes/")) //$NON-NLS-1$
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

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return refName;
		}
	}

	private final static class ChangeContentProposal implements
			IContentProposal {
		private final Change myChange;

		ChangeContentProposal(Change change) {
			myChange = change;
		}

		public String getContent() {
			return myChange.getRefName();
		}

		public int getCursorPosition() {
			return 0;
		}

		public String getDescription() {
			return NLS.bind(
					UIText.FetchGerritChangePage_ContentAssistDescription,
					myChange.getPatchSetNumber(), myChange.getChangeNumber());
		}

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
