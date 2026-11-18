package vn.dreamhouse.glade;

import java.util.ArrayList;
import java.util.List;
import static vn.dreamhouse.glade.GladeDesign.*;

final class ArchitecturePresets {
    record Entry(String name,BlueprintModule module){}
    private static final List<Entry> CATALOG=List.of(terrace(),courtyard(),coveredBridge(),archway());

    static List<Entry> catalog(){
        return CATALOG;
    }

    private static Entry terrace(){
        var parts=new ArrayList<Item>();
        parts.add(block(0,Kind.HOUSE,0,0,5,3.4f,1.8f,0,-1,0));
        parts.add(block(1,Kind.HOUSE,-.7f,-.55f,3.3f,2.4f,2.0f,1.15f,0,0));
        parts.add(block(2,Kind.STAIRS_STONE,2.8f,1.5f,1.3f,3f,1.8f,0,0,-1.8f));
        return preset("Nhà bậc thang",parts);
    }

    private static Entry courtyard(){
        return preset("Nhà sân trong",List.of(
                block(0,Kind.HOUSE,0,0,6.4f,2.2f,2.4f,1.1f,-1,0),
                block(1,Kind.HOUSE,-2.1f,2.75f,2.2f,3.3f,2.4f,1.1f,0,-2.4f),
                block(2,Kind.HOUSE,2.1f,2.75f,2.2f,3.3f,2.4f,1.1f,0,-2.4f),
                block(3,Kind.PLANTER,0,2.8f,.95f,.95f,.55f,0,0,-2.4f)));
    }

    private static Entry coveredBridge(){
        return preset("Cầu có mái",List.of(
                block(0,Kind.DECK,0,0,2.2f,5.5f,.30f,0,-1,.35f),
                block(1,Kind.PAVILION,0,0,2.2f,5.2f,2.1f,.85f,0,0)));
    }

    private static Entry archway(){
        var root=block(0,Kind.HOUSE,0,0,3.6f,2.2f,2.7f,0,-1,0);
        return preset("Cổng vòm nối nhà",List.of(root,
                block(1,Kind.HOUSE,-3,0,2.4f,2.6f,2.7f,1.05f,0,-2.7f),
                block(2,Kind.HOUSE,3,0,2.4f,2.6f,2.7f,1.05f,0,-2.7f),
                new Item(3,Kind.DOOR_ARCH,root.center(),1.9f,.5f,2.45f,false,List.of(),0,new Mount(0,0,.5f,0)),
                new Item(4,Kind.DOOR_ARCH,root.center(),1.9f,.5f,2.45f,false,List.of(),180,new Mount(0,0,-.5f,0))));
    }

    private static Item block(long id,Kind kind,float x,float z,float w,float d,float h,float roof,long support,float lift){
        return new Item(id,kind,new Point(x,z),w,d,h,roof>0,List.of(),0,null,new Form(lift,roof,0,support).finish(GladeCatalog.plain()));
    }

    private static Entry preset(String name,List<Item> parts){
        return new Entry(name,new BlueprintModule(new GladeDesign(parts,2)));
    }
}
