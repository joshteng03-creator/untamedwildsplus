package untamedwilds.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import untamedwilds.entity.ProjectileSerratedArrow;

/** The item form of the serrated arrow; bows and crossbows fire it through the {@code minecraft:arrows} tag. */
public class SerratedArrowItem extends ArrowItem {

    public SerratedArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        return new ProjectileSerratedArrow(level, shooter);
    }
}
