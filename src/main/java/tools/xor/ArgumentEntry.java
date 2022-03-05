package tools.xor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ArgumentEntry extends JPanel {

    private JPanel parent;
    private JTextField jtfValue;
    private int position;
    private ColumnGenEditor editor;

    public ArgumentEntry(JPanel panel, ColumnGenEditor editor, String colgen, int position) {
        super();

        this.parent = panel;
        this.editor = editor;
        this.position = position;

        setLayout(new BorderLayout());

        JPanel argumentPanel = new JPanel(new BorderLayout());
        JPanel valuePanel = new JPanel();
        valuePanel.setLayout(new BoxLayout(valuePanel, BoxLayout.X_AXIS));

        jtfValue = new JTextField(colgen);
        jtfValue.setColumns(30);
        valuePanel.add(jtfValue);
        valuePanel.setBackground(Color.lightGray);

        try {
            Image img = ImageIO.read(getClass().getResource("/close2.png"));
            ImageIcon icon = new ImageIcon(img);
            JButton deleteBtn = new JButton(icon);
            deleteBtn.setFocusPainted(false);
            deleteBtn.setContentAreaFilled(false); // flat button
            deleteBtn.setToolTipText("Delete this argument");
            deleteBtn.addActionListener(new DeleteActionListener(this.parent, this));
            valuePanel.add(deleteBtn);

            Image imgOrig = ImageIO.read(Configurator.class.getResource("/new_big.png"));
            img = imgOrig.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
            icon = new ImageIcon(img);
            JButton newBtn = new JButton(icon);
            newBtn.setFocusPainted(false);
            newBtn.setContentAreaFilled(false); // flat button
            newBtn.setToolTipText("Insert a new argument entry above current");
            newBtn.addActionListener(new AddActionListener(this.parent, this));
            argumentPanel.add(newBtn, BorderLayout.EAST);

            Image insertImg = ImageIO.read(getClass().getResource("/insert_light.png"));
            JLabel arrowLabel = new JLabel(new ImageIcon(insertImg));

            argumentPanel.add(valuePanel, BorderLayout.CENTER);

            add(arrowLabel, BorderLayout.EAST);
            add(argumentPanel, BorderLayout.SOUTH);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getValue() {
        return jtfValue.getText();
    }

    private static class DeleteActionListener implements ActionListener {
        private JPanel parent;
        private ArgumentEntry argumentEntry;

        public DeleteActionListener(JPanel parent, ArgumentEntry argument) {
            this.parent = parent;
            this.argumentEntry = argument;
        }

        private int getPosition() {
            return argumentEntry.position;
        }

        public void actionPerformed (ActionEvent e)
        {
            int position = getPosition();

            // We start with -1 to account for the entry that is going to be removed
            int numArguments = -1;
            for (Component c : this.parent.getComponents()) {
                if (c instanceof ArgumentEntry) {
                    ArgumentEntry ae = (ArgumentEntry)c;
                    numArguments++;
                    if(ae.position > position) {
                        System.out.println(String.format("    Updating position from %s to %s due to remove at %s", ae.position, (ae.position-1), position));
                        --ae.position;
                    }
                }
            }
            argumentEntry.editor.updateIntChbx(numArguments);

            this.parent.remove(this.argumentEntry);
            this.parent.revalidate();
            this.parent.repaint();
        }
    }

    static class AddActionListener implements ActionListener {
        private JPanel parent;
        private ArgumentEntry argumentEntry;

        public AddActionListener(JPanel parent, ArgumentEntry caller) {
            this.parent = parent;
            this.argumentEntry = caller;
        }

        private int getPosition() {
            return argumentEntry.position;
        }

        public void actionPerformed (ActionEvent e)
        {
            int position = getPosition();

            // renumber the positions
            // -1 means we add it last
            int numArguments = 0;
            for (Component c : this.parent.getComponents()) {
                if (c instanceof ArgumentEntry) {
                    ArgumentEntry ae = (ArgumentEntry)c;
                    numArguments++;
                    if(position != -1 && ae.position >= position) {
                        System.out.println(String.format("    Updating position from %s to %s due to insert at %s", ae.position, (ae.position+1), position));
                        ++ae.position;
                    }
                }
            }

            int argPosition = position;
            if(argPosition == -1) {
                argPosition = numArguments;
            }
            System.out.println("Adding at position: " + argPosition);
            this.parent.add(new ArgumentEntry(this.parent, argumentEntry.editor, "", argPosition), argPosition);
            this.parent.revalidate();
            this.parent.repaint();

            // Add 1 to account for the entry being added
            argumentEntry.editor.updateIntChbx(numArguments+1);
        }
    }
}
