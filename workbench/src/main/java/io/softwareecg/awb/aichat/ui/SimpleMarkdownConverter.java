package io.softwareecg.awb.aichat.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal Markdown → JavaFX Node converter.
 * Returns a VBox with TextFlow-based content. No external dependencies.
 */
final class SimpleMarkdownConverter {

    private static final double BASE_SIZE = 13;
    private static final String MONO_FAMILY = "Monospace";

    // Soft color palette
    private static final Color TEXT_COLOR = Color.web("#2d3748");       // dark blue-gray
    private static final Color HEADING_COLOR = Color.web("#1a365d");    // deep navy
    private static final Color BOLD_COLOR = Color.web("#2d3748");       // same as text, weight does the work
    private static final Color ITALIC_COLOR = Color.web("#4a5568");     // medium gray
    private static final Color CODE_COLOR = Color.web("#6b46c1");       // muted purple for code
    private static final Color BULLET_COLOR = Color.web("#718096");     // light gray for bullets
    private static final Pattern INLINE = Pattern.compile(
            "\\*\\*(.+?)\\*\\*"           // group 1: bold
            + "|\\*(.+?)\\*"              // group 2: italic
            + "|`(.+?)`"                  // group 3: inline code
    );

    private SimpleMarkdownConverter() {}

    /** Convert markdown text to a VBox of styled JavaFX nodes. */
    static VBox toNodes(String markdown) {
        VBox box = new VBox(4);
        if (markdown == null || markdown.isEmpty()) return box;

        String[] lines = markdown.split("\n", -1);
        boolean inCodeBlock = false;
        StringBuilder codeBuffer = new StringBuilder();
        String listTag = null; // "ul" or "ol"
        List<Node> listItems = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();

        for (String line : lines) {
            // --- fenced code block ---
            if (line.startsWith("```")) {
                if (inCodeBlock) {
                    box.getChildren().add(createCodeBlock(codeBuffer.toString()));
                    codeBuffer.setLength(0);
                    inCodeBlock = false;
                } else {
                    flushParagraph(box, paragraph);
                    flushList(box, listItems, listTag); listTag = null;
                    inCodeBlock = true;
                }
                continue;
            }
            if (inCodeBlock) {
                if (codeBuffer.length() > 0) codeBuffer.append("\n");
                codeBuffer.append(line);
                continue;
            }

            // --- headings ---
            if (line.startsWith("#")) {
                int level = 0;
                while (level < line.length() && level < 6 && line.charAt(level) == '#') level++;
                if (level > 0 && level < line.length() && line.charAt(level) == ' ') {
                    flushParagraph(box, paragraph);
                    flushList(box, listItems, listTag); listTag = null;
                    box.getChildren().add(createHeading(line.substring(level + 1), level));
                    continue;
                }
            }

            // --- unordered list ---
            if (line.matches("^\\s*[-*+]\\s+.*")) {
                flushParagraph(box, paragraph);
                if (!"ul".equals(listTag)) { flushList(box, listItems, listTag); listTag = "ul"; }
                String content = line.replaceFirst("^\\s*[-*+]\\s+", "");
                listItems.add(createListItem("\u2022  ", content));
                continue;
            }

            // --- ordered list ---
            if (line.matches("^\\s*\\d+\\.\\s+.*")) {
                flushParagraph(box, paragraph);
                if (!"ol".equals(listTag)) { flushList(box, listItems, listTag); listTag = "ol"; }
                String num = line.replaceFirst("^\\s*(\\d+)\\.\\s+.*", "$1");
                String content = line.replaceFirst("^\\s*\\d+\\.\\s+", "");
                listItems.add(createListItem(num + ".  ", content));
                continue;
            }

            // --- blank line ---
            if (line.trim().isEmpty()) {
                flushParagraph(box, paragraph);
                flushList(box, listItems, listTag); listTag = null;
                continue;
            }

            // --- regular text ---
            flushList(box, listItems, listTag); listTag = null;
            if (paragraph.length() > 0) paragraph.append(" ");
            paragraph.append(line);
        }

        if (inCodeBlock) box.getChildren().add(createCodeBlock(codeBuffer.toString()));
        flushList(box, listItems, listTag);
        flushParagraph(box, paragraph);
        return box;
    }

