//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.collector.common;

import java.util.ArrayList;
import java.util.List;


/**
 * JTable aehnliche Texttabelle fuer Debugzwecke oder Textreports.
 * Die Berechnung der Spaltengroessen und Ausrichtung der Inhalte erfolgt automatisch.
 * Die Darstellung sieht wie folgt aus (Beispiel):
 * <pre>
 *  ________________ ___________________________ _________________________
 * |                |                           |                         |
 * |     Header     |        Header X           |      Description        |
 * |________________|___________________________|_________________________|
 * |                |                           |                         |
 * |  000323233233  |  xxxxxxxxxxxxxxxxxxxxxxx  |  zzzzzzzzzzzzzzzzzzzz   |
 * |  000323233233  |  yyyyyyyyyyyyyyyyyyyyyyy  |  zzzzzzzzzzzzzzzzzzzz   |
 * |________________|___________________________|_________________________|
 * </pre>
 *
 */
public class TextTable {
    /**
     * Text mittig in der Zelle ausrichten.
     */
    public static final int ALIGNMENT_CENTER = 0;
    /**
     * Text links in der Zelle ausrichten.
     */
    public static final int ALIGNMENT_LEFT = 1;
    /**
     * Text rechts in der Zelle ausrichten.
     */
    public static final int ALIGNMENT_RIGHT = 2;


    /**
     * Die Stringdarstellung.
     */
    private String mText;

    /**
     * Horizontaler Rand innerhalb der Zelle.
     */
    private int mHorizontalInset = 1;

    /**
     * Die Liste der Spaltenüberschriften.
     */
    private final List<String> mHeaderLabels;

    /**
     * Liste mit Zeilenbeschreibungen.
     */
    private final List<String> mRowLabels;

    /**
     * Die Daten.
     */
    private final List<List<String>> mRowData;

    /**
     * Fullzeichen.
     */
    private static final char FILL = ' ';
    /**
     * Horizontale Border.
     */
    private static final char HORIZONTAL_BORDER = '_';
    /**
     * Vertikale Border.
     */
    private static final char VERTICAL_BORDER = '|';
    /**
     * Linke, obere Ecke.
     */
    private static final char LEFT_UPPER_CORNER = ' ';
    /**
     * Rechte, obere Ecke.
     */
    private static final char RIGHT_UPPER_CORNER = ' ';
    /**
     * Linke, untere Ecke.
     */
    private static final char LEFT_LOWER_CORNER = '|';

    /**
     * Rechte, untere Ecke.
     */
    private static final char RIGHT_LOWER_CORNER = '|';

    /**
     * Konstruktor mit Übergabe von Zeilenbeschreibungen.
     *
     * @param rowLabels     Beschreibung für die Zeilen der Tabelle
     * @param pHeaderLabels Eine Liste(String) von Headerlabels.
     * @param pData         Die zweidimensionale Datenmatrix (List<List<?>>).
     */
    public TextTable(final List<String> rowLabels, final List<String> pHeaderLabels, final List<List<String>> pData) {
        mRowLabels = rowLabels;
        mHeaderLabels = pHeaderLabels;
        mRowData = pData;
        mText = renderTable();
    }

    /**
     * Konstruktor ohne Zeilenbeschreibungen
     * <p/>
     * Die Spalte mit den Zeilenbeschreibungen wird nicht angezeigt.
     *
     * @param pHeaderLabels Eine Liste(String) von Headerlabels.
     * @param pData         Die zweidimensionale Datenmatrix (List<List<?>>).
     */
    public TextTable(final List<String> pHeaderLabels, final List<List<String>> pData) {
        mRowLabels = null;
        mHeaderLabels = pHeaderLabels;
        mRowData = pData;
        mText = renderTable();
    }

    /**
     * To string.
     *
     * @return Die Tabelle als plain Text.
     */
    public String toString() {
        return mText;
    }

    /**
     * Rendert die Tabelle.
     *
     * @return Die gerenderte Tabelle als String.
     */
    private String renderTable() {
        StringBuilder result = new StringBuilder();
        int[] columnSizes = calculateColumnSizes();

        List<String> header = new ArrayList<>();
        if (mRowLabels != null) {
            header.add("");
        }
        header.addAll(mHeaderLabels);

        // Rendert die Überschriftszeilen (Header)
        textLine(result, new TextLineParameters(columnSizes, null, ALIGNMENT_CENTER, LEFT_UPPER_CORNER, RIGHT_UPPER_CORNER, FILL, HORIZONTAL_BORDER));
        textLine(result, new TextLineParameters(columnSizes, null, ALIGNMENT_CENTER, VERTICAL_BORDER, VERTICAL_BORDER, VERTICAL_BORDER, FILL));
        textLine(result, new TextLineParameters(columnSizes, header, ALIGNMENT_CENTER, VERTICAL_BORDER, VERTICAL_BORDER, VERTICAL_BORDER, FILL));
        textLine(result, new TextLineParameters(columnSizes, null, ALIGNMENT_CENTER, VERTICAL_BORDER, VERTICAL_BORDER, VERTICAL_BORDER, HORIZONTAL_BORDER));
        textLine(result, new TextLineParameters(columnSizes, null, ALIGNMENT_CENTER, VERTICAL_BORDER, VERTICAL_BORDER, VERTICAL_BORDER, FILL));

        // Rendert den Tabelleninhalt
        int i = 0;
        for (List<String> aMRowData : mRowData) {
            List<String> row = aMRowData;
            if (mRowLabels != null) {
                row = new ArrayList<>();
                row.add(mRowLabels.get(i++));
                row.addAll(aMRowData);
            }
            textLine(result, new TextLineParameters(columnSizes, row, ALIGNMENT_LEFT, VERTICAL_BORDER, VERTICAL_BORDER, VERTICAL_BORDER, FILL));
        }

        // Rendert die letzte Zeile
        textLine(result, new TextLineParameters(columnSizes, null, ALIGNMENT_CENTER, LEFT_LOWER_CORNER, RIGHT_LOWER_CORNER, VERTICAL_BORDER, HORIZONTAL_BORDER));

        return result.toString();
    }

