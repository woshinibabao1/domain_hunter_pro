package burp;

import GUI.GUI;
import GUI.LineEntryMenuForBurp;
import Tools.ToolPanel;
import bsh.This;
import domain.DomainConsumer;
import domain.DomainPanel;
import domain.DomainProducer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import title.TitlePanel;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BurpExtender implements IBurpExtender, ITab, IExtensionStateListener,IContextMenuFactory,IHttpListener{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static IBurpExtenderCallbacks callbacks;
	private static PrintWriter stdout;
	private static PrintWriter stderr;
	private static String ExtenderName = "Domain Hunter Pro";
	private static String Version =  This.class.getPackage().getImplementationVersion();
	private static String Author = "by bit4woo";
	private static String github = "https://github.com/bit4woo/domain_hunter_pro";
	private static GUI gui;
	public static final String Extension_Setting_Name_Line_Config = "domain-Hunter-pro-line-config";
	public static final String Extension_Setting_Name_DB_File = "domain-Hunter-pro-db-file-path";

	private static final Logger log=LogManager.getLogger(BurpExtender.class);
	public static DomainProducer liveAnalysisTread;
	public static DomainConsumer liveDataSaveTread;
	public static BlockingQueue<IHttpRequestResponse> liveinputQueue = new LinkedBlockingQueue<IHttpRequestResponse>();
	//use to store messageInfo of proxy live
	public static BlockingQueue<IHttpRequestResponse> inputQueue = new LinkedBlockingQueue<IHttpRequestResponse>();
	//use to store messageInfo
	public static BlockingQueue<String> subDomainQueue = new LinkedBlockingQueue<String>();
	public static BlockingQueue<String> similarDomainQueue = new LinkedBlockingQueue<String>();
	public static BlockingQueue<String> relatedDomainQueue = new LinkedBlockingQueue<String>();
	public static BlockingQueue<String> emailQueue = new LinkedBlockingQueue<String>();
	public static BlockingQueue<String> packageNameQueue = new LinkedBlockingQueue<String>();

	public static PrintWriter getStdout() {
		//不同的时候调用这个参数，可能得到不同的值
		try{
			stdout = new PrintWriter(callbacks.getStdout(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
		}
		return stdout;
	}

	public static PrintWriter getStderr() {
		try{
			stderr = new PrintWriter(callbacks.getStderr(), true);
		}catch (Exception e){
			stderr = new PrintWriter(System.out, true);
		}
		return stderr;
	}

	public static IBurpExtenderCallbacks getCallbacks() {
		return callbacks;
	}

	public static String getGithub() {
		return github;
	}

	public static GUI getGui() {
		return gui;
	}

	public static String getExtenderName() {
		return ExtenderName;
	}

	//name+version+author
	public static String getFullExtenderName(){
		return ExtenderName+" "+Version+" "+Author;
	}


	public static void saveDBfilepathToExtension() {
		//to save domain result to extensionSetting
		//仅仅存储sqllite数据库的名称,也就是domainResult的项目名称
		if (GUI.currentDBFile != null) {
			BurpExtender.getCallbacks().saveExtensionSetting(BurpExtender.Extension_Setting_Name_DB_File, null);
			BurpExtender.getCallbacks().saveExtensionSetting(BurpExtender.Extension_Setting_Name_DB_File, GUI.currentDBFile.getAbsolutePath());
		}
			
	}

	public static String loadDBfilepathFromExtension() {
		return BurpExtender.getCallbacks().loadExtensionSetting(BurpExtender.Extension_Setting_Name_DB_File);
	}


	//当更换DB文件时，需要清空。虽然不清空最终结果不受影响，但是输出内容会比较奇怪。
	public static void clearQueue() {
		liveinputQueue.clear();
		inputQueue.clear();

		subDomainQueue.clear();
		similarDomainQueue.clear();
		relatedDomainQueue.clear();
		emailQueue.clear();
		packageNameQueue.clear();
	}


	public void startLiveCapture(){
		liveAnalysisTread = new DomainProducer(BurpExtender.liveinputQueue,BurpExtender.subDomainQueue,
				BurpExtender.similarDomainQueue,BurpExtender.relatedDomainQueue,
				BurpExtender.emailQueue,BurpExtender.packageNameQueue,9999);//必须是9999，才能保证流量进程不退出。
		liveAnalysisTread.start();

		liveDataSaveTread = new DomainConsumer(1);
		liveDataSaveTread.start();
	}

	public void stopLiveCapture(){
		if (null != liveAnalysisTread){
			liveAnalysisTread.stopThread();
		}

		if (null != liveDataSaveTread){
			liveDataSaveTread.QueueToResult();
			liveDataSaveTread.stopThread();
		}
	}

	//插件加载过程中需要做的事
	@Override
	public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
	{
		BurpExtender.callbacks = callbacks;

		getStdout();
		getStderr();
		stdout.println(getFullExtenderName());
		stdout.println(github);

		callbacks.setExtensionName(getFullExtenderName()); //插件名称
		callbacks.registerExtensionStateListener(this);
		callbacks.registerContextMenuFactory(this);
		callbacks.registerHttpListener(this);//主动根据流量收集信息

		gui = new GUI();

		SwingUtilities.invokeLater(new Runnable()
		{//create GUI
			public void run()
			{
				BurpExtender.callbacks.addSuiteTab(BurpExtender.this); //这里的BurpExtender.this实质是指ITab对象，也就是getUiComponent()中的contentPane.这个参数由GUI()函数初始化。
				//如果这里报java.lang.NullPointerException: Component cannot be null 错误，需要排查contentPane的初始化是否正确。
			}
		});

		//recovery save domain results from extensionSetting
		String dbFilePath = loadDBfilepathFromExtension();
		System.out.println("Database FileName From Extension Setting: "+dbFilePath);
		if (dbFilePath != null && dbFilePath.endsWith(".db")) {
			gui.LoadData(dbFilePath);
		}

		gui.getToolPanel().loadConfigToGUI();
		startLiveCapture();
	}

	@Override
	public void extensionUnloaded() {
		try {//避免这里错误导致保存逻辑的失效
			GUI.getProjectMenu().remove();
			stopLiveCapture();
			if (TitlePanel.threadGetTitle != null) {
				TitlePanel.threadGetTitle.interrupt();//maybe null
			}//必须要先结束线程，否则获取数据的操作根本无法结束，因为线程一直通过sync占用资源
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}

		gui.getToolPanel().saveConfigToDisk();
		DomainPanel.autoSave();//域名面板自动保存逻辑有点复杂，退出前再自动保存一次
		saveDBfilepathToExtension();
	}

	//ITab必须实现的两个方法
	@Override
	public String getTabCaption() {
		return (ExtenderName.replaceAll(" ",""));
	}
	@Override
	public Component getUiComponent() {
		return gui.getContentPane();
	}

	@Override
	public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
		if (ToolPanel.DisplayContextMenuOfBurp.isSelected()) {
			return new LineEntryMenuForBurp().createMenuItemsForBurp(invocation);
		}else {
			return null;
		}
	}

	@Override
	public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
		if (toolFlag == IBurpExtenderCallbacks.TOOL_PROXY && !messageIsRequest) {
			liveinputQueue.add(messageInfo);
		}
	}

	public static void main(String[] args){
		while (true){
			int aaa = new Date().getMinutes();
			if (aaa % 5 == 0) {
				System.out.println(aaa);
			}
		}
	}
}