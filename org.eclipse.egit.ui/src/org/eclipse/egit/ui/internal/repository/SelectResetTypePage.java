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
package org.eclipse.egit.ui.internal.repository;

import java.io.IOException;

import org.eclipse.egit.core.op.ResetOperation.ResetType;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
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

	private RevCommit getLatestCommit(String branch) {
		ObjectId resolved;
		try {
			resolved = repo.resolve(branch);
		} catch (IOException e) {
			return null;
		}
		if (resolved == null)
			return null;
		RevWalk walk = new RevWalk(repo);
		walk.setRetainBody(true);
		try {
			return walk.parseCommit(resolved);
		} catch (IOException ignored) {
			return null;
		} finally {
			walk.release();
		}
	}

	public void createControl(Composite parent) {
		Composite displayArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false)
				.applyTo(displayArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(displayArea);

		RevCommit currentCommit = getLatestCommit(current);

		Label currentLabel = new Label(displayArea, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER)
				.applyTo(currentLabel);
		currentLabel.setText(UIText.SelectResetTypePage_labelCurrentHead);
		currentLabel.setToolTipText(UIText.SelectResetTypePage_tooltipCurrentHead);

		Composite currentArea = new Composite(displayArea, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false)
				.applyTo(currentArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(currentArea);

		CLabel currentValue = new CLabel(currentArea, SWT.NONE);
		GridDataFactory.swtDefaults().applyTo(currentValue);
		Image currentIcon = getIcon(current);
		UIUtils.hookDisposal(currentValue, currentIcon);
		currentValue.setImage(currentIcon);
		currentValue.setText(Repository.shortenRefName(current));

		if (currentCommit != null) {
			CLabel commitLabel = new CLabel(currentArea, SWT.NONE);
			Image commitIcon = UIIcons.CHANGESET.createImage();
			UIUtils.hookDisposal(commitLabel, commitIcon);
			commitLabel.setImage(commitIcon);
			commitLabel.setText(currentCommit.abbreviate(7).name() + ":  " //$NON-NLS-1$
					+ currentCommit.getShortMessage());
			if (isCommit(current))
				((GridData) currentValue.getLayoutData()).exclude = true;
		}

		RevCommit targetCommit = getLatestCommit(target);

		Label targetLabel = new Label(displayArea, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER)
				.applyTo(targetLabel);
		targetLabel.setText(UIText.SelectResetTypePage_labelResettingTo);
		targetLabel.setToolTipText(UIText.SelectResetTypePage_tooltipResettingTo);

		Composite targetArea = new Composite(displayArea, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false)
				.applyTo(targetArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(targetArea);

		CLabel targetValue = new CLabel(targetArea, SWT.NONE);
		GridDataFactory.swtDefaults().applyTo(targetValue);
		Image targetIcon = getIcon(target);
		UIUtils.hookDisposal(targetValue, targetIcon);
		targetValue.setImage(targetIcon);
		targetValue.setText(Repository.shortenRefName(target));

		if (targetCommit != null) {
			CLabel commitLabel = new CLabel(targetArea, SWT.NONE);
			Image commitIcon = UIIcons.CHANGESET.createImage();
			UIUtils.hookDisposal(commitLabel, commitIcon);
			commitLabel.setImage(commitIcon);
			commitLabel.setText(targetCommit.abbreviate(7).name() + ":  " //$NON-NLS-1$
					+ targetCommit.getShortMessage());
			if (isCommit(target))
				((GridData) targetValue.getLayoutData()).exclude = true;
		} else
			GridDataFactory.swtDefaults().span(2, 1).applyTo(targetLabel);

		Group g = new Group(displayArea, SWT.NONE);
		g.setText(UIText.ResetTargetSelectionDialog_ResetTypeGroup);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(g);
		GridLayoutFactory.swtDefaults().applyTo(g);

		Button soft = new Button(g, SWT.RADIO);
		soft.setText(UIText.ResetTargetSelectionDialog_ResetTypeSoftButton);
		soft.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection())
					resetType = ResetType.SOFT;
			}
		});

		Button medium = new Button(g, SWT.RADIO);
		medium.setSelection(true);
		medium.setText(UIText.ResetTargetSelectionDialog_ResetTypeMixedButton);
		medium.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection())
					resetType = ResetType.MIXED;
			}
		});

		Button hard = new Button(g, SWT.RADIO);
		hard.setText(UIText.ResetTargetSelectionDialog_ResetTypeHardButton);
		hard.addListener(SWT.Selection, new Listener() {
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
