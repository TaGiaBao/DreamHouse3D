package vn.dreamhouse.glade;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import java.util.*;
class ResourceCacheCheck {
    public static void main(String[]args)throws Exception{
        var art=new GladeArt(new DesktopAssetManager(true));Material retained=art.material("#deaa88");var before=((ColorRGBA)retained.getParam("Diffuse").getValue()).clone();
        for(int i=0;i<3000;i++)art.material(String.format(Locale.ROOT,"#%06x",i));
        var field=GladeArt.class.getDeclaredField("materials");field.setAccessible(true);var cache=(Map<?,?>)field.get(art);
        if(cache.size()>512 || !retained.getParam("Diffuse").getValue().equals(before))throw new AssertionError("material cache grows or mutates a retained scene material");
        var restored=art.material("#deaa88");if(!restored.getParam("Diffuse").getValue().equals(before))throw new AssertionError("evicted material cannot be recreated correctly");
        var river = new GladeDesign.Item(0, GladeDesign.Kind.WATER, new GladeDesign.Point(0, 0),
                4, 2, .7f, false, List.of(new GladeDesign.Point(-2, 0), new GladeDesign.Point(0, 0), new GladeDesign.Point(2, 0)));
        var roadStart = new GladeDesign.Point(0, .83f);
        var road = new GladeDesign.Item(1, GladeDesign.Kind.PATH, roadStart,
                1, 1, 1, false, List.of(roadStart, new GladeDesign.Point(2, .83f)));
        var scene = new GladeDesign(List.of(river), 2);
        var renderer = new SceneRenderer();
        var root = new Node();
        renderer.update(root, art, scene);
        int beforeRoad = ((Node) root.getChild(0)).getChild("Bờ suối") instanceof Node bank ? bank.getTriangleCount() : 0;
        renderer.update(root, art, scene.add(road));
        int withRoad = ((Node) root.getChild(0)).getChild("Bờ suối") instanceof Node bank ? bank.getTriangleCount() : 0;
        renderer.update(root, art, scene);
        int restoredBank = ((Node) root.getChild(0)).getChild("Bờ suối") instanceof Node bank ? bank.getTriangleCount() : 0;
        if (!(beforeRoad > withRoad && beforeRoad == restoredBank)) {
            throw new AssertionError("riverbank decor must rebuild when an adjacent road is added or removed");
        }
        var first=new GladeDesign.Point(-3,-3);
        var second=new GladeDesign.Point(3,3);
        var source=WaterGesture.draft(scene,List.of(first),first,1,0);
        var onlySource=scene.add(source);
        var flow=SceneRules.of(onlySource).flows.get(source.id());
        renderer.update(root,art,onlySource);
        var sourceNode=root.getChild(1);
        var additional=WaterGesture.draft(onlySource,List.of(second),second,2,0);
        var twoSources=onlySource.add(additional);
        renderer.update(root,art,twoSources);
        if(SceneRules.of(twoSources).flows.get(source.id())!=flow || root.getChild(1)!=sourceNode)
            throw new AssertionError("independent flow and mesh should be reused when another source is drawn");
        var raised=twoSources.withTerrain(twoSources.terrain().brush(first.x(),first.z(),2,2,null));
        renderer.update(root,art,raised);
        if(SceneRules.of(raised).flows.get(source.id())==flow || root.getChild(1)==sourceNode)
            throw new AssertionError("terrain edits must invalidate the affected flow and mesh");
        var empty=new GladeDesign(List.of(),2);
        var meadow=art.meadow(empty);
        var terrain=meadow.getChild("Địa hình");
        int clear=meadow.getTriangleCount();
        ReactiveFeatures.trimGrass(meadow,empty.add(WaterGesture.draft(empty,List.of(first),first,4,0)));
        if(meadow.getTriangleCount()>=clear || meadow.getChild("Địa hình")!=terrain)
            throw new AssertionError("wet ground trims grass without replacing the terrain");
        ReactiveFeatures.trimGrass(meadow,empty);
        if(meadow.getTriangleCount()!=clear)throw new AssertionError("grass returns after removing water");
        var distant=ProceduralMechanicsCheck.block(0,-8,0,2,2,2,1);
        var near=ProceduralMechanicsCheck.block(1,4,2,2,2,2,1);
        var dryRoad=ProceduralMechanicsCheck.path(2,-10,-4,-6,-4);
        var wetRoad=ProceduralMechanicsCheck.path(3,2,0,7,0);
        var village=new GladeDesign(List.of(distant,near,dryRoad,wetRoad),2);
        renderer.update(root,art,village);
        var original=List.copyOf(root.getChildren());
        var creek=ProceduralMechanicsCheck.source(4,4,0);
        renderer.update(root,art,village.add(creek));
        if(root.getChild(0)!=original.get(0) || root.getChild(2)!=original.get(2))
            throw new AssertionError("a distant dry house and road should retain their existing meshes");
        if(root.getChild(1)==original.get(1) || root.getChild(3)==original.get(3))
            throw new AssertionError("house near the bank and road crossing water should refresh");
        renderer.update(root,art,village);
        if(root.getChild(3).getParent()!=root)throw new AssertionError("removing water restores the road");
        System.out.println("RESOURCE_CACHE_PASS: bounded materials, path-sensitive banks, independent water, reversible grass and local scene updates");
    }
}
