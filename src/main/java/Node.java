import java.util.ArrayList;

public class Node {
    private String name;
    private Node previousNode;
    private ArrayList<Node> relatedNodes;

    public Node(String name) {
        this.name = name;
        relatedNodes = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Node getPreviousNode() {
        return previousNode;
    }

    public void setPreviousNode(Node previousNode) {
        this.previousNode = previousNode;
    }

    public ArrayList<Node> getRelatedNodes() {
        return relatedNodes;
    }

    public void setRelatedNodes(ArrayList<Node> relatedNodes) {
        this.relatedNodes = relatedNodes;
    }
}
