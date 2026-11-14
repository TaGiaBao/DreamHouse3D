package vn.dreamhouse.glade;

import com.jme3.math.Vector3f;
import java.io.IOException;
import java.nio.file.Path;

record CameraBookmark(Vector3f focus,float yaw,float elevation,float distance){
    CameraBookmark{
        if(!Vector3f.isValidVector(focus) || !Float.isFinite(yaw) || !Float.isFinite(elevation)
                || !Float.isFinite(distance) || distance<=0)throw new IllegalArgumentException("Góc nhìn không hợp lệ.");
        focus=focus.clone();
    }

    static CameraBookmark read(Path world){
        var raw=PlayerFiles.settings().getProperty(key(world));
        if(raw==null)return null;
        var parts=raw.split(",");
        if(parts.length!=6)return null;
        try{
            return new CameraBookmark(new Vector3f(Float.parseFloat(parts[0]),Float.parseFloat(parts[1]),Float.parseFloat(parts[2])),
                    Float.parseFloat(parts[3]),Float.parseFloat(parts[4]),Float.parseFloat(parts[5]));
        }catch(IllegalArgumentException invalid){return null;}
    }

    void save(Path world)throws IOException{
        var settings=PlayerFiles.settings();
        settings.setProperty(key(world),focus.x+","+focus.y+","+focus.z+","+yaw+","+elevation+","+distance);
        PlayerFiles.saveSettings(settings);
    }

    private static String key(Path world){
        if(world==null)return "camera.bookmark";
        return "camera.bookmark."+java.util.UUID.nameUUIDFromBytes(world.toAbsolutePath().normalize().toString()
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
