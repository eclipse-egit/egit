///*******************************************************************************
// * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
// *
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the Eclipse Public License v1.0
// * which accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// *******************************************************************************/
//package org.eclipse.egit.ui.internal;
//
//import static org.eclipse.compare.CompareEditorInput.DIRTY_STATE;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//
//import org.eclipse.compare.ICompareContainer;
//import org.eclipse.compare.ITypedElement;
//import org.eclipse.compare.internal.ISavingSaveable;
//import org.eclipse.compare.structuremergeviewer.ICompareInput;
//import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.core.runtime.NullProgressMonitor;
//import org.eclipse.egit.ui.Activator;
//import org.eclipse.egit.ui.UIText;
//import org.eclipse.jface.resource.ImageDescriptor;
//import org.eclipse.jface.util.IPropertyChangeListener;
//import org.eclipse.jface.util.PropertyChangeEvent;
//import org.eclipse.jgit.dircache.DirCache;
//import org.eclipse.jgit.dircache.DirCacheEditor;
//import org.eclipse.jgit.dircache.DirCacheEntry;
//import org.eclipse.jgit.lib.Constants;
//import org.eclipse.jgit.lib.ObjectInserter;
//import org.eclipse.jgit.lib.Repository;
//import org.eclipse.team.ui.mapping.SaveableComparison;
//import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
//import org.eclipse.ui.ISaveablesLifecycleListener;
//import org.eclipse.ui.IWorkbenchPart;
//import org.eclipse.ui.Saveable;
//import org.eclipse.ui.SaveablesLifecycleEvent;
//
///**
// *
// */
//public class GitStagedSaveableComparison extends SaveableComparison implements
//		IPropertyChangeListener, ICompareInputChangeListener, ISavingSaveable {
//
//	private final EditableRevision left;
//
//	private Repository repo;
//
//
//	private final GitCompareFileRevisionEditorInput input;
//
//	private boolean isSaving = false;
//
//	/**
//	 * @param left
//	 * @param gitCompareFileRevisionEditorInput
//	 */
//	public GitStagedSaveableComparison(EditableRevision left, GitCompareFileRevisionEditorInput gitCompareFileRevisionEditorInput) {
//		this.left = left;
//		this.input = gitCompareFileRevisionEditorInput;
//		input.addPropertyChangeListener(this); // TODO remove this listener somewhere
//
//		ITypedElement element = SaveableCompareEditorInput.getFileElement(input, editorInput);
//		if (element instanceof LocalResourceTypedElement) {
//			lrte = (LocalResourceTypedElement) element;
//			if (lrte.isConnected()) {
//				registerSaveable(true);
//			} else {
//				lrte.setSharedDocumentListener(this);
//			}
//		}
//	}
//
//	protected void fireInputChange() {
//		SaveableCompareEditorInput.this.fireInputChange();
//	}
//	public void dispose() {
//		super.dispose();
//		if (lrte != null)
//			lrte.setSharedDocumentListener(null);
//	}
//	public void handleDocumentConnected() {
//		if (connected)
//			return;
//		connected = true;
//		registerSaveable(false);
//		if (lrte != null)
//			lrte.setSharedDocumentListener(null);
//	}
//
//	private void registerSaveable(boolean init) {
//		ICompareContainer container = getContainer();
//		IWorkbenchPart part = container.getWorkbenchPart();
//		if (part != null) {
//			ISaveablesLifecycleListener lifecycleListener= getSaveablesLifecycleListener(part);
//			// Remove this saveable from the lifecycle listener
//			if (!init)
//				lifecycleListener.handleLifecycleEvent(
//						new SaveablesLifecycleEvent(part, SaveablesLifecycleEvent.POST_CLOSE, new Saveable[] { this }, false));
//			// Now fix the hashing so it uses the connected document
//			initializeHashing();
//			// Finally, add this saveable back to the listener
//			lifecycleListener.handleLifecycleEvent(
//					new SaveablesLifecycleEvent(part, SaveablesLifecycleEvent.POST_OPEN, new Saveable[] { this }, false));
//		}
//	}
//
//	@Override
//	protected void performSave(IProgressMonitor monitor) throws CoreException {
//		DirCache cache = null;
//		try {
//			isSaving = true;
//			cache = left.getRepository().lockDirCache();
//			DirCacheEditor editor = cache.editor();
//			final DirCacheEntry entry = cache.getEntry(left.getPath());
//			editor.add(new DirCacheEditor.PathEdit(left.getPath()) {
//				@Override
//				public void apply(DirCacheEntry ent) {
//					byte[] newContent = left.getModifiedContent();
//					ent.copyMetaData(entry);
//
//					ObjectInserter inserter = repo
//							.newObjectInserter();
//					ent.copyMetaData(entry);
//					ent.setLength(newContent.length);
//					ent.setLastModified(System.currentTimeMillis());
//					InputStream in = new ByteArrayInputStream(
//							newContent);
//					try {
//						ent.setObjectId(inserter.insert(
//								Constants.OBJ_BLOB, newContent.length,
//								in));
//						inserter.flush();
//					} catch (IOException ex) {
//						throw new RuntimeException(ex);
//					} finally {
//						try {
//							in.close();
//						} catch (IOException e) {
//							// ignore here
//						}
//						isSaving = false;
//						input.fireInputChange();
//					}
//				}
//			});
//			try {
//				editor.commit();
//			} catch (RuntimeException e) {
//				if (e.getCause() instanceof IOException)
//					throw (IOException) e.getCause();
//				else
//					throw e;
//			}
//
//		} catch (IOException e) {
//			Activator.handleError(
//					UIText.CompareWithIndexAction_errorOnAddToIndex, e,
//					true);
//		} finally {
//			if (cache != null)
//				cache.unlock();
//		}
//	}
//
//	@Override
//	protected void performRevert(IProgressMonitor monitor) {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public String getName() {
//		return left.getName();
//	}
//
//	@Override
//	public String getToolTipText() {
//		return null;
//	}
//
//	@Override
//	public ImageDescriptor getImageDescriptor() {
//		return null;
//	}
//
//	public boolean isSaving() {
//		return isSaving;
//	}
//
//	public void propertyChange(PropertyChangeEvent e) {
//		String propertyName= e.getProperty();
//		if (DIRTY_STATE.equals(propertyName)) {
//			boolean changed= false;
//			Object newValue= e.getNewValue();
//			if (newValue instanceof Boolean)
//				changed= ((Boolean)newValue).booleanValue();
//			setDirty(changed);
//		}
//	}
//
//	public void compareInputChanged(ICompareInput source) {
//		if (!isSaving())
//			try {
//				performSave(new NullProgressMonitor());
//			} catch (CoreException e) {
//				Activator.logError(e.getMessage(), e);
//			}
//	}
//
//	@Override
//	public boolean equals(Object object) {
//		if (object == this)
//			return true;
//
//		if (object instanceof GitStagedSaveableComparison) {
//			GitStagedSaveableComparison other = (GitStagedSaveableComparison) object;
//
//			return left.getRepository().getWorkTree()
//					.equals(other.left.getRepository().getWorkTree())
//					&& left.getPath().equals(other.left.getPath());
//		}
//
//		return false;
//	}
//
//	@Override
//	public int hashCode() {
//		return left.getRepository().getWorkTree().hashCode() ^ left.getPath().hashCode();
//	}
//
//}
