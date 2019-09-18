package org.eclipse.egit.ui.internal.history;

import java.util.Objects;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;

/**
 * A wrapper around {@link ActionContributionItem} which always returns
 * {@code true} in {@link #isDynamic()}
 */
public class DynamicActionContributionItem extends ContributionItem {

	private final ActionContributionItem inner;

	/**
	 * @param action
	 */
	public DynamicActionContributionItem(IAction action) {
		inner = new ActionContributionItem(action);
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	@Override
	public void fill(Composite parent) {
		inner.fill(parent);
	}

	@Override
	public void fill(Menu menu, int index) {
		inner.fill(menu, index);
	}

	@Override
	public void fill(ToolBar parent, int index) {
		inner.fill(parent, index);
	}

	@Override
	public void dispose() {
		inner.dispose();
	}

	@Override
	public boolean isEnabled() {
		return inner.isEnabled();
	}

	@Override
	public boolean isVisible() {
		return inner.isVisible();
	}

	@Override
	public void update() {
		inner.update();
	}

	@Override
	public void update(String id) {
		inner.update(id);
	}

	@Override
	public int hashCode() {
		return inner.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DynamicActionContributionItem))
			return false;

		return Objects.equals(inner,
				((DynamicActionContributionItem) obj).inner);
	}
}
