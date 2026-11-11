package vn.dreamhouse.glade;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Writable player data never lives in the installed application directory. */
final class PlayerFiles {
    static Path root(){
        String override=System.getProperty("dreamhouse.userDir");
        if(override!=null)return Path.of(override).toAbsolutePath();
        String local=System.getenv("LOCALAPPDATA");
        return (local==null?Path.of(System.getProperty("user.home"),".dreamhouse"):Path.of(local,"DreamHouse")).toAbsolutePath();
    }
    static Path directory(String name)throws IOException {return Files.createDirectories(root().resolve(name));}
    static void error(String message,Throwable error){
        try(var out=new PrintWriter(Files.newBufferedWriter(directory("logs").resolve("game-errors.log"),java.nio.charset.StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND))){out.println(java.time.Instant.now()+" "+message);if(error!=null)error.printStackTrace(out);}catch(IOException ignored){}
    }
    static Properties settings(){var p=new Properties();try(var input=Files.newInputStream(root().resolve("settings.properties"))){p.load(input);}catch(IOException ignored){}catch(IllegalArgumentException invalid){p.clear();error("Cài đặt lỗi, dùng giá trị mặc định",invalid);}return p;}
    static void saveSettings(Properties p)throws IOException {
        Files.createDirectories(root());Path temporary=Files.createTempFile(root(),"settings-",".tmp");
        try{try(var out=Files.newOutputStream(temporary)){p.store(out,"DreamHouse settings");}move(temporary,root().resolve("settings.properties"));}finally{Files.deleteIfExists(temporary);}
    }
    static void move(Path source,Path destination)throws IOException {try{Files.move(source,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(source,destination,StandardCopyOption.REPLACE_EXISTING);}}
}
