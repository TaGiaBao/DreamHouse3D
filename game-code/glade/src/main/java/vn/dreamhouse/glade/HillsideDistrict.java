package vn.dreamhouse.glade;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import static vn.dreamhouse.glade.GladeDesign.*;

final class HillsideDistrict {
    private static final String[] PLASTER={"#dc8a49","#dc8d7c","#e3bd69","#ece0b9","#d57759","#c2bc9d","#e4ab83"};
    private static final Finish OCHRE=new Finish("plaster","#d79547","tile","#a75840",18);
    private static final Finish BELL_TOWER=new Finish("plaster","#d49a53","slate","#35404a",18);
    private static final float[][] EAST_OFFSETS={
        {-.7f,.35f,1.25f,.55f,-.8f,-1.35f,.45f},
        {1.05f,.42f,-.50f,-1.25f,-.78f,.82f,1.26f},
        {.75f,-.65f,-1.40f,-.34f,.78f,1.44f,.44f}
    };
    private static final float[][] WEST_OFFSETS={
        {.7f,-.34f,.30f,1.32f,.56f,-1.25f,-.7f,.49f,1.18f},
        {-1.12f,.06f,1.10f,.42f,-.92f,-1.38f,.18f,1.21f},
        {.95f,.32f,-1.08f,-1.44f,-.48f,.85f,1.24f,.12f}
    };
    private record Parcel(float width,float step,int floors,float setback,float depth,float storey,float roof){}
    private static final Parcel[][] EAST={
        {new Parcel(6.2f,1.35f,2,.15f,3.9f,2.15f,1.12f),new Parcel(5.3f,1.35f,1,.48f,3.3f,2.10f,.9f),new Parcel(8.1f,1.65f,2,.08f,4.05f,2.22f,1.25f),new Parcel(4.6f,1.65f,0,0,0,0,0),new Parcel(6.8f,2.12f,2,.34f,3.75f,2.25f,1.18f),new Parcel(7.4f,2.12f,3,.18f,4.05f,2.05f,1.45f),new Parcel(5.6f,1.78f,1,.65f,3.05f,2.30f,1.00f)},
        {new Parcel(7.1f,1.12f,1,.58f,3.35f,2.20f,1.18f),new Parcel(6.8f,1.38f,2,.22f,3.85f,2.15f,1.22f),new Parcel(6.2f,1.38f,2,.08f,4.00f,2.25f,1.08f),new Parcel(4.4f,1.66f,0,0,0,0,0),new Parcel(8.2f,2.05f,3,.18f,4.00f,2.20f,1.40f),new Parcel(5.3f,2.05f,2,.58f,3.50f,2.15f,1.07f),new Parcel(6.0f,1.70f,2,.24f,3.70f,2.25f,1.23f)},
        {new Parcel(6.4f,1.0f,1,.72f,3.20f,2.15f,1.1f),new Parcel(7.8f,1.20f,1,.38f,3.75f,2.20f,1.18f),new Parcel(5.6f,1.55f,2,.22f,3.65f,2.12f,1.22f),new Parcel(4.7f,1.55f,0,0,0,0,0),new Parcel(6.0f,1.82f,2,.46f,3.62f,2.25f,1.22f),new Parcel(7.5f,2.30f,4,.16f,4.02f,2.20f,1.50f),new Parcel(6.0f,2.02f,3,.44f,3.55f,2.25f,1.32f)}
    };
    private static final Parcel[][] WEST={
        {new Parcel(4.0f,1.18f,2,.05f,4.02f,2.05f,1.40f),new Parcel(5.1f,1.18f,3,.28f,3.80f,1.92f,1.12f),new Parcel(4.35f,1.18f,2,.12f,3.95f,2.35f,1.82f),new Parcel(5.45f,1.18f,1,.64f,3.35f,2.65f,.93f),new Parcel(3.2f,1.18f,0,0,0,0,0),new Parcel(6.0f,1.18f,2,.15f,4.06f,2.25f,1.52f),new Parcel(4.7f,1.18f,3,.30f,3.80f,2.05f,1.30f),new Parcel(5.8f,1.18f,2,.08f,4.02f,2.18f,1.69f),new Parcel(5.4f,1.18f,1,.40f,3.62f,2.52f,1.12f)},
        {new Parcel(5.2f,1.08f,1,.34f,3.68f,2.30f,1.10f),new Parcel(4.5f,1.08f,2,.55f,3.45f,2.10f,1.40f),new Parcel(6.6f,1.26f,2,.15f,4.05f,2.27f,1.75f),new Parcel(3.4f,1.26f,0,0,0,0,0),new Parcel(4.8f,1.46f,3,.27f,3.90f,2.04f,1.27f),new Parcel(6.1f,1.46f,2,.58f,3.42f,2.18f,1.85f),new Parcel(5.4f,1.46f,1,.22f,3.95f,2.58f,1.18f),new Parcel(8.0f,1.17f,2,.38f,3.72f,2.35f,1.55f)},
        {new Parcel(6.3f,.96f,1,.48f,3.55f,2.30f,1.18f),new Parcel(5.1f,1.12f,2,.20f,4.0f,2.13f,1.52f),new Parcel(4.4f,1.12f,1,.60f,3.45f,2.47f,1.25f),new Parcel(3.5f,1.32f,0,0,0,0,0),new Parcel(5.4f,1.32f,2,.28f,3.90f,2.28f,1.58f),new Parcel(5.0f,1.42f,3,.12f,3.95f,2.44f,2.25f),new Parcel(6.7f,1.42f,1,.54f,3.50f,2.58f,1.04f),new Parcel(7.6f,1.23f,2,.34f,3.77f,2.13f,1.37f)}
    };

