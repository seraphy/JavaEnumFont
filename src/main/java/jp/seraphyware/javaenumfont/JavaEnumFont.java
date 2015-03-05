package jp.seraphyware.javaenumfont;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * フォントおよびキャラクターセットの一覧を表示し、 使用可能なフォント等を検証するためのテスト用アプリケーション.<br>
 * そのほかシステムプロパティや環境変数を確認することも可能としている.<br>
 */
public class JavaEnumFont extends JFrame {

    /**
     * コンストラクタ
     */
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

    /**
     * 画面を構成する.
     */
    private void initLayout() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAllFonts();

        final TableModel fontsTableModel = createFontsTableModel(fonts);
        final TableModel charsetsTableModel = createCharsetTableModel();
        final TableModel sysPropTableModel = createSysPropTableModel();
        final TableModel envTableModel = createEnvTableModel();

        final ExportablePanel[] panels = {
            createFontsPanel(fonts, fontsTableModel),
            createCharsetPanel(charsetsTableModel),
            createSysPropPanel(sysPropTableModel),
            createEnvPanel(envTableModel)
        };

        JTabbedPane tabPanel = new JTabbedPane();
        for (ExportablePanel panel : panels) {
            tabPanel.add(panel.getTitle(), panel);
        }

        contentPane.add(tabPanel, BorderLayout.CENTER);

        Exportable exp = new Exportable() {
            @Override
            public void export(Writer wr) throws IOException {
                for (ExportablePanel panel : panels) {
                    panel.export(wr);
                }
            }
        };

