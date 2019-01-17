/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2013, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2016, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2019, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.revision;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ICompareContainer;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.storage.IndexFileRevision;
import org.eclipse.egit.core.internal.storage.OpenWorkspaceVersionEnabled;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.synchronize.compare.LocalNonWorkspaceTypedElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.synchronize.EditableSharedDocumentAdapter.ISharedDocumentAdapterListener;
import org.eclipse.team.internal.ui.synchronize.LocalResourceSaveableComparison;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISaveablesLifecycleListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.SaveablesLifecycleEvent;

/**
 * The input provider for the compare editor when working on resources
 * under Git control.
 */
@SuppressWarnings("restriction")
public class GitCompareFileRevisionEditorInput extends SaveableCompareEditorInput {

	private ITypedElement left;
	private ITypedElement right;
	private ITypedElement ancestor;

	/**
	 * Creates a new CompareFileRevisionEditorInput.
	 * @param left
	 * @param right
	 * @param page
	 */
	public GitCompareFileRevisionEditorInput(ITypedElement left, ITypedElement right, IWorkbenchPage page) {
		super(new CompareConfiguration(), page);
		this.left = left;
		this.right = right;
	}

	/**
	 * Creates a new CompareFileRevisionEditorInput.
	 * @param left
	 * @param right
	 * @param ancestor
	 * @param page
	 */
	public GitCompareFileRevisionEditorInput(ITypedElement left, ITypedElement right, ITypedElement ancestor, IWorkbenchPage page) {
		super(new CompareConfiguration(), page);
		this.left = left;
		this.right = right;
		this.ancestor = ancestor;
	}

	FileRevisionTypedElement getRightRevision() {
		if (right instanceof FileRevisionTypedElement) {
			return (FileRevisionTypedElement) right;
		}
		return null;
	}

	FileRevisionTypedElement getLeftRevision() {
		if (left instanceof FileRevisionTypedElement) {
			return (FileRevisionTypedElement) left;
		}
		return null;
	}

	FileRevisionTypedElement getAncestorRevision() {
		if (ancestor instanceof FileRevisionTypedElement)
			return (FileRevisionTypedElement) ancestor;
		return null;
	}

