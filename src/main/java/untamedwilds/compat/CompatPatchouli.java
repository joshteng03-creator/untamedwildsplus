package untamedwilds.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import untamedwilds.UntamedWilds;
import vazkii.patchouli.api.PatchouliAPI;

/**
 * The only class that touches Patchouli (a compile-only dependency). Callers check
 * {@code ModList.get().isLoaded("patchouli")} first, so this class is never loaded without it.
 */
public final class CompatPatchouli {

    private static final ResourceLocation BOOK = new ResourceLocation(UntamedWilds.MOD_ID, "encyclopedia_of_life");

    private CompatPatchouli() {}

    /** Opens the Encyclopedia of Life on {@code <type>/<species>} (entries are filed as entries/<type>/<species>.json). */
    public static void openEntry(ServerPlayer player, String type, String species) {
        String entry = species.isEmpty() ? type : type + "/" + species;
        PatchouliAPI.get().openBookEntry(player, BOOK, new ResourceLocation(UntamedWilds.MOD_ID, entry), 0);
    }
}
