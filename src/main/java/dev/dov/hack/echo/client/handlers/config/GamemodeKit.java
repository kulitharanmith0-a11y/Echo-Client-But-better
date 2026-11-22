package dev.dov.hack.echo.client.handlers.config;

public enum GamemodeKit {
    CRYSTAL("Crystal", "End Crystal + Obsidian / Anchor / Glowstone"),
    CART("Cart", "TNT Minecart + Rail + Bow / Crossbow / Flint"),
    MACE("Mace", "Mace in hotbar"),
    SPEAR("Spear", "Spear in hotbar"),
    NETHPOT("NethPot", "Splash Potions + Netherite weapon or armor"),
    DIAPOT("DiaPot", "Splash Potions + Diamond weapon or armor"),
    UHC("UHC", "Golden Apple + Lava/Water + Bow/Crossbow/Cobweb"),
    AXE("Axe", "Axe + Shield in hotbar"),
    SMP("SMP", "Netherite gear + Totem/XP + Pearl + Golden Apple"),
    DIA_SMP("Dia SMP", "Diamond gear + Totem/XP + Pearl + Golden Apple"),
    SWORD("Sword", "Sword only (no Crystals, Mace, Pots, Shield, Lava)");

    private final String displayName;
    private final String hint;

    GamemodeKit(String displayName, String hint) {
        this.displayName = displayName;
        this.hint = hint;
    }

    public String displayName() {
        return displayName;
    }

    public String hint() {
        return hint;
    }

    /**
     * Returns true when {@code this} kit is merely a lower-specificity leftover
     * subset of {@code locked} (for example running out of crystals or potions mid-fight
     * and only having a sword/totem/pearls left in the hotbar).
     */
    public boolean isDepletionSubsetOf(GamemodeKit locked) {
        if (locked == null || locked == this) {
            return false;
        }
        return switch (this) {
            case SWORD -> locked == CRYSTAL || locked == CART || locked == MACE || locked == SPEAR
                    || locked == NETHPOT || locked == DIAPOT || locked == UHC || locked == AXE
                    || locked == SMP || locked == DIA_SMP;
            case SMP, DIA_SMP -> locked == CRYSTAL || locked == CART || locked == MACE || locked == NETHPOT
                    || locked == DIAPOT || locked == UHC;
            case AXE -> locked == CRYSTAL || locked == UHC || locked == SMP || locked == DIA_SMP || locked == MACE;
            default -> false;
        };
    }
}
