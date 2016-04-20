/*******************************************************************************
 * Copyright (C) 2013 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.op.CreateLocalBranchOperation.UpstreamConfig;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * Component for configuring the upstream of a branch (merge, rebase).
 */
public class UpstreamConfigComponent {

	/**
	 * Listener for when the user has selected an upstream config.
	 */
	public interface UpstreamConfigSelectionListener {
		/**
		 * @param upstreamConfig
		 *            selected by the user
		 */
		public void upstreamConfigSelected(UpstreamConfig upstreamConfig);
	}

	private final Composite container;

	private Button configureUpstreamCheck;

	private Button mergeRadio;

	private Button rebaseRadio;

	private List<UpstreamConfigSelectionListener> listeners = new ArrayList<>();

	private Group upstreamConfigGroup;

	/**
	 * @param parent
	 *            the composite to use as a parent for the container
	 * @param style
	 *            the style of the container
	 */
	public UpstreamConfigComponent(Composite parent, int style) {
		container = new Composite(parent, style);
		container.setLayout(GridLayoutFactory.fillDefaults()
				.extendedMargins(0, 0, 0, 10).create());

		configureUpstreamCheck = new Button(container, SWT.CHECK);
		configureUpstreamCheck.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());
		configureUpstreamCheck
				.setText(UIText.UpstreamConfigComponent_ConfigureUpstreamCheck);
		configureUpstreamCheck
				.setToolTipText(UIText.UpstreamConfigComponent_ConfigureUpstreamToolTip);
		configureUpstreamCheck.setSelection(true);

		upstreamConfigGroup = new Group(container, SWT.SHADOW_ETCHED_IN);
		upstreamConfigGroup.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).indent(UIUtils.getControlIndent(), 0)
				.create());
		upstreamConfigGroup.setLayout(GridLayoutFactory.swtDefaults().create());
		upstreamConfigGroup
				.setText(UIText.UpstreamConfigComponent_PullGroup);

		mergeRadio = new Button(upstreamConfigGroup, SWT.RADIO);
		mergeRadio.setText(UIText.UpstreamConfigComponent_MergeRadio);
		mergeRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				upstreamConfigSelected();
			}
		});
		mergeRadio.setSelection(true);

		rebaseRadio = new Button(upstreamConfigGroup, SWT.RADIO);
		rebaseRadio.setText(UIText.UpstreamConfigComponent_RebaseRadio);
		rebaseRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				upstreamConfigSelected();
			}
		});

		configureUpstreamCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateEnabled();
				upstreamConfigSelected();
			}
		});
	}

	/**
	 * @return the container which holds all the controls
	 */
	public Composite getContainer() {
		return container;
	}

	/**
	 * @param listener
	 *            to add
	 */
	public void addUpstreamConfigSelectionListener(
			UpstreamConfigSelectionListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param upstreamConfig
	 *            to set the controls to
	 */
	public void setUpstreamConfig(UpstreamConfig upstreamConfig) {
		if (upstreamConfig == UpstreamConfig.NONE) {
			configureUpstreamCheck.setSelection(false);
		} else {
			configureUpstreamCheck.setSelection(true);
			mergeRadio.setSelection(upstreamConfig == UpstreamConfig.MERGE);
			rebaseRadio.setSelection(upstreamConfig == UpstreamConfig.REBASE);
		}
		updateEnabled();
	}

	private void upstreamConfigSelected() {
		UpstreamConfig config = getSelectedUpstreamConfig();
		for (UpstreamConfigSelectionListener listener : listeners)
			listener.upstreamConfigSelected(config);
	}

	private UpstreamConfig getSelectedUpstreamConfig() {
		if (!configureUpstreamCheck.getSelection())
			return UpstreamConfig.NONE;
		else if (mergeRadio.getSelection())
			return UpstreamConfig.MERGE;
		else if (rebaseRadio.getSelection())
			return UpstreamConfig.REBASE;
		return UpstreamConfig.NONE;
	}

	private void updateEnabled() {
		boolean enabled = configureUpstreamCheck.getSelection();
		upstreamConfigGroup.setEnabled(enabled);
		mergeRadio.setEnabled(enabled);
		rebaseRadio.setEnabled(enabled);
	}
}
