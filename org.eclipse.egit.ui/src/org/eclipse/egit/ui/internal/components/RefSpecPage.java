/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.SubmoduleConfig.FetchRecurseSubmodulesMode;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.PlatformUI;

/**
 * This wizard page allows user easy selection of specifications for push or
 * fetch (configurable).
 * <p>
 * Page is relying highly on {@link RefSpecPanel} component, see its description
 * for details.
 */
public class RefSpecPage extends WizardPage {

	private final Repository local;

	private final boolean pushPage;

	private RepositorySelection validatedRepoSelection;

	private RepositorySelection currentRepoSelection;

	private RefSpecPanel specsPanel;

	private Button saveButton;

	private Button tagsAutoFollowButton;

	private Button tagsFetchTagsButton;

	private Button tagsNoTagsButton;

	private Button recurseSubmodulesYesButton;

	private Button recurseSubmodulesNoButton;

	private Button recurseSubmodulesOnDemandButton;

	private String transportError;

	private UserPasswordCredentials credentials;

	private String helpContext = null;

	/**
	 * Create specifications selection page for provided context.
	 *
	 * @param local
	 *            local repository.
	 * @param pushPage
	 *            true if this page is used for push specifications selection,
	 *            false if it used for fetch specifications selection.
	 */
	public RefSpecPage(final Repository local, final boolean pushPage) {
		super(RefSpecPage.class.getName());
		this.local = local;
		this.pushPage = pushPage;
		if (pushPage) {
			setTitle(UIText.RefSpecPage_titlePush);
			setDescription(UIText.RefSpecPage_descriptionPush);
			setImageDescriptor(UIIcons.WIZBAN_PUSH);
		} else {
			setTitle(UIText.RefSpecPage_titleFetch);
			setDescription(UIText.RefSpecPage_descriptionFetch);
			setImageDescriptor(UIIcons.WIZBAN_FETCH);
		}

	}

	/**
	 * @param selection
	 */
	public void setSelection(RepositorySelection selection) {
		if (!selection.equals(validatedRepoSelection)) {
			currentRepoSelection = selection;
			setPageComplete(false);
		} else
			checkPage();
		revalidate();
	}

	/**
	 * @param credentials
	 */
	public void setCredentials(UserPasswordCredentials credentials) {
		this.credentials = credentials;
	}

	@Override
	public void createControl(Composite parent) {
		final Composite panel = new Composite(parent, SWT.NULL);
		panel.setLayout(new GridLayout());

		specsPanel = new RefSpecPanel(panel, pushPage);
		specsPanel.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		specsPanel.addRefSpecTableListener(new SelectionChangeListener() {
			@Override
			public void selectionChanged() {
				checkPage();
			}
		});

		if (!pushPage) {
			final Group tagsGroup = new Group(panel, SWT.NULL);
			tagsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					false));
			tagsGroup.setText(UIText.RefSpecPage_annotatedTagsGroup);
			tagsGroup.setLayout(new GridLayout());
			tagsAutoFollowButton = new Button(tagsGroup, SWT.RADIO);
			tagsAutoFollowButton
					.setText(UIText.RefSpecPage_annotatedTagsAutoFollow);
			tagsFetchTagsButton = new Button(tagsGroup, SWT.RADIO);
			tagsFetchTagsButton
					.setText(UIText.RefSpecPage_annotatedTagsFetchTags);
			tagsNoTagsButton = new Button(tagsGroup, SWT.RADIO);
			tagsNoTagsButton.setText(UIText.RefSpecPage_annotatedTagsNoTags);

