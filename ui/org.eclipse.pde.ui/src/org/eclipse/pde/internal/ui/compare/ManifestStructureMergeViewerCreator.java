/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;
import org.eclipse.compare.structuremergeviewer.StructureDiffViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;

public class ManifestStructureMergeViewerCreator implements IViewerCreator {

	public Viewer createViewer(Composite parent, CompareConfiguration config) {
		StructureDiffViewer diffViewer = new StructureDiffViewer(parent, config);
		diffViewer.setStructureCreator(new ManifestStructureCreator());
		return diffViewer;
	}

}
