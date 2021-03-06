package controls;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import pojo.ProgressiveState;
import pojo.ProgressiveViewItem;
import utils.ControlBuilder;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by salterok on 04.05.2015.
 */
public class ProgressiveTabPane extends BorderPane {
    @FXML
    private Button helpBtn;
    @FXML
    private Button escapeBtn;
    @FXML
    private VBox progressiveBar;
    @FXML
    private AnchorPane content;

    private List<ProgressiveViewItem> navBar = new ArrayList<>();
    private int currentItemIndex = -1;
    private ProgressiveState progressiveState = new ProgressiveState();


    public ProgressiveTabPane() throws Exception {
        ControlBuilder.bindView(this);
    }

    public void addProgressItem(ProgressiveViewItem item) {
        navBar.add(item);
    }

    public void setup() {
        drawProgressiveBar();
        initProgressiveItems();
        Optional<ProgressiveViewItem> item = navBar.stream().findFirst();
        if (item.isPresent())
            setContent(item.get(), null);
    }

    private void initProgressiveItems() {
        navBar.forEach(item -> {
            item.instance.setNavigation(item.navs);
            item.instance.setPrevCommand(this::navigatePrev);
            item.instance.setNextCommand(this::navigateNext);
            item.instance.setCustomCommand(this::navigateCustom);
            item.instance.setStateGetter(this::stateGetter);

            AnchorPane.setTopAnchor(item.instance, 0d);
            AnchorPane.setBottomAnchor(item.instance, 0d);
            AnchorPane.setLeftAnchor(item.instance, 0d);
            AnchorPane.setRightAnchor(item.instance, 0d);
        });
    }

    private ProgressiveState stateGetter() {
        return this.progressiveState;
    }

    private void navigateNext() {
        int nextIndex = currentItemIndex + 1;
        if (navBar.size() > nextIndex) {
            setContent(navBar.get(nextIndex), navBar.get(currentItemIndex));
        }
    }

    private void navigatePrev() {
        int prevIndex = currentItemIndex - 1;
        if (prevIndex >= 0) {
            setContent(navBar.get(prevIndex), navBar.get(currentItemIndex));
        }
    }

    private void navigateCustom(String key) {
        Optional<ProgressiveViewItem> pane = navBar.stream()
                .filter(entry -> entry.id.equals(key))
                .findFirst();
        if (pane.isPresent())

            setContent(pane.get(), navBar.get(currentItemIndex));
        else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText(String.format("There is no view '%s'", key));
            alert.show();
        }
    }

    private void drawProgressiveBar() {
        List<HBox> items = navBar.stream().map(entry -> {
            HBox hBox = new HBox();
            Label label = new Label(entry.title);
            HBox.setHgrow(label, Priority.ALWAYS);
            hBox.paddingProperty().setValue(new Insets(2.0, 0.0, 2.0, 0.0));
            hBox.getChildren().add(label);
            hBox.setStyle("-fx-background-color: #2c2;");
            return hBox;
        }).collect(Collectors.toList());
        progressiveBar.getChildren().addAll(items);
    }

    private void setContent(ProgressiveViewItem item, ProgressiveViewItem oldItem) {
        if (oldItem != null) {
            oldItem.instance.triggerHide();
        }
        content.getChildren().clear();
        currentItemIndex = navBar.indexOf(item);
        content.getChildren().add(item.instance);
        item.instance.triggerShow();
    }
}
