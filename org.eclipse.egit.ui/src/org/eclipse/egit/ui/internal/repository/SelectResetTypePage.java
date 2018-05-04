/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * Copyright (C) 2013 Robin Rosenberg
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.IOException;

import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

/**
 * Asks the user for a {@link ResetType}
 */
public class SelectResetTypePage extends WizardPage {

	private ResetType resetType = ResetType.MIXED;

	private final Repository repo;

	private final String current;

	private final String target;

	/**
	 * @param repoName
	 *            the repository name
	 * @param repository
	 *            the repository being reset
	 * @param currentRef
	 *            current ref (which will be overwritten)
	 * @param targetRef
	 *            target ref (which contains the new content)
	 */
	public SelectResetTypePage(String repoName, Repository repository,
			String currentRef, String targetRef) {
		super(SelectResetTypePage.class.getName());
		setTitle(NLS.bind(UIText.SelectResetTypePage_PageTitle, repoName));
		setMessage(UIText.SelectResetTypePage_PageMessage);

		repo = repository;
		current = currentRef;
		target = targetRef;
	}

	private Image getIcon(final String ref) {
		if (ref.startsWith(Constants.R_TAGS))
			return UIIcons.TAG.createImage();
		else if (ref.startsWith(Constants.R_HEADS)
				|| ref.startsWith(Constants.R_REMOTES))
			return UIIcons.BRANCH.createImage();
		else
			return UIIcons.CHANGESET.createImage();
	}

	private boolean isCommit(final String ref) {
		return !ref.startsWith(Constants.R_REFS);
	}

	private String formatCommit(final RevCommit commit) {
		return commit.abbreviate(7).name() + ":  " + commit.getShortMessage(); //$NON-NLS-1$
	}

	private RevCommit getLatestCommit(String branch) {
		ObjectId resolved;
		try {
			resolved = repo.resolve(branch);
		} catch (IOException e) {
			return null;
		}
		if (resolved == null)
			return null;
		try (RevWalk walk = new RevWalk(repo)) {
			walk.setRetainBody(true);
			return walk.parseCommit(resolved);
		} catch (IOException ignored) {
			return null;
		}
	}

	@SuppressWarnings("unused")
	@Override
	public void createControl(Composite parent) {
		Composite displayArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false)
				.applyTo(displayArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(displayArea);

		RevCommit currentCommit = getLatestCommit(current);

		Label currentLabel = new Label(displayArea, SWT.NONE);
		currentLabel.setText(UIText.SelectResetTypePage_labelCurrentHead);
		currentLabel
				.setToolTipText(UIText.SelectResetTypePage_tooltipCurrentHead);

		CLabel currentValue = new CLabel(displayArea, SWT.NONE);
		GridDataFactory.swtDefaults().applyTo(currentValue);
		Image currentIcon = getIcon(current);
		UIUtils.hookDisposal(currentValue, currentIcon);
		currentValue.setImage(currentIcon);
		currentValue.setText(Repository.shortenRefName(current));

		if (currentCommit != null) {
			if (isCommit(current))
				currentValue.setText(formatCommit(currentCommit));
			else {
				new Label(displayArea, SWT.NONE);
				CLabel commitLabel = new CLabel(displayArea, SWT.NONE);
				Image commitIcon = UIIcons.CHANGESET.createImage();
				UIUtils.hookDisposal(commitLabel, commitIcon);
				commitLabel.setImage(commitIcon);
				commitLabel.setText(formatCommit(currentCommit));
			}
		}

		boolean resetToSelf = current.equals(target);
		if (!resetToSelf) {
			RevCommit targetCommit = getLatestCommit(target);

			Label targetLabel = new Label(displayArea, SWT.NONE);
			targetLabel.setText(UIText.SelectResetTypePage_labelResettingTo);
			targetLabel
					.setToolTipText(UIText.SelectResetTypePage_tooltipResettingTo);

			CLabel targetValue = new CLabel(displayArea, SWT.NONE);
			Image targetIcon = getIcon(target);
			UIUtils.hookDisposal(targetValue, targetIcon);
			targetValue.setImage(targetIcon);
			targetValue.setText(Repository.shortenRefName(target));

			if (targetCommit != null) {
				if (isCommit(target))
					targetValue.setText(formatCommit(targetCommit));
				else {
					new Label(displayArea, SWT.NONE);
					CLabel commitLabel = new CLabel(displayArea, SWT.NONE);
					Image commitIcon = UIIcons.CHANGESET.createImage();
					UIUtils.hookDisposal(commitLabel, commitIcon);
					commitLabel.setImage(commitIcon);
					commitLabel.setText(formatCommit(targetCommit));
				}
			}
		}

		Group g = new Group(displayArea, SWT.NONE);
		g.setText(UIText.ResetTargetSelectionDialog_ResetTypeGroup);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.indent(0, 5).applyTo(g);
		GridLayoutFactory.swtDefaults().applyTo(g);

		if (!resetToSelf) {
			Button soft = new Button(g, SWT.RADIO);
			soft.setText(UIText.ResetTargetSelectionDialog_ResetTypeSoftButton);
			soft.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if (((Button) event.widget).getSelection())
						resetType = ResetType.SOFT;
				}
			});
		}

		Button medium = new Button(g, SWT.RADIO);
		medium.setSelection(true);
		medium.setText(resetToSelf ? UIText.ResetTargetSelectionDialog_ResetTypeHEADMixedButton : UIText.ResetTargetSelectionDialog_ResetTypeMixedButton);
		medium.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection())
					resetType = ResetType.MIXED;
			}
		});

		Button hard = new Button(g, SWT.RADIO);
		hard.setText(resetToSelf ? UIText.ResetTargetSelectionDialog_ResetTypeHEADHardButton : UIText.ResetTargetSelectionDialog_ResetTypeHardButton);
		hard.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection())
					resetType = ResetType.HARD;
			}
		});

		Dialog.applyDialogFont(displayArea);
		setControl(displayArea);
	}

	/**
	 * @return the reset type
	 */
	public ResetType getResetType() {
		return resetType;
	}

}
