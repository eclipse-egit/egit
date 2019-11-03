/*******************************************************************************
 * Copyright (C) 2019 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jgit.attributes.AttributesNodeProvider;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.events.ConfigChangedEvent;
import org.eclipse.jgit.events.IndexChangedEvent;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.ListenerList;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RepositoryEvent;
import org.eclipse.jgit.events.RepositoryListener;
import org.eclipse.jgit.events.WorkingTreeModifiedEvent;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefRename;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.FS;

/**
 * A {@code RepositoryHandle} completely wraps another {@link Repository}.
 */
class RepositoryHandle extends Repository {

	private final Repository delegate;

	private final List<ListenerHandle> listeners = new ArrayList<>();

	/**
	 * Creates a new {@link RepositoryHandle} for the given {@code delegate}.
	 *
	 * @param delegate
	 *            to wrap with the handle; must <em>not</em> be another
	 *            {@link RepositoryHandle}
	 */
	RepositoryHandle(Repository delegate) {
		super(new RepositoryBuilder());
		// Internal error; no translation.
		Assert.isLegal(!(delegate instanceof RepositoryHandle),
				"Delegate of a RepositoryHandle must not be another RepositoryHandle"); //$NON-NLS-1$
		this.delegate = delegate;
		addListeners(listeners, delegate.getListenerList(),
				new WeakReference<>(this));
	}

	private static void addListeners(List<ListenerHandle> listeners,
			ListenerList list, WeakReference<RepositoryHandle> ref) {
		// Add listeners that change the repository so that forwarded events
		// have this handle set as repository. Since we can't change the
		// repository in the original event we have to re-fire new ones.
		//
		// We use a static method and a WeakReference to the RepositoryHandle
		// instance to avoid a hard reference loop from the delegate back to the
		// RepositoryHandle:
		//
		// delegate->listerList->listenerHandle->repositoryHandle,
		//
		// which would prevent GC from collecting the RepositoryHandle because
		// of the hard reference to the delegate inside the RepositoryCache's
		// RepositoryReference.
		listeners.add(list.addConfigChangedListener(event -> {
			Repository repo = ref.get();
			if (repo != null) {
				fireEvent(repo, new ConfigChangedEvent());
			}
		}));
		listeners.add(list.addIndexChangedListener(event -> {
			Repository repo = ref.get();
			if (repo != null) {
				fireEvent(repo, new IndexChangedEvent(event.isInternal()));
			}
		}));
		listeners.add(list.addRefsChangedListener(event -> {
			Repository repo = ref.get();
			if (repo != null) {
				fireEvent(repo, new RefsChangedEvent());
			}
		}));
		listeners.add(list.addWorkingTreeModifiedListener(event -> {
			Repository repo = ref.get();
			if (repo != null) {
				fireEvent(repo,
						new WorkingTreeModifiedEvent(event.getModified(),
						event.getDeleted()));
			}
		}));
	}

	private static void fireEvent(Repository repo,
			RepositoryEvent<? extends RepositoryListener> event) {
		// Since the delegate will also dispatch its event through the global
		// list, which we intercept in the RepositoryCache, we don't use
		// repo.fireEvent() here but instead dispatch the event ourselves
		// through the repo's listener list. Otherwise there'd be two
		// notifications through the global list.
		event.setRepository(repo);
		repo.getListenerList().dispatch(event);
	}

	Repository getDelegate() {
		return delegate;
	}

	@Override
	public final ListenerList getListenerList() {
		return super.getListenerList();
	}

	@Override
	public final void fireEvent(RepositoryEvent<?> event) {
		super.fireEvent(event);
	}

	@Override
	public final void incrementOpen() {
		super.incrementOpen();
	}

	@Override
	public final void close() {
		if (delegate instanceof CachingRepository) {
			((CachingRepository) delegate).clearConfigCache();
		}
		super.close();
	}

	@Override
	protected void doClose() {
		listeners.forEach(ListenerHandle::remove);
		listeners.clear();
		delegate.close();
	}

	@Override
	public String toString() {
		return '[' + this.getClass().getSimpleName() + ':' + delegate.toString()
				+ ']';
	}

	// Override all other methods and forward to the delegate

	@Override
	public void create() throws IOException {
		delegate.create();
	}

	@Override
	public void create(boolean bare) throws IOException {
		delegate.create(bare);
	}

	@Override
	public File getDirectory() {
		return delegate.getDirectory();
	}

	@Override
	public String getIdentifier() {
		return delegate.getIdentifier();
	}

	@Override
	public ObjectDatabase getObjectDatabase() {
		return delegate.getObjectDatabase();
	}

	@Override
	public ObjectInserter newObjectInserter() {
		return delegate.newObjectInserter();
	}

	@Override
	public ObjectReader newObjectReader() {
		return delegate.newObjectReader();
	}

	@Override
	public RefDatabase getRefDatabase() {
		return delegate.getRefDatabase();
	}

	@Override
	public StoredConfig getConfig() {
		return delegate.getConfig();
	}

	@Override
	public AttributesNodeProvider createAttributesNodeProvider() {
		return delegate.createAttributesNodeProvider();
	}

	@Override
	public FS getFS() {
		return delegate.getFS();
	}

	@Override
	public ObjectLoader open(AnyObjectId objectId)
			throws MissingObjectException, IOException {
		return delegate.open(objectId);
	}

