package title;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableRowSorter;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.IMessageEditor;
import burp.IPAddressUtils;
import dao.TitleDao;
import thread.ThreadGetSubnet;
import thread.ThreadGetTitleWithForceStop;
import title.search.SearchTextField;

public class TitlePanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel buttonPanel;
	private JLabel lblSummaryOfTitle;
	private JRadioButton rdbtnUnCheckedItems;
	private JRadioButton rdbtnCheckingItems;
	private JRadioButton rdbtnCheckedItems;
	private JRadioButton rdbtnMoreActionItems;
	private SearchTextField textFieldSearch;

	private IMessageEditor requestViewer;
	private IMessageEditor responseViewer;
	private JSplitPane detailPanel;

	//add table and tablemodel to GUI
	private LineTable titleTable;
	private TitleDao titleDao;
	PrintWriter stdout;
	PrintWriter stderr;
	private ThreadGetTitleWithForceStop threadGetTitle;
	private GetTitleTempConfig tempConfig; //每次获取title过程中的配置。
	private IndexedHashMap<String,LineEntry> BackupLineEntries;
	private GUIMain guiMain;
	private JTabbedPane RequestPanel;
	private JTabbedPane ResponsePanel;


	public JTextField getTextFieldSearch() {
		return textFieldSearch;
	}

	/*
	public static void setTextFieldSearch(JTextField textFieldSearch) {
		TitlePanel.textFieldSearch = textFieldSearch;
	}*/

	public LineTable getTitleTable() {
		return titleTable;
	}

	public IndexedHashMap<String,LineEntry> getBackupLineEntries() {
		return BackupLineEntries;
	}

	public GetTitleTempConfig getTempConfig() {
		return tempConfig;
	}

	public void setTempConfig(GetTitleTempConfig tempConfig) {
		this.tempConfig = tempConfig;
	}

	public JLabel getLblSummaryOfTitle() {
		return lblSummaryOfTitle;
	}

	public void setLblSummaryOfTitle(JLabel lblSummaryOfTitle) {
		this.lblSummaryOfTitle = lblSummaryOfTitle;
	}

	public JRadioButton getRdbtnUnCheckedItems() {
		return rdbtnUnCheckedItems;
	}

	public void setRdbtnUnCheckedItems(JRadioButton rdbtnUnCheckedItems) {
		this.rdbtnUnCheckedItems = rdbtnUnCheckedItems;
	}

	public JRadioButton getRdbtnCheckingItems() {
		return rdbtnCheckingItems;
	}

	public void setRdbtnCheckingItems(JRadioButton rdbtnCheckingItems) {
		this.rdbtnCheckingItems = rdbtnCheckingItems;
	}

	public JRadioButton getRdbtnCheckedItems() {
		return rdbtnCheckedItems;
	}

	public void setRdbtnCheckedItems(JRadioButton rdbtnCheckedItems) {
		this.rdbtnCheckedItems = rdbtnCheckedItems;
	}

	public JRadioButton getRdbtnMoreActionItems() {
		return rdbtnMoreActionItems;
	}

	public void setRdbtnMoreActionItems(JRadioButton rdbtnMoreActionItems) {
		this.rdbtnMoreActionItems = rdbtnMoreActionItems;
	}

	public IMessageEditor getRequestViewer() {
		return requestViewer;
	}

	public IMessageEditor getResponseViewer() {
		return responseViewer;
	}

	public ThreadGetTitleWithForceStop getThreadGetTitle() {
		return threadGetTitle;
	}

	public void setThreadGetTitle(ThreadGetTitleWithForceStop threadGetTitle) {
		this.threadGetTitle = threadGetTitle;
	}

	public TitlePanel(GUIMain guiMain) {//构造函数
		this.guiMain = guiMain;
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(new BorderLayout(0, 0));
		this.add(createButtonPanel(), BorderLayout.NORTH);



		titleTable = new LineTable(guiMain);
		detailPanel = DetailPanel();

		JSplitPane splitPane = new JSplitPane();//table area + detail area
		splitPane.setResizeWeight(0.5);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

		JScrollPane scrollPaneRequests = new JScrollPane(titleTable,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);//table area

		splitPane.setLeftComponent(scrollPaneRequests);
		splitPane.setRightComponent(detailPanel);
		this.add(splitPane,BorderLayout.CENTER);

	}

	public JPanel createButtonPanel() {
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		JButton btnAction = new JButton("Action");
		btnAction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new GetTitleMenu(guiMain).show(btnAction, btnAction.getX(), btnAction.getY());
			}
		});
		buttonPanel.add(btnAction);

		JButton buttonSearch = new JButton("Search");
		textFieldSearch = new SearchTextField("",buttonSearch);
		buttonPanel.add(textFieldSearch);

		buttonSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = textFieldSearch.getText();
				titleTable.search(keyword);
				//searchHistory.addRecord(keyword);
				digStatus();
			}
		});
		buttonPanel.add(buttonSearch);

		rdbtnUnCheckedItems = new JRadioButton(LineEntry.CheckStatus_UnChecked);
		rdbtnUnCheckedItems.setSelected(true);
		rdbtnUnCheckedItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonSearch.doClick();
			}
		});
		buttonPanel.add(rdbtnUnCheckedItems);

		rdbtnCheckingItems = new JRadioButton(LineEntry.CheckStatus_Checking);
		rdbtnCheckingItems.setSelected(true);
		rdbtnCheckingItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonSearch.doClick();
			}
		});
		buttonPanel.add(rdbtnCheckingItems);

		rdbtnCheckedItems = new JRadioButton(LineEntry.CheckStatus_Checked);
		rdbtnCheckedItems.setSelected(false);
		rdbtnCheckedItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonSearch.doClick();
			}
		});
		buttonPanel.add(rdbtnCheckedItems);

		rdbtnMoreActionItems = new JRadioButton(LineEntry.CheckStatus_MoreAction);
		rdbtnMoreActionItems.setSelected(false);
		rdbtnMoreActionItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonSearch.doClick();
			}
		});
		buttonPanel.add(rdbtnMoreActionItems);

		lblSummaryOfTitle = new JLabel("^_^");
		buttonPanel.add(lblSummaryOfTitle);
		buttonPanel.setToolTipText("");

		return buttonPanel;
	}

	public JSplitPane DetailPanel(){

		JSplitPane RequestDetailPanel = new JSplitPane();//request and response
		RequestDetailPanel.setResizeWeight(0.5);

		RequestPanel = new JTabbedPane();
		RequestDetailPanel.setLeftComponent(RequestPanel);

		ResponsePanel = new JTabbedPane();
		RequestDetailPanel.setRightComponent(ResponsePanel);

		return RequestDetailPanel;
	}
	/**
	 * 转移手动保存的记录
	 */
	public void transferManualSavedItems(){
		if (BackupLineEntries == null){
			System.out.println("BackupLineEntries is Null");
			stderr.println("BackupLineEntries is Null");
			return;
		}
		for (LineEntry entry:BackupLineEntries.values()) {
			if (entry.getEntrySource().equalsIgnoreCase(LineEntry.Source_Manual_Saved)) {
				titleTable.getLineTableModel().addNewLineEntry(entry);
			}
		}
	}
	
	public static void AddWithSoureType(HashMap<String,String> domains,Set<String> input,String type) {
		if (input != null && domains != null) {
			for (String in:input) {
				domains.put(in, type);
			}
		}
	}

	/**
	 *
	 * 根据所有已知域名获取title
	 */
	public void getAllTitle(){
		if (!stopGetTitleThread(true)){//其他get title线程未停止
			stdout.println("still have get title thread is running, will do nothing.");
			return;
		}
		guiMain.getDomainPanel().backupDB();

		Set<String> targetsToReq = new HashSet<String>();
		targetsToReq.addAll(guiMain.getDomainPanel().getDomainResult().getSubDomainSet());
		targetsToReq.addAll(guiMain.getDomainPanel().fetchTargetModel().fetchTargetIPSet());
		targetsToReq.addAll(guiMain.getDomainPanel().getDomainResult().getIPSetOfCert());
		targetsToReq.removeAll(guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet());
		
		HashMap<String,String> domains = new HashMap<String,String>();//新建一个对象，直接赋值后的删除操作，实质是对domainResult的操作。
		AddWithSoureType(domains,targetsToReq,LineEntry.Source_Certain);
		
		targetsToReq = new HashSet<String>();
		targetsToReq.addAll(guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets());
		targetsToReq.removeAll(guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet());
		AddWithSoureType(domains,targetsToReq,LineEntry.Source_Custom_Input);
		
		stdout.println(domains.size()+" targets to request");
		if (domains.size() <= 0) return;
		tempConfig = new GetTitleTempConfig(domains.size());
		if (tempConfig.getThreadNumber() <=0) {
			return;
		}
		//backup to history
		BackupLineEntries = titleTable.getLineTableModel().getLineEntries();

		//clear tableModel
		LineTableModel titleTableModel = new LineTableModel(guiMain);//clear
		loadData(titleTableModel);
		//转移以前手动保存的记录
		transferManualSavedItems();

		setThreadGetTitle(new ThreadGetTitleWithForceStop(guiMain,domains,tempConfig.getThreadNumber()));
		getThreadGetTitle().start();
	}


	public void getExtendTitle(){
		if (!stopGetTitleThread(true)){//其他get title线程未停止
			return;
		}
		guiMain.getDomainPanel().backupDB();

		HashMap<String,String> extendIPs = new HashMap<String,String>();//新建一个对象，直接赋值后的删除操作，实质是对domainResult的操作。

		Set<String> extendIPSet = titleTable.getLineTableModel().GetExtendIPSet();
		stdout.println(extendIPSet.size()+" targets to request");
		stdout.println(extendIPSet.size()+" extend IP Address founded"+extendIPSet);
		if (extendIPSet.size() <= 0) return;

		AddWithSoureType(extendIPs,extendIPSet,LineEntry.Source_Subnet_Extend);
		
		tempConfig = new GetTitleTempConfig(extendIPSet.size());
		if (tempConfig.getThreadNumber() <=0) {
			return;
		}

		setThreadGetTitle(new ThreadGetTitleWithForceStop(guiMain,extendIPs,tempConfig.getThreadNumber()));
		getThreadGetTitle().start();

	}

	/**
	 * 获取新发现域名的title，这里会尝试之前请求失败的域名，可能需要更多时间
	 */
	public void getTitleOfNewDomain(){
		if (!stopGetTitleThread(true)){//其他get title线程未停止
			return;
		}

		guiMain.getDomainPanel().backupDB();

		Set<String> newDomains = new HashSet<>(guiMain.getDomainPanel().getDomainResult().getSubDomainSet());//新建一个对象，直接赋值后的删除操作，实质是对domainResult的操作。

		newDomains.addAll(guiMain.getDomainPanel().getTargetTable().getTargetModel().fetchTargetIPSet());
		newDomains.addAll(guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets());
		newDomains.addAll(guiMain.getDomainPanel().getDomainResult().getIPSetOfCert());

		Set<String> hostsInTitle = titleTable.getLineTableModel().GetHostsWithSpecialPort();
		newDomains.removeAll(hostsInTitle);

		//remove domains in black list
		newDomains.removeAll(guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet());
		stdout.println(newDomains.size()+" targets to request");
		
		if (newDomains.size() <= 0) return;
		tempConfig = new GetTitleTempConfig(newDomains.size());
		if (tempConfig.getThreadNumber() <=0) {
			return;
		}

		HashMap<String,String> newDomainsMap = new HashMap<String,String>();//新建一个对象，直接赋值后的删除操作，实质是对domainResult的操作。

		AddWithSoureType(newDomainsMap,newDomains,LineEntry.Source_Certain);
		
		setThreadGetTitle(new ThreadGetTitleWithForceStop(guiMain,newDomainsMap,tempConfig.getThreadNumber()));
		getThreadGetTitle().start();
	}


	public String getSubnet(boolean isCurrent,boolean justPulic){
		//stdout.println(" "+isCurrent+justPulic);
		Set<String> subnets;
		if (isCurrent) {//获取的是现有可成功连接的IP集合+用户指定的IP网段集合
			subnets = titleTable.getLineTableModel().GetSubnets();
		}else {//重新解析所有域名的IP
			ThreadGetSubnet thread = new ThreadGetSubnet(guiMain.getDomainPanel().getDomainResult().getSubDomainSet());
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return "thread Interrupted";
			}
			Set<String> IPsOfDomain = thread.IPset;
			Set<String> IPsOfcertainSubnets = guiMain.getDomainPanel().fetchTargetModel().fetchTargetIPSet();//用户配置的确定IP+网段
			IPsOfDomain.addAll(IPsOfcertainSubnets);
			subnets = IPAddressUtils.toSmallerSubNets(IPsOfDomain);
		}

		HashSet<String> result = new HashSet<>(subnets);
		if (justPulic) {
			//stdout.println("删除私有IP");
			for (String subnet :subnets) {
				String tmp = subnet.split("/")[0];
				if (IPAddressUtils.isPrivateIPv4(tmp)) {
					result.remove(subnet);
					//stdout.println("删除"+subnet);
				}
			}
		}
		List<String> tmplist= new ArrayList<>(result);
		Collections.sort(tmplist);
		return String.join(System.lineSeparator(), tmplist);
	}

	/**
	 * 用于从DB文件中加载数据，没有去重检查。
	 * 这种加载方式没有改变tableModel，所以tableModelListener也还在。
	 */
	public void loadData(String currentDBFile) {
		titleDao = new TitleDao(currentDBFile);
		List<LineEntry> lines = titleDao.selectAllTitle();
		LineTableModel titleTableModel = new LineTableModel(guiMain, lines);
		loadData(titleTableModel);
	}

	private void loadData(LineTableModel titleTableModel){

		TableRowSorter<LineTableModel> tableRowSorter = new TableRowSorter<LineTableModel>(titleTableModel);
		titleTable.setRowSorter(tableRowSorter);
		titleTable.setModel(titleTableModel);
		//IndexOutOfBoundsException size为0，为什么会越界？
		//!!!注意：这里必须先setRowSorter，然后再setModel。否则就会出现越界问题。因为当setModel时，会触发数据变更事件，这个时候会调用Sorter。
		// 而这个时候的Sorter中还是旧数据，就会认为按照旧数据的容量去获取数据，从而导致越界。

		int row = titleTableModel.getLineEntries().size();
		System.out.println(row+" title entries loaded from database file");
		stdout.println(row+" title entries loaded from database file");
		digStatus();
		titleTable.search("");// hide checked items
		titleTable.tableinit();

		try {
			requestViewer = BurpExtender.getCallbacks().createMessageEditor(titleTable.getLineTableModel(), false);
			responseViewer = BurpExtender.getCallbacks().createMessageEditor(titleTable.getLineTableModel(), false);
			RequestPanel.removeAll();
			ResponsePanel.removeAll();
			RequestPanel.addTab("Request", requestViewer.getComponent());
			ResponsePanel.addTab("Response", responseViewer.getComponent());
		} catch (Exception e) {
			//捕获异常，以便程序以非burp插件运行时可以启动
			//e.printStackTrace();
		}
	}


	public void digStatus() {
		String status = titleTable.getLineTableModel().getStatusSummary();
		lblSummaryOfTitle.setText(status);
	}

	/**
	 * 返回是否发送了stop信号
	 * @param askBeforeStop
	 * @return
	 */
	public boolean stopGetTitleThread(boolean askBeforeStop){
		if (getThreadGetTitle() != null && getThreadGetTitle().isAlive()){
			if (askBeforeStop){
				int confirm = JOptionPane.showConfirmDialog(null,"Other Get Title Thread Is Running," +
						"Are you sure to stop it and run this task?");
				if (confirm != JOptionPane.YES_OPTION){
					return false;
				}
			}
			getThreadGetTitle().interrupt();
			return true;
		}
		return true;
	}
}