	private static void ensureContentsCached(FileRevisionTypedElement left, FileRevisionTypedElement right, FileRevisionTypedElement ancestor,
			IProgressMonitor monitor) {
		if (left != null) {
			try {
				left.cacheContents(monitor);
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		if (right != null) {
			try {
				right.cacheContents(monitor);
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		if (ancestor != null) {
			try {
				ancestor.cacheContents(monitor);
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
	}

	private boolean isLeftEditable(ICompareInput input) {
		Object tmpLeft = input.getLeft();
		return isEditable(tmpLeft);
	}

	private boolean isRightEditable(ICompareInput input) {
		Object tmpRight = input.getRight();
		return isEditable(tmpRight);
	}

	private boolean isEditable(Object object) {
		if (object instanceof IEditableContent) {
			return ((IEditableContent) object).isEditable();
		}
		return false;
	}

	private IResource getResource() {
		if (left instanceof IResourceProvider) {
			IResourceProvider resourceProvider = (IResourceProvider) left;
			return resourceProvider.getResource();
		}
		return null;
	}

	private ICompareInput createCompareInput() {
		return compare(left, right, ancestor);
	}

	private DiffNode compare(ITypedElement actLeft, ITypedElement actRight, ITypedElement actAncestor) {
		if (actLeft.getType().equals(ITypedElement.FOLDER_TYPE)) {
			//			return new MyDiffContainer(null, left,right);
			DiffNode diffNode = new DiffNode(null, Differencer.CHANGE,
					actAncestor, actLeft, actRight);
			ITypedElement[] lc = (ITypedElement[])((IStructureComparator)actLeft).getChildren();
			ITypedElement[] rc = (ITypedElement[])((IStructureComparator)actRight).getChildren();
			ITypedElement[] ac = null;
			if (actAncestor != null)
				ac = (ITypedElement[]) ((IStructureComparator) actAncestor)
						.getChildren();
			int li=0;
			int ri=0;
			while (li<lc.length && ri<rc.length) {
				ITypedElement ln = lc[li];
				ITypedElement rn = rc[ri];
				ITypedElement an = null;
				if (ac != null)
					an = ac[ri];
				int compareTo = ln.getName().compareTo(rn.getName());
				// TODO: Git ordering!
				if (compareTo == 0) {
					if (!ln.equals(rn))
						diffNode.add(compare(ln,rn, an));
					++li;
					++ri;
				} else if (compareTo < 0) {
					DiffNode childDiffNode = new DiffNode(Differencer.ADDITION, an, ln, null);
					diffNode.add(childDiffNode);
					if (ln.getType().equals(ITypedElement.FOLDER_TYPE)) {
						ITypedElement[] children = (ITypedElement[])((IStructureComparator)ln).getChildren();
						if(children != null && children.length > 0) {
							for (ITypedElement child : children) {
								childDiffNode.add(addDirectoryFiles(child, Differencer.ADDITION));
							}
						}
					}
					++li;
				} else {
					DiffNode childDiffNode = new DiffNode(Differencer.DELETION, an, null, rn);
					diffNode.add(childDiffNode);
					if (rn.getType().equals(ITypedElement.FOLDER_TYPE)) {
						ITypedElement[] children = (ITypedElement[])((IStructureComparator)rn).getChildren();
						if(children != null && children.length > 0) {
							for (ITypedElement child : children) {
								childDiffNode.add(addDirectoryFiles(child, Differencer.DELETION));
							}
						}
					}
					++ri;
				}
			}
			while (li<lc.length) {
				ITypedElement ln = lc[li];
				ITypedElement an = null;
				if (ac != null)
					an= ac[li];
				DiffNode childDiffNode = new DiffNode(Differencer.ADDITION, an, ln, null);
				diffNode.add(childDiffNode);
				if (ln.getType().equals(ITypedElement.FOLDER_TYPE)) {
					ITypedElement[] children = (ITypedElement[])((IStructureComparator)ln).getChildren();
					if(children != null && children.length > 0) {
						for (ITypedElement child : children) {
							childDiffNode.add(addDirectoryFiles(child, Differencer.ADDITION));
						}
					}
				}
				++li;
			}
			while (ri<rc.length) {
				ITypedElement rn = rc[ri];
				ITypedElement an = null;
				if (ac != null)
					an = ac[ri];
				DiffNode childDiffNode = new DiffNode(Differencer.DELETION, an, null, rn);
				diffNode.add(childDiffNode);
				if (rn.getType().equals(ITypedElement.FOLDER_TYPE)) {
					ITypedElement[] children = (ITypedElement[])((IStructureComparator)rn).getChildren();
					if(children != null && children.length > 0) {
						for (ITypedElement child : children) {
							childDiffNode.add(addDirectoryFiles(child, Differencer.DELETION));
						}
					}
				}
				++ri;
			}
			return diffNode;
		} else {
			if (actAncestor != null)
				return new DiffNode(Differencer.CONFLICTING, actAncestor, actLeft, actRight);
			else
				return new DiffNode(actLeft, actRight);
		}
	}

	private DiffNode addDirectoryFiles(ITypedElement elem, int diffType) {
		ITypedElement l = null;
		ITypedElement r = null;
		if (diffType == Differencer.DELETION) {
			r = elem;
		} else {
			l = elem;
		}

		if (elem.getType().equals(ITypedElement.FOLDER_TYPE)) {
			DiffNode diffNode = null;
			diffNode = new DiffNode(null,Differencer.CHANGE,null,l,r);
			ITypedElement[] children = (ITypedElement[])((IStructureComparator)elem).getChildren();
			for (ITypedElement child : children) {
				diffNode.add(addDirectoryFiles(child, diffType));
			}
			return diffNode;
		} else {
			return new DiffNode(diffType, null, l, r);
		}
	}

	private void initLabels(ICompareInput input) {
		CompareConfiguration cc = getCompareConfiguration();
		if (getLeftRevision() != null) {
			String leftLabel = getFileRevisionLabel(getLeftRevision());
			cc.setLeftLabel(leftLabel);
		} else if (getResource() != null) {
			String label = NLS.bind(UIText.GitCompareFileRevisionEditorInput_LocalLabel, new Object[]{ input.getLeft().getName() });
			cc.setLeftLabel(label);
		} else {
			cc.setLeftLabel(left.getName());
		}
		if (getRightRevision() != null) {
			String rightLabel = getFileRevisionLabel(getRightRevision());
			cc.setRightLabel(rightLabel);
		} else {
			cc.setRightLabel(right.getName());
		}
		if (getAncestorRevision() != null) {
			String ancestorLabel = getFileRevisionLabel(getAncestorRevision());
			cc.setAncestorLabel(ancestorLabel);
		}

	}

	private String getFileRevisionLabel(FileRevisionTypedElement element) {
		Object fileObject = element.getFileRevision();
		if (fileObject instanceof IndexFileRevision) {
			if (isEditable(element))
				return NLS.bind(
						UIText.GitCompareFileRevisionEditorInput_IndexEditableLabel,
						element.getName());
			else
				return NLS.bind(
						UIText.GitCompareFileRevisionEditorInput_IndexLabel,
						element.getName());
		} else {
			return NLS.bind(UIText.GitCompareFileRevisionEditorInput_RevisionLabel, new Object[]{element.getName(),
					CompareUtils.truncatedRevision(element.getContentIdentifier()), element.getAuthor()});
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#getToolTipText()
	 */
	@Override
	public String getToolTipText() {
		Object[] titleObject = new Object[3];
		titleObject[0] = getLongName(left);
		titleObject[1] = CompareUtils.truncatedRevision(getContentIdentifier(getLeftRevision()));
		titleObject[2] = CompareUtils.truncatedRevision(getContentIdentifier(getRightRevision()));
		return NLS.bind(UIText.GitCompareFileRevisionEditorInput_CompareTooltip, titleObject);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#getTitle()
	 */
	@Override
	public String getTitle() {
		Object[] titleObject = new Object[3];
		titleObject[0] = getShortName(left);
		titleObject[1] = CompareUtils.truncatedRevision(getContentIdentifier(getLeftRevision()));
		titleObject[2] = CompareUtils.truncatedRevision(getContentIdentifier(getRightRevision()));
		return NLS.bind(UIText.GitCompareFileRevisionEditorInput_CompareTooltip, titleObject);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IFile.class || adapter == IResource.class) {
			if (left instanceof LocalNonWorkspaceTypedElement) {
				return null;
			}
			return getResource();
		} else if (adapter == Repository.class && left != null) {
			return AdapterUtils.adapt(left, Repository.class);
		}
		return super.getAdapter(adapter);
	}

	private String getShortName(ITypedElement element) {
		if (element instanceof FileRevisionTypedElement){
			FileRevisionTypedElement fileRevisionElement = (FileRevisionTypedElement) element;
			return fileRevisionElement.getName();
		} else if (element instanceof LocalResourceTypedElement){
			LocalResourceTypedElement typedContent = (LocalResourceTypedElement) element;
			return typedContent.getResource().getName();
		}
		return element.getName();
	}

	private String getLongName(ITypedElement element) {
		if (element instanceof FileRevisionTypedElement){
			FileRevisionTypedElement fileRevisionElement = (FileRevisionTypedElement) element;
			return fileRevisionElement.getPath();
		}
		else if (element instanceof LocalResourceTypedElement){
			LocalResourceTypedElement typedContent = (LocalResourceTypedElement) element;
			return typedContent.getResource().getFullPath().toString();
		}
		return element.getName();
	}

	private String getContentIdentifier(ITypedElement element){
		if (element instanceof FileRevisionTypedElement){
			FileRevisionTypedElement fileRevisionElement = (FileRevisionTypedElement) element;
			return fileRevisionElement.getContentIdentifier();
		}
		return UIText.GitCompareFileRevisionEditorInput_CurrentTitle;
	}

	@Override
	protected void fireInputChange() {
		// have the diff node notify its listeners of a change
		((NotifiableDiffNode) getCompareResult()).fireChange();
	}

	@Override
	protected ICompareInput prepareCompareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		ICompareInput input = createCompareInput();
		getCompareConfiguration().setLeftEditable(isLeftEditable(input));
		getCompareConfiguration().setRightEditable(isRightEditable(input));
		ensureContentsCached(getLeftRevision(), getRightRevision(), getAncestorRevision(), monitor);
		initLabels(input);
		setTitle(NLS.bind(UIText.GitCompareFileRevisionEditorInput_CompareInputTitle, new String[] { input.getName() }));

		// The compare editor (Structure Compare) will show the diff filenames
		// with their project relative path. So, no need to also show directory entries.
		DiffNode flatDiffNode = new NotifiableDiffNode(null,
				ancestor != null ? Differencer.CONFLICTING : Differencer.CHANGE, ancestor, left, right);
		flatDiffView(flatDiffNode, (DiffNode) input);

		return flatDiffNode;
	}

	@Override
	protected Saveable createSaveable() {
		// copied from
		// org.eclipse.team.ui.synchronize.SaveableCompareEditorInput.createSaveable()
		Object compareResult = getCompareResult();
		Assert.isNotNull(compareResult,
				"This method cannot be called until after prepareInput is called"); //$NON-NLS-1$
		return new InternalResourceSaveableComparison(
				(ICompareInput) compareResult, this);
	}

	private void flatDiffView(DiffNode rootNode, DiffNode currentNode) {
		if(currentNode != null) {
			IDiffElement[] dElems = currentNode.getChildren();
			if(dElems != null) {
				for(IDiffElement dElem : dElems) {
					DiffNode dNode = (DiffNode) dElem;
					if(dNode.getChildren() != null && dNode.getChildren().length > 0) {
						flatDiffView(rootNode, dNode);
					} else {
						rootNode.add(dNode);
					}
				}
			}
		}
	}

	@Override
	public void registerContextMenu(MenuManager menu,
			final ISelectionProvider selectionProvider) {
		super.registerContextMenu(menu, selectionProvider);
		registerOpenWorkspaceVersion(menu, selectionProvider);
	}

	private void registerOpenWorkspaceVersion(MenuManager menu,
			final ISelectionProvider selectionProvider) {
		FileRevisionTypedElement leftRevision = getLeftRevision();
		if (leftRevision != null) {
			IFileRevision fileRevision = leftRevision.getFileRevision();
			if (fileRevision instanceof OpenWorkspaceVersionEnabled) {
				OpenWorkspaceVersionEnabled workspaceVersion = (OpenWorkspaceVersionEnabled) fileRevision;
				final File workspaceFile = new File(workspaceVersion
						.getRepository().getWorkTree(),
						workspaceVersion.getGitPath());
				if (workspaceFile.exists())
					menu.addMenuListener(new IMenuListener() {
						@Override
						public void menuAboutToShow(IMenuManager manager) {
							Action action = new OpenWorkspaceVersionAction(
									UIText.CommitFileDiffViewer_OpenWorkingTreeVersionInEditorMenuLabel,
									selectionProvider, workspaceFile);
							manager.insertAfter("file", action); //$NON-NLS-1$
						}
					});
			}
		}
	}

	private static class OpenWorkspaceVersionAction extends Action {

		private final ISelectionProvider selectionProvider;

		private final File workspaceFile;

		private OpenWorkspaceVersionAction(String text,
				ISelectionProvider selectionProvider, File workspaceFile) {
			super(text);
			this.selectionProvider = selectionProvider;
			this.workspaceFile = workspaceFile;
		}

		@Override
		public void run() {
			int selectedLine = 0;
			if (selectionProvider.getSelection() instanceof ITextSelection) {
				ITextSelection selection = (ITextSelection) selectionProvider
						.getSelection();
				selectedLine = selection.getStartLine();
			}

			IWorkbenchWindow window = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			IEditorPart editor = EgitUiEditorUtils.openEditor(workspaceFile,
					page);
			EgitUiEditorUtils.revealLine(editor, selectedLine);
		}
	}

	/**
	 * ITypedElement without content. May be used to indicate that a file is not
	 * available.
	 */
	public static class EmptyTypedElement implements ITypedElement{

		private String name;

		/**
		 * @param name the name used for display
		 */
		public EmptyTypedElement(String name) {
			this.name = name;
		}

		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getType() {
			return ITypedElement.UNKNOWN_TYPE;
		}

	}

	// copy of
	// org.eclipse.team.ui.synchronize.SaveableCompareEditorInput.InternalResourceSaveableComparison
	// we need to copy this private class to prevent NPE in
	// org.eclipse.team.internal.ui.synchronize.LocalResourceSaveableComparison.propertyChange(PropertyChangeEvent)
	// when moving and saving partial changes when comparing version from git
	// index with HEAD
	private class InternalResourceSaveableComparison extends
			LocalResourceSaveableComparison implements
			ISharedDocumentAdapterListener {
		private LocalResourceTypedElement lrte;

		private boolean connected = false;

		public InternalResourceSaveableComparison(ICompareInput input,
				CompareEditorInput editorInput) {
			super(input, editorInput, left);
			ITypedElement element = left;
			if (element instanceof LocalResourceTypedElement) {
				lrte = (LocalResourceTypedElement) element;
				if (lrte.isConnected()) {
					registerSaveable(true);
				} else {
					lrte.setSharedDocumentListener(this);
				}
			}
		}

		@Override
		protected void fireInputChange() {
			GitCompareFileRevisionEditorInput.this.fireInputChange();
		}

		@Override
		public void dispose() {
			super.dispose();
			if (lrte != null)
				lrte.setSharedDocumentListener(null);
		}

		@Override
		public void handleDocumentConnected() {
			if (connected)
				return;
			connected = true;
			registerSaveable(false);
			if (lrte != null)
				lrte.setSharedDocumentListener(null);
		}

		private void registerSaveable(boolean init) {
			ICompareContainer container = getContainer();
			IWorkbenchPart part = container.getWorkbenchPart();
			if (part != null) {
				ISaveablesLifecycleListener lifecycleListener = getSaveablesLifecycleListener(part);
				// Remove this saveable from the lifecycle listener
				if (!init)
					lifecycleListener
							.handleLifecycleEvent(new SaveablesLifecycleEvent(
									part, SaveablesLifecycleEvent.POST_CLOSE,
									new Saveable[] { this }, false));
				// Now fix the hashing so it uses the connected document
				initializeHashing();
				// Finally, add this saveable back to the listener
				lifecycleListener
						.handleLifecycleEvent(new SaveablesLifecycleEvent(part,
								SaveablesLifecycleEvent.POST_OPEN,
								new Saveable[] { this }, false));
			}
		}

		private ISaveablesLifecycleListener getSaveablesLifecycleListener(
				IWorkbenchPart part) {
			ISaveablesLifecycleListener listener = AdapterUtils.adapt(part,
					ISaveablesLifecycleListener.class);
			if (listener == null)
				listener = CommonUtils.getService(part.getSite(), ISaveablesLifecycleListener.class);
			return listener;
		}

		@Override
		public void handleDocumentDeleted() {
			// Ignore
		}

		@Override
		public void handleDocumentDisconnected() {
			// Ignore
		}

		@Override
		public void handleDocumentFlushed() {
			// Ignore
		}

		@Override
		public void handleDocumentSaved() {
			// Ignore
		}

		@Override
		public void doSave(IProgressMonitor monitor) throws CoreException {
			// SaveableComparison unconditionally resets the dirty flag to
			// false, but LocalResourceSaveableComparison's performSave may not
			// actually save: if the file has been changed outside the compare
			// editor, it displays a dialog that the user may cancel.
			if (isDirty()) {
				performSave(monitor);
				// LocalResourecSaveableComparison does already reset the dirty
				// flag if it did save.
			}
		}
	}

}
