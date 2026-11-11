package vn.dreamhouse.glade;

import java.util.*;
import static vn.dreamhouse.glade.GladeDesign.*;

/** Small authored scene built exclusively from the same saved items available to the player. */
final class CozyShowcase {
    static GladeDesign create(){
        var items=new ArrayList<Item>();var finish=GladeCatalog.plain();
        items.add(new Item(0,Kind.HOUSE,new Point(-2,-1),4.6f,3.6f,2.6f,true,List.of(),0,null,new Form(0,1.5f,0,-1).finish(finish)));
        items.add(new Item(1,Kind.HOUSE,new Point(.8f,-1.8f),3,3.2f,2.6f,true,List.of(),0,null,new Form(0,1.5f,0,-1).finish(finish)));
        items.add(new Item(2,Kind.TOWER,new Point(3,-2.5f),2.4f,2.4f,3.8f,true,List.of(),0,null,new Form(0,1.4f,0,-1).finish(finish)));
        items.add(new Item(3,Kind.DOOR_ARCH,new Point(-2,-1),1,.5f,1.9f,true,List.of(),0,new Mount(0,.12f,.5f,0)));
        items.add(new Item(4,Kind.WINDOW_ARCH,new Point(-2,-1),.9f,.5f,1.08f,true,List.of(),0,new Mount(0,-.28f,.5f,.57f)));
        items.add(new Item(5,Kind.WINDOW_ARCH,new Point(.8f,-1.8f),.9f,.5f,1.08f,true,List.of(),0,new Mount(1,0,.12f,1)));
        items.add(new Item(6,Kind.CHIMNEY_BRICK,new Point(-2,-1),.55f,.55f,1.1f,false,List.of(),0,new Mount(0,-.25f,-.15f,1)));
        items.add(new Item(7,Kind.TREE,new Point(-6,-3),2.2f,2.2f,3.4f,false,List.of()));
        items.add(new Item(8,Kind.TREE,new Point(6,4.5f),1.8f,1.8f,2.8f,false,List.of(),0,null,new Form(0,0,0,-1).finish(Finish.DEFAULT.variant(4))));
        items.add(new Item(9,Kind.FLOWERS,new Point(-4.6f,1.5f),1.4f,1,.5f,false,List.of()));
        items.add(new Item(10,Kind.WATER,new Point(5,1.5f),.5f,.5f,1,false,List.of(new Point(5,1.5f)),0,null,new Form(0,0,0,-1).finish(Finish.DEFAULT.variant(20))));
        var path=new ArrayList<Point>();for(int i=0;i<=48;i++){float t=i/48f;path.add(new Point(-1.45f+(float)Math.sin(t*3.3f)*1.2f,.8f+t*5.6f));}
        items.add(new Item(11,Kind.PATH,path.getFirst(),1,1,1,false,path));
        items.add(new Item(12,Kind.BENCH,new Point(1.4f,4.5f),1.6f,.7f,1,false,List.of()));
        var terrain=new TerrainField().brush(-7,-5,3, .5f,null);
        return new GladeDesign(items,new LandBounds(-11,11,-8,8),terrain);
    }
}
