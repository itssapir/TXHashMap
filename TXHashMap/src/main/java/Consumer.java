
public interface Consumer<T> {
    void consume(T val);
    static void clearNode(PCNode node)
    {
        node.CompareAndSet(PCNode.State.LOCKED, PCNode.State.READY);
    }

}
