package vn.dreamhouse.glade;

import java.util.ArrayList;
import java.util.List;
import static vn.dreamhouse.glade.GladeDesign.*;

/** An editable village made from the public building tools and the normal save model. */
final class RiversideVillage {
    static GladeDesign create() {
        var land=new LandBounds(-28,28,-18,18);
        var terrain=new TerrainField().brush(-20,-11,8,2.6f,null).brush(21,-10,8,2,null);
        var items=new ArrayList<Item>();
        String[] plaster={"#edd1a2","#dca894","#b5c8ae","#e8ca8e"};
        for(int i=0;i<12;i++){
            var point=new Point(-18+(i%6)*7.2f,i<6?-7:5.8f);
            long host=items.size();
            var kind=i%4==0?Kind.PENTAGON:i%4==1?Kind.LOG_CABIN:Kind.HOUSE;
            var finish=new Finish(i%4==1?"wood":"plaster",plaster[i%4],"tile",i%3==0?"#a96450":"#c68158",18);
            float angle=i<6?0:180;
            items.add(new Item(host,kind,point,3.4f,2.8f,2.2f+(i%3)*.36f,true,List.of(),angle,null,new Form(0,1.18f,0,-1).finish(finish)));
            if(kind!=Kind.PENTAGON){
                items.add(new Item(items.size(),Kind.DOOR_ARCH,point,.92f,.50f,1.66f,false,List.of(),0,new Mount(host,.16f,.5f,0)));
                items.add(new Item(items.size(),Kind.WINDOW_ARCH,point,.82f,.50f,.92f,false,List.of(),0,new Mount(host,-.22f,.5f,.54f)));
            }else{
                var edge=Pentagon.edges(3.4f,2.8f).getFirst();
                var anchor=edge.from().add(edge.to()).multLocal(.5f);var normal=edge.normal();
                float facing=(float)Math.toDegrees(Math.atan2(normal.x,normal.y));
                items.add(new Item(items.size(),Kind.WINDOW_ARCH,point,.78f,.50f,.94f,false,List.of(),facing,new Mount(host,anchor.x/3.4f,anchor.y/2.8f,.53f)));
                items.add(new Item(items.size(),Kind.CHIMNEY_STONE,point,.5f,.5f,.85f,false,List.of(),0,new Mount(host,.15f,0,0)));
            }
            if(i%4==1)items.add(new Item(items.size(),Kind.ROOF_SHED,point,3.4f,2.8f,.95f,true,List.of(),0,new Mount(host,0,0,0)));
        }
        items.add(new Item(items.size(),Kind.PERGOLA,new Point(0,-2.6f),2.2f,1.8f,1.8f,false,List.of()));
        addRoad(items,-22,22,-3,1.6f,"gravel");
        addRoad(items,-22,22,2.7f,1.35f,"brick");
        var river=new ArrayList<Point>();
        for(int i=0;i<=166;i++){float x=-24+i*.29f;river.add(new Point(x,(float)Math.sin(x*.22)*1.15f));}
        items.add(WaterGesture.draft(new GladeDesign(items,land,terrain),river,river.getLast(),1.05f,0));
        for(int bank:new int[]{-1,1}){
            var creek=new ArrayList<Point>();
            for(int i=0;i<=35;i++)creek.add(new Point(bank*(20+i*.055f),-9+i*.245f));
            items.add(WaterGesture.draft(new GladeDesign(items,land,terrain),creek,creek.getLast(),.6f,0));
        }
        for(int i=0;i<300;i++){
            int cluster=i/10,member=i%10;
            float centerX=-23+(cluster%10)*5.1f;
            float centerZ=(cluster/10==1?-1:1)*(cluster/10==2?15.2f:11.4f);
            float angle=member*2.39996f;
            float radius=.28f+(float)Math.sqrt(member)*.61f;
            float x=centerX+radius*(float)Math.cos(angle),z=centerZ+radius*(float)Math.sin(angle);
            var kind=member==0 && cluster%2==0?Kind.TREE:i%31==0?Kind.BENCH:Kind.FLOWERS;
            if(member==9){
                z=(cluster%2==0?-1:1)*(4.5f+.3f*(float)Math.sin(x));
                kind=Kind.FLOWERS;
            }
            float size=kind==Kind.TREE?1.5f:kind==Kind.BENCH?1.1f:.55f;
            float height=kind==Kind.TREE?2.45f:kind==Kind.BENCH?.78f:.33f;
            items.add(new Item(items.size(),kind,new Point(x,z),size,size,height,false,List.of(),0,null,new Form(0,0,0,-1).finish(Finish.DEFAULT.variant(i%4))));
        }
        return new GladeDesign(items,land,terrain);
    }
    private static void addRoad(List<Item> items,float start,float end,float z,float width,String material){
        var points=new ArrayList<Point>();
        for(int i=0;i<=220;i++){
            float x=start+(end-start)*i/220;
            points.add(new Point(x,z+.48f*(float)Math.sin(x*.35)));
        }
        items.add(new Item(items.size(),Kind.PATH,points.getFirst(),width,width,1,false,points,0,null,
                new Form(0,0,0,-1).finish(PathSurface.finish(material,""))));
    }
}