	@Override
	public ObjectLoader open(AnyObjectId objectId, int typeHint)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		return delegate.open(objectId, typeHint);
	}

	@Override
	public RefUpdate updateRef(String ref) throws IOException {
		return delegate.updateRef(ref);
	}

	@Override
	public RefUpdate updateRef(String ref, boolean detach) throws IOException {
		return delegate.updateRef(ref, detach);
	}

	@Override
	public RefRename renameRef(String fromRef, String toRef)
			throws IOException {
		return delegate.renameRef(fromRef, toRef);
	}

	@Override
	public ObjectId resolve(String revstr)
			throws AmbiguousObjectException, IncorrectObjectTypeException,
			RevisionSyntaxException, IOException {
		return delegate.resolve(revstr);
	}

	@Override
	public String simplify(String revstr)
			throws AmbiguousObjectException, IOException {
		return delegate.simplify(revstr);
	}

	@Override
	public String getFullBranch() throws IOException {
		return delegate.getFullBranch();
	}

	@Override
	public String getBranch() throws IOException {
		return delegate.getBranch();
	}

	@Override
	public Set<ObjectId> getAdditionalHaves() {
		return delegate.getAdditionalHaves();
	}

	@Override
	public Map<AnyObjectId, Set<Ref>> getAllRefsByPeeledObjectId() {
		return delegate.getAllRefsByPeeledObjectId();
	}

	@Override
	public File getIndexFile() throws NoWorkTreeException {
		return delegate.getIndexFile();
	}

	@Override
	public RevCommit parseCommit(AnyObjectId id)
			throws IncorrectObjectTypeException, IOException,
			MissingObjectException {
		return delegate.parseCommit(id);
	}

	@Override
	public DirCache readDirCache() throws NoWorkTreeException,
			CorruptObjectException, IOException {
		return delegate.readDirCache();
	}

	@Override
	public DirCache lockDirCache() throws NoWorkTreeException,
			CorruptObjectException, IOException {
		return delegate.lockDirCache();
	}

	@Override
	public RepositoryState getRepositoryState() {
		return delegate.getRepositoryState();
	}

	@Override
	public boolean isBare() {
		return delegate.isBare();
	}

	@Override
	public File getWorkTree() throws NoWorkTreeException {
		return delegate.getWorkTree();
	}

	@Override
	public void scanForRepoChanges() throws IOException {
		delegate.scanForRepoChanges();
	}

	@Override
	public void notifyIndexChanged(boolean internal) {
		delegate.notifyIndexChanged(internal);
	}

	@Override
	public String shortenRemoteBranchName(String refName) {
		return delegate.shortenRemoteBranchName(refName);
	}

	@Override
	public String getRemoteName(String refName) {
		return delegate.getRemoteName(refName);
	}

	@Override
	public String getGitwebDescription() throws IOException {
		return delegate.getGitwebDescription();
	}

	@Override
	public void setGitwebDescription(String description)
			throws IOException {
		delegate.setGitwebDescription(description);
	}

	@Override
	public ReflogReader getReflogReader(String refName) throws IOException {
		return delegate.getReflogReader(refName);
	}

	@Override
	public String readMergeCommitMsg() throws IOException, NoWorkTreeException {
		return delegate.readMergeCommitMsg();
	}

	@Override
	public void writeMergeCommitMsg(String msg) throws IOException {
		delegate.writeMergeCommitMsg(msg);
	}

	@Override
	public String readCommitEditMsg() throws IOException, NoWorkTreeException {
		return delegate.readCommitEditMsg();
	}

	@Override
	public void writeCommitEditMsg(String msg) throws IOException {
		delegate.writeCommitEditMsg(msg);
	}

	@Override
	public List<ObjectId> readMergeHeads()
			throws IOException, NoWorkTreeException {
		return delegate.readMergeHeads();
	}

	@Override
	public void writeMergeHeads(List<? extends ObjectId> heads)
			throws IOException {
		delegate.writeMergeHeads(heads);
	}

	@Override
	public ObjectId readCherryPickHead()
			throws IOException, NoWorkTreeException {
		return delegate.readCherryPickHead();
	}

	@Override
	public ObjectId readRevertHead() throws IOException, NoWorkTreeException {
		return delegate.readRevertHead();
	}

	@Override
	public void writeCherryPickHead(ObjectId head) throws IOException {
		delegate.writeCherryPickHead(head);
	}

	@Override
	public void writeRevertHead(ObjectId head) throws IOException {
		delegate.writeRevertHead(head);
	}

	@Override
	public void writeOrigHead(ObjectId head) throws IOException {
		delegate.writeOrigHead(head);
	}

	@Override
	public ObjectId readOrigHead() throws IOException, NoWorkTreeException {
		return delegate.readOrigHead();
	}

	@Override
	public String readSquashCommitMsg() throws IOException {
		return delegate.readSquashCommitMsg();
	}

	@Override
	public void writeSquashCommitMsg(String msg) throws IOException {
		delegate.writeSquashCommitMsg(msg);
	}

	@Override
	public List<RebaseTodoLine> readRebaseTodo(String path,
			boolean includeComments) throws IOException {
		return delegate.readRebaseTodo(path, includeComments);
	}

	@Override
	public void writeRebaseTodoFile(String path, List<RebaseTodoLine> steps,
			boolean append) throws IOException {
		delegate.writeRebaseTodoFile(path, steps, append);
	}

	@Override
	public Set<String> getRemoteNames() {
		return delegate.getRemoteNames();
	}

	@Override
	public void autoGC(ProgressMonitor monitor) {
		delegate.autoGC(monitor);
	}
}
