/*
 * Copyright (c)  Subhra Das Gupta
 *
 * This file is part of Xtream Download Manager.
 *
 * Xtream Download Manager is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Xtream Download Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with Xtream Download Manager; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package org.sdg.xdman.gui;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.TransferHandler;
import javax.swing.border.LineBorder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

public class DropBox extends JWindow {
	private static final long serialVersionUID = -6560446385567170000L;
	JLabel label;
	int relx, rely;
	DropListener listerner;

	public DropBox(final DropListener listener, ImageIcon icon, Dimension d) {
		setAlwaysOnTop(true);
		this.listerner = listener;
		setSize(icon.getIconWidth() + 5, icon.getIconHeight() + 5);
		setLocation(d.width - (getWidth() + 10), d.height - getHeight() - 100);
		label = new JLabel(icon);
		label.setBorder(new LineBorder(Color.GRAY, 1));
		label.setTransferHandler(new TransferHandler() {
			private static final long serialVersionUID = 8226815435490071235L;

			@Override
			public boolean canImport(JComponent comp,
					DataFlavor[] transferFlavors) {
				return true;
			}

			@Override
			public boolean importData(JComponent comp, Transferable t) {
				System.out.println("import");
				DataFlavor[] flavors = t.getTransferDataFlavors();
				if (flavors == null)
					return false;
				if (flavors.length < 1)
					return false;
				try {
					for (int i = 0; i < flavors.length; i++) {
						DataFlavor flavor = flavors[i];
						System.out.println(flavor.getMimeType());
						if (flavor.isFlavorTextType()) {
							String data = getData(t.getTransferData(flavor));
							System.out.println(data);
							try {
								new URL(data);
							} catch (Exception e) {
								continue;
							}
							if (listener != null) {
								listener.drop(data);
							}
							return true;
						}
					}
					return false;
				} catch (Exception e) {
					return false;
				}
			}
		});
		add(label);

		label.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent me) {
				int x = getX();
				int y = getY();
				setLocation(x + me.getX() - relx, y + me.getY() - rely);
			}
		});
		label.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent me) {
				relx = me.getX();
				rely = me.getY();
			}
		});

	}

	String getData(Object obj) {
		try {
			if (obj instanceof Reader) {
				Reader r = (Reader) obj;
				StringBuffer data = new StringBuffer();
				while (true) {
					int x = r.read();
					if (x == -1)
						break;
					data.append((char) x);
				}
				return data.toString();
			}
			if (obj instanceof InputStream) {
				InputStream r = (InputStream) obj;
				StringBuffer data = new StringBuffer();
				while (true) {
					int x = r.read();
					if (x == -1)
						break;
					data.append((char) x);
				}
				return data.toString();
			} else if (obj instanceof String) {
				return obj.toString();
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	public static void main(String[] args) {
		new DropBox(null, null, null).setVisible(true);
	}
}
