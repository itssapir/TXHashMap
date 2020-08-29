public class Pair<T, V> {
	private T first;
	private V second;
	
	Pair(T t, V v) {
		first = t;
		second = v;
	}
	
	public T getFirst() {
		return first;
	}
	
	public V getSecond() {
		return second;
	}
}
