/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jacek Pospychala <jacek.pospychala@pl.ibm.com> - bug 202583
 *******************************************************************************/
package org.eclipse.ui.internal.views.log;

import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;

public class LogViewLabelProvider
	extends LabelProvider
	implements ITableLabelProvider {
	
	private static int MAX_LABEL_LENGTH = 200;
	
	private Image infoImage;
	private Image okImage;
	private Image errorImage;
	private Image warningImage;
	private Image errorWithStackImage;
	private Image hierarchicalImage;
	ArrayList consumers = new ArrayList();

	public LogViewLabelProvider() {
		errorImage = SharedImages.getImage(SharedImages.DESC_ERROR_ST_OBJ);
		warningImage = SharedImages.getImage(SharedImages.DESC_WARNING_ST_OBJ);
		infoImage = SharedImages.getImage(SharedImages.DESC_INFO_ST_OBJ);
		okImage = SharedImages.getImage(SharedImages.DESC_OK_ST_OBJ);
		errorWithStackImage = SharedImages.getImage(SharedImages.DESC_ERROR_STACK_OBJ);
		hierarchicalImage = SharedImages.getImage(SharedImages.DESC_HIERARCHICAL_LAYOUT_OBJ);
	}
	public void dispose() {
		if (consumers.size() == 0){
			super.dispose();
		}
	}
	public Image getColumnImage(Object element, int columnIndex) {
		if (element instanceof LogSession) {
			return (columnIndex == 0) ? hierarchicalImage : null;
		}
		
		LogEntry entry = (LogEntry) element;
		if (columnIndex == 0) {
			switch (entry.getSeverity()) {
				case IStatus.INFO :
					return infoImage;
				case IStatus.OK :
					return okImage;
				case IStatus.WARNING :
					return warningImage;
				default :
					return (entry.getStack() == null ? errorImage : errorWithStackImage);
			}
		}
		return null;
	}
	
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof LogSession) {
			LogSession entry = (LogSession) element;
			if (columnIndex == 0) {
				return Messages.LogViewLabelProvider_Session;
			} else if (columnIndex == 2) {
				if (entry.getDate() != null) {
					DateFormat formatter = new SimpleDateFormat(LogEntry.F_DATE_FORMAT);
					return formatter.format(entry.getDate()); 
				}
			}
			return null;
		}
		
		LogEntry entry = (LogEntry) element;
		switch (columnIndex) {
		case 0:
			if (entry.getMessage() != null) {
				String message = entry.getMessage();
				if (message.length() > MAX_LABEL_LENGTH) {
					String warning = Messages.LogViewLabelProvider_truncatedMessage;
					StringBuffer sb = new StringBuffer(message.substring(0, MAX_LABEL_LENGTH - warning.length()));
					sb.append(warning);
					return sb.toString();
				}
				return entry.getMessage();
			}
		case 1:
			if (entry.getPluginId() != null)
				return entry.getPluginId();
		case 2:
			return entry.getFormattedDate();
		}
		return ""; //$NON-NLS-1$
	}

	public void connect(Object consumer) {
		if (!consumers.contains(consumer))
			consumers.add(consumer);
	}
	
	public void disconnect(Object consumer) {
		consumers.remove(consumer);
		if (consumers.size() == 0) {
			dispose();
		}
	}
}
