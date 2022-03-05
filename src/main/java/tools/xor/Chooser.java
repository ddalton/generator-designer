package tools.xor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

public class Chooser extends JDialog
{

    private String[] columnNames;
    private Object[][] data;
    private Object[][] picks;
    private CallbackHandler callbackHandler;
    private JTable availableTable;
    private JEditorPane jtHtmlPane;

    public Chooser (Frame owner,
                    CallbackHandler callbackHandler,
                    String title,
                    boolean modal,
                    String[] columnNames,
                    Object[][] data,
                    Object[][] picks)
    {
        super(owner, title, modal);

        init(callbackHandler, columnNames, data, picks);
    }

    public Chooser (Dialog owner,
                    CallbackHandler callbackHandler,
                    String title,
                    boolean modal,
                    String[] columnNames,
                    Object[][] data,
                    Object[][] picks)
    {
        super(owner, title, modal);

        init(callbackHandler, columnNames, data, picks);
    }

    private void init (CallbackHandler callbackHandler,
                       String[] columnNames,
                       Object[][] data,
                       Object[][] picks)
    {
        // main panel
        getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        this.callbackHandler = callbackHandler;
        this.columnNames = columnNames;
        this.data = data;
        this.picks = picks;

        boolean hasHTMLPane = hasHTML();

        Container top = this, topPanel = this;
        if(hasHTMLPane) {
            // top panel
            topPanel = new JPanel(new BorderLayout());
            top = new JScrollPane(topPanel);
        }

        // Add the first section containing table information
        addChooserPanes(topPanel, isSingleSelection());

        if(hasHTMLPane) {
            // bottom panel
            Container bottom = new JPanel(new BorderLayout());
            jtHtmlPane = new JEditorPane();
            jtHtmlPane.setContentType("text/html");
            jtHtmlPane.setEditable(false);
            jtHtmlPane.setPreferredSize(new Dimension(450, 300));
            bottom.add(jtHtmlPane, BorderLayout.CENTER);

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top,
                bottom);
            add(splitPane, BorderLayout.CENTER);

            bottom.add(createOkCancel(), BorderLayout.SOUTH);
        } else {
            top.add(createOkCancel(), BorderLayout.SOUTH);
        }

        pack();
    }

    private boolean hasHTML() {
        return data.length > 0 && (data[0].length == columnNames.length + 1);
    }

    private boolean isSingleSelection() {
        return picks == null;
    }

    private JPanel createOkCancel ()
    {
        JPanel panel = new JPanel();
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed (ActionEvent e)
            {
                int[] selectedRows = availableTable.getSelectedRows();
                int colCount = availableTable.getColumnCount();

                Object[][] selected = new Object[selectedRows.length][colCount];
                for (int i = 0; i < selectedRows.length; i++) {
                    for (int j = 0; j < colCount; j++) {
                        selected[i][j] = availableTable.getValueAt(selectedRows[i], j);
                    }
                }

                if(selected.length > 0) {
                    callbackHandler.execute(Configurator.OdorEvent.CHOOSER_SELECTED,
                        isSingleSelection() ? selected[0] : selected);
                }

                Chooser.this.dispose();
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ColumnGenEditor.CloseDialog(this));

        panel.add(okButton);
        panel.add(cancelButton);

        return panel;
    }

    private void addChooserPanes (Container c, boolean singleSelection)
    {
        DefaultTableModel model = new DefaultTableModel(data, columnNames);
        availableTable = new JTable(model);
        TableRowSorter<TableModel> rowSorter = new TableRowSorter<>(availableTable.getModel());

        if(hasHTML()) {
            ListSelectionModel selectionModel = availableTable.getSelectionModel();
            selectionModel.addListSelectionListener(new ListSelectionListener()
            {
                public void valueChanged (ListSelectionEvent e)
                {
                    boolean isAdj = e.getValueIsAdjusting();
                    int index = e.getFirstIndex();
                    index = ((ListSelectionModel)e.getSource()).isSelectedIndex(index) ? index : e.getLastIndex();
                    System.out.println(String.format(
                        "Is Adj: %s, first Index: %s, last Index: %s, is selected index: %s", isAdj,
                        e.getFirstIndex(), e.getLastIndex(),
                        ((ListSelectionModel)e.getSource()).isSelectedIndex(index)));
                    if (isAdj)
                        return;

                    if (((ListSelectionModel)e.getSource()).isSelectedIndex(index)) {
                        String htmlText = data[index][columnNames.length].toString();
                        jtHtmlPane.setText(htmlText);
                    }
                }
            });
        }

        // add search field
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Search"), BorderLayout.WEST);
        JTextField jtfFilter = new JTextField();
        panel.add(jtfFilter, BorderLayout.CENTER);
        c.add(panel, BorderLayout.NORTH);
        configureSearchField(jtfFilter, rowSorter);

        JPanel dataPanel = new JPanel(new BorderLayout());
        dataPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        // Add the left chooser pane
        availableTable.setRowSorter(rowSorter);
        Configurator.updateFontSize(availableTable);

        if (!singleSelection) {
            dataPanel.add(new JScrollPane(availableTable), BorderLayout.WEST);

            // Add the controls pane
            JPanel controlsPanel = new JPanel(new BorderLayout());
            controlsPanel.setBorder(new EmptyBorder(100, 30, 100, 30));
            controlsPanel.add(addMoveRightButton(), BorderLayout.NORTH);
            controlsPanel.add(addMoveLeftButton(), BorderLayout.SOUTH);
            dataPanel.add(controlsPanel, BorderLayout.CENTER);

            // Add the right chooser pane
            updatePicks(new int[] {});
            DefaultTableModel picksModel = new DefaultTableModel(picks, columnNames);
            JTable picksTable = new JTable(picksModel);
            dataPanel.add(new JScrollPane(picksTable), BorderLayout.EAST);
            Configurator.updateFontSize(picksTable);
        }
        else {
            dataPanel.add(new JScrollPane(availableTable), BorderLayout.NORTH);
            availableTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        c.add(dataPanel, BorderLayout.CENTER);
    }

    private JButton addMoveRightButton ()
    {
        try {
            Image imgOrig = ImageIO.read(Configurator.class.getResource("/right_big.png"));
            Image img = imgOrig.getScaledInstance(30, 30, java.awt.Image.SCALE_SMOOTH);
            Icon icon = new ImageIcon(img);
            JButton moveButton = new JButton(icon);
            moveButton.setFocusPainted(false);
            moveButton.setContentAreaFilled(false); // flat button

            return moveButton;
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load image right_big.png");
        }
    }

    private JButton addMoveLeftButton ()
    {
        try {
            Image imgOrig = ImageIO.read(Configurator.class.getResource("/left_big.png"));
            Image img = imgOrig.getScaledInstance(30, 30, java.awt.Image.SCALE_SMOOTH);
            Icon icon = new ImageIcon(img);
            JButton moveButton = new JButton(icon);
            moveButton.setFocusPainted(false);
            moveButton.setContentAreaFilled(false); // flat button

            return moveButton;
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load image left_big.png");
        }
    }

    private void updatePicks (int[] selected)
    {
        if (selected.length > 0) {
            // TODO: fix the data and picks models
        }
    }

    private void configureSearchField (JTextField jtfFilter,
                                       TableRowSorter<TableModel> rowSorter)
    {

        jtfFilter.getDocument().addDocumentListener(new DocumentListener()
        {

            @Override
            public void insertUpdate (DocumentEvent e)
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
            public void removeUpdate (DocumentEvent e)
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
            public void changedUpdate (DocumentEvent e)
            {
                throw new UnsupportedOperationException(
                    "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        });
    }
}
