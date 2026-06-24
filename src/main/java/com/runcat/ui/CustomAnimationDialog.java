package com.runcat.ui;

import com.runcat.animation.AnimationManager;
import com.runcat.i18n.I18nManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Dialog for importing and managing custom animations
 */
public class CustomAnimationDialog extends JDialog {

    private final AnimationManager animationManager;
    private final I18nManager i18n;
    private boolean applied = false;

    private JTextField nameField;
    private JLabel previewLabel;
    private JButton importButton;
    private JButton applyButton;
    private JButton cancelButton;
    private JLabel statusLabel;
    private Path selectedDir;

    public CustomAnimationDialog(AnimationManager animationManager) {
        this.animationManager = animationManager;
        this.i18n = I18nManager.getInstance();
        initUI();
    }

    private void initUI() {
        setTitle(i18n.get("custom.title"));
        setLayout(new BorderLayout(10, 10));
        setSize(450, 380);
        setLocationRelativeTo(null);
        setModal(true);

        // Top panel: name input
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel(i18n.get("custom.name") + ":"));
        nameField = new JTextField(15);
        topPanel.add(nameField);
        add(topPanel, BorderLayout.NORTH);

        // Center: preview area
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        previewLabel = new JLabel(i18n.get("custom.description"), SwingConstants.CENTER);
        previewLabel.setPreferredSize(new Dimension(400, 200));
        previewLabel.setBorder(BorderFactory.createDashedBorder(Color.GRAY));
        centerPanel.add(previewLabel, BorderLayout.CENTER);

        // Import button
        importButton = new JButton(i18n.get("custom.select"));
        importButton.addActionListener(e -> selectDirectory());
        JPanel importPanel = new JPanel();
        importPanel.add(importButton);
        centerPanel.add(importPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom: buttons
        JPanel bottomPanel = new JPanel(new FlowLayout());
        applyButton = new JButton(i18n.get("custom.apply"));
        applyButton.setEnabled(false);
        applyButton.addActionListener(e -> applyAnimation());

        cancelButton = new JButton(i18n.get("custom.cancel"));
        cancelButton.addActionListener(e -> dispose());

        bottomPanel.add(applyButton);
        bottomPanel.add(cancelButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Status
        statusLabel = new JLabel(" ");
        add(statusLabel, BorderLayout.PAGE_END);
    }

    private void selectDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(i18n.get("custom.select"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            selectedDir = Paths.get(dir.getAbsolutePath());

            // Count PNG files
            File[] pngFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
            if (pngFiles != null && pngFiles.length > 0) {
                statusLabel.setText(i18n.get("custom.frames") + ": " + pngFiles.length);
                previewLabel.setText(String.format(i18n.get("custom.selected"), dir.getName(), pngFiles.length));
                if (nameField.getText().isEmpty()) {
                    nameField.setText(dir.getName());
                }
                applyButton.setEnabled(true);
            } else {
                statusLabel.setText(i18n.get("custom.noPngFound"));
                applyButton.setEnabled(false);
            }
        }
    }

    private void applyAnimation() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, i18n.get("custom.enterName"),
                    i18n.get("app.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedDir == null) {
            JOptionPane.showMessageDialog(this, i18n.get("custom.selectDirFirst"),
                    i18n.get("app.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean success = animationManager.importCustomAnimation(name, selectedDir);
        if (success) {
            JOptionPane.showMessageDialog(this,
                    String.format(i18n.get("custom.importSuccess"), name),
                    i18n.get("app.title"), JOptionPane.INFORMATION_MESSAGE);
            applied = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    i18n.get("custom.importFailed"),
                    i18n.get("app.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isApplied() {
        return applied;
    }
}
