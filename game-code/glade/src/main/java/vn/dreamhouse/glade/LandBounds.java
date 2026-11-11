package vn.dreamhouse.glade;

record LandBounds(float minX,float maxX,float minZ,float maxZ) {
    static final float LIMIT_X=256;
    static final float LIMIT_Z=256;
    enum Edge { LEFT, RIGHT, BACK, FRONT }

    LandBounds {
        if(!Float.isFinite(minX) || !Float.isFinite(maxX) || !Float.isFinite(minZ) || !Float.isFinite(maxZ)
                || minX < -LIMIT_X || maxX > LIMIT_X || minZ < -LIMIT_Z || maxZ > LIMIT_Z
                || maxX-minX < 8 || maxX-minX > 512 || maxZ-minZ < 6 || maxZ-minZ > 512)
            throw new IllegalArgumentException("Mép đất nằm ngoài vùng dựng 512 × 512.");
    }

    static LandBounds level(int level) {
        if(level<1 || level>3) throw new IllegalArgumentException("Cấp đất không hợp lệ.");
        float x=9+4*(level-1),z=6.5f+3*(level-1);
        return new LandBounds(-x,x,-z,z);
    }
    float width() { return maxX-minX; }
    float depth() { return maxZ-minZ; }
    float centerX() { return (minX+maxX)/2; }
    float centerZ() { return (minZ+maxZ)/2; }
}
