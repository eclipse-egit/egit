/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import static org.eclipse.jgit.lib.Constants.HEAD;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.synchronize.GitModelSynchronize;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 *
 */
public class SynchronizeWithMenu extends ContributionItem implements
		IWorkbenchContribution {

	private final Image tagImage;

	private final Image branchImage;

	private ISelectionService srv;

	/**
	 *
	 */
	public SynchronizeWithMenu() {
		this(null);
	}

	/**
	 * @param id
	 */
	public SynchronizeWithMenu(String id) {
		super(id);

		tagImage = UIIcons.TAG.createImage();
		branchImage = UIIcons.BRANCH.createImage();
	}

	@Override
	public void fill(Menu menu, int index) {
		if (srv == null)
			return;
		final IProject selectedProject = getSelection();
		if (selectedProject == null)
			return;

		RepositoryMapping mapping = RepositoryMapping
				.getMapping(selectedProject);
		if (mapping == null)
			return;

		final Repository repo = mapping.getRepository();
		if (repo == null)
			return;

		List<Ref> refs = new LinkedList<Ref>();
		RefDatabase refDatabase = repo.getRefDatabase();
		try {
			refs.addAll(refDatabase.getAdditionalRefs());
		} catch (IOException e) {
			// do nothing
		}
		try {
			refs.addAll(refDatabase.getRefs(RefDatabase.ALL).values());
		} catch (IOException e) {
			// do nothing
		}
		Collections.sort(refs, CommonUtils.REF_ASCENDING_COMPARATOR);
		String currentBranch;
		try {
			currentBranch = repo.getFullBranch();
		} catch (IOException e) {
			currentBranch = ""; //$NON-NLS-1$
		}

		for (Ref ref : refs) {
			final String name = ref.getName();
			if (name.equals(Constants.HEAD) || name.equals(currentBranch))
				continue;

			MenuItem item = new MenuItem(menu, SWT.PUSH);
			item.setText(name);
			if (name.startsWith(Constants.R_TAGS))
				item.setImage(tagImage);
			else if (name.startsWith(Constants.R_HEADS) || name.startsWith(Constants.R_REMOTES))
				item.setImage(branchImage);
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					GitSynchronizeData data;
					try {
						data = new GitSynchronizeData(repo, HEAD, name, true);
						GitModelSynchronize.launch(data, new IResource[] { selectedProject });
					} catch (IOException e) {
						Activator.logError(e.getMessage(), e);
					}
				}
			});
		}
	}

	private IProject getSelection() {
		ISelection sel = srv.getSelection();

		if (!(sel instanceof IStructuredSelection))
			return null;

		Object selected = ((IStructuredSelection) sel).getFirstElement();
		if (selected instanceof IAdaptable)
			return (IProject) ((IAdaptable) selected)
					.getAdapter(IProject.class);
		if (selected instanceof IProject)
			return (IProject) selected;

		return null;
	}

	public void initialize(IServiceLocator serviceLocator) {
		srv = (ISelectionService) serviceLocator
		.getService(ISelectionService.class);
	}

	@Override
	public boolean isDynamic() {
		return true;
	}


	@Override
	public void dispose() {
		tagImage.dispose();
		branchImage.dispose();
	}

}
