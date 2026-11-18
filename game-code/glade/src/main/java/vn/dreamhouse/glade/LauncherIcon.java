package vn.dreamhouse.glade;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

final class LauncherIcon {
    static BufferedImage image(int size){
        var image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0,0,new Color(255,245,209),size,size,new Color(198,165,115)));g.fillRoundRect(1,1,size-2,size-2,Math.max(4,size/4),Math.max(4,size/4));GladeIcons.paint(g,"HOUSE",size/16,size/20,size*7/8);g.dispose();return image;
    }
    public static void main(String[] args)throws IOException{
        int[] sizes={16,32,48,64,128,256};var data=new ArrayList<byte[]>();for(int size:sizes){var output=new ByteArrayOutputStream();ImageIO.write(image(size),"png",output);data.add(output.toByteArray());}
        int header=6+16*sizes.length,total=header+data.stream().mapToInt(a->a.length).sum();var bytes=ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);bytes.putShort((short)0).putShort((short)1).putShort((short)sizes.length);int offset=header;
        for(int i=0;i<sizes.length;i++){int size=sizes[i];bytes.put((byte)(size==256?0:size)).put((byte)(size==256?0:size)).put((byte)0).put((byte)0).putShort((short)1).putShort((short)32).putInt(data.get(i).length).putInt(offset);offset+=data.get(i).length;}
        data.forEach(bytes::put);Files.write(Path.of(args[0]),bytes.array());
    }
}
