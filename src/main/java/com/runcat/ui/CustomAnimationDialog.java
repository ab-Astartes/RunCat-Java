package com.runcat.ui;

import com.runcat.animation.AnimationManager;
import com.runcat.i18n.I18nManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Dialog for importing and managing custom animations.
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
    private JButton deleteButton;
    private JButton renameButton;
    private JButton activateButton;
    private JLabel statusLabel;
    private Path selectedDir;
    private DefaultListModel<String> animationListModel;
    private JList<String> animationList;

    public CustomAnimationDialog(AnimationManager animationManager) {
        this.animationManager = animationManager;
        this.i18n = I18nManager.getInstance();
        initUI();
        refreshAnimationList();
    }

    private void initUI() {
        setTitle(i18n.get("custom.title"));
        setLayout(new BorderLayout(10, 10));
        setSize(680, 420);
        setLocationRelativeTo(null);
        setModal(true);

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.add(new JLabel(i18n.get("custom.name") + ":"));
        nameField = new JTextField(18);
        namePanel.add(nameField);
        topPanel.add(namePanel, BorderLayout.WEST);

        importButton = new JButton(i18n.get("custom.select"));
        importButton.addActionListener(e -> selectDirectory());
        topPanel.add(importButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        centerPanel.add(createImportPanel());
        centerPanel.add(createManagePanel());
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        statusLabel = new JLabel(" ");
        bottomPanel.add(statusLabel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        applyButton = new JButton(i18n.get("custom.apply"));
        applyButton.setEnabled(false);
        applyButton.addActionListener(e -> applyAnimation());
        cancelButton = new JButton(i18n.get("custom.cancel"));
        cancelButton.addActionListener(e -> dispose());
        actionPanel.add(applyButton);
        actionPanel.add(cancelButton);
        bottomPanel.add(actionPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createImportPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder(i18n.get("custom.import")));

        previewLabel = new JLabel(i18n.get("custom.description"), SwingConstants.CENTER);
        previewLabel.setPreferredSize(new Dimension(280, 220));
        previewLabel.setBorder(BorderFactory.createDashedBorder(Color.GRAY));
        panel.add(previewLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createManagePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder(i18n.get("custom.manage")));

        animationListModel = new DefaultListModel<>();
        animationList = new JList<>(animationListModel);
        animationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        animationList.addListSelectionListener(e -> updateSelectedAnimationPreview());
        panel.add(new JScrollPane(animationList), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(3, 1, 6, 6));
        activateButton = new JButton(i18n.get("custom.activate"));
        activateButton.addActionListener(e -> activateSelectedAnimation());
        renameButton = new JButton(i18n.get("custom.rename"));
        renameButton.addActionListener(e -> renameSelectedAnimation());
        deleteButton = new JButton(i18n.get("custom.delete"));
        deleteButton.addActionListener(e -> deleteSelectedAnimation());
        buttons.add(activateButton);
        buttons.add(renameButton);
        buttons.add(deleteButton);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void selectDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(i18n.get("custom.select"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File dir = chooser.getSelectedFile();
        selectedDir = Paths.get(dir.getAbsolutePath());
        List<Path> frames = animationManager.listAnimationFrames(selectedDir);
        if (frames.size() < 2) {
            statusLabel.setText(i18n.get("custom.noPngFound"));
            applyButton.setEnabled(false);
            previewLabel.setIcon(null);
            previewLabel.setText(i18n.get("custom.description"));
            return;
        }

        statusLabel.setText(i18n.get("custom.frames") + ": " + frames.size());
        previewLabel.setIcon(new ImageIcon(frames.get(0).toAbsolutePath().toString()));
        previewLabel.setText(String.format(i18n.get("custom.selected"), dir.getName(), frames.size()));
        if (nameField.getText().isEmpty()) {
            nameField.setText(dir.getName());
        }
        applyButton.setEnabled(true);
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
            animationManager.setCurrentAnimation(name);
            JOptionPane.showMessageDialog(this,
                    String.format(i18n.get("custom.importSuccess"), name),
                    i18n.get("app.title"), JOptionPane.INFORMATION_MESSAGE);
            applied = true;
            refreshAnimationList();
            animationList.setSelectedValue(name, true);
        } else {
            JOptionPane.showMessageDialog(this,
                    i18n.get("custom.importFailed"),
                    i18n.get("app.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshAnimationList() {
        animationListModel.clear();
        for (String name : animationManager.getAllAnimationNames()) {
            animationListModel.addElement(name);
        }
    }

    private void updateSelectedAnimationPreview() {
        String selected = animationList.getSelectedValue();
        if (selected == null) {
            return;
        }
        previewLabel.setIcon(animationManager.createPreviewIcon(selected, 160));
        previewLabel.setText(animationManager.getAnimationDisplayName(selected));
        boolean builtIn = animationManager.isBuiltInAnimation(selected);
        deleteButton.setEnabled(!builtIn);
        renameButton.setEnabled(!builtIn);
        activateButton.setEnabled(true);
    }

    private void activateSelectedAnimation() {
        String selected = animationList.getSelectedValue();
        if (selected == null) return;
        animationManager.setCurrentAnimation(selected);
        applied = true;
        statusLabel.setText(String.format(i18n.get("custom.activated"), selected));
    }

    private void renameSelectedAnimation() {
        String selected = animationList.getSelectedValue();
        if (selected == null || animationManager.isBuiltInAnimation(selected)) return;
        String newName = JOptionPane.showInputDialog(this, i18n.get("custom.renamePrompt"), selected);
        if (newName == null || newName.isBlank()) return;
        if (animationManager.renameCustomAnimation(selected, newName.trim())) {
            refreshAnimationList();
            animationList.setSelectedValue(newName.trim(), true);
            applied = true;
        } else {
            JOptionPane.showMessageDialog(this, i18n.get("custom.renameFailed"),
                    i18n.get("app.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedAnimation() {
        String selected = animationList.getSelectedValue();
        if (selected == null || animationManager.isBuiltInAnimation(selected)) return;
        int result = JOptionPane.showConfirmDialog(
                this,
                String.format(i18n.get("custom.deletePrompt"), selected),
                i18n.get("custom.delete"),
                JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) return;
        if (animationManager.removeCustomAnimation(selected)) {
            refreshAnimationList();
            previewLabel.setIcon(null);
            previewLabel.setText(i18n.get("custom.description"));
            applied = true;
        }
    }

    public boolean isApplied() {
        return applied;
    }
}
