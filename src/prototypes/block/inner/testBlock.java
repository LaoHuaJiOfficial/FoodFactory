package prototypes.block.inner;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Log;
import contents.FFBlock;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.input.Placement;
import mindustry.world.Tile;
import prototypes.block.production.AssemblerBlock;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.stroke;
import static mindustry.Vars.*;
import static utilities.FFGlobalVars.iconPrefix;
import static utilities.FFGlobalVars.rotPrefix;

public class testBlock extends AssemblerBlock {
    public Seq<Point2> linkPoints = new Seq<>();
    public TextureRegion PreviewIcon;
    public TextureRegion[] BaseRegion = new TextureRegion[4];
    public testBlock(String name) {
        super(name);
        rotate = true;
        quickRotate = false;

        /*
        placeEffect = new Effect(16, e -> {
            color(Pal.accent);
            stroke(3f - e.fin() * 2f);
            Lines.square(e.x, e.y, getWidth((int) (e.rotation/90)) / 2f * e.rotation + e.fin() * 3f);
        });

         */

        linkPoints.add(new Point2[]{
            new Point2(2,0),
            new Point2(2,1),
            new Point2(-1,0),
            new Point2(-1,1),
        });

        inputItemDir.add(new Point2[]{
            new Point2(-2, 0),
            new Point2(-2, 1),
        });

        outputItemDir.add(new Point2[]{
            new Point2(3, 0),
            new Point2(3, 1),
        });
    }

    @Override
    public void load() {
        super.load();
        PreviewIcon = Core.atlas.find(name + iconPrefix);
        for (int i= 0; i < 4; i++){
            BaseRegion[i] = Core.atlas.find(name + rotPrefix[i]);
        }
    }

    @Override
    public void drawDefaultPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(BaseRegion[plan.rotation], plan.drawx(), plan.drawy());

