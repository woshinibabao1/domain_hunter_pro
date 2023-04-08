package domain.target;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import GUI.GUIMain;
import assetSearch.Search;
import burp.BurpExtender;
import burp.Commons;
import burp.DomainNameUtils;
import burp.GrepUtils;
import burp.IPAddressUtils;
import config.ConfigPanel;
import domain.DomainManager;
import domain.DomainPanel;

public class TargetEntryMenu extends JPopupMenu {

	private static final long serialVersionUID = 1L;
	PrintWriter stdout = BurpExtender.getStdout();
	PrintWriter stderr = BurpExtender.getStderr();
	private TargetTable rootDomainTable;
	private GUIMain guiMain;

	public TargetEntryMenu(GUIMain guiMain,final TargetTable rootDomainTable, final int[] modelRows, final int columnIndex){
		this.rootDomainTable = rootDomainTable;
		this.guiMain = guiMain;

		JMenuItem getSubDomainsOf = new JMenuItem(new AbstractAction("Get All Subdomin Of This") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String results = "";
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					String line = guiMain.getDomainPanel().getDomainResult().fetchSubDomainsOf(rootDomain);
					results = results+System.lineSeparator()+line;
				}

				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(results);
				clipboard.setContents(selection, null);
			}
		});

		JMenuItem whoisItem = new JMenuItem(new AbstractAction("Whois") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					try {
						Commons.browserOpen("https://whois.chinaz.com/"+rootDomain,null);
						Commons.browserOpen("https://www.whois.com/whois/"+rootDomain,null);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		JMenuItem ASNInfoItem = new JMenuItem(new AbstractAction("ASN Info") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (int row:modelRows) {
					String target = (String) rootDomainTable.getTargetModel().getValueAt(row,0);

					try {
						//https://bgp.he.net/dns/shopee.com
						//https://bgp.he.net/net/143.92.111.0/24
						//https://bgp.he.net/ip/143.92.127.1
						String url =null;
						if (IPAddressUtils.isValidIP(target)){
							url = "https://bgp.he.net/ip/"+target;
						}
						if (IPAddressUtils.isValidSubnet(target)){
							url = "https://bgp.he.net/net/"+target;
						}
						if (DomainNameUtils.isValidDomain(target)){
							url = "https://bgp.he.net/dns/"+target;
						}
						if (url!= null){
							Commons.browserOpen(url,null);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		JMenuItem OpenWithBrowserItem = new JMenuItem(new AbstractAction("Open With Browser") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					try {
						Commons.browserOpen("https://"+rootDomain, guiMain.getConfigPanel().getLineConfig().getBrowserPath());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		JMenuItem batchAddCommentsItem = new JMenuItem(new AbstractAction("Add Comments") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String Comments = JOptionPane.showInputDialog("Comments", null).trim();
				while(Comments.trim().equals("")){
					Comments = JOptionPane.showInputDialog("Comments", null).trim();
				}
				rootDomainTable.getTargetModel().updateComments(modelRows,Comments);
			}
		});

		JMenuItem addToBlackItem = new JMenuItem(new AbstractAction("Add To Black List") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				guiMain.getDomainPanel().getControlPanel().selectedToBalck();
			}
		});

		/**
		 * 查找邮箱的搜索引擎
		 */
		JMenuItem SearchEmailOnHunterIOItem = new JMenuItem(new AbstractAction("Seach Email On hunter.io") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					String url= "https://hunter.io/try/search/%s";
					//https://hunter.io/try/search/shopee.com?locale=en
					url= String.format(url, rootDomain);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});


		JMenuItem SearchOnFoFaItem = new JMenuItem(new AbstractAction("Seach On fofa.info") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					rootDomain = new String(Base64.getEncoder().encode(rootDomain.getBytes()));
					String url= "https://fofa.info/result?qbase64=%s";
					url= String.format(url, rootDomain);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});



		JMenuItem SearchOnShodanItem = new JMenuItem(new AbstractAction("Seach On shodan.io") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					rootDomain = URLEncoder.encode(rootDomain);
					String url= "https://www.shodan.io/search?query=%s";
					//https://www.shodan.io/search?query=baidu.com
					url= String.format(url, rootDomain);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});

		//360quake,zoomeye,hunter,shodan
		//https://quake.360.net/quake/#/searchResult?searchVal=baidu.com
		JMenuItem SearchOn360QuakeItem = new JMenuItem(new AbstractAction("Seach On quake.360.net") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					rootDomain = URLEncoder.encode(rootDomain);
					String url= "https://quake.360.net/quake/#/searchResult?searchVal=%s";
					url= String.format(url, rootDomain);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});

		//https://ti.qianxin.com/v2/search?type=domain&value=example.com
		JMenuItem SearchOnTiQianxinItem = new JMenuItem(new AbstractAction("Seach On ti.qianxin.com") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					rootDomain = URLEncoder.encode(rootDomain);
					String url= "https://ti.qianxin.com/v2/search?type=domain&value=%s";
					url= String.format(url, rootDomain);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});


		//https://hunter.qianxin.com/list?search=domain%3D%22example.com%22
		JMenuItem SearchOnHunterQianxinItem = new JMenuItem(new AbstractAction("Seach On hunter.qianxin.com") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					String domainPara = String.format("domain=\"%s\"",rootDomain);
					domainPara = URLEncoder.encode(domainPara);
					String url= "https://hunter.qianxin.com/list?search=%s";
					url= String.format(url, domainPara);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});


		//https://quake.360.net/quake/#/searchResult?searchVal=favicon%3A%20%22c5618c85980459ce4325eb324428d622%22


		JMenuItem SearchOnZoomEyeItem = new JMenuItem(new AbstractAction("Seach On zoomeye.org") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					rootDomain = URLEncoder.encode(rootDomain);
					String url= "https://www.zoomeye.org/searchResult?q=%s";
					url= String.format(url, rootDomain);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});


		JMenuItem SearchOnFoFaAutoItem = new JMenuItem(new AbstractAction("Auto Search On fofa.info") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						String email = ConfigPanel.textFieldFofaEmail.getText();
						String key = ConfigPanel.textFieldFofaKey.getText();
						if (email.equals("") ||key.equals("")) {
							stdout.println("fofa.info emaill or key not configurated!");
							return null;
						}
						for (int row:modelRows) {
							String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
							String rootDomainPara = new String(Base64.getEncoder().encode(rootDomain.getBytes()));
							String responseBody = Search.searchFofa(email,key,rootDomainPara);

							Set<String> domains = GrepUtils.grepDomain(responseBody);
							List<String> iplist = GrepUtils.grepIP(responseBody);
							stdout.println(String.format("%s: %s sub-domain names; %s ip addresses found by fofa.info",rootDomain,domains.size(),iplist.size()));
							guiMain.getDomainPanel().getDomainResult().addIfValid(domains);
							guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets().addAll(iplist);
						}
						return null;
					}

					@Override
					protected void done(){
					}
				};
				worker.execute();
			}
		});
		
		
		JMenuItem SearchOnQuakeAutoItem = new JMenuItem(new AbstractAction("Auto Search On quake.360.net") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						String key = ConfigPanel.textFieldQuakeAPIKey.getText();
						if (key.equals("")) {
							stdout.println("quake.360.net API key not configurated!");
							return null;
						}
						for (int row:modelRows) {
							String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
							String responseBody = Search.searchQuake(key,rootDomain);

							Set<String> domains = GrepUtils.grepDomain(responseBody);
							List<String> iplist = GrepUtils.grepIP(responseBody);
							stdout.println(String.format("%s: %s sub-domain names; %s ip addresses found by quake.360.net",rootDomain,domains.size(),iplist.size()));
							guiMain.getDomainPanel().getDomainResult().addIfValid(domains);
							guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets().addAll(iplist);
						}
						return null;
					}

					@Override
					protected void done(){
					}
				};
				worker.execute();
			}
		});

		
		JMenuItem SearchOnHunterAutoItem = new JMenuItem(new AbstractAction("Auto Search On hunter.qianxin.com") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						String key = ConfigPanel.textFieldHunterAPIKey.getText();
						if (key.equals("")) {
							stdout.println("hunter.qianxin.com API key not configurated!");
							return null;
						}
						for (int row:modelRows) {
							String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
							String responseBody = Search.searchHunter(key,rootDomain);

							Set<String> domains = GrepUtils.grepDomain(responseBody);
							List<String> iplist = GrepUtils.grepIP(responseBody);
							stdout.println(String.format("%s: %s sub-domain names; %s ip addresses found by hunter.qianxin.com",rootDomain,domains.size(),iplist.size()));
							guiMain.getDomainPanel().getDomainResult().addIfValid(domains);
							guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets().addAll(iplist);
						}
						return null;
					}

					@Override
					protected void done(){
					}
				};
				worker.execute();
			}
		});


		this.add(getSubDomainsOf);
		this.add(batchAddCommentsItem);
		this.add(addToBlackItem);
		this.addSeparator();

		this.add(SearchOnFoFaItem);
		this.add(SearchOn360QuakeItem);
		this.add(SearchOnTiQianxinItem);
		this.add(SearchOnHunterQianxinItem);
		this.add(SearchOnZoomEyeItem);
		this.add(SearchOnShodanItem);
		this.addSeparator();

		this.add(SearchOnFoFaAutoItem);
		this.add(SearchOnQuakeAutoItem);
		this.add(SearchOnHunterAutoItem);
		this.addSeparator();

		this.add(OpenWithBrowserItem);
		this.add(whoisItem);
		this.add(ASNInfoItem);
		this.add(SearchEmailOnHunterIOItem);
		this.addSeparator();
	}

}
