import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class NestingTest {
	
	// TODO find good parameters for closed nesting
	
	private static final int DEFAULT_KEY_RANGE = (1 << 10);
	private static final int DEFAULT_NUM_OF_KEYS = (1 << 8);
	private static final int DEFAULT_NUM_TXS = (1 << 10);
	private static final int DEFAULT_NUM_THREADS = (1);
	private static final int DEFAULT_NUM_IN_LIST = 20;
	private static final int DEFAULT_NUM_ACTION_IN_INNER = 3;
	private static final int DEFAULT_NUM_GETS_IN_OUTTER = (1 << 6);
	private static final boolean CLOSED_NESTING = TX.CLOSED_NESTING;
	private static final boolean FLAT_NESTING = TX.FLAT_NESTING;

	private static int KEY_RANGE;
	private static int NUM_OF_KEYS;
	private static int NUM_TXS;
	private static int NUM_THREADS;
	private static int NUM_IN_INNER_LIST;
	private static int NUM_ACTION_IN_INNER;
	private static int NUM_GETS_IN_OUTTER;

	private static Random rand = new Random(1);

	private static void displayUsage() {
		System.out.println("Usage:");
		System.out.println("\nOptions: (defaults)\n");
		System.out.println("t <UINT>	Number of [t]hreads (" + DEFAULT_NUM_THREADS + ")");
		System.out.println("x <UINT>	Transactions(t[x]) per thread (" + DEFAULT_NUM_TXS + ")");
		System.out.println("r <UINT>	Key [r]ange (" + DEFAULT_KEY_RANGE + ")");
		System.out.println("k <UINT>	Number of [k]eys (" + DEFAULT_NUM_OF_KEYS + ")");
		System.out.println("l <UINT>	Number of keys in inner [l]ist (" + DEFAULT_NUM_IN_LIST + ")");
		System.out.println("a <UINT>	Number of [a]ctions in inner TX (" + DEFAULT_NUM_ACTION_IN_INNER + ")");
		System.out.println("a <UINT>	Number of [g]ets in outter TX (" + DEFAULT_NUM_GETS_IN_OUTTER + ")");
		System.exit(1);
	}

	private void setDefaultParams() {
		KEY_RANGE = DEFAULT_KEY_RANGE;
		NUM_OF_KEYS = DEFAULT_NUM_OF_KEYS;
		NUM_TXS = DEFAULT_NUM_TXS;
		NUM_THREADS = DEFAULT_NUM_THREADS;
		NUM_IN_INNER_LIST = DEFAULT_NUM_IN_LIST;
		NUM_ACTION_IN_INNER = DEFAULT_NUM_ACTION_IN_INNER;
		NUM_GETS_IN_OUTTER = DEFAULT_NUM_GETS_IN_OUTTER;
	}

	private void parseArgs(String argv[]) {
		int opterr = 0;

		setDefaultParams();
		for (int i = 0; i < argv.length; i++) {
			String arg = argv[i];
			switch (arg) {
				case "-t":
					NUM_THREADS = Integer.parseInt(argv[++i]);
					break;
				case "-x":
					NUM_TXS = Integer.parseInt(argv[++i]);
					break;
				case "-r":
					KEY_RANGE = Integer.parseInt(argv[++i]);
					break;
				case "-k":
					NUM_OF_KEYS = Integer.parseInt(argv[++i]);
					break;
				case "-l":
					NUM_IN_INNER_LIST = Integer.parseInt(argv[++i]);
					break;
				case "-a":
					NUM_ACTION_IN_INNER = Integer.parseInt(argv[++i]);
					break;
				case "-g":
					NUM_GETS_IN_OUTTER = Integer.parseInt(argv[++i]);
					break;
				default:
					opterr++;
					break;
			}
		}

		if (opterr > 0) {
			displayUsage();
		}
		
		if(FLAT_NESTING){
			System.out.println("Flat nesting");
		} else if (CLOSED_NESTING){
			System.out.println("Closed nesting");
		} else {
			System.out.println("No nesting");
			System.exit(1);
		}

		System.out.println("Threads = " + NUM_THREADS);
		System.out.println("Transactions/thread = " + NUM_TXS);
		System.out.println("Key range = " + KEY_RANGE);
		System.out.println("Num of keys = " + NUM_OF_KEYS);
		System.out.println("Num of keys in inner list = " + NUM_IN_INNER_LIST);
		System.out.println("Num of actions in inner TX = " + NUM_ACTION_IN_INNER);
		System.out.println("Num of gets in outter TX = " + NUM_GETS_IN_OUTTER + "\n");

	}

	class ListObjectThread implements Runnable {

		LinkedList ll;
		CountDownLatch latch;

		ListObjectThread(LinkedList LL, CountDownLatch cdl) {
			ll = LL;
			latch = cdl;
		}
		
		@Override
		public void run() {
			try {
				latch.await();
			} catch (InterruptedException exp) {
				System.out.println("InterruptedException");
			}
			for (int i = 0; i < NUM_TXS; i++) {
				while (true) {
					try {
						try {
							TX.TXbegin();
							for (int j = 0; j < NUM_GETS_IN_OUTTER; j++) {
								int key = rand.nextInt(KEY_RANGE);
								LinkedList innerll = (LinkedList) ll.get(key);
								if (innerll != null) {
									InnerTransaction(innerll);
								}
							}
						} finally {
							TX.TXend();
						}
					} catch (TXLibExceptions.AbortException exp) {
						continue;
					}
					break;
				}
			}
		}

		void InnerTransaction(LinkedList ll) {
			while (true) {
				try {
					try {
						TX.TXbegin();
						for (int i = 0; i < NUM_ACTION_IN_INNER; i++) {
							int action = rand.nextInt(2);
							Integer node = rand.nextInt(NUM_IN_INNER_LIST);
							if ((action & 1) == 0) { // add
								ll.putIfAbsent(node, node);
							} else { // remove
								ll.remove(node);
							}
						}
					} finally {
						TX.TXend();
					}
				} catch (TXLibExceptions.NestedAbortException exp) {
					throw exp;
				} catch (TXLibExceptions.AbortException exp) {
					if (FLAT_NESTING) {
						throw exp;
					}
					if (CLOSED_NESTING) {
						continue;
					}
				}
				break;
			}
		}

	}

	public static void main(String[] args) throws InterruptedException {

		NestingTest test = new NestingTest();
		test.parseArgs(args);

		CountDownLatch latch = new CountDownLatch(1);
		LinkedList ll = new LinkedList();
		long start, stop, diff;
		ArrayList<Thread> threads = new ArrayList<>(NUM_THREADS);

		// init
		System.out.println("init");
		for (int i = 0; i < NUM_OF_KEYS; i++) {
			int key = rand.nextInt(KEY_RANGE);
			LinkedList innerll = new LinkedList();
			for (int j = 0; j < NUM_IN_INNER_LIST; j++) {
				Integer node = rand.nextInt(NUM_IN_INNER_LIST);
				innerll.putIfAbsent(node, node);
			}
			ll.putIfAbsent(key, innerll);
		}
		for (int i = 0; i < NUM_THREADS; i++) {
			threads.add(new Thread(test.new ListObjectThread(ll, latch)));
		}
		for (int i = 0; i < NUM_THREADS; i++) {
			threads.get(i).start();
		}

		// run
		System.out.println("run");
		latch.countDown();
		start = System.currentTimeMillis();
		for (int i = 0; i < NUM_THREADS; i++) {
			threads.get(i).join();
		}
		stop = System.currentTimeMillis();

		diff = stop - start;
		System.out.println("\nTIME = " + diff + " ms\n");
		
		// validate
		System.out.println("validating...");
		for (int i = 0; i < KEY_RANGE; i++) {
			LinkedList o = (LinkedList) ll.remove(i);
			if (o != null) {
				for (int j = 0; j < NUM_IN_INNER_LIST; j++) {
					// assert
					Integer rm = (Integer) o.remove(j);
					if (rm != null) {
						if (rm < 0 || rm > NUM_IN_INNER_LIST) {
							System.out.println("validation failed");
							System.exit(1);
						}
					}
				}
			}
		}

		System.out.println("done");
	}

}
