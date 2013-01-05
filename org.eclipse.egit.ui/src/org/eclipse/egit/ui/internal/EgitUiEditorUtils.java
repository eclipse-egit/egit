/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;

/**
 * Taken from the Team UI plug in Utils class
 */
public class EgitUiEditorUtils {

	/**
	 * @param page
	 * @param revision
	 * @param monitor
	 * @return the part
	 * @throws CoreException
	 */
	public static IEditorPart openEditor(IWorkbenchPage page,
			IFileRevision revision, IProgressMonitor monitor)
			throws CoreException {
		IStorage file = revision.getStorage(monitor);
		if (file instanceof IFile) {
			// if this is the current workspace file, open it
			return IDE.openEditor(page, (IFile) file, OpenStrategy
					.activateOnOpen());
		} else {
			FileRevisionEditorInput fileRevEditorInput = FileRevisionEditorInput
					.createEditorInputFor(revision, monitor);
			IEditorPart part = openEditor(page, fileRevEditorInput);
			return part;
		}
	}

	/**
	 * Opens a text editor on a revision
	 *
	 * @param page
	 *            the page
	 * @param revision
	 *            the revision
	 * @param monitor
	 *            a progress monitor, may be null
	 * @throws CoreException
	 *             upon failure
	 */
	public static void openTextEditor(IWorkbenchPage page,
			IFileRevision revision, IProgressMonitor monitor)
			throws CoreException {
		FileRevisionEditorInput fileRevEditorInput = FileRevisionEditorInput
				.createEditorInputFor(revision, monitor);
		openEditor(page, fileRevEditorInput, "org.eclipse.ui.DefaultTextEditor"); //$NON-NLS-1$
	}

	/**
	 * @param page
	 * @param editorInput
	 * @return the part
	 * @throws PartInitException
	 */
	private static IEditorPart openEditor(IWorkbenchPage page,
			FileRevisionEditorInput editorInput) throws PartInitException {
		String id = getEditorId(editorInput);
		return openEditor(page, editorInput, id);
	}

	/**
	 * @param page
	 * @param editorInput
	 * @param editorId
	 * @return the part
	 * @throws PartInitException
	 */
	private static IEditorPart openEditor(IWorkbenchPage page,
			FileRevisionEditorInput editorInput, String editorId)
			throws PartInitException {
		try {
			IEditorPart part = page.openEditor(editorInput, editorId,
					OpenStrategy.activateOnOpen());
			if (part == null) {
				throw new PartInitException(NLS.bind(
						UIText.EgitUiUtils_CouldNotOpenEditorMessage, editorId));
			}
			return part;
		} catch (PartInitException e) {
			if (editorId.equals("org.eclipse.ui.DefaultTextEditor")) { //$NON-NLS-1$
				throw e;
			} else {
				return page.openEditor(editorInput,
						"org.eclipse.ui.DefaultTextEditor"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Looks up a resource for the given file and opens an editor on it. A text
	 * editor is opened if the file is not contained in the workspace.
	 *
	 * @param file
	 *            File to open an editor for. {@code file} must exist.
	 * @param page
	 */
	public static void openEditor(File file, IWorkbenchPage page) {
		if (!file.exists())
			return;
		IFile fileResource = ResourceUtil.getFileForLocation(new Path(file.getAbsolutePath()));
		if (fileResource != null) {
			try {
				IDE.openEditor(page, fileResource, OpenStrategy.activateOnOpen());
			} catch (PartInitException e) {
				Activator.handleError(UIText.EgitUiEditorUtils_openFailed, e,
						true);
			}
		} else {
			IFileStore store = EFS.getLocalFileSystem().getStore(
					new Path(file.getAbsolutePath()));
			try {
				IDE.openEditor(page, new FileStoreEditorInput(store),
						EditorsUI.DEFAULT_TEXT_EDITOR_ID);
			} catch (PartInitException e) {
				Activator.handleError(UIText.EgitUiEditorUtils_openFailed, e,
						true);
			}
		}
	}

	private static String getEditorId(FileRevisionEditorInput editorInput) {
		String id = getEditorId(editorInput.getFileRevision().getName(),
				getContentType(editorInput));
		return id;
	}

	private static String getEditorId(String fileName, IContentType type) {
		IEditorRegistry registry = PlatformUI.getWorkbench()
				.getEditorRegistry();
		IEditorDescriptor descriptor = registry
				.getDefaultEditor(fileName, type);
		String id;
		if (descriptor == null || descriptor.isOpenExternal()) {
			id = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
		} else {
			id = descriptor.getId();
		}
		return id;
	}

	private static IContentType getContentType(
			FileRevisionEditorInput editorInput) {
		IContentType type = null;
		try {
			InputStream contents = editorInput.getStorage().getContents();
			try {
				type = getContentType(editorInput.getFileRevision().getName(),
						contents);
			} finally {
				try {
					contents.close();
				} catch (IOException e) {
					// ignore
				}
			}
		} catch (CoreException e) {
			Activator.handleError(e.getMessage(), e, false);
		}
		return type;
	}

	private static IContentType getContentType(String fileName,
			InputStream contents) {
		IContentType type = null;
		if (contents != null) {
			try {
				type = Platform.getContentTypeManager().findContentTypeFor(
						contents, fileName);
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, false);
			}
		}
		if (type == null) {
			type = Platform.getContentTypeManager()
					.findContentTypeFor(fileName);
		}
		return type;
	}
}
