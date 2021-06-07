/*******************************************************************************
 * Copyright (C) 2010, 2021 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.ContentMergeViewer;
import org.eclipse.compare.structuremergeviewer.DiffContainer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.efs.HiddenResources;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ui.synchronize.EditableSharedDocumentAdapter.ISharedDocumentAdapterListener;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.services.IServiceLocator;

/**
 * A Git-specific {@link CompareEditorInput}.
 */
@SuppressWarnings("restriction")
public abstract class AbstractGitCompareEditorInput extends CompareEditorInput {

	private static final Comparator<String> CMP = (left, right) -> {
		String l = left.startsWith("/") ? left.substring(1) : left; //$NON-NLS-1$
		String r = right.startsWith("/") ? right.substring(1) : right; //$NON-NLS-1$
		return l.replace('/', '\001')
				.compareToIgnoreCase(r.replace('/', '\001'));
	};

	private static final Image FOLDER_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

	private static final Image PROJECT_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(SharedImages.IMG_OBJ_PROJECT);

	private final IPath[] locations;

	private List<IFile> toDelete;

	private Map<String, IHandlerActivation> activations = new HashMap<>();

	private Repository repository;

	private Collection<String> gitPaths;

	private boolean initialized;

	/**
	 * Creates a new {@link AbstractGitCompareEditorInput}. Note that if the
	 * repository is null and no locations are given, initPaths will throw an
	 * exception.
	 *
	 * @param repository
	 *            to operate in; if {@code null} will be determined from the
	 *            {@code locations}
	 * @param locations
	 *            absolute file system locations of the files/folders to
	 *            restrict the operation to
	 */
	protected AbstractGitCompareEditorInput(Repository repository,
			IPath... locations) {
		super(new CompareConfiguration());
		this.repository = repository;
		this.locations = locations;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if ((adapter == IFile.class || adapter == IResource.class)
				&& isUIThread()) {
			Object selectedEdition = getSelectedEdition();
			if (selectedEdition instanceof DiffNode) {
				DiffNode diffNode = (DiffNode) selectedEdition;
				ITypedElement element = diffNode.getLeft();
				IResource resource = null;
				if (element instanceof HiddenResourceTypedElement) {
					resource = ((HiddenResourceTypedElement) element)
							.getRealFile();
				}
				if (resource == null && element instanceof IResourceProvider) {
					resource = ((IResourceProvider) element).getResource();
				}
				if (resource != null && adapter.isInstance(resource)) {
					return resource;
				}
			}
		}
		return super.getAdapter(adapter);
	}

	@Override
	protected void contentsCreated() {
		super.contentsCreated();
		// select the first conflict
		getNavigator().selectChange(true);
	}

	@Override
	public Viewer createDiffViewer(Composite parent) {
		Viewer viewer = super.createDiffViewer(parent);
		if (viewer instanceof StructuredViewer) {
			((StructuredViewer) viewer)
					.setComparator(new ViewerComparator(CMP) {

						@Override
						public int category(Object element) {
							if (element instanceof FolderNode) {
								return 0;
							} else {
								return 1;
							}
						}
					});
		}
		return viewer;
	}

	@Override
	public Viewer findContentViewer(Viewer oldViewer, ICompareInput input,
			Composite parent) {
		Viewer newViewer = super.findContentViewer(oldViewer, input, parent);
		ToolBarManager manager = CompareViewerPane.getToolBarManager(parent);
		if (manager != null) {
			initActions(manager, newViewer, input);
		}
		return newViewer;
	}

	/**
	 * Something that can get or create an action to be added to the toolbar of
	 * a content merge viewer.
	 */
	@FunctionalInterface
	protected interface ActionSupplier {

		/**
		 * Obtains an action.
		 *
		 * @param create
		 *            whether to create an action if none was created yet
		 * @return the action, or {@code null} if none created
		 */
		public CompareEditorInputViewerAction get(boolean create);
	}

	/**
	 * Hook for subclasses to provide toolbar actions.
	 *
	 * @param manager
	 *            to add the action to
	 * @param newViewer
	 *            for the action
	 * @param input
	 *            for the viewer
	 */
	protected void initActions(ToolBarManager manager, Viewer newViewer,
			ICompareInput input) {
		// Nothing.
	}

