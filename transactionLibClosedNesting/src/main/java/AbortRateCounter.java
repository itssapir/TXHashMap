public class AbortRateCounter {

    protected long parentAborts;
    protected long nestedAborts;
    protected long nestedTries;
    protected long parentTries;

    public AbortRateCounter()
    {
        parentAborts = 0;
        nestedAborts = 0;
        nestedTries = 0;
        parentTries = 0;
    }

    public void nestedAbort()
    {
        nestedAborts++;
    }

    public void parentAbort()
    {
        parentAborts++;
    }
    public void nestedTry()
    {
        nestedTries++;
    }

    public void parentTry()
    {
        parentTries++;
    }

    public long getParentAborts() {
        return parentAborts;
    }

    public long getNestedAborts() {
        return nestedAborts;
    }

    public long getNestedTries() {
        return nestedTries;
    }

    public long getParentTries() {
        return parentTries;
    }

    public void accumulate(AbortRateCounter counter)
    {
        this.nestedTries += counter.nestedTries;
        this.nestedAborts += counter.nestedAborts;
        this.parentTries += counter.parentTries;
        this.parentAborts += counter.parentAborts;
    }

    public double ParentRate()
    {
        if(parentTries == 0) return 0;
        return (double)parentAborts/parentTries;
    }

    public double NestedRate()
    {
        if(nestedTries == 0) return 0;
        return (double)nestedAborts/nestedTries;
    }

}
