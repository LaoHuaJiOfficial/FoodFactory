package contents.blocks;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.defense.RegenProjector;
import mindustry.world.blocks.production.Pump;
import mindustry.world.draw.*;
import prototypes.block.effect.whitelistOverdriveProjector;

import static mindustry.type.ItemStack.with;

public class SpecialBlock {
    public static Block
        MendDome, MendTower,
        overdrive,
        SmallForceShieldGenerator;

    public static void load(){
        MendDome = new RegenProjector("mend-dome"){{
            requirements(Category.effect, with(Items.silicon, 80));
            size = 3;
            range = 29;
            baseColor = Pal.heal;

            consumePower(2f);

            healPercent = 4f / 60f;

            drawer = new DrawMulti(
                new DrawDefault(),
                new DrawGlowRegion(){{
                    color = Pal.heal;
                }},
                new DrawPulseShape(true){{
                    color = Pal.heal;
                    layer = Layer.effect;
                }}
            );
        }};

        overdrive = new whitelistOverdriveProjector("overdrive"){{
            whiteListBlock.add(Blocks.eruptionDrill, Blocks.impactDrill);
            requirements(Category.effect, with(Items.lead, 100, Items.titanium, 75, Items.silicon, 75, Items.plastanium, 30));
            consumePower(3.50f);
            size = 2;
            consumeItem(Items.phaseFabric).boost();
        }};

        SmallForceShieldGenerator = new ForceProjector("small-force-shield-generator"){{
            requirements(Category.effect, with(Items.lead, 100, Items.titanium, 75, Items.silicon, 125));
            size = 2;
            radius = 80f;
            shieldHealth = 3000f;
            cooldownNormal = 6f;
            cooldownBrokenBase = 2f;

            consumeLiquid(Liquids.water, 12f / 60f);
            consumePower(3f);
        }};
    }
}
