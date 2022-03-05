package tools.xor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.dbcp2.BasicDataSource;
import org.json.JSONArray;
import org.json.JSONObject;
import tools.xor.providers.jdbc.JDBCDataModel;
import tools.xor.service.AggregateManager;
import tools.xor.service.DefaultDataModelFactory;
import tools.xor.service.JDBCConfigDataModelBuilder;
import tools.xor.service.exim.CSVLoader;
import tools.xor.util.ClassUtil;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import javax.sql.DataSource;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class Configurator extends JPanel implements CallbackHandler
{

    private String[] columnNames = { "Country", "Capital", "Population in Millions", "Democracy" };

    private Object[][] data = { { "USA", "Washington DC", 280, true },
        { "Canada", "Ottawa", 32, true }, { "United Kingdom", "London", 60, true },
        { "Germany", "Berlin", 83, true }, { "France", "Paris", 60, true },
        { "Norway", "Oslo", 4.5, true }, { "India", "New Delhi", 1046, true } };

    private static final Font[] fonts = new Font[7];
    private static int fontLevel = 2;

    static {
        fonts[0] = new Font("Lucida Grande", Font.TRUETYPE_FONT, 11);
        fonts[1] = new Font("Lucida Grande", Font.TRUETYPE_FONT, 12);
        fonts[2] = new Font("Lucida Grande", Font.TRUETYPE_FONT, 13);
        fonts[3] = new Font("Lucida Grande", Font.TRUETYPE_FONT, 14);
        fonts[4] = new Font("Lucida Grande", Font.TRUETYPE_FONT, 15);
        fonts[5] = new Font("Lucida Grande", Font.TRUETYPE_FONT, 16);
        fonts[6] = new Font("Lucida Grande", Font.TRUETYPE_FONT, 17);
    }

    private static BasicDataSource ds;
    private static JLabel statusLabel;
    private static final Deque<String> status = new ConcurrentLinkedDeque<String>();

    private static final String GENERATORS_FILENAME = "Generators.xml";
    private static Generators generators;

    private static JDBCDataModel dataModel;
    private static AggregateManager xor;
    private static tools.xor.service.Shape shape;

    private JTextField tableField;
    private JTextField dateField;
    private JTextField jtfFind;
    private JTextField jsonSchema;
    private DefaultTableModel resultModel;
    private JTable resultTable;
    private JPanel columnGenPanel;
    private JPanel dependsOnPanel;
    private JPanel foreignKeysPanel;
    private ColumnGenEntry entityGenerator;
    private JButton newEntityGenBtn;

    public static JDBCDataModel getDataModel ()
    {
        if (dataModel == null && xor != null) {
            dataModel = (JDBCDataModel)xor.getDataModel();
            shape = dataModel.getShape();
        }

        return dataModel;
    }

    public Configurator ()
    {
        try {

            // main panel
            setBorder(new EmptyBorder(10, 10, 10, 10));
            BoxLayout boxLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
            setLayout(boxLayout);

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createTopPanel(),
                createResultPanel());

            add(splitPane);

            loadGenerators();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadGenerators() {
        try {
            InputStream stream = Configurator.class.getClassLoader().getResourceAsStream(GENERATORS_FILENAME);
            if(stream == null) {
                throw new RuntimeException("Unable to find the view configuration file: " + GENERATORS_FILENAME);
            }

            JAXBContext jaxbContext = JAXBContext.newInstance(Generators.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            jaxbUnmarshaller.setEventHandler(new javax.xml.bind.helpers.DefaultValidationEventHandler());

            generators = (Generators) jaxbUnmarshaller.unmarshal(stream);
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to read " + GENERATORS_FILENAME, e);
        }
    }

    public static GridBagConstraints gbc (int gridX,
                                          int gridY,
                                          int fill,
                                          int anchor,
                                          double weightX,
                                          double weightY,
                                          int gridwidth)
    {
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = gridX;
        c.gridy = gridY;
        c.fill = fill;
        c.anchor = anchor;
        c.weightx = weightX;
        c.weighty = weightY;
        c.gridwidth = gridwidth;

        return c;
    }

    private JScrollPane createTopPanel () throws IOException
    {
        JPanel topPanel = new JPanel(new GridBagLayout());
        JScrollPane topScroller = new JScrollPane(topPanel);

        topPanel.add(createTableNamePanel(),
            gbc(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 0, 1));
        topPanel.add(createJsonStringPanel(),
            gbc(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 0, 1));
        topPanel.add(createDateFormatPanel(),
            gbc(0, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 0, 1));
        topPanel.add(createEntityGeneratorPanel(),
            gbc(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 0, 1));
        topPanel.add(createColumnGeneratorsPanel(),
            gbc(0, 2, GridBagConstraints.BOTH, GridBagConstraints.WEST, 0, 5, 2));
        topPanel.add(createDependsOnPanel(),
            gbc(0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH, 0, 0, 1));
        topPanel.add(createForeignKeysPanel(),
            gbc(1, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH, 0, 0, 1));

        return topScroller;
    }

    private JPanel createResultPanel ()
    {
        // add search field
        JPanel panel = new JPanel(new BorderLayout());
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
        searchPanel.add(new JLabel("Filter: "));
        JTextField jtfFilter = new JTextField();
        jtfFilter.setColumns(50);
        searchPanel.add(jtfFilter);
        panel.add(searchPanel, BorderLayout.NORTH);

        // Add the result panel
        this.resultModel = new DefaultTableModel(new Object[][] {},
            new String[] { "Generated data" });
        this.resultTable = new JTable(resultModel);
        TableRowSorter<TableModel> rowSorter = new TableRowSorter<>(resultTable.getModel());
        configureSearchField(rowSorter, jtfFilter);
        resultTable.setRowSorter(rowSorter);
        panel.add(new JScrollPane(resultTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        updateFontSize(resultTable);

        return panel;
    }

    public JTextField getJsonSchema ()
    {
        return this.jsonSchema;
    }

    public JTable getResultTable ()
    {
        return resultTable;
    }

    public DefaultTableModel getResultModel ()
    {
        return resultModel;
    }

    public String getFindText() {
        String text = jtfFind.getText();
        return (text != null && !"".equals(text.trim())) ? text : null;
    }

    private void configureFindField ()
    {

        jtfFind.getDocument().addDocumentListener(new DocumentListener()
        {
            private void filterResults ()
            {
                String text = getFindText();
                for(Component c: columnGenPanel.getComponents()) {
                    if(c instanceof ColumnGenEntry) {
                        ColumnGenEntry ce = (ColumnGenEntry) c;
                        ce.setBgColor(text);
                    }
                }
            }

            @Override
            public void insertUpdate (DocumentEvent e)
            {
                filterResults();
            }

            @Override
            public void removeUpdate (DocumentEvent e)
            {
                filterResults();
            }

            @Override
            public void changedUpdate (DocumentEvent e)
            {
                throw new UnsupportedOperationException(
                    "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        });
    }

    private void configureSearchField (TableRowSorter<TableModel> rowSorter,
                                       JTextField jtfFilter)
    {

        jtfFilter.getDocument().addDocumentListener(new DocumentListener()
        {

            private void filterResults ()
            {
                String text = jtfFilter.getText();

                if (text.trim().length() == 0) {
                    rowSorter.setRowFilter(null);
                }
                else {
                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }

            @Override
            public void insertUpdate (DocumentEvent e)
            {
                filterResults();
            }

            @Override
            public void removeUpdate (DocumentEvent e)
            {
                filterResults();
            }

            @Override
            public void changedUpdate (DocumentEvent e)
            {
                throw new UnsupportedOperationException(
                    "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        });
    }

    private JPanel createEntityGeneratorPanel () throws IOException
    {
        JPanel entityGeneratorPanel = new JPanel();

        entityGeneratorPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        entityGeneratorPanel.add(new JLabel("Entity Generator: "));

        Image imgOrig = ImageIO.read(Configurator.class.getResource("/new_big.png"));
        Image img = imgOrig.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
        Icon icon = new ImageIcon(img);
        newEntityGenBtn = new JButton(icon);
        newEntityGenBtn.setToolTipText("Create Entity generator");
        newEntityGenBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed (ActionEvent e)
            {
                JSONObject json = new JSONObject(
                    "{\"className\":\"tools.xor.CounterGenerator\",\"arguments\":[100]}");
                addEntityGenerator(entityGeneratorPanel, json);
            }
        });

        entityGeneratorPanel.add(newEntityGenBtn);

        return entityGeneratorPanel;
    }

    private void addEntityGenerator (JPanel entityGeneratorPanel,
                                     JSONObject json)
    {
        newEntityGenBtn.setVisible(false);
        entityGenerator = new ColumnGenEntry(entityGeneratorPanel, json, this, newEntityGenBtn);
        entityGeneratorPanel.add(entityGenerator);
    }

    private JPanel createColumnGeneratorsPanel () throws IOException
    {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

        Image imgOrig = ImageIO.read(Configurator.class.getResource("/new_big.png"));
        Image img = imgOrig.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
        Icon icon = new ImageIcon(img);
        JButton newBtn = new JButton(icon);
        newBtn.setToolTipText("Create a new column generator");
        newBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed (ActionEvent e)
            {
                columnGenPanel.add(new ColumnGenEntry(columnGenPanel, new JSONObject(
                    String.format("{\"column\" : \"%s\"}", ColumnGenEntry.NOT_CONFIGURED)),
                    getTableName(), Configurator.this));

                columnGenPanel.revalidate();
                columnGenPanel.repaint();
            }
        });
        toolbarPanel.add(newBtn);

        img = ImageIO.read(getClass().getResource("/edit.png"));
        icon = new ImageIcon(img);
        JButton editBtn = new JButton(icon);
        editBtn.setToolTipText("Mass edit - Add/remove column generators");
        editBtn.setFocusPainted(false);
        editBtn.setContentAreaFilled(false); // flat button
        editBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed (ActionEvent e)
            {
                JFrame topFrame = (JFrame)SwingUtilities.getWindowAncestor(
                    (Component)e.getSource());
                ColumnChooserModel model = new ColumnChooserModel(getTableName());
                // TODO: add appropriate callback handler
                Chooser chooser = new Chooser(topFrame, Configurator.this, "Chooser", true,
                    model.getHeaderNames(), model.getData(), new Object[][] {});
                chooser.pack();
                chooser.setVisible(true);
            }
        });
        toolbarPanel.add(editBtn);

        // add find field
        JPanel findPanel = new JPanel();
        findPanel.setLayout(new BoxLayout(findPanel, BoxLayout.X_AXIS));
        findPanel.setBorder(new EmptyBorder(0, 25, 0, 0));
        findPanel.setBackground(new Color(222, 222, 222));
        findPanel.add(new JLabel("Find: "));
        jtfFind = new JTextField();
        jtfFind.setColumns(25);
        findPanel.add(jtfFind);
        configureFindField();
        toolbarPanel.add(findPanel);
        toolbarPanel.setBackground(new Color(222, 222, 222));

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory
            .createTitledBorder(BorderFactory.createEtchedBorder(), "Column generators",
                TitledBorder.LEFT, TitledBorder.TOP));

        panel.setLayout(new GridBagLayout());
        panel.add(toolbarPanel,
            gbc(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 0, 1));
        panel.add(addColumnGeneratorBody(),
            gbc(0, 1, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 5, 1));

        return panel;
    }

    private JPanel addColumnGeneratorBody () throws IOException
    {
        this.columnGenPanel = new JPanel(new WrapLayout(FlowLayout.LEADING, 15, 10));
        columnGenPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        String colConfig = String.format("{\"column\" : \"%s\"}", ColumnGenEntry.NOT_CONFIGURED);
        columnGenPanel.add(new ColumnGenEntry(columnGenPanel, new JSONObject(colConfig), getTableName(), Configurator.this));
        columnGenPanel.add(new ColumnGenEntry(columnGenPanel, new JSONObject(colConfig), getTableName(), Configurator.this));

        return columnGenPanel;
    }

    private JPanel createDependsOnPanel () throws IOException
    {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

        Image imgOrig = ImageIO.read(Configurator.class.getResource("/edit.png"));
        Image img = imgOrig.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
        Icon icon = new ImageIcon(img);
        JButton editBtn = new JButton(icon);
        editBtn.setToolTipText("Mass edit - Add/remove table dependencies");
        editBtn.setFocusPainted(false);
        editBtn.setContentAreaFilled(false); // flat button
        editBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed (ActionEvent e)
            {
                JFrame topFrame = (JFrame)SwingUtilities.getWindowAncestor(
                    (Component)e.getSource());
                TableChooserModel model = new TableChooserModel(getDataModel().getTables());
                // TODO: add appropriate callback handler
                Chooser chooser = new Chooser(topFrame, Configurator.this, "Chooser", true,
                    model.getHeaderNames(), model.getData(), new Object[][] {});
                chooser.pack();
                chooser.setVisible(true);
            }
        });
        toolbarPanel.add(editBtn);
        toolbarPanel.setBackground(new Color(222, 222, 222));

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory
            .createTitledBorder(BorderFactory.createEtchedBorder(), "Depends On",
                TitledBorder.LEFT, TitledBorder.TOP));

        panel.setLayout(new GridBagLayout());
        panel.add(toolbarPanel,
            gbc(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 0, 1));
        panel.add(addDependsOnBody(),
            gbc(0, 1, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 5, 1));

        return panel;
    }

    private JPanel addDependsOnBody () throws IOException
    {
        this.dependsOnPanel = new JPanel(new WrapLayout(FlowLayout.LEADING, 15, 10));
        dependsOnPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        return dependsOnPanel;
    }

    private JPanel createForeignKeysPanel () throws IOException
    {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

        Image imgOrig = ImageIO.read(Configurator.class.getResource("/new_big.png"));
        Image img = imgOrig.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
        Icon icon = new ImageIcon(img);
        JButton newBtn = new JButton(icon);
        newBtn.setToolTipText("Create a new Foreign Key generator");
        newBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed (ActionEvent e)
            {
                foreignKeysPanel.add(new ColumnGenEntry(foreignKeysPanel, new JSONObject(
                    String.format("{\"column\" : \"%s\"}", ColumnGenEntry.NOT_CONFIGURED)),
                    getTableName(), Configurator.this));

                foreignKeysPanel.revalidate();
                foreignKeysPanel.repaint();
            }
        });
        toolbarPanel.add(newBtn);

        img = ImageIO.read(getClass().getResource("/edit.png"));
        icon = new ImageIcon(img);
        JButton editBtn = new JButton(icon);
        editBtn.setToolTipText("Mass edit - Add/remove foreign key generators");
        editBtn.setFocusPainted(false);
        editBtn.setContentAreaFilled(false); // flat button
        editBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed (ActionEvent e)
            {
                JFrame topFrame = (JFrame)SwingUtilities.getWindowAncestor(
                    (Component)e.getSource());
                ColumnChooserModel model = new ColumnChooserModel(getTableName());
                // TODO: add appropriate callback handler
                Chooser chooser = new Chooser(topFrame, Configurator.this, "Chooser", true,
                    model.getHeaderNames(), model.getData(), new Object[][] {});
                chooser.pack();
                chooser.setVisible(true);
            }
        });
        toolbarPanel.add(editBtn);
        toolbarPanel.setBackground(new Color(222, 222, 222));

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory
            .createTitledBorder(BorderFactory.createEtchedBorder(), "Foreign keys",
                TitledBorder.LEFT, TitledBorder.TOP));

        panel.setLayout(new GridBagLayout());
        panel.add(toolbarPanel,
            gbc(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 0, 1));
        panel.add(addForeignKeysBody(),
            gbc(0, 1, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 5, 1));

        return panel;
    }

    private JPanel addForeignKeysBody () throws IOException
    {
        this.foreignKeysPanel = new JPanel(new WrapLayout(FlowLayout.LEADING, 15, 10));
        foreignKeysPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        return foreignKeysPanel;
    }

    private JPanel createTableNamePanel ()
    {
        JPanel panel = new JPanel();

        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Table: "));

        tableField = new JTextField();
        tableField.setColumns(25);
        tableField.setToolTipText("Enter table name or select from database");
        panel.add(tableField);
        panel.add(addTableChooser(this));

        return panel;
    }

    private JPanel createDateFormatPanel ()
    {
        JPanel panel = new JPanel();

        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Date format: "));

        dateField = new JTextField();
        dateField.setColumns(30);
        dateField.setToolTipText(
            "Enter date format or select from built-in formats that represent the generated date values. "
                + "Required if at least one of the generated fields is a date field. All the date fields should conform to this date format");
        panel.add(dateField);
        panel.add(addDateFormatChooser());

        return panel;
    }

    private void updateCETableName (String tableName)
    {
        for(Component c: columnGenPanel.getComponents()) {
            if(c instanceof ColumnGenEntry) {
                ColumnGenEntry ce = (ColumnGenEntry) c;
                ce.setTableName(tableName);
            }
        }
    }

    private JSONObject getJSON ()
    {
        String text = jsonSchema.getText();

        if (text == null || "".equals(text.trim())) {
            return null;
        }
        else {
            return new JSONObject(text);
        }
    }

    private String getTableName() {
        return tableField.getText();
    }

    private void updateTableName (String tableName) {
        tableField.setText(tableName);
        updateCETableName(tableName);
    }

    private void updateTableName (JSONObject json)
    {

        if (!json.has("tableName")) {
            notifyStatus("tableName not provided!");
        }

        if (json == null) {
            tableField.setText("");
        }
        else {
            updateTableName(json.getString("tableName"));
        }
    }

    private void updateDateFormat(String format) {
        dateField.setText(format);
    }

    private void updateDateFormat (JSONObject json)
    {

        if (json == null || !json.has("dateFormat")) {
            dateField.setText("");
        }
        else {
            updateDateFormat(json.getString("dateFormat"));
        }
    }

    public enum OdorEvent
    {
        ENTITY_GENERATOR_REMOVED,
        COLUMN_SELECTED,
        GENERATOR_SELECTED,
        CHOOSER_SELECTED,
        DATE_FORMAT_SELECTED,
        COLUMN_GENERATOR_UPDATED,
        TABLE_SELECTED;
    }

    public void notify (OdorEvent event)
    {
        switch (event) {
        case ENTITY_GENERATOR_REMOVED:
            removeEntityGenerator();
            break;
        }
    }

    private void removeEntityGenerator ()
    {
        if (entityGenerator != null) {
            entityGenerator.getParent().remove(entityGenerator);
            entityGenerator = null;
            newEntityGenBtn.setVisible(true);
        }
    }

    private void updateEntityGenerator (JSONObject json)
    {
        removeEntityGenerator();

        JPanel entityGenPanel = (JPanel)newEntityGenBtn.getParent();
        if (json.has("entityGenerator")) {
            JSONObject entityJson = json.getJSONObject("entityGenerator");
            addEntityGenerator(entityGenPanel, entityJson);
        }

        entityGenPanel.revalidate();
        entityGenPanel.repaint();
    }

    private void buildColGenEntries (JSONObject json)
    {
        columnGenPanel.removeAll();
        if (json == null) {
            return;
        }

        if (json.has("columnGenerators")) {
            JSONArray jsonArray = json.getJSONArray("columnGenerators");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject colgen = jsonArray.getJSONObject(i);
                columnGenPanel.add(new ColumnGenEntry(columnGenPanel, colgen, getTableName(), Configurator.this));
            }
        }

        columnGenPanel.revalidate();
        columnGenPanel.repaint();
    }

    private JPanel createJsonStringPanel () throws IOException
    {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Config: "), BorderLayout.WEST);

        Image imgOrig = ImageIO.read(Configurator.class.getResource("/build_big.png"));
        Image img = imgOrig.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
        Icon icon = new ImageIcon(img);
        JButton buildBtn = new JButton(icon);
        buildBtn.setToolTipText(
            "Build the UI elements from the JSON string (Will override existing changes!)");

        buildBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed (ActionEvent e)
            {
                JSONObject json = getJSON();
                updateTableName(json);
                updateDateFormat(json);
                updateEntityGenerator(json);
                buildColGenEntries(json);
            }
        });
        panel.add(buildBtn, BorderLayout.CENTER);

        jsonSchema = new JTextField();
        jsonSchema.setColumns(40);
        jsonSchema.setToolTipText(
            "Enter the table configuration in json format that you would like to modify");
        panel.add(jsonSchema, BorderLayout.EAST);

        outerPanel.add(panel, BorderLayout.WEST);

        return outerPanel;
    }

    public static void notifyStatus (String msg)
    {
        status.push(msg);
        updateStatus();
        status.pop();
    }

    public static JButton addTableChooser (CallbackHandler callbackHandler)
    {
        try {
            Image img = ImageIO.read(Configurator.class.getResource("/list.png"));
            Icon icon = new ImageIcon(img);
            JButton pickTable = new JButton(icon);
            pickTable.setToolTipText("Select the table name to configure");
            pickTable.setFocusPainted(false);
            pickTable.setContentAreaFilled(false); // flat button
            pickTable.addActionListener(new ActionListener()
            {
                public void actionPerformed (ActionEvent e)
                {
                    if (getDataModel() == null) {
                        notifyStatus("Database not configured");
                        return;
                    }

                    java.util.List<JDBCDataModel.TableInfo> tables = getDataModel().getTables();
                    ChooserModel model = new TableChooserModel(tables);
                    JFrame topFrame = (JFrame)SwingUtilities.getWindowAncestor(
                        (Component)e.getSource());
                    Chooser chooser = new Chooser(topFrame,
                        new TableSelectedHandler(callbackHandler), "Chooser", true,
                        model.getHeaderNames(), model.getData(), null);
                    chooser.pack();
                    chooser.setVisible(true);
                }
            });

            return pickTable;
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load image list.png");
        }
    }

    private JButton addDateFormatChooser ()
    {
        try {
            Image img = ImageIO.read(getClass().getResource("/list.png"));
            Icon icon = new ImageIcon(img);
            JButton pickTable = new JButton(icon);
            pickTable.setToolTipText("Select one of built-in date formats");
            pickTable.setFocusPainted(false);
            pickTable.setContentAreaFilled(false); // flat button
            pickTable.addActionListener(new ActionListener()
            {
                public void actionPerformed (ActionEvent e)
                {
                    DateChooserModel model = new DateChooserModel();
                    JFrame topFrame = (JFrame)SwingUtilities.getWindowAncestor(
                        (Component)e.getSource());
                    Chooser chooser = new Chooser(topFrame,
                        new DateFormatSelectedHandler(Configurator.this), "Chooser", true,
                        model.getHeaderNames(), model.getData(), null);
                    chooser.pack();
                    chooser.setVisible(true);
                }
            });

            return pickTable;
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load image list.png");
        }
    }

    public static void updateFontSize (JTable jTable)
    {
        jTable.setFont(fonts[fontLevel]);
        jTable.getTableHeader().setFont(fonts[fontLevel]);
    }

    public static void updateStatus ()
    {
        if (status.size() == 0) {
            statusLabel.setText("");
        }
        else {
            statusLabel.setText(status.peek());
        }
    }

    private static JButton addNewButton (JFrame frame)
    {
        try {
            Image imgOrig = ImageIO.read(Configurator.class.getResource("/new_big.png"));
            Image img = imgOrig.getScaledInstance(30, 30, java.awt.Image.SCALE_SMOOTH);
            Icon icon = new ImageIcon(img);
            JButton openButton = new JButton(icon);
            openButton.setToolTipText("Create a new generator configuration.");

            openButton.addActionListener(new ActionListener()
            {
                public void actionPerformed (ActionEvent e)
                {
                    JOptionPane.showMessageDialog(frame, "File open clicked");
                }
            });

            return openButton;
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load image new_big.png");
        }
    }

    private static JButton addSaveButton (JFrame frame)
    {
        try {
            Image imgOrig = ImageIO.read(Configurator.class.getResource("/save_big.png"));
            Image img = imgOrig.getScaledInstance(30, 30, java.awt.Image.SCALE_SMOOTH);
            Icon icon = new ImageIcon(img);
            JButton saveButton = new JButton(icon);
            saveButton.setToolTipText("Save changes made to a table config.");

            saveButton.addActionListener(new ActionListener()
            {
                public void actionPerformed (ActionEvent e)
                {
                    JOptionPane.showMessageDialog(frame, "save clicked");
                }
            });

            return saveButton;
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load image save_big.png");
        }
    }

    private static JButton addOpenButton (JFrame frame)
    {
        try {
            Image imgOrig = ImageIO.read(Configurator.class.getResource("/open_big.png"));
            Image img = imgOrig.getScaledInstance(30, 30, java.awt.Image.SCALE_SMOOTH);
            Icon icon = new ImageIcon(img);
            JButton openButton = new JButton(icon);
            openButton.setToolTipText("Open a CSV generator file.");

            openButton.addActionListener(new ActionListener()
            {
                public void actionPerformed (ActionEvent e)
                {
                    JOptionPane.showMessageDialog(frame, "File open clicked");
                }
            });

            return openButton;
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load image open_big.png");
        }
    }

    private static JToggleButton addSettingsButton (JFrame frame,
                                                    Configurator c)
    {
        try {
            Image imgOrig = ImageIO.read(Configurator.class.getResource("/settings_big.png"));
            Image img = imgOrig.getScaledInstance(30, 30, java.awt.Image.SCALE_SMOOTH);
            Icon icon = new ImageIcon(img);
            JToggleButton settingsButton = new JToggleButton(icon);
            settingsButton.setFocusPainted(false);
            settingsButton.setContentAreaFilled(false);
            settingsButton.setSelected(true);
            settingsButton.setToolTipText("Toggle column layout in results pane");

            settingsButton.addActionListener(new ActionListener()
            {
                public void actionPerformed (ActionEvent e)
                {
                    AbstractButton abstractButton = (AbstractButton)e.getSource();
                    boolean selected = abstractButton.getModel().isSelected();

                    if (selected) {
                        c.getResultTable().setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
                    }
                    else {
                        c.getResultTable().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                    }
                }
            });

            return settingsButton;
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load image settings_big.png");
        }
    }

    private static JButton addGenerateButton (JFrame frame,
                                              Configurator c)
    {
        try {
            Image imgOrig = ImageIO.read(Configurator.class.getResource("/generate_big.png"));
            Image img = imgOrig.getScaledInstance(30, 30, java.awt.Image.SCALE_SMOOTH);
            Icon icon = new ImageIcon(img);
            JButton generateButton = new JButton(icon);
            generateButton.setToolTipText("Generate and display the data.");

            generateButton.addActionListener(new ActionListener()
            {
                private void extractData (java.util.List<JSONObject> generatedObjects)
                {
                    Map<String, Integer> columnToPos = new HashMap<>();

                    // Generate the column name to position map
                    int pos = 0;
                    for (JSONObject json : generatedObjects) {
                        for (String colName : json.keySet()) {
                            if (!columnToPos.containsKey(colName)) {
                                columnToPos.put(colName, pos++);
                            }
                        }
                    }

                    // Populate the headers
                    String[] columnNames = new String[pos];
                    for (Map.Entry<String, Integer> entry : columnToPos.entrySet()) {
                        columnNames[entry.getValue()] = entry.getKey();
                    }

                    // populate the data
                    Object[][] data = new Object[generatedObjects.size()][pos];
                    int row = 0;
                    for (JSONObject json : generatedObjects) {
                        for (String colName : columnToPos.keySet()) {
                            if (json.has(colName)) {
                                data[row][columnToPos.get(colName)] = json.get(colName);
                            }
                        }
                        row++;
                    }
                    System.out.println(
                        "data.length, data[0].length: " + data.length + ", " + data[0].length);
                    System.out.println("Column length: " + columnNames.length);
                    c.getResultModel().setDataVector(data, columnNames);

                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run ()
                        {
                            c.getResultModel().fireTableStructureChanged();
                        }
                    });
                }

                public void actionPerformed (ActionEvent e)
                {
                    String jsonSchema = c.getJsonSchema().getText();
                    JSONObject json = new JSONObject(jsonSchema);

                    System.out.println(jsonSchema);
                    if (getDataModel() == null) {
                        notifyStatus("Database not configured");
                        return;
                    }

                    CSVLoader csvLoader = new CSVLoader(shape);
                    CSVLoader.CSVState csvState = csvLoader.getCSVState(json, null);

                    Future<CSVLoader.CreateRecordIteration> future = csvLoader.generateAsynchronous(
                        csvState, 1000);

                    try {
                        CSVLoader.CreateRecordIteration iteration = future.get();
                    }
                    catch (InterruptedException | ExecutionException ex) {
                        throw ClassUtil.wrapRun(ex);
                    }

                    BlockingQueue<JSONObject> queue = csvState.getBoundedQueue();
                    int numRecords = 0;
                    JSONObject jsonObj = queue.poll();
                    java.util.List<JSONObject> generatedObjects = new ArrayList<>();
                    while (jsonObj != null) {
                        if (numRecords == 50) {
                            break;
                        }
                        System.out.println(jsonObj.toString());
                        numRecords++;
                        generatedObjects.add(jsonObj);
                        jsonObj = queue.poll();
                    }

                    extractData(generatedObjects);
                }
            });

            return generateButton;
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load image generate_big.png");
        }
    }

    private static JButton addExecuteButton (JFrame frame)
    {
        try {
            Image imgOrig = ImageIO.read(Configurator.class.getResource("/execute_big.png"));
            Image img = imgOrig.getScaledInstance(30, 30, java.awt.Image.SCALE_SMOOTH);
            Icon icon = new ImageIcon(img);
            JButton executeButton = new JButton(icon);
            executeButton.setToolTipText("Import the generated data to the DB if provided.");

            executeButton.addActionListener(new ActionListener()
            {
                public void actionPerformed (ActionEvent e)
                {
                    JOptionPane.showMessageDialog(frame, "Generate clicked");
                }
            });

            return executeButton;
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load image execute_big.png");
        }
    }

    private static JButton addStopButton (JFrame frame)
    {
        try {
            Image imgOrig = ImageIO.read(Configurator.class.getResource("/stop_big.png"));
            Image img = imgOrig.getScaledInstance(30, 30, java.awt.Image.SCALE_SMOOTH);
            Icon icon = new ImageIcon(img);
            JButton stopButton = new JButton(icon);
            stopButton.setEnabled(false); // enabled when a long running action is in progress
            stopButton.setToolTipText("Cancel long running tasks/actions.");

            stopButton.addActionListener(new ActionListener()
            {
                public void actionPerformed (ActionEvent e)
                {
                    JOptionPane.showMessageDialog(frame, "Stop clicked");
                }
            });

            return stopButton;
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load image stop_big.png");
        }
    }

    private static void addStatusBar (JFrame frame)
    {
        // create the status bar panel and shove it down the bottom of the frame
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        frame.add(statusPanel, BorderLayout.SOUTH);
        statusPanel.setPreferredSize(new Dimension(frame.getWidth(), 16));
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        statusLabel = new JLabel("status");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusPanel.add(statusLabel);
    }

    public static class CreateSchemaTask implements Callable
    {
        private DataSource ds;
        private String scriptFile;
        private String delimiter;

        public CreateSchemaTask (DataSource ds,
                                 String scriptFile,
                                 String delimiter)
        {
            this.ds = ds;
            this.scriptFile = scriptFile;
            this.delimiter = delimiter;
        }

        public Object call ()
        {
            try {
                ClassUtil.executeScript(ds, scriptFile, false, delimiter);
                initializeDataModel();
            }
            catch (SQLException e) {
                e.printStackTrace();
                System.exit(1);
            }
            return null;
        }
    }

    private static void initializeDataModel ()
    {
        xor = new AggregateManager();
        DefaultDataModelFactory factory = new DefaultDataModelFactory("jdbc");
        factory.setAggregateManager(xor);
        JDBCConfigDataModelBuilder builder = new JDBCConfigDataModelBuilder();
        builder.setDataSource(ds);
        factory.setDataModelBuilder(builder);
        xor.setDataModelFactory(factory);
        xor.setTypeMapper(new UnchangedTypeMapper());
    }

    public static void main (String[] args)
    {
        Options options = new Options();

        Option url = new Option("c", "url", true, "JDBC url");
        url.setRequired(false);
        options.addOption(url);

        Option script = new Option("s", "script", true, "SQL script file to create a schema");
        script.setRequired(false);
        options.addOption(script);

        Option del = new Option("d", "delimiter", true, "SQL Script delimiter");
        del.setRequired(false);
        options.addOption(del);

        Option user = new Option("u", "username", true, "Username for the JDBC connection");
        user.setRequired(false);
        options.addOption(user);

        Option pass = new Option("p", "password", true, "Password of the user");
        pass.setRequired(false);
        options.addOption(pass);

        Option driverClass = new Option("r", "driver", true, "JDBC Driver class name");
        pass.setRequired(false);
        options.addOption(driverClass);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            String jdbcURL = null;
            if (cmd.hasOption("c")) {
                jdbcURL = cmd.getOptionValue("url");
                System.out.println("JDBC url:  " + jdbcURL);
            }

            String scriptFile = null;
            if (cmd.hasOption("s")) {
                scriptFile = cmd.getOptionValue("script");
                System.out.println("Script file:  " + scriptFile);
            }

            String delimiter = null;
            if (cmd.hasOption("d")) {
                delimiter = cmd.getOptionValue("delimiter");
                System.out.println("Delimiter:  " + delimiter);
            }

            String username = null;
            if (cmd.hasOption("u")) {
                username = cmd.getOptionValue("username");
                System.out.println("Username:  " + username);
            }

            String password = null;
            if (cmd.hasOption("p")) {
                password = cmd.getOptionValue("password");
                System.out.println("Password:  " + password);
            }

            String driver = null;
            if (cmd.hasOption("r")) {
                driver = cmd.getOptionValue("driver");
                System.out.println("driver:  " + driver);
            }

            if (jdbcURL != null && driver != null) {
                // Create the data source
                ds = new BasicDataSource();
                ds.setDriverClassName(driver);
                ds.setUrl(jdbcURL);
                if (username != null) {
                    ds.setUsername(username);
                }
                if (password != null) {
                    ds.setPassword(password);
                }

                CreateSchemaTask task = new CreateSchemaTask(ds, scriptFile, delimiter);
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Future future = executorService.submit(task);

                executorService.shutdown();
            }
        }
        catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Usage:", options);

            System.exit(1);
        }

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run ()
            {
                JFrame frame = new JFrame("Table generator config editor");
                frame.setPreferredSize(new Dimension(1280, 1024));
                Configurator c = new Configurator();
                frame.add(c, BorderLayout.CENTER);

                JToolBar toolbar = new JToolBar("File controls");
                toolbar.setBackground(Color.lightGray);
                toolbar.setFloatable(false);
                toolbar.add(addNewButton(frame));
                toolbar.add(addOpenButton(frame));
                toolbar.add(addSaveButton(frame));
                toolbar.addSeparator();
                toolbar.add(addSettingsButton(frame, c));
                toolbar.addSeparator();
                toolbar.add(addGenerateButton(frame, c));
                toolbar.add(addExecuteButton(frame));
                toolbar.addSeparator();
                toolbar.add(addStopButton(frame));
                frame.getContentPane().add(toolbar, BorderLayout.PAGE_START);

                addStatusBar(frame);
                frame.pack();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    public static class TableChooserModel implements ChooserModel {

        private java.util.List<JDBCDataModel.TableInfo> tables;
        private String[] headerNames = { "Table name", "Primary keys" };
        private Object[][] tableInfo;

        public TableChooserModel(java.util.List<JDBCDataModel.TableInfo> tables) {
            this.tables = tables;

            tableInfo = new Object[tables.size()][headerNames.length];
            for (int i = 0; i < tables.size(); i++) {
                JDBCDataModel.TableInfo ti = tables.get(i);
                tableInfo[i][0] = ti.getName();
                StringBuilder str = new StringBuilder();
                for (String key : ti.getPrimaryKeys()) {
                    ((str.length() > 0) ? str.append(",") : str).append(key);
                }
                tableInfo[i][1] = str.toString();
            }
        }

        @Override
        public String[] getHeaderNames ()
        {
            return this.headerNames;
        }

        @Override
        public Object[][] getData ()
        {
            return this.tableInfo;
        }
    }

    public static class GeneratorChooserModel implements ChooserModel {

        private String[] headerNames = { "Name", "Java class", "Type", "Description" };
        private Object[][] data;

        public GeneratorChooserModel ()
        {
            Set<Generator> g = generators.getGenerators();
            // The last column holds the html text that is displayed in a separate panel
            data = new Object[g.size()][headerNames.length + 1];
            int i = 0;
            for (Generator generator: g) {
                data[i][0] = generator.getDisplayName();
                data[i][1] = generator.getClassName();
                data[i][2] = generator.getType();
                data[i][3] = generator.getDescription();
                data[i][4] = generator.getHtmlHelp();

                i++;
            }
        }

        @Override
        public String[] getHeaderNames ()
        {
            return this.headerNames;
        }

        @Override
        public Object[][] getData ()
        {
            return this.data;
        }
    }

    public static class ColumnChooserModel implements ChooserModel {

        private String[] headerNames = { "Name", "Type", "Nullable", "Length" };
        private Object[][] data;

        public ColumnChooserModel(String tableName) {
            JDBCDataModel.TableInfo ti = getDataModel().getTable(tableName);
            if(ti == null) {
                notifyStatus(String.format("Table with name '%s' not found", tableName));

                data = new Object[][]{};
            } else {
                java.util.List<JDBCDataModel.ColumnInfo> columnInfos = ti.getColumns();
                data = new Object[columnInfos.size()][headerNames.length];
                for(int i = 0; i < columnInfos.size(); i++) {
                    JDBCDataModel.ColumnInfo ci = columnInfos.get(i);
                   data[i][0] = ci.getName();
                   data[i][1] = ci.getDataType();
                   data[i][2] = ci.isNullable() ? "True" : "False";
                   data[i][3] = ci.getLength();
                }
            }
        }

        @Override
        public String[] getHeaderNames ()
        {
            return this.headerNames;
        }

        @Override
        public Object[][] getData ()
        {
            return this.data;
        }
    }

    public static class DateChooserModel implements ChooserModel {

        String[] headerNames = { "Pattern", "Example" };
        Object[][] data = { { "dd-MM-yy", "22-01-21" }, { "dd-MM-yyyy", "22-01-2021" },
            { "MM-dd-yyyy", "01-22-2021" }, { "yyyy-MM-dd", "2021-01-22" },
            { "yyyy-MM-dd HH:mm:ss", "2021-01-22 23:59:59" },
            { "yyyy-MM-dd HH:mm:ssZ", "2021-10-11 18:33:17-0500" },
            { "yyyy-MM-dd HH:mm:ss.SSS", "2021-01-22 23:59:59.999" },
            { "yyyy-MM-dd HH:mm:ss.SSSZ", "2021-01-22 23:59:59.999+0100" },
            { "EEEEE MMMMM yyyy HH:mm:ss.SSSZ",
                "Friday January 2021 10:45:42.720+0100" },
            { "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "2021-10-11T18:32:35.736-05:00" } };

        @Override
        public String[] getHeaderNames ()
        {
            return this.headerNames;
        }

        @Override
        public Object[][] getData ()
        {
            return this.data;
        }
    }

    public static class DateFormatSelectedHandler implements CallbackHandler {
        private CallbackHandler callbackHandler;

        public DateFormatSelectedHandler(CallbackHandler callbackHandler) {
            this.callbackHandler = callbackHandler;
        }

        @Override
        public void execute (OdorEvent event,
                             Object data)
        {
            callbackHandler.execute(OdorEvent.DATE_FORMAT_SELECTED, data);
        }
    }

    public static class TableSelectedHandler implements CallbackHandler {
        private CallbackHandler callbackHandler;

        public TableSelectedHandler(CallbackHandler callbackHandler) {
            this.callbackHandler = callbackHandler;
        }

        @Override
        public void execute (OdorEvent event,
                             Object data)
        {
            callbackHandler.execute(OdorEvent.TABLE_SELECTED, data);
        }
    }

    @Override
    public void execute (OdorEvent event,
                         Object data)
    {
        switch (event) {
        case TABLE_SELECTED:
            updateTableName(((Object[])data)[0].toString());
            break;

        case DATE_FORMAT_SELECTED:
            updateDateFormat(((Object[])data)[0].toString());
            break;
        }
    }
}
