package vn.dreamhouse.glade;
import com.jme3.math.*;

/** Preserve attachment data while its supporting face is temporarily absent or covered. */
final class AttachmentRules {
    static boolean hidden(GladeDesign.Item part,GladeDesign scene){
        if(part.mount()==null)return false;
        var host=scene.items().stream().filter(i->i.id()==part.mount().hostId()).findFirst().orElseThrow();
        if(host.height()<=.12f)return true;
        if(part.kind().roof())return scene.hasUpper(host.id());
        if(part.kind().opening() && part.mount().lift()>=1){
            var roof=scene.attachedRoof(host.id());
            return scene.hasUpper(host.id()) || (roof==null?scene.roofRise(host)<=.02f:roof.kind()==GladeDesign.Kind.ROOF_FLAT);
        }
        if(!part.kind().opening())return false;
        var placement=scene.placement(part);float angle=placement.rotation()*FastMath.DEG_TO_RAD;
        float x=placement.center().x()+FastMath.sin(angle)*.08f,z=placement.center().z()+FastMath.cos(angle)*.08f;
        float y=placement.elevation()+(part.kind()==GladeDesign.Kind.DOOR_ARCH || part.kind()==GladeDesign.Kind.DOOR_DOUBLE?part.height()/2:0);
        for(var other:scene.items())if(other.id()!=host.id() && StructureShell.solid(other) && SceneRules.contains(other,x,z,-.01f)){
            float base=scene.placement(other).elevation();if(y>base+.02f && y<base+other.height()-.02f)return true;
        }
        return false;
    }
}
