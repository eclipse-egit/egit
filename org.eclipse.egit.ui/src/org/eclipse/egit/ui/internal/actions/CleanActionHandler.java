/*******************************************************************************
 * Copyright (C) 2011, Chris Aniszczyk <zx@redhat.com>
 * Copyright (C) 2011, Abhishek Bhatnagar <abhatnag@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.CleanOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.job.JobUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.dialogs.ListSelectionDialog;

/**
 * This operation cleans the repository
 *
 * @see CleanOperation
 */
public class CleanActionHandler extends RepositoryActionHandler {
	/**
	 *
	 */
	public Repository repo;

	private Set<String> fileListReturned;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IResource[] resources = getSelectedResources(event);
		repo = getRepository(true, event);
		fileListReturned = new HashSet<String>();

		// checks
		if (repo == null)
			return null;
		if (resources.length == 0)
			return null;

		// Do a dry run on CleanCommand to get list of files that would be deleted
		CleanOperation op = new CleanOperation(resources);
		Set<String> fileList = op.dryRun();

		// create dialog
		ListSelectionDialog dialog = new ListSelectionDialog(
												getShell(event), // shell
												fileList, // set of file names to populate
												new ArrayContentProvider(), // acp
												new ItemLabelProvider(), // ilp
												UIText.CleanDialog_HeaderMessage // branch name
									);
		dialog.setTitle(UIText.CleanDialog_TitleMessage);

		// if cancel clicked, return
		if (dialog.open() != IDialogConstants.OK_ID)
			return null;

		// get user selected files
		for (int i = 0; i < dialog.getResult().length; i++) {
			fileListReturned.add(dialog.getResult()[i].toString());
		}

		// run clean job
		JobUtil.scheduleUserJob(op.setPaths(fileListReturned), "Clean", //$NON-NLS-1$
				JobFamilies.CLEAN);

		// refresh work-tree in package explorer
		try {
			ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
					IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return null;
	}

	class ItemLabelProvider implements ILabelProvider {
		public org.eclipse.swt.graphics.Image getImage(Object element) {
			//return RepositoryTreeNodeType.REF.getIcon();
			return null;
		}
		public void dispose() {
			// TODO Auto-generated method stub
		}
		public String getText(Object element) {
			// TODO Auto-generated method stub
			return (String) element;
		}
		public boolean isLabelProperty(Object element, String property) {
			// TODO Auto-generated method stub
			return false;
		}
		public void addListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub
		}
		public void removeListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub
		}
	}
}
