/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.components.RefContentProposal;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Obtain the Fetch Source, i.e. the {@link Ref} on the remote
 * {@link Repository}
 */
public class FetchDestinationPage extends WizardPage {
	private final Repository repository;

	private Text sourceText;

	private Text destinationText;

	private Button force;

	private List<Ref> trackingBranches;

	/**
	 * Default constructor
	 *
	 * @param repository
	 */
	public FetchDestinationPage(Repository repository) {
		super(FetchDestinationPage.class.getName());
		this.repository = repository;
		setTitle(UIText.FetchDestinationPage_PageTitle);
	}

	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		Label repositoryLabel = new Label(main, SWT.NONE);
		repositoryLabel.setText(UIText.FetchDestinationPage_RepositoryLabel);
		Text repositoryText = new Text(main, SWT.READ_ONLY | SWT.BORDER);
		repositoryText.setText(Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository));
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(repositoryText);

		Label sourceLabel = new Label(main, SWT.NONE);
		sourceLabel.setText(UIText.FetchDestinationPage_SourceLabel);

		sourceText = new Text(main, SWT.READ_ONLY | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sourceText);

		Label destinationLabel = new Label(main, SWT.NONE);
		destinationLabel.setText(UIText.FetchDestinationPage_DestinationLabel);
		destinationText = new Text(main, SWT.BORDER);
		destinationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(
				destinationText);
		addProposal(destinationText);

		force = new Button(main, SWT.CHECK);
		force.setText(UIText.FetchDestinationPage_ForceCheckbox);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(
				force);

		checkPage();
		destinationText.setFocus();
		setControl(main);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			FetchSourcePage fsp = (FetchSourcePage) getWizard()
					.getPreviousPage(this);
			String sourceString = fsp.getSource();
			sourceText.setText(sourceString);
			if (sourceString.length() > 0) {
				destinationText.setText(Constants.R_REMOTES
						+ Repository.shortenRefName(sourceString));
			}
			destinationText.setFocus();
		}
	}

	/**
	 * @return the source
	 */
	public String getDestination() {
		return destinationText.getText();
	}

	/**
	 * @return if force is checked
	 */
	public boolean isForce() {
		return force.getSelection();
	}

	private List<Ref> getRemoteRefs() {
		if (this.trackingBranches == null) {
			List<Ref> proposals = new ArrayList<Ref>();
			try {
				for (Ref ref : repository.getRefDatabase().getRefs(
						Constants.R_REMOTES).values()) {
					proposals.add(ref);
				}
				this.trackingBranches = proposals;
			} catch (IOException e) {
				setErrorMessage(UIText.FetchDestinationPage_CouldNotGetBranchesMessage);
			}
		}
		return this.trackingBranches;
	}

	private void addProposal(final Text textField) {
		KeyStroke stroke;
		try {
			stroke = KeyStroke.getInstance("M1+SPACE"); //$NON-NLS-1$
			UIUtils.addBulbDecorator(textField, NLS.bind(
					UIText.UIUtils_PressShortcutMessage, stroke.format()));
		} catch (ParseException e1) {
			Activator.handleError(e1.getMessage(), e1, false);
			stroke = null;
			UIUtils.addBulbDecorator(textField,
					UIText.UIUtils_StartTypingForPreviousValuesMessage);
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

				List<Ref> proposals = getRemoteRefs();

				if (proposals != null)
					for (final Ref ref : proposals) {
						final String shortenedName = Repository
								.shortenRefName(ref.getName());

						if (pattern != null
								&& !pattern.matcher(ref.getName()).matches()
								&& !pattern.matcher(shortenedName).matches())
							continue;

						IContentProposal propsal = new RefContentProposal(
								repository, ref);
						resultList.add(propsal);
					}

				return resultList.toArray(new IContentProposal[resultList
						.size()]);
			}
		};

		ContentProposalAdapter adapter = new ContentProposalAdapter(textField,
				new TextContentAdapter(), cp, stroke,
				UIUtils.VALUE_HELP_ACTIVATIONCHARS);
		// set the acceptance style to always replace the complete content
		adapter
				.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	}

	private void checkPage() {
		setMessage(null, IMessageProvider.WARNING);
		setErrorMessage(null);
		setMessage(UIText.FetchDestinationPage_PageMessage);
		if (destinationText.getText().length() == 0) {
			setPageComplete(false);
			return;
		}
		boolean found = false;
		for (Ref ref : getRemoteRefs()) {
			if (ref.getName().equals(destinationText.getText()))
				found = true;
		}
		if (!found)
			setMessage(NLS.bind(
					UIText.FetchDestinationPage_TrackingBranchNotFoundMessage,
					destinationText.getText()), IMessageProvider.WARNING);
		setPageComplete(true);
	}
}
