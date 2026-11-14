package vn.dreamhouse.glade;

import java.util.*;
import static vn.dreamhouse.glade.GladeDesign.*;

final class GardenScene {
    static GladeDesign island() {
        var items = new ArrayList<Item>();
        var cottage = new Finish("plaster","#f4d5a4","tile","#b15e4a");
        add(items,Kind.HOUSE,0,-2,4.4f,3.8f,2.8f,1.65f,cottage);
        mounted(items,Kind.CHIMNEY_BRICK,0,.28f,-.16f,1,.52f,.52f,1.15f);
        path(items,0,0,2.3f,5,1);
        add(items,Kind.TREE,-4.2f,-1.6f,2.8f,2.8f,4.2f,0,Finish.DEFAULT.variant(6));
        add(items,Kind.TREE,4.1f,-3,2.6f,2.6f,4,0,Finish.DEFAULT);
        add(items,Kind.FLOWERS,-2.4f,.4f,1.4f,.9f,.52f,0,Finish.DEFAULT.variant(2));
        add(items,Kind.FLOWERS,2.7f,.7f,1.2f,.9f,.48f,0,Finish.DEFAULT.variant(1));
        add(items,Kind.BENCH,-2.9f,2.3f,1.6f,.7f,.85f,0,Finish.DEFAULT);
        return new GladeDesign(items,new LandBounds(-8,8,-7,7),new TerrainField());
    }

    static GladeDesign create() {
        var a=new ArrayList<Item>();
        var cream=new Finish("plaster","#eedbb2","tile","#c77b4b");
        var stone=new Finish("stone","#c9bea3","tile","#bc7346");
        add(a,Kind.HOUSE,-3,-2.6f,5.2f,3.7f,3.3f,1.9f,cream);
        add(a,Kind.HOUSE,-5.3f,.25f,2.5f,2.9f,2.4f,1.5f,cream);
        add(a,Kind.TOWER,.9f,-3.3f,2.5f,2.5f,5.5f,1.9f,stone);
        add(a,Kind.HOUSE,6,-1.6f,3.4f,3,2.4f,1.45f,cream);
        add(a,Kind.PAVILION,-.5f,-1.3f,2.1f,2.2f,2.4f,1.3f,cream);
        add(a,Kind.DECK,-2.8f,.12f,3.8f,1.4f,.18f,0,new Finish("wood","#b78e59","original",""));
        int balcony=a.size()-1;var b=a.get(balcony);a.set(balcony,new Item(b.id(),b.kind(),b.center(),b.width(),b.depth(),b.height(),false,List.of(),0,null,b.form().stilts(1.65f)));
        mounted(a,Kind.CHIMNEY_BRICK,0,-.30f,-.05f,1,.62f,.62f,1.25f);
        mounted(a,Kind.CHIMNEY_BRICK,3,.26f,-.10f,1,.50f,.50f,1);
        mounted(a,Kind.WINDOW_ARCH,0,.15f,.5f,.73f,.9f,.5f,1.05f);
        mounted(a,Kind.WINDOW_ARCH,0,-.20f,.2f,1,.8f,.5f,1);
        var stream=new ArrayList<Point>();for(int i=0;i<=90;i++){float t=i/90f;stream.add(new Point(-10.8f+21.6f*t,5.2f+(float)Math.sin(t*6.2f)*.75f));}
        a.add(new Item(a.size(),Kind.WATER,new Point(0,5.2f),21.6f,3.8f,1.05f,false,stream));
        var bridge=List.of(new Point(-1.2f,3.5f),new Point(-1.2f,6.9f));
        a.add(new Item(a.size(),Kind.BRIDGE,bridge.getFirst(),1.5f,1.5f,.8f,false,bridge));
        path(a,-2.8f,1.35f,-1.2f,3.55f,1.25f);path(a,-1.2f,6.9f,-.3f,8.5f,1.15f);
        path(a,-1,2.8f,6,.1f,1.05f);
        add(a,Kind.WELL,8.5f,2.2f,1.2f,1.2f,1.5f,0,Finish.DEFAULT);
        add(a,Kind.BENCH,2,2.8f,1.8f,.8f,1,0,Finish.DEFAULT);
        add(a,Kind.PERGOLA,5.7f,.65f,3.1f,1.9f,2.1f,0,Finish.DEFAULT);
        add(a,Kind.STAIRS_STONE,7.6f,.35f,1.2f,2.2f,.8f,0,Finish.DEFAULT);
        float[][] trees={{-9,-5,4.8f,0},{-8.6f,1.8f,4.2f,0},{-5.5f,-6.8f,3.8f,6},{4.3f,-6.5f,4.5f,0},{8.8f,-5.8f,4.2f,6},{10,1,3.5f,4},{-8,7.8f,3.2f,4},{7.4f,7.8f,3.8f,0}};
        for(var p:trees)add(a,Kind.TREE,p[0],p[1],p[2]*.67f,p[2]*.67f,p[2],0,Finish.DEFAULT.variant((int)p[3]));
        float[][] beds={{-6.6f,2},{-4.8f,1.9f},{-4,-4.7f},{-1.8f,1.8f},{1.3f,-1.6f},{3.8f,.4f},{5,.3f},{7.7f,.2f},{8.6f,2.9f},{-6,6.9f},{-4,7.1f},{2.2f,6.8f},{4,7.3f},{9,5.7f},{-9,4}};
        for(int i=0;i<beds.length;i++){var p=beds[i];add(a,Kind.FLOWERS,p[0],p[1],1.15f,.9f,.48f,0,Finish.DEFAULT.variant(i%3));}
        var fence=List.of(new Point(-10,-7),new Point(-1,-7),new Point(8,-7),new Point(10,-5));
        a.add(new Item(a.size(),Kind.FENCE,fence.getFirst(),1,1,.9f,false,fence));
        return new GladeDesign(a,new LandBounds(-12,12,-10,10),new TerrainField());
    }
    private static void add(List<Item>a,Kind k,float x,float z,float w,float d,float h,float roof,Finish f){a.add(new Item(a.size(),k,new Point(x,z),w,d,h,roof>0,List.of(),0,null,new Form(0,roof,0,-1).finish(f)));}
    private static void mounted(List<Item>a,Kind kind,int host,float u,float v,float lift,float w,float d,float h){a.add(new Item(a.size(),kind,a.get(host).center(),w,d,h,false,List.of(),0,new Mount(host,u,v,lift)));}
    private static void path(List<Item>a,float ax,float az,float bx,float bz,float width){var p=new ArrayList<Point>();for(int i=0;i<=32;i++){float t=i/32f;p.add(new Point(ax+(bx-ax)*t+(float)Math.sin(t*Math.PI)*.35f,az+(bz-az)*t));}a.add(new Item(a.size(),Kind.PATH,p.getFirst(),width,width,1,false,p));}
}