    // ---- Node factories ----

    private static TextFlow createHeading(String text, int level) {
        double size = switch (level) {
            case 1 -> BASE_SIZE + 3;
            case 2 -> BASE_SIZE + 2;
            default -> BASE_SIZE + 1;
        };
        TextFlow flow = new TextFlow();
        for (Node n : parseInline(text)) {
            if (n instanceof Text t) {
                t.setFont(Font.font(t.getFont().getFamily(), FontWeight.BOLD, size));
                t.setFill(HEADING_COLOR);
            }
            flow.getChildren().add(n);
        }
        flow.setPadding(new Insets(4, 0, 2, 0));
        return flow;
    }

    private static Node createCodeBlock(String code) {
        Text text = new Text(code);
        text.setFont(Font.font(MONO_FAMILY, BASE_SIZE - 1));
        text.setFill(CODE_COLOR);
        TextFlow flow = new TextFlow(text);
        flow.setStyle("-fx-background-color: #edf2f7; -fx-padding: 6; -fx-background-radius: 4;");
        return flow;
    }

    private static Node createListItem(String bullet, String content) {
        Text bulletText = new Text(bullet);
        bulletText.setFont(Font.font(BASE_SIZE));
        bulletText.setFill(BULLET_COLOR);
        TextFlow contentFlow = new TextFlow();
        contentFlow.getChildren().addAll(parseInline(content));
        HBox item = new HBox(bulletText, contentFlow);
        item.setPadding(new Insets(0, 0, 0, 8));
        return item;
    }

    private static void flushParagraph(VBox box, StringBuilder para) {
        if (para.length() > 0) {
            TextFlow flow = new TextFlow();
            flow.getChildren().addAll(parseInline(para.toString()));
            box.getChildren().add(flow);
            para.setLength(0);
        }
    }

    private static void flushList(VBox box, List<Node> items, String tag) {
        if (!items.isEmpty()) {
            VBox listBox = new VBox(2);
            listBox.setPadding(new Insets(0, 0, 0, 4));
            listBox.getChildren().addAll(items);
            box.getChildren().add(listBox);
            items.clear();
        }
    }

    // ---- Inline parsing ----

    /** Parse inline markdown (bold, italic, code) into styled Text nodes. */
    private static List<Node> parseInline(String text) {
        List<Node> nodes = new ArrayList<>();
        Matcher m = INLINE.matcher(text);
        int last = 0;

        while (m.find()) {
            if (m.start() > last) {
                nodes.add(plainText(text.substring(last, m.start())));
            }
            if (m.group(1) != null) {
                // bold
                Text t = new Text(m.group(1));
                t.setFont(Font.font(null, FontWeight.BOLD, BASE_SIZE));
                t.setFill(BOLD_COLOR);
                nodes.add(t);
            } else if (m.group(2) != null) {
                // italic
                Text t = new Text(m.group(2));
                t.setFont(Font.font(null, FontPosture.ITALIC, BASE_SIZE));
                t.setFill(ITALIC_COLOR);
                nodes.add(t);
            } else if (m.group(3) != null) {
                // inline code
                Text t = new Text(m.group(3));
                t.setFont(Font.font(MONO_FAMILY, BASE_SIZE - 1));
                t.setFill(CODE_COLOR);
                t.setStyle("-fx-background-color: #edf2f7;");
                nodes.add(t);
            }
            last = m.end();
        }

        if (last < text.length()) {
            nodes.add(plainText(text.substring(last)));
        }
        if (nodes.isEmpty()) {
            nodes.add(plainText(text));
        }
        return nodes;
    }

    private static Text plainText(String s) {
        Text t = new Text(s);
        t.setFont(Font.font(BASE_SIZE));
        t.setFill(TEXT_COLOR);
        return t;
    }
}
