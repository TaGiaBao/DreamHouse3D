package vn.dreamhouse.glade;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Per-attempt readiness receipt. Never reports readiness just because a process exists. */
final class LaunchSignals {
    static final String TOKEN_PROPERTY="dreamhouse.launchToken";
    static Path path(String token)throws IOException{
        if(token==null || !token.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"))throw new IllegalArgumentException("Mã lượt mở game không hợp lệ.");
        return PlayerFiles.directory("logs").resolve("launch-"+token+".status");
    }
    static void publish(String state){
        String token=System.getProperty(TOKEN_PROPERTY);if(token==null)return;
        try{
            var destination=path(token);var temporary=Files.createTempFile(destination.getParent(),"ready-",".tmp");
            try{
                Files.writeString(temporary,state+"\n");
                for(int attempt=0;;attempt++){
                    try{PlayerFiles.move(temporary,destination);break;}
                    catch(AccessDeniedException busy){
                        // A concurrent Windows reader can briefly hold the previous receipt open.
                        if(attempt==5)throw busy;
                        Thread.sleep(12L*(attempt+1));
                    }
                }
            }finally{Files.deleteIfExists(temporary);}
        }
        catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
        catch(IOException|IllegalArgumentException failure){PlayerFiles.error("Không ghi được trạng thái khởi động",failure);}
    }
    static String read(String token)throws IOException{var file=path(token);return Files.isRegularFile(file)?Files.readString(file).strip():"";}
}
