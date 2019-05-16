package burp;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.Arrays;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

public class LineTable extends JTable
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private LineTableModel lineTableModel;
	private IMessageEditor requestViewer;
	private IMessageEditor responseViewer;
	private BurpExtender burp;
	private TableRowSorter<LineTableModel> rowSorter;//TableRowSorter vs. RowSorter
	public JTabbedPane RequestPanel;
	public JTabbedPane ResponsePanel;
	private JSplitPane splitPane;//table area + detail area

	private int selectedRow = this.getSelectedRow();//to identify the selected row after search or hide lines

	public JSplitPane getSplitPane() {
		return splitPane;
	}

	public void setSplitPane(JSplitPane splitPane) {
		this.splitPane = splitPane;
	}

	public LineTable(LineTableModel lineTableModel,BurpExtender burp)
	{
		super(lineTableModel);
		this.lineTableModel = lineTableModel;
		this.burp = burp;
		this.setFillsViewportHeight(true);//在table的空白区域显示右键菜单
		//https://stackoverflow.com/questions/8903040/right-click-mouselistener-on-whole-jtable-component

		splitPane = new JSplitPane();//table area + detail area
		splitPane.setResizeWeight(0.5);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		//TitlePanel.add(splitPane, BorderLayout.CENTER); // getTitlePanel to get it

		JScrollPane scrollPaneRequests = new JScrollPane();//table area
		splitPane.setLeftComponent(scrollPaneRequests);
		scrollPaneRequests.setViewportView(this);

		JSplitPane RequestDetailPanel = new JSplitPane();//request and response
		RequestDetailPanel.setResizeWeight(0.5);
		splitPane.setRightComponent(RequestDetailPanel);

		RequestPanel = new JTabbedPane();
		RequestDetailPanel.setLeftComponent(RequestPanel);

		ResponsePanel = new JTabbedPane();
		RequestDetailPanel.setRightComponent(ResponsePanel);

		requestViewer = BurpExtender.callbacks.createMessageEditor(lineTableModel, false);
		responseViewer = BurpExtender.callbacks.createMessageEditor(lineTableModel, false);
		RequestPanel.addTab("Request", requestViewer.getComponent());
		ResponsePanel.addTab("Response", responseViewer.getComponent());
		tableinit();
		addClickSort();
		registerListeners();

	}

	public BurpExtender getBurp() {
		return burp;
	}

	public void tableinit(){
		//Font f = new Font("Arial", Font.PLAIN, 12);
		Font f = this.getFont();
		FontMetrics fm = this.getFontMetrics(f);
		int width = fm.stringWidth("A");//一个字符的宽度

		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("#")).setPreferredWidth(width*5);
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("#")).setMaxWidth(width*8);
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Status")).setPreferredWidth(width*"Status".length());
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Status")).setMaxWidth(width*("Status".length()+3));
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("isNew")).setPreferredWidth(width*"isNew".length());
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("isNew")).setMaxWidth(width*("isNew".length()+3));
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("isChecked")).setPreferredWidth(width*"isChecked".length());
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("isChecked")).setMaxWidth(width*("isChecked".length()+3));
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Length")).setPreferredWidth(width*10);
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Length")).setMaxWidth(width*15);
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("MIME Type")).setPreferredWidth(width*"MIME Type".length());
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("MIME Type")).setMaxWidth(width*("MIME Type".length()+3));
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Time")).setPreferredWidth(width*22);
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Time")).setMaxWidth(width*25);
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Text")).setPreferredWidth(width*0);//response text,for search
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Text")).setMaxWidth(width*0);//response text,for search
		this.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);


		rowSorter = new TableRowSorter<LineTableModel>(lineTableModel);//排序和搜索
		LineTable.this.setRowSorter(rowSorter);
	}

	@Override
	public void changeSelection(int row, int col, boolean toggle, boolean extend)
	{
		// show the log entry for the selected row
		LineEntry Entry = this.lineTableModel.getLineEntries().get(super.convertRowIndexToModel(row));
		requestViewer.setMessage(Entry.getRequest(), true);
		responseViewer.setMessage(Entry.getResponse(), false);
		this.lineTableModel.setCurrentlyDisplayedItem(Entry);
		super.changeSelection(row, col, toggle, extend);
	}

	@Override
	public LineTableModel getModel(){
		return (LineTableModel) super.getModel();
	}

	private void addClickSort() {
		//    	rowSorter = new TableRowSorter<LineTableModel>(lineTableModel);
		//		LineTable.this.setRowSorter(rowSorter); // tableinit()

		JTableHeader header = this.getTableHeader();
		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					LineTable.this.getRowSorter().getSortKeys().get(0).getColumn();
					////当Jtable中无数据时，jtable.getRowSorter()是nul
				} catch (Exception e1) {
					e1.printStackTrace(LineTable.this.burp.stderr);
				}
			}
		});
	}

	public void search(String keywork) {
		//rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + keywork));

		final RowFilter filter = new RowFilter() {
			@Override
			public boolean include(Entry entry) {
				//entry --- a non-null object that wraps the underlying object from the model
				int row = (int) entry.getIdentifier();
				LineEntry line = rowSorter.getModel().getLineEntries().get(row);

				if (BurpExtender.rdbtnHideCheckedItems.isSelected()&& line.isChecked()) {//to hide checked lines
					if (selectedRow == row) {
						selectedRow = row+1;
					}
					return false;
				}

				if (keywork.trim().length() == 0) {
					return true;
				} else {
					if (new String(line.getRequest()).toLowerCase().contains(keywork.toLowerCase())) {
						return true;
					}
					if (new String(line.getResponse()).toLowerCase().contains(keywork.toLowerCase())) {
						return true;
					}
					if (new String(line.getUrl()).toLowerCase().contains(keywork.toLowerCase())) {
						return true;
					}
					if (new String(line.getIP()).toLowerCase().contains(keywork.toLowerCase())) {
						return true;
					}
					if (new String(line.getCDN()).toLowerCase().contains(keywork.toLowerCase())) {
						return true;
					}
					if (new String(line.getComment()).toLowerCase().contains(keywork.toLowerCase())) {
						return true;
					}
					if (selectedRow== row) {
						selectedRow = row+1;
					}
					return false;
				}
			}
		};
		rowSorter.setRowFilter(filter);

		try {
			this.setRowSelectionInterval(selectedRow,selectedRow);
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	private void registerListeners(){
		final LineTable _this = this;
		this.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){//左键双击
					int[] rows = getSelectedRows();

					for (int i=0; i < rows.length; i++){
						rows[i] = convertRowIndexToModel(rows[i]);//转换为Model的索引，否则排序后索引不对应〿
					}
					Arrays.sort(rows);//升序

					//int row = ((LineTable) e.getSource()).rowAtPoint(e.getPoint()); // 获得行位置
					int col = ((LineTable) e.getSource()).columnAtPoint(e.getPoint()); // 获得列位置


					if ((col < LineTable.this.getColumnModel().getColumnIndex("Comments"))) {//last column----comments
						String host = LineTable.this.lineTableModel.getLineEntries().get(rows[0]).getHost();
						String url= "https://www.google.com/search?q=site%3A"+host;
						try {
							URI uri = new URI(url);
							Desktop desktop = Desktop.getDesktop();
							if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
								desktop.browse(uri);
							}
						} catch (Exception e2) {
							e2.printStackTrace();
						}
					}
				}
			}

			@Override
			public void mouseReleased( MouseEvent e ){
				if ( SwingUtilities.isRightMouseButton( e )){
					if (e.isPopupTrigger() && e.getComponent() instanceof JTable ) {
						//getSelectionModel().setSelectionInterval(rows[0], rows[1]);
						int[] rows = getSelectedRows();
						if (rows.length>0){
							for (int i=0; i < rows.length; i++){
								rows[i] = convertRowIndexToModel(rows[i]);//转换为Model的索引，否则排序后索引不对应〿
							}
							Arrays.sort(rows);//升序

							new LineEntryMenu(_this, rows).show(e.getComponent(), e.getX(), e.getY());
						}else{//在table的空白处显示右键菜单
							//https://stackoverflow.com/questions/8903040/right-click-mouselistener-on-whole-jtable-component
							//new LineEntryMenu(_this).show(e.getComponent(), e.getX(), e.getY());
						}
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				//no need
			}

		});

	}
}