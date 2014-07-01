package jp.seraphyware.enumfont;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;

public class JavaEnumFont extends JFrame {
	
	public JavaEnumFont() {
		try {
			setTitle(getClass().getSimpleName());
			
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					onClose();
				}
			});
			
			initLayout();
			
			pack();
			
		} catch (RuntimeException ex) {
			dispose();
			throw ex;
		}
	}
	
	private void initLayout() {
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		final Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		
		final HashMap<String, String> defaultFamilies = new HashMap<String, String>();
		final HashMap<String, String> defaultNames = new HashMap<String, String>();
		
		Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value != null && value instanceof FontUIResource) {
				FontUIResource fr = (FontUIResource) value;
				String family = fr.getFamily(Locale.ENGLISH);
				String name = fr.getName();
				
				String keyName = key.toString();
				
				String prevFamily = defaultFamilies.get(family);
				if (prevFamily == null) {
					prevFamily = keyName;
				} else {
					prevFamily = prevFamily + ", " + keyName;
				}
				defaultFamilies.put(family, prevFamily);

				String prevName = defaultNames.get(name);
				if (prevName == null) {
					prevName = keyName;
				} else {
					prevName = prevName + ", " + keyName;
				}
				defaultNames.put(name, prevName);
			}
		}

		final String[] columnNames = {"Family", "Name", "Posix", "Japanese", "UI Default"};
		
		DefaultTableModel model = new DefaultTableModel() {
			@Override
			public int getColumnCount() {
				return columnNames.length;
			}
			
			@Override
			public String getColumnName(int column) {
				return columnNames[column];
			}
			
			@Override
			public int getRowCount() {
				return fonts.length;
			}
			
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return String.class;
			}
			
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
			
			@Override
			public Object getValueAt(int row, int column) {
				Font font = fonts[row];
				String ret = null;
				switch (column) {
					case 0:
						ret = font.getFamily(Locale.ENGLISH);
						break;

					case 1:
						ret = font.getName();
						break;

					case 2:
						ret = font.getPSName();
						break;
						
					case 3:
					{
						if (font.canDisplay(0x29E3D) && font.canDisplay(0x1F623)) {
							ret = "(1) 絵文字可";
						} else if (font.canDisplay(0x29E3D)) {
							ret = "(2) サロゲート可";
						} else if (font.canDisplay('あ') && font.canDisplay('・')) {
							ret = "(3) 日本語可";
						} else if (font.canDisplay('あ')) {
							ret = "(4) ひらがな可";
						} else {
							ret = "";
						}
						break;
					}

					case 4:
					{
						ret = defaultFamilies.get(font.getFamily(Locale.ENGLISH));
						if (ret == null) {
							ret = defaultNames.get(font.getName());
						}
						break;
					}
				}
				return ret;
			}
		};
		
		final JTable tbl = new JTable(model);
		tbl.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JScrollPane scr = new JScrollPane(tbl);
		contentPane.add(scr, BorderLayout.CENTER);
		
		final JTextArea textArea = new JTextArea();
		textArea.setText("あいうえおかきくけこ\r\n森鷗外・繫がる \r\n123456789\r\nABCDEFG\r\n[\ud867\ude3d] [\ud83c\udf82]");
		
		final SpinnerNumberModel fontSizeModel = new SpinnerNumberModel(10, 4, 48, 1);
		final JSpinner fontSize = new JSpinner(fontSizeModel);
		
		final JPanel samplePanel = new JPanel(new BorderLayout());
		samplePanel.add(textArea, BorderLayout.CENTER);
		samplePanel.add(fontSize, BorderLayout.EAST);
		
		contentPane.add(samplePanel, BorderLayout.SOUTH);

		final Runnable redrawSample = new Runnable() {
			@Override
			public void run() {
				int row = tbl.getSelectedRow();
				if (row >= 0) {
					Font font = fonts[row];
					int fontSize = (Integer) fontSizeModel.getValue();
					Font font2 = new Font(font.getFamily(), font.getStyle(), fontSize);
					System.out.println("font2=" + font2);
					textArea.setFont(font2);
				}
			}
		};
		
		tbl.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					redrawSample.run();
				}
			}
		});
		
		fontSize.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				redrawSample.run();
			}
		});
	}
	
	protected void onClose() {
		dispose();
	}
	
	public static void main(String... args) throws Exception {
		
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JavaEnumFont main = new JavaEnumFont();
				main.setLocationByPlatform(true);
				main.setVisible(true);
			}
		});
	}
	
}
