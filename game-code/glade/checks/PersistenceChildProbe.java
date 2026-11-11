package vn.dreamhouse.glade;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
class PersistenceChildProbe {
    public static void main(String[]args)throws Exception{
        var folder=Path.of(new String(Base64.getUrlDecoder().decode(args[1]),java.nio.charset.StandardCharsets.UTF_8));int version=Integer.parseInt(args[2]);
        if(args[0].equals("write")){try(var store=new SessionStore(folder)){store.save(SessionPersistenceCheck.scene(version)).get(10,TimeUnit.SECONDS);}System.out.println("CHILD_COMMITTED "+version);}
        else if(args[0].equals("recover")){try(var store=new SessionStore(folder)){var found=store.recover().orElseThrow();if(!found.design().items().equals(SessionPersistenceCheck.scene(version).items()))throw new AssertionError("wrong restarted snapshot");}System.out.println("CHILD_RECOVERY_PASS "+version);}
        else if(args[0].equals("interrupt")){
            Path candidate=folder.resolve("candidate.glade");SessionPersistenceCheck.scene(version).save(candidate);
            WorldFiles.replace(folder.resolve("autosave-0.glade"),out->Files.copy(candidate,out),(temporary,target)->{Files.writeString(folder.resolve("prepared.marker"),temporary.toString());try{Thread.sleep(60000);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new java.io.IOException(e);}});
        }
    }
}
