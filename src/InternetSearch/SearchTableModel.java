package InternetSearch;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import GUI.GUIMain;
import base.IndexedHashMap;
import burp.BurpExtender;
import burp.IPAddressUtils;


public class SearchTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	private IndexedHashMap<String,SearchResultEntry> lineEntries =new IndexedHashMap<String,SearchResultEntry>();

	PrintWriter stdout;
	PrintWriter stderr;
	private GUIMain guiMain;
	public static final List<String> HeadList = SearchTableHead.getTableHeadList();


	public SearchTableModel(GUIMain guiMain){
		this.guiMain = guiMain;
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}
	}

	public SearchTableModel(GUIMain guiMain,IndexedHashMap<String,SearchResultEntry> lineEntries){
		this(guiMain);
		this.lineEntries = lineEntries;
	}

	public SearchTableModel(GUIMain guiMain,List<SearchResultEntry> entries){
		this(guiMain);
		for (SearchResultEntry entry:entries) {
			lineEntries.put(entry.getIdentify(), entry);
		}
	}

	////////getter setter//////////


	public IndexedHashMap<String, SearchResultEntry> getLineEntries() {
		return lineEntries;
	}

	public void setLineEntries(IndexedHashMap<String, SearchResultEntry> lineEntries) {
		this.lineEntries = lineEntries;
	}


	public SearchResultEntry getRowAt(int rowIndex) {
		return getLineEntries().get(rowIndex);
	}

	////////////////////// extend AbstractTableModel////////////////////////////////

	@Override
	public int getColumnCount()
	{
		return HeadList.size();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		if (columnIndex == HeadList.indexOf(SearchTableHead.Index)) {
			return Integer.class;//id
		}
		if (columnIndex == HeadList.indexOf(SearchTableHead.Favicon)) {
			return ImageIcon.class;
		}
		return String.class;
	}

	@Override
	public int getRowCount()
	{
		return lineEntries.size();
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex >= 0 && columnIndex <= HeadList.size()) {
			return HeadList.get(columnIndex);
		}else {
			return "";
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}


	public int getColumnIndexByName(String Name) {
		return HeadList.indexOf(Name);
	}


	public List<SearchResultEntry> getEntries(int[] rowIndexes)
	{
		List<SearchResultEntry> result = new ArrayList<>();
		for (int rowIndex:rowIndexes) {
			SearchResultEntry entry = lineEntries.get(rowIndex);
			result.add(entry);
		}
		return result;
	}


	public List<String> getMultipleValue(int[] rowIndexes, String columnName)
	{
		List<String> result = new ArrayList<>();
		int columnIndex = getColumnIndexByName(columnName);
		if (columnIndex < 0) {
			result.add("Wrong column name to get value");
		}
		for (int rowIndex:rowIndexes) {
			result.add((String)getValueAt(rowIndex,columnIndex));
		}
		return result;
	}


	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (rowIndex >= lineEntries.size()) {
			return "IndexOutOfBoundsException";
		}
		SearchResultEntry entry = lineEntries.get(rowIndex);
		if (columnIndex == HeadList.indexOf(SearchTableHead.Index)) {
			return rowIndex;
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.URL)){
			return entry.getIdentify();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Protocol)){
			return entry.getProtocol();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Host)){
			return entry.getHost();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Port)){
			return entry.getPort();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Title)){
			return entry.getTitle();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Server)){
			return entry.getWebcontainer();
		}

		else if (columnIndex == HeadList.indexOf(SearchTableHead.IP)){
			return String.join(",", new TreeSet<String>(entry.getIPSet()));//用TreeSet进行排序
		}

		else if (columnIndex == HeadList.indexOf(SearchTableHead.Favicon)){
			//return entry.getIcon_hash();
			byte[] data = entry.getIcon_bytes();
			String hash = entry.getIcon_hash();
			//排序比较是获取对象的toString()结果进行的。当ImageIcon有描述description时，toString()的值就是description。
			//所以传递hash作为描述，可以实现图标的点击排序，还和hash的排序一致
			if (data != null) {
				return new ImageIcon(data,hash);
			}
			return null;
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.IconHash)){
			return entry.getIcon_hash();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.CertInfo)){
			return String.join(",", new TreeSet<String>(entry.getCertDomainSet()));
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.ASNInfo)){
			return entry.getASNInfo();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Source)) {
			return entry.getSource();
		}
		return "";
	}


	/**
	 * 返回可以用于网络搜索引擎进行搜索的字段
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public String getValueForSearch(int rowIndex, int columnIndex,String engine)
	{
		if (rowIndex >= lineEntries.size()) {
			return null;
		}
		SearchResultEntry entry = lineEntries.get(rowIndex);
		if (columnIndex == HeadList.indexOf(SearchTableHead.Port)){
			return entry.getPort()+"";
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Title)){
			String value =  entry.getTitle();
			value = SearchEngine.buildSearchDork(value,engine,SearchType.Title);
			return value;
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Server)){
			String value =  entry.getWebcontainer();
			value = SearchEngine.buildSearchDork(value,engine,SearchType.Server);
			return value;
		}else if (columnIndex == HeadList.indexOf(SearchTableHead.IP)){
			String value = String.join(",", new TreeSet<String>(entry.getIPSet()));//用TreeSet进行排序
			value = SearchEngine.buildSearchDork(value,engine,SearchType.IP);
			return value;
		}else if (columnIndex == HeadList.indexOf(SearchTableHead.Favicon) || columnIndex == HeadList.indexOf(SearchTableHead.IconHash)){
			String value = entry.getIcon_hash();
			value = SearchEngine.buildSearchDork(value,engine,SearchType.IconHash);
			return value;
		}else if (columnIndex == HeadList.indexOf(SearchTableHead.CertInfo)){
			String value =  String.join(",", new TreeSet<String>(entry.getCertDomainSet()));
			value = SearchEngine.buildSearchDork(value,engine,SearchType.Domain);
			return value;
		}else if (columnIndex == HeadList.indexOf(SearchTableHead.ASNInfo)){
			//TODO，应该获取ASN编号
			return entry.getASNInfo();
		}else {
			String value = entry.getHost();
			if (IPAddressUtils.isValidIP(value)) {
				value = SearchEngine.buildSearchDork(value,engine,SearchType.IP);
			}else {
				value = SearchEngine.buildSearchDork(value,engine,SearchType.Domain);
			}
			return value;
		}
	}


	///////////////////^^^多个行内容的增删查改^^^/////////////////////////////////

	public void addNewEntry(SearchResultEntry entry){
		if (entry == null) {
			return;
		}
		String key = entry.getIdentify();
		SearchResultEntry ret = lineEntries.put(key,entry);
		//以前的做法是，put之后再次统计size来判断是新增还是替换，这种方法在多线程时可能不准确，
		//concurrentHashMap的put方法会在替换时返回原来的值，可用于判断是替换还是新增
		int index = lineEntries.IndexOfKey(key);
		if (ret == null) {
			try {
				fireTableRowsInserted(index, index);
			} catch (Exception e) {
				//出错只会暂时影响显示，不影响数据内容，不再打印
				//e.printStackTrace(BurpExtender.getStderr());
				//BurpExtender.getStderr().println("index: "+index+" url: "+key);
			}
			//这里偶尔出现IndexOutOfBoundsException错误,
			// 但是debug发现javax.swing.DefaultRowSorter.checkAgainstModel在条件为false时(即未越界)抛出了异常，奇怪！
		}else {
			fireTableRowsUpdated(index, index);
		}
	}

	public static void main(String[] args) {
		System.out.println(HeadList);
	}
}