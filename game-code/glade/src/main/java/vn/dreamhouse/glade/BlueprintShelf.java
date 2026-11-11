package vn.dreamhouse.glade;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** One atomic .glade file per personal template; a damaged entry never blocks the other entries. */
final class BlueprintShelf {
    static final int MAX_BYTES=2*1024*1024,MAX_ENTRIES=128;
    record Entry(String id,String name,BlueprintModule module){}
    record Listing(List<Entry> entries,int skipped){}
    private final Path directory;
    BlueprintShelf(Path directory)throws IOException{this.directory=Files.createDirectories(directory);}
    Listing list()throws IOException{
        var entries=new ArrayList<Entry>();int skipped=0;
        try(var stream=Files.list(directory)){
            var files=stream.filter(p->p.getFileName().toString().endsWith(".glade")).sorted().limit(MAX_ENTRIES+1L).toList();
            for(var path:files){if(entries.size()>=MAX_ENTRIES){skipped++;continue;}try{entries.add(read(path));}catch(IOException|IllegalArgumentException error){skipped++;}}
        }
        entries.sort(Comparator.comparing(Entry::name,String.CASE_INSENSITIVE_ORDER));return new Listing(List.copyOf(entries),skipped);
    }
    Entry save(String name,BlueprintModule module)throws IOException{
        try(var files=Files.list(directory)){if(files.filter(p->p.getFileName().toString().endsWith(".glade")).count()>=MAX_ENTRIES)throw new IOException("Hộp đã có 128 mẫu. Xóa bớt mẫu trước khi lưu thêm.");}
        var entry=new Entry(UUID.randomUUID().toString(),name(name),module);write(entry);return entry;
    }
    Entry rename(String id,String name)throws IOException{var old=read(path(id));var entry=new Entry(old.id(),name(name),old.module());write(entry);return entry;}
    void delete(String id)throws IOException{Files.deleteIfExists(path(id));}
    private Entry read(Path path)throws IOException{
        if(Files.size(path)>MAX_BYTES)throw new IOException("Mẫu vượt 2 MB.");
        var props=new Properties();try(var input=Files.newInputStream(path)){props.load(input);}
        if(!"1".equals(props.getProperty("module.version")))throw new IOException("Mẫu chưa đúng định dạng.");
        String id=path.getFileName().toString().replaceFirst("\\.glade$","");path(id);
        return new Entry(id,name(props.getProperty("module.name","Mẫu công trình")),new BlueprintModule(GladeDesign.load(path)));
    }
    private void write(Entry entry)throws IOException{
        Path destination=path(entry.id()),temporary=Files.createTempFile(directory,"module-",".tmp");
        try{
            entry.module().design().save(temporary);var props=new Properties();try(var input=Files.newInputStream(temporary)){props.load(input);}props.setProperty("module.version","1");props.setProperty("module.name",name(entry.name()));
            try(var output=Files.newOutputStream(temporary)){props.store(output,"DreamHouse personal blueprint");}
            if(Files.size(temporary)>MAX_BYTES)throw new IOException("Mẫu vượt 2 MB. Bản cũ vẫn được giữ.");
            try(var channel=java.nio.channels.FileChannel.open(temporary,StandardOpenOption.WRITE)){channel.force(true);}
            GladeDesign.load(temporary);
            PlayerFiles.move(temporary,destination);
        }finally{Files.deleteIfExists(temporary);}
    }
    private Path path(String id){if(id==null || !id.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"))throw new IllegalArgumentException("Mã mẫu không hợp lệ.");return directory.resolve(id+".glade");}
    static String name(String value){String name=value==null?"":value.strip();if(name.isEmpty() || name.length()>40 || name.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Tên mẫu cần 1–40 ký tự, không xuống dòng.");return name;}
}