        setJMenuBar(createMenuBar(exp));
    }

    /**
     * テーブルをもつエクスポート可能なパネルの抽象実装
     */
    private static abstract class ExportablePanel extends JPanel implements Exportable {

        private static final String newline = System.getProperty("line.separator");

        public abstract JTable getTable();

        public abstract String getTitle();

        @Override
        public void export(Writer wr) throws IOException {
            wr.write("[");
            wr.write(getTitle());
            wr.write("]");
            wr.write(newline);

            JTable table = getTable();

            int rowmx = table.getRowCount();
            int colmx = table.getColumnCount();
            for (int row = 0; row < rowmx; row++) {
                for (int col = 0; col < colmx; col++) {
                    Object val = table.getValueAt(row, col);
                    if (val == null) {
                        val = "";
                    }
                    if (col != 0) {
                        wr.write("\t");
                    }
                    wr.write(val.toString());
                }
                wr.write(newline);
            }
            wr.write(newline);
        }

    }

    /**
     * エクスポート可能であることを示すインターフェイス
     */
    private interface Exportable {

        void export(Writer wr) throws IOException;

    }

    /**
     * フォント表示用テーブルモデルを作成して返す.
     *
     * @param fonts
     * @return
     */
    private TableModel createFontsTableModel(final Font[] fonts) {

        final HashMap<String, String> defaultFamilies = new HashMap<>();
        final HashMap<String, String> defaultNames = new HashMap<>();

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

        final String[] columnNames = {"Family", "Name", "Posix", "Japanese",
            "UI Default"};

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

                    case 3: {
                        ArrayList<String> titles = new ArrayList<>();
                        if (font.canDisplay('あ')) {
                            titles.add("ひらがな");
                        }
                        if (font.canDisplay('あ') && font.canDisplay('・')) {
                            titles.add("日本語");
                        }
                        if (font.canDisplay(0x29E3D)) {
                            titles.add("サロゲート");
                        }
                        if (font.canDisplay('编')) {
                            titles.add("簡体字");
                        }
                        if (font.canDisplay(0x29E3D) && font.canDisplay(0x1F623)) {
                            titles.add("絵文字");
                        }

                        StringBuilder buf = new StringBuilder();
                        for (String title : titles) {
                            if (buf.length() > 0) {
                                buf.append(", ");
                            }
                            buf.append(title);
                        }
                        ret = buf.toString();
                        break;
                    }

                    case 4: {
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
        return model;
    }

    /**
     * フォント選択パネルを作成して返す.
     *
     * @param fonts
     * @param model
     * @return
     */
    private ExportablePanel createFontsPanel(final Font[] fonts, TableModel model) {

        final JTable fontTable = new JTable(model);
        fontTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        fontTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fontTable.setAutoCreateRowSorter(true);

        adjustColumns(fontTable);

        final JTextArea textArea = new JTextArea();
        String sample = loadText();
        textArea.setText(sample);

        final SpinnerNumberModel fontSizeModel = new SpinnerNumberModel(10, 4,
                48, 1);
        final JSpinner fontSize = new JSpinner(fontSizeModel);

        final JPanel samplePanel = new JPanel(new BorderLayout());

        final JPanel fontSizePanel = new JPanel(new FlowLayout());
        fontSizePanel.add(new JLabel("Font size: "));
        fontSizePanel.add(fontSize);

        samplePanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        samplePanel.add(fontSizePanel, BorderLayout.NORTH);

        final Runnable redrawSample = new Runnable() {
            @Override
            public void run() {
                int vRow = fontTable.getSelectedRow();
                if (vRow >= 0) {
                    int mRow = fontTable.convertRowIndexToModel(vRow);
                    Font font = fonts[mRow];
                    int fontSize = (Integer) fontSizeModel.getValue();
                    Font font2 = new Font(font.getFamily(), font.getStyle(),
                            fontSize);
                    System.out.println("font2=" + font2);
                    textArea.setFont(font2);
                }
            }
        };

        fontTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
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

        ExportablePanel panel = new ExportablePanel() {
            @Override
            public String getTitle() {
                return "Fonts";
            }

            @Override
            public JTable getTable() {
                return fontTable;
            }
        };
        panel.setLayout(new BorderLayout());

        JScrollPane scFontTable = new JScrollPane(fontTable);

        JSplitPane fontsPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                scFontTable, samplePanel);
        panel.add(fontsPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * キャラクターセット表示用テーブルモデルを作成して返す.
     *
     * @return
     */
    private TableModel createCharsetTableModel() {
        final Charset defaultCharset = Charset.defaultCharset();

        final ArrayList<Charset> charsets = new ArrayList<>();
        for (Map.Entry<String, Charset> entry : Charset.availableCharsets().entrySet()) {
            charsets.add(entry.getValue());
        }

        final String[] columnNames = {"Name", "Alias", "Default"};

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
                return charsets.size();
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
                Charset charset = charsets.get(row);
                String ret = null;
                switch (column) {
                    case 0:
                        ret = charset.displayName();
                        break;

                    case 1: {
                        StringBuilder buf = new StringBuilder();
                        for (String name : charset.aliases()) {
                            if (buf.length() > 0) {
                                buf.append(", ");
                            }
                            buf.append(name);
                        }
                        ret = buf.toString();
                    }
                    break;

                    case 2:
                        if (charset.equals(defaultCharset)) {
                            ret = "<DEFAULT CHARSET>";
                        }
                        break;

                }
                return ret;
            }
        };
        return model;
    }

    /**
     * キャラクターセット表示用パネルを作成して返す.
     *
     * @param model
     * @return
     */
    private ExportablePanel createCharsetPanel(TableModel model) {
        final JTable charsetTable = new JTable(model);
        charsetTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        charsetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        charsetTable.setAutoCreateRowSorter(true);

        adjustColumns(charsetTable);

        JScrollPane scr = new JScrollPane(charsetTable);

        ExportablePanel panel = new ExportablePanel() {
            @Override
            public JTable getTable() {
                return charsetTable;
            }

            @Override
            public String getTitle() {
                return "Charsets";
            }
        };
        panel.setLayout(new BorderLayout());
        panel.add(scr, BorderLayout.CENTER);
        return panel;
    }

    /**
     * システムプロパティ用テーブルモデルを作成して返す.
     *
     * @return
     */
    private TableModel createSysPropTableModel() {
        final ArrayList<String> names = new ArrayList<>();
        final Properties prop = System.getProperties();
        Enumeration<?> enm = prop.propertyNames();
        while (enm.hasMoreElements()) {
            String name = (String) enm.nextElement();
            names.add(name);
        }
        Collections.sort(names);

        final String[] columnNames = {"Name", "Value"};

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
                return names.size();
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
                String ret = "";
                String name = names.get(row);
                switch (column) {
                    case 0:
                        ret = name;
                        break;

                    case 1:
                        ret = prop.getProperty(name);
                        break;
                }
                return ret;
            }
        };
        return model;
    }

    /**
     * システムプロパティパネルを作成して返す.
     *
     * @param model
     * @return
     */
    private ExportablePanel createSysPropPanel(TableModel model) {
        final JTable sysPropTable = new JTable(model);
        sysPropTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        sysPropTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sysPropTable.setAutoCreateRowSorter(true);

        adjustColumns(sysPropTable);

        JScrollPane scr = new JScrollPane(sysPropTable);

        ExportablePanel panel = new ExportablePanel() {
            @Override
            public JTable getTable() {
                return sysPropTable;
            }

            @Override
            public String getTitle() {
                return "System Properties";
            }
        };
        panel.setLayout(new BorderLayout());
        panel.add(scr, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 環境変数表示用のテーブルモデルを作成して返す.
     *
     * @return
     */
    private TableModel createEnvTableModel() {
        final ArrayList<String> names = new ArrayList<>();
        final Map<?, ?> prop = System.getenv();
        for (Map.Entry<?, ?> entry : prop.entrySet()) {
            String name = (String) entry.getKey();
            names.add(name);
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);

        final String[] columnNames = {"Name", "Value"};

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
                return names.size();
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
                String ret = "";
                String name = names.get(row);
                switch (column) {
                    case 0:
                        ret = name;
                        break;

                    case 1:
                        ret = (String) prop.get(name);
                        break;
                }
                return ret;
            }
        };
        return model;
    }

    /**
     * 環境変数表示パネルを作成して返す.
     *
     * @param model
     * @return
     */
    private ExportablePanel createEnvPanel(TableModel model) {
        final JTable envTable = new JTable(model);
        envTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        envTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        envTable.setAutoCreateRowSorter(true);

        adjustColumns(envTable);

        JScrollPane scr = new JScrollPane(envTable);
        ExportablePanel panel = new ExportablePanel() {
            @Override
            public JTable getTable() {
                return envTable;
            }

            @Override
            public String getTitle() {
                return "Environments";
            }
        };
        panel.setLayout(new BorderLayout());
        panel.add(scr, BorderLayout.CENTER);
        return panel;
    }

    /**
     * メニューを構築する
     *
     * @param exp Saveコマンド用のハンドラ
     * @return
     */
    private JMenuBar createMenuBar(final Exportable exp) {
        Toolkit tk = Toolkit.getDefaultToolkit();
        final int shortcutKey = tk.getMenuShortcutKeyMask();

        JMenuBar menubar = new JMenuBar();

        JMenu menuFile = new JMenu("File");
        menuFile.setMnemonic('F');
        menubar.add(menuFile);

        JMenuItem menuSave = new JMenuItem(new AbstractAction("Save") {
            {
                putValue(ACCELERATOR_KEY,
                        KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKey));
                putValue(MNEMONIC_KEY, (int) 'S');
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                onSave(exp);
            }
        });
        menuFile.add(menuSave);

        menuFile.add(new JSeparator());

        JMenuItem menuExit = new JMenuItem(new AbstractAction("Close") {
            {
                putValue(ACCELERATOR_KEY,
                        KeyStroke.getKeyStroke(KeyEvent.VK_W, shortcutKey));
                putValue(MNEMONIC_KEY, (int) 'C');
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        });
        menuFile.add(menuExit);

        return menubar;
    }

    /**
     * テーブルの情報をファイルに保存する.
     *
     * @param exp
     */
    private void onSave(Exportable exp) {
        JFileChooser chooser = new JFileChooser() {
            /**
             * OKボタン押下時の処理.
             */
            @Override
            public void approveSelection() {
                File outFile = getSelectedFile();
                if (outFile == null) {
                    return;
                }

                // ファイルが存在すれば上書き確認する.
                if (outFile.exists()) {
                    if (JOptionPane.showConfirmDialog(this, "confirmOverwrite",
                            "confirm", JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                super.approveSelection();
            }
        };
        chooser.setSelectedFile(new File("report.txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            Charset cs = Charset.forName("UTF-8");
            try (FileOutputStream os = new FileOutputStream(file);
                    Writer wr = new OutputStreamWriter(os, cs)) {
                exp.export(wr);

            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.toString());
        }
    }

    /**
     * 終了する.
     */
    protected void onClose() {
        dispose();
    }

    /**
     * 最適なカラムの幅をデータから算定する.<br>
     * http://tips4java.wordpress.com/2008/11/10/table-column-adjuster/
     *
     * @param table
     */
    private void adjustColumns(JTable table) {
        for (int column = 0; column < table.getColumnCount(); column++) {
            TableColumn tableColumn = table.getColumnModel().getColumn(column);
            int preferredWidth = tableColumn.getMinWidth();
            int maxWidth = tableColumn.getMaxWidth();

            // ヘッダの最小の列幅の算定
            TableCellRenderer headerRenderer = tableColumn.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            Component hCompo = headerRenderer.getTableCellRendererComponent(
                    table, tableColumn.getHeaderValue(), false, false, -1,
                    column);
            preferredWidth = Math.max(preferredWidth,
                    hCompo.getPreferredSize().width);

            // すべてのデータを表示できる最小の列幅の算定 + 10px
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer cellRenderer = table.getCellRenderer(row,
                        column);
                Component c = table.prepareRenderer(cellRenderer, row, column);
                int width = c.getPreferredSize().width
                        + table.getIntercellSpacing().width;

                width += 10; // 余白分を加算する (ぴっちり詰めると見づらいため)

                preferredWidth = Math.max(preferredWidth, width);

                if (preferredWidth >= maxWidth) {
                    // 最大幅を超えた場合は、これ以上チェックする必要はない.
                    preferredWidth = maxWidth;
                    break;
                }
            }

            tableColumn.setPreferredWidth(preferredWidth);
        }
    }

    /**
     * サンプル用テキストをロードする
     *
     * @return
     */
    private String loadText() {
        ProtectionDomain pd = getClass().getProtectionDomain();
        CodeSource codeSource = pd.getCodeSource();

        File baseDir = new File(".");
        if (codeSource != null) {
            URL codeBaseUrl = codeSource.getLocation();
            if (codeBaseUrl.getProtocol().equals("file")) {
                baseDir = new File(codeBaseUrl.getPath()).getParentFile();
            }
        }

        File file = new File(baseDir, "sample.txt");
        if (file.exists()) {
            try {
                Charset cs = Charset.forName("UTF-8");
                try (FileInputStream fis = new FileInputStream(file);
                        InputStreamReader rd = new InputStreamReader(fis, cs)) {
                    StringBuilder buf = new StringBuilder();
                    int c;
                    while ((c = rd.read()) != -1) {
                        buf.append((char) c);
                    }
                    return buf.toString();

                }

            } catch (Exception ex) {
                return ex.getMessage();
            }
        }

        // デフォルト
        return "123456789ABCDEFG\r\n"
                + "あいうえおかきくけこ がぎぐげご\r\n"
                + "森鷗外\r\n"
                + "・\r\n"
                + "繫がる\r\n"
                + "编辑-预设-打开配置-保存为图片-伪春菜\r\n"
                + "[𩸽]\r\n"
                + "[🎂]";
    }

    /**
     * エントリポイント
     *
     * @param args
     * @throws Exception
     */
    public static void main(String... args) throws Exception {

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        if (System.getProperty("os.name").contains("Mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

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