	/**
	 * Manages the action with the given id. If none exists in the toolbar but
	 * it is applicable, create one through the supplier. Otherwise set the
	 * enablement according to whether it is applicable. The action is
	 * registered with the {@link IHandlerService} as a command.
	 *
	 * @param manager
	 *            to add the action to
	 * @param viewer
	 *            for the action
	 * @param isApplicable
	 *            {@code true} if the action applies
	 * @param id
	 *            of the action; also used as a command id
	 * @param supplier
	 *            to create or get the action
	 */
	protected void setAction(ToolBarManager manager, Viewer viewer,
			boolean isApplicable, String id, ActionSupplier supplier) {
		IContributionItem item = manager.find(id);
		if (item != null) {
			if (item instanceof ActionContributionItem) {
				IAction action = ((ActionContributionItem) item).getAction();
				if (action instanceof CompareEditorInputViewerAction) {
					((CompareEditorInputViewerAction) action).setViewer(
							isApplicable ? (ContentMergeViewer) viewer : null);
					action.setEnabled(isApplicable);
					if (item.isVisible() != isApplicable) {
						item.setVisible(isApplicable);
						manager.update(true);
					}
				}
			}
		} else if (isApplicable) {
			CompareEditorInputViewerAction action = supplier.get(true);
			action.setViewer((ContentMergeViewer) viewer);
			action.setEnabled(true);
			manager.insert(0, new ActionContributionItem(action));
			manager.update(true);
			registerAction(action, id);
		} else {
			// Neither present nor applicable: disable it if it exists
			CompareEditorInputViewerAction action = supplier.get(false);
			if (action != null) {
				action.setEnabled(false);
			}
		}
	}

	private void registerAction(IAction action, String commandId) {
		if (activations.containsKey(commandId)) {
			return;
		}
		action.setActionDefinitionId(commandId);
		IServiceLocator locator = getContainer().getServiceLocator();
		if (locator != null) {
			IHandlerService handlers = locator
					.getService(IHandlerService.class);
			if (handlers != null) {
				activations.put(commandId, handlers.activateHandler(commandId,
						new ActionHandler(action)));
			}
		}
	}

	/**
	 * Hook for subclasses to dispose actions, if needed. Invoked during
	 * {@link #handleDispose()}.
	 */
	protected void disposeActions() {
		// Nothing
	}

	@Override
	protected void handleDispose() {
		super.handleDispose();
		// We do NOT dispose the images, as these are shared.
		activations.values()
				.forEach(a -> a.getHandlerService().deactivateHandler(a));
		activations.clear();
		disposeActions();
		// We need to remove the temporary resources. A CompareEditorInput is
		// supposed to be the very last thing that is disposed in a compare
		// viewer, but this is not always true. If content merge viewers add
		// additional widgets, for instance for the Java structure comparison,
		// we're suddenly no longer the last item to be disposed. The various
		// viewers (left, right, structure, and so on) are all disposed when
		// their widgets are disposed. Widget disposal happens recursively
		// top-down on the UI thread, so an asyncExec should be safe here to
		// ensure that we remove the files only once everything else has been
		// disposed of. If we delete temporary resources before all viewers had
		// disconnected the Document, some might not disconnect because
		// SharedDocumentAdapter.getDocumentKey() returns null if the file has
		// been deleted. If this happens the framework will find that still
		// connected document the next time this resource is opened and show
		// that instead of the true resource contents. This is wrong and is very
		// annoying if this cached document is dirty: one can open only this
		// dirty version from then on, until the next restart of Eclipse.
		PlatformUI.getWorkbench().getDisplay().asyncExec(this::cleanUp);
	}

