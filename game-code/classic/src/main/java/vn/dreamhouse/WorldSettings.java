package vn.dreamhouse;

import java.util.Properties;
import java.util.Set;

record WorldSettings(double hour, String weather, boolean automatic) {
    WorldSettings {
        if (!Double.isFinite(hour) || hour < 0 || hour > 24
                || !Set.of("CLEAR","RAIN","SNOW","MIST").contains(weather)) {
            throw new IllegalArgumentException("Thiết lập thời gian hoặc thời tiết không hợp lệ.");
        }
    }

    static WorldSettings defaults() { return new WorldSettings(16,"CLEAR",false); }

    void write(Properties properties) {
        properties.setProperty("world.hour",Double.toString(hour));
        properties.setProperty("world.weather",weather);
        properties.setProperty("world.automatic",Boolean.toString(automatic));
    }

    static WorldSettings read(Properties properties) {
        String automatic=properties.getProperty("world.automatic","false");
        if (!automatic.equals("true") && !automatic.equals("false"))
            throw new IllegalArgumentException("Chu kỳ thời gian không hợp lệ.");
        return new WorldSettings(Double.parseDouble(properties.getProperty("world.hour","16")),
                properties.getProperty("world.weather","CLEAR"),Boolean.parseBoolean(automatic));
    }
}
