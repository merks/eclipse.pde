/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.xhtml;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class TocReplaceTable {

	protected class TocReplaceEntry implements IAdaptable {
		private String fEntry;
		private String fLabel;
		private String fReplacement;
		private IFile fTocFile;
		private IFile fEntryFile;
		private TocReplaceEntry(String replaceString, String title, IFile parentFile) {
			fLabel = title;
			fEntry = replaceString;
			fTocFile = parentFile;
			if (fEntry != null && fTocFile != null)
				fEntryFile = fTocFile.getProject().getFile(fEntry);
		}
		public String getHref() {
			return fEntry;
		}
		public IFile getTocFile() {
			return fTocFile;
		}
		public String getLabel() {
			return fLabel;
		}
		public boolean fileMissing() {
			return fEntryFile == null || !fEntryFile.exists();
		}
		public IFile getOriginalFile() {
			return fEntryFile;
		}
		public Object getAdapter(Class adapter) {
			if (adapter == IWorkbenchAdapter.class)
				return new IWorkbenchAdapter() {
					public Object[] getChildren(Object o) {
						return null;
					}
					public ImageDescriptor getImageDescriptor(Object object) {
						if (fileMissing())
							return PDEPluginImages.DESC_ALERT_OBJ;
						return PDEPluginImages.DESC_DISCOVERY;
					}
					public String getLabel(Object o) {
						String label = TocReplaceEntry.this.getLabel();
						String href = TocReplaceEntry.this.getHref();
						return label == null ? href : href + " (" + label + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					public Object getParent(Object o) {
						return TocReplaceEntry.this.getTocFile();
					}
				};
			return null;
		}
		public void setReplacement(String string) {
			fReplacement = string;
		}
		public String getReplacement() {
			return fReplacement;
		}
	}
	
	private Hashtable fEntries = new Hashtable();
	
	public void addToTable(String href, String title, IFile tocFile) {
		TocReplaceEntry tro = new TocReplaceEntry(href, title, tocFile);
		if (tro.fileMissing())
			return; // ignore invalid entries
		if (fEntries.containsKey(tocFile)) {
			ArrayList tocList = (ArrayList)fEntries.get(tocFile);
			tocList.add(tro);
		} else {
			ArrayList tocList = new ArrayList();
			tocList.add(tro);
			fEntries.put(tocFile, tocList);
		}
	}
	
	public IFile[] getTocs() {
		Set keys = fEntries.keySet();
		Iterator it = keys.iterator();
		IFile[] files = new IFile[fEntries.size()];
		int i = 0;
		while (it.hasNext())
			files[i++] = (IFile)it.next();
		return files;
	}
	
	public TocReplaceEntry[] getToBeConverted(IFile file) {
		ArrayList list = (ArrayList)fEntries.get(file);
		return (TocReplaceEntry[]) list.toArray(new TocReplaceEntry[list.size()]);
	}

	public int numEntries() {
		return fEntries.size();
	}
	
	public void clear() {
		fEntries.clear();
	}

}
