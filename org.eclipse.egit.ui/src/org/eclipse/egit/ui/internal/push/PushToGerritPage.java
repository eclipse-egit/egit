/*******************************************************************************
 * Copyright (c) 2012 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.core.op.PushOperationSpecification;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Push the current HEAD to Gerrit
 */
class PushToGerritPage extends WizardPage {
	private static final String PUSH_TO_GERRIT_PAGE_SECTION = "PushToGerritPage"; //$NON-NLS-1$

	private static final String LAST_URI_POSTFIX = ".lastUri"; //$NON-NLS-1$

	private static final String LAST_BRANCH_POSTFIX = ".lastBranch"; //$NON-NLS-1$

	private final Repository repository;

	private final IDialogSettings settings;

	private final String lastUriKey;

	private final String lastBranchKey;

	private Combo uriCombo;

	private Combo prefixCombo;

	private Label branchTextlabel;

	private Text branchText;

	private Text reviewerText;

	/**
	 * @param repository
	 */
	PushToGerritPage(Repository repository) {
		super(PushToGerritPage.class.getName());
		this.repository = repository;
		setTitle(NLS.bind(UIText.PushToGerritPage_Title, Activator.getDefault()
				.getRepositoryUtil().getRepositoryName(repository)));
		setMessage(UIText.PushToGerritPage_Message);
		settings = getDialogSettings();
		lastUriKey = repository + LAST_URI_POSTFIX;
		lastBranchKey = repository + LAST_BRANCH_POSTFIX;
	}

	protected IDialogSettings getDialogSettings() {
		IDialogSettings s = Activator.getDefault().getDialogSettings();
		IDialogSettings section = s.getSection(PUSH_TO_GERRIT_PAGE_SECTION);
		if (section == null)
			section = s.addNewSection(PUSH_TO_GERRIT_PAGE_SECTION);
		return section;
	}

	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		new Label(main, SWT.NONE).setText(UIText.PushToGerritPage_UriLabel);
		uriCombo = new Combo(main, SWT.DROP_DOWN);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.applyTo(uriCombo);
		uriCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		branchTextlabel = new Label(main, SWT.NONE);

		// we visualize the prefix here
		prefixCombo = new Combo(main, SWT.READ_ONLY | SWT.DROP_DOWN);
		prefixCombo.add("refs/for/"); //$NON-NLS-1$
		prefixCombo.add("refs/drafts/"); //$NON-NLS-1$
		prefixCombo.select(0);

		branchTextlabel.setText(UIText.PushToGerritPage_BranchLabel);
		branchText = new Text(main, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(branchText);
		branchText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		// give focus to the nameText if label is activated using the mnemonic
		branchTextlabel.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				branchText.setFocus();
				branchText.selectAll();
			}
		});
		addRefContentProposalToText(branchText);

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
		setLastUsedBranch();
		branchText.setFocus();

		Label reviewerLabel = new Label(main, SWT.NONE);
		reviewerLabel.setText("Reviewer:"); //$NON-NLS-1$

		reviewerText = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
		.applyTo(reviewerText);

		Dialog.applyDialogFont(main);
		setControl(main);
	}

	private void storeLastUsedUri(String uri) {
		settings.put(lastUriKey, uri.trim());
	}

	private void storeLastUsedBranch(String branch) {
		settings.put(lastBranchKey, branch.trim());
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

	private void setLastUsedBranch() {
		String lastBranch = settings.get(lastBranchKey);
		if (lastBranch != null)
			branchText.setText(lastBranch);
	}

	private void checkPage() {
		setErrorMessage(null);
		try {
			if (uriCombo.getText().length() == 0) {
				setErrorMessage(UIText.PushToGerritPage_MissingUriMessage);
				return;
			}
			if (branchText.getText().length() == 0) {
				setErrorMessage(UIText.PushToGerritPage_MissingBranchMessage);
				return;
			}
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

	void doPush(IProgressMonitor monitor) {
		try {
			int timeout = Activator.getDefault().getPreferenceStore()
					.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
			URIish uri = new URIish(uriCombo.getText());
			Ref currentHead = repository.getRef(Constants.HEAD);
			RemoteRefUpdate update = new RemoteRefUpdate(repository,
					currentHead, prefixCombo.getItem(prefixCombo
							.getSelectionIndex()) + branchText.getText(),
					false, null, null);
			PushOperationSpecification spec = new PushOperationSpecification();

			spec.addURIRefUpdates(uri, Arrays.asList(update));
			PushOperationUI op = new PushOperationUI(repository, spec, timeout,
					false);
			op.setCredentialsProvider(new EGitCredentialsProvider());

			// It's just a prototype to present that it really works
			op.setReceivePack(String.format("git receive-pack --reviewer=%s", reviewerText.getText())); //$NON-NLS-1$
			PushOperationResult result = op.execute(monitor);
			PushResultDialog dlg = new PushResultDialog(getShell(), repository,
					result, op.getDestinationString());
			dlg.showConfigureButton(false);
			dlg.open();
			storeLastUsedUri(uriCombo.getText());
			storeLastUsedBranch(branchText.getText());
		} catch (CoreException e) {
			Activator.handleError(e.getMessage(), e, true);
		} catch (URISyntaxException e) {
			Activator.handleError(e.getMessage(), e, true);
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
	}

	private void addRefContentProposalToText(final Text textField) {
		KeyStroke stroke;
		try {
			stroke = KeyStroke.getInstance("CTRL+SPACE"); //$NON-NLS-1$
			UIUtils.addBulbDecorator(textField, NLS.bind(
					UIText.PushToGerritPage_ContentProposalHoverText,
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
				if (!patternString.endsWith(".*")) //$NON-NLS-1$
					patternString = patternString + ".*"; //$NON-NLS-1$

				// let's compile a case-insensitive pattern (assumes ASCII only)
				Pattern pattern;
				try {
					pattern = Pattern.compile(patternString,
							Pattern.CASE_INSENSITIVE);
				} catch (PatternSyntaxException e) {
					pattern = null;
				}

				Set<String> proposals = new TreeSet<String>();

				try {
					Set<String> remotes = repository.getRefDatabase()
							.getRefs(Constants.R_REMOTES).keySet();
					for (String remote : remotes) {
						// these are "origin/master", "origin/xxx"...
						int slashIndex = remote.indexOf('/');
						if (slashIndex > 0 && slashIndex < remote.length() - 1)
							proposals
									.add(remote.substring(remote.indexOf('/') + 1));
					}
				} catch (IOException e) {
					// simply ignore, no proposals then
				}

				for (final String proposal : proposals) {
					if (pattern != null && !pattern.matcher(proposal).matches())
						continue;
					IContentProposal propsal = new BranchContentProposal(
							proposal);
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

	private final static class BranchContentProposal implements
			IContentProposal {
		private final String myString;

		BranchContentProposal(String string) {
			myString = string;
		}

		public String getContent() {
			return myString;
		}

		public int getCursorPosition() {
			return 0;
		}

		public String getDescription() {
			return myString;
		}

		public String getLabel() {
			return myString;
		}

		@Override
		public String toString() {
			return getContent();
		}
	}
}
