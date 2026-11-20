package io.softwareecg.awb.tools.h2browser;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.h2.jdbcx.JdbcDataSource;

import java.io.Reader;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Standalone JavaFX application for browsing the Software-EKG H2 metadata database.
 * This is a read-only debugging tool for inspecting the embedded H2 database contents.
 *
 * Launch directly via {@code main()} -- no FXML required.
 */
public class H2BrowserApp extends Application {

    private static final String DB_URL = "jdbc:h2:" + System.getProperty("user.home")
            + "/.ekg/metadata;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private static final String[] TABLE_NAMES = {
            "projects",
            "project_dimension_aliases",
            "repositories",
            "bookmark_groups",
            "metric_bookmarks"
    };

    /** Maps table name to the column used for project-based filtering. */
    private static final Map<String, String> FILTER_COLUMNS = new LinkedHashMap<>();

    static {
        FILTER_COLUMNS.put("projects", "name");
        FILTER_COLUMNS.put("project_dimension_aliases", "project_name");
        FILTER_COLUMNS.put("repositories", "according_project_name");
        FILTER_COLUMNS.put("bookmark_groups", "project_name");
        FILTER_COLUMNS.put("metric_bookmarks", "project_name");
    }

    private static final int CLOB_TRUNCATE_LENGTH = 100;

    private TreeView<String> tableTreeView;
    private TableView<ObservableList<String>> dataTableView;
    private Label tableNameLabel;
    private TextField filterField;
    private Connection connection;

    // ------------------------------------------------------------------ //
    //  Application lifecycle
    // ------------------------------------------------------------------ //

    @Override
    public void start(Stage primaryStage) {
        connectToDatabase();

        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.25);
        splitPane.getItems().addAll(buildLeftPane(), buildRightPane());

        Scene scene = new Scene(splitPane, 1200, 700);
        scene.getStylesheets().add("data:text/css," + inlineCss());

        primaryStage.setTitle("Software-EKG H2 Metadata Browser");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> disconnect());
        primaryStage.show();

        refreshTree();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // ------------------------------------------------------------------ //
    //  UI construction
    // ------------------------------------------------------------------ //

    private VBox buildLeftPane() {
        Label header = new Label("H2 Metadata Browser");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setPadding(new Insets(0, 0, 6, 0));

        filterField = new TextField();
        filterField.setPromptText("Projekt-Filter...");
        HBox.setHgrow(filterField, Priority.ALWAYS);

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshTree());

        HBox filterBox = new HBox(6, filterField, refreshButton);
        filterBox.setPadding(new Insets(0, 0, 6, 0));

        tableTreeView = new TreeView<>();
        tableTreeView.setShowRoot(false);
        tableTreeView.setRoot(new TreeItem<>("Tables"));
        tableTreeView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> onTableSelected(newVal));
        VBox.setVgrow(tableTreeView, Priority.ALWAYS);

        VBox left = new VBox(6, header, filterBox, tableTreeView);
        left.setPadding(new Insets(10));
        return left;
    }

    private VBox buildRightPane() {
        tableNameLabel = new Label("Select a table");
        tableNameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        tableNameLabel.setPadding(new Insets(0, 0, 6, 0));

        dataTableView = new TableView<>();
        dataTableView.setEditable(false);
        dataTableView.setPlaceholder(new Label("No data"));
        dataTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(dataTableView, Priority.ALWAYS);

        VBox right = new VBox(6, tableNameLabel, dataTableView);
        right.setPadding(new Insets(10));
        return right;
    }

    // ------------------------------------------------------------------ //
    //  Database helpers
    // ------------------------------------------------------------------ //

    private void connectToDatabase() {
        try {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL(DB_URL);
            ds.setUser(DB_USER);
            ds.setPassword(DB_PASSWORD);
            connection = ds.getConnection();
        } catch (SQLException ex) {
            showError("Failed to connect to H2 database", ex);
        }
    }

    private void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // closing silently on exit
        }
    }

    // ------------------------------------------------------------------ //
    //  Tree population
    // ------------------------------------------------------------------ //

    private void refreshTree() {
        TreeItem<String> root = tableTreeView.getRoot();
        root.getChildren().clear();

        String filter = filterField != null ? filterField.getText().trim() : "";

        for (String table : TABLE_NAMES) {
            int count = countRows(table, filter);
            TreeItem<String> item = new TreeItem<>(table + " (" + count + ")");
            root.getChildren().add(item);
        }
        root.setExpanded(true);

        // Clear the table view on refresh
        dataTableView.getColumns().clear();
        dataTableView.getItems().clear();
        tableNameLabel.setText("Select a table");
    }

    private int countRows(String table, String filter) {
        try {
            String sql = "SELECT COUNT(*) FROM " + table;
            sql = appendFilterClause(sql, table, filter);

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            // Table may not exist yet -- return 0
            return 0;
        }
    }

    // ------------------------------------------------------------------ //
    //  Table data loading
    // ------------------------------------------------------------------ //

    private void onTableSelected(TreeItem<String> item) {
        if (item == null || item.getValue() == null) return;

        // Extract table name from "tablename (N)"
        String value = item.getValue();
        String tableName = value.contains(" (") ? value.substring(0, value.indexOf(" (")) : value;
        loadTableData(tableName);
    }

    private void loadTableData(String tableName) {
        dataTableView.getColumns().clear();
        dataTableView.getItems().clear();
        tableNameLabel.setText(tableName);

        String filter = filterField != null ? filterField.getText().trim() : "";

        try {
            String sql = "SELECT * FROM " + tableName;
            sql = appendFilterClause(sql, tableName, filter);

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                // Build columns dynamically from metadata
                List<String> columnNames = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String colName = meta.getColumnName(i);
                    columnNames.add(colName);
                    final int colIndex = i - 1;

                    TableColumn<ObservableList<String>, String> col = new TableColumn<>(colName);
                    col.setCellValueFactory(param -> {
                        ObservableList<String> row = param.getValue();
                        return new SimpleStringProperty(
                                colIndex < row.size() ? row.get(colIndex) : "");
                    });
                    col.setMinWidth(80);
                    dataTableView.getColumns().add(col);
                }

                // Populate rows
                ObservableList<ObservableList<String>> rows = FXCollections.observableArrayList();
                while (rs.next()) {
                    ObservableList<String> row = FXCollections.observableArrayList();
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(readCellValue(rs, i, meta.getColumnType(i)));
                    }
                    rows.add(row);
                }
                dataTableView.setItems(rows);
            }
        } catch (SQLException ex) {
            showError("Failed to load table: " + tableName, ex);
        }
    }

    /**
     * Reads a single cell value, truncating CLOB fields to {@value #CLOB_TRUNCATE_LENGTH} chars.
     */
    private String readCellValue(ResultSet rs, int columnIndex, int sqlType) throws SQLException {
        if (sqlType == Types.CLOB) {
            Clob clob = rs.getClob(columnIndex);
            if (clob == null) return "";
            try {
                long length = clob.length();
                if (length <= CLOB_TRUNCATE_LENGTH) {
                    return clob.getSubString(1, (int) length);
                }
                return clob.getSubString(1, CLOB_TRUNCATE_LENGTH) + "...";
            } finally {
                clob.free();
            }
        }

        // CHARACTER LARGE OBJECT may also appear as Types.LONGVARCHAR
        if (sqlType == Types.LONGVARCHAR || sqlType == Types.LONGNVARCHAR) {
            try (Reader reader = rs.getCharacterStream(columnIndex)) {
                if (reader == null) return "";
                char[] buffer = new char[CLOB_TRUNCATE_LENGTH + 1];
                int charsRead = reader.read(buffer);
                if (charsRead <= 0) return "";
                if (charsRead <= CLOB_TRUNCATE_LENGTH) {
                    return new String(buffer, 0, charsRead);
                }
                return new String(buffer, 0, CLOB_TRUNCATE_LENGTH) + "...";
            } catch (Exception e) {
                return rs.getString(columnIndex);
            }
        }

        String value = rs.getString(columnIndex);
        return value != null ? value : "";
    }

    // ------------------------------------------------------------------ //
    //  Filtering
    // ------------------------------------------------------------------ //

    private String appendFilterClause(String sql, String table, String filter) {
        if (filter == null || filter.isEmpty()) return sql;

        String column = FILTER_COLUMNS.get(table.toLowerCase());
        if (column == null) return sql;

        // Use UPPER for case-insensitive matching
        return sql + " WHERE UPPER(" + column + ") LIKE UPPER('%" + escapeSQL(filter) + "%')";
    }

    private static String escapeSQL(String input) {
        return input.replace("'", "''");
    }

    // ------------------------------------------------------------------ //
    //  Inline CSS
    // ------------------------------------------------------------------ //

    private String inlineCss() {
        return String.join("",
                ".table-view { -fx-font-size: 13px; }",

                ".table-view .column-header {",
                "  -fx-background-color: %233a3f47;",
                "  -fx-text-fill: white;",
                "  -fx-font-weight: bold;",
                "  -fx-padding: 6 8 6 8;",
                "}",

                ".table-view .column-header .label {",
                "  -fx-text-fill: white;",
                "}",

                ".table-row-cell:odd {",
                "  -fx-background-color: %23f4f4f4;",
                "}",

                ".table-row-cell:even {",
                "  -fx-background-color: white;",
                "}",

                ".table-row-cell:selected {",
                "  -fx-background-color: %234a90d9;",
                "  -fx-text-fill: white;",
                "}",

                ".tree-view { -fx-font-size: 13px; }",

                ".split-pane-divider {",
                "  -fx-background-color: %23cccccc;",
                "}"
        );
    }

    // ------------------------------------------------------------------ //
    //  Error dialog
    // ------------------------------------------------------------------ //

    private void showError(String header, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Database Error");
        alert.setHeaderText(header);
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }
}
