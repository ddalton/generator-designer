package tools.xor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class TableDepEntry extends JPanel {

    private JPanel parent;
    private Configurator configurator;
    private String tableName;
    private JLabel lblLabel;

    public TableDepEntry(JPanel panel, Configurator configurator, String label)
    {
        super();

        this.parent = panel;
        this.configurator = configurator;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        lblLabel = new JLabel(label);
        add(lblLabel);
        setBackground(Color.lightGray);

        try {
            Image img = ImageIO.read(getClass().getResource("/close2.png"));
            ImageIcon icon = new ImageIcon(img);
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

    public Configurator getConfigurator() {
        return this.configurator;
    }

    private static class DeleteActionListener implements ActionListener {
        private JPanel parent;
        private TableDepEntry tableDepEntry;

        public DeleteActionListener(JPanel parent, TableDepEntry tableDepEntry) {
            this.parent = parent;
            this.tableDepEntry = tableDepEntry;
        }

        public void actionPerformed (ActionEvent e)
        {
            this.parent.remove(this.tableDepEntry);

            this.parent.revalidate();
            this.parent.repaint();
        }
    }
}
