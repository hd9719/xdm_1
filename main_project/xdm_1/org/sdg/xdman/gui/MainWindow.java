
package org.sdg.xdman.gui;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.sdg.xdman.core.common.Authenticator;
import org.sdg.xdman.core.common.ConnectionManager;
import org.sdg.xdman.core.common.IXDMConstants;
import org.sdg.xdman.core.common.UnsupportedProtocolException;
import org.sdg.xdman.core.common.XDMConfig;
import org.sdg.xdman.core.common.http.XDMHttpClient;
import org.sdg.xdman.gui.FileTransferHandler.FileTransferable;
import org.sdg.xdman.proxy.RequestHandler;
import org.sdg.xdman.proxy.RequestIntercepter;
import org.sdg.xdman.proxy.XDMProxyServer;
import org.sdg.xdman.util.XDMUtil;
import org.sdg.xdman.util.win.RegUtil;

public class MainWindow extends JFrame implements ActionListener,
		TreeSelectionListener, Observer, IXDMQueue, Runnable,
		RequestIntercepter, DropListener, BatchDownloadListener,
		YoutubeMediaListener {
	int windowState;
	XDMProxyServer server;
	private static final long serialVersionUID = -3846059297444673997L;
	Box toolbar;
	JButton addurl, resume, pause, delete, option, restart, media, exit;
	JLabel closetree;
	JSplitPane split;
	JTree tree;
	JTable table;
	MainTableModel model = new MainTableModel();
	static DownloadList list = null;// new DownloadList();
	static String tempdir = System.getProperty("user.home");
	static String destdir = tempdir;
	static String appdir = tempdir;
	IDownloadListener dlistener;
	boolean stop = false;
	ConfigWindow cwin;
	static XDMConfig config;
	JMenuItem cat;
	Box statusBar;
	JToolBar browserBar;
	JLabel browser;
	JPanel content;
	JLabel catview, interceptview;
	MediaGrabberWindow mw;
	MediaTableModel mmodel;
	Toolkit t;
	JPopupMenu pop;
	PropDlg propDlg;
	DownloadListItem queuedItem;
	boolean queue;
	Thread scheduler;
	DownloadCompleteDialog completeDlg;
	ShutdownDlg sdlg;
	public static ImageIcon icon;
	boolean hasTray;
	AboutDlg abtDlg;
	Clipboard clipboard;
	RefreshLinkDlg rdlg;
	boolean haltPending = false;
	DropBox dropBox;
	HelpDialog view;
	BrowserIntDlg bint;
	HttpTableModel httpModel;
	HttpMonitorDlg httpDlg;
	BatchDownloadDlg batchDlg;
	BatchDlg bdlg;
	boolean proxyAttached = false;
	JLabel queueLabel;
	Icon qIcon;
	YoutubeGrabberDlg ytgdlg;

	public static String version = "Version: 3.01 Build 15 (June 09, 2013)";

	String updateURL = "http://xdm.sourceforge.net/update.php";
	String homeURL = "http://xdm.sourceforge.net/";

	public MainWindow(DownloadList list) {
		icon = getIcon("icon.png");
		MainWindow.list = list;
		this.dlistener = list;
		createWindow();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				shutdownHook();
			}
		});
		windowState = getState();
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (getState() != ICONIFIED) {
					windowState = getState();
					System.out.println("State ICONIFIED: "
							+ (windowState == ICONIFIED));
				}
			}
		});
		hasTray = createTrayIcon();
		// t = new Thread(this);
		// t.start();
	}

	void setList(DownloadList list, IDownloadListener l) {
		MainWindow.list = list;
		this.dlistener = l;
		model.setList(list);
	}

	void createWindow() {
		setTitle("seosh Download Manager");
		setIconImage(icon.getImage());
		content = new JPanel(new BorderLayout());
		browserBar = new JToolBar();
		add(browserBar, BorderLayout.SOUTH);
		t = Toolkit.getDefaultToolkit();
		Dimension d = t.getScreenSize();
		int w = 700;
		int h = 400;
		if (d.width < w)
			w = d.width;
		if (d.height < h)
			h = d.height;
		// setSize(d.width - d.width / 4, d.height - d.height / 3);
		setSize(w, h);
		setLocation(d.width / 2 - this.getWidth() / 2, d.height / 2
				- this.getHeight() / 2);
		createToolbar();
		createMenu();
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		split.setDividerLocation(getWidth() / 5);
		split.setDividerSize(2);
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		p.add(split);
		content.add(p);
		add(content);
		createTreePane();
		createTable();
		createBrowserBar();
	}

	JPanel p;

	void createTreePane() {
		p = new JPanel(new BorderLayout());
		Box b = Box.createHorizontalBox();
		b.add(Box.createHorizontalGlue());
		// b.add(Box.createRigidArea(new Dimension(10, 0)));
		JLabel top = new JLabel("Catagories", JLabel.CENTER);
		top.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
		b.add(top);
		closetree = new JLabel(getIcon("close.png"));
		closetree.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				hideCatagory();
				cat.setText("Show Catagory");
			}
		});
		b.add(Box.createHorizontalGlue());
		b.add(closetree);
		p.add(b, BorderLayout.NORTH);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Downloads");
		DefaultMutableTreeNode unfinished = new DefaultMutableTreeNode(
				"Unfinished");
		String types[] = { "Compressed", "Documents", "Music", "Programs",
				"Video" };
		DefaultMutableTreeNode all = new DefaultMutableTreeNode("All");
		for (int i = 0; i < types.length; i++) {
			all.add(new DefaultMutableTreeNode(types[i]));
		}
		root.add(all);
		for (int i = 0; i < types.length; i++) {
			unfinished.add(new DefaultMutableTreeNode(types[i]));
		}
		root.add(unfinished);
		DefaultMutableTreeNode finished = new DefaultMutableTreeNode("Finished");
		for (int i = 0; i < types.length; i++) {
			finished.add(new DefaultMutableTreeNode(types[i]));
		}
		root.add(finished);
		tree = new JTree(root);
		tree.addTreeSelectionListener(this);
		p.add(new JScrollPane(tree));
		split.add(p, JSplitPane.LEFT);
	}

	void createTable() {
		table = new JTable(model);
		table.setTransferHandler(new FileTransferHandler());
		table.setDragEnabled(true);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent me) {
				if (me.getButton() == MouseEvent.BUTTON3) {
					if (pop == null) {
						createPop();
					}
					pop.show(table, me.getX(), me.getY());
				}
			}
		});
		model.setList(list);
		// table.setDefaultRenderer(XDMTableCellRenderer.class,
		// new XDMTableCellRenderer());
		// table.setIntercellSpacing(new Dimension(0, 5));
		table.setAutoCreateRowSorter(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setShowGrid(false);
		TableColumnModel cm = table.getColumnModel();
		for (int i = 0; i < cm.getColumnCount(); i++) {
			TableColumn c = cm.getColumn(i);
			if (c.getHeaderValue().equals(""))
				c.setPreferredWidth(20);
			else if (c.getHeaderValue().equals("File Name"))
				c.setPreferredWidth(200);
			else if (c.getHeaderValue().equals("Q"))
				c.setPreferredWidth(50);
			else
				c.setPreferredWidth(100);
		}
		JScrollPane jsp = new JScrollPane(table);
		table.setFillsViewportHeight(true);
		split.add(jsp, JSplitPane.RIGHT);
	}

	void createBrowserBar() {
		browserBar.setFloatable(false);
		catview = new JLabel("[Downloads]");
		// browserBar = Box.createHorizontalBox();
		String status = mod_xdm ? "ON (" + config.port + ")" : "OFF";
		interceptview = new JLabel("Download Grabber: " + status);
		// browserBar.add(Box.createRigidArea(new Dimension(10, 0)));
		browser = new JLabel("No Browser Detected");

		browser.setToolTipText("Click Here for more information...");
		// Map<TextAttribute, Object> map = new HashMap<TextAttribute,
		// Object>();
		// map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		// Font font = none.getFont().deriveFont(map);
		// none.setForeground(Color.BLUE);
		// none.setFont(font);
		// browser.setHorizontalTextPosition(JLabel.LEADING);
		browser.setIcon(getIcon("hlp.png"));
		browser.setCursor(new Cursor(Cursor.HAND_CURSOR));
		// browserBar.add(new JLabel(getIcon("sep.png")));
		browserBar.add(Box.createRigidArea(new Dimension(10, 0)));
		browserBar.add(browser);
		browserBar.add(Box.createRigidArea(new Dimension(10, 0)));
		browserBar.add(new JLabel(getIcon("sep.png")));
		browserBar.add(Box.createRigidArea(new Dimension(10, 0)));
		browserBar.add(interceptview);
		browserBar.add(Box.createRigidArea(new Dimension(10, 0)));
		// browserBar.add(Box.createHorizontalGlue());
		browserBar.add(new JLabel(getIcon("sep.png")));
		browserBar.add(Box.createRigidArea(new Dimension(10, 0)));
		browserBar.add(catview);
		browserBar.add(Box.createRigidArea(new Dimension(10, 0)));
		browserBar.add(new JLabel(getIcon("sep.png")));
		browserBar.add(Box.createHorizontalGlue());
		browserBar.add(Box.createRigidArea(new Dimension(10, 0)));
		queueLabel = new JLabel();
		browserBar.add(queueLabel);
		browserBar.add(Box.createRigidArea(new Dimension(10, 0)));

		// menuBox.add(Box.createRigidArea(new Dimension(10, 20)));
		// menuBox.add(interceptview);
		// menuBox.add(Box.createHorizontalGlue());
		// menuBox.add(Box.createRigidArea(new Dimension(10, 20)));
		// menuBox.add(Box.createRigidArea(new Dimension(10, 20)));
		// menuBox.add(browserBar);
		// menuBox.add(Box.createRigidArea(new Dimension(10, 20)));
		browser.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				showBrowserStatus();
			}
		});
	}

	void createToolbar() {
		toolbar = Box.createHorizontalBox();// new JToolBar();
		Box p = Box.createHorizontalBox();// new JPanel(new GridLayout());
		addurl = new JButton("Add URL", getIcon("add.png"));
		addurl.setRolloverIcon(getIcon("add_r.png"));
		addurl.addActionListener(this);
		decorateButton(addurl);
		p.add(addurl);
		delete = new JButton("Delete", getIcon("remove.png"));
		delete.setRolloverIcon(getIcon("remove_r.png"));
		delete.addActionListener(this);
		decorateButton(delete);
		p.add(delete);
		resume = new JButton("Resume", getIcon("resume.png"));
		resume.setRolloverIcon(getIcon("resume_r.png"));
		resume.addActionListener(this);
		decorateButton(resume);
		p.add(resume);
		pause = new JButton("Pause", getIcon("pause.png"));
		pause.setRolloverIcon(getIcon("pause_r.png"));
		pause.addActionListener(this);
		decorateButton(pause);
		p.add(pause);
		restart = new JButton("Restart", getIcon("restart.png"));
		restart.setRolloverIcon(getIcon("restart_r.png"));
		restart.addActionListener(this);
		decorateButton(restart);
		p.add(restart);
		option = new JButton("Options", getIcon("settings.png"));
		option.setRolloverIcon(getIcon("settings_r.png"));
		option.addActionListener(this);
		decorateButton(option);
		p.add(option);
		media = new JButton("Grabber", getIcon("media.png"));
		media.setRolloverIcon(getIcon("media_r.png"));
		media.addActionListener(this);
		decorateButton(media);
		p.add(media);
		toolbar.add(p);
		exit = new JButton("Exit", getIcon("exit.png"));
		exit.setRolloverIcon(getIcon("exit_r.png"));
		exit.addActionListener(this);
		decorateButton(exit);
		p.add(exit);
		content.add(toolbar, BorderLayout.NORTH);
	}

	JMenuBar bar;

	void createMenu() {
		bar = new JMenuBar();
		bar.add(Box.createRigidArea(new Dimension(10, 0)));
		JMenu file = new JMenu("File");
		JMenuItem add = new JMenuItem("Add URL");
		add.addActionListener(this);
		file.add(add);
		JMenuItem ytgrab = new JMenuItem("YouTube Downloader");
		ytgrab.addActionListener(this);
		file.add(ytgrab);
		JMenuItem batchClip = new JMenuItem("Add from clipboard");
		batchClip.addActionListener(this);
		file.add(batchClip);
		JMenuItem batch = new JMenuItem("Batch download");
		batch.addActionListener(this);
		file.add(batch);
		JMenuItem grab = new JMenuItem("Grabber");
		grab.addActionListener(this);
		file.add(grab);
		JMenuItem del = new JMenuItem("Delete");
		del.addActionListener(this);
		file.add(del);
		JMenuItem delc = new JMenuItem("Delete Completed");
		delc.addActionListener(this);
		file.add(delc);
		JMenuItem exit = new JMenuItem("Exit");
		exit.addActionListener(this);
		file.add(exit);
		JMenu dwn = new JMenu("Download");
		dwn.addActionListener(this);
		JMenuItem pause = new JMenuItem("Pause");
		pause.addActionListener(this);
		dwn.add(pause);
		JMenuItem resume = new JMenuItem("Resume");
		resume.addActionListener(this);
		dwn.add(resume);
		JMenuItem restart = new JMenuItem("Restart");
		restart.addActionListener(this);
		dwn.add(restart);
		JMenuItem startQ = new JMenuItem("Start Queue");
		startQ.addActionListener(this);
		dwn.add(startQ);
		JMenuItem stopQ = new JMenuItem("Stop Queue");
		stopQ.addActionListener(this);
		dwn.add(stopQ);
		JMenu view = new JMenu("Tools");
		JMenuItem option = new JMenuItem("Options");
		option.addActionListener(this);
		view.add(option);
		JMenuItem prop = new JMenuItem("Properties");
		prop.addActionListener(this);
		view.add(prop);
		cat = new JMenuItem("Hide Catagory");
		view.add(cat);
		cat.addActionListener(this);
		JMenuItem refresh = new JMenuItem("Refresh Link");
		refresh.addActionListener(this);
		view.add(refresh);
		JMenuItem link = new JMenuItem("Make Shortcut");
		link.addActionListener(this);
		view.add(link);
		JMenuItem test = new JMenuItem("Test Browser");
		test.addActionListener(this);
		view.add(test);
		JMenuItem bint = new JMenuItem("Browser Integration");
		bint.addActionListener(this);
		view.add(bint);
		JMenuItem http = new JMenuItem("HTTP Monitor");
		http.addActionListener(this);
		view.add(http);
		JMenu help = new JMenu("help");
		JMenuItem hvid = new JMenuItem("Capturing videos");
		hvid.addActionListener(this);
		help.add(hvid);
		JMenuItem abi = new JMenuItem("Capturing downloads");
		abi.addActionListener(this);
		help.add(abi);
		JMenuItem hpg = new JMenuItem("XDM Home page");
		hpg.addActionListener(this);
		help.add(hpg);
		JMenuItem updt = new JMenuItem("Check for update");
		updt.addActionListener(this);
		help.add(updt);

		JMenuItem abt = new JMenuItem("About XDM");
		abt.addActionListener(this);
		help.add(abt);
		bar.add(file);
		bar.add(dwn);
		bar.add(view);
		bar.add(help);
		setJMenuBar(bar);
		// menuBox.add(bar);
		bar.add(Box.createHorizontalGlue());
		// this.add(new
		// JLabel("Browser: None (Configure/Restart your Browser)"),
		// BorderLayout.SOUTH);
	}

	static ImageIcon getIcon(String name) {
		try {
			return new ImageIcon(MainWindow.class.getResource("/res/" + name));
		} catch (Exception e) {
			return new ImageIcon("res/" + name);
		}
	}

	void shutdownHook() {
		System.out.println("ShutdownHook...");
		dlistener.downloadStateChanged();
		config.save();
		Authenticator.getInstance().save();
		System.out.println("Stopping server");
		server.stop();
		System.out.println("Stopping server...done");
		if (proxyAttached) {
			File bak = new File(System.getProperty("user.home"), "reg-bak.reg");
			RegUtil.restore(bak);
			attachProxy(true);// runWPAC(false);
		}
	}

	void decorateButton(JButton b) {
		b.setBorderPainted(false);
		b.setFocusPainted(false);
		b.setContentAreaFilled(false);
		b.setHorizontalTextPosition(JButton.CENTER);
		b.setVerticalTextPosition(JButton.BOTTOM);
	}

	DownloadListItem getSelectedItem() {
		int index = table.getSelectedRow();
		if (index < 0) {
			return null;
		}
		index = table.convertRowIndexToModel(index);
		return list.get(index);
	}

	int getSelectedItemIndex() {
		int index = table.getSelectedRow();
		if (index < 0) {
			return -1;
		}
		index = table.convertRowIndexToModel(index);
		return index;
	}

	public void actionPerformed(ActionEvent e) {
		String str = e.getActionCommand();
		if (str.equals("Add URL")) {
			addURL(null, null, null, null);
		}
		if (str.equals("Download Later")) {
			DownloadFileInfoDialog dfi = (DownloadFileInfoDialog) e.getSource();
			if (dfi.interceptor != null) {
				((RequestHandler) dfi.interceptor).intercept = true;
				synchronized (dfi) {
					dfi.notifyAll();
				}
			}
			String url = dfi.getURL();
			String user = dfi.getUser();
			String pass = dfi.getPass();
			HashMap<String, String> extra = dfi.extra;
			if (user.length() > 0) {
				try {
					if (extra == null) {
						extra = new HashMap<String, String>();
					}
					extra.put("USER", user);
					extra.put("PASS", pass);
				} catch (Exception err) {
				}
			}
			// addDownload(url, file, tempdir, list, dfi.client, extra,
			// dfi.cookies);
			DownloadListItem item = new DownloadListItem();
			item.dateadded = new Date().toString();
			item.lasttry = new Date().toString();
			item.q = true;
			item.url = url;
			item.extra = extra;
			item.filename = dfi.getFile();// XDMUtil.getFileName(url);
			item.icon = IconUtil.getIcon(XDMUtil.findCategory(item.filename));
			item.saveto = dfi.getDir();// destdir;
			item.cookies = dfi.cookies;
			list.add(item);
			model.fireTableDataChanged();
		}
		if (str.equals("Download Now")) {
			DownloadFileInfoDialog dfi = (DownloadFileInfoDialog) e.getSource();
			if (dfi.interceptor != null) {
				((RequestHandler) dfi.interceptor).intercept = true;
				synchronized (dfi) {
					dfi.notifyAll();
				}
			}
			String url = dfi.getURL();
			String file = dfi.getFile();
			String dir = dfi.getDir();
			String user = dfi.getUser();
			String pass = dfi.getPass();
			HashMap<String, String> extra = dfi.extra;
			if (user.length() > 0) {
				try {
					if (extra == null) {
						extra = new HashMap<String, String>();
					}
					extra.put("USER", user);
					extra.put("PASS", pass);
				} catch (Exception err) {
				}
			}
			addDownload(url, file, dir, tempdir, list, dfi.client, extra,
					dfi.cookies);
		}
		if (str.equals("Delete")) {
			removeDownloads();
		}
		if (str.equals("Resume")) {
			DownloadListItem item = getSelectedItem();
			if (item == null) {
				JOptionPane.showMessageDialog(this, "No item selected");
				return;
			}

			System.out.println(item.state);
			// if (item.state == IXDMConstants.FAILED
			// || item.state == IXDMConstants.STOPPED) {
			if (item.mgr == null && item.state != IXDMConstants.COMPLETE) {
				item.lasttry = new Date().toString();
				if (item.tempdir.equals("")) {
					startDownload(item.url, item.filename, item.saveto,
							tempdir, item, null, item.extra, item.cookies);
				} else {
					ConnectionManager c = new ConnectionManager(item.url,
							item.filename, item.saveto, item.tempdir,
							item.extra, config);
					c.setTimeOut(config.timeout);
					c.setMaxConn(config.maxConn);
					if (config.showDownloadPrgDlg) {
						DownloadWindow w = new DownloadWindow(c);
						c.addObserver(w);
						w.showWindow();
						item.window = w.window;
					}
					item.setCallback(c, model, dlistener);
					item.addObserver(this);
					c.resume();
				}
			} else {
				JOptionPane.showMessageDialog(this,
						"Download is currently active or finished");
			}
		}
		if (str.equals("Restart")) {
			DownloadListItem item = getSelectedItem();
			if (item == null) {
				JOptionPane.showMessageDialog(this, "No item selected");
				return;
			}
			System.out.println(item.state);
			if (item.mgr == null) {
				startDownload(item.url, item.filename, item.saveto, tempdir,
						item, null, item.extra, item.cookies);
			} else {
				JOptionPane.showMessageDialog(this,
						"Download is currently active");
			}
		}
		if (str.equals("Pause")) {
			DownloadListItem item = getSelectedItem();
			if (item == null) {
				JOptionPane.showMessageDialog(this, "No item selected");
				return;
			}
			if (item.mgr != null) {
				item.mgr.stop();
			} else {
				JOptionPane.showMessageDialog(this, "Download is not active.");
			}
		}
		if (str.equals("Options")) {
			if (cwin == null) {
				cwin = new ConfigWindow(config, this);
			}
			cwin.setLocationRelativeTo(this);
			cwin.showDialog();
		}
		if (str.equals("Show Catagory")) {
			showCatagory();
			cat.setText("Hide Catagory");
		}
		if (str.equals("Hide Catagory")) {
			hideCatagory();
			cat.setText("Show Catagory");
		}
		if (str.equals("Grabber")) {
			if (mw == null) {
				mw = new MediaGrabberWindow(mmodel, this);
			}
			mw.setLocationRelativeTo(this);
			mw.setVisible(true);
		}
		if (str.equals("Properties")) {
			showProp();
		}
		if (str.equals("Open")) {
			openFile();
		}
		if (str.equals("Open Folder")) {
			openFolder();
		}
		if (str.equals("Firefox Integration")) {
			showFFDlg();
		}
		if (str.equals("Browser Integration")) {
			showBrowserIntegrationDlg();
		}
		if (str.equals("Show/Hide Window")) {
			System.out.println("SHOW?HIDE WINDOW");
			showDownloadWindow();
		}
		if (str.equals("Start Queue")) {
			if (queue) {
				JOptionPane.showMessageDialog(this, "Queue already started");
				return;
			}
			startQ();
		}
		if (str.equals("Stop Queue")) {
			if (!queue) {
				JOptionPane.showMessageDialog(this, "Queue already stopped");
				return;
			}
			stopQ();
		}
		if (str.equals("Exit")) {
			System.exit(0);
		}
		if (str.equals("About XDM")) {
			showAboutDialog();
		}
		if (str.equals("Restore")) {
			restore();
		}
		if (str.equals("Delete Completed")) {
			removeCompleteDownloads();
		}
		if (str.equals("Copy URL")) {
			DownloadListItem item = getSelectedItem();
			if (item != null)
				copyURL(item.url);
		}
		if (str.equals("Copy File")) {
			copyPath();
		}
		if (str.equals("Make Shortcut")) {
			if (File.separatorChar == '\\') {
				winCreateLink();
			} else {
				linuxCreateLink();
			}
		}

		if (str.equals("Refresh Link")) {
			DownloadListItem item = getSelectedItem();
			String ref;
			if (item != null) {
				HashMap<String, String> hm = item.extra;
				if (hm == null)
					ref = "";
				else {
					ref = hm.get("referer");
				}
				if (rdlg == null) {
					rdlg = new RefreshLinkDlg(this);
				}
				rdlg.setLocationRelativeTo(this);
				rdlg.showDlg(item, ref);
			} else {
				JOptionPane.showMessageDialog(this, "No item Selected.");
			}
		}
		if (str.equals("Test Browser")) {
			testBrowser();
		}
		if (str.equals("Copy")) {
			copyURL("http://localhost:9614/test");
		}
		if (str.equals("Advanced Browser Integration")) {
			showInfo();
		}
		if (str.equals("Capturing downloads")) {
			try {

				if (view == null) {
					view = getHTMLViwer();
				}
				view.setDocument(getClass().getResource(
						"/help/browser_integration.html"));
				view.setVisible(true);
				// JOptionPane.showMessageDialog(this,
				// "Type this address in your browser:\n"
				// + "http://localhost:" + config.port
				// + "/help/index.html");

			} catch (Exception err) {
				err.printStackTrace();
			}
		}
		if (str.equals("Capturing videos")) {
			try {
				if (view == null) {
					view = getHTMLViwer();
				}
				view.setDocument(getClass().getResource(
						"/help/video_download.html"));
				view.setVisible(true);

			} catch (Exception err) {
				err.printStackTrace();
			}
		}
		if (str.equals("Auto Configure")) {
			showInfo();
		}
		if (str.equals("Run XDM on startup")) {
			JCheckBox chk = (JCheckBox) e.getSource();
			setAutoStart(chk.isSelected());
			System.out.println(chk.isSelected());
		}
		if (str.equals("Manual Configure")) {
			try {
				if (view == null) {
					view = getHTMLViwer();
				}
				view.setDocument(getClass().getResource(
						"/help/browser_integration.html"));
				view.setVisible(true);

			} catch (Exception err) {
				err.printStackTrace();
			}
		}
		if (str.equals("HTTP Monitor")) {
			if (httpDlg == null) {
				httpDlg = new HttpMonitorDlg(httpModel);
				httpDlg.setSize(this.getSize());
			}
			httpDlg.setLocation(this.getLocation());
			httpDlg.setVisible(true);
		}
		if (str.equals("Save As")) {
			DownloadListItem item = getSelectedItem();
			if (item == null) {
				JOptionPane.showMessageDialog(this, "No Item Selected.");
				return;
			}
			if (item.state == IXDMConstants.COMPLETE) {
				JOptionPane.showMessageDialog(this,
						"Can't change completed downloads");
				return;
			}
			if (fc == null) {
				fc = new JFileChooser();
			}
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setSelectedFile(new File(item.saveto, item.filename));
			if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				if (item.state == IXDMConstants.COMPLETE
						|| item.state == IXDMConstants.ASSEMBLING) {
					JOptionPane
							.showMessageDialog(this,
									"Can't change download property while it is almost complete");
					return;
				}
				item.saveto = fc.getSelectedFile().getParent();
				item.filename = fc.getSelectedFile().getName();
				if (item.mgr != null) {
					item.mgr.setDestdir(item.saveto);
					item.mgr.setFileName(item.filename);
				}
				// model.fireTableDataChanged();
				list.downloadStateChanged();
			}
		}
		if (str.equals("Add from clipboard")) {
			System.out.println("$$$$$$$$$");
			if (batchDlg == null) {
				batchDlg = new BatchDownloadDlg();
			}
			batchDlg.setLocationRelativeTo(null);
			batchDlg.showDialog(config.destdir, this);
		}
		if (str.equals("Batch download")) {
			if (bdlg == null) {
				bdlg = new BatchDlg();
			}
			bdlg.setLocationRelativeTo(null);
			bdlg.showDialog(this);// batchDlg.showDialog(config.destdir, this);
		}
		if (str.equals("YouTube Downloader")) {
			if (ytgdlg == null) {
				ytgdlg = new YoutubeGrabberDlg(this);
			}
			ytgdlg.setLocationRelativeTo(null);
			ytgdlg.showDialog(this, config);
		}
		if (str.equals("Check for update")) {
			if (Desktop.isDesktopSupported()) {
				Desktop d = Desktop.getDesktop();
				try {
					d.browse(new URI(updateURL));
				} catch (Exception ex) {
				}
			}
		}
		if (str.equals("XDM Home page")) {
			if (Desktop.isDesktopSupported()) {
				Desktop d = Desktop.getDesktop();
				try {
					d.browse(new URI(homeURL));
				} catch (Exception ex) {
				}
			}
		}
	}

	void restore() {
		setState(windowState);
		setVisible(true);
	}

	void addURL(String url, XDMHttpClient client, HashMap<String, String> map,
			String cookies) {
		addURL(url, client, map, cookies, null);
	}

	void addURL(String url, XDMHttpClient client, HashMap<String, String> map,
			String cookies, Object invoker) {
		System.out.println("Called");
		DownloadFileInfoDialog dlg = new DownloadFileInfoDialog(this, this,
				config);
		dlg.setURL(url);
		dlg.setDir(config.destdir);
		dlg.client = client;
		dlg.extra = map;
		dlg.cookies = cookies;
		dlg.setAlwaysOnTop(true);
		dlg.showDlg();
		if (config.allowbrowser) {
			if (invoker != null) {
				dlg.interceptor = invoker;
				synchronized (dlg) {
					try {
						dlg.wait();
						System.out.println("Returned Intercept? "
								+ ((RequestHandler) invoker).intercept);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			if (invoker != null) {
				((RequestHandler) invoker).intercept = true;
			}
		}
	}

	void addDownload(String url, String fileName, String destdir,
			String tempdir, DownloadList lst, XDMHttpClient client,
			HashMap<String, String> extra, String cookies) {
		DownloadListItem item = new DownloadListItem();
		item.dateadded = new Date().toString();
		item.lasttry = new Date().toString();
		item.url = url;
		item.extra = extra;
		item.filename = fileName;// XDMUtil.getFileName(url);
		item.icon = IconUtil.getIcon(XDMUtil.findCategory(item.filename));
		item.saveto = destdir;
		item.cookies = cookies;
		list.add(item);
		startDownload(url, fileName, destdir, tempdir, item, client,
				item.extra, cookies);
	}

	void startDownload(String url, String fileName, String destdir,
			String tempdir, DownloadListItem item, XDMHttpClient client,
			HashMap<String, String> extra, String cookies) {
		startDownload(url, fileName, destdir, tempdir, item, client, extra,
				cookies, true);
	}

	void startDownload(String url, String fileName, String destdir,
			String tempdir, DownloadListItem item, XDMHttpClient client,
			HashMap<String, String> extra, String cookies, boolean fg) {
		ConnectionManager c = new ConnectionManager(url, fileName, destdir,
				tempdir, extra, config);
		c.extra = extra;
		c.setTimeOut(config.timeout);
		c.setMaxConn(config.maxConn);
		if (config.showDownloadPrgDlg && fg) {
			DownloadWindow w = new DownloadWindow(c);
			c.addObserver(w);
			item.window = w.window;
			w.showWindow();
		}
		item.setCallback(c, model, dlistener);
		item.addObserver(this);
		model.fireTableDataChanged();
		dlistener.downloadStateChanged();
		if (client == null) {
			try {
				c.start();
			} catch (UnsupportedProtocolException e) {
				JOptionPane.showMessageDialog(this, "Unsupported protocol");
			}
		} else {
			try {
				c.start(client);
			} catch (UnsupportedProtocolException e) {
				JOptionPane.showMessageDialog(this, "Unsupported protocol");
			}
		}
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		// TODO Auto-generated method stub
		System.out.println(e.getPath());
		String status = null;
		int state = 0;
		Object o[] = e.getPath().getPath();
		for (int i = 0; i < o.length; i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) o[i];
			System.out.println(node.toString());
			if (node.toString().equalsIgnoreCase("unfinished")) {
				state = 1;
			}
			if (node.toString().equalsIgnoreCase("finished")) {
				state = IXDMConstants.COMPLETE;
			}
			if (node.toString().equalsIgnoreCase("documents")) {
				status = IXDMConstants.DOCUMENTS;
			}
			if (node.toString().equalsIgnoreCase("compressed")) {
				status = IXDMConstants.COMPRESSED;
			}
			if (node.toString().equalsIgnoreCase("music")) {
				status = IXDMConstants.MUSIC;
			}
			if (node.toString().equalsIgnoreCase("programs")) {
				status = IXDMConstants.PROGRAMS;
			}
			if (node.toString().equalsIgnoreCase("video")) {
				status = IXDMConstants.VIDEO;
			}
			list.setState(state);
			list.setType(status);
			catview.setText(e.getPath().toString());
			model.fireTableDataChanged();
			System.out.println("New State: " + state + " " + status);
		}
	}

	void removeDownload() {
		DownloadListItem item = getSelectedItem();
		if (item == null) {
			JOptionPane.showMessageDialog(this, "No item selected");
			return;
		}
		if (item.mgr != null) {
			JOptionPane
					.showMessageDialog(this,
							"The download is currently active.\nPlease stop the download then try again.");
			return;
		}
		list.remove(item);
		model.fireTableDataChanged();
		dlistener.downloadStateChanged();
	}

	void removeDownloads() {
		int count = table.getSelectedRowCount();
		System.out.println("Selected: " + count);
		if (count > 0) {
			DownloadListItem item[] = new DownloadListItem[count];
			int index[] = table.getSelectedRows();
			for (int i = 0; i < index.length; i++) {
				index[i] = table.convertRowIndexToModel(index[i]);
				item[i] = list.get(index[i]);
			}
			for (int i = 0; i < item.length; i++) {
				boolean del = true;
				if (item[i].mgr == null) {
					list.remove(item[i]);
					String td = item[i].tempdir;
					List<DownloadListItem> items = list.list;
					if (items != null) {
						for (int k = 0; k < items.size(); k++) {
							if (items.get(k).tempdir.equals(td)) {
								del = false;
								break;
							}
						}
					}
					if (del) {
						System.out.println("Deleting " + td + "...");
						File dir = new File(td);
						if (dir.exists()) {
							File files[] = dir.listFiles();
							for (int c = 0; c < files.length; c++) {
								System.out.println("Deleting " + files[c] + " "
										+ files[c].delete());
							}
						}
						System.out.println("Deleting " + dir + " "
								+ dir.delete());
					} else {
						System.out.println("Not deleting ref exists");
					}
				}
			}
			model.fireTableDataChanged();
		}
	}

	void removeCompleteDownloads() {
		List<DownloadListItem> lst = list.list;
		List<DownloadListItem> rl = new LinkedList<DownloadListItem>();
		for (int i = 0; i < lst.size(); i++) {
			DownloadListItem item = lst.get(i);
			if (item.state == IXDMConstants.COMPLETE)
				rl.add(lst.get(i));
		}
		for (int i = 0; i < rl.size(); i++) {
			lst.remove(rl.get(i));
		}
		list.downloadStateChanged();
		model.fireTableDataChanged();
	}

	private void hideCatagory() {
		split.remove(p);
		validate();
		list.setState(0);
		list.setType(null);
		model.fireTableDataChanged();
	}

	private void showCatagory() {
		split.add(p);
		validate();
		list.setState(0);
		list.setType(null);
		model.fireTableDataChanged();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void update(Observable o, Object obj) {
		if (o == config) {
			if (config.showDownloadBox) {
				if (dropBox == null) {
					dropBox = new DropBox(this, icon, t.getScreenSize());
				}
				dropBox.setVisible(true);
			} else {
				if (dropBox != null) {
					dropBox.setVisible(false);
				}
			}
			System.out.println("Config updated...");
			if (config.tempdir != null)
				if (config.tempdir.length() > 0) {
					if (new File(config.tempdir).exists()) {
						MainWindow.tempdir = config.tempdir;
					}
				}
			System.out.println("M_DESTDIR: " + destdir + "\nTEMpDIR: "
					+ tempdir);
			System.out.println("C_DESTDIR: " + config.destdir + "\nTEMpDIR: "
					+ config.tempdir);
			if (config.destdir != null)
				if (config.destdir.length() > 0) {
					if (new File(config.destdir).exists()) {
						MainWindow.destdir = config.destdir;
					}
				}

			System.out.println("DESTDIR: " + destdir + "\nTEMpDIR: " + tempdir);
			if (config.schedule) {
				if (scheduler == null) {
					scheduler = new Thread(this);
					scheduler.start();
					return;
				}
				if (!scheduler.isAlive()) {
					scheduler = new Thread(this);
					scheduler.start();
				}
			}
		}
		if (obj instanceof HashMap) {
			HashMap<String, Object> ht = (HashMap<String, Object>) obj;
			addURL((String) ht.get("URL"), null, (HashMap<String, String>) ht
					.get("HT"), (String) ht.get("COOKIES"));
		}
		if (obj instanceof HashSet) {
			HashSet<String> hs = (HashSet<String>) obj;
			Iterator<String> it = hs.iterator();
			System.out.println(hs + " " + hs.size());
			String txt = "Browser: ";
			if (hs.size() < 3) {
				if (hs.size() == 1)
					txt += it.next();
				if (hs.size() == 2) {
					txt = it.next() + " , " + it.next();
				}
			} else {
				txt += it.next() + " , " + it.next() + " & " + (hs.size() - 2)
						+ " More";
			}
			browser.setText(txt);
			browser.setIcon(null);
		}
		if (o instanceof DownloadListItem) {
			DownloadListItem item = (DownloadListItem) o;
			if (item.state == IXDMConstants.COMPLETE) {
				System.out.println("COMPLETE CALLBACK");
				if (queue) {
					next();
				} else if (config.showDownloadCompleteDlg) {
					if (completeDlg == null) {
						completeDlg = new DownloadCompleteDialog();
						completeDlg.setLocationRelativeTo(null);
						completeDlg.setAlwaysOnTop(true);
					}
					completeDlg.setData(item.filename, item.url);
					completeDlg.file_path = item.filename;
					completeDlg.folder_path = item.saveto;
					completeDlg.setVisible(true);
				}
				if (config.halt) {
					if (config.haltTxt == null || config.haltTxt.length() < 1)
						return;
					if (allFinished())
						shutdownComputer(config.haltTxt);
					else
						haltPending = true;
				}
				File file = new File(item.saveto, item.filename);
				if (config.executeCmd) {
					executeCommand(config.cmdTxt + " " + file);
				}
				if (config.hungUp) {
					hungUp(config.hungUpTxt);
				}
				if (config.antivir) {
					virusScan(config.antivirTxt + " " + file);
				}
			}
			if (item.state == IXDMConstants.FAILED) {
				System.out.println("FAILED CALLBACK");
				if (queue) {
					item.q = false;
					next();
				}
			}
		}
	}

	private boolean allFinished() {
		for (int i = 0; i < list.list.size(); i++) {
			int state = list.list.get(i).state;
			if (state != IXDMConstants.STOPPED
					|| state != IXDMConstants.COMPLETE
					|| state != IXDMConstants.FAILED) {
				return false;
			}
		}
		return true;
	}

	public void setConfig(XDMConfig c) {
		System.out.println("Setting config");
		config = c;
		if (config == null) {
			config = new XDMConfig(new File(appdir, ".xdmconf"));
		}
		if (config.showDownloadBox) {
			if (dropBox == null) {
				dropBox = new DropBox(this, icon, t.getScreenSize());
			}
			dropBox.setVisible(true);
		}
		if (config.tempdir == null || config.tempdir.length() < 1) {
			config.tempdir = tempdir;
		}
		if (config.destdir == null || config.destdir.length() < 1) {
			config.destdir = destdir;
		}
		System.out.println(config);
		if (config.tempdir != null)
			if (config.tempdir.length() > 0) {
				if (new File(config.tempdir).exists()) {
					MainWindow.tempdir = config.tempdir;
				}
			}
		if (config.destdir != null)
			if (config.destdir.length() > 0) {
				if (new File(config.destdir).exists()) {
					MainWindow.destdir = config.destdir;
				}
			}
		config.addObserver(this);
		if (config.schedule) {
			if (scheduler == null) {
				scheduler = new Thread(this);
				scheduler.start();
				return;
			}
			if (!scheduler.isAlive()) {
				scheduler = new Thread(this);
				scheduler.start();
			}
		}
		// System.out.println("HASH CODE: "+config.hashCode());
	}

	void createPop() {
		pop = new JPopupMenu();
		JMenuItem m1 = new JMenuItem("Open");
		m1.addActionListener(this);
		pop.add(m1);
		JMenuItem m2 = new JMenuItem("Open Folder");
		m2.addActionListener(this);
		pop.add(m2);
		JMenuItem m32 = new JMenuItem("Save As");
		m32.addActionListener(this);
		pop.add(m32);
		pop.addSeparator();
		JMenuItem m16 = new JMenuItem("Show/Hide Window");
		m16.addActionListener(this);
		pop.add(m16);
		pop.addSeparator();
		JMenuItem m3 = new JMenuItem("Pause");
		m3.addActionListener(this);
		pop.add(m3);
		JMenuItem m4 = new JMenuItem("Resume");
		m4.addActionListener(this);
		pop.add(m4);
		JMenuItem m5 = new JMenuItem("Restart");
		m5.addActionListener(this);
		pop.add(m5);
		JMenuItem m6 = new JMenuItem("Remove");
		m6.addActionListener(this);
		pop.add(m6);
		JMenuItem m = new JMenuItem("Refresh Link");
		m.addActionListener(this);
		pop.add(m);
		pop.addSeparator();
		JMenuItem m7 = new JMenuItem("Copy URL");
		m7.addActionListener(this);
		pop.add(m7);
		JMenuItem m8 = new JMenuItem("Copy File");
		m8.addActionListener(this);
		pop.add(m8);
		pop.addSeparator();
		JMenuItem m9 = new JMenuItem("Properties");
		m9.addActionListener(this);
		pop.add(m9);
		pop.setInvoker(pop);
	}

	void showProp() {
		DownloadListItem item = getSelectedItem();
		if (item != null) {
			if (propDlg == null) {
				propDlg = new PropDlg(this);
				propDlg.pack();
			}
			propDlg.setLocationRelativeTo(this);
			propDlg.setDownloadProperty(item);
			propDlg.setVisible(true);
		} else {
			JOptionPane.showMessageDialog(this, "No item selected");
		}
	}

	private void openFile() {
		// TODO Auto-generated method stub
		DownloadListItem item = getSelectedItem();
		if (item != null) {
			if (item.state == IXDMConstants.COMPLETE) {
				File file = new File(item.saveto, item.filename);
				if (file.exists()) {
					XDMUtil.open(file);
				} else {
					JOptionPane.showMessageDialog(this, "File does not exists");
				}
			} else {
				JOptionPane.showMessageDialog(this, "Download not complete");
			}
			// System.out.println(item.filename);
			// XDMUtil.open(item.)
		} else {
			JOptionPane.showMessageDialog(this, "No item selected");
		}
	}

	private void openFolder() {
		// TODO Auto-generated method stub
		DownloadListItem item = getSelectedItem();
		if (item != null) {
			File file = new File(item.saveto);
			if (file.exists()) {
				XDMUtil.open(file);
			} else {
				JOptionPane.showMessageDialog(this, "Folder does not exists");
			}
		} else {
			JOptionPane.showMessageDialog(this, "No item selected");
		}
	}

	void showDownloadWindow() {
		System.out.println("CALLED");
		DownloadListItem item = getSelectedItem();
		if (item == null) {
			JOptionPane.showMessageDialog(this, "No item selected");
			return;
		}
		if (item.window != null) {
			boolean show = !item.window.isVisible();
			System.out.println("SHOW WINDOW: " + show);
			item.window.setVisible(show);
		}
		if (item.window == null) {
			if (item.mgr != null) {
				DownloadWindow w = new DownloadWindow(item.mgr);
				item.mgr.addObserver(w);
				item.window = w.window;
				w.showWindow();
			}
		}
	}

	@Override
	public void startQ() {
		// TODO Auto-generated method stub
		if (!queue) {
			queue = true;
			next();
		}
	}

	@Override
	public void stopQ() {
		// TODO Auto-generated method stub
		queue = false;
		if (queuedItem != null) {
			if (queuedItem.mgr != null) {
				queuedItem.mgr.stop();
				queuedItem = null;
			}
		}
		queueLabel.setIcon(null);
	}

	@Override
	public void next() {
		// TODO Auto-generated method stub
		for (int i = 0; i < list.list.size(); i++) {
			DownloadListItem item = list.list.get(i);
			if (item.q) {
				if (item.mgr == null) {
					if (item.state != IXDMConstants.COMPLETE) {
						if (item.tempdir.equals("")) {
							startDownload(item.url, item.filename, item.saveto,
									tempdir, item, null, item.extra,
									item.cookies, false);
						} else {
							ConnectionManager c = new ConnectionManager(
									item.url, item.filename, item.saveto,
									item.tempdir, item.extra, config);
							c.setTimeOut(config.timeout);
							c.setMaxConn(config.maxConn);
							item.setCallback(c, model, dlistener);
							item.addObserver(this);
							c.resume();
						}
						queuedItem = item;
						if (qIcon == null) {
							qIcon = getIcon("icon16.png");
						}
						queueLabel.setIcon(qIcon);
						return;
					}
				}
			}
		}
		queue = false;
		queuedItem = null;
		queueLabel.setIcon(null);
	}

	@Override
	public void run() {
		while (config.schedule) {
			System.out.println("Scheduler running...");
			long now = System.currentTimeMillis();
			if (config.startDate != null && config.endDate != null) {
				if (now > config.startDate.getTime()) {
					if (now < config.endDate.getTime())
						if (!queue)
							startQ();
				} else {
					System.out.println("Date error " + "Now: " + now
							+ " START: " + config.startDate.getTime()
							+ " END: " + config.endDate.getTime()
							+ (now > config.startDate.getTime()) + " "
							+ (now < config.endDate.getTime()));
				}
				if (config.endDate.getTime() < now) {
					stopQ();
					break;
				}
			} else {
				System.out.println("Dates are null");
			}
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
		}
		System.out.println("Scheduler finished...");
	}

	void executeUserRequests() {
	}

	void shutdownComputer(String cmd) {
		List<DownloadListItem> items = list.list;
		for (int i = 0; i < items.size(); i++) {
			DownloadListItem item = list.get(i);
			if (item.state == IXDMConstants.CONNECTING
					|| item.state == IXDMConstants.DOWNLOADING
					|| item.state == IXDMConstants.ASSEMBLING) {
				return;
			}
		}
		if (sdlg == null)
			sdlg = new ShutdownDlg();
		sdlg.start(cmd);
	}

	PopupMenu trayPop;
	boolean showInfo;

	SystemTray tray;
	TrayIcon trayIcon;

	boolean createTrayIcon() {
		if (SystemTray.isSupported()) {
			tray = SystemTray.getSystemTray();
			trayIcon = new TrayIcon(icon.getImage(), "Xtreme Download Manager");
			trayIcon.setImageAutoSize(true);
			trayPop = new PopupMenu();
			MenuItem newDownload = new MenuItem("Add URL");
			newDownload.addActionListener(this);
			MenuItem aboutXDM = new MenuItem("About XDM");
			aboutXDM.addActionListener(this);
			MenuItem restore = new MenuItem("Restore");
			restore.addActionListener(this);
			MenuItem showDropBox = new MenuItem("Show/Hide Drop Box");
			showDropBox.addActionListener(this);
			MenuItem exit2 = new MenuItem("Exit");
			exit2.addActionListener(this);
			trayPop.add(newDownload);
			trayPop.add(aboutXDM);
			trayPop.add(restore);
			trayPop.add(showDropBox);
			trayPop.add(exit2);
			trayIcon.setPopupMenu(trayPop);
			trayIcon.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (mw == null) {
						mw = new MediaGrabberWindow(mmodel, MainWindow.this);
					}
					Dimension dim = t.getScreenSize();
					mw.setLocation(dim.width / 2 - mw.getWidth() / 2,
							dim.height / 2 - mw.getHeight() / 2);
					mw.setVisible(true);
				}
			});
			trayIcon.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1) {
						restore();
					}
				}
			});
			try {
				tray.add(trayIcon);
				return true;
			} catch (AWTException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public void intercept(Object obj, Object invoker) {
		if (obj instanceof XDMHttpClient) {
			XDMHttpClient client = (XDMHttpClient) obj;
			addURL(client.url.toString(), client, client.requestHeader,
					client.cook, invoker);
			System.out.println("Returnd from addurl");
		}
	}

	void showAboutDialog() {
		if (abtDlg == null) {
			abtDlg = new AboutDlg();
		}
		Dimension d = t.getScreenSize();
		abtDlg.setLocation(d.width / 2 - abtDlg.getWidth() / 2, d.height / 2
				- abtDlg.getHeight() / 2);
		abtDlg.setVisible(true);
	}

	void copyURL(String url) {
		if (clipboard == null) {
			clipboard = t.getSystemClipboard();
		}
		clipboard.setContents(new StringSelection(url), null);
	}

	void copyPath() {
		int rows[] = table.getSelectedRows();
		if (rows == null)
			return;
		Object values[] = new Object[rows.length];
		if (rows != null) {
			values = new Object[rows.length];
			for (int i = 0; i < rows.length; i++) {
				int index = table.convertRowIndexToModel(rows[i]);
				DownloadListItem item = MainWindow.list.get(index);
				File file = new File(item.saveto, item.filename);
				values[i] = file;// table.getValueAt(rows[i], 0);
			}
		}
		StringBuffer plainBuf = new StringBuffer();
		StringBuffer htmlBuf = new StringBuffer();

		htmlBuf.append("<html>\n<body>\n<ul>\n");

		for (int i = 0; i < values.length; i++) {
			Object obj = values[i];
			String val = ((obj == null) ? "" : obj.toString());
			plainBuf.append(val + "\n");
			htmlBuf.append("  <li>" + val + "\n");
		}
		if (clipboard == null) {
			clipboard = t.getSystemClipboard();
		}
		clipboard.setContents(new FileTransferable(plainBuf.toString(), htmlBuf
				.toString(), values), null);

	}

	private void virusScan(String antivirTxt) {
		// TODO Auto-generated method stub
		createProcess(antivirTxt);
	}

	private void hungUp(String hungUpTxt) {
		// TODO Auto-generated method stub
		createProcess(hungUpTxt);
	}

	private void executeCommand(String cmdTxt) {
		createProcess(cmdTxt);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void intercept(Object obj) {
		if (obj instanceof HashMap) {
			HashMap<String, String> arg = (HashMap<String, String>) obj;
			addURL(arg.get("url"), null, null, arg.get("cookie"));
			return;
		}
		if (obj instanceof ArrayList) {
			ArrayList<String> flvList = (ArrayList<String>) obj;
			for (int i = 0; i < flvList.size(); i++) {
				try {
					URL url = new URL(flvList.get(i));
					MediaInfo minfo = new MediaInfo();
					minfo.name = XDMUtil.getFileName(url.getPath());
					minfo.url = url + "";
					minfo.size = "Unknown";
					minfo.type = "Unknown";
					mmodel.add(minfo);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (mw == null) {
				mw = new MediaGrabberWindow(mmodel, this);
			}
			mw.setLocationRelativeTo(this);
			mw.setVisible(true);
			return;
		}
		if (obj != null) {
			if (obj.toString().length() > 0)
				addURL(obj.toString(), null, null, null);
		}
	}

	void showTrayMessage() {
		try {
			if (tray != null) {
				trayIcon.displayMessage("Video Captured",
						"A FLV Video has been captured",
						TrayIcon.MessageType.INFO);
			}
		} catch (Throwable e) {
			// TODO: handle exception
		}
	}

	static private void createProcess(String cmd) {
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	JFileChooser fc;

	void createWinLink(String targetFile) {
		try {
			System.out.println("Creating shortcut at: " + targetFile);
			String jarFolder = getJarPath();
			String jarfile = new File(jarFolder, "xdman.jar").getAbsolutePath();
			File file = new File(System.getProperty("user.home"), "link.vbs");
			OutputStream out = new FileOutputStream(file);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					MainWindow.class.getResourceAsStream("/script/link.txt")));
			while (true) {
				String ln = in.readLine();
				if (ln == null)
					break;
				String l1 = ln.replace("<TARGET_LOCATION>", targetFile);
				String l2 = l1.replace("<JAR_PATH>", jarfile);
				String l3 = l2.replace("<ICON_LOCATION>", new File(jarFolder,
						"icon.ico").getAbsolutePath())
						+ "\r\n";
				out.write(l3.getBytes());
			}
			out.close();
			in.close();
			createProcess("WScript.exe \"" + file.getAbsolutePath() + "\"");
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	void winCreateLink() {
		try {
			if (fc == null) {
				fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			}
			if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				String desktopFile = fc.getSelectedFile().getAbsolutePath();
				createWinLink(desktopFile);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	public static String getJarPath() {
		try {
			String path = MainWindow.class.getResource("/").toURI().getPath();
			System.out.println(path);
			return new File(path).getAbsolutePath();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Error");
		return "Error";
	}

	void testBrowser() {
		JTextField url = new JTextField("http://localhost:" + config.port
				+ "/test");
		url.setEditable(false);
		JButton copy = new JButton("Copy");
		copy.addActionListener(this);
		Object arr[] = {
				"To check if XDM is synchronized with your browser\n"
						+ "Paste/type this URL in your browser.\n",
				url,
				copy,
				"If its not synchronized\n"
						+ "You have to configure the browser.\n"
						+ "If you have configured it already then try restarting it again." };
		JOptionPane.showOptionDialog(this, arr, "Test Browser",
				JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
				null, null, null);
	}

	void showBrowserStatus() {
		if (XDMProxyServer.browsers.size() > 0) {
			String bstr = "Monitoring browser: ";
			Iterator<String> it = XDMProxyServer.browsers.iterator();
			while (it.hasNext()) {
				bstr += " " + it.next();
			}
			JOptionPane.showMessageDialog(this, bstr);
		} else {
			JOptionPane
					.showMessageDialog(
							this,
							"XDM could not detect any network activity performed by your browser.\n"
									+ "To capture downloads you must configure your browser.\n"
									+ "[Goto help->Advanced Browser Integration]\n"
									+ "If you already configured it then try restarting the browser again");
		}
	}

	@Override
	public void drop(String url) {
		addURL(url, null, null, null);
	}

	FFIntDlg ffdlg;
	DefaultListModel ffmodel;

	void showFFDlg() {
		if (ffdlg == null) {
			ffmodel = new DefaultListModel();
			ffmodel.add(0, "http://localhost:" + config.port + "/xdmff.xpi");
			ffdlg = new FFIntDlg(ffmodel, this);
		}
		ffdlg.setVisible(true);
	}

	static boolean mod_xdm = false;
	static boolean first_run = false;

	private boolean attachProxy(boolean refresh) {
		File exe = new File(tempdir, "xdm_net_helper.exe");
		try {
			File tmp = new File(tempdir, "xdm_win_proxy_attach");
			InputStream in = getClass().getResourceAsStream("/resource/xdm");
			OutputStream out = new FileOutputStream(tmp);
			byte buf[] = new byte[8192];
			while (true) {
				int x = in.read(buf);
				if (x == -1)
					break;
				out.write(buf, 0, x);
			}
			out.close();
			tmp.renameTo(exe);
			List<String> cmds = new ArrayList<String>();
			cmds.add(exe.getAbsolutePath());
			if (!refresh) {
				cmds.add("http=http://localhost:" + config.port);
			}
			ProcessBuilder pb = new ProcessBuilder(cmds);
			pb.directory(new File(tempdir));
			Process proc = pb.start();
			proc.waitFor();
			if (proc.exitValue() != 0) {
				throw new Exception("Return code!=0" + " : " + proc.exitValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
			exe.delete();
			return false;
		}
		System.out.println("File Deleted: " + exe.delete());
		return true;
	}

	void showInfo() {
		if (File.separatorChar == '\\') {
			JCheckBox enableCapture = new JCheckBox("Capture Downloads",
					proxyAttached);
			File bak = new File(System.getProperty("user.home"), "reg-bak.reg");
			File main_bak = new File(System.getProperty("user.home"),
					"xdm-main-reg-bak.reg");

			// restore.setEnabled(bak.exists());
			if (JOptionPane.showOptionDialog(null,
					new Object[] { enableCapture }, "Capture Downloads...",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.INFORMATION_MESSAGE, null, null, null) == JOptionPane.OK_OPTION) {
				if (enableCapture.isSelected()) {
					config.attachProxy = true;
					if (proxyAttached) {
						return;
					} else {
						if (!RegUtil.takeBackup(bak)) {
							config.attachProxy = false;
							proxyAttached = false;
							JOptionPane
									.showMessageDialog(
											null,
											"Auto configuration Failed because Network backup failed.\nPlease try manual configuration");
							return;
						}
						if (!attachProxy(false)) {
							config.attachProxy = false;
							proxyAttached = false;
							JOptionPane
									.showMessageDialog(null,
											"Auto configuration Failed.\nPlease try manual configuration");
							return;
						} else {
							config.attachProxy = true;
							proxyAttached = true;
						}
					}
				} else {
					boolean restore_main_bak = false;
					if (main_bak.exists()) {
						restore_main_bak = JOptionPane.showConfirmDialog(null,
								"Restore previous network settings?\nSettings will be restored to date: "
										+ new Date(main_bak.lastModified())) == JOptionPane.YES_OPTION;
					}
					if (!restore_main_bak) {
						RegUtil.restore(bak);
						proxyAttached = false;
						config.attachProxy = false;
						attachProxy(true);
					} else {
						RegUtil.restore(main_bak);
						proxyAttached = false;
						config.attachProxy = false;
						attachProxy(true);
					}

				}
			}
		} else {
			JOptionPane.showMessageDialog(this,
					"You have to manually configure your network for this OS: "
							+ System.getProperty("os.name"));
			try {
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().browse(
							new URI("http://localhost:" + config.port
									+ "/help/index.html"));
				} else {
					JOptionPane.showMessageDialog(this,
							"Type this address in your browser:\n"
									+ "http://localhost:" + config.port
									+ "/help/index.html");
				}
			} catch (Exception err) {
				err.printStackTrace();
			}
		}
	}

	void createLinuxLink(String target, boolean min) {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("[Desktop Entry]\n");
			buf.append("Encoding=UTF-8\n");
			buf.append("Version=1.0\n");
			buf.append("Type=Application\n");
			buf.append("Terminal=false\n");
			String jarPath = getJarPath();
			buf.append("Exec=java -jar '"
					+ new File(jarPath, "xdman.jar").getAbsolutePath() + "'"
					+ (min ? " -m" : "") + "\n");
			buf.append("Name=Xtreme Download Manager\n");
			buf.append("Icon="
					+ new File(jarPath, "icon.png").getAbsolutePath() + "\n");
			File desktop = new File(target, "xdman.desktop");
			OutputStream out = new FileOutputStream(desktop);
			out.write(buf.toString().getBytes());
			out.close();
			desktop.setExecutable(true);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error creating shortcut");
		}
	}

	void linuxCreateLink() {
		try {
			if (fc == null) {
				fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			}
			if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				String desktopFile = fc.getSelectedFile().getAbsolutePath();
				createLinuxLink(desktopFile, false);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	void showBrowserIntegrationDlg() {
		boolean win = File.separatorChar == '\\';
		if (bint == null) {
			bint = new BrowserIntDlg();
			bint.setIconImage(MainWindow.icon.getImage());
			bint.setListeners(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						if (view == null) {
							view = getHTMLViwer();
						}
						view.setDocument(getClass().getResource(
								"/help/video_download.html"));
						view.setVisible(true);

					} catch (Exception err) {
						err.printStackTrace();
					}
				}
			}, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						if (view == null) {
							view = getHTMLViwer();
						}
						view.setDocument(getClass().getResource(
								"/help/browser_integration.html"));
						view.setVisible(true);

					} catch (Exception err) {
						err.printStackTrace();
					}
				}
			}, this, this, this, "http://localhost:9614/xdmff.xpi");
		}
		if (!win)
			bint.auto.setEnabled(false);
		// bint.autoStart.setSelected(false);
		bint.setLocationRelativeTo(this);
		bint.setVisible(true);
	}

	void setAutoStart(boolean on) {
		boolean win = File.separatorChar == '\\';
		if (on) {
			if (win)
				eanableAutoStartWin();
			else {
				if (!enableAutoStartLinux()) {
					JOptionPane.showMessageDialog(this,
							"Please Manually Add XDM at startup");
				}
			}
		} else {
			if (win)
				disableAutoStartWin();
			else {
				disableAutoStartLinux();
			}
		}
	}

	boolean disableAutoStartLinux() {
		String autoStartDirs[] = { ".config/autostart", ".kde/Autostart",
				".kde/autostart", ".config/Autostart", ".kde4/Autostart" };
		File home = new File(System.getProperty("user.home"));
		File autoStartDir = null;
		for (int i = 0; i < autoStartDirs.length; i++) {
			autoStartDir = new File(home, autoStartDirs[i]);
			if (!autoStartDir.exists()) {
				autoStartDir = null;
			} else {
				// createLinuxLink(autoStartDir.getAbsolutePath());
				File file = new File(autoStartDir, "xdman.desktop");
				if (file.exists()) {
					if (file.delete()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	boolean enableAutoStartLinux() {
		String autoStartDirs[] = { ".config/autostart", ".kde/Autostart",
				".kde/autostart", ".config/Autostart", ".kde4/Autostart" };
		File home = new File(System.getProperty("user.home"));
		File autoStartDir = null;
		for (int i = 0; i < autoStartDirs.length; i++) {
			autoStartDir = new File(home, autoStartDirs[i]);
			if (!autoStartDir.exists()) {
				autoStartDir = null;
			} else {
				createLinuxLink(autoStartDir.getAbsolutePath(), true);
				return true;
			}
		}
		return false;
	}

	boolean eanableAutoStartWin() {
		try {
			System.out.println("Adding startup entry");
			String jarFolder = getJarPath();
			String jarfile = new File(jarFolder, "xdman.jar").getAbsolutePath();
			File file = new File(System.getProperty("user.home"), "startup.vbs");
			OutputStream out = new FileOutputStream(file);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					MainWindow.class
							.getResourceAsStream("/script/startup_add.txt")));
			while (true) {
				String ln = in.readLine();
				if (ln == null)
					break;
				String l2 = ln.replace("<JAR_PATH>", jarfile);
				String l3 = l2.replace("<ICON_LOCATION>", new File(jarFolder,
						"icon.ico").getAbsolutePath())
						+ "\r\n";
				out.write(l3.getBytes());
			}
			out.close();
			in.close();
			createProcess("WScript.exe \"" + file.getAbsolutePath() + "\"");
		} catch (Exception e) {
			System.out.println(e);
			return false;
		}
		return true;
	}

	void runScriptWin() {
		try {
			System.out.println("Adding Desktop entry");
			String jarFolder = getJarPath();
			String jarfile = new File(jarFolder, "xdman.jar").getAbsolutePath();
			File file = new File(System.getProperty("user.home"),
					"desktop_shortcut.vbs");
			OutputStream out = new FileOutputStream(file);
			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							MainWindow.class
									.getResourceAsStream("/script/desktop_shortcut.txt")));
			while (true) {
				String ln = in.readLine();
				if (ln == null)
					break;
				String l2 = ln.replace("<JAR_PATH>", jarfile);
				String l3 = l2.replace("<ICON_LOCATION>", new File(jarFolder,
						"icon.ico").getAbsolutePath())
						+ "\r\n";
				out.write(l3.getBytes());
			}
			out.close();
			in.close();
			createProcess("WScript.exe \"" + file.getAbsolutePath() + "\"");
		} catch (Exception e) {
			System.out.println(e);
		}
		try {
			System.out.println("Adding Prgrams entry");
			String jarFolder = getJarPath();
			String jarfile = new File(jarFolder, "xdman.jar").getAbsolutePath();
			File file = new File(System.getProperty("user.home"),
					"programs_shortcut.vbs");
			OutputStream out = new FileOutputStream(file);
			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							MainWindow.class
									.getResourceAsStream("/script/programs_shortcut.txt")));
			while (true) {
				String ln = in.readLine();
				if (ln == null)
					break;
				String l2 = ln.replace("<JAR_PATH>", jarfile);
				String l3 = l2.replace("<ICON_LOCATION>", new File(jarFolder,
						"icon.ico").getAbsolutePath())
						+ "\r\n";
				out.write(l3.getBytes());
			}
			out.close();
			in.close();
			createProcess("WScript.exe \"" + file.getAbsolutePath() + "\"");
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	boolean disableAutoStartWin() {
		try {
			InputStream in = getClass().getResourceAsStream(
					"/script/startup_del.txt");
			File remScript = new File(System.getProperty("user.home"),
					"rem.vbs");
			OutputStream out = new FileOutputStream(remScript);
			byte b[] = new byte[1024];
			int x = in.read(b);
			out.write(b, 0, x);
			out.close();
			Runtime.getRuntime().exec(
					"wscript \"" + remScript.getAbsolutePath() + "\"");
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	HelpDialog getHTMLViwer() {
		HashMap<String, URL> map = new HashMap<String, URL>();
		map.put("Browser Integration", getClass().getResource(
				"/help/browser_integration.html"));
		map.put("Capturing Videos", getClass().getResource(
				"/help/video_download.html"));
		map.put("Refresh Broken Downloads", getClass().getResource(
				"/help/refresh_link.html"));
		HelpDialog hlp = new HelpDialog();
		hlp.addPages(map);
		return hlp;
	}

	@Override
	public void download(List<BatchItem> list, boolean startQ) {
		for (int i = 0; i < list.size(); i++) {
			BatchItem item = list.get(i);
			DownloadListItem ditem = new DownloadListItem();
			ditem.url = item.url;
			ditem.saveto = item.dir;
			ditem.filename = item.fileName;
			ditem.dateadded = new Date().toString();
			ditem.lasttry = new Date().toString();
			ditem.q = true;
			String user = item.user;
			String pass = item.pass;
			HashMap<String, String> extra = null;
			if (!(user == null || pass == null)) {
				if (user.length() > 0) {
					try {
						if (extra == null) {
							extra = new HashMap<String, String>();
						}
						extra.put("USER", user);
						extra.put("PASS", pass);
					} catch (Exception err) {
					}
				}
			}
			ditem.extra = extra;
			ditem.icon = IconUtil.getIcon(XDMUtil.findCategory(ditem.filename));
			MainWindow.list.add(ditem);
		}
		if (startQ)
			startQ();
		model.fireTableDataChanged();
	}

	public void initBatchDownload(List<String> list, String user, String pass) {
		System.out.println("Batch");
		if (list == null || list.size() < 1) {
			return;
		}
		if (batchDlg == null) {
			batchDlg = new BatchDownloadDlg();
		}
		batchDlg.setLocationRelativeTo(null);
		List<BatchItem> blist = new ArrayList<BatchItem>();
		for (int i = 0; i < list.size(); i++) {
			BatchItem item = new BatchItem();
			item.url = list.get(i);
			item.fileName = XDMUtil.getFileName(item.url);
			item.user = user;
			item.pass = pass;
			blist.add(item);
			System.out.println(item.url);
		}
		batchDlg.showDialog(blist, config.destdir, this);
	}

	public void mediaCaptured(ArrayList<String> list) {
		if (list == null || list.size() < 1) {
			return;
		}
		mmodel.list.clear();
		for (int i = 0; i < list.size(); i++) {
			MediaInfo info = new MediaInfo();
			String yt = list.get(i);
			info.url = yt;
			info.name = XDMUtil.getFileName(info.url);
			mmodel.add(info);
		}
		mmodel.fireTableDataChanged();
		if (mw == null) {
			mw = new MediaGrabberWindow(mmodel, this);
		}
		mw.setLocationRelativeTo(this);
		mw.setVisible(true);
	}

	public static void main(String[] args) {
		HashMap<String, String> arg = new HashMap<String, String>();
		String key = "url";
		boolean min = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-c")) {
				key = "cookie";
				continue;
			} else if (args[i].equals("-r")) {
				key = "referer";
				continue;
			} else if (args[i].equals("-m")) {
				min = true;
				continue;
			} else if (args[i].equals("-u")) {
				key = "url";
				continue;
			} else {
				arg.put(key, args[i]);
			}
		}
		// System.out.println(System.getProperty("java.home"));
		// System.out.println(System.getProperty("java.class.path"));
		boolean showInfo = false;
		File configFile = new File(appdir, ".xdmconf");
		first_run = !configFile.exists();
		config = XDMConfig.load(configFile);
		if (config.tcpBuf <= 8192) {
			config.tcpBuf = 8192;
		}
		showInfo = (!configFile.exists());
		XDMProxyServer server = new XDMProxyServer(null, config, null, null,
				arg);
		if (!server.init()) {
			// JOptionPane
			// .showMessageDialog(null,
			// "Advanced Browser Integration Module could not be started.");
			return;
		} else {
			mod_xdm = true;
		}
		/*
		 * if (config.port != port) { showInfo = true; JOptionPane
		 * .showMessageDialog( null,
		 * "XDM Module is running on an alternate port\n" +
		 * "Advanced Browser Integration and Firefox Integration may not work");
		 * }
		 */
		DownloadList list = new DownloadList(appdir);
		IDownloadListener l = list;
		final MainWindow w = new MainWindow(list);
		w.setConfig(config);
		w.mmodel = new MediaTableModel(w);
		w.setList(list, l);
		if (!min)
			w.setVisible(true);
		w.showInfo = showInfo;
		Authenticator a = Authenticator.getInstance();
		a.load(new File(appdir, "sites.conf"));
		server.observer = w;
		server.intercepter = w;
		server.model = w.mmodel;
		w.server = server;
		w.httpModel = new HttpTableModel();
		w.server.cl = w.httpModel;
		System.out.println();
		if (arg.size() > 0) {
			System.out.println("creating new process with param" + arg);
			String url = arg.get("url");
			String cookie = arg.get("cookie");
			System.out.println("URL " + url + " COOKIE " + cookie);
			w.addURL(url, null, null, cookie);
		} else {
			System.out.println(args.length);
		}
		new Thread() {
			@Override
			public void run() {
				String path = getJarPath();
				// first_run = true;
				if (first_run) {
					// boolean win = File.separatorChar == '\\';
					// if (win) {
					// w.eanableAutoStartWin();
					// }
					config.jarPath = path;
					w.showBrowserIntegrationDlg();
					w.runScriptWin();
					if (File.separatorChar == '\\') {
						System.out.println("Taking Main Network backup...");
						File bak = new File(System.getProperty("user.home"),
								"xdm-main-reg-bak.reg");
						if (RegUtil.takeBackup(bak)) {
							System.out.println("Main Backup successfull");
						} else {
							System.out.println("Main Backup Failed");
						}
					}
				} else {
					String jarPath = path;
					System.out.println("Old path: " + config.jarPath
							+ " Current Path: " + jarPath);
					config.jarPath = jarPath;
				}
				// /if (new File(path, "Xtreme Download Manager.lnk").exists())
				// {
				// / return;
				// /}
				if (File.separatorChar == '\\') {
					w.createWinLink(path);
					System.out.println("Taking Network backup...");
					File bak = new File(System.getProperty("user.home"),
							"reg-bak.reg");
					if (RegUtil.takeBackup(bak)) {
						System.out.println("Backup successfull");
						if (config.attachProxy) {
							System.out.println("Attaching Proxy...");
							w.proxyAttached = w.attachProxy(false);
							config.attachProxy = w.proxyAttached;
						}// w.runWPAC(true);
					} else {
						System.out.println("Backup failed");
					}
				} else {
					w.createLinuxLink(path, false);
				}
			}
		}.start();
		w.server.start();
	}
	// ftp://ftp1.freebsd.org/pub/FreeBSD/
	// http://sound27.mp3pk.com/indian/khiladi786/%5BSongs.PK%5D%20Khiladi%20786%20-%2008%20-%20Lonely%20%28Remix%29.mp3
	// http://sourceforge.net/projects/sevenzip/files/7-Zip/9.20/7z920.exe/download?use_mirror=ncu
}
