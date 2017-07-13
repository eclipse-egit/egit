/*******************************************************************************
 * Copyright (c) 2013, 2016 SAP AG and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 485511
 *******************************************************************************/
package org.eclipse.egit.core.internal.rebase;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffChangedListener;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.jgit.errors.IllegalTodoFileModification;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RebaseTodoFile;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.RebaseTodoLine.Action;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.GitDateFormatter;

/**
 * Representation of the {@link RebaseTodoFile} for Rebase-Todo and
 * Rebase-Done-File of a {@link Repository}.
 *
 * Reparses the rebase plan when the index changes or when a {@code Ref} is
 * moving in order to keep the in-memory plan in sync with the one on disk.
 */
public class RebaseInteractivePlan implements IndexDiffChangedListener,
		RefsChangedListener {

	/**
	 * Classes that implement this interface provide methods that deal with
	 * changes made to a {@link RebaseInteractivePlan}
	 * <p>
	 * An Instance that implements this interface it can be added to a
	 * {@link RebaseInteractivePlan} by using the
	 * {@link RebaseInteractivePlan#addRebaseInteractivePlanChangeListener(RebaseInteractivePlanChangeListener)
	 * addRebaseInteractivePlanChangeListener} method and removed using the
	 * {@link RebaseInteractivePlan#removeRebaseInteractivePlanChangeListener(RebaseInteractivePlanChangeListener)
	 * removeRebaseInteractivePlanChangeListener} method. When a change is made
	 * to an {@link PlanElement} in this {@link RebaseInteractivePlan} (including
	 * structural changes) the appropriate method will be invoked.
	 */
	public static interface RebaseInteractivePlanChangeListener {
		/**
		 * Will be invoked if the order of either the Rebase-Todo-File or
		 * Rebase-Done-File changes (structural change).
		 *
		 * @param rebaseInteractivePlan
		 * @param element
		 * @param oldIndex
		 * @param newIndex
		 */
		public void planElementsOrderChanged(
				RebaseInteractivePlan rebaseInteractivePlan, PlanElement element,
				int oldIndex, int newIndex);

		/**
		 * Will be invoked if the {@link ElementType} of an {@link PlanElement}
		 * changes.
		 *
		 * @param rebaseInteractivePlan
		 * @param element
		 * @param oldType
		 * @param newType
		 */
		public void planElementTypeChanged(
				RebaseInteractivePlan rebaseInteractivePlan, PlanElement element,
				ElementAction oldType, ElementAction newType);

		/**
		 * Will be invoked after the list of {@link PlanElement Elements} has been
		 * parsed from the {@link Repository}
		 *
		 * @param plan
		 */
		public void planWasUpdatedFromRepository(RebaseInteractivePlan plan);
	}

	private CopyOnWriteArrayList<RebaseInteractivePlanChangeListener> planChangeListeners = new CopyOnWriteArrayList<RebaseInteractivePlanChangeListener>();

	private List<PlanElement> todoList;

	private List<PlanElement> doneList;

	private JoinedList<List<PlanElement>, PlanElement> planList;

	private final WeakReference<Repository> repositoryRef;

	private final File myGitDir;

	private ListenerHandle refsChangedListener;

	private static final Map<File, RebaseInteractivePlan> planRegistry = new HashMap<File, RebaseInteractivePlan>();

	private static final String REBASE_TODO = "rebase-merge/git-rebase-todo"; //$NON-NLS-1$

	private static final String REBASE_DONE = "rebase-merge/done"; //$NON-NLS-1$

	/**
	 * Provides a singleton instance of {@link RebaseInteractivePlan} for a
	 * given {@link Repository}
	 * <p>
	 * If a {@link RebaseInteractivePlan} for the given {@link Repository} has
	 * already been created and has not been disposed yet, this instance is
	 * returned, otherwise a newly created instance is returned.
	 *
	 * @param repo
	 * @return the {@link RebaseInteractivePlan} for the given
	 *         {@link Repository}
	 */
	public static RebaseInteractivePlan getPlan(Repository repo) {
		RebaseInteractivePlan plan = planRegistry.get(repo.getDirectory());
		if (plan == null) {
			plan = new RebaseInteractivePlan(repo);
			planRegistry.put(repo.getDirectory(), plan);
		}
		return plan;
	}

	private RebaseInteractivePlan(Repository repo) {
		this.repositoryRef = new WeakReference<>(repo);
		this.myGitDir = repo.getDirectory().getAbsoluteFile();
		reparsePlan(repo);
		registerIndexDiffChangeListener(repo);
		registerRefChangedListener();
	}

	private void registerIndexDiffChangeListener(Repository repository) {
		IndexDiffCacheEntry entry = org.eclipse.egit.core.Activator.getDefault()
				.getIndexDiffCache().getIndexDiffCacheEntry(repository);
		if (entry != null) {
			entry.addIndexDiffChangedListener(this);
		}
	}

	private void unregisterIndexDiffChangeListener() {
		Repository repository = getRepository();
		if (repository != null) {
			IndexDiffCacheEntry entry = org.eclipse.egit.core.Activator
					.getDefault().getIndexDiffCache()
					.getIndexDiffCacheEntry(repository);
			if (entry != null) {
				entry.removeIndexDiffChangedListener(this);
			}
		}
	}

	private void registerRefChangedListener() {
		refsChangedListener = Repository.getGlobalListenerList()
				.addRefsChangedListener(this);
	}

	/**
	 * Reparse plan when {@code IndexDiff} changed
	 */
	@Override
	public void indexDiffChanged(Repository repo, IndexDiffData indexDiffData) {
		if (getRepository() == repo)
			reparsePlan(repo);
	}

	/**
	 * Reparse plan when a {@code Ref} changed
	 *
	 * @param event
	 */
	@Override
	public void onRefsChanged(RefsChangedEvent event) {
		Repository repo = event.getRepository();
		if (getRepository() == repo)
			reparsePlan(repo);
	}

	/**
	 * Dispose the plan.
	 * <p>
	 * The next invocation of {@link RebaseInteractivePlan#getPlan(Repository)}
	 * will create a new {@link RebaseInteractivePlan} instance.
	 */
	public void dispose() {
		reparsePlan(getRepository());
		notifyPlanWasUpdatedFromRepository();
		planRegistry.remove(myGitDir);
		planList.clear();
		planChangeListeners.clear();
		unregisterIndexDiffChangeListener();
		refsChangedListener.remove();
	}

	/**
	 * @return a list representation of the {@link PlanElement Elements} for this
	 *         plan.
	 */
	public List<PlanElement> getList() {
		return planList;
	}

	/**
	 * @return the repository
	 */
	public Repository getRepository() {
		return repositoryRef.get();
	}

	/**
	 * Adds a {@link RebaseInteractivePlanChangeListener} to this
	 * {@link RebaseInteractivePlan} if it has not been registered yet
	 *
	 * @param listener
	 *            the {@link RebaseInteractivePlanChangeListener} to be added
	 * @return true if the listener has been added, otherwise false
	 */
	public boolean addRebaseInteractivePlanChangeListener(
			RebaseInteractivePlanChangeListener listener) {
		return planChangeListeners.addIfAbsent(listener);
	}

	/**
	 * Removes a {@link RebaseInteractivePlanChangeListener} from this
	 * {@link RebaseInteractivePlan} if it has been registered before
	 *
	 * @param listener
	 *            the {@link RebaseInteractivePlanChangeListener} to be removed
	 * @return true if the listener has been removed, otherwise false
	 */
	public boolean removeRebaseInteractivePlanChangeListener(
			RebaseInteractivePlanChangeListener listener) {
		return planChangeListeners.remove(listener);
	}

	private void notifyPlanElementsOrderChange(PlanElement element, int oldIndex,
			int newIndex) {
		persist(getRepository());
		for (RebaseInteractivePlanChangeListener listener : planChangeListeners)
			listener.planElementsOrderChanged(this, element, oldIndex, newIndex);
	}

	private void notifyPlanElementActionChange(PlanElement element,
			ElementAction oldType, ElementAction newType) {
		persist(getRepository());
		for (RebaseInteractivePlanChangeListener listener : planChangeListeners)
			listener.planElementTypeChanged(this, element, oldType, newType);
	}

	private void notifyPlanWasUpdatedFromRepository() {
		for (RebaseInteractivePlanChangeListener listener : planChangeListeners)
			listener.planWasUpdatedFromRepository(this);
	}

	private void reparsePlan(Repository repository) {
		if (repository != null) {
			try (RevWalk walk = new RevWalk(repository.newObjectReader())) {
				doneList = parseDone(repository, walk);
				todoList = parseTodo(repository, walk);
			}
			planList = JoinedList.wrap(doneList, todoList);
			notifyPlanWasUpdatedFromRepository();
		}
	}

	private List<PlanElement> parseTodo(Repository repository, RevWalk walk) {
		List<RebaseTodoLine> rebaseTodoLines;
		try {
			rebaseTodoLines = repository.readRebaseTodo(REBASE_TODO, true);
		} catch (IOException e) {
			rebaseTodoLines = new LinkedList<RebaseTodoLine>();
		}
		List<PlanElement> todoElements = createElementList(rebaseTodoLines,
				walk);
		return todoElements;
	}

	private List<PlanElement> parseDone(Repository repository, RevWalk walk) {
		List<RebaseTodoLine> rebaseDoneLines;
		try {
			rebaseDoneLines = repository.readRebaseTodo(REBASE_DONE, false);
		} catch (IOException e) {
			rebaseDoneLines = new LinkedList<RebaseTodoLine>();
		}
		List<PlanElement> doneElements = createElementList(rebaseDoneLines,
				walk);
		return doneElements;
	}

	private List<PlanElement> createElementList(
			List<RebaseTodoLine> rebaseTodoLines, RevWalk walk) {
		List<PlanElement> planElements = new ArrayList<PlanElement>(
				rebaseTodoLines.size());
		for (RebaseTodoLine todoLine : rebaseTodoLines) {
			PlanElement element = createElement(todoLine, walk);
			planElements.add(element);
		}
		return planElements;
	}

	private PlanElement createElement(RebaseTodoLine todoLine, RevWalk walk) {
		PersonIdent author = null;
		PersonIdent committer = null;

		RevCommit commit = loadCommit(todoLine.getCommit(), walk);
		if (commit != null) {
			author = commit.getAuthorIdent();
			committer = commit.getCommitterIdent();
		}

		PlanElement element = new PlanElement(todoLine, author, committer);
		return element;
	}

	private RevCommit loadCommit(AbbreviatedObjectId abbreviatedObjectId,
			RevWalk walk) {
		if (abbreviatedObjectId != null) {
			try {
				Collection<ObjectId> resolved = walk.getObjectReader().resolve(
						abbreviatedObjectId);
				if (resolved.size() == 1) {
					RevCommit commit = walk.parseCommit(resolved
							.iterator().next());
					return commit;
				}
			} catch (IOException e) {
				// ignore, we assume no author/committer then
			}
		}
		return null;
	}

	/**
	 * @return true if the rebase has already been started processing the plan,
	 *         otherwise false
	 */
	public boolean hasRebaseBeenStartedYet() {
		return isRebasingInteractive() && doneList.size() > 0;
	}

	/**
	 * @return true if repository state is
	 *         {@link RepositoryState#REBASING_INTERACTIVE}
	 */
	public boolean isRebasingInteractive() {
		Repository repository = getRepository();
		return repository != null && repository
				.getRepositoryState() == RepositoryState.REBASING_INTERACTIVE;
	}

	/**
	 * Moves an {@link PlanElement} of Type {@link ElementType#TODO} down if
	 * possible
	 *
	 * @param element
	 *            the {@link PlanElement} to move down
	 */
	public void moveTodoEntryDown(PlanElement element) {
		new MoveHelper(todoList, this).moveTodoEntryDown(element);
	}

	/**
	 * Moves an {@link PlanElement} of Type {@link ElementType#TODO} up if possible
	 *
	 * @param element
	 *            the {@link PlanElement} to move up
	 */
	public void moveTodoEntryUp(PlanElement element) {
		new MoveHelper(todoList, this).moveTodoEntryUp(element);
	}

	/**
	 * Moves a given {@link PlanElement sourceElement} of Type
	 * {@link ElementType#TODO} to the current position of a {@link PlanElement
	 * targetElement} in it's list representation (considering that this list
	 * representation may be reversed). If <code>before</code> is true the
	 * {@link PlanElement sourceElement} will be placed just before the
	 * {@link PlanElement targetElement}
	 *
	 * @param sourceElement
	 * @param targetElement
	 * @param before
	 */
	public void moveTodoEntry(PlanElement sourceElement, PlanElement targetElement,
			boolean before) {
		new MoveHelper(todoList, this).moveTodoEntry(sourceElement,
				targetElement, before);
	}

	/**
	 * Writes the plan to the FS.
	 * <p>
	 * Only {@link PlanElement Elements} of {@link ElementType#TODO} are
	 * persisted.
	 *
	 * @param repository
	 *            the plan belongs to
	 *
	 * @return true if the todo file has been written successfully, otherwise
	 *         false
	 */
	private boolean persist(Repository repository) {
		if (repository == null || repository
				.getRepositoryState() != RepositoryState.REBASING_INTERACTIVE) {
			return false;
		}
		List<RebaseTodoLine> todoLines = new LinkedList<RebaseTodoLine>();
		for (PlanElement element : planList.getSecondList())
			todoLines.add(element.getRebaseTodoLine());
		try {
			repository.writeRebaseTodoFile(REBASE_TODO, todoLines, false);
		} catch (IOException e) {
			Activator.logError(CoreText.RebaseInteractivePlan_WriteRebaseTodoFailed, e);
			throw new RuntimeException(e);
		}
		return true;
	}

	/**
	 * Parses the plan from the FS by reading the todo-File and the done-File if
	 * in state RebaseInteractive
	 *
	 * @throws IOException
	 */
	public void parse() throws IOException {
		if (!isRebasingInteractive())
			return;
		reparsePlan(getRepository());
	}

	/**
	 * This class wraps a {@link RebaseTodoLine} and holds additional
	 * information about the underlying commit, if available.
	 */
	public class PlanElement implements IAdaptable {
		private final RebaseTodoLine line;

		/** author info, may be null */
		private final PersonIdent author;

		/** committer info, may be null */
		private final PersonIdent committer;

		private PlanElement(RebaseTodoLine line, PersonIdent author,
				PersonIdent committer) {
			if (line == null)
				throw new IllegalArgumentException();
			this.line = line;
			this.author = author;
			this.committer = committer;
		}

		/**
		 * @return the {@link ElementType} for this {@link PlanElement}
		 */
		public ElementType getElementType() {
			if (todoList.indexOf(this) != -1)
				return ElementType.TODO;
			int indexInDone = doneList.indexOf(this);
			if (indexInDone != -1) {
				if (indexInDone == doneList.size() - 1
						&& isRebasingInteractive())
					return ElementType.DONE_CURRENT;
				return ElementType.DONE;
			}
			return null;
		}

		private RebaseTodoLine getRebaseTodoLine() {
			return line;
		}

		/**
		 * @return the CommitId of the wrapped {@link RebaseTodoLine}
		 */
		public AbbreviatedObjectId getCommit() {
			return line.getCommit();
		}

		/**
		 * @return the shortMessage of the wrapped {@link RebaseTodoLine}
		 */
		public String getShortMessage() {
			return line.getShortMessage();
		}

		/**
		 * @return the author name of the underlying commit
		 */
		public String getAuthor() {
			if (author == null)
				return ""; //$NON-NLS-1$
			else
				return author.getName();
		}

		/**
		 * @param dateFormatter
		 * @return the authored date of the underlying commit
		 */
		public String getAuthoredDate(GitDateFormatter dateFormatter) {
			if (author == null)
				return ""; //$NON-NLS-1$
			else
				return dateFormatter.formatDate(author);
		}

		/**
		 * @return the committer name of the underlying commit
		 */
		public String getCommitter() {
			if (committer == null)
				return ""; //$NON-NLS-1$
			else
				return committer.getName();
		}

		/**
		 * @param dateFormatter
		 * @return the commit date of the underlying commit
		 */
		public String getCommittedDate(GitDateFormatter dateFormatter) {
			if (committer == null)
				return ""; //$NON-NLS-1$
			else
				return dateFormatter.formatDate(committer);
		}

		/**
		 * This method maps the given {@link ElementAction} to the wrapped
		 * {@link RebaseTodoLine RebaseTodoLines} {@link Action}. If the
		 * {@link ElementAction} changes the registered
		 * {@link RebaseInteractivePlanChangeListener
		 * RebaseInteractivePlanChangeListeners} are notified.
		 *
		 * @param newAction
		 *            the {@link ElementAction} to be set
		 */
		public void setPlanElementAction(ElementAction newAction) {
			if (isComment()) {
				if (newAction == null)
					return;
				throw new IllegalArgumentException();
			}
			ElementAction oldAction = this.getPlanElementAction();
			if (oldAction == newAction)
				return;
			try {
				switch (newAction) {
				case SKIP:
					line.setAction(Action.COMMENT);
					break;
				case EDIT:
					line.setAction(Action.EDIT);
					break;
				case FIXUP:
					line.setAction(Action.FIXUP);
					break;
				case PICK:
					line.setAction(Action.PICK);
					break;
				case REWORD:
					line.setAction(Action.REWORD);
					break;
				case SQUASH:
					line.setAction(Action.SQUASH);
					break;
				default:
					throw new IllegalArgumentException();
				}
			} catch (IllegalTodoFileModification e) {
				// shouldn't happen
				throw new IllegalArgumentException(e);
			}
			notifyPlanElementActionChange(this, oldAction, newAction);
		}

		/**
		 * @return the {@link ElementAction} for this {@link PlanElement}
		 */
		public ElementAction getPlanElementAction() {
			if (isSkip())
				return ElementAction.SKIP;
			if (isComment())
				return null;
			switch (line.getAction()) {
			case EDIT:
				return ElementAction.EDIT;
			case FIXUP:
				return ElementAction.FIXUP;
			case PICK:
				return ElementAction.PICK;
			case SQUASH:
				return ElementAction.SQUASH;
			case REWORD:
				return ElementAction.REWORD;
			default:
				throw new IllegalStateException();
			}

		}

		/**
		 * @return true, if the given line is a pure comment, i.e. a comment
		 *         that doesn't hold a valid action line, otherwise false
		 */
		public boolean isComment() {
			return (Action.COMMENT.equals(line.getAction()) && null == line
					.getCommit());
		}

		/**
		 * @return true if this element is marked for deletion, i.e. a valid
		 *         action line has been commented out, otherwise false
		 */
		public boolean isSkip() {
			return (Action.COMMENT.equals(line.getAction()) && null != line
					.getCommit());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PlanElement other = (PlanElement) obj;
			if (other.line.getCommit() == null) {
				if (this.line.getCommit() == null)
					return true;
				return false;
			}
			if (!other.line.getCommit().equals(this.line.getCommit()))
				return false;
			if (!other.getPlanElementAction().equals(
					this.getPlanElementAction()))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		@Override
		public String toString() {
			return line.toString();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object getAdapter(Class adapter) {
			// TODO generify once EGit baseline is Eclipse 4.5
			if (adapter.isInstance(this)) {
				return this;
			} else if (Repository.class.equals(adapter)) {
				return repositoryRef.get();
			}
			return null;
		}
	}

	/**
	 * Wraps {@link Action} and additionally provides
	 * {@link ElementAction#SKIP}
	 */
	public enum ElementAction {
		/**
		 * The {@link PlanElement} will not be cherry-picked, i.e. changes are
		 * lost on the new branch. Internally this is mapped to
		 * {@link Action#COMMENT}, to comment out a {@link RebaseTodoLine}
		 */
		SKIP,
		/**
		 * Equivalent to {@link Action#EDIT};
		 */
		EDIT,
		/**
		 * Equivalent to {@link Action#PICK};
		 */
		PICK,
		/**
		 * Equivalent to {@link Action#SQUASH};
		 */
		SQUASH,
		/**
		 * Equivalent to {@link Action#FIXUP};
		 */
		FIXUP,
		/**
		 * Equivalent to {@link Action#REWORD};
		 */
		REWORD;
	}

	/**
	 * The type of an {@link PlanElement}
	 */
	public static enum ElementType {
		/**
		 * The {@link PlanElement} is present in the done-File, i.e. the
		 * {@link RebaseTodoLine} has already been processed.
		 */
		DONE,
		/**
		 * The {@link PlanElement} is present in the todo-File, i.e. the
		 * {@link RebaseTodoLine} has not been processed yet.
		 */
		TODO,
		/**
		 * Special case of {@link ElementType#DONE}.
		 * <p>
		 * The {@link PlanElement} is the last entry in the done-File, i.e. the
		 * {@link RebaseTodoLine} has already been processed and furthermore the
		 * rebase has not been finished yet
		 */
		DONE_CURRENT;
	}

	/**
	 * Helper class for moving plan elements
	 */
	public static class MoveHelper {

		private List<PlanElement> todoList;

		private RebaseInteractivePlan plan;

		/**
		 * @param todoList
		 * @param plan
		 */
		public MoveHelper(List<PlanElement> todoList, RebaseInteractivePlan plan) {
			this.todoList = todoList;
			this.plan = plan;
		}

		/**
		 * Move the element at the given index in the given list up if possible,
		 * otherwise this method has no effect on the list. Moving an element up
		 * is not possible if the list does not contain an element with the
		 * given index or the element at the given index has no next element.
		 *
		 * @param index
		 * @param list
		 * @return true if an element has been moved up, otherwise false
		 */
		private static boolean moveUp(final int index, final List<?> list) {
			if (index <= 0 || index > list.size())
				return false;
			Collections.swap(list, index, index - 1);
			return true;
		}

		/**
		 * Move the element at the given index in the given list down if
		 * possible, otherwise this method has no effect on the list. Moving an
		 * element down is not possible if the list does not contain an element
		 * with the given index or the element at the given index has no
		 * previous element.
		 *
		 * @param index
		 * @param list
		 * @return true if an element has been moved down, otherwise false
		 */
		private static boolean moveDown(final int index, final List<?> list) {
			if (index < 0 || index >= list.size() - 1)
				return false;
			Collections.swap(list, index, index + 1);
			return true;
		}

		/**
		 * Moves an {@link PlanElement} of Type {@link ElementType#TODO} down if
		 * possible
		 *
		 * @param element
		 *            the {@link PlanElement} to move down
		 */
		public void moveTodoEntryDown(PlanElement element) {
			List<PlanElement> list = todoList;
			int initialIndex = list.indexOf(element);
			int oldIndex;
			do {
				oldIndex = list.indexOf(element);
				moveDown(oldIndex, list);
			} while (list.get(oldIndex).isComment());
			int newIndex = list.indexOf(element);
			if (initialIndex != newIndex)
				plan.notifyPlanElementsOrderChange(element, initialIndex,
						newIndex);
		}

		/**
		 * Moves an {@link PlanElement} of Type {@link ElementType#TODO} up if
		 * possible
		 *
		 * @param element
		 *            the {@link PlanElement} to move up
		 */
		public void moveTodoEntryUp(PlanElement element) {
			List<PlanElement> list = todoList;
			int initialIndex = list.indexOf(element);
			int oldIndex;
			do {
				oldIndex = list.indexOf(element);
				moveUp(oldIndex, list);
			} while (list.get(oldIndex).isComment());
			int newIndex = list.indexOf(element);
			if (initialIndex != newIndex)
				plan.notifyPlanElementsOrderChange(element, initialIndex,
						newIndex);
		}

		private static void move(int sourceIndex, int targetIndex,
				final List<PlanElement> list) {
			if (sourceIndex == targetIndex)
				return;
			if (sourceIndex < targetIndex) {
				Collections.rotate(list.subList(sourceIndex, targetIndex + 1),
						-1);
			} else {
				Collections.rotate(list.subList(targetIndex, sourceIndex + 1),
						1);
			}
		}

		/**
		 * Moves a given {@link PlanElement sourceElement} of Type
		 * {@link ElementType#TODO} to the current position of a
		 * {@link PlanElement targetElement} in it's list representation
		 * (considering that this list representation may be reversed). If
		 * <code>before</code> is true the {@link PlanElement sourceElement}
		 * will be placed just before the {@link PlanElement targetElement}
		 *
		 * @param sourceElement
		 * @param targetElement
		 * @param before
		 */
		public void moveTodoEntry(PlanElement sourceElement,
				PlanElement targetElement, boolean before) {
			if (sourceElement == targetElement)
				return;
			Assert.isNotNull(sourceElement);
			Assert.isNotNull(targetElement);
			if (ElementType.TODO != sourceElement.getElementType())
				throw new IllegalArgumentException();

			List<PlanElement> list = todoList;

			int initialSourceIndex = list.indexOf(sourceElement);
			int targetIndex = list.indexOf(targetElement);

			if (targetIndex == -1 || initialSourceIndex == -1)
				return;
			if (targetIndex == initialSourceIndex)
				return;

			if (targetIndex > initialSourceIndex && before)
				targetIndex--;
			if (targetIndex < initialSourceIndex && !before)
				targetIndex++;

			move(initialSourceIndex, targetIndex, list);
			int newIndex = list.indexOf(sourceElement);
			if (initialSourceIndex != newIndex)
				plan.notifyPlanElementsOrderChange(sourceElement,
						initialSourceIndex, newIndex);
		}
	}

	/**
	 * List that provides a view to two joined lists
	 *
	 * @param <L>
	 *            The concrete type of the two lists
	 * @param <T>
	 *            The type of the elements in the lists
	 */
	public static class JoinedList<L extends List<T>, T> extends
			AbstractList<T> {

		private final L firstList, secondList;

		/**
		 * @return the first list
		 */
		public L getFirstList() {
			return firstList;
		}

		/**
		 * @return the second list
		 */
		public L getSecondList() {
			return secondList;
		}

		/**
		 * @param first
		 * @param second
		 */
		private JoinedList(L first, L second) {
			super();
			Assert.isNotNull(first);
			Assert.isNotNull(second);
			this.firstList = first;
			this.secondList = second;
		}

		/**
		 * Creates a newly List that provides a view to two joined lists.
		 *
		 * @param first
		 * @param second
		 * @return a new view on a concatenation of both lists
		 */
		public static <L extends List<T>, T> JoinedList<L, T> wrap(L first,
				L second) {
			return new JoinedList<L, T>(first, second);
		}

		private static class RelativeIndex<T> {
			private final int relativeIndex;

			private final List<T> list;

			public final int getRelativeIndex() {
				return relativeIndex;
			}

			public final List<T> getList() {
				return list;
			}

			RelativeIndex(int relativeIndex, List<T> list) {
				super();
				this.relativeIndex = relativeIndex;
				this.list = list;
			}
		}

		private RelativeIndex<T> mapAbsolutIndex(int index) {
			if (index < firstList.size())
				return new RelativeIndex<T>(index, firstList);
			return new RelativeIndex<T>(index - firstList.size(), secondList);
		}

		/**
		 * if the given index is smaller than the first lists size the element
		 * is added to the first list. if the given index points to the seam of
		 * the joined lists, the given element will be added to the second list.
		 * More precisely a element is added to the second list if the given
		 * index is greater or equals the first lists size, otherwise it's added
		 * to the first list.
		 */
		@Override
		public void add(int index, T element) {
			RelativeIndex<T> rel = mapAbsolutIndex(index);
			rel.getList().add(rel.getRelativeIndex(), element);
			modCount++;
		}

		@Override
		public T get(int index) {
			RelativeIndex<T> rel = mapAbsolutIndex(index);
			return rel.getList().get(rel.getRelativeIndex());
		}

		@Override
		public T remove(int index) {
			RelativeIndex<T> rel = mapAbsolutIndex(index);
			modCount++;
			return rel.getList().remove(rel.getRelativeIndex());
		}

		@Override
		public T set(int index, T element) {
			RelativeIndex<T> rel = mapAbsolutIndex(index);
			return rel.getList().set(rel.getRelativeIndex(), element);
		}

		@Override
		public int size() {
			return firstList.size() + secondList.size();
		}
	}
}
