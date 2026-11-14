package com.gregtechceu.gtceu.integration.xei;

import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;

/**
 * Converts recipe ingredient roles to and from the UI ingredient contract.
 */
public final class GTXEIIngredientRoleAdapter {

    private GTXEIIngredientRoleAdapter() {}

    public static IngredientIO toIngredientIO(GTXEIIngredientRole role) {
        return switch (role) {
            case INPUT -> IngredientIO.INPUT;
            case OUTPUT -> IngredientIO.OUTPUT;
            case CATALYST -> IngredientIO.CATALYST;
            case NONE -> IngredientIO.NONE;
        };
    }

    public static GTXEIIngredientRole fromIngredientIO(IngredientIO ingredientIO) {
        return switch (ingredientIO) {
            case INPUT -> GTXEIIngredientRole.INPUT;
            case OUTPUT -> GTXEIIngredientRole.OUTPUT;
            case CATALYST -> GTXEIIngredientRole.CATALYST;
            case NONE -> GTXEIIngredientRole.NONE;
        };
    }
}