	private void cleanUp() {
		if (toDelete == null || toDelete.isEmpty()) {
			return;
		}
		List<IFile> toClean = toDelete;
		toDelete = null;
		// Don't clean up if the workbench is shutting down; we would exit with
		// unsaved workspace changes. Instead, EGit core cleans the project on
		// start.
		Job job = new Job(UIText.GitMergeEditorInput_ResourceCleanupJobName) {

			@Override
			public boolean shouldSchedule() {
				return super.shouldSchedule()
						&& !PlatformUI.getWorkbench().isClosing();
			}

			@Override
			public boolean shouldRun() {
				return super.shouldRun()
						&& !PlatformUI.getWorkbench().isClosing();
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IWorkspaceRunnable remove = m -> {
					SubMonitor progress = SubMonitor.convert(m, toClean.size());
					for (IFile tmp : toClean) {
						if (PlatformUI.getWorkbench().isClosing()) {
							return;
						}
						try {
							tmp.delete(true, progress.newChild(1));
						} catch (CoreException e) {
							// Ignore
						}
					}
				};
				try {
					ResourcesPlugin.getWorkspace().run(remove, null,
							IWorkspace.AVOID_UPDATE, monitor);
				} catch (CoreException e) {
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setUser(false);
		job.schedule();
	}

	private static boolean isUIThread() {
		return Display.getCurrent() != null;
	}


	/**
	 * Creates a {@link HiddenResourceTypedElement}.
	 *
	 * @param uri
	 *            to link to
	 * @param name
	 *            for the hidden resource
	 * @param file
	 *            original file, if any
	 * @param encoding
	 *            to use
	 * @return a {@link HiddenResourceTypedElement}
	 * @throws IOException
	 *             on errors
	 */
	protected LocalResourceTypedElement createWithHiddenResource(URI uri,
			String name, IFile file, Charset encoding) throws IOException {
		IFile tmp = createHiddenResource(uri, name, encoding);
		return new HiddenResourceTypedElement(tmp, file);
	}

	/**
	 * Creates a hidden resource that will be removed when this
	 * {@link AbstractGitCompareEditorInput} is disposed.
	 *
	 * @param uri
	 *            to link to
	 * @param name
	 *            for the resource
	 * @param encoding
	 *            to use
	 * @return the hidden resource
	 * @throws IOException
	 *             on errors
	 */
	protected IFile createHiddenResource(URI uri, String name, Charset encoding)
			throws IOException {
		try {
			IFile tmp = HiddenResources.INSTANCE.createFile(uri, name, encoding,
					null);
			if (toDelete == null) {
				toDelete = new ArrayList<>();
			}
			toDelete.add(tmp);
			return tmp;
		} catch (CoreException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/**
	 * Constructs diff nodes for folders connecting the file to the root.
	 *
	 * @param root
	 *            to connect to
	 * @param repositoryPath
	 *            full absolute path of the git repository working tree
	 * @param file
	 *            to determine a {@link IProject} from
	 * @param location
	 *            full absolute path of the file
	 * @return the folder node to attach a new {@link DiffNode} for the file to,
	 *         already attached to root
	 */
	protected IDiffContainer getFileParent(IDiffContainer root,
			IPath repositoryPath, IFile file, IPath location) {
		int projectSegment = -1;
		String projectName = null;
		if (file != null) {
			IProject project = file.getProject();
			IPath projectLocation = project.getLocation();
			if (projectLocation != null) {
				IPath projectPath = project.getLocation().makeRelativeTo(
						repositoryPath);
				projectSegment = projectPath.segmentCount() - 1;
				projectName = project.getName();
			}
		}

		IPath path = location.makeRelativeTo(repositoryPath);
		IDiffContainer child = root;
		for (int i = 0; i < path.segmentCount() - 1; i++) {
			if (i == projectSegment) {
				child = getOrCreateChild(child, projectName, true);
			} else {
				child = getOrCreateChild(child, path.segment(i), false);
			}
		}
		return child;
	}

	/**
	 * Constructs diff nodes for folders connecting the file to the root.
	 *
	 * @param root
	 *            to connect to
	 * @param gitPath
	 *            git path (relative to the repository root)
	 * @return the folder node to attach a new {@link DiffNode} for the file to,
	 *         already attached to root
	 */
	protected IDiffContainer getFileParent(IDiffContainer root,
			String gitPath) {
		IDiffContainer child = root;
		IPath path = Path.fromPortableString(gitPath);
		for (int i = 0; i < path.segmentCount() - 1; i++) {
			child = getOrCreateChild(child, path.segment(i), false);
		}
		return child;
	}

	private DiffNode getOrCreateChild(IDiffContainer parent, final String name,
			final boolean projectMode) {
		for (IDiffElement child : parent.getChildren()) {
			if (child.getName().equals(name)) {
				return ((DiffNode) child);
			}
		}
		return new FolderNode(parent, name,
				projectMode ? PROJECT_IMAGE : FOLDER_IMAGE);
	}

	private void collapse(DiffContainer top) {
		IDiffElement[] children = top.getChildren();
		boolean isRoot = top.getParent() == null;
		if (!isRoot) {
			while (children != null && children.length == 1) {
				IDiffElement singleChild = children[0];
				if (singleChild instanceof FolderNode) {
					FolderNode node = (FolderNode) singleChild;
					top.remove(singleChild);
					top.getParent().add(singleChild);
					node.setName(top.getName() + '/' + singleChild.getName());
					((DiffContainer) top.getParent()).remove(top);
					children = node.getChildren();
					top = node;
				} else {
					// Hit a leaf.
					return;
				}
			}
		}
		if (children != null && (isRoot || children.length > 1)) {
			for (IDiffElement node : children) {
				if (node instanceof FolderNode) {
					collapse((DiffContainer) node);
				}
			}
		}
	}

	@Override
	public boolean canRunAsJob() {
		return true;
	}

	@Override
	protected final Object prepareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		monitor.beginTask(UIText.GitMergeEditorInput_CheckingResourcesTaskName,
				IProgressMonitor.UNKNOWN);
		try {
			initPaths();
			if (monitor.isCanceled()) {
				throw new InterruptedException();
			}
			DiffContainer result = buildInput(monitor);
			if (result != null) {
				collapse(result);
			}
			inputBuilt(result);
			return result;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Build the {@link DiffNode}s and return the root node.
	 *
	 * @param monitor
	 *            for cancellation and progress reporting
	 * @return the root diff node
	 * @throws InvocationTargetException
	 *             on errors
	 * @throws InterruptedException
	 *             on cancellation
	 * @see CompareEditorInput#prepareInput(IProgressMonitor monitor)
	 */
	protected abstract DiffContainer buildInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException;

	/**
	 * Hook for subclasses called once the full compare result has been built.
	 * <p>
	 * This default implementation does nothing.
	 * </p>
	 *
	 * @param root
	 *            that will be returned as compare result
	 */
	protected void inputBuilt(DiffContainer root) {
		// Nothing
	}

	private void initPaths() throws InvocationTargetException {
		if (initialized) {
			return;
		}
		initialized = true;
		if (repository == null || locations != null && locations.length > 0) {
			Map<Repository, Collection<String>> pathsByRepository = ResourceUtil
					.splitPathsByRepository(Arrays.asList(locations));
			if (pathsByRepository.size() != 1) {
				throw new InvocationTargetException(new IllegalStateException(
						UIText.RepositoryAction_multiRepoSelection));
			}
			Entry<Repository, Collection<String>> entry = pathsByRepository
					.entrySet().iterator().next();
			Repository repo = entry.getKey();
			if (repository != null
					&& !repo.getDirectory().equals(repository.getDirectory())) {
				throw new InvocationTargetException(
						new IllegalStateException("Paths not in repo " //$NON-NLS-1$
								+ repository.getDirectory()));
			}
			if (repository == null) {
				repository = repo;
			}
			gitPaths = new ArrayList<>(entry.getValue());
		}
	}

	/**
	 * Retrieves the repository.
	 *
	 * @return the {@link Repository}
	 */
	protected Repository getRepository() {
		return repository;
	}

	/**
	 * Retrieves the git paths to filter the comparison by.
	 *
	 * @return the paths, or an empty collection if all paths in the repository
	 *         shall be compared
	 */
	protected Collection<String> getFilterPaths() {
		if (gitPaths == null) {
			return Collections.emptyList();
		}
		return gitPaths;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(locations);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		AbstractGitCompareEditorInput other = (AbstractGitCompareEditorInput) obj;
		return Arrays.equals(locations, other.locations);
	}

	/**
	 * {@link AbstractGitCompareEditorInput} is not a
	 * {@code SaveableCompareEditorInput}. Editable {@link ITypedElement}s must
	 * handle saving on being flushed. Attaching a {@code LocalResourceSaver} to
	 * a {@link LocalResourceTypedElement} achieves that, and also refreshes as
	 * needed when hidden resources are used.
	 */
	protected static class LocalResourceSaver
			implements ISharedDocumentAdapterListener {

		LocalResourceTypedElement element;

		/**
		 * Creates a new {@link LocalResourceSaver} for the given
		 * {@link LocalResourceTypedElement}.
		 *
		 * @param element
		 *            to handle saving of
		 */
		public LocalResourceSaver(LocalResourceTypedElement element) {
			this.element = element;
		}

		/**
		 * Saves the element; invoked via {@link #handleDocumentFlushed()}.
		 *
		 * @throws CoreException
		 *             on errors
		 */
		protected void save() throws CoreException {
			element.saveDocument(true, null);
			refreshIndexDiff();
		}

		private void refreshIndexDiff() {
			IResource resource = element.getResource();
			if (resource != null && HiddenResources.INSTANCE
					.isHiddenProject(resource.getProject())) {
				String gitPath = null;
				Repository repository = null;
				URI uri = resource.getLocationURI();
				if (EFS.SCHEME_FILE.equals(uri.getScheme())) {
					IPath location = new Path(uri.getSchemeSpecificPart());
					repository = ResourceUtil.getRepository(location);
					if (repository != null) {
						location = ResourceUtil.getRepositoryRelativePath(
								location, repository);
						if (location != null) {
							gitPath = location.toPortableString();
						}
					}
				} else {
					repository = HiddenResources.INSTANCE.getRepository(uri);
					if (repository != null) {
						gitPath = HiddenResources.INSTANCE.getGitPath(uri);
					}
				}
				if (gitPath != null && repository != null) {
					IndexDiffCacheEntry indexDiffCacheForRepository = IndexDiffCache
							.getInstance().getIndexDiffCacheEntry(repository);
					if (indexDiffCacheForRepository != null) {
						indexDiffCacheForRepository.refreshFiles(
								Collections.singletonList(gitPath));
					}
				}
			}
		}

		@Override
		public void handleDocumentConnected() {
			// Nothing
		}

		@Override
		public void handleDocumentDisconnected() {
			// Nothing
		}

		@Override
		public void handleDocumentFlushed() {
			try {
				save();
			} catch (CoreException e) {
				Activator.handleStatus(e.getStatus(), true);
			}
		}

		@Override
		public void handleDocumentDeleted() {
			// Nothing
		}

		@Override
		public void handleDocumentSaved() {
			// Nothing
		}
	}

	/**
	 * A {@link LocalResourceTypedElement} for a hidden resource, which may
	 * correspond to a real {@link IFile}.
	 */
	protected static class HiddenResourceTypedElement
			extends LocalResourceTypedElement {

		private final IFile realFile;

		private HiddenResourceTypedElement(IFile file, IFile realFile) {
			super(file);
			this.realFile = realFile;
		}

		/**
		 * Retrieves the real file for this {@link HiddenResourceTypedElement}.
		 *
		 * @return the {@link IFile}, or {@code null} if none
		 */
		public IFile getRealFile() {
			return realFile;
		}

		@Override
		public boolean equals(Object obj) {
			// realFile not considered
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			// realFile not considered
			return super.hashCode();
		}
	}

	private static class FolderNode extends DiffNode {

		private final Image image;

		private String name;

		FolderNode(IDiffContainer parent, String name, Image image) {
			super(parent, Differencer.NO_CHANGE);
			this.name = name;
			this.image = image;
		}

		@Override
		public String getName() {
			return name;
		}

		void setName(String name) {
			this.name = name;
		}

		@Override
		public Image getImage() {
			return image;
		}
	}
}