        drawPlanConfig(plan, list);
    }

    @Override
    public TextureRegion[] icons() {
        return new TextureRegion[]{PreviewIcon};
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation) {
        boolean linkOccupy = false;
        for (Point2 linkPoint2: getRotatedTiles(rotation)){
            Tile linkTile = world.tile(tile.x + linkPoint2.x, tile.y + linkPoint2.y);
            if (linkTile.build != null) {
                linkOccupy = true;
                break;
            }
        }
        return !linkOccupy && super.canPlaceOn(tile, team, rotation);
    }

    //todo place line
    /*
    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation){
        Placement.calculateNodes(points, this, rotation,
            (point, other) -> rotation%2==0?
                Math.max(Math.abs(point.x - other.x), Math.abs(point.y - other.y))==getWidth(rotation):
                Math.max(Math.abs(point.x - other.x), Math.abs(point.y - other.y))==getHeight(rotation)
            );
    }

    @Override
    public void handlePlacementLine(Seq<BuildPlan> plans) {
        super.handlePlacementLine(plans);
    }

     */

    /** return the linked tiles when rotated.*/
    public Seq<Point2> getRotatedTiles(int rot){
        Seq<Point2> tmp = new Seq<>();
        tmp.clear();
        for (Point2 point2 : linkPoints) {
            //avoid annoying x shouldn't be y
            int px = point2.x, py = point2.y;
            switch (rot) {
                case 0 -> tmp.add(point2);
                case 1 -> tmp.add(new Point2(1 - py, px));
                case 2 -> tmp.add(new Point2(1 - px, 1 - py));
                case 3 -> tmp.add(new Point2(py, 1 - px));
            }
        }
        return tmp;
    }

    public Seq<Point2> getRotatedTiles(int rot, Seq<Point2> seq){
        Seq<Point2> tmp = new Seq<>();
        tmp.clear();
        for (Point2 point2 : seq) {
            //avoid annoying x shouldn't be y
            int px = point2.x, py = point2.y;
            switch (rot) {
                case 0 -> tmp.add(point2);
                case 1 -> tmp.add(new Point2(1 - py, px));
                case 2 -> tmp.add(new Point2(1 - px, 1 - py));
                case 3 -> tmp.add(new Point2(py, 1 - px));
            }
        }
        return tmp;
    }

    public int getWidth(int rot){
        int w1 = size/2, w2 = -(size%2==0?size/2-1:size/2);
        for (Point2 point2: getRotatedTiles(rot)){
            if (point2.x > w1) w1 = point2.x;
            if (point2.x < w2) w2 = point2.x;
        }
        return w1-w2;
    }

    public int getHeight(int rot){
        int h1 = size/2, h2 = -(size%2==0?size/2-1:size/2);
        for (Point2 point2: getRotatedTiles(rot)){
            if (point2.y > h1) h1 = point2.y;
            if (point2.y < h2) h2 = point2.y;
        }
        return h1-h2;
    }

    public Point2 getBottomRightPoint(int rot){
        int w = size/2, h = -(size%2==0?size/2-1:size/2);
        Point2 p = new Point2();
        for (Point2 point2: getRotatedTiles(rot)){
            if (point2.x > w) p.x = point2.x - w;
            if (point2.y < h) p.y = point2.y - h;
        }
        //Log.info("w:"+ w + " h:" + h + " x:" + p.x + " y:" + p.y);
        return p;
    }

    public class testBuild extends AssemblerBlockBuild{
        Seq<LinkBlock.LinkBuild> linkBuilds = new Seq<>();
        //Seq<Tile> proximityTiles = new Seq<>();
        @Override
        public void created() {
            super.created();

            for (Point2 point2: getRotatedTiles(rotation)){
                Tile linkTile = Vars.world.tile(tile.x + point2.x, tile.y + point2.y);
                linkTile.setBlock(FFBlock.linkBuild, this.team);
                LinkBlock.LinkBuild build = (LinkBlock.LinkBuild) linkTile.build;
                build.updateLink(this);
                linkBuilds.add(build);
            }
        }
        @Override
        public void remove() {
            for (Point2 point2: getRotatedTiles(rotation)){
                Tile linkTile = Vars.world.tile(tile.x + point2.x, tile.y + point2.y);
                linkTile.remove();
            }
            super.remove();
        }

        @Override
        public void draw() {
            Draw.rect(BaseRegion[rotation], x, y);
        }

        @Override
        public void drawStatus() {
            if (this.block.enableDrawStatus && this.block.consumers.length > 0) {
                float multiplier = this.block.size > 1 ? 1.0F : 0.64F;
                float brcx = (this.x + getBottomRightPoint(rotation).x * tilesize) + (float)(this.block.size * 8) / 2.0F - 8.0F * multiplier / 2.0F;
                float brcy = (this.y + getBottomRightPoint(rotation).y * tilesize) - (float)(this.block.size * 8) / 2.0F + 8.0F * multiplier / 2.0F;
                Draw.z(71.0F);
                Draw.color(Pal.gray);
                Fill.square(brcx, brcy, 2.5F * multiplier, 45.0F);
                Draw.color(this.status().color);
                Fill.square(brcx, brcy, 1.5F * multiplier, 45.0F);
                Draw.color();
            }

        }

        public void updateOutputItemBuild(){
            if (!outputItemDir.any())return;
            outputItemDirBuild.clear();
            for (Point2 point2: getRotatedTiles(rotation, outputItemDir)){
                outputItemDirBuild.add(world.build(tile.x + point2.x, tile.y + point2.y));
            }
        }
        public void updateInputItemBuild(){
            if (!inputItemDir.any())return;
            inputItemDirBuild.clear();
            for (Point2 point2: getRotatedTiles(rotation, inputItemDir)){
                inputItemDirBuild.add(world.build(tile.x + point2.x, tile.y + point2.y));
            }
        }

        @Override
        public void onProximityUpdate() {
            super.onProximityUpdate();
            updateLinkProxy();
            //todo this is wired
        }

        public void updateLinkProxy(){
            proximity.clear();
            updateInputItemBuild();
            updateOutputItemBuild();
            for (var build: inputItemDirBuild){
                if (build != null){
                    proximity.add(build);
                }
            }
            for (var build: outputItemDirBuild){
                if (build != null){
                    proximity.add(build);
                }
            }
        }
    }
}