    /**
     * Setzt den horizontalen Inset.
     *
     * @param pHorizontalInset eine positive Integerzahl beschreibt den Inset (innerer Rand).
     */
    public void setHorizontalInset(int pHorizontalInset) {
        mHorizontalInset = pHorizontalInset;
        mText = renderTable();
    }

    /**
     * Helper class for text line parameters.
     */
    private static final class TextLineParameters {
        private final int[] pColumnSizes;
        private final List pRow;
        private final int pAlignment;
        private final char pLeftRowBorder;
        private final char pRightRowBorder;
        private final char pBorder;
        private final char pFillChar;

        /**
         * Helper class for a text line.
         *
         * @param pColumnSizes    the sizes of all columns.
         * @param pRow            the elements in a row.
         * @param pAlignment      the alignment.
         * @param pLeftRowBorder  the left border sign.
         * @param pRightRowBorder the right border sign.
         * @param pBorder         the border sign.
         * @param pFillChar       the fill char.
         */
        private TextLineParameters(int[] pColumnSizes, List pRow, int pAlignment, char pLeftRowBorder, char pRightRowBorder, char pBorder, char pFillChar) {
            this.pColumnSizes = pColumnSizes.clone();
            this.pRow = pRow;
            this.pAlignment = pAlignment;
            this.pLeftRowBorder = pLeftRowBorder;
            this.pRightRowBorder = pRightRowBorder;
            this.pBorder = pBorder;
            this.pFillChar = pFillChar;
        }
    }

    /**
     * Rendert eine Zeile in der Tabelle.
     *
     * @param pResult    der Stringbuffer in den die Zeile gespeichert wird.
     * @param parameters die Parameter.
     */
    private void textLine(final StringBuilder pResult, TextLineParameters parameters) {
        pResult.append(parameters.pLeftRowBorder);
        for (int i = 0; i < parameters.pColumnSizes.length; i++) {
            // center
            String content = parameters.pRow != null ? parameters.pRow.get(i) != null ? parameters.pRow.get(i).toString() : "" : "";
            int startPos = 0;
            if (parameters.pAlignment == ALIGNMENT_CENTER) {
                startPos = (parameters.pColumnSizes[i] - content.length()) / 2;
            } else if (parameters.pAlignment == ALIGNMENT_RIGHT) {
                startPos = parameters.pColumnSizes[i] - content.length() - mHorizontalInset;
            } else if (parameters.pAlignment == ALIGNMENT_LEFT) {
                startPos = mHorizontalInset;
            }
            for (int j = 0; j < startPos; j++) {
                pResult.append(parameters.pFillChar);
            }
            pResult.append(content);
            for (int j = content.length() + startPos; j < parameters.pColumnSizes[i]; j++) {
                pResult.append(parameters.pFillChar);
            }

            if (i < parameters.pColumnSizes.length - 1) {
                pResult.append(parameters.pBorder);
            }
        }
        pResult.append(parameters.pRightRowBorder);
        pResult.append('\n');
    }

    /**
     * Berechnet die Grösse der einzelnen Spalten. Die Grösse ist das Maximum aller
     * Inhalte der jeweiligen Spalte plus mHorizontalInset links und rechts des Inhalts.
     *
     * @return eine Array der Spaltengrössen.
     */
    private int[] calculateColumnSizes() {
        int colCount = mHeaderLabels.size() + (mRowLabels != null ? 1 : 0);
        int[] columnSizes = new int[colCount];

        int i = 0;
        if (mRowLabels != null) {
            for (Object rowLabel : mRowLabels) {
                columnSizes[0] = Math.max(rowLabel.toString().length() + mHorizontalInset * 2, columnSizes[0]);
            }
            i++;
        }

        for (Object mHeaderLabel : mHeaderLabels) {
            // vorbesetzen mit den Längen der Header Labels
            String headerLabel = mHeaderLabel.toString();
            columnSizes[i++] = headerLabel.length() + mHorizontalInset * 2;
        }

        // Berechne die Maximallänge pro Spalte
        for (Object aMRowData : mRowData) {
            List row = (List) aMRowData;
            i = (mRowLabels != null ? 1 : 0);
            for (Object aRow : row) {
                String rowData = aRow.toString();
                columnSizes[i] = Math.max(rowData.length() + mHorizontalInset * 2, columnSizes[i]);
                i++;
            }
        }
        return columnSizes;
    }
}


