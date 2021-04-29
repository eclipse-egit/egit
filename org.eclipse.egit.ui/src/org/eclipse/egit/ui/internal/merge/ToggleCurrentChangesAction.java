/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.contentmergeviewer.ContentMergeViewer;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * An {@link Action} to toggle showing changes between the ancestor and the
 * 'current' ('ours') side in a three-way comparison.
 * <p>
 * Compare viewers with swapped sides are a nuisance. One would expect this to
 * be transparent and all "left" things simply mean "right" and vice versa.
 * Alas, this is not the case. The ContentMergeViewer swaps the inputs, but that
 * means that RangeDifference.LEFT still means "left" and now refers to
 * 'theirs'. Ugh.
 * </p>
 */
public class ToggleCurrentChangesAction extends CompareEditorInputViewerAction {

	/**
	 * The command ID for this action.
	 */
	public static final String COMMAND_ID = "org.eclipse.egit.ui.internal.merge.ToggleCurrentChangesCommand"; //$NON-NLS-1$

	/**
	 * Creates a new {@link ToggleCurrentChangesAction}.
	 *
	 * @param title
	 *            for the action
	 * @param comparison
	 *            to modify
	 */
	public ToggleCurrentChangesAction(String title,
			CompareEditorInput comparison) {
		super(title, IAction.AS_CHECK_BOX, comparison);
		CompareConfiguration config = comparison.getCompareConfiguration();
		setChecked(config.isChangeIgnored(currentSide(config)));
		if (config.isMirrored()) {
			setImageDescriptor(UIIcons.IGNORE_RIGHT_CHANGES);
		} else {
			setImageDescriptor(UIIcons.IGNORE_LEFT_CHANGES);
		}
		addEventListeners(config);
	}

	@SuppressWarnings("restriction")
	private void addEventListeners(CompareConfiguration config) {
		config.addPropertyChangeListener(event -> {
			if (org.eclipse.compare.internal.ICompareUIConstants.PROP_IGNORE_ANCESTOR
					.equals(event.getProperty())) {
				boolean threeWay = isThreeWay(config);
				if (threeWay) {
					super.setEnabled(true);
					if (isEnabled()) {
						run();
					}
				} else {
					super.setEnabled(false);
					forceOff();
				}
			}
		});
	}

	private boolean isThreeWay(CompareConfiguration config) {
		@SuppressWarnings("restriction")
		Object property = config.getProperty(
				org.eclipse.compare.internal.ICompareUIConstants.PROP_IGNORE_ANCESTOR);
		if (property instanceof Boolean) {
			return !((Boolean) property).booleanValue();
		} else if (property == null) {
			return true;
		}
		return !Boolean.parseBoolean(property.toString());
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(
				enabled && isThreeWay(getInput().getCompareConfiguration()));
	}

	@Override
	public void run() {
		ContentMergeViewer viewer = getViewer();
		if (viewer == null) {
			return;
		}
		Object input = viewer.getInput();
		if (input instanceof MergeDiffNode) {
			boolean ignoreLeft = isChecked();
			CompareConfiguration config = getInput().getCompareConfiguration();
			int side = currentSide(config);
			int otherSide = side == RangeDifference.LEFT ? RangeDifference.RIGHT
					: RangeDifference.LEFT;
			boolean anyDifference = ignoreLeft != config.isChangeIgnored(side);
			anyDifference |= ignoreLeft != config
					.isChangeIgnored(RangeDifference.ANCESTOR);
			anyDifference |= config.isChangeIgnored(otherSide);
			if (anyDifference) {
				config.setChangeIgnored(side, ignoreLeft);
				config.setChangeIgnored(RangeDifference.ANCESTOR, ignoreLeft);
				config.setChangeIgnored(otherSide, false);
				((MergeDiffNode) input).fireChange();
			}
		}
	}

	private void forceOff() {
		ContentMergeViewer viewer = getViewer();
		if (viewer == null) {
			return;
		}
		Object input = viewer.getInput();
		if (input instanceof MergeDiffNode) {
			CompareConfiguration config = getInput().getCompareConfiguration();
			boolean anyIgnored = config.isChangeIgnored(RangeDifference.LEFT);
			anyIgnored |= config.isChangeIgnored(RangeDifference.ANCESTOR);
			anyIgnored |= config.isChangeIgnored(RangeDifference.RIGHT);
			if (anyIgnored) {
				config.setChangeIgnored(RangeDifference.LEFT, false);
				config.setChangeIgnored(RangeDifference.ANCESTOR, false);
				config.setChangeIgnored(RangeDifference.RIGHT, false);
				((MergeDiffNode) input).fireChange();
			}
		}
	}

	private int currentSide(CompareConfiguration config) {
		if (config.isMirrored()) {
			return RangeDifference.RIGHT;
		}
		return RangeDifference.LEFT;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (CompareConfiguration.MIRRORED.equals(event.getProperty())) {
			if (getInput().getCompareConfiguration().isMirrored()) {
				setImageDescriptor(UIIcons.IGNORE_RIGHT_CHANGES);
			} else {
				setImageDescriptor(UIIcons.IGNORE_LEFT_CHANGES);
			}
			if (isEnabled()) {
				run();
			}
		}
	}
}
