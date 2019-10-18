/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.revision.FileRevisionEditorInput;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jgit.annotations.Nullable;
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
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Taken from the Team UI plug in Utils class
 */
public class EgitUiEditorUtils {

	/**
	 * Opens an {@link IFileRevision} is the configured editor.
	 *
	 * @param page
	 *            to open the editor in
	 * @param revision
	 *            to open
	 * @param monitor
	 *            for progress reporting
	 * @return the part; may be {@code null} if an external editor was opened
	 * @throws CoreException
	 */
	public static IEditorPart openEditor(IWorkbenchPage page,
			IFileRevision revision, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		IStorage file = revision.getStorage(progress.newChild(1));
		if (file instanceof IFile) {
			// if this is the current workspace file, open it
			return IDE.openEditor(page, (IFile) file, OpenStrategy
					.activateOnOpen());
		} else {
			FileRevisionEditorInput fileRevEditorInput = FileRevisionEditorInput
					.createEditorInputFor(revision, progress.newChild(1));
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
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		FileRevisionEditorInput fileRevEditorInput = FileRevisionEditorInput
				.createEditorInputFor(revision, progress.newChild(1));
		page.openEditor(fileRevEditorInput, EditorsUI.DEFAULT_TEXT_EDITOR_ID);
	}

	/**
	 * Opens an editor for a {@link FileRevisionEditorInput}.
	 *
	 * @param page
	 *            to open the editor in
	 * @param editorInput
	 *            to open
	 * @return the part; may be {@code null} if an external editor was opened
	 * @throws PartInitException
	 */
	public static IEditorPart openEditor(IWorkbenchPage page,
			FileRevisionEditorInput editorInput) throws PartInitException {
		return openEditor(page, editorInput, getEditor(editorInput));
	}

	/**
	 * @param page
	 * @param editorInput
	 * @param editor
	 * @return the part
	 * @throws PartInitException
	 */
	private static IEditorPart openEditor(IWorkbenchPage page,
			FileRevisionEditorInput editorInput, IEditorDescriptor editor)
			throws PartInitException {
		String editorId = editor.getId();
		try {
			IEditorPart part = page.openEditor(editorInput, editorId,
					OpenStrategy.activateOnOpen());
			if (part == null && !editor.isOpenExternal()) {
				throw new PartInitException(NLS.bind(
						UIText.EgitUiUtils_CouldNotOpenEditorMessage, editorId));
			}
			return part;
		} catch (PartInitException e) {
			if (editorId.equals(EditorsUI.DEFAULT_TEXT_EDITOR_ID)) {
				throw e;
			} else {
				return page.openEditor(editorInput,
						EditorsUI.DEFAULT_TEXT_EDITOR_ID);
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
	 * @return the created editor or null in case of an error
	 */
	@Nullable
	public static IEditorPart openEditor(File file, IWorkbenchPage page) {
		if (!file.exists()) {
			return null;
		}
		IPath path = new Path(file.getAbsolutePath());
		IFile ifile = ResourceUtil.getFileForLocation(path, true);
		try {
			if (ifile != null) {
				return IDE.openEditor(page, ifile,
						OpenStrategy.activateOnOpen());
			} else {
				IFileStore store = EFS.getLocalFileSystem().getStore(path);
				return IDE.openEditorOnFileStore(page, store);
			}
		} catch (PartInitException e) {
			Activator.handleError(UIText.EgitUiEditorUtils_openFailed, e, true);
		}
		return null;
	}

	/**
	 * Looks for and returns an open editor on the given page that has the given file as input.
	 *
	 * @param file to find an editor for
	 * @param page on which to look for open editors
	 * @return the open editor, or {@code null} if none could be found
	 */
	public static IEditorPart findEditor(File file, IWorkbenchPage page) {
		if (!file.exists() || page == null) {
			return null;
		}
		IPath path = new Path(file.getAbsolutePath());
		IFile iFile = ResourceUtil.getFileForLocation(path, true);
		if (iFile != null) {
			return page.findEditor(new FileEditorInput(iFile));
		} else {
			IFileStore store = EFS.getLocalFileSystem().getStore(path);
			return page.findEditor(new FileStoreEditorInput(store));
		}
	}

	private static IEditorDescriptor getEditor(
			FileRevisionEditorInput editorInput) {
		IEditorRegistry registry = PlatformUI.getWorkbench()
				.getEditorRegistry();
		String fileName = editorInput.getFileRevision().getName();
		IContentType type = getContentType(editorInput);
		IEditorDescriptor descriptor = registry
				.getDefaultEditor(fileName, type);
		if (descriptor != null) {
			descriptor = IDE.overrideDefaultEditorAssociation(editorInput, type,
					descriptor);
		}
		if (descriptor == null) {
			descriptor = registry.findEditor(EditorsUI.DEFAULT_TEXT_EDITOR_ID);
		}
		return descriptor;
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

	/**
	 * Reveals the given {@code lineNo} if it is greater than zero and the
	 * editor is an {@link ITextEditor}.
	 *
	 * @param editor
	 *            to reveal the line in
	 * @param lineNo
	 *            to reveal
	 */
	public static void revealLine(IEditorPart editor, int lineNo) {
		if (lineNo < 0) {
			return;
		}
		ITextEditor textEditor = getTextEditor(editor);
		if (textEditor == null) {
			return;
		}
		IDocument document = textEditor.getDocumentProvider()
				.getDocument(textEditor.getEditorInput());
		if (document == null) {
			return;
		}
		try {
			textEditor.selectAndReveal(document.getLineOffset(lineNo), 0);
		} catch (BadLocationException e) {
			// Ignore
		}
	}

	private static ITextEditor getTextEditor(IEditorPart editor) {
		if (editor instanceof ITextEditor) {
			return (ITextEditor) editor;
		} else if (editor instanceof MultiPageEditorPart) {
			Object nestedEditor = ((MultiPageEditorPart) editor)
					.getSelectedPage();
			if (nestedEditor instanceof ITextEditor) {
				return (ITextEditor) nestedEditor;
			}
		}
		return null;
	}
}