    static GladeDesign create(boolean eastern){
        var land=new LandBounds(-28,29,-20,17);
        var heights=new HashMap<TerrainField.Cell,Float>();
        for(int x=-38;x<=39;x++)for(int z=-27;z<=23;z++){
            float wx=x*TerrainField.STEP,wz=z*TerrainField.STEP;
            float riseLine=eastern?-1.5f+wx*.13f:-1.4f+(float)Math.cos(wx*.14f)*1.8f;
            float hill=Math.clamp((riseLine-wz)/(eastern?9.5f:14f),0,1);
            float river=Math.clamp((z*.75f-9)/4,0,1);
            float hillHeight=eastern?8.5f+Math.clamp(wx*.20f,0,3.8f):6.4f;
            heights.put(new TerrainField.Cell(x,z),hill*hill*(3-2*hill)*hillHeight-river*.92f);
        }
        var terrain=new TerrainField(heights);
        var items=new ArrayList<Item>();
        var stone=GladeCatalog.finish(GladeCatalog.v("DECK","Nền bậc đá",STONE_PLATFORM));
        var rows=eastern?EAST:WEST;
        var offsets=eastern?EAST_OFFSETS:WEST_OFFSETS;
        var landings=new ArrayList<Point>();
        for(int row=0;row<rows.length;row++){
            float streetZ=4-row*(eastern?5.50f:5.75f);
            float edge=-22+(eastern?row*2.1f:row*.75f);
            var route=new ArrayList<Point>();
            for(int bay=0;bay<rows[row].length;bay++){
                var parcel=rows[row][bay];
                float width=parcel.width(),x=edge+width/2;
                float z=streetZ+offsets[row][bay];
                edge+=width;
                route.add(new Point(x,z+2.38f));
                long platform=items.size();
                items.add(new Item(platform,Kind.DECK,new Point(x,z),width,6.0f,parcel.step(),false,List.of(),0,null,
                        new Form(0,0,0,-1).finish(stone)));
                var finish=eastern?GladeCatalog.hillside():new Finish("plaster",PLASTER[(bay+2*row)%PLASTER.length],"tile",bay%2==0?"#a9563f":"#b7644b",18);
                if(parcel.floors()==0)landings.add(new Point(x,z));
                long support=platform;
                int floors=parcel.floors();
                if(eastern && floors>0 && row==1 && bay>=4)floors++;
                for(int floor=0;floor<floors;floor++){
                    long id=items.size();
                    float sideShift=eastern?offsets[row][bay]*floor*.28f:offsets[row][bay]*floor*.07f;
                    float retreat=floor*(eastern?.28f:.045f)+Math.abs(offsets[row][bay])*floor*.11f;
                    var center=new Point(x+sideShift,z-.60f-parcel.setback()-retreat);
                    float frontWidth=width-(eastern?floor*.62f:floor*.045f);
                    float depth=parcel.depth()+(eastern?.95f:.52f)-floor*(eastern?.50f:.08f);
                    float height=parcel.storey()-(eastern?floor*.06f:0)+(!eastern && row==0?.45f:0);
                    items.add(new Item(id,Kind.HOUSE,center,frontWidth,depth,height,true,List.of(),0,null,
                            new Form(0,parcel.roof(),0,support).finish(finish)));
                    if(floor==0)items.add(new Item(items.size(),Kind.DOOR_ARCH,center,.8f,.5f,1.52f,false,List.of(),0,new Mount(id,0,.5f,0)));
                    if(eastern)items.add(new Item(items.size(),Kind.WINDOW_ARCH,center,.72f,.5f,.86f,false,List.of(),0,new Mount(id,-.24f,.5f,.56f)));
                    support=id;
                }
            }
            road(items,route,1.03f,eastern?"gravel":"brick");
        }
        if(eastern)ridgeBuildings(items);
        else bellTower(items);
        road(items,-22,22,8.0f,1.55f,eastern?"concrete":"gravel");
        var paved=new GladeDesign(items,land,terrain);
        var rules=SceneRules.of(paved);
        for(var landing:landings){
            float x=landing.x(),z=landing.z();
            float low=rules.walkSurface(x,z+3.6f);
            float high=rules.walkSurface(x,z+2.3f);
            if(high-low>.30f)items.add(new Item(items.size(),Kind.STAIRS_STONE,new Point(x,z+3f),1.12f,1.35f,high-low,false,List.of(),0,null,
                    new Form(low-terrain.height(x,z+3f),0,0,-1)));
        }
        var river=new ArrayList<Point>();
        for(int i=0;i<=104;i++)river.add(new Point(-23+i*.44f,12.1f+.28f*(float)Math.sin(i*.08f)));
        items.add(WaterGesture.draft(new GladeDesign(items,land,terrain),river,river.getLast(),1.12f,0));
        return new GladeDesign(items,land,terrain);
    }

