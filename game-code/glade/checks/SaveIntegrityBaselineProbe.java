package vn.dreamhouse.glade;
import java.nio.file.*;
import java.util.*;
class SaveIntegrityBaselineProbe {
    public static void main(String[]args)throws Exception{
        var file=PlayerFiles.directory("integrity-baseline").resolve("changed.glade");CozyShowcase.create().save(file);
        var props=new Properties();try(var input=Files.newInputStream(file)){props.load(input);}props.setProperty("count","0");try(var output=Files.newOutputStream(file)){props.store(output,"accidental semantic corruption");}
        try{var loaded=GladeDesign.load(file);System.out.println("BASELINE_SILENT_CORRUPTION_ACCEPTED objects="+loaded.items().size());}catch(Exception rejected){System.out.println("BASELINE_CORRUPTION_REJECTED");}
    }
}
