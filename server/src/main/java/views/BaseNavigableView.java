package views;

import com.sun.prism.impl.BaseMeshView;
import constants.NavigationMethod;
import controls.IProgressiveBasicRouting;
import controls.IProgressiveCustomRouting;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pojo.NavigationDescriptor;
import pojo.ProgressiveState;
import utils.ControlBuilder;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by salterok on 16.05.2015.
 */
public class BaseNavigableView extends BorderPane implements IProgressiveBasicRouting, IProgressiveCustomRouting {
    private static final Logger logger = LogManager.getLogger(BaseMeshView.class);
    private static final String I18N_DESCRIPTION_KEY = "stage_desc";
    protected Runnable nextCommand;
    protected Runnable prevCommand;
    protected Consumer<String> customCommand;
    protected ResourceBundle resourceBundle;
    protected Supplier<ProgressiveState> stateSupplier;

    public BaseNavigableView() throws Exception {
        resourceBundle = ControlBuilder.bindView(this);

        Platform.runLater(this::init); // postpone execution to allow object finish construction
    }

    @Override
    public void setStateGetter(Supplier<ProgressiveState> stateGetter) {
        stateSupplier = stateGetter;
    }

    @Override
    public void setNextCommand(Runnable nextCommand) {
        this.nextCommand = nextCommand;
    }

    @Override
    public void setPrevCommand(Runnable prevCommand) {
        this.prevCommand = prevCommand;
    }

    @Override
    public void setCustomCommand(Consumer<String> customCommand) {
        this.customCommand = customCommand;
    }

    @FXML
    protected void next(ActionEvent event) {
        nextCommand.run();
    }

    @FXML
    protected void prev(ActionEvent event) {
        prevCommand.run();
    }

    @FXML
    protected void custom(ActionEvent event) {
        Button btn = (Button)event.getSource();
        NavigationDescriptor desc = (NavigationDescriptor)btn.getUserData();
        customCommand.accept(desc.value);
    }

    public final void triggerHide() {
        Platform.runLater(this::onHide);
    }

    public final void triggerShow() {
        Platform.runLater(this::onShown);
    }

    protected void onShown() {}

    protected void onHide() {}

    protected String getString(String key) {
        if (!resourceBundle.containsKey(key)) {
            throw new RuntimeException(String.format("No property '%s' defined in bundle '%s' with lang '%s'",
                    key, resourceBundle.getBaseBundleName(), resourceBundle.getLocale().toLanguageTag()));
        }
        return resourceBundle.getString(key);
    }

    private void init() {
        setupDescription();

        try {
            onLoad();
        } catch (Exception ex) {
            logger.error("Error at onLoad occurred", ex);
            throw new RuntimeException(ex);
        }
    }

    protected void onLoad() throws Exception {

    }

    protected void setupDescription() {
        Label label = new Label();
        label.paddingProperty().setValue(new Insets(20));
        label.setText(getString(I18N_DESCRIPTION_KEY));
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.JUSTIFY);
        BorderPane.setAlignment(label, Pos.CENTER);
        this.setTop(label);
    }

    public void setNavigation(NavigationDescriptor[] navs) {
        if (navs == null) {
            return;
        }
        HBox pane = new HBox();
        pane.alignmentProperty().setValue(Pos.BOTTOM_RIGHT);
        for (NavigationDescriptor nav : navs) {
            if (nav.id != null) {
                bindNavigation(nav);
                continue;
            }
            Button btn = new Button(getLocalized(nav.title));
            btn.setUserData(nav);
            btn.setDisable(!nav.isEnabled);
            btn.setOnAction(getAction(nav.method));
            pane.getChildren().add(btn);
        }
        this.setBottom(pane);
    }

    private EventHandler<ActionEvent> getAction(NavigationMethod method) {
        switch (method) {
            case PREV:
                return this::prev;
            case NEXT:
                return this::next;
            case CUSTOM:
            default:
                return this::custom;
        }
    }

    protected String getLocalized(String value) {
        return value.startsWith("%") ? getString(value.substring(1)) : value;
    }

    private void bindNavigation(NavigationDescriptor nav) {
        Node node = this.lookup(nav.id);
        if (node == null) {
            throw new RuntimeException(String.format("Element with id %s not found", nav.id));
        }
        if (!(node instanceof ButtonBase)) {
            throw new RuntimeException(String.format("Id %s must reference to ButtonBase element", nav.id));
        }
        ButtonBase el = (ButtonBase)node;
        String title = getLocalized(nav.title);
        el.setText(title);
        el.setOnAction(getAction(nav.method));
        el.setUserData(nav);
    }
}
