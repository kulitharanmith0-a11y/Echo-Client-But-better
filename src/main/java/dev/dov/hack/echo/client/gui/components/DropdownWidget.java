package dev.dov.hack.echo.client.gui.components;

import java.util.List;

public final class DropdownWidget {
    public final String label;
    private final List<String> options;
    private int selectedIndex;
    private boolean open;

    public DropdownWidget(String label, List<String> options, int initial) {
        this.label = label;
        this.options = List.copyOf(options);
        this.selectedIndex = Math.max(0, Math.min(initial, this.options.size() - 1));
    }

    public String getSelected() { return options.isEmpty() ? "" : options.get(selectedIndex); }

    public int getSelectedIndex() { return selectedIndex; }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size()) selectedIndex = index;
    }

    public List<String> getOptions() { return options; }

    public boolean isOpen() { return open; }

    public void setOpen(boolean open) { this.open = open; }

    public void toggle() { open = !open; }
}