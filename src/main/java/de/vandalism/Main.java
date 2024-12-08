package de.vandalism;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String FILE_NAME = "knownPlugins.json";
    private static List<Entry> entries = new ArrayList<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        // Load data from file on startup
        loadData();

        // Create the main frame
        final JFrame frame = new JFrame("JSON List App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setLayout(new BorderLayout());

        // Create input fields
        final JTextField nameField = new JTextField();
        final JTextField descriptionField = new JTextField();
        final JTextField websiteField = new JTextField();

        nameField.setBorder(BorderFactory.createTitledBorder("Name"));
        descriptionField.setBorder(BorderFactory.createTitledBorder("Description"));
        websiteField.setBorder(BorderFactory.createTitledBorder("Website"));

        // Add to a panel
        final JPanel inputPanel = new JPanel(new GridLayout(4, 1));
        inputPanel.add(nameField);
        inputPanel.add(descriptionField);
        inputPanel.add(websiteField);

        final JButton addButton = new JButton("Add");
        inputPanel.add(addButton);

        // Create a panel to hold the list and delete buttons
        final JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        // Create a search field
        final JTextField searchField = new JTextField();
        searchField.setBorder(BorderFactory.createTitledBorder("Search"));

        // Add search functionality
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterList(listPanel, searchField.getText());
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterList(listPanel, searchField.getText());
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterList(listPanel, searchField.getText());
            }
        });

        // Add components to the frame
        final JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(inputPanel, BorderLayout.NORTH);
        topPanel.add(searchField, BorderLayout.SOUTH);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Add button action listener
        addButton.addActionListener(e -> {
            final String name = nameField.getText().trim();
            final String description = descriptionField.getText().trim();
            final String website = websiteField.getText().trim();

            if (!name.isEmpty() && !description.isEmpty() && !website.isEmpty()) {
                // Check for duplicate name
                if (entries.stream().anyMatch(entry -> entry.name.equalsIgnoreCase(name))) {
                    JOptionPane.showMessageDialog(frame, "The name already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Validate website (must start with https://)
                if (!website.startsWith("https://")) {
                    JOptionPane.showMessageDialog(frame, "The website must be a valid HTTPS link (start with https://)", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                final Entry entry = new Entry(name, description, website);
                entries.add(entry);

                // Add to list panel
                addEntryToListPanel(listPanel, entry);

                // Save to JSON
                saveData();

                // Clear fields
                nameField.setText("");
                descriptionField.setText("");
                websiteField.setText("");
            } else {
                JOptionPane.showMessageDialog(frame, "All fields must be filled out!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Populate list panel on load
        for (final Entry entry : entries) {
            addEntryToListPanel(listPanel, entry);
        }

        frame.setVisible(true);
    }

    // Save entries to JSON file
    private static void saveData() {
        try (final FileWriter writer = new FileWriter(FILE_NAME)) {
            gson.toJson(entries, writer);
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    // Load entries from JSON file
    private static void loadData() {
        final File file = new File(FILE_NAME);

        if (file.exists()) {
            try (final FileReader reader = new FileReader(file)) {
                final Type listType = new TypeToken<List<Entry>>() {}.getType();
                entries = gson.fromJson(reader, listType);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    // Filter the list based on the search query
    private static void filterList(JPanel listPanel, String query) {
        listPanel.removeAll();

        final List<Entry> filteredEntries = entries.stream()
                .filter(entry -> entry.name.toLowerCase().contains(query.toLowerCase()) ||
                        entry.description.toLowerCase().contains(query.toLowerCase()) ||
                        entry.website.toLowerCase().contains(query.toLowerCase()))
                .toList();

        for (final Entry entry : filteredEntries) {
            addEntryToListPanel(listPanel, entry);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    // Add entry to list panel
    private static void addEntryToListPanel(JPanel listPanel, Entry entry) {
        final JPanel entryPanel = new JPanel();
        entryPanel.setLayout(new BorderLayout());
        entryPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        entryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // Format the text
        final String formattedText = "<html>" +
                "Name: <b>" + entry.name + "</b><br>" +
                "Description: " + entry.description + "<br>" +
                "Website: " + entry.website +
                "</html>";
        final JLabel label = new JLabel(formattedText);

        // Buttons panel
        final JPanel buttonsPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        final JButton editButton = new JButton("Edit");
        final JButton deleteButton = new JButton("Delete");

        editButton.setPreferredSize(new Dimension(100, 30));
        deleteButton.setPreferredSize(new Dimension(100, 30));

        // Edit button action
        editButton.addActionListener(e -> {
            final JTextField nameField = new JTextField(entry.name);
            final JTextField descriptionField = new JTextField(entry.description);
            final JTextField websiteField = new JTextField(entry.website);

            final Object[] message = {
                    "Name:", nameField,
                    "Description:", descriptionField,
                    "Website:", websiteField
            };

            final int option = JOptionPane.showConfirmDialog(null, message, "Edit Entry", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                // Validate website
                if (!websiteField.getText().startsWith("https://")) {
                    JOptionPane.showMessageDialog(
                            null, "The website must start with https://", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Update entry
                entry.name = nameField.getText().trim();
                entry.description = descriptionField.getText().trim();
                entry.website = websiteField.getText().trim();

                // Save changes
                saveData();

                // Refresh list panel
                listPanel.removeAll();
                for (final Entry updatedEntry : entries) {
                    addEntryToListPanel(listPanel, updatedEntry);
                }

                listPanel.revalidate();
                listPanel.repaint();
            }
        });

        // Delete button action
        deleteButton.addActionListener(e -> {
            final int confirmation = JOptionPane.showConfirmDialog(
                    null,
                    "Are you sure you want to delete this entry?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirmation == JOptionPane.YES_OPTION) {
                entries.remove(entry);
                listPanel.remove(entryPanel);
                listPanel.revalidate();
                listPanel.repaint();
                saveData();
            }
        });

        buttonsPanel.add(editButton);
        buttonsPanel.add(deleteButton);

        entryPanel.add(label, BorderLayout.CENTER);
        entryPanel.add(buttonsPanel, BorderLayout.EAST);

        listPanel.add(entryPanel);
        listPanel.revalidate();
        listPanel.repaint();
    }

    private static class Entry {
        private String name;
        private String description;
        private String website;

        public Entry(String name, String description, String website) {
            this.name = name;
            this.description = description;
            this.website = website;
        }
    }

}