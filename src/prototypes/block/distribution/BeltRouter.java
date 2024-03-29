package prototypes.block.distribution;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import mindustry.entities.TargetPriority;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.world.Edges;
import mindustry.world.Tile;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.meta.BlockGroup;
import prototypes.block.HeatBox.BlockF;
import utilities.game.ItemQueueBuffer;

import static mindustry.Vars.*;

public class BeltRouter extends BlockF {
    public TextureRegion sorterRegion, overflowRegion, topRegion, invertRegion, selectRegion;
    public TextureRegion conveyorPartAtlas;
    public TextureRegion[][] conveyorParts;

    public float itemPerSecond;


    public BeltRouter(String name) {
        super(name);

        group = BlockGroup.transportation;
        update = true;
        solid = false;
        hasItems = true;
        unloadable = false;
        itemCapacity = 1;
        noUpdateDisabled = true;
        configurable = true;
        saveConfig = true;
        rotate = true;
        clearOnDoubleTap = true;
        underBullets = true;
        priority = TargetPriority.transport;

        config(Item.class, (BeltRouterBuild tile, Item item) -> tile.sortItem = item);
        configClear((BeltRouterBuild tile) -> tile.sortItem = null);
    }

    @Override
    public void load() {
        super.load();

        sorterRegion = Core.atlas.find(name + "-sorter");
        overflowRegion = Core.atlas.find(name + "-overflow");
        topRegion = Core.atlas.find(name + "-top");
        invertRegion = Core.atlas.find(name + "-invert");
        selectRegion = Core.atlas.find(name + "-select");

        conveyorPartAtlas = Core.atlas.find(name + "-base");
        conveyorParts = conveyorPartAtlas.split(32, 32);
    }

    @Override
    public void setBars() {
        super.setBars();
        removeBar("items");
    }

    @Override
    public boolean outputsItems() {
        return true;
    }

    @Override
    public int minimapColor(Tile tile) {
        var build = (BeltRouterBuild) tile.build;
        return build == null || build.sortItem == null ? 0 : build.sortItem.color.rgba();
    }

    @Override
    protected TextureRegion[] icons() {
        return new TextureRegion[]{sorterRegion};
    }

    public class BeltRouterBuild extends BuildF {
        public @Nullable Item sortItem;
        //public Queue<Item> ItemQueueBuffer = new Queue<>(10);
        ItemQueueBuffer ItemQueueBuffer = new ItemQueueBuffer(10);

        public float speed = Time.toSeconds / itemPerSecond;
        public float timeInterval = speed;
        public float progress;
        public boolean isSorter = true;
        public boolean invert;

        @Override
        public void created() {
            super.created();
        }


        @Override
        public void configured(Unit player, Object value) {
            super.configured(player, value);

            if (!headless) {
                renderer.minimap.update(tile);
            }
        }

        @Override
        public void drawSelect() {
            Draw.rect(selectRegion, x, y, rotation * 90);

            String modeText = isSorter?"[accent]Sort Mode[]/Overflow Mode":"Sort Mode/[accent]Overflow Mode[]";
            String invertText = !invert?"[accent]Normal[]/Invert":"Normal/[accent]Invert[]";

            Font font = Fonts.outline;
            GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.setUseIntegerPositions(false);
            font.getData().setScale(1f / 6f / Scl.scl(1f));

            layout.setText(font, modeText);
            font.draw(modeText, x, y + 10f, Align.center);

            layout.setText(font, invertText);
            font.draw(invertText, x, y + 14f, Align.center);

            font.setUseIntegerPositions(ints);
            font.setColor(Color.white);
            font.getData().setScale(1f);
            Draw.reset();
            Pools.free(layout);
        }

