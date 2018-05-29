/*******************************************************************************
 * Copyright (c) 2014 Vadim Dmitriev and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Vadim Dmitriev - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.ElementAction;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.RebaseInteractivePlanChangeListener;

/**
 * This class is used to find an index of the rebase plan entry in a list of
 * non-comment rebase plan entries. It is optimized for cases in which
 * {@link RebasePlanIndexer#indexOf indexOf} is invoked for plan elements in the
 * same order in which these elements are stored in the rebase plan (in no
 * specific direction). For example, if the last found element was at position
 * 10 then the next time {@link RebasePlanIndexer#indexOf indexOf} is invoked it
 * will first check element at index 10 and elements in adjacent indices: 9 and
 * 11. If the element isn't found in these positions
 * {@link RebasePlanIndexer#indexOf indexOf} falls back to sequential search.
 */
public class RebasePlanIndexer {
	private RebaseInteractivePlan plan;

	private List<PlanElement> filteredPlan;

	private RebaseInteractivePlanChangeListener listener;

	private int lastFoundElementPosition;

	/**
	 * Constructs indexer for the rebase plan.
	 *
	 * @param plan
	 *            rebase plan
	 */
	public RebasePlanIndexer(RebaseInteractivePlan plan) {
		this.plan = plan;
		this.filteredPlan = new ArrayList<>();

		listener = new RebasePlanChangeListener();
		plan.addRebaseInteractivePlanChangeListener(listener);

		createIndex();
	}

	private void createIndex() {
		lastFoundElementPosition = 0;

		filteredPlan.clear();
		for (PlanElement element : plan.getList()) {
			if (!element.isComment())
				filteredPlan.add(element);
		}
	}

	/**
	 * Returns index of the element in the rebase plan.
	 *
	 * @param element
	 *            rebase plan element
	 * @return index of the element in the rebase plan or -1 if the element is
	 *         not found
	 */
	public int indexOf(Object element) {
		if (filteredPlan.isEmpty())
			return -1;

		if (filteredPlan.get(lastFoundElementPosition).equals(element))
			return lastFoundElementPosition;

		int upIndex = mapToCircularIndex(lastFoundElementPosition + 1);
		if (filteredPlan.get(upIndex).equals(element)) {
			lastFoundElementPosition = upIndex;
			return lastFoundElementPosition;
		}

		int downIndex = mapToCircularIndex(lastFoundElementPosition - 1);
		if (filteredPlan.get(downIndex).equals(element)) {
			lastFoundElementPosition = downIndex;
			return lastFoundElementPosition;
		}

		int index = mapToCircularIndex(upIndex + 1);
		while (index != downIndex) {
			if (filteredPlan.get(index).equals(element)) {
				lastFoundElementPosition = index;
				return lastFoundElementPosition;
			}
			index = mapToCircularIndex(index + 1);
		}

		lastFoundElementPosition = 0;
		return -1;
	}

	private int mapToCircularIndex(int index) {
		int size = filteredPlan.size();

		if (index < 0)
			return size + index;

		if (index >= size)
			return index - size;

		return index;
	}

	/**
	 * Disposes of this indexer.
	 */
	public void dispose() {
		plan.removeRebaseInteractivePlanChangeListener(listener);
	}

	private class RebasePlanChangeListener implements RebaseInteractivePlanChangeListener {
		@Override
		public void planElementTypeChanged(
				RebaseInteractivePlan rebaseInteractivePlan,
				PlanElement element, ElementAction oldType,
				ElementAction newType) {
			// do nothing
		}

		@Override
		public void planElementsOrderChanged(
				RebaseInteractivePlan rebaseInteractivePlan,
				PlanElement element, int oldIndex, int newIndex) {
			createIndex();
		}

		@Override
		public void planWasUpdatedFromRepository(RebaseInteractivePlan newPlan) {
			createIndex();
		}
	}
}
