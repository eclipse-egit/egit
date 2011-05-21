/*******************************************************************************
 * Copyright (C) 2009, Yann Simon <yann.simon.fr@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.EditableRevision;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEditor.PathEdit;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The "compare with index" action. This action opens a diff editor comparing
 * the file as found in the working directory and the version found in the index
 * of the repository.
 */
public class CompareWithIndexActionHandler extends RepositoryActionHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// assert all resources map to the same repository
		if (getRepository(true, event) == null)
			return null;
		final IResource[] resources = getSelectedResources(event);

		if (resources.length == 1 && resources[0] instanceof IFile) {
			final IFile baseFile = (IFile) resources[0];

			final ITypedElement base = SaveableCompareEditorInput
					.createFileElement(baseFile);

			final ITypedElement next;
			try {
				next = getHeadTypedElement(baseFile);
			} catch (IOException e) {
				Activator.handleError(
						UIText.CompareWithIndexAction_errorOnAddToIndex, e,
						true);
				return null;
			}

			final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
					base, next, null);
			IWorkbenchPage workBenchPage = HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage();
			CompareUtils.openInCompare(workBenchPage, in);
		} else {
			CompareTreeView view;
			try {
				view = (CompareTreeView) PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage().showView(
								CompareTreeView.ID);
				view.setInput(resources, CompareTreeView.INDEX_VERSION);
			} catch (PartInitException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		}
		return null;
	}

	private ITypedElement getHeadTypedElement(final IFile baseFile)
			throws IOException {
		final RepositoryMapping mapping = RepositoryMapping.getMapping(baseFile
				.getProject());
		final Repository repository = mapping.getRepository();
		final String gitPath = mapping.getRepoRelativePath(baseFile);

		DirCache dc = repository.lockDirCache();
		final DirCacheEntry entry = dc.getEntry(gitPath);
		dc.unlock();
		if (entry == null) {
			// the file cannot be found in the index
			return new GitCompareFileRevisionEditorInput.EmptyTypedElement(NLS
					.bind(UIText.CompareWithIndexAction_FileNotInIndex,
							baseFile.getName()));
		}

		IFileRevision nextFile = GitFileRevision.inIndex(repository, gitPath);
		String encoding = CompareUtils.getResourceEncoding(baseFile);
		final EditableRevision next = new EditableRevision(nextFile, encoding);

		IContentChangeListener listener = new IContentChangeListener() {
			public void contentChanged(IContentChangeNotifier source) {
				final byte[] newContent = next.getModifiedContent();
				DirCache cache = null;
				try {
					cache = repository.lockDirCache();
					DirCacheEditor editor = cache.editor();
					editor.add(new PathEdit(gitPath) {
						@Override
						public void apply(DirCacheEntry ent) {
							ent.copyMetaData(entry);

							ObjectInserter inserter = repository
									.newObjectInserter();
							ent.copyMetaData(entry);
							ent.setLength(newContent.length);
							ent.setLastModified(System.currentTimeMillis());
							InputStream in = new ByteArrayInputStream(
									newContent);
							try {
								ent.setObjectId(inserter.insert(
										Constants.OBJ_BLOB, newContent.length,
										in));
								inserter.flush();
							} catch (IOException ex) {
								throw new RuntimeException(ex);
							} finally {
								try {
									in.close();
								} catch (IOException e) {
									// ignore here
								}
							}
						}
					});
					try {
						editor.commit();
					} catch (RuntimeException e) {
						if (e.getCause() instanceof IOException)
							throw (IOException) e.getCause();
						else
							throw e;
					}

				} catch (IOException e) {
					Activator.handleError(
							UIText.CompareWithIndexAction_errorOnAddToIndex, e,
							true);
				} finally {
					if (cache != null)
						cache.unlock();
				}
			}
		};

		next.addContentChangeListener(listener);
		return next;
	}

	@Override
	public boolean isEnabled() {
		return getRepository() != null;
	}
}