    private static void bellTower(List<Item> items){
        var center=new Point(-15.5f,-13.9f);
        long base=items.size();
        items.add(new Item(base,Kind.HOUSE,center,10.5f,5.1f,4.3f,true,List.of(),0,null,
                new Form(0,1.45f,0,-1).finish(OCHRE)));
        var tower=new Point(-16.6f,-14.4f);
        long shaft=items.size();
        items.add(new Item(shaft,Kind.HOUSE,tower,2.85f,2.9f,5.6f,false,List.of(),0,null,
                new Form(0,0,0,base).finish(BELL_TOWER)));
        items.add(new Item(items.size(),Kind.WINDOW_ARCH,tower,.95f,.5f,1.9f,false,List.of(),0,
                new Mount(shaft,0,.5f,.60f)));
        long crown=items.size();
        items.add(new Item(crown,Kind.TOWER,tower,2.35f,2.35f,2.3f,true,List.of(),0,null,
                new Form(0,1.30f,0,shaft).finish(BELL_TOWER)));
        items.add(new Item(items.size(),Kind.WINDOW_ROUND,tower,.7f,.5f,.7f,false,List.of(),0,
                new Mount(crown,0,.5f,.55f)));
        for(float x:new float[]{-22f,-19.8f,-8.3f,-5.6f,3.5f,7.4f,11.3f,16.5f,22f}){
            items.add(new Item(items.size(),Kind.TREE,new Point(x,-17.7f),1.15f,1.15f,3.6f,false,List.of(),0,null,
                    new Form(0,0,0,-1).finish(Finish.DEFAULT.variant(5))));
        }
    }

    private static void ridgeBuildings(List<Item> items){
        float[] heights={9,13,7,11,16,12,19,14};
        for(int i=0;i<heights.length;i++){
            var point=new Point(-23.5f+i*6.75f,-16.3f-(i%2)*1.15f);
            var finish=new Finish("stone",i%2==0?"#7c929e":"#a7aeb4","original","",0);
            items.add(new Item(items.size(),Kind.HOUSE,point,4.0f+i%3*.65f,3.8f,heights[i],false,List.of(),0,null,
                    new Form(0,0,0,-1).finish(finish)));
        }
    }

    private static void road(List<Item> items,float minX,float maxX,float z,float width,String material){
        road(items,List.of(new Point(minX,z),new Point(maxX,z)),width,material);
    }

    private static void road(List<Item> items,List<Point> route,float width,String material){
        var points=new ArrayList<Point>();
        points.add(route.getFirst());
        for(int i=1;i<route.size();i++){
            var from=route.get(i-1);var to=route.get(i);
            int segments=(int)Math.ceil(from.distance(to)/.24f);
            for(int j=1;j<=segments;j++){
                float t=j/(float)segments;
                points.add(new Point(from.x()+(to.x()-from.x())*t,from.z()+(to.z()-from.z())*t));
            }
        }
        items.add(new Item(items.size(),Kind.PATH,points.getFirst(),width,width,1,false,points,0,null,
                new Form(0,0,0,-1).finish(PathSurface.finish(material,""))));
    }
}
