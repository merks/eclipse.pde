/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * SimpleCSAddStepAction
 *
 */
public class SimpleCSRemoveSubStepAction extends Action {

	private ISimpleCSSubItemObject fSubItem;
	
	/**
	 * 
	 */
	public SimpleCSRemoveSubStepAction() {
		// TODO: MP: Update
		setText(PDEUIMessages.SimpleCSRemoveSubStepAction_0);
//		setImageDescriptor(PDEPluginImages.DESC_GEL_SC_OBJ);
//		setToolTipText(PDEUIMessages.SchemaEditor_NewElement_tooltip);
	}

	/**
	 * @param subitem
	 */
	public void setSubItem(ISimpleCSSubItemObject subitem) {
		fSubItem = subitem;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (fSubItem != null) {
			// Determine parent type and remove accordingly 
			ISimpleCSObject parent = fSubItem.getParent();
			if (parent.getType() == ISimpleCSConstants.TYPE_ITEM) {
				ISimpleCSItem item = (ISimpleCSItem)parent;
				item.removeSubItem(fSubItem);
			} else if ((parent.getType() == ISimpleCSConstants.TYPE_REPEATED_SUBITEM) &&
					(fSubItem.getType() == ISimpleCSConstants.TYPE_SUBITEM)) {
				ISimpleCSRepeatedSubItem item = (ISimpleCSRepeatedSubItem)parent;
				item.setSubItem(null);
			} else if ((parent.getType() == ISimpleCSConstants.TYPE_CONDITIONAL_SUBITEM) &&
					(fSubItem.getType() == ISimpleCSConstants.TYPE_SUBITEM)) {
				ISimpleCSConditionalSubItem item = (ISimpleCSConditionalSubItem)parent;
				item.removeSubItem((ISimpleCSSubItem)fSubItem);
			}
		}
	}
}
