package tools.xor;

import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import static tools.xor.ColumnGenEntry.KEY_ARGUMENTS;
import static tools.xor.ColumnGenEntry.KEY_CLASSNAME;
import static tools.xor.ColumnGenEntry.KEY_COLUMN;
import static tools.xor.Configurator.gbc;
import static tools.xor.Configurator.getDataModel;
import static tools.xor.Configurator.notifyStatus;

public class ColumnGenEditor extends JDialog implements CallbackHandler
{

    private ColumnGenEntry entry;
    private JPanel argumentsPanel;
    private JCheckBox chbxIsInt;
    private JTextField jtfValue;
    private JButton addLast;
    private JTextField jtfColumnName;
    private JTextField jtfClassName;
    private String tableName;

    public ColumnGenEditor (Frame owner,
                            String title,
                            String tableName,
                            boolean modal,
                            ColumnGenEntry entry,
                            boolean isEntityGen)
    {
        super(owner, title, modal);

        // main panel
        getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        this.entry = entry;
        this.tableName = tableName;

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        if (!isEntityGen) {
            add(createColumnNamePanel());
        }
        add(createClassNamePanel());
        add(createArgumentsPanel());
        add(createOkCancel());

        setPreferredSize(new Dimension(600, 800));

        pack();
    }

    private JPanel createOkCancel ()
    {
        JPanel panel = new JPanel();
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new OkDialog(this));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new CloseDialog(this));

        panel.add(okButton);
        panel.add(cancelButton);

        return panel;
    }

    public static class OkDialog implements ActionListener
    {
        private ColumnGenEditor editor;

        public OkDialog (ColumnGenEditor editor)
        {
            this.editor = editor;
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            editor.entry.updateEntry(editor);
            editor.dispose();
        }
    }

    public static class CloseDialog implements ActionListener
    {
        private JDialog editor;

        public CloseDialog (JDialog editor)
        {
            this.editor = editor;
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            editor.dispose();
        }
    }

    private JPanel createColumnNamePanel ()
    {
        JPanel panel = new JPanel();

        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Column: "));

        jtfColumnName = new JTextField();
        jtfColumnName.setColumns(30);
        panel.add(jtfColumnName);
        panel.add(addColumnChooser());

        JSONObject colgen = entry.getColGenJson();
        if (colgen.has(KEY_COLUMN)) {
            jtfColumnName.setText(colgen.getString(KEY_COLUMN));
        }

        return panel;
    }

    public JTextField getClassName ()
    {
        return this.jtfClassName;
    }

    public JTextField getColumnName ()
    {
        return this.jtfColumnName;
    }

    private void updateColumnName (String columnName)
    {
        this.jtfColumnName.setText(columnName);
    }

    private void updateClassName (String columnName)
    {
        this.jtfClassName.setText(columnName);
    }

    private JButton addColumnChooser ()
    {
        try {
            Image img = ImageIO.read(ColumnGenEditor.class.getResource("/list.png"));
            Icon icon = new ImageIcon(img);
            JButton pickTable = new JButton(icon);
            pickTable.setToolTipText("Select the column name to configure. Disabled if table not previously selected.");
            pickTable.setFocusPainted(false);
            pickTable.setContentAreaFilled(false); // flat button
            if (this.tableName == null || "".equals(tableName.trim())) {
                pickTable.setEnabled(false);
            }

            pickTable.addActionListener(new ActionListener()
            {
                public void actionPerformed (ActionEvent e)
                {
                    if (getDataModel() == null) {
                        notifyStatus("Database not configured");
                        return;
                    }

                    Configurator.ColumnChooserModel model = new Configurator.ColumnChooserModel(
                        ColumnGenEditor.this.tableName);
                    JDialog topFrame = (JDialog)SwingUtilities.getWindowAncestor(
                        (Component)e.getSource());
                    Chooser chooser = new Chooser(topFrame,
                        new UpdateNameHandler(ColumnGenEditor.this), "Chooser", true,
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

    public static class UpdateNameHandler implements CallbackHandler
    {

        private CallbackHandler target;

        public UpdateNameHandler (CallbackHandler target)
        {
            this.target = target;
        }

        @Override
        public void execute (Configurator.OdorEvent event,
                             Object data)
        {
            target.execute(Configurator.OdorEvent.COLUMN_SELECTED, data);
        }
    }

    public static class UpdateGeneratorHandler implements CallbackHandler
    {

        private CallbackHandler target;

        public UpdateGeneratorHandler (CallbackHandler target)
        {
            this.target = target;
        }

        @Override
        public void execute (Configurator.OdorEvent event,
                             Object data)
        {
            target.execute(Configurator.OdorEvent.GENERATOR_SELECTED, data);
        }
    }

    private JPanel createClassNamePanel ()
    {
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(10, 0, 10, 0));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Generator class: "));

        jtfClassName = new JTextField();
        jtfClassName.setColumns(30);
        panel.add(jtfClassName);
        panel.add(addGeneratorChooser());

        JSONObject colgen = entry.getColGenJson();
        if (colgen.has(KEY_CLASSNAME)) {
            jtfClassName.setText(colgen.getString(KEY_CLASSNAME));
        }

        return panel;
    }

    private JButton addGeneratorChooser ()
    {
        try {
            Image img = ImageIO.read(ColumnGenEditor.class.getResource("/list.png"));
            Icon icon = new ImageIcon(img);
            JButton pickTable = new JButton(icon);
            pickTable.setToolTipText("Select the class name for the generator of this column");
            pickTable.setFocusPainted(false);
            pickTable.setContentAreaFilled(false); // flat button

            pickTable.addActionListener(new ActionListener()
            {
                public void actionPerformed (ActionEvent e)
                {
                    Configurator.GeneratorChooserModel model = new Configurator.GeneratorChooserModel();
                    JDialog topFrame = (JDialog)SwingUtilities.getWindowAncestor(
                        (Component)e.getSource());
                    Chooser chooser = new Chooser(topFrame,
                        new UpdateGeneratorHandler(ColumnGenEditor.this), "Chooser", true,
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

    private JPanel createArgumentsPanel ()
    {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
            "Generator constructor arguments", TitledBorder.LEFT, TitledBorder.TOP));
        panel.setLayout(new GridBagLayout());

        panel.add(createCheckBox(),
            gbc(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 0, 0, 1));
        panel.add(Box.createRigidArea(new Dimension(10, 0)),
            gbc(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 0, 0, 1));
        panel.add(createValueLabel(),
            gbc(2, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 0, 0, 1));
        panel.add(createValueField(),
            gbc(3, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 5, 0, 1));
        panel.add(createAddLastButton(),
            gbc(4, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 0, 0, 1));
        panel.add(addArgumentsBody(),
            gbc(0, 1, GridBagConstraints.BOTH, GridBagConstraints.WEST, 0, 5, 5));

        return panel;
    }

    private JCheckBox createCheckBox ()
    {
        // This is enabled if there is a single argument since
        // a generator has a list of String arguments and a single integer argument
        chbxIsInt = new JCheckBox("Is Integer argument");
        chbxIsInt.setHorizontalTextPosition(SwingConstants.LEFT);
        return chbxIsInt;
    }

    private JLabel createValueLabel ()
    {
        return new JLabel("Enter value: ");
    }

    private JTextField createValueField ()
    {
        jtfValue = new JTextField();
        jtfValue.setColumns(30);
        return jtfValue;
    }

    private JButton createAddLastButton ()
    {
        try {
            Image imgOrig = ImageIO.read(Configurator.class.getResource("/new_big.png"));
            Image img = imgOrig.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
            Icon icon = new ImageIcon(img);
            addLast = new JButton(icon);
            addLast.setToolTipText("Add value as a new argument to end of list");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return addLast;
    }

    private JScrollPane addArgumentsBody ()
    {
        JPanel variablePanel = new JPanel();
        variablePanel.setLayout(new GridBagLayout());

        this.argumentsPanel = new JPanel();
        this.argumentsPanel.setLayout(new BoxLayout(argumentsPanel, BoxLayout.Y_AXIS));
        argumentsPanel.setBorder(new EmptyBorder(10, 25, 10, 25));

        GridBagConstraints argumentsGbc = gbc(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 0, 1);
        argumentsGbc.ipadx = 50;

        variablePanel.add(argumentsPanel, argumentsGbc);
        variablePanel.add(new JPanel(),
            gbc(0, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 5, 1));

        JSONObject colgen = entry.getColGenJson();
        if (colgen.has(KEY_ARGUMENTS)) {
            JSONArray argArray = colgen.getJSONArray(KEY_ARGUMENTS);
            if (argArray.length() == 1 && argArray.get(0) instanceof Integer) {
                this.chbxIsInt.setSelected(true);
                this.chbxIsInt.setEnabled(true);

                argumentsPanel.add(
                    new ArgumentEntry(argumentsPanel, this, Integer.toString(argArray.getInt(0)), 0));
            }
            else {
                this.chbxIsInt.setSelected(false);
                if(argArray.length() != 1) {
                    this.chbxIsInt.setEnabled(false);
                }

                for (int i = 0; i < argArray.length(); i++) {
                    argumentsPanel.add(new ArgumentEntry(argumentsPanel, this, argArray.getString(i), i));
                }
            }
        }

        addLast.addActionListener(new ArgumentEntry.AddActionListener(argumentsPanel,
            new ArgumentEntry(argumentsPanel, this,"", -1)));

        JScrollPane scrollPane = new JScrollPane(variablePanel);

        return scrollPane;
    }

    public void updateIntChbx(int numArgs) {
        if(numArgs == 1) {
            this.chbxIsInt.setEnabled(true);
        } else {
            this.chbxIsInt.setSelected(false);
            this.chbxIsInt.setEnabled(false);
        }
    }

    @Override
    public void execute (Configurator.OdorEvent event,
                         Object data)
    {
        switch (event) {
        case COLUMN_SELECTED:
            String newColumnName = ((Object[])data)[0].toString();
            updateColumnName(newColumnName);
            break;
        case GENERATOR_SELECTED:
            String className = ((Object[])data)[1].toString();
            updateColumnName(className);
            break;
        }
    }
}
