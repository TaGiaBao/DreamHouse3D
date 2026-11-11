package vn.dreamhouse.glade;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;

/** OS-held lock releases even after a crash; an old file is never treated as a running game. */
final class GameSessionLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;
    private GameSessionLock(FileChannel channel,FileLock lock){this.channel=channel;this.lock=lock;}
    static GameSessionLock acquire()throws IOException{
        return acquireNamed("game-session.lock");
    }
    static GameSessionLock acquireLauncher()throws IOException{return acquireNamed("launcher-session.lock");}
    private static GameSessionLock acquireNamed(String name)throws IOException{
        Files.createDirectories(PlayerFiles.root());var channel=FileChannel.open(PlayerFiles.root().resolve(name),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        try{FileLock lock;try{lock=channel.tryLock();}catch(OverlappingFileLockException busy){lock=null;}if(lock==null){channel.close();return null;}return new GameSessionLock(channel,lock);}
        catch(IOException|RuntimeException error){channel.close();throw error;}
    }
    static boolean isRunning()throws IOException{try(var acquired=acquire()){return acquired==null;}}
    @Override public void close()throws IOException{try{if(lock.isValid())lock.release();}finally{channel.close();}}
}
