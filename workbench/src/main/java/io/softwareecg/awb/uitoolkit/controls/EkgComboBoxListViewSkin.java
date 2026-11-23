package io.softwareecg.awb.uitoolkit.controls;

import afester.javafx.svg.SvgLoader;
import io.softwareecg.awb.runtime.NamedEnum;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.CellSkinBase;
import javafx.scene.control.skin.ComboBoxListViewSkin;

import java.io.InputStream;
import java.util.Map;

/**
 * ViewSkin that styles/skins the ComboBox components used in ProjectSelector.
 * The skins will used especially to addAndSum icons the ListCell and ButtonCell sub-elements
 * of the ComboBox.
 *
 * @since 28.06.2018
 */
public class EkgComboBoxListViewSkin<T extends NamedEnum> extends ComboBoxListViewSkin<T> {

    /**
     * a map of icon resources and observables used for the combobox items
     */
    private final Map<T, CellBehaviorResource> valueBehaviourMap;

    /**
     * the class that has instantiated this ViewSkin (used as base for resources loading)
     */
    private Class<?> instantiatingClass;

    /**
     * Construct a new EkgComboBoxListViewSkin instance that will use the
     * given properties for styling the combobox.
     *
     * @param comboBox the combobox to skins
     * @param behaviourMap a map of icon resources and observables used for the combobox items
     * @param defaultValue the default value for new combobox instances
     */
    public EkgComboBoxListViewSkin(ComboBox<T> comboBox, Map<T, CellBehaviorResource> behaviourMap, T defaultValue) {
        super(comboBox);
        this.valueBehaviourMap = behaviourMap;
        comboBox.getStyleClass().addAll("project-selector-combobox");
        comboBox.setCellFactory(value -> new EkgListCell<>(instantiatingClass, valueBehaviourMap, defaultValue));
        instantiatingClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
    }

    /**
     * This method should return a Node that will be positioned within the
     * ComboBox 'button' area.
     * @return the node that will be positioned within the ComboBox 'button' area
     */
    public Node getDisplayNode() {
        Node node = super.getDisplayNode();

        node.getStyleClass().add("comboBoxIconButtonCell");

        // we do nothing if no selection available
        if (getSkinnable().getValue() == null || valueBehaviourMap == null) {
            return node;
        }

        //noinspection unchecked
        return new EkgButtonCell(instantiatingClass, valueBehaviourMap, getSkinnable().getValue());
    }



    private static class EkgButtonCell<T extends NamedEnum> extends Cell<T> {

        private final Class<?> instantiatingClass;

        private final Map<T, CellBehaviorResource> valueBehaviourMap;

        public EkgButtonCell(Class<?> instantiatingClass, Map<T, CellBehaviorResource> valueBehaviourMap, T initialValue) {
            super.getStyleClass().add("ekgComboboxButtonCell");
            this.instantiatingClass = instantiatingClass;
            this.valueBehaviourMap = valueBehaviourMap;
            this.updateItem(initialValue, false);
        }

        /** {@inheritDoc} */
        @Override
        protected void updateItem(T selectedValue, boolean empty) {
            super.updateItem(selectedValue, empty);
            if (selectedValue != null) {
                setGraphic(getSvgIcon(valueBehaviourMap.get(selectedValue).iconPath));
                disableProperty().bind(valueBehaviourMap.get(selectedValue).disableStateObservable);
                setText(" " + selectedValue.getName());
            }
        }

        /** {@inheritDoc} */
        @Override protected Skin<?> createDefaultSkin() {
            return new CellSkinBase<>(this);
        }

        /**
         * Loads an SVG icon using the caller class as resource base
         *
         * @param iconResourceURL an absolute or relative resource URL of the icon image
         * @return the icon as scaled group.
         */
        private Group getSvgIcon(String iconResourceURL) {

            InputStream iconStream =  instantiatingClass.getResourceAsStream(iconResourceURL);

            // to get the right size SVG images with 4mm x 4mm should use for a 34px height combobox
            Group iconImage = new SvgLoader().loadSvg(iconStream);
            iconImage.setScaleY(0.06);
            iconImage.setScaleX(0.06);
            return new Group(iconImage);
        }
    }

    /**
     * A customized list cell type that display the content depending
     * on the properties a map defines for each value. On this way the
     * specific the combobox gets item specific icons, text values and
     * enable/disable states.
     */
    private static class EkgListCell<T extends NamedEnum> extends ListCell<T> {

        /**
         * a map of icon resources and observables used for the combobox items
         */
        private final Map<T, CellBehaviorResource> valueBehaviourMap;

        /**
         * the class that has instantiated this ViewSkin (used as base for resources loading)
         */
        private final Class<?> instantiatingClass;


        /**
         * Constructs a new instance of CustomSkinnableListCell that will use the
         * given properties to control the cell behaviour and style.
         *
         * @param instantiatingClass the caller class that has instantiated the EkgComboBoxListViewSkin
         * @param valueBehaviourMap a map with each value the combobox will store. It provides icon and more for it.
         * @param initialValue the initial value the combobox should show.
         */
        public EkgListCell(Class<?> instantiatingClass, Map<T, CellBehaviorResource> valueBehaviourMap, T initialValue) {

            super.getStyleClass().add("ekgComboboxListCell");

            this.instantiatingClass = instantiatingClass;
            this.valueBehaviourMap = valueBehaviourMap;
            this.updateItem(initialValue, false);

        }

        /** {@inheritDoc} */
        @Override
        protected void updateItem(T selectedValue, boolean empty) {
            super.updateItem(selectedValue, empty);
            if (selectedValue != null) {
                setGraphic(getSvgIcon(valueBehaviourMap.get(selectedValue).iconPath));
                disableProperty().bind(valueBehaviourMap.get(selectedValue).disableStateObservable);
                setText(" " + selectedValue.getName());
            }
        }

        /**
         * Loads an SVG icon using the caller class as resource base
         *
         * @param iconResourceURL an absolute or relative resource URL of the icon image
         * @return the icon as scaled group.
         */
        private Group getSvgIcon(String iconResourceURL) {

            InputStream iconStream =  instantiatingClass.getResourceAsStream(iconResourceURL);

            // to get the right size SVG images with 4mm x 4mm should use for a 34px height combobox
            Group iconImage = new SvgLoader().loadSvg(iconStream);
            iconImage.setScaleX(0.06);
            iconImage.setScaleY(0.06);
            return new Group(iconImage);
        }
    }


    public static class CellBehaviorResource {

        private final String iconPath;

        private final ObservableValue<? extends Boolean> disableStateObservable;

        public CellBehaviorResource(String iconPath, ObservableValue<? extends Boolean> disableStateObservable) {
            this.iconPath = iconPath;
            this.disableStateObservable = disableStateObservable;
        }
    }
}
