/*******************************************************************************
 * Copyright (C) 2011, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.net.URISyntaxException;

import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.SWTUtils;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.egit.ui.internal.components.RepositorySelectionPage.Protocol;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page that offers simplified configuration if upstream repository
 * is hosted by Gerrit.
 */
public class GerritConfigurationPage extends WizardPage {

	private final static int GERRIT_DEFAULT_SSH_PORT = 29418;

	private static final String GERRIT_HTTP_PATH_PREFIX = "/r/p"; //$NON-NLS-1$

	private String helpContext = null;

	private Button configureGerrit;

	private Text branch;

	private Group branchGroup;

	private Group uriGroup;

	private Combo scheme;

	private Text uriText;

	private URIish pushURI;

	private Text user;

	private int eventDepth;

	GerritConfigurationPage() {
		super(GerritConfigurationPage.class.getName());
		setTitle(UIText.GerritConfigurationPage_title);
		setDescription(UIText.GerritConfigurationPage_PageDescription);
	}

	public void createControl(Composite parent) {
		final Composite panel = new Composite(parent, SWT.NULL);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		panel.setLayout(layout);

		createGerritCheckbox(panel);
		createURIGroup(panel);
		createBranchGroup(panel);

		Dialog.applyDialogFont(panel);
		setControl(panel);
		UIUtils.setEnabledRecursively(branchGroup,
				configureGerrit.getSelection());
	}

	private void createGerritCheckbox(Composite panel) {
		Composite comp = SWTUtils.createHFillComposite(panel,
				SWTUtils.MARGINS_NONE, 2);
		configureGerrit = new Button(comp, SWT.CHECK);
		configureGerrit.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				updateEnablement();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				updateEnablement();
			}
		});
		new Label(comp, SWT.NULL).setText(UIText.GerritConfigurationPage_configurePushToGerrit);
	}

	private void createURIGroup(Composite panel) {
		uriGroup = SWTUtils.createHFillGroup(panel,
				UIText.GerritConfigurationPage_pushUri,
				SWTUtils.MARGINS_DEFAULT, 2);

		scheme = new Combo(uriGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		uriText = SWTUtils.createText(uriGroup);

		new Label(uriGroup, SWT.NULL).setText((UIText.RepositorySelectionPage_promptUser + ":")); //$NON-NLS-1$
		user = SWTUtils.createText(uriGroup);
		user.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				eventDepth++;
				try {
					if (eventDepth == 1) {
						if (pushURI != null) {
							pushURI = pushURI.setUser(user.getText());
							uriText.setText(pushURI.toString());
							checkPage();
						}
					}
				} finally {
					eventDepth--;
				}
			}
		});

		uriText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				eventDepth++;
				try {
					if (eventDepth == 1) {
						URIish u = new URIish(uriText.getText());
						String newUser = u.getUser();
						user.setText(newUser != null ? newUser : ""); //$NON-NLS-1$
					}
				} catch (URISyntaxException e1) {
					// empty
				} finally {
					eventDepth--;
				}
				checkPage();
			}
		});

		for (Protocol p : Protocol.values()) {
			scheme.add(p.getDefaultScheme());
		}
		scheme.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				URIish oldPushURI = pushURI;
				final int idx = scheme.getSelectionIndex();
				pushURI = pushURI.setScheme(scheme.getItem(idx));

				if (Protocol.SSH.handles(pushURI))
					pushURI = pushURI.setPort(GERRIT_DEFAULT_SSH_PORT);
				else
					pushURI = pushURI.setPort(-1);

				if (isHttpProtocol(pushURI))
					pushURI = prependGerritHttpPathPrefix(pushURI);
				else if (isHttpProtocol(oldPushURI))
					pushURI = removeGerritHttpPathPrefix(pushURI);

				uriText.setText(pushURI.toString());
				scheme.setToolTipText(Protocol.values()[idx].getTooltip());
			}
		});
	}

	private void createBranchGroup(Composite panel) {
		branchGroup = SWTUtils.createHFillGroup(panel,
				UIText.GerritConfigurationPage_groupPush,
				SWTUtils.MARGINS_DEFAULT, 2);
		new Label(branchGroup, SWT.NULL).setText(UIText.GerritConfigurationPage_labelDestinationBranch);
		branch = SWTUtils.createText(branchGroup);
		branch.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				checkPage();
			}
		});
	}

	/**
	 * @return true if Gerrit configuration should be done
	 */
	public boolean configureGerrit() {
		return configureGerrit.getSelection();
	}

	/**
	 * @return the push URI for Gerrit code review
	 */
	public URIish getURI() {
		return pushURI;
	}

	/**
	 * @return the branch used in the gerrit push refspec: refs/for/branch
	 */
	public String getBranch() {
		return branch.getText();
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

	/**
	 * @param selection the source repository
	 */
	public void setSelection(RepositorySelection selection) {
		setDefaults(selection);
		checkPage();
		updateEnablement();
	}

	private void updateEnablement() {
		UIUtils.setEnabledRecursively(branchGroup,
				configureGerrit.getSelection());
		UIUtils.setEnabledRecursively(uriGroup, configureGerrit.getSelection());
	}

	private void setDefaults(RepositorySelection selection) {
		URIish uri = selection.getURI();
		URIish newPushURI = uri;
		if (Protocol.SSH.handles(uri)) {
			newPushURI = newPushURI.setPort(GERRIT_DEFAULT_SSH_PORT);
		} else if (Protocol.GIT.handles(uri)) {
			newPushURI = newPushURI.setScheme(Protocol.SSH.getDefaultScheme());
			newPushURI = newPushURI.setPort(GERRIT_DEFAULT_SSH_PORT);
		} else if (isHttpProtocol(uri)) {
			newPushURI = prependGerritHttpPathPrefix(newPushURI);
		}
		uriText.setText(newPushURI.toString());
		scheme.select(scheme.indexOf(newPushURI.getScheme()));
		branch.setText(Constants.MASTER);
	}

	private boolean isHttpProtocol(URIish uri) {
		return Protocol.HTTP.handles(uri) || Protocol.HTTPS.handles(uri);
	}

	/**
	 * @param u
	 * @return URI with path prefixed for Gerrit smart HTTP support
	 */
	private URIish prependGerritHttpPathPrefix(URIish u) {
		String path = u.getPath();
		if (!path.startsWith(GERRIT_HTTP_PATH_PREFIX))
			return u.setPath(GERRIT_HTTP_PATH_PREFIX + path);
		return u;
	}

	/**
	 * @param u
	 * @return URI without Gerrit smart HTTP path prefix
	 */
	private URIish removeGerritHttpPathPrefix(URIish u) {
		String path = u.getPath();
		if (path.startsWith(GERRIT_HTTP_PATH_PREFIX))
			return u.setPath(path.substring(4));
		return u;
	}

	private void checkPage() {
		if (!configureGerrit()) {
			setPageComplete(true);
			return;
		}

		try {
			pushURI = new URIish(uriText.getText());
			scheme.select(scheme.indexOf(pushURI.getScheme()));
		} catch (URISyntaxException e) {
			setErrorMessage(e.getLocalizedMessage());
			setPageComplete(false);
			return;
		}
		String branchName = branch.getText();
		if (branchName.length() == 0) {
			setErrorMessage(UIText.GerritConfigurationPage_errorBranchName);
			setPageComplete(false);
			return;
		}

		setErrorMessage(null);
		setPageComplete(true);
	}

}
