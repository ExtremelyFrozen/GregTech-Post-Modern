package com.gregtechceu.gtceu.api.placeholder.exceptions;

import net.minecraft.network.chat.Component;

public class PlaceholderException extends Exception {

    private final Component message;
    private int line = 0, symbol = 0;

    public PlaceholderException(String message) {
        this(Component.literal(String.valueOf(message)));
    }

    public PlaceholderException(Component message) {
        this.message = message;
    }

    public void setLineInfo(int line, int symbol) {
        this.line = line;
        this.symbol = symbol;
    }

    public Component getMessageComponent() {
        return Component.literal("%d:%d: ".formatted(line, symbol)).append(message);
    }

    @Override
    public String getMessage() {
        return getMessageComponent().getString();
    }
}
