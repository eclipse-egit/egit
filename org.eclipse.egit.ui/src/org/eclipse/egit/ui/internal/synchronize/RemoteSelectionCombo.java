/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.util.List;

import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.synchronize.SyncRepoEntity.SyncRefEntity;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

class RemoteSelectionCombo extends Composite {

	private final Combo refsCombo;

	private final Combo remotesCombo;

	private final List<SyncRepoEntity> syncRepos;

	public RemoteSelectionCombo(Composite parent, List<SyncRepoEntity> syncRepos) {
		super(parent, SWT.NONE);
		this.syncRepos = syncRepos;

		setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());

		Label baseLabel = new Label(this, SWT.NONE);
		baseLabel.setText(UIText.RemoteSelectionCombo_remote);
		baseLabel.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).span(2, 1).create());

		remotesCombo = new Combo(this, SWT.NONE);
		refsCombo = new Combo(this, SWT.NONE);

		remotesCombo.setLayoutData(GridDataFactory.fillDefaults().grab(true,
				false).create());
		refsCombo.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());

		for (SyncRepoEntity syncRepoEnt : syncRepos) {
			remotesCombo.add(syncRepoEnt.getName());
		}

		remotesCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fillRefsCombo();
			}
		});
	}

	public String getValue() {
		int refSelectedIndex = refsCombo.getSelectionIndex();
		int remoteSelectedIndex = remotesCombo.getSelectionIndex();

		if (remoteSelectedIndex < 0 && refSelectedIndex < 0) {
			return ""; //$NON-NLS-1$
		}

		return syncRepos.get(remoteSelectedIndex).getRefList().get(
				refSelectedIndex).getValue();
	}

	private void fillRefsCombo() {
		int selected = remotesCombo.getSelectionIndex();
		if (selected < 0) {
			return;
		}

		refsCombo.removeAll();
		SyncRepoEntity syncRepoEnt = syncRepos.get(selected);
		for (SyncRefEntity syncRefEnt : syncRepoEnt.getRefList()) {
			refsCombo.add(syncRefEnt.getDescription());
		}

		if (refsCombo.getItemCount() > 0) {
			refsCombo.setText(refsCombo.getItem(0));
		}
	}

}
