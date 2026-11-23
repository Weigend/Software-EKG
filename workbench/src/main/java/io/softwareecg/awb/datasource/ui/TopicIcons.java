package io.softwareecg.awb.datasource.ui;

import io.softwareecg.awb.datasource.spi.AnalysisTopic;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/**
 * The pictures on the tiles of the topics: plain line drawings on a 48 x 48 grid, coloured by
 * the stylesheet (style class {@code topic-icon}) so that they follow the state of their tile.
 * They are generic signs - a terminal, a cup, a cylinder - not the logos of the products.
 */
final class TopicIcons {

    private static final double SIZE = 48;

    private TopicIcons() {
    }

    static Node of(AnalysisTopic topic) {
        return icon(switch (topic) {
            // a terminal with a prompt
            case LINUX -> "M8 9 H40 A3 3 0 0 1 43 12 V36 A3 3 0 0 1 40 39 H8 A3 3 0 0 1 5 36 V12 A3 3 0 0 1 8 9 Z"
                    + " M13 18 L20 24 L13 30 M24 31 H34";
            // a window with four panes
            case WINDOWS -> "M8 8 H40 V40 H8 Z M24 8 V40 M8 24 H40";
            // a container: a cube seen from its corner
            case KUBERNETES -> "M24 4 L41 14 V34 L24 44 L7 34 V14 Z M24 24 V44 M24 24 L7 14 M24 24 L41 14";
            // a steaming cup
            case JVM -> "M10 19 H34 V30 A8 8 0 0 1 26 38 H18 A8 8 0 0 1 10 30 Z"
                    + " M34 22 H37 A4.5 4.5 0 0 1 37 31 H34"
                    + " M17 6 Q20 9 17 12 M24 4 Q27 8 24 12 M31 6 Q34 9 31 12"
                    + " M8 43 H36";
            // a cylinder with three layers
            case DATABASES -> "M8 12 A16 6 0 1 0 40 12 A16 6 0 1 0 8 12"
                    + " M8 12 V36 A16 6 0 0 0 40 36 V12"
                    + " M8 24 A16 6 0 0 0 40 24";
            // one service calling two others
            case SERVICES -> "M5 24 A5 5 0 1 0 15 24 A5 5 0 1 0 5 24"
                    + " M33 11 A5 5 0 1 0 43 11 A5 5 0 1 0 33 11"
                    + " M33 37 A5 5 0 1 0 43 37 A5 5 0 1 0 33 37"
                    + " M14.5 21.5 L33.5 13 M14.5 26.5 L33.5 35";
        });
    }

    /** A magnifying glass, for "show me what is there". */
    static Node explore() {
        return icon("M7 20 A13 13 0 1 0 33 20 A13 13 0 1 0 7 20 M29.5 29.5 L42 42");
    }

    private static Node icon(String path) {
        SVGPath shape = new SVGPath();
        shape.setContent(path);
        shape.getStyleClass().add("topic-icon");
        shape.setMouseTransparent(true);

        // a fixed box, so that every picture takes the same room whatever its outline is
        StackPane box = new StackPane(shape);
        box.setMinSize(SIZE, SIZE);
        box.setPrefSize(SIZE, SIZE);
        box.setMaxSize(SIZE, SIZE);
        box.setMouseTransparent(true);
        return box;
    }
}
