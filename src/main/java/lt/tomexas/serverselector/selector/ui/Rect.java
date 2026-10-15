package lt.tomexas.serverselector.selector.ui;

/** An axis-aligned hit area on the UI plane, given by its centre. */
public record Rect(double centerX, double centerY, double width, double height) {

    public boolean contains(double x, double y) {
        return Math.abs(x - centerX) <= width / 2 && Math.abs(y - centerY) <= height / 2;
    }
}