			final Group recurseGroup = new Group(panel, SWT.NULL);
			recurseGroup.setLayoutData(
					new GridData(SWT.FILL, SWT.FILL, true, false));
			recurseGroup.setText(UIText.RefSpecPage_recurseSubmodulesGroup);
			recurseGroup.setLayout(new GridLayout());
			recurseSubmodulesYesButton = new Button(recurseGroup, SWT.RADIO);
			recurseSubmodulesYesButton
					.setText(UIText.RefSpecPage_recurseSubmodulesYes);
			recurseSubmodulesNoButton = new Button(recurseGroup, SWT.RADIO);
			recurseSubmodulesNoButton
					.setText(UIText.RefSpecPage_recurseSubmodulesNo);
			recurseSubmodulesOnDemandButton = new Button(recurseGroup,
					SWT.RADIO);
			recurseSubmodulesOnDemandButton
					.setText(UIText.RefSpecPage_recurseSubmodulesOnDemand);
		}

		saveButton = new Button(panel, SWT.CHECK);
		saveButton.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));

		Dialog.applyDialogFont(panel);
		setControl(panel);
		checkPage();
	}

	/**
	 * @return ref specifications as selected by user. Returned collection is a
	 *         copy, so it may be modified by caller.
	 */
	public List<RefSpec> getRefSpecs() {
		if (specsPanel == null)
			return Collections.emptyList();
		else
			return new ArrayList<>(specsPanel.getRefSpecs());
	}

	/**
	 * @return true if user chosen to save selected specification in remote
	 *         configuration, false otherwise.
	 */
	public boolean isSaveRequested() {
		return saveButton.getSelection();
	}

	/**
	 * @return selected tag fetching strategy. This result is relevant only for
	 *         fetch page.
	 */
	public TagOpt getTagOpt() {
		if (tagsAutoFollowButton.getSelection())
			return TagOpt.AUTO_FOLLOW;
		if (tagsFetchTagsButton.getSelection())
			return TagOpt.FETCH_TAGS;
		return TagOpt.NO_TAGS;
	}

	/**
	 * @return selected submodule recurse mode. This result is relevant only for
	 *         fetch page.
	 */
	public FetchRecurseSubmodulesMode getFetchRecurseSubmodulesMode() {
		if (recurseSubmodulesYesButton.getSelection()) {
			return FetchRecurseSubmodulesMode.YES;
		}
		if (recurseSubmodulesNoButton.getSelection()) {
			return FetchRecurseSubmodulesMode.NO;
		}

		return FetchRecurseSubmodulesMode.ON_DEMAND;
	}

	/**
	 * Compare provided specifications to currently selected ones.
	 *
	 * @param specs
	 *            specifications to compare to. May be null.
	 * @return true if provided specifications are equal to currently selected
	 *         ones, false otherwise.
	 */
	public boolean specsSelectionEquals(final List<RefSpec> specs) {
		return getRefSpecs().equals(specs);
	}

	/**
	 * Set the ID for context sensitive help
	 *
	 * @param id
	 *            help context
	 */
	public void setHelpContext(String id) {
		helpContext = id;
	}

	@Override
	public void performHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp(helpContext);
	}

	private void revalidate() {

		if (currentRepoSelection != null
				&& currentRepoSelection.equals(validatedRepoSelection)) {
			// nothing changed on previous page
			checkPage();
			return;
		}

		if (currentRepoSelection == null)
			return;

		specsPanel.clearRefSpecs();
		specsPanel.setEnable(false);
		saveButton.setVisible(false);
		saveButton.setSelection(false);
		validatedRepoSelection = null;
		transportError = null;
		getControl().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				revalidateImpl(currentRepoSelection);
			}
		});
	}

	private void revalidateImpl(final RepositorySelection newRepoSelection) {
		final ListRemoteOperation listRemotesOp;
		try {
			final URIish uri;
			uri = newRepoSelection.getURI(pushPage);
			int timeout = Activator.getDefault().getPreferenceStore().getInt(
					UIPreferences.REMOTE_CONNECTION_TIMEOUT);
			listRemotesOp = new ListRemoteOperation(local, uri, timeout);
			if (credentials != null)
				listRemotesOp
						.setCredentialsProvider(new EGitCredentialsProvider(
								credentials.getUser(), credentials.getPassword()));
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					listRemotesOp.run(monitor);
				}
			});
		} catch (InvocationTargetException e) {
			final Throwable cause = e.getCause();
			transportError(cause.getMessage());
			Activator
					.handleError(
							UIText.RefSpecPage_errorTransportDialogMessage,
							cause, true);
			return;
		} catch (InterruptedException e) {
			transportError(UIText.RefSpecPage_operationCancelled);
			return;
		}

		this.validatedRepoSelection = newRepoSelection;

		specsPanel.setAssistanceData(local, listRemotesOp.getRemoteRefs(),
				currentRepoSelection.getConfig());

		if (newRepoSelection.isConfigSelected()) {
			saveButton.setVisible(true);
			saveButton.setText(NLS.bind(UIText.RefSpecPage_saveSpecifications,
					currentRepoSelection.getConfigName()));
			saveButton.getParent().layout();

			if (!pushPage) {
				tagsAutoFollowButton.setSelection(false);
				tagsFetchTagsButton.setSelection(false);
				tagsNoTagsButton.setSelection(false);

				final TagOpt tagOpt = newRepoSelection.getConfig().getTagOpt();
				switch (tagOpt) {
				case AUTO_FOLLOW:
					tagsAutoFollowButton.setSelection(true);
					break;
				case FETCH_TAGS:
					tagsFetchTagsButton.setSelection(true);
					break;
				case NO_TAGS:
					tagsNoTagsButton.setSelection(true);
					break;
				}

				recurseSubmodulesYesButton.setSelection(false);
				recurseSubmodulesNoButton.setSelection(false);
				recurseSubmodulesOnDemandButton.setSelection(false);

				final FetchRecurseSubmodulesMode recurse = local.getConfig()
						.getEnum(ConfigConstants.CONFIG_FETCH_SECTION, null,
								ConfigConstants.CONFIG_KEY_RECURSE_SUBMODULES,
								FetchRecurseSubmodulesMode.ON_DEMAND);
				switch (recurse) {
				case YES:
					recurseSubmodulesYesButton.setSelection(true);
					break;
				case NO:
					recurseSubmodulesNoButton.setSelection(true);
					break;
				case ON_DEMAND:
					recurseSubmodulesOnDemandButton.setSelection(true);
					break;
				}
			}
		} else if (!pushPage) {
			tagsAutoFollowButton.setSelection(true);
			recurseSubmodulesOnDemandButton.setSelection(true);
		}

		checkPage();
	}

	private void transportError(final String message) {
		transportError = message;
		checkPage();
	}

	private void checkPage() {
		if (transportError != null) {
			setErrorMessage(transportError);
			setPageComplete(false);
			return;
		}
		if (!specsPanel.isEmpty() && specsPanel.isValid()
				&& !specsPanel.isMatchingAnyRefs()) {
			setErrorMessage(UIText.RefSpecPage_errorDontMatchSrc);
			setPageComplete(false);
			return;
		}
		setErrorMessage(specsPanel.getErrorMessage());
		setPageComplete(!specsPanel.isEmpty() && specsPanel.isValid());
	}
}
