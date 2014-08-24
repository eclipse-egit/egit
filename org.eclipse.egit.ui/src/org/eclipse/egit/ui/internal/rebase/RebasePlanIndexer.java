package org.eclipse.egit.ui.internal.rebase;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.ElementAction;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.RebaseInteractivePlanChangeListener;

/**
 * @author Vadim
 *
 */
public class RebasePlanIndexer {
	private RebaseInteractivePlan plan;

	private List<PlanElement> filteredPlan;

	private RebaseInteractivePlanChangeListener listener = new RebasePlanChangeListener();

	private int lastFoundElementPosition;

	/**
	 * @param plan
	 */
	public RebasePlanIndexer(RebaseInteractivePlan plan) {
		this.plan = plan;
		this.filteredPlan = new ArrayList<PlanElement>();

		createIndex();

		plan.addRebaseInteractivePlanChangeListener(listener);
	}

	private void createIndex() {
		lastFoundElementPosition = 0;

		filteredPlan.clear();
		for (PlanElement element : plan.getList()) {
			if (!element.isComment()) {
				filteredPlan.add(element);
			}
		}
	}

	/**
	 * @param element
	 * @return 123
	 */
	public int indexOf(Object element) {
		if (filteredPlan.isEmpty())
			return -1;

		if (filteredPlan.get(lastFoundElementPosition).equals(element))
			return lastFoundElementPosition;

		int upIndex = getSeqIndex(lastFoundElementPosition + 1);
		if (filteredPlan.get(upIndex).equals(element)) {
			lastFoundElementPosition = upIndex;
			return lastFoundElementPosition;
		}

		int downIndex = getSeqIndex(lastFoundElementPosition - 1);
		if (filteredPlan.get(downIndex).equals(element)) {
			lastFoundElementPosition = downIndex;
			return lastFoundElementPosition;
		}

		int index = getSeqIndex(upIndex + 1);
		while (index != downIndex) {
			if (filteredPlan.get(index).equals(element)) {
				lastFoundElementPosition = index;
				return lastFoundElementPosition;
			}
			index = getSeqIndex(index + 1);
		}

		lastFoundElementPosition = 0;
		return -1;
	}

	private int getSeqIndex(int index) {
		if (index < 0) {
			return filteredPlan.size() + index;
		}

		if (index >= filteredPlan.size()) {
			return filteredPlan.size() - index;
		}

		return index;
	}

	/**
	 *
	 */
	public void dispose() {
		plan.removeRebaseInteractivePlanChangeListener(listener);
	}

	private class RebasePlanChangeListener implements RebaseInteractivePlanChangeListener {
		public void planElementTypeChanged(
				RebaseInteractivePlan rebaseInteractivePlan,
				PlanElement element, ElementAction oldType,
				ElementAction newType) {
			// do nothing
		}

		public void planElementsOrderChanged(
				RebaseInteractivePlan rebaseInteractivePlan,
				PlanElement element, int oldIndex, int newIndex) {
			createIndex();
		}

		public void planWasUpdatedFromRepository(RebaseInteractivePlan newPlan) {
			createIndex();
		}
	}
}
