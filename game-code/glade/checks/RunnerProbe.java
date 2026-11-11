package vn.dreamhouse.glade;
class RunnerProbe {
    public static void main(String[] args)throws Exception {
        int code=Integer.parseInt(args[0]);System.out.println("RUNNER_PROBE_PASS");
        if(code==-1)Thread.sleep(30000);else System.exit(code);
    }
}
