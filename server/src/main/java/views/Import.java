/**
 * Created by salterok on 10.05.2015.
 */

package views;

import app.ImportStage;
import constants.ImportColumns;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import app.Config;
import pojo.LocalConfig;
import utils.TableViewUtils;
import utils.TaskUtil;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Import extends BaseNavigableView {
    @FXML
    private TableView<List<String>> importPreviewTable;
    @FXML
    private StackPane previewStack;
    @FXML
    private ComboBox<String> importFileDelimiter;
    @FXML
    private ComboBox<String> importFileEncoding;
    @FXML
    private TextField importBillPattern;
    @FXML
    private ProgressIndicator previewProgressIndicator = null;

    private ImportStage importStage = new ImportStage();

    public Import() throws Exception {}

    @FXML
    private void selectImportFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Standard CSV", "*.csv"));
        chooser.setTitle(getString("file-chooser-title"));
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            stateSupplier.get().importState.selectedForImport = file;
        }
    }

    @FXML
    private void previewSelectionChanged(Event event) {
        if (event.getSource() instanceof Tab) {
            Tab tab = (Tab)event.getSource();
            if (tab.isSelected()) {
                LocalConfig.CSVReadProps props = getCSVReadProps();
                TaskUtil.run(this::previewImport, props).thenUI(this::updatePreviewTable);
            }
        }
    }

    private LocalConfig.CSVReadProps getCSVReadProps() {
        LocalConfig.CSVReadProps props = new LocalConfig.CSVReadProps();
        props.file = stateSupplier.get().importState.selectedForImport;
        props.delim = importFileDelimiter.getValue();
        props.encoding = importFileEncoding.getValue();
        props.billPattern = importBillPattern.getText();
        return props;
    }

    private void toggleProgressIndicator() {
        if (previewProgressIndicator == null) {
            previewProgressIndicator = new ProgressIndicator();
            previewStack.getChildren().add(previewProgressIndicator);
        }
        else {
            previewStack.getChildren().remove(previewProgressIndicator);
            previewProgressIndicator = null;
        }
    }

    private LocalConfig.PreviewResult previewImport(LocalConfig.CSVReadProps props) {
        Platform.runLater(this::toggleProgressIndicator);
        try {
            Thread.sleep(200);
            LocalConfig.PreviewResult result = importStage.getPreviewData(props);
            // finally must be called after getPreviewData - local var for this case
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            Platform.runLater(this::toggleProgressIndicator);
        }
        return null;
    }

    @Override
    protected void onLoad() {
        Config config = Config.getInstance();
        importFileDelimiter.setItems(FXCollections.observableList(config.anImport.csvDelimiters));
        importFileDelimiter.setValue(config.anImport.csvDelimiters.get(0));
        importFileEncoding.setItems(FXCollections.observableList(config.anImport.fileEncoding));
        importFileEncoding.setValue(config.anImport.fileEncoding.get(0));
    }

    private void updatePreviewTable(LocalConfig.PreviewResult result) {
        if (result != null) {
            importPreviewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            addCustomColumns(importPreviewTable, result.columns, Config.getInstance().anImport.columns);
            importPreviewTable.setItems(FXCollections.observableList(result.items));
        }
    }

    private void addCustomColumns(TableView<List<String>> table, int count, Map<ImportColumns, String> options) {
        ObservableList<TableColumn<List<String>, ?>> columns = table.getColumns();
        columns.clear();
        columns.addAll(Collections.nCopies(count, 0)
                .stream()
                .map(i -> createChooserColumn(options))
                .collect(Collectors.toList()));
    }

    private TableColumn<List<String>, String> createChooserColumn(Map<ImportColumns, String> options) {
        TableColumn<List<String>, String> col = new TableColumn<>();
        ChoiceBox<Map.Entry<ImportColumns, String>> choiceBox = new ChoiceBox<>();
        choiceBox.setMaxWidth(1.7976931348623157E308);
        ObservableList<Map.Entry<ImportColumns, String>> list = FXCollections.observableArrayList(options.entrySet());
        choiceBox.setItems(list);
        choiceBox.setValue(list.get(0));
        choiceBox.setConverter(new StringConverter<Map.Entry<ImportColumns, String>>() {
            @Override
            public String toString(Map.Entry<ImportColumns, String> object) {
                return getLocalized(object.getValue());
            }

            @Override
            public Map.Entry<ImportColumns, String> fromString(String string) {
                return null;
            }
        });
        col.setGraphic(choiceBox);
        col.setCellValueFactory(TableViewUtils::readOnlyIndexedCellValueFactory);
        return col;
    }

    @Override
    protected void next(ActionEvent event) {
        Button btn = (Button) event.getSource();
        btn.setDisable(true);
        btn.setText(getString("import_importing"));
        Cursor cursor = this.getCursor();
        this.setCursor(Cursor.WAIT);
        //this.setDisable(true);

        LocalConfig.CSVReadProps props = getCSVReadProps();

        TaskUtil.run(this::importSprintData, props).thenUI(c -> {
            btn.setDisable(false);
            btn.setText(getString("make_import"));
            this.setCursor(cursor);
            nextCommand.run();
        });
    }

    private Void importSprintData(LocalConfig.CSVReadProps props) {
        try {
            List<TableColumn<List<String>, ?>> columns = importPreviewTable.getColumns();
            List<Map.Entry<ImportColumns, Integer>> dataCols = columns.stream().map(c -> {
                ChoiceBox<Map.Entry<ImportColumns, String>> choiceBox = (ChoiceBox) c.getGraphic();
                return new AbstractMap.SimpleEntry<>(choiceBox.getValue().getKey(), columns.indexOf(c));
            }).filter(c -> c.getKey() != ImportColumns.UNUSED).collect(Collectors.toList());

            Map<ImportColumns, Integer> map = new HashMap<>();
            for (Map.Entry<ImportColumns, Integer> col : dataCols) {
                map.put(col.getKey(), col.getValue());
            }
            props.columnsBinding = map;

            importStage.importSprint(props);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
