package tools.xor;

import org.json.JSONObject;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class ColumnGenEntry extends JPanel {

    public static final String NOT_CONFIGURED = "<Not configured>";
    public static final String KEY_COLUMN = "column";
    public static final String KEY_CLASSNAME = "className";
    public static final String KEY_ARGUMENTS = "arguments";

    private JPanel parent;
    private JButton newEntityGenBtn;
    private Configurator configurator;
    private JSONObject colGenJson;
    private String tableName;
    private JLabel lblLabel;

    public ColumnGenEntry(JPanel panel, JSONObject colgen, String tableName, Configurator configurator)
    {
        this(panel, colgen, configurator, null);

        this.tableName = tableName;
    }

    public ColumnGenEntry(JPanel panel, JSONObject generator, Configurator configurator, JButton newEntityGenBtn)
    {
        super();

        this.parent = panel;
        this.newEntityGenBtn = newEntityGenBtn;
        this.configurator = configurator;
        this.colGenJson = generator;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        String label = NOT_CONFIGURED;
        if (newEntityGenBtn == null) {
            label = generator.getString(KEY_COLUMN);
        } else {
            // Get simple name to display
            if(generator.has(KEY_CLASSNAME)) {
                label = generator.getString(KEY_CLASSNAME);
                label = label.substring(label.lastIndexOf('.') + 1);
            }
        }

        lblLabel = new JLabel(label);
        add(lblLabel);
        setBgColor(configurator.getFindText());

        try {
            Image img = ImageIO.read(getClass().getResource("/edit.png"));
            ImageIcon icon = new ImageIcon(img);
            JButton editBtn = new JButton(icon);
            editBtn.setFocusPainted(false);
            editBtn.setContentAreaFilled(false); // flat button
            editBtn.addActionListener(new ActionListener()
            {
                public void actionPerformed (ActionEvent e)
                {
                    JFrame topFrame = (JFrame)SwingUtilities.getWindowAncestor(
                        (Component)e.getSource());
                    String title = newEntityGenBtn != null ?
                        "Edit entity generator" :
                        "Edit column generator";
                    ColumnGenEditor colGenEditor = new ColumnGenEditor(topFrame,
                        title, tableName,true, ColumnGenEntry.this, newEntityGenBtn != null);
                    colGenEditor.pack();
                    colGenEditor.setVisible(true);
                }
            });
            add(editBtn);

            img = ImageIO.read(getClass().getResource("/close2.png"));
            icon = new ImageIcon(img);
            JButton deleteBtn = new JButton(icon);
            deleteBtn.setFocusPainted(false);
            deleteBtn.setContentAreaFilled(false); // flat button
            deleteBtn.addActionListener(new DeleteActionListener(this.parent, this));
            add(deleteBtn);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setBgColor(String findText) {
        if(findText != null && getColumnName().toLowerCase().contains(findText.toLowerCase())) {
            setBackground(new Color(254, 216, 177));
        } else {
            setBackground(Color.lightGray);
        }
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return colGenJson.getString(KEY_COLUMN);
    }

    public void updateEntry(ColumnGenEditor editor) {
        String label = "";
        if (newEntityGenBtn == null) {
            label = editor.getColumnName().getText();
            colGenJson.put(KEY_COLUMN, label);
        } else {
            // Get simple name to display
            label = editor.getClassName().getText();
            colGenJson.put(KEY_CLASSNAME, label);
        }

        this.lblLabel.setText(label);
    }

    public JButton getNewEntityGenBtn() {
        return this.newEntityGenBtn;
    }

    public Configurator getConfigurator() {
        return this.configurator;
    }

    public JSONObject getColGenJson() {
        return this.colGenJson;
    }

    private static class DeleteActionListener implements ActionListener {
        private JPanel parent;
        private ColumnGenEntry columnGenEntry;

        public DeleteActionListener(JPanel parent, ColumnGenEntry columnGenEntry) {
            this.parent = parent;
            this.columnGenEntry = columnGenEntry;
        }

        public void actionPerformed (ActionEvent e)
        {
            if (columnGenEntry.getNewEntityGenBtn() != null) {
                this.columnGenEntry.getConfigurator().notify(
                    Configurator.OdorEvent.ENTITY_GENERATOR_REMOVED);
            }
            else {
                this.parent.remove(this.columnGenEntry);
            }

            this.parent.revalidate();
            this.parent.repaint();
        }
    }
}