        @Override
        public void draw() {
            if (isSorter) {
                Draw.rect(sorterRegion, x, y);
                if (sortItem != null) {
                    Draw.color(sortItem.color);
                    Draw.rect(topRegion, x, y);
                    Draw.reset();
                }
            } else {
                Draw.rect(overflowRegion, x, y);
            }

            if (invert) {
                Draw.rect(invertRegion, x, y);
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            return
                ItemQueueBuffer.accepts() &&
                    (Edges.getFacingEdge(source.tile(), tile).relativeTo(tile) == rotation);
        }

        @Override
        public void handleItem(Building source, Item item) {
            ItemQueueBuffer.accept(item);
        }

        @Override
        public void updateTile() {
            progress += edelta();

            if (isSorter) {
                if(ItemQueueBuffer.lastItem() != null){
                    if (progress >= timeInterval) {
                        var target = routerTarget();
                        if (target != null) {
                            target.handleItem(this, ItemQueueBuffer.lastItem());
                            ItemQueueBuffer.remove();
                            progress = 0;
                        }
                    }
                } else {
                    progress = -80 / itemPerSecond;
                }
            } else {
                if(ItemQueueBuffer.lastItem() != null){
                    if (progress >= timeInterval) {
                        var target = overflowTarget();
                        if (target != null) {
                            target.handleItem(this, ItemQueueBuffer.lastItem());
                            cdump = (byte) (cdump == 0 ? 2 : 0);
                            ItemQueueBuffer.remove();
                            progress = 0;
                        }
                    }
                } else {
                    progress = -80 / itemPerSecond;
                }
            }

        }

        @Nullable
        public Building routerTarget() {
            if (ItemQueueBuffer.lastItem() == null) return null;

            int dump = cdump;

            for (int i = 0; i < proximity.size; i++) {
                Building other = proximity.get((i + dump) % proximity.size);
                int rel = relativeTo(other);

                if (!invert) {
                    if (!(sortItem != null && (ItemQueueBuffer.lastItem() == sortItem) != (rel == rotation))
                        && !(rel == (rotation + 2) % 4) && other.team == team
                        && other.acceptItem(this, ItemQueueBuffer.lastItem())) {


                        incrementDump(proximity.size);
                        return other;
                    }
                } else {
                    if (!(sortItem != null && (ItemQueueBuffer.lastItem() == sortItem) == (rel == rotation))
                        && !(rel == (rotation + 2) % 4) && other.team == team
                        && other.acceptItem(this, ItemQueueBuffer.lastItem())) {


                        incrementDump(proximity.size);
                        return other;
                    }
                }


                incrementDump(proximity.size);
            }

            return null;
        }

        @Nullable
        public Building overflowTarget() {
            if (ItemQueueBuffer.lastItem() == null) return null;

            if (invert) {
                Building l = left(), r = right();
                boolean lc = l != null && l.team == team && l.acceptItem(this, ItemQueueBuffer.lastItem()),
                    rc = r != null && r.team == team && r.acceptItem(this, ItemQueueBuffer.lastItem());

                if (lc && !rc) {
                    return l;
                } else if (rc && !lc) {
                    return r;
                } else if (lc) {
                    return cdump == 0 ? l : r;
                }
            }

            Building front = front();
            if (front != null && front.team == team && front.acceptItem(this, ItemQueueBuffer.lastItem())) {
                return front;
            }

            if (invert) return null;

            for (int i = -1; i <= 1; i++) {
                int dir = Mathf.mod(rotation + (((i + cdump + 1) % 3) - 1), 4);
                if (dir == rotation) continue;
                Building other = nearby(dir);
                if (other != null && other.team == team && other.acceptItem(this, ItemQueueBuffer.lastItem())) {
                    return other;
                }
            }

            return null;
        }

        @Override
        public void drawConfigure(){
            Draw.rect(selectRegion, x, y, rotation * 90);

            String modeText = isSorter?"[accent]Sort Mode[]/Overflow Mode":"Sort Mode/[accent]Overflow Mode[]";
            String invertText = !invert?"[accent]Normal[]/Invert":"Normal/[accent]Invert[]";

            Font font = Fonts.outline;
            GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.setUseIntegerPositions(false);
            font.getData().setScale(1f / 6f / Scl.scl(1f));

            layout.setText(font, modeText);
            font.draw(modeText, x, y + 10f, Align.center);

            layout.setText(font, invertText);
            font.draw(invertText, x, y + 14f, Align.center);

            font.setUseIntegerPositions(ints);
            font.setColor(Color.white);
            font.getData().setScale(1f);
            Draw.reset();
            Pools.free(layout);
        }

        @Override
        public void buildConfiguration(Table table) {

            Table select = new Table();

            select.button(Icon.diagonal, Styles.squarei, () -> isSorter = !isSorter).size(86f, 50).tooltip("Sort Mode/Overflow Mode").left();
            select.button(Icon.refresh, Styles.squarei, () -> invert = !invert).size(86f, 50).tooltip("Normal Output/Invert Output").right();
            table.add(select);
            table.row();
            ItemSelection.buildTable(BeltRouter.this, table, content.items(), () -> sortItem, this::configure, selectionRows, selectionColumns);
        }

        @Override
        public Item config() {
            return sortItem;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(sortItem == null ? -1 : sortItem.id);
            write.f(progress);
            write.bool(isSorter);
            write.bool(invert);

            ItemQueueBuffer.write(write);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            sortItem = content.item(read.s());
            isSorter = read.bool();
            invert = read.bool();
            progress = read.f();

            ItemQueueBuffer.read(read);
        }
    }
}
