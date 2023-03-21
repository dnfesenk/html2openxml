package com.denisfesenko.util;

public class Formatting {

    private boolean bold;
    private boolean italic;

    public Formatting() {
        this.bold = false;
        this.italic = false;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }
}
