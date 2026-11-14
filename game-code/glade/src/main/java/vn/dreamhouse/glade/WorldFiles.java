package vn.dreamhouse.glade;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

final class WorldFiles {
    static final String HEADER="DreamHouse Glade integrity=v1",DIGEST="integrity.sha256",FORMAT="integrity.format";
    @FunctionalInterface interface Content {void write(OutputStream output)throws IOException;}
    @FunctionalInterface interface BeforeReplace {void ready(Path temporary,Path destination)throws IOException;}
    static void write(Path destination,Properties values)throws IOException{
        values.setProperty(FORMAT,"1");values.setProperty(DIGEST,digest(values));
        replace(destination,out->values.store(out,HEADER),(temporary,target)->{});
    }
    static void replace(Path destination,Content content,BeforeReplace before)throws IOException{
        var target=destination.toAbsolutePath();var temporary=Files.createTempFile(target.getParent(),"glade-",".tmp");
        try{
            try(var channel=FileChannel.open(temporary,StandardOpenOption.WRITE);var output=Channels.newOutputStream(channel)){
                var bounded=new FilterOutputStream(output){long written;
                    @Override public void write(int value)throws IOException{if(++written>GladeDesign.MAX_FILE_BYTES)throw new IOException("Bản thiết kế vượt 16 MB; bản lưu trước được giữ nguyên.");out.write(value);}
                    @Override public void write(byte[] data,int offset,int length)throws IOException{if(written+length>GladeDesign.MAX_FILE_BYTES)throw new IOException("Bản thiết kế vượt 16 MB; bản lưu trước được giữ nguyên.");written+=length;out.write(data,offset,length);}
                };
                content.write(bounded);bounded.flush();channel.force(true);
            }
            GladeDesign.decode(read(temporary));before.ready(temporary,target);PlayerFiles.move(temporary,target);
        }finally{Files.deleteIfExists(temporary);}
    }
    static Properties read(Path file)throws IOException{
        if(Files.size(file)>GladeDesign.MAX_FILE_BYTES)throw new IOException("Tệp vượt giới hạn 16 MB.");
        byte[] data;try(var input=Files.newInputStream(file)){data=input.readNBytes(GladeDesign.MAX_FILE_BYTES+1);}
        if(data.length>GladeDesign.MAX_FILE_BYTES)throw new IOException("Tệp vượt giới hạn 16 MB.");
        var values=new Properties();try{values.load(new ByteArrayInputStream(data));}catch(IllegalArgumentException invalid){throw new IOException("Dữ liệu tệp lưu bị lỗi.",invalid);}
        boolean header=new String(data,StandardCharsets.ISO_8859_1).startsWith("#"+HEADER);
        if(header || values.containsKey(DIGEST) || values.containsKey(FORMAT)){
            String actual=values.getProperty(DIGEST,"");
            if(!"1".equals(values.getProperty(FORMAT)) || !actual.matches("[0-9a-fA-F]{64}") || !actual.equalsIgnoreCase(digest(values)))throw new IOException("Tệp lưu không còn nguyên vẹn. Hãy chọn bản phục hồi hoặc bản lưu khác.");
        }
        return values;
    }
    private static String digest(Properties values){
        try{
            var digest=MessageDigest.getInstance("SHA-256");
            for(String key:values.stringPropertyNames().stream().filter(k->!k.equals(DIGEST) && !k.startsWith("module.")).sorted().toList())for(String part:List.of(key,values.getProperty(key))){var bytes=part.getBytes(StandardCharsets.UTF_8);digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);}
            return HexFormat.of().formatHex(digest.digest());
        }catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
