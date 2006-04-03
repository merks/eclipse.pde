/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.compare;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;

public class OrderedMatching extends AbstractMatching {

	public OrderedMatching() {
		super();
	}

	protected int orderedMath(XMLNode x, XMLNode y) {
		//assumes x and y have children
		Object[] xc = x.getChildren();
		Object[] yc = y.getChildren();

		ArrayList xc_elementsAL = new ArrayList();
		ArrayList xc_attrsAL = new ArrayList();

		ArrayList yc_elementsAL = new ArrayList();
		ArrayList yc_attrsAL = new ArrayList();

		//find attributes and elements and put them in xc_elementsAL and xc_attrsAL, respectively
		for (int i = 0; i < xc.length; i++) {
			XMLNode x_i = (XMLNode) xc[i];
			if (x_i.getXMLType().equals(XMLStructureCreator.TYPE_ELEMENT)) {
				xc_elementsAL.add(x_i);
			} else if (
				x_i.getXMLType().equals(XMLStructureCreator.TYPE_ATTRIBUTE)) {
				xc_attrsAL.add(x_i);
			}
		}

		//do the same for yc				
		for (int i = 0; i < yc.length; i++) {
			XMLNode y_i = (XMLNode) yc[i];
			if (y_i.getXMLType().equals(XMLStructureCreator.TYPE_ELEMENT)) {
				yc_elementsAL.add(y_i);
			} else if (
				y_i.getXMLType().equals(XMLStructureCreator.TYPE_ATTRIBUTE)) {
				yc_attrsAL.add(y_i);
			}
		}

		Object[] xc_elements = xc_elementsAL.toArray();
		Object[] yc_elements = yc_elementsAL.toArray();

		ArrayList DTMatching = new ArrayList();
		// Matching to be added to Entry in fDT_Matchings
		int distance = 0; //distance to be added to entry in fDT

		// perform unordered matching on attributes
		// this updates fDT and fDT_Matchings
		if (xc_attrsAL.size() > 0 || yc_attrsAL.size() > 0) {
			if (xc_attrsAL.size() == 0)
				distance += yc_attrsAL.size();
			else if (yc_attrsAL.size() == 0)
				distance += xc_attrsAL.size();
			else {
				distance = handleAttributes(xc_attrsAL, yc_attrsAL, DTMatching);
			}
		}

		distance = handleRangeDifferencer(
				xc_elements,
				yc_elements,
				DTMatching,
				distance);

		fDT[indexOfLN(x)][indexOfRN(y)]= distance;
		fDT_Matchings[indexOfLN(x)][indexOfRN(y)]= DTMatching;
		return distance;

	}

	/* matches two trees according to paper "X-Diff", p. 16 */
	public void match(XMLNode LeftTree, XMLNode RightTree, boolean rightTreeIsAncestor,
			IProgressMonitor monitor) throws InterruptedException {

		fNLeft = new Vector();
		//numbering LeftTree: Mapping nodes in LeftTree to numbers to be used as array indexes
		fNRight = new Vector();
		//numbering RightTree: Mapping nodes in RightTree to numbers to be used as array indexes
		numberNodes(LeftTree, fNLeft);
		numberNodes(RightTree, fNRight);
		fDT = new int[fNLeft.size()][fNRight.size()];
		fDT_Matchings = new ArrayList[fNLeft.size()][fNRight.size()];
		for (int i = 0; i < fDT.length; i++) {
			fDT[i] = new int[fNRight.size()];
			for (int j = 0; j < fDT[0].length; j++) {
				fDT[i][j] = NO_ENTRY;
			}
		}

		dist(LeftTree, RightTree);
		//		/* mark matchings on LeftTree and RightTree */
		fMatches = new Vector();
		if (!LeftTree.getSignature().equals(RightTree.getSignature())) {
			//matching is empty	
		} else {
			fMatches.add(new Match(LeftTree, RightTree));
			for (int i_M = 0; i_M < fMatches.size(); i_M++) {
				Match m = (Match) fMatches.elementAt(i_M);
				if (!isLeaf(m.fx) && !isLeaf(m.fy)) {
					if (fDT_Matchings[indexOfLN(m.fx)][indexOfRN(m.fy)] != null)
						fMatches.addAll(fDT_Matchings[indexOfLN(m.fx)][indexOfRN(m.fy)]);
				}
			}
		}
		//end of Step2
		/* Renumber Id of Nodes to follow Matches. Or for ancestor, copy over Id to ancestor */
		if (rightTreeIsAncestor) {
			for (ListIterator it_M = fMatches.listIterator(); it_M.hasNext();) {
				Match m = (Match) it_M.next();
				if (m.fx != null && m.fy != null)
					m.fy.setId(m.fx.getId());
			}
		} else {
			int newId = 0;
			for (ListIterator it_M = fMatches.listIterator(); it_M.hasNext(); newId++) {
				Match m = (Match) it_M.next();
				if (m.fx != null)
					m.fx.setId(Integer.toString(newId));
				if (m.fy != null)
					m.fy.setId(Integer.toString(newId));
			}
		}
	}

	public int handleAttributes(ArrayList xc_attrs, ArrayList yc_attrs, ArrayList DTMatching) {
		int distance = 0;
		x_for : for (
			Iterator iter_xc = xc_attrs.iterator(); iter_xc.hasNext();) {
			XMLNode x_attr = (XMLNode) iter_xc.next();
			String x_attr_name = x_attr.getName();
			for (Iterator iter_yc = yc_attrs.iterator(); iter_yc.hasNext();) {
				XMLNode y_attr = (XMLNode) iter_yc.next();
				if (y_attr.getName().equals(x_attr_name)) {
					if (!y_attr.getValue().equals(x_attr.getValue()))
						distance += 1;
					DTMatching.add(new Match(x_attr, y_attr));
					yc_attrs.remove(y_attr);
					continue x_for;
				}
			}
			DTMatching.add(new Match(x_attr, null));
			distance += 1;
		}

		for (Iterator iter_yc = yc_attrs.iterator(); iter_yc.hasNext();) {
			DTMatching.add(new Match(null, (XMLNode) iter_yc.next()));
			distance += 1;
		}

		return distance;
	}

	protected int handleXandYnotLeaves(XMLNode x, XMLNode y) {
		/* handle entries as ordered*/
		return orderedMath(x, y);
	}
}
