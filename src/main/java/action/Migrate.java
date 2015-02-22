package action;

import org.btrplace.model.Node;
import org.btrplace.model.VM;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by vkherbac on 17/02/15.
 */
public class Migrate extends ActionLauncher {
    
    private Node srcNode, dstNode;
    private VM vm;
    private int bw;

    public Migrate(VM vm, Node srcNode, Node dstNode, int bw) {

        this.srcNode = srcNode;
        this.dstNode = dstNode;
        this.bw = bw;
        
        // TODO: mapper from VM/Node id to real name
        String vmName = vm.toString();
        String srcNodeName = srcNode.toString();
        String dstNodeName = dstNode.toString();

        script = "migrate.sh";

        params = new ArrayList<String>();

        params.addAll(Arrays.asList(vmName, srcNodeName, dstNodeName));
        params.add(String.valueOf(bw));
        params.add(" --live --timeout 600 ");
        //params.add(" --live --p2p --copy-storage-inc ");
    }

    @Override
    public String toString() {
        return "migrate.sh(vm='" + vm.toString() + "'; from='" + srcNode.toString() + "'; to='" + dstNode.toString() +
                "'; speed='" + String.valueOf(bw) + " mb/s')";
    }
}
